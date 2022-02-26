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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.config.RequestConfig;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.ResizeRequest;
import org.elasticsearch.client.indices.ResizeResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.ReindexRequest;
import org.elasticsearch.index.reindex.ScrollableHitSource;
import org.idb.cacao.web.utils.ESUtils;
import org.idb.cacao.web.utils.ErrorUtils;
import org.idb.cacao.web.utils.ReflectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.index.MappingBuilder;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service with some 'sanitization' functions (e.g.: remove replicates from databases)
 * 
 * @author Gustavo Figueiredo
 *
 */
@Service
public class SanitizationService {

	private static final Logger log = Logger.getLogger(SanitizationService.class.getName());

	@Autowired
	private Collection<Repository<?, ?>> all_repositories;

	@Autowired
	private RestHighLevelClient elasticsearchClient;

	@FunctionalInterface
	public static interface ConsumeMappingDiff {
		public void onDifference(String path, String key, Object value1, Object value2);
	}

	/**
	 * Return the sorted list of all ElasticsearchRepository classes
	 */
	public List<Class<?>> getESRepositories() {
		List<Class<?>> es_repositories = new LinkedList<>();
		for (Repository<?,?> repo: all_repositories) {
			Class<?> found_interface = ReflectUtils.getInterfaceWithFilter(repo.getClass(), 
					/*filter*/c->c.getInterfaces()!=null && Arrays.stream(c.getInterfaces()).anyMatch(ElasticsearchRepository.class::equals));
			if (found_interface==null)
				continue;
			es_repositories.add(found_interface);
		}
		Collections.sort(es_repositories, new Comparator<Class<?>>() {
			@Override
			public int compare(Class<?> c1, Class<?> c2) {
				return c1.getSimpleName().compareToIgnoreCase(c2.getSimpleName());
			}			
		});
		return es_repositories;
	}

	/**
	 * Returns TRUE if the difference should be ignored
	 * @param field_definition_current Current configuration as defined in current database
	 * @param field_definition_expected Expected configuration
	 * @returns Returns FALSE if the difference should not be ignored (i.e. it should migrate to). Returns TRUE otherwise (should not migrate to)
	 */
	private static boolean avoidMigration(Map<?, ?> field_definition_current, Map<?, ?> field_definition_expected) {
		if (field_definition_current==null || field_definition_expected==null)
			return false; // migrate!
		if (isFieldKeyword(field_definition_expected) && isFieldTextWithKeyword(field_definition_current)) {
			// Ignore migration from   {type=text, fields={keyword={ignore_above=256, type=keyword}}}  to  {type=keyword}
			return true; // don't migrate!
		}
		if (isNestedKeyword(field_definition_expected)) {
			// Ignore migration from nested fields
			return true; // don't migrate!
		}
		return false; // migrate!
	}
	
	/**
	 * Returns TRUE if the field defition is something like this: 
	 *  {type=keyword}
	 */
	private static boolean isFieldKeyword(Map<?,?> field_definition) {
		if (field_definition==null || field_definition.isEmpty())
			return false;
		Object type = field_definition.get("type");
		if (!"keyword".equals(type))
			return false;
		Object fields = field_definition.get("fields");
		if (fields!=null)
			return false;
		return true;
	}
	
	/**
	 * Returns TRUE if the field defition is something like this: 
	 *  {type=nested}
	 */
	private static boolean isNestedKeyword(Map<?,?> field_definition) {
		if (field_definition==null || field_definition.isEmpty())
			return false;
		Object type = field_definition.get("type");
		if (!"nested".equals(type))
			return false;
		Object fields = field_definition.get("fields");
		if (fields!=null)
			return false;
		return true;
	}

	/**
	 * Returns TRUE if the field defition is something like this: 
	 *  {type=text, fields={keyword={type=keyword}}}
	 */
	private static boolean isFieldTextWithKeyword(Map<?,?> field_definition) {
		if (field_definition==null || field_definition.isEmpty())
			return false;
		Object type = field_definition.get("type");
		if (!"text".equals(type))
			return false;
		Object fields = field_definition.get("fields");
		if (!(fields instanceof Map))
			return false;
		Object keyword_internal_field = ((Map<?,?>)fields).get("keyword");
		if (!(keyword_internal_field instanceof Map))
			return false;
		Object keyword_internal_field_type = ((Map<?,?>)keyword_internal_field).get("type");
		return "keyword".equals(keyword_internal_field_type);
	}

	private static String append(String p1, String p2, String separator) {
		if (p1==null || p1.length()==0)
			return p2;
		if (p2==null || p2.length()==0)
			return p1;
		return p1+separator+p2;
	}

	/**
	 * Returns TRUE if the provided MAP corresponds to a field definition.
	 */
	private static boolean isFieldDefinition(Map<?,?> map) {
		return map!=null && map.containsKey("type") && map.containsKey("fields");
	}
	
	/**
	 * Returns TRUE if both objects contains the same contents.
	 */
	private static boolean sameContents(Object o1, Object o2) {
		if (o1==o2)
			return true;
		if (o1==null || o2==null)
			return false;
		if ((o1 instanceof Collection) || (o2 instanceof Collection)) {
			if (!(o1 instanceof Collection) || !(o2 instanceof Collection)) 
				return false;
			Collection<?> c1 = (Collection<?>)o1;
			Collection<?> c2 = (Collection<?>)o2;
			if (c1.size()!=c2.size())
				return false;
			Iterator<?> it1 = c1.iterator();
			Iterator<?> it2 = c2.iterator();
			while (it1.hasNext() && it2.hasNext()) {
				if (!sameContents(it1.next(), it2.next()))
					return false;
			}
			return true;
		}
		else if (o1.getClass().isArray() || o2.getClass().isArray()) {
			if (!(o1.getClass().isArray()) || !(o2.getClass().isArray()))
				return false;
			int len1 = Array.getLength(o1);
			int len2 = Array.getLength(o2);
			if (len1!=len2)
				return false;
			for (int i=0; i<len1; i++) {
				if (!sameContents(Array.get(o1, i), Array.get(o2, i)))
					return false;
			}
			return true;
		}
		else { 
			if (o1.equals(o2))
				return true;
			if ("float".equals(o1) && "double".equals(o2))
				return true;
			if ("double".equals(o1) && "float".equals(o2))
				return true;
			return false;
		}
	}

	/**
	 * Compares two maps and check if there are different values assigned to the same key or if there are missing keys. Ignores the following keys: 'id' and '_class'. 
	 * Also ignores absent 'fields definition' (i.e. definitions about fields that are declared in one place and absent in other).<BR>
	 * The purpose of this method is to check changes in code related to index mapping that should be reflected on pre-existing index.
	 */
	public static void compareCommonMappings(Map<?, ?> map1, Map<?, ?> map2, ConsumeMappingDiff diff, BooleanSupplier stopCondition, String path) {
		if (avoidMigration(map1, map2))
			return;
		for (Map.Entry<?, ?> map1_entry: map1.entrySet()) {
			if (stopCondition.getAsBoolean())
				break;
			Object key = map1_entry.getKey();
			if ("id".equals(key) || "_class".equals(key))
				continue; // ignore these fields
			Object map1_value = map1_entry.getValue();
			Object map2_value = map2.get(key);
			if (map1_value==null && map2_value==null)
				continue;
			if (map2_value==null && (map1_value instanceof Map) && isFieldDefinition((Map<?,?>)map1_value))
				continue;
			if (map1_value!=map2_value) {
				if (map1_value==null || map2_value==null 
					|| (!(map1_value instanceof Map) 
							&& !(map2_value instanceof Map) 
							&& !sameContents(map1_value, map2_value))) {
					if (map2_value!=null) {
						diff.onDifference(path, key.toString(), map1_value, map2_value);
					}
				}
			}
			if ((map1_value instanceof Map) && (map2_value instanceof Map)) {
				compareCommonMappings((Map<?,?>)map1_value, (Map<?,?>)map2_value, diff, stopCondition,
						/*path next level*/append(path, String.valueOf(key), "/"));
			}
		}
		// Check for keys that are absent in 'map1'
		for (Map.Entry<?, ?> map2_entry: map2.entrySet()) {
			if (stopCondition.getAsBoolean())
				break;
			Object key = map2_entry.getKey();
			if ("id".equals(key) || "_class".equals(key))
				continue; // ignore these fields
			if (map1.containsKey(key))
				continue; // we have already treated it in previous LOOP
			Object map2_value = map2_entry.getValue();
			if (map2_value instanceof Map && isFieldDefinition((Map<?,?>)map2_value))
				continue;
			if (map2_value!=null && !(map2_value instanceof Map))
				diff.onDifference(path, key.toString(), /*map1_value*/null, map2_value);
		}
	}

	/**
	 * Check all ElasticSearch indices mappings and compares to the expected mapping according to
	 * the source code annotations. Recreates indices that are not compatible, possibly because
	 * the code has been changed after the data has been indexed.
	 */
	@org.springframework.transaction.annotation.Transactional
	public void compatibilizeIndicesMappings() {

		MappingElasticsearchConverter map_conv = new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext());
		MappingBuilder map_builder = new MappingBuilder(map_conv);

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		for (Class<?> repository_class: getESRepositories()) {
			Class<?> entity = ReflectUtils.getParameterType(repository_class);
			if (entity==null)
				continue;
			Document doc_anon = entity.getAnnotation(Document.class);
			String indexName = doc_anon.indexName();
			if (indexName==null || indexName.trim().length()==0)
				continue;
			
			try {
				// Get current mapping
				Map<String, Object> curr_mapping = ESUtils.getMapping(elasticsearchClient, indexName).getSourceAsMap();
				
				// Get expected mapping
				String expected_mapping_as_json = map_builder.buildPropertyMapping(entity);
				@SuppressWarnings("unchecked")
				Map<String, Object> expected_mapping = (Map<String, Object>)mapper.readValue(expected_mapping_as_json, Map.class);
				
				// Compares both mappings
				// Ignores fields suppressed or included
				final AtomicBoolean found_difference = new AtomicBoolean();
				compareCommonMappings(curr_mapping, expected_mapping, 
					/*diff*/(path,key,value1,value2)->{
						found_difference.set(true);
						log.log(Level.WARNING, "Found a difference in index '"+indexName+"' used to store entity "+entity.getSimpleName()
						+" in mapping for field '"+key+"' at '"+path+"'. Value in current stored mapping: "+value1+". Value expected by class definition: "+value2);
					},
					/*stopCondition*/found_difference::get, 
					/*path*/"");
				
				if (!found_difference.get())
					continue;
				
				// Since we have a difference in index mapping, let's change the current index mapping

				RequestConfig requestConfig = RequestConfig.custom()
				.setConnectTimeout(60000)
				.setSocketTimeout(120000)
				.build();
				RequestOptions options = RequestOptions.DEFAULT.toBuilder()
				.setRequestConfig(requestConfig)
				.build();
				
				// Make the index read only (necessary for 'clone')
				ESUtils.changeBooleanIndexSetting(elasticsearchClient, indexName, ESUtils.SETTING_READ_ONLY, /*setting_value*/true, /*closeAndReopenIndex*/false);

				// Let's include in our mapping some parts that may have been left out
				@SuppressWarnings("unchecked")
				Map<String, Object> curr_props = (Map<String, Object>)curr_mapping.get("properties");
				@SuppressWarnings("unchecked")
				Map<String, Object> expected_props = (Map<String, Object>)expected_mapping.get("properties");
				if (curr_props!=null && expected_props!=null) {
					for (String internal_field: new String[] { "id", "_class"}) {
						if (curr_props.containsKey(internal_field))
							expected_props.computeIfAbsent(internal_field, k->curr_props.get(internal_field));
					}
				}

				final String cloned_indexName = indexName+"_temp";

				// Deletes temporary index if it already exists
				try {
					ESUtils.changeBooleanIndexSetting(elasticsearchClient, cloned_indexName, ESUtils.SETTING_READ_ONLY, /*setting_value*/false, /*closeAndReopenIndex*/false);
					// Deletes the current stored index
					DeleteIndexRequest delete_request = new DeleteIndexRequest(cloned_indexName);
					elasticsearchClient.indices().delete(delete_request, options);
				}
				catch (Throwable ex) {
					if (!ErrorUtils.isErrorNoIndexFound(ex)) {
						log.log(Level.WARNING, "Error while deleting temporary index"+cloned_indexName, ex);
					}
				}
				
				// Make a copy of the current stored index, as is
				ResizeRequest clone_request = new ResizeRequest(/*target_index*/cloned_indexName, /*source_index*/indexName);
				clone_request.setTimeout(TimeValue.timeValueSeconds(60));
				clone_request.setMasterTimeout(TimeValue.timeValueSeconds(60));
				clone_request.setWaitForActiveShards(1);
				ResizeResponse resizeResponse = elasticsearchClient.indices().clone(clone_request, options);
				if (!resizeResponse.isAcknowledged()) {
					ESUtils.changeBooleanIndexSetting(elasticsearchClient, indexName, ESUtils.SETTING_READ_ONLY, /*setting_value*/false, /*closeAndReopenIndex*/false);
					throw new RuntimeException("Could not change mapping of index '"+indexName+"'. Failed to acknownledge CLONE to "+cloned_indexName);
				}
				if (!resizeResponse.isShardsAcknowledged()) {
					
					// In case of error, try once more after a short delay
					
					Thread.sleep(10_000);
					Thread.yield();
					
					boolean ack1 = false, ack2 = false;
					try {
						resizeResponse = elasticsearchClient.indices().clone(clone_request, options);
						ack1 = resizeResponse.isAcknowledged();
						ack2 = resizeResponse.isShardsAcknowledged();
					}
					catch (Throwable ex) {
						
						if (ErrorUtils.isErrorIndexReadOnly(ex)) {
							
							// Try again (change READ ONLY status and deletes temporary index if it already exists)
							try {
								ESUtils.changeBooleanIndexSetting(elasticsearchClient, cloned_indexName, ESUtils.SETTING_READ_ONLY, /*setting_value*/false, /*closeAndReopenIndex*/false);
								DeleteIndexRequest delete_request = new DeleteIndexRequest(cloned_indexName);
								elasticsearchClient.indices().delete(delete_request, options);
							}
							catch (Throwable ex2) {
								if (!ErrorUtils.isErrorNoIndexFound(ex2)) {
									log.log(Level.WARNING, "Error while deleting temporary index"+cloned_indexName, ex2);
								}
							}

							try {
								resizeResponse = elasticsearchClient.indices().clone(clone_request, options);
								ack1 = resizeResponse.isAcknowledged();
								ack2 = resizeResponse.isShardsAcknowledged();
							}
							catch (Throwable ex2) {
								log.log(Level.SEVERE, "Error while cloning index "+indexName+" as "+cloned_indexName, ex2);
							}
						}
						else {
							log.log(Level.SEVERE, "Error while cloning index "+indexName+" as "+cloned_indexName, ex);
						}
					}
					if (!ack1) {
						ESUtils.changeBooleanIndexSetting(elasticsearchClient, indexName, ESUtils.SETTING_READ_ONLY, /*setting_value*/false, /*closeAndReopenIndex*/false);
						throw new RuntimeException("Could not change mapping of index '"+indexName+"'. Failed to acknownledge CLONE to "+cloned_indexName);
					}
					
					if (!ack2) {
						ESUtils.changeBooleanIndexSetting(elasticsearchClient, indexName, ESUtils.SETTING_READ_ONLY, /*setting_value*/false, /*closeAndReopenIndex*/false);
						throw new RuntimeException("Could not change mapping of index '"+indexName+"'. Failed to acknownledge CLONE to "+cloned_indexName);
					}
				}
				
				// Deletes the current stored index
				ESUtils.changeBooleanIndexSetting(elasticsearchClient, indexName, ESUtils.SETTING_READ_ONLY, /*setting_value*/false, /*closeAndReopenIndex*/false);
				DeleteIndexRequest delete_request = new DeleteIndexRequest(indexName);
				AcknowledgedResponse deleteIndexResponse = elasticsearchClient.indices().delete(delete_request, options);
				if (!deleteIndexResponse.isAcknowledged())
					throw new RuntimeException("Could not change mapping of index '"+indexName+"'. Failed to acknownledge DELETE "+indexName);
				
				// Recreates new index
				CreateIndexRequest create_request = new CreateIndexRequest(indexName);
				// mappings ...
				create_request.mapping(expected_mapping_as_json,XContentType.JSON);
				CreateIndexResponse createIndexResponse = elasticsearchClient.indices().create(create_request, options);
				if (!createIndexResponse.isAcknowledged())
					throw new RuntimeException("Could not change mapping of index '"+indexName+"'. Failed to recreate index "+indexName);

				// Reindex
				ReindexRequest request = new ReindexRequest();
				request.setSourceIndices(cloned_indexName);
				request.setDestIndex(indexName);
				request.setConflicts("proceed");
				request.setMaxRetries(3);
				request.getSearchRequest().allowPartialSearchResults(true);
				request.setTimeout(TimeValue.timeValueHours(1));
				BulkByScrollResponse bulkResponse =
						elasticsearchClient.reindex(request, options);

				TimeValue timeTaken = bulkResponse.getTook(); 
				boolean timedOut = bulkResponse.isTimedOut(); 
				long totalDocs = bulkResponse.getTotal(); 
				long updatedDocs = bulkResponse.getUpdated(); 
				long deletedDocs = bulkResponse.getDeleted(); 
				long batches = bulkResponse.getBatches(); 
				long noops = bulkResponse.getNoops(); 
				long versionConflicts = bulkResponse.getVersionConflicts(); 
				long bulkRetries = bulkResponse.getBulkRetries(); 
				long searchRetries = bulkResponse.getSearchRetries(); 
				List<ScrollableHitSource.SearchFailure> searchFailures =
				        bulkResponse.getSearchFailures(); 
				List<BulkItemResponse.Failure> bulkFailures =
				        bulkResponse.getBulkFailures(); 
				
				log.log(Level.INFO, "Finished UpdateByRequest over index '"+indexName+"'\n"
						+ "Time taken: "+timeTaken+"\n"
						+ "Timed out: "+timedOut+"\n"
						+ "Total docs: "+totalDocs+"\n"
						+ "Updated docs: "+updatedDocs+"\n"
						+ "Deleted docs: "+deletedDocs+"\n"
						+ "Batches: "+batches+"\n"
						+ "Noops: "+noops+"\n"
						+ "Version conflicts: "+versionConflicts+"\n"
						+ "Bulk retries: "+bulkRetries+"\n"
						+ "Search retries: "+searchRetries+"\n"
						+ "Search failures: "+searchFailures.size()+"\n"
						+ "Bulk failures: "+bulkFailures.size()+"\n");

				// Deletes the temporary index
				ESUtils.changeBooleanIndexSetting(elasticsearchClient, cloned_indexName, ESUtils.SETTING_READ_ONLY, /*setting_value*/false, /*closeAndReopenIndex*/false);
				delete_request = new DeleteIndexRequest(cloned_indexName);
				deleteIndexResponse = elasticsearchClient.indices().delete(delete_request, options);

			}
			catch (Throwable ex) {
				if (ErrorUtils.isErrorNoIndexFound(ex))
					continue;
				log.log(Level.WARNING, "Error while trying to compatibilize index mapping '"+indexName+"'", ex);
			}
		} // LOOP over all ElasticSearch repository classes

	}

}
