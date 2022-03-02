/*******************************************************************************
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in the Software without 
 * restriction, including without limitation the rights to use, copy, modify, merge, publish, 
 * distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or 
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS 
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR 
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN 
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.services;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.elasticsearch.client.RestHighLevelClient;
import org.idb.cacao.api.PublishedDataFieldNames;
import org.idb.cacao.api.errors.CommonErrors;
import org.idb.cacao.api.templates.TemplateArchetype;
import org.idb.cacao.api.templates.TemplateArchetypes;
import org.idb.cacao.api.utils.IndexNamesUtils;
import org.idb.cacao.web.utils.ESUtils;
import org.idb.cacao.web.utils.ESUtils.KibanaIndexPattern;
import org.idb.cacao.web.utils.ESUtils.KibanaSavedObject;
import org.idb.cacao.web.utils.ESUtils.KibanaSpace;
import org.idb.cacao.web.utils.HttpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service for synchronizing saved objects at Kibana Spaces with the corresponding
 * objects at CACAO web application
 * 
 * @author Gustavo Figueiredo
 *
 */
@Service
public class KibanaSpacesService {

	private static final Logger log = Logger.getLogger(KibanaSpacesService.class.getName());
	
	/**
	 * The field name to be used by default as date/time filter of published data
	 */
	private static final String dashboardTimestamp = IndexNamesUtils.formatFieldName(PublishedDataFieldNames.TIMESTAMP.name());

	@Autowired
	private RestHighLevelClient elasticsearchClient;

	@Autowired
	private PublishedDataService publishedDataService;
	
	@Autowired
	private Environment env;
	
	/**
	 * Keeps track of all archetypes names for which we've already checked the corresponding index pattern at Kibana<BR>
	 * Avoids too many lookups
	 */
	private final Set<String> checkedArchetypes;
	
	/**
	 * Keeps track of all ElasticSearch index names for which we've already checked the corresponding index pattern at Kibana<BR>
	 * Avoids too many lookups
	 */
	private final Set<String> checkedIndices;

    private RestTemplate restTemplate;

	@Autowired
	public KibanaSpacesService(RestTemplateBuilder builder) {
		this.restTemplate = builder
				.setConnectTimeout(Duration.ofMinutes(5))
				.setReadTimeout(Duration.ofMinutes(5))
				.requestFactory(HttpUtils::getTrustAllHttpRequestFactory)
				.build();
		this.checkedArchetypes = Collections.synchronizedSet(new HashSet<>());
		this.checkedIndices = Collections.synchronizedSet(new HashSet<>());
	}

	/**
	 * Checks all existent templates associated to published data. If any one of them is not yet represented at Kibana space
	 * as a 'index pattern', automatically creates one and replicates this over all existent Kibana spaces.
	 */
	public void syncKibanaIndexPatterns() {
		syncKibanaIndexPatterns(/*collectCreationInfo*/null);
	}
	
	/**
	 * Checks all existent templates associated to published data. If any one of them is not yet represented at Kibana space
	 * as a 'index pattern', automatically creates one and replicates this over all existent Kibana spaces.
	 * @param collectCreationInfo If not NULL, collects output from this method about index creation
	 */
	public void syncKibanaIndexPatterns(Consumer<String> collectCreationInfo) {
		
		Map<String,KibanaSavedObject> map_patterns;
		try {
			List<KibanaSavedObject> saved_objects = ESUtils.getKibanaIndexPatterns(env, restTemplate, /*spaceId*/null);
			map_patterns = saved_objects.stream().collect(
					Collectors.toMap(
							/*keyMapper*/s->s.getTitle(), 
							/*valueMapper*/Function.identity(), 
							/*mergeFunction*/(a,b)->a));
		} catch (Throwable ex) {
			log.log(Level.WARNING, "Failed to get Kibana index patterns for the default space", ex);
			return;
		}
		
		List<KibanaSpace> spaces;
		try {
			spaces = ESUtils.getKibanaSpaces(env, restTemplate);
		} catch (Throwable ex) {
			log.log(Level.WARNING, "Failed to get Kibana spaces", ex);
			return;
		}

		// LOOP over all index related to published data
		List<String> indices;
		try {
			indices = publishedDataService.getIndicesForPublishedData();
		}
		catch (Throwable ex) {
			if (CommonErrors.isErrorNoIndexFound(ex) || CommonErrors.isErrorNoMappingFoundForColumn(ex))
				indices = Collections.emptyList();
			else {
				log.log(Level.WARNING, "Failed to get ElasticSearch indices", ex);
				return;
			}
		}
		
		for (String index: indices) {

			try {
				syncKibanaIndexPatternsInternal(index, spaces, map_patterns, collectCreationInfo);
			}
			catch (Throwable ex) {
				log.log(Level.WARNING, "Failed to synchronize index patterns at Kibana for ElasticSearch index "+index, ex);
			}

		}
	}

	/**
	 * Check if already created at Kibana default space all the index patterns related to published data related to the 
	 * archetype. Creates a new index-pattern if none exists. Replicates the created index-pattern to all existent Kibana spaces.
	 * @param avoidRedundantChecks If TRUE, avoids redundant check of index patterns for the provided archetypes (i.e. do not check
	 * again if already checked before)
	 */
	public void syncKibanaIndexPatterns(boolean avoidRedundantChecks, String... archetypes) {
		
		if (archetypes==null || archetypes.length==0)
			return;
		
		Map<String,KibanaSavedObject> map_patterns = null; // lazy instantiated
		List<KibanaSpace> spaces = null; // lazy instantiated
		
		for (String archetype: archetypes) {
			
			if (archetype==null || archetype.length()==0)
				continue;
			
			synchronized (archetype.intern()) {
		
				// If we've already checked all index patterns related to this archetype name,
				// let's skip
				if (avoidRedundantChecks 
						&& checkedArchetypes.contains(archetype))
					continue;
				
				// Get the indices names for all published (denormalized) data derived from the archetype
				Optional<TemplateArchetype> arch = TemplateArchetypes.getArchetype(archetype);
				if (!arch.isPresent())
					continue;
				
				// Consider the published data indices related to this archetype, except those indices
				// we've already checked before
				Set<String> indices = TemplateArchetypes.getRelatedPublishedDataIndices(
						/*filter*/i->!checkedIndices.contains(i),
						arch.get());
				
				if (indices.isEmpty())
					return;
				
				// We need to know all the existent index patterns
				if (map_patterns==null) {
					try {
						List<KibanaSavedObject> saved_objects = ESUtils.getKibanaIndexPatterns(env, restTemplate, /*spaceId*/null);
						map_patterns = saved_objects.stream().collect(
								Collectors.toMap(
										/*keyMapper*/s->s.getTitle(), 
										/*valueMapper*/Function.identity(), 
										/*mergeFunction*/(a,b)->a));
					} catch (Throwable ex) {
						log.log(Level.WARNING, "Failed to get Kibana index patterns for the default space", ex);
						return;
					}
				}
		
				// We need to know all the existent Kibana spaces
				if (spaces==null) {
					try {
						spaces = ESUtils.getKibanaSpaces(env, restTemplate);
					} catch (Throwable ex) {
						log.log(Level.WARNING, "Failed to get Kibana spaces", ex);
						return;
					}
				}
		
				int count_index_patterns_ok = 0;
				
				for (String index: indices) {
		
					try {
						boolean ok = syncKibanaIndexPatternsInternal(index, spaces, map_patterns, /*collectCreationInfo*/null);
						if (ok)
							count_index_patterns_ok++;
					}
					catch (Throwable ex) {
						log.log(Level.WARNING, "Failed to synchronize index patterns at Kibana for ElasticSearch index "+index+" related to archetype "+archetype, ex);
					}
		
				}
				
				if (count_index_patterns_ok==indices.size()) {
					checkedArchetypes.add(archetype); // prevent checking again this archetype
				}

			}
			
		}

	}
	
	/**
	 * Check if already created at Kibana default space all the index patterns related to published data related to the 
	 * index corresponding to generic template (not related to archetype). Creates a new index-pattern if none exists. Replicates the created index-pattern to all existent Kibana spaces.
	 * @param avoidRedundantChecks If TRUE, avoids redundant check of index patterns for the provided index (i.e. do not check
	 * again if already checked before)
	 */	
	public void syncKibanaIndexPatternForGenericTemplate(boolean avoidRedundantChecks, String index) {

		synchronized (index.intern()) {

			if (avoidRedundantChecks && checkedIndices.contains(index))
				return;
			
			// We need to know all the existent index patterns
			Map<String,KibanaSavedObject> map_patterns;
			try {
				List<KibanaSavedObject> saved_objects = ESUtils.getKibanaIndexPatterns(env, restTemplate, /*spaceId*/null);
				map_patterns = saved_objects.stream().collect(
						Collectors.toMap(
								/*keyMapper*/s->s.getTitle(), 
								/*valueMapper*/Function.identity(), 
								/*mergeFunction*/(a,b)->a));
			} catch (Throwable ex) {
				log.log(Level.WARNING, "Failed to get Kibana index patterns for the default space", ex);
				return;
			}
	
			// We need to know all the existent Kibana spaces
			List<KibanaSpace> spaces;
			try {
				spaces = ESUtils.getKibanaSpaces(env, restTemplate);
			} catch (Throwable ex) {
				log.log(Level.WARNING, "Failed to get Kibana spaces", ex);
				return;
			}
	
			try {
				syncKibanaIndexPatternsInternal(index, spaces, map_patterns, /*collectCreationInfo*/null);
			}
			catch (Throwable ex) {
				log.log(Level.WARNING, "Failed to synchronize index patterns at Kibana for ElasticSearch index "+index, ex);
			}
			
		}

	}

	/**
	 * Internal function used by {@link #syncKibanaIndexPatterns() syncKibanaIndexPatterns}<BR>
	 * Check if there is an index-pattern created at Kibana default space for the given template. Creates a new index-pattern
	 * if none exists. Replicates the created index-pattern to all existente Kibana spaces.
	 * @param collectCreationInfo If not NULL, collects output from this method about index creation
	 * @return Returns TRUE if the index pattern was created successfully or if it already exists
	 */
	private boolean syncKibanaIndexPatternsInternal(String indexName, List<KibanaSpace> spaces, Map<String,KibanaSavedObject> map_patterns,
			Consumer<String> collectCreationInfo) {
		
		if (indexName==null || indexName.trim().length()==0)
			return false;

		final String indexPatternTitle = indexName+"*";

		synchronized (indexPatternTitle.intern()) {
			
			if (checkedIndices.contains(indexName))
				return true;
		
			final String dataName = IndexNamesUtils.getCapitalizedNameForPublishedData(indexName);
			if (map_patterns.containsKey(indexPatternTitle)) {
				checkedIndices.add(indexName);
				return true;
			}
			else {
				boolean created = syncKibanaIndexPatternsInternal(indexName, indexPatternTitle, dataName, spaces);
				if (created && collectCreationInfo!=null)
					collectCreationInfo.accept("Created index-pattern "+indexPatternTitle+" at all known Kibana spaces\n");
				return created;
			}
			
		}

	}
	
	/**
	 * If the index pattern does not exist yet, let's create a new index pattern for it, as long
	 * as we have enough data
	 * @return Returns TRUE if the index pattern was created successfully
	 */
	private boolean syncKibanaIndexPatternsInternal(String indexName, String indexPatternTitle, String templateName, List<KibanaSpace> spaces) {
		
		long count;
		try {
			count = ESUtils.countDocs(elasticsearchClient, indexName);
		}
		catch (Throwable ex) {
			if (CommonErrors.isErrorNoIndexFound(ex))
				return false; // index does not exist
			log.log(Level.WARNING, "Failed to get documents count at index "+indexName, ex);
			return false;
		}
		
		if (count<getMinimumDocumentsForAutoCreateIndexPattern()) {
			return false; // not enough documents to consider for auto creating an index pattern at Kibana
		}
		
		checkedIndices.add(indexName);
		
		// Creates an index pattern at Kibana for representing this index
		KibanaIndexPattern new_index_pattern = new KibanaIndexPattern();
		new_index_pattern.setTitle(indexPatternTitle);
		new_index_pattern.setTimeFieldName(dashboardTimestamp);
		
		String indexPatternId;
		try {
			indexPatternId = ESUtils.createKibanaIndexPattern(env, restTemplate, /*spaceId*/null, new_index_pattern);
			if (indexPatternId==null || indexPatternId.trim().length()==0)
				throw new RuntimeException("The API did not create a new index pattern!");
		}
		catch (Throwable ex) {
			if (ex.getMessage()!=null && ex.getMessage().contains("Duplicate index pattern")) {
				// Ignore if the index pattern already exists
				List<KibanaSavedObject> existing_index_patterns;
				try {
					existing_index_patterns = ESUtils.getKibanaIndexPatterns(env, restTemplate, /*spaceId*/null);
				} catch (Exception e) {
					log.log(Level.WARNING, "Failed to create a new index pattern called '"+indexPatternTitle+"' at Kibana for template "+templateName, ex);
					return false;
				}
				indexPatternId = (existing_index_patterns==null) ? null
						: existing_index_patterns.stream().filter(ip->indexPatternTitle.equalsIgnoreCase(ip.getTitle())).findAny().map(ip->ip.getId()).orElse(null);
				if (indexPatternId==null) {
					log.log(Level.WARNING, "Failed to create a new index pattern called '"+indexPatternTitle+"' at Kibana for template "+templateName, ex);
					return false;
				}
			}
			else {
				log.log(Level.WARNING, "Failed to create a new index pattern called '"+indexPatternTitle+"' at Kibana for template "+templateName, ex);
				return false;
			}
		}
		
		// Copy the same index pattern to all known Kibana spaces
		if (spaces!=null && !spaces.isEmpty()) {
			for (KibanaSpace space: spaces) {
				if (space.getId()==null || space.getId().trim().length()==0 || "default".equalsIgnoreCase(space.getId()))
					continue;
				try {
					ESUtils.copyKibanaSavedObjects(env, restTemplate, /*spaceIdSource*/null, /*spaceIdTarget*/space.getId(), "index-pattern", new String[] {indexPatternId});
				}
				catch (Throwable ex) {
					log.log(Level.WARNING, "Failed to copy index pattern called '"+indexPatternTitle+"' with id '"+indexPatternId+"' at Kibana default space to the space "+space.getName()+" with id "+space.getId(), ex);						
				}
			}
		}

		return true;
	}
	
	/**
	 * Returns the configured minimum number of documents it expects to find at a particular index before trying
	 * to create automatically an index pattern at Kibana
	 */
	public int getMinimumDocumentsForAutoCreateIndexPattern() {
		try {
			return Integer.parseInt(env.getProperty("docs.min.auto.index.pattern", /*defaultValue*/"1"));
		}
		catch (Throwable ex) {
			return 0;
		}
	}

	/**
	 * Removes all in-memory 'checked' status (necessary after some index-pattern was deleted)
	 */
	public void clearChecked() {
		checkedArchetypes.clear();
		checkedIndices.clear();
	}
}
