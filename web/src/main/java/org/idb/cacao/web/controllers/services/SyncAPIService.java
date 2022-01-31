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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.idb.cacao.api.errors.GeneralException;
import org.idb.cacao.api.storage.FileSystemStorageService;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.TemplateArchetype;
import org.idb.cacao.api.templates.TemplateArchetypes;
import org.idb.cacao.api.utils.DateTimeUtils;
import org.idb.cacao.api.utils.IndexNamesUtils;
import org.idb.cacao.web.Synchronizable;
import org.idb.cacao.web.controllers.rest.SyncAPIController;
import org.idb.cacao.web.dto.SyncDto;
import org.idb.cacao.web.entities.ConfigSync;
import org.idb.cacao.web.entities.SyncCommitHistory;
import org.idb.cacao.web.entities.SyncCommitMilestone;
import org.idb.cacao.web.entities.SyncPeriodicity;
import org.idb.cacao.web.errors.InvalidParameter;
import org.idb.cacao.web.errors.SyncIsRunningException;
import org.idb.cacao.web.repositories.DocumentTemplateRepository;
import org.idb.cacao.web.repositories.SyncCommitHistoryRepository;
import org.idb.cacao.web.repositories.SyncCommitMilestoneRepository;
import org.idb.cacao.web.utils.ESUtils;
import org.idb.cacao.web.utils.ErrorUtils;
import org.idb.cacao.web.utils.ReflectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.Repository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Service methods for 'synchronization' with other cacao servers.
 * 
 * @author Gustavo Figueiredo
 *
 */
@Service
public class SyncAPIService {
	
	private static final Logger log = Logger.getLogger(SyncAPIService.class.getName());
	
	/**
	 * Number of instances to put in the same 'batch' for saving in local database (improves performance over individual saves)
	 */
	private static final int BATCH_SIZE = 100;
	
	/**
	 * Number of records before partial commits while doing SYNC with validated or published data (using bulk load).<BR>
	 * Too low means more delays between batches.<BR>
	 * Too high means more memory consumed.
	 */
	private static final int BULK_LOAD_BATCH_COMMIT = 10_000;

    @Autowired
    private MessageSource messages;

	@Autowired
	private ApplicationContext app;

	@Autowired
	private ConfigSyncService configSyncService;

	@Autowired
	private FieldsConventionsService fieldsConventionsService;

	@Autowired
	private FileSystemStorageService fileSystemStorageService;
	
	@Autowired
	private PublishedDataService publishedDataService;

	@Autowired
	private DocumentTemplateRepository templateRepository;
	
	@Autowired
	private SyncCommitMilestoneRepository syncCommitMilestoneRepository;
	
	@Autowired
	private SyncCommitHistoryRepository syncCommitHistoryRepository;

	@Autowired
	private RestHighLevelClient elasticsearchClient;
	
	@Autowired
	private Collection<Repository<?, ?>> all_repositories;

	@Value("${spring.elasticsearch.rest.connection-timeout}")
	private String elasticSearchConnectionTimeout;

    private RestTemplate restTemplate;
    
    /**
     * Keeps reference to 'Future' object created when we scheduled the SYNC task
     */
    private volatile ScheduledFuture<?> futureSyncScheduled;

	/**
	 * Keeps reference for thread running a SYNC process started locally (at subscriber server)
	 */
	private static final AtomicReference<WeakReference<SyncThread>> runningSyncThread = new AtomicReference<>();
	
	@Autowired
	public SyncAPIService(RestTemplateBuilder builder) {
		this.restTemplate = builder
				.setConnectTimeout(Duration.ofMinutes(5))
				.setReadTimeout(Duration.ofMinutes(5))
				.build();
	}

	/**
	 * Call all SYNC methods for all objects.
	 * @param tokenApi Token API registered at master for SYNC requests
	 * @param resumeFromLastSync Indicates if we should care about the last SYNC milestone to avoid requesting the same data again
	 */
	public void syncAll(String tokenApi, boolean resumeFromLastSync) {
		
		Optional<Long> end = Optional.of(System.currentTimeMillis());
		
		try {
			// SYNC original files
			syncOriginalFiles(tokenApi, (resumeFromLastSync)?getStartForNextSync(SyncContexts.ORIGINAL_FILES.getEndpoint(),0L):0L, end);
		}
		catch (Throwable ex) {
			log.log(Level.SEVERE, "Error while performing SYNC for original files", ex);
		}
		
		// SYNC bases (data kept in repositories, except documents)
		List<Class<?>> all_sync_repos = getAllSyncRepositories();
		for (Class<?> repository_class: all_sync_repos) {
			try {
				syncBase(tokenApi, repository_class, (resumeFromLastSync)?getStartForNextSync(SyncContexts.REPOSITORY_ENTITIES.getEndpoint(repository_class.getSimpleName()),0L):0L, end);
			}
			catch (Throwable ex) {
				log.log(Level.SEVERE, "Error while performing SYNC for "+repository_class.getSimpleName(), ex);
			}
		}
				
		// SYNC validated data for all templates and versions
		List<DocumentTemplate> templates;
		try {
			templates = StreamSupport.stream(templateRepository.findAll().spliterator(), false)
					.sorted(Comparator.comparing(DocumentTemplate::getName).thenComparing(DocumentTemplate::getVersion))
					.collect(Collectors.toList());
		} catch (Throwable ex) {
			if (!ErrorUtils.isErrorNoIndexFound(ex)) 
				throw ex;
			else
				templates = Collections.emptyList();
		}
		
		Set<String> archetypesNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		for (DocumentTemplate template: templates) {
			try {
				syncValidatedData(tokenApi, template, (resumeFromLastSync)?getStartForNextSync(SyncContexts.VALIDATED_DATA.getEndpoint(template.getName(),template.getVersion()),0L):0L, end);
				if (template.getArchetype()!=null && template.getArchetype().trim().length()>0)
					archetypesNames.add(template.getArchetype());
			}
			catch (Throwable ex) {
				log.log(Level.SEVERE, "Error while performing SYNC for parsed docs of template "+template.getName()+"/"+template.getVersion(), ex);
			}
		}
		
		List<String> indices_for_published_data;
		
		if (!archetypesNames.isEmpty()) {
			TemplateArchetype[] archetypes = archetypesNames.stream()
					.map(TemplateArchetypes::getArchetype)
					.filter(Optional::isPresent)
					.map(Optional::get)
					.toArray(TemplateArchetype[]::new);
			Set<String> published_indices = TemplateArchetypes.getRelatedPublishedDataIndices(archetypes);
			if (published_indices!=null && !published_indices.isEmpty()) {
				indices_for_published_data = new ArrayList<>(published_indices);
			}
			else {
				indices_for_published_data = Collections.emptyList();
			}
		}
		else {
			indices_for_published_data = Collections.emptyList();
		}
		
		// SYNC published data for all indices produced by ETL phases
		for (String indexname: indices_for_published_data) {
			try {
				syncPublishedData(tokenApi, indexname, (resumeFromLastSync)?getStartForNextSync(SyncContexts.PUBLISHED_DATA.getEndpoint(indexname),0L):0L, end);
			}
			catch (Throwable ex) {
				log.log(Level.SEVERE, "Error while performing SYNC for published data "+indexname, ex);
			}
		}

		// SYNC Kibana assets
		try {
			syncKibana(tokenApi, (resumeFromLastSync)?getStartForNextSync(SyncContexts.KIBANA_ASSETS.getEndpoint(),0L):0L, end);
		}
		catch (Throwable ex) {
			log.log(Level.SEVERE, "Error while performing SYNC for Kibana assets", ex);
		}
	}

	/**
	 * Call SYNC methods relative some some endpoints.
	 * @param tokenApi Token API registered at master for SYNC requests
	 * @param resumeFromLastSync Indicates if we should care about the last SYNC milestone to avoid requesting the same data again
	 * @param endpoints List of endpoints we will try to resolve for objects
	 */
	public void syncSome(String tokenApi, boolean resumeFromLastSync, List<String> endpoints) {
		
		Optional<Long> end = Optional.of(System.currentTimeMillis());
		
		try {
			// SYNC original files
			if (SyncContexts.hasContext(endpoints, SyncContexts.ORIGINAL_FILES))
				syncOriginalFiles(tokenApi, (resumeFromLastSync)?getStartForNextSync(SyncContexts.ORIGINAL_FILES.getEndpoint(),0L):0L, end);
		}
		catch (Throwable ex) {
			log.log(Level.SEVERE, "Error while performing SYNC for original files", ex);
		}
		
		// SYNC bases (data kept in repositories, except documents)
		List<Class<?>> all_sync_repos = getAllSyncRepositories();
		for (Class<?> repository_class: all_sync_repos) {
			try {
				if (SyncContexts.hasContext(endpoints, SyncContexts.REPOSITORY_ENTITIES, repository_class.getSimpleName()))
					syncBase(tokenApi, repository_class, (resumeFromLastSync)?getStartForNextSync(SyncContexts.REPOSITORY_ENTITIES.getEndpoint(repository_class.getSimpleName()),0L):0L, end);
			}
			catch (Throwable ex) {
				log.log(Level.SEVERE, "Error while performing SYNC for "+repository_class.getSimpleName(), ex);
			}
		}
				
		// SYNC validated data for all templates and versions
		List<DocumentTemplate> templates;
		try {
			templates = StreamSupport.stream(templateRepository.findAll().spliterator(), false)
					.sorted(Comparator.comparing(DocumentTemplate::getName).thenComparing(DocumentTemplate::getVersion))
					.collect(Collectors.toList());
		} catch (Throwable ex) {
			if (!ErrorUtils.isErrorNoIndexFound(ex)) 
				throw ex;
			else
				templates = Collections.emptyList();
		}
		for (DocumentTemplate template: templates) {
			try {
				if (SyncContexts.hasContext(endpoints, SyncContexts.VALIDATED_DATA, template.getName()+"/"+template.getVersion()))
					syncValidatedData(tokenApi, template, (resumeFromLastSync)?getStartForNextSync(SyncContexts.VALIDATED_DATA.getEndpoint(template.getName(),template.getVersion()),0L):0L, end);
			}
			catch (Throwable ex) {
				log.log(Level.SEVERE, "Error while performing SYNC for parsed docs of template "+template.getName()+"/"+template.getVersion(), ex);
			}
		}
		
		// SYNC published data for all indices produced by ETL phases
		List<String> indices_for_published_data;
		try {
			indices_for_published_data = publishedDataService.getIndicesForPublishedData().stream().sorted().collect(Collectors.toList());
		} catch (Throwable ex) {
			if (!ErrorUtils.isErrorNoIndexFound(ex)) 
				throw new RuntimeException(ex);
			else
				indices_for_published_data = Collections.emptyList();
		}
		for (String indexname: indices_for_published_data) {
			try {
				if (SyncContexts.hasContext(endpoints, SyncContexts.PUBLISHED_DATA, indexname))
					syncPublishedData(tokenApi, indexname, (resumeFromLastSync)?getStartForNextSync(SyncContexts.PUBLISHED_DATA.getEndpoint(indexname),0L):0L, end);
			}
			catch (Throwable ex) {
				log.log(Level.SEVERE, "Error while performing SYNC for published data "+indexname, ex);
			}
		}

		try {
			// SYNC Kibana assets
			if (SyncContexts.hasContext(endpoints, SyncContexts.KIBANA_ASSETS))
				syncKibana(tokenApi, (resumeFromLastSync)?getStartForNextSync(SyncContexts.KIBANA_ASSETS.getEndpoint(),0L):0L, end);
		}
		catch (Throwable ex) {
			log.log(Level.SEVERE, "Error while performing SYNC for Kibana assets", ex);
		}

	}

	/**
	 * Retrieves and saves locally all original files created or changed between a time interval
	 */
	@Transactional
	public void syncOriginalFiles(String tokenApi, long start, Optional<Long> end) {
		
		final File original_files_dir;
		try {
			original_files_dir = fileSystemStorageService.getRootLocation().toFile();
			if (!original_files_dir.exists())
				original_files_dir.mkdirs();
			if (!original_files_dir.exists())
				throw new FileNotFoundException(original_files_dir.getAbsolutePath());
		} catch (Throwable ex) {
			log.log(Level.SEVERE, "Error while synchronizing with "+SyncContexts.ORIGINAL_FILES.getEndpoint(), ex);
			throw new GeneralException(messages.getMessage("error.failed.sync", null, LocaleContextHolder.getLocale()));
		}
		
		// Expects to find files with this name pattern inside ZIP
		// Let's reject anything else for safety (e.g. 'man in the middle attack' or compromised master would serve malicious files) 
		final Pattern pattern_name_entry = Pattern.compile("^(?>original)?[\\/]?(\\d{4})[\\/](\\d{1,2})[\\/]([\\d+\\-A-Za-z]+)$");

		LongAdder counter = new LongAdder();
		LongAdder sum_bytes = new LongAdder();
		final long timestamp = System.currentTimeMillis();
		
		syncSomething(tokenApi, SyncContexts.ORIGINAL_FILES.getEndpoint(), start, end, /*limit*/-1,
		/*consumer*/(entry,input)->{
			String entry_name = entry.getName();
			Matcher matcher_name_entry = pattern_name_entry.matcher(entry_name);
			if (!matcher_name_entry.find()) {
				log.log(Level.INFO, "Ignoring entry with unrecognizable name ("+entry_name+") received from "+SyncContexts.ORIGINAL_FILES.getEndpoint());
				return;
			}
			File year_dir = new File(original_files_dir, matcher_name_entry.group(1));
			File month_dir = new File(year_dir, matcher_name_entry.group(2));
			File target_file = new File(month_dir, matcher_name_entry.group(3));
			if (!target_file.getParentFile().exists())
				target_file.getParentFile().mkdirs();
			if (target_file.exists())
				target_file.delete();
			Files.copy(input, target_file.toPath());
			if (entry.getLastModifiedTime()!=null)
				target_file.setLastModified(entry.getLastModifiedTime().toMillis());
			counter.increment();
			sum_bytes.add(target_file.length());
		});

		final long elapsed_time = System.currentTimeMillis() - timestamp;
		log.log(Level.INFO, "Finished copying "+counter.longValue()+" files from "+SyncContexts.ORIGINAL_FILES.getEndpoint()+" in "+elapsed_time+" ms and "+sum_bytes.longValue()+" bytes");
	}
	
	/**
	 * Retrieves and saves locally all entity instances created or changed between a time interval
	 */
	@Transactional
	public void syncBase(String tokenApi, Class<?> repository_class, long start, Optional<Long> end) {
		
		final Class<?> entity = ReflectUtils.getParameterType(repository_class);
		if (entity==null) {
			throw new GeneralException("Could not find entity class related to '"+repository_class+"'!");
		}
		
		@SuppressWarnings({ "unchecked" })
		CrudRepository<Object, ?> repository = (CrudRepository<Object, ?>)app.getBean(repository_class);

		final ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.registerModule(new JavaTimeModule());

		final long timestamp = System.currentTimeMillis();
		final String uri = SyncContexts.REPOSITORY_ENTITIES.getEndpoint(repository_class.getSimpleName());
		
		final BatchSave batch_to_save = new BatchSave(BATCH_SIZE, entity.getSimpleName(), repository);

		syncSomething(tokenApi, uri, start, end, /*limit*/-1,
		/*consumer*/(entry,input)->{
			
			// We will ignore the entry name because it's supposed to be equal to the entity 'ID', which we
			// also have inside de JSON contents
			
			// Let's deserialize the JSON contents
			Object instance = mapper.readValue(input, entity);
			
			// Add instance to batch (may also save if batch hits configured size)
			batch_to_save.push(instance);
			
		});
		
		// Saves data that remained at batch
		batch_to_save.saveBatch();
		
		final long elapsed_time = System.currentTimeMillis() - timestamp;
		log.log(Level.INFO, "Finished copying "+batch_to_save.counter.longValue()+" files from "+uri+" in "+elapsed_time+" ms");
		if (batch_to_save.count_errors.longValue()>0)
			log.log(Level.INFO, "Number of errors while copying "+batch_to_save.counter.longValue()+" files from "+uri+": "+batch_to_save.count_errors.longValue());
	}
	
	/**
	 * Utility object used for storing batch of elements we want to save
	 * @author Gustavo Figueiredo
	 *
	 */
	private static class BatchSave {
		private final int batch_size;
		private final List<Object> batch_to_save;
		private final CrudRepository<Object, ?> repository;
		private final LongAdder count_errors;
		private final LongAdder counter;
		private final String simple_name;
		BatchSave(int batch_size,
				String simple_name,
				CrudRepository<Object, ?> repository) {
			this.batch_size = batch_size;
			this.simple_name = simple_name;
			this.repository = repository;
			batch_to_save = new ArrayList<>(batch_size);
			count_errors = new LongAdder();
			counter = new LongAdder();
		}
		void push(Object instance) {
			batch_to_save.add(instance);
			if (batch_to_save.size()>=batch_size) {
				saveBatch();
			}
		}
		void saveBatch() {
			if (batch_to_save.isEmpty())
				return;
			// Let's save this information in repository (either JPA or ElasticSearch)
			try {
				repository.saveAll(batch_to_save);
				counter.add(batch_to_save.size());
			}
			catch (Throwable ex) {
				// In case of error, let's try saving one at a time
				if (batch_to_save.size()>1) {
					for (Object record: batch_to_save) {
						try {
							repository.save(record);
							counter.increment();
						}
						catch (Throwable ex2) {
							log.log(Level.WARNING, "Error while saving "+simple_name+" with contents "+getSampleInfoForLog(record), ex);
							count_errors.increment();
							return;							
						}
					}
				}
				else {
					log.log(Level.WARNING, "Error while saving "+simple_name+" with contents "+getSampleInfoForLog(batch_to_save), ex);
					count_errors.increment();
					return;
				}
			}
			batch_to_save.clear();
		}
		static String getSampleInfoForLog(Object target) {
			if ((target instanceof Collection) && ((Collection<?>)target).isEmpty())
				return "<none>";
			if (target==null)
				return "<none>";
			if (target instanceof Collection) {
				Object instance = ((Collection<?>)target).iterator().next();
				ObjectMapper m_out_json = new ObjectMapper();
				m_out_json.registerModule(new JavaTimeModule());
				m_out_json.setSerializationInclusion(Include.NON_NULL);
				String info;
				try {
					info = m_out_json.writeValueAsString(instance);
				} catch (JsonProcessingException e) {
					return "<no information available>";
				}
				if (((Collection<?>)target).size()>1) {
					info += " and "+(((Collection<?>)target).size()-1)+" others";
				}
				return info;
			}
			else {
				ObjectMapper m_out_json = new ObjectMapper();
				m_out_json.setSerializationInclusion(Include.NON_NULL);
				m_out_json.registerModule(new JavaTimeModule());
				try {
					return m_out_json.writeValueAsString(target);
				} catch (JsonProcessingException e) {
					return "<no information available>";
				}				
			}
		}
	}

	/**
	 * Retrieves and saves locally all parsed documents created or changed between a time interval
	 */
	@Transactional
	public void syncValidatedData(String tokenApi, DocumentTemplate template, long start, Optional<Long> end) {
		
    	if (template==null) {
    		throw new InvalidParameter("template");
    	}
    	
		final String index_name = IndexNamesUtils.formatIndexNameForValidatedData(template);

        // Creates the index, in case it has not been created yet
        try {
        	boolean has_indice;
        	try {
        		ESUtils.hasMappings(elasticsearchClient, index_name);
        		has_indice = true; // the index may exist and have no mapping
        	}
        	catch (Throwable ex) {
        		if (ErrorUtils.isErrorNoIndexFound(ex))
        			has_indice = false;
        		else
        			throw ex;
        	}
	        if (!has_indice) {
		        try {
			        ESUtils.createIndex(elasticsearchClient, index_name, /*ignore_malformed*/true);
		        }
		        catch (Throwable ex) {
		        	log.log(Level.WARNING, "Ignoring error while creating new index '"+index_name+"' for template '"+template.getName()+"'", ex);
		        }
	        }
        }
        catch (Throwable ex) {
        	log.log(Level.WARNING, "Ignoring error while checking existence of index '"+index_name+"' for template '"+template.getName()+"'", ex);
        }

		final ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.registerModule(new JavaTimeModule());

		LongAdder counter = new LongAdder();
		final long timestamp = System.currentTimeMillis();
		final String uri = SyncContexts.VALIDATED_DATA.getEndpoint(template.getName(), template.getVersion());
		
		final BulkRequest bulkRequest = new BulkRequest();
		if (elasticSearchConnectionTimeout!=null
			&& elasticSearchConnectionTimeout.trim().length()>0)
			bulkRequest.timeout(elasticSearchConnectionTimeout);
		
		final LongAdder recordsForCommit = new LongAdder();

		syncSomething(tokenApi, uri, start, end, /*limit*/BULK_LOAD_BATCH_COMMIT,
		/*consumer*/(entry,input)->{
			
			// We will ignore the entry name because it's supposed to be equal to the entity 'ID', which we
			// also have inside de JSON contents

			// Let's deserialize the JSON contents
			@SuppressWarnings("unchecked")
			Map<String, Object> parsed_contents = (Map<String, Object>)mapper.readValue(input, Map.class);
			
			String id = (String)parsed_contents.get("id");
			if (id==null)
				return;
			
			bulkRequest.add(new IndexRequest(index_name)
					.id(id)
					.source(parsed_contents));

			recordsForCommit.increment();
			
			if (recordsForCommit.longValue()>BULK_LOAD_BATCH_COMMIT) {
				commitBulkRequest(bulkRequest, index_name);
				recordsForCommit.reset();
			}

			counter.increment();
		});

		commitBulkRequest(bulkRequest, index_name);

		final long elapsed_time = System.currentTimeMillis() - timestamp;
		log.log(Level.INFO, "Finished copying "+counter.longValue()+" files from "+uri+" in "+elapsed_time+" ms");
	}

	/**
	 * Retrieves and saves locally all published (denormalized) data created or changed between a time interval
	 */
	@Transactional
	public void syncPublishedData(String tokenApi, String indexname, long start, Optional<Long> end) {
		
    	if (indexname==null || indexname.trim().length()==0 || !indexname.startsWith(IndexNamesUtils.PUBLISHED_DATA_INDEX_PREFIX)) {
    		throw new InvalidParameter("indexname");
    	}
    	
        // Creates the index, in case it has not been created yet
        try {
        	boolean has_indice;
        	try {
        		ESUtils.hasMappings(elasticsearchClient, indexname);
        		has_indice = true; // the index may exist and have no mapping
        	}
        	catch (Throwable ex) {
        		if (ErrorUtils.isErrorNoIndexFound(ex))
        			has_indice = false;
        		else
        			throw ex;
        	}
	        if (!has_indice) {
		        try {
			        ESUtils.createIndex(elasticsearchClient, indexname, /*ignore_malformed*/true);
		        }
		        catch (Throwable ex) {
		        	log.log(Level.WARNING, "Ignoring error while creating new index '"+indexname+"' ", ex);
		        }
	        }
        }
        catch (Throwable ex) {
        	log.log(Level.WARNING, "Ignoring error while checking existence of index '"+indexname+"' ", ex);
        }

		final ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.registerModule(new JavaTimeModule());

		LongAdder counter = new LongAdder();
		final long timestamp = System.currentTimeMillis();
		final String uri = SyncContexts.PUBLISHED_DATA.getEndpoint(indexname);

		final BulkRequest bulkRequest = new BulkRequest();

		final LongAdder recordsForCommit = new LongAdder();

		syncSomething(tokenApi, uri, start, end, /*limit*/BULK_LOAD_BATCH_COMMIT,
		/*consumer*/(entry,input)->{
			
			// We will ignore the entry name because it's supposed to be equal to the entity 'ID', which we
			// also have inside de JSON contents

			// Let's deserialize the JSON contents
			@SuppressWarnings("unchecked")
			Map<String, Object> parsed_contents = (Map<String, Object>)mapper.readValue(input, Map.class);
			
			String id = (String)parsed_contents.get("id");
			if (id==null)
				return;
			
			bulkRequest.add(new IndexRequest(indexname)
					.id(id)
					.source(parsed_contents));
			
			recordsForCommit.increment();
			
			if (recordsForCommit.longValue()>BULK_LOAD_BATCH_COMMIT) {
				commitBulkRequest(bulkRequest, indexname);
				recordsForCommit.reset();
			}

			counter.increment();
		});
		
		commitBulkRequest(bulkRequest, indexname);
		
		final long elapsed_time = System.currentTimeMillis() - timestamp;
		log.log(Level.INFO, "Finished copying "+counter.longValue()+" files from "+uri+" in "+elapsed_time+" ms");
	}

	/**
	 * Retrieves and saves locally all Kibana assets created or changed between a time interval
	 */
	@Transactional
	public void syncKibana(String tokenApi, long start, Optional<Long> end) {
		
		final ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.registerModule(new JavaTimeModule());

		LongAdder counter = new LongAdder();
		final long timestamp = System.currentTimeMillis();
		final String uri = SyncContexts.KIBANA_ASSETS.getEndpoint();

		syncSomething(tokenApi, uri, start, end, /*limit*/-1,
		/*consumer*/(entry,input)->{
			
			// The entry name includes the index used for Kibana. We need to keep it. The rest of the entry name
			// is the 'id' converted to something compatible with file syntax. We can't use it because it has been
			// converted.			
			String entry_name = entry.getName();
			int sep = entry_name.indexOf('/');
			if (sep<=0)
				sep = entry_name.indexOf('\\');
			if (sep<=0)
				return; // not expected
			
			String index_name = entry_name.substring(0, sep);
			if (!index_name.startsWith(".kibana"))
				return; // not expected
			if (SyncAPIController.KIBANA_IGNORE_PATTERNS.matcher(index_name).find())
				return; // not expected

			// Let's deserialize the JSON contents
			@SuppressWarnings("unchecked")
			Map<String, Object> parsed_contents = (Map<String, Object>)mapper.readValue(input, Map.class);
			
			String id = (String)parsed_contents.remove("id");
			if (id==null)
				return;
			
			for (String fields_should_be_long: new String[] {
			"canvas-workpad-template.template.pages.elements.position.top",
			"canvas-workpad-template.template.pages.elements.position.left",
			"canvas-workpad-template.template.pages.elements.position.width",
			"canvas-workpad-template.template.pages.elements.position.height"
			}) {
				recursiveSet(parsed_contents, fields_should_be_long, new ConvertToDouble());
			}
			
			ESUtils.indexWithRetry(elasticsearchClient,
					new IndexRequest(index_name)
					.id(id)
					.source(parsed_contents)
					.setRefreshPolicy(RefreshPolicy.IMMEDIATE));

			counter.increment();
		});
		
		final long elapsed_time = System.currentTimeMillis() - timestamp;
		log.log(Level.INFO, "Finished copying "+counter.longValue()+" files from "+uri+" in "+elapsed_time+" ms");
	}
	
	private static class ConvertToDouble implements Function<Object,Object> {
		@Override
		public Object apply(Object value) {
			if ((value instanceof Short) || (value instanceof Integer) || (value instanceof Long))
				return ((Number)value).doubleValue();
			else
				return value;
		}		
	}
	
	private static void recursiveSet(Map<String,Object> parsed_contents, String path, Function<Object,Object> changer) {
		int sep = path.indexOf('.');
		if (sep>0) {
			String seek_nested_obj = path.substring(0, sep);
			String remaining_path = path.substring(sep+1);
			Object nested_obj = parsed_contents.get(seek_nested_obj);
			if (nested_obj instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String,Object> nested_map = (Map<String,Object>)nested_obj;
				recursiveSet(nested_map, remaining_path, changer);
			}
			else if (nested_obj instanceof List) {
				List<?> list = (List<?>)nested_obj;
				for (Object el:list) {
					if (el instanceof Map) {
						@SuppressWarnings("unchecked")
						Map<String,Object> nested_map = (Map<String,Object>)el;
						recursiveSet(nested_map, remaining_path, changer);						
					}
				}
			}
			return;
		}
		else {
			Object value = parsed_contents.get(path);
			if (value!=null) {
				Object new_value = changer.apply(value);
				if (new_value!=value) {
					parsed_contents.put(path, new_value);
				}
			}
		}
	}

	private void syncSomething(String tokenApi, String endPoint, long start, Optional<Long> end, long limit,
			ConsumeSyncContents consumer) {
		
		ConfigSync config = configSyncService.getActiveConfig();
		if (config==null || config.getMaster()==null || config.getMaster().trim().length()==0) 
			throw new GeneralException(messages.getMessage("error.missing.config_sync", null, LocaleContextHolder.getLocale()));
		
		RequestCallback requestCallback = req->{
			req.getHeaders().set(HttpHeaders.AUTHORIZATION, "api-key "+tokenApi);
			req.getHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
		};
		
		final String master = config.getMaster();
		String uri = "https://"+master+endPoint+"?start="+start;
		if (end!=null && end.isPresent())
			uri +="&end="+end.get();
		if (limit>0)
			uri += "&limit="+limit;
		
		boolean has_more = false; // will be set to TRUE if there is more data to SYNC for
		
		log.log(Level.INFO, "SYNC started for endpoint '"+endPoint+"'");
		
		OffsetDateTime lastTimeStart = (start==0) ? null : new Date(start).toInstant().atOffset(ZoneOffset.UTC);

		final LongAdder count_incoming_objects_overall = new LongAdder();

		do {
			
			has_more = false; // resets the flag if we are repeating SYNC
		
			// Make the HTTP request and load its response by streaming data
			
			AtomicReference<SyncDto> sync_info = new AtomicReference<>();
			
			final OffsetDateTime lastTimeRun = DateTimeUtils.now();
			
			final LongAdder count_incoming_objects = new LongAdder();
			
			boolean successful = false; // will be set to TRUE after 'restTemplate.execute' completes
			
			try {

				restTemplate.execute(uri, HttpMethod.GET, requestCallback, clientHttpResponse->{
		
					// Got the synchronized contents in a local temporary file. Let's open it and read its contents
		
					InputStream file_input = new BufferedInputStream(clientHttpResponse.getBody());
					CheckedInputStream chk_input = new CheckedInputStream(file_input, new Adler32());
					ZipInputStream zip_input = new ZipInputStream(chk_input);
					
					try {
						ZipEntry ze;
						while (((ze = zip_input.getNextEntry()) != null)) {
							
							// If entry corresponds to SyncDto, read and store this information (should be only one per ZIP file)
							if (SyncDto.DEFAULT_FILENAME.equals(ze.getName())) {
								sync_info.set(readSyncDto(zip_input));
								continue;
							}
							
							// Populates the local base with incoming data
							
							consumer.load(ze, new NoClosingInputStream(zip_input));
							count_incoming_objects.increment();
							count_incoming_objects_overall.increment();
							
						} // LOOP through zip contents
					} catch (IOException ex) {
						if (ErrorUtils.isErrorStreamClosed(ex)) {
							// ignore this error silently
							//ex.printStackTrace();
						}
						else {
							//ex.printStackTrace();
							throw ex;
						}
					}
						
					return Boolean.TRUE;
					
				});
				
				successful = true;
				
			}
			finally {

				if (sync_info.get()!=null) {
					log.log(Level.FINE, "SYNC info for endpoint '"+endPoint+"': "+sync_info.get());
				}
	
				if (successful || count_incoming_objects.longValue()>0) {
					try {
						// Update SYNC repository with milestone to avoid replay the same SYNC again
						OffsetDateTime lastTimeEnd = (sync_info.get()!=null && sync_info.get().hasMore()) ? 
								sync_info.get().getActualEnd().toInstant().atOffset(ZoneOffset.UTC) : (end!=null && end.isPresent()) ? 
										new Date(end.get().longValue()).toInstant().atOffset(ZoneOffset.UTC) : lastTimeRun;
						saveSyncMilestone(master, endPoint, lastTimeRun, lastTimeStart, lastTimeEnd, count_incoming_objects.longValue(), successful);
					}
					catch (Throwable ex) {
						log.log(Level.SEVERE, "SYNC COMMIT failed for endpoint '"+endPoint+"'", ex);
					}
				}
				
			}

			// Check if there is more to search for
			if (sync_info.get()!=null && sync_info.get().hasMore()) {
				// Will do the next SYNC with the next timestamp (should not be NULL if 'hasMore')
				uri = "https://"+master+endPoint+"?start="+sync_info.get().getNextStart().getTime();
				if (sync_info.get().getNextLineStart()!=null && sync_info.get().getNextLineStart().longValue()>0)
					uri += "&line_start="+sync_info.get().getNextLineStart();
				if (end!=null && end.isPresent())
					uri +="&end="+end.get();
				if (log.isLoggable(Level.INFO))
					log.log(Level.INFO, "URI for resuming partial SYNC: "+uri+" (got "+count_incoming_objects_overall.longValue()+" objects so far)");
				has_more = true; // will repeat SYNC with remaining data
				lastTimeStart = sync_info.get().getNextStart().toInstant().atOffset(ZoneOffset.UTC);
			}
			
		} while (has_more);
		
	}
	
	/**
	 * Save SYNC milestone at database for next call and save SYNC history for logging
	 */
	@Transactional
	public void saveSyncMilestone(String master, String endPoint, OffsetDateTime lastTimeRun, OffsetDateTime lastTimeStart, OffsetDateTime lastTimeEnd,
			long countArrivedObjects, boolean successful) {
		
		if (successful) {
			Optional<SyncCommitMilestone> existent_milestone = syncCommitMilestoneRepository.findByEndPoint(endPoint);
			SyncCommitMilestone milestone = existent_milestone.orElseGet(SyncCommitMilestone::new);
			milestone.setEndPoint(endPoint);
			milestone.setLastTimeRun(lastTimeRun);
			if (lastTimeStart!=null && lastTimeStart.toEpochSecond()>0)
				milestone.setLastTimeStart(lastTimeStart);
			if (lastTimeEnd!=null && lastTimeEnd.toEpochSecond()>0)
				milestone.setLastTimeEnd(lastTimeEnd);
			syncCommitMilestoneRepository.save(milestone);
		}
		
		SyncCommitHistory hist = new SyncCommitHistory();
		hist.setMaster(master);
		hist.setEndPoint(endPoint);
		hist.setTimeRun(lastTimeRun);
		if (lastTimeStart!=null && lastTimeStart.toEpochSecond()>0)
			hist.setTimeStart(lastTimeStart);
		if (lastTimeEnd!=null && lastTimeEnd.toEpochSecond()>0)
			hist.setTimeEnd(lastTimeEnd);
		hist.setCountObjects(countArrivedObjects);
		hist.setSuccessful(successful);
		syncCommitHistoryRepository.save(hist);
	}
	
	/**
	 * Verifies if we have already called SYNC operation for some endpoint. If we did, returns the last requested timestamp
	 * to be the next start timestamp. If we didn't, returns 'fallback'.
	 */
	@Transactional(readOnly=true)
	public long getStartForNextSync(String endPoint, long fallback) {
		Optional<SyncCommitMilestone> existent_milestone = syncCommitMilestoneRepository.findByEndPoint(endPoint);
		return existent_milestone.map(SyncCommitMilestone::getLastTimeEnd).map(off->off.toInstant().toEpochMilli()).orElse(fallback);
	}
	
	/**
	 * Returns indication that we have previously execute a SYNC operation for any context
	 * @return
	 */
	@Transactional(readOnly=true)
	public boolean hasPreviousSync() {
		try {
			return syncCommitMilestoneRepository.count()>0;
		}
		catch (Throwable ex) {
			if (ErrorUtils.isErrorNoIndexFound(ex))
				return false;
			throw ex;
		}		
	}
	
	/**
	 * Returns indication that we have previously execute a SYNC operation for one specific endpoint
	 * @return
	 */
	@Transactional(readOnly=true)
	public boolean hasPreviousSync(String endpoint) {
		try {
			return syncCommitMilestoneRepository.findByEndPoint(endpoint).isPresent();
		}
		catch (Throwable ex) {
			if (ErrorUtils.isErrorNoIndexFound(ex))
				return false;
			throw ex;
		}		
	}

	/**
	 * Return the list of repositories for all synchronizable instances
	 */
	public List<Class<?>> getAllSyncRepositories() {
		List<Class<?>> synchronizable_repositories = new LinkedList<>();
		for (Repository<?,?> repo: all_repositories) {
			Class<?> found_interface = ReflectUtils.getInterfaceWithFilter(repo.getClass(), 
					/*filter*/cl->cl.getAnnotation(Synchronizable.class)!=null);
			if (found_interface!=null) {
				Synchronizable sync_anon = found_interface.getAnnotation(Synchronizable.class);
				if (sync_anon==null)
					continue;
				synchronizable_repositories.add(found_interface);
			}
		}
		return synchronizable_repositories;
	}

	/**
	 * Functional interface using internally for reusing code
	 * @author Gustavo Figueiredo
	 *
	 */
	@FunctionalInterface
	public static interface ConsumeSyncContents {
		
		/**
		 * Method called for each ZIP entry from incoming synchronized data
		 */
		public void load(ZipEntry entry, InputStream input) throws IOException;
		
	}

	/**
	 * Reads 'SyncDto' object stored in JSON format from InputStream
	 */
	public static SyncDto readSyncDto(InputStream input) {
		final ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.registerModule(new JavaTimeModule());
		try {
			return mapper.readValue(input, SyncDto.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Return the instance created with information about the SYNC thread
	 */
	public SyncThread getRunningSyncThread() {
		WeakReference<SyncThread> ref = runningSyncThread.get();
		return (ref==null) ? null : ref.get();
	}
	
	/**
	 * Starts a new SYNC thread, unless there is another running SYNC thread already
	 */
	public void startSyncThread(String user, boolean resumeFromLastSync, List<String> endpoints) {
		synchronized (runningSyncThread) {
			SyncThread t = getRunningSyncThread();
			if (t!=null && t.isRunning()) {
				throw new SyncIsRunningException(messages.getMessage("error.sync.already.running", new Object[] { t.getUser(), 
						fieldsConventionsService.formatValue(t.getStart())
				}, LocaleContextHolder.getLocale()));
			}
			
			t = app.getBean(SyncThread.class);
			t.setUser(user);
			t.setStart(new Date());
			t.setResumeFromLastSync(resumeFromLastSync);
			t.setEndpoints(endpoints);
			TaskExecutor taskExecutor = app.getBean("SyncTaskExecutor", TaskExecutor.class);
			taskExecutor.execute(t);
			runningSyncThread.set(new WeakReference<>(t));
		}
	}
	
	/**
	 * Start a new SCHEDULE thread that will trigger SYNC events according to configuration
	 */
	public void scheduleSyncThread() {
		ConfigSync config = configSyncService.getActiveConfig();
		if (config==null || config.getMaster()==null || config.getMaster().trim().length()==0) 
			throw new GeneralException(messages.getMessage("error.missing.config_sync", null, LocaleContextHolder.getLocale()));
		cancelScheduledSync();
		if (SyncPeriodicity.NONE.equals(config.getPeriodicity()) || config.getPeriodicity()==null)
			return;
		CronTrigger trigger;
		if (SyncPeriodicity.DAILY.equals(config.getPeriodicity())) {
			trigger = new CronTrigger("0 0 "+config.getHourOfDay()+" * * ?");
		}
		else if (SyncPeriodicity.HOURLY.equals(config.getPeriodicity())) {
			trigger = new CronTrigger("0 0 * * * ?");
		}
		else if (SyncPeriodicity.WEEKLY.equals(config.getPeriodicity())) {
			trigger = new CronTrigger("0 0 "+config.getHourOfDay()+" ? * "+(config.getDayOfWeek().ordinal()+1));
		}
		else {
			return;
		}
		synchronized (runningSyncThread) {
			TaskScheduler taskScheduler = app.getBean("SyncTaskScheduler", TaskScheduler.class);
			futureSyncScheduled = taskScheduler.schedule(()->{
				try {
					startSyncThread(/*user*/null, /*resumeFromLastSync*/true, /*endpoints*/null);
				}
				catch (Throwable ex) {
					log.log(Level.SEVERE, "Error while running SYNC at schedule time", ex);
				}
			}, trigger);
		}
	}
	
	/**
	 * Cancel a previous scheduled SYNC
	 */
	public void cancelScheduledSync() {
		synchronized (runningSyncThread) {
			// Removes previous schedule
			if (futureSyncScheduled!=null) {
				try {
					boolean canceled = futureSyncScheduled.cancel(/*mayInterruptIfRunning*/false);
					if (canceled) {
						log.log(Level.INFO, "Previous scheduled SYNC has been successfully canceled");
					}
					else {
						log.log(Level.INFO, "Previous scheduled SYNC could not be canceled");
					}
				}
				catch (Throwable ex) {
					log.log(Level.SEVERE, "Error while trying to cancel previous scheduled SYNC", ex);
				}
				finally {
					futureSyncScheduled = null;
				}
			}
		}		
	}
	
	/**
	 * Wrapper for a InputStream preventing it from getting closed.
	 * @author Gustavo Figueiredo
	 */
	public static class NoClosingInputStream extends FilterInputStream {

		public NoClosingInputStream(InputStream in) {
			super(in);
		}

		@Override
		public void close() throws IOException {
			// don't close
		}
		
	}
	
	private void commitBulkRequest(BulkRequest bulkRequest, String index_name) {
		if (bulkRequest.numberOfActions()>0) {
			bulkRequest.setRefreshPolicy(RefreshPolicy.NONE);
			try {
				elasticsearchClient.bulk(bulkRequest,
					RequestOptions.DEFAULT);
			}
			catch (Throwable ex) {
				try {
					if (null!=ErrorUtils.getIllegalArgumentTypeMismatch(ex)
							|| null!=ErrorUtils.getIllegalArgumentInputString(ex)) {
						// In case of an error relative to type mismatch, lets try again after changing some of the index parameters
						ESUtils.changeBooleanIndexSetting(elasticsearchClient, index_name, ESUtils.SETTING_IGNORE_MALFORMED, true, /*closeAndReopenIndex*/true);
						elasticsearchClient.bulk(bulkRequest,
								RequestOptions.DEFAULT);
					}
					else {
						throw ex;
					}
				}
				catch (RuntimeException|Error ex2) {
					throw ex2;
				}
				catch (Exception ex2) {
					throw new RuntimeException(ex2);
				}
			}
			bulkRequest.requests().clear();
		}		
	}
}
