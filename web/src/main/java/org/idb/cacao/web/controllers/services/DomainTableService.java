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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.sort.SortOrder;
import org.idb.cacao.api.DomainLanguage;
import org.idb.cacao.api.errors.CommonErrors;
import org.idb.cacao.api.templates.DomainEntry;
import org.idb.cacao.api.templates.DomainTable;
import org.idb.cacao.api.templates.TemplateArchetype;
import org.idb.cacao.api.templates.TemplateArchetypes;
import org.idb.cacao.api.utils.ScrollUtils;
import org.idb.cacao.web.controllers.AdvancedSearch;
import org.idb.cacao.web.repositories.DomainTableRepository;
import org.idb.cacao.web.utils.SearchUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service methods for domain tables related to the WEB component
 * 
 * @author Gustavo Figueiredo
 *
 */
@Service
public class DomainTableService {

	private static final Logger log = Logger.getLogger(DomainTableService.class.getName());

	private static final String PROPERTIES_PREFIX = "classpath:messages_";
	private static final String PROPERTIES_SUFFIX = ".properties";

	@Autowired
	private MessageSource messages;
	
	@Autowired
	private Environment env;
	
	@Autowired
	private DomainTableRepository domainTableRepository;

	@Autowired
	private RestHighLevelClient elasticsearchClient;

	/**
	 * Returns all the 'provided languages' (i.e. languages defined in 'DomainLanguage' for which we
	 * have a corresponding 'messages_XX.properties' file at classpath).
	 */
	public Set<DomainLanguage> getProvidedLanguages() {
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		Set<DomainLanguage> provided = new TreeSet<>();
		String default_language = env.getProperty("cacao.user.language");
		for (DomainLanguage language: DomainLanguage.values()) {
			Locale locale = language.getDefaultLocale();
			if (locale==null)
				continue;
			if (default_language!=null && default_language.equalsIgnoreCase(locale.getLanguage())) {
				provided.add(language);
			}
			else {
				Resource resource = resourceLoader.getResource(PROPERTIES_PREFIX + locale.getLanguage() + PROPERTIES_SUFFIX);
				if (resource!=null && resource.exists())
					provided.add(language);
				else {
					resource = resourceLoader.getResource(PROPERTIES_PREFIX + locale.getLanguage() + "_" + locale.getCountry() + PROPERTIES_SUFFIX);
					if (resource!=null && resource.exists())
						provided.add(language);
				}
			}
		}
		return provided;
	}
	
	/**
	 * Given a built-in domain table without specific language definitions, search for 'messages.properties'
	 * entries for the given language and tries to resolve all the messages provided in the domain table entries.
	 */
	public DomainTable resolveDomainTableForLanguage(DomainTable builtInTable, DomainLanguage language) {
		if (builtInTable==null || builtInTable.getNumEntries()==0 || language==null)
			return builtInTable;
		
		Locale locale = language.getDefaultLocale();
		if (locale==null)
			return null;
		
		DomainTable resolved = builtInTable.clone();
		resolved.clearEntries();
		
		for (DomainEntry entry: builtInTable.getEntriesOfLanguage(null)) {
			String message = messages.getMessage(entry.getDescription(), /*args*/null, /*defaultMessage*/null, locale);
			if (message==null)
				continue;
			resolved.addEntry(entry.getKey(), language, message, /*locked*/true);
		}
		
		return resolved;
	}
	
	/**
	 * Given a built-in domain table without specific language definitions, search for 'messages.properties'
	 * entries for the given languages and tries to resolve all the messages provided in the domain table entries.
	 * @param languages Lists all the languages to resolve, or empty if you want it for all defined languages
	 */
	public DomainTable resolveDomainTableForLanguages(DomainTable builtInTable, DomainLanguage... languages) {
		if (builtInTable==null || builtInTable.getNumEntries()==0)
			return builtInTable;
		
		if (languages==null || languages.length==0)
			languages = getProvidedLanguages().toArray(new DomainLanguage[0]);

		DomainTable resolved = builtInTable.clone();
		resolved.clearEntries();
		
		for (DomainLanguage language: languages) {
			
			Locale locale = language.getDefaultLocale();
			if (locale==null)
				continue;
			
			for (DomainEntry entry: builtInTable.getEntriesOfLanguage(null)) {
				String message = messages.getMessage(entry.getDescription(), /*args*/null, /*defaultMessage*/null, locale);
				if (message==null)
					continue;
				resolved.addEntry(entry.getKey(), language, message, /*locked*/true);
			}
			
		}
		
		if (resolved.getNumEntries()==0)
			return null;
		
		return resolved;
	}
	
	/**
	 * Given a template archetype, looks for all of the domain table definitions. Creates each of these that are not created yet, resolving
	 * the internal descriptions to the provided languages.
	 */
	public void assertDomainTablesForAchetype(TemplateArchetype arch) {
		if (arch==null)
			return;
		List<DomainTable> builtInDomainTables = arch.getBuiltInDomainTables();
		if (builtInDomainTables==null || builtInDomainTables.isEmpty())
			return;
		
		for (DomainTable builtInDomainTable: builtInDomainTables) {
			try {
				assertDomainTable(builtInDomainTable, /*overwrite*/false);
			}
			catch (Throwable ex) {
				log.log(Level.SEVERE, "Error while asserting the built-in domain table "+builtInDomainTable.getName(), ex);
			}
		}
	}
	
	/**
	 * Check if the domain table exists. If it does not exist, creates the domain table resolving all its message according
	 * to the provided languages.
	 * @param overwrite If FALSE, will not overwrite existing domain tables.
	 */
	public void assertDomainTable(DomainTable builtInDomainTable, boolean overwrite) {

		// Locate domain table
		Optional<DomainTable> matchingDomainTable = domainTableRepository.findByNameAndVersion(builtInDomainTable.getName(), builtInDomainTable.getVersion());
		// If the table already exists, keep the existing one
		if (!overwrite && matchingDomainTable.isPresent()) 
			return;
		
		// If the table does not exists yet, or if we should overwrite, creates according to the domain table specification
		
		builtInDomainTable.setActive(true);

		if (overwrite && matchingDomainTable.isPresent()) {
			domainTableRepository.delete(matchingDomainTable.get());
		}

		if (builtInDomainTable.getNumEntries(null)>0) {
			// The built-in domain table has entries without a language specification, so let's resolve them to the provided messages.properties files
			DomainTable resolvedDomainTable = resolveDomainTableForLanguages(builtInDomainTable);
			// If it could not resolve the domain table entries, does not create one
			if (resolvedDomainTable==null)
				return;
			domainTableRepository.saveWithTimestamp(resolvedDomainTable);
		}
		else if (builtInDomainTable.getNumEntries()>0) {
			// The built-in domain table has entries for a specific language, so let's create the domain table with the provided information
			domainTableRepository.saveWithTimestamp(builtInDomainTable);
		}		
	}
	
	/**
	 * For all installed 'TemplateArchetype's, check for the presence of built-in DomainTable's and creates the missing
	 * ones.
	 * @param overwrite If FALSE, will not overwrite existing domain tables.
	 * @return Returns the number of domain tables created
	 */
	public int assertDomainTablesForAllArchetypes(boolean overwrite) {
		
		List<DomainTable> builtInDomainTables = TemplateArchetypes.getBuiltInDomainTables();
		if (builtInDomainTables==null || builtInDomainTables.isEmpty())
			return 0;

		int count = 0;
		for (DomainTable builtInDomainTable: builtInDomainTables) {
			try {
				assertDomainTable(builtInDomainTable, overwrite);
				count++;
			}
			catch (Throwable ex) {
				log.log(Level.SEVERE, "Error while asserting the built-in domain table "+builtInDomainTable.getName(), ex);
			}
		}
		
		return count;
	}
	
	/**
	 * Returns the names of all domain tables ignoring their versions
	 */
	public Set<String> getDomainTablesNames() {
		
		try (Stream<DomainTable> stream = ScrollUtils.findAll(domainTableRepository, elasticsearchClient, 1)) {
			return stream.map(DomainTable::getName).collect(Collectors.toCollection(()->new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
		}
		catch (Throwable ex) {
			if (CommonErrors.isErrorNoIndexFound(ex) || CommonErrors.isErrorNoMappingFoundForColumn(ex))
				return Collections.emptySet();
			else
				throw ex;
		}
		
	}
	
	/**
	 * Returns the versions for a given domain table
	 */
	public Set<String> getDomainTablesVersions(String name) {
		try {
			return domainTableRepository.findByNameIgnoreCaseAndActiveTrue(name).stream().map(DomainTable::getVersion).collect(Collectors.toCollection(()->new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
		}
		catch (Throwable ex) {
			if (CommonErrors.isErrorNoIndexFound(ex) || CommonErrors.isErrorNoMappingFoundForColumn(ex))
				return Collections.emptySet();
			else
				throw ex;			
		}
	}
	
	/**
     * Search Domain Table objects using AdvancedSearch filters
     */
	@Transactional(readOnly=true)
	public Page<DomainTable> searchDomainTables(Optional<AdvancedSearch> filters,
			Optional<Integer> page, 
			Optional<Integer> size) {
		try {
			return SearchUtils.doSearch(filters.get().wiredTo(messages), DomainTable.class, elasticsearchClient, page, size, Optional.of("name"), Optional.of(SortOrder.ASC));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}
}
