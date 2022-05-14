/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.services;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.idb.cacao.api.errors.CommonErrors;
import org.idb.cacao.api.errors.GeneralException;
import org.idb.cacao.api.storage.FileSystemStorageService;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.TemplateArchetype;
import org.idb.cacao.api.templates.TemplateArchetypes;
import org.idb.cacao.api.utils.DateTimeUtils;
import org.idb.cacao.api.utils.IndexNamesUtils;
import org.idb.cacao.api.utils.MappingUtils;
import org.idb.cacao.api.utils.ReflectUtils;
import org.idb.cacao.api.utils.ScrollUtils;
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
import org.idb.cacao.web.utils.KeepAliveStrategy;
import org.idb.cacao.web.utils.LoadFromParquet;
import org.idb.cacao.web.utils.ZipConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.Repository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.io.CountingInputStream;

/**
 * Service methods for 'synchronization' with other cacao servers.
 * 
 * @author Gustavo Figueiredo
 *
 */
@Service
public class SyncAPIService {
	
	private static final String BYTES = " bytes";
	private static final String FILES_FROM = " files from ";
	private static final String FINISHED_COPYING = "Finished copying ";
	
	private static final Logger log = Logger.getLogger(SyncAPIService.class.getName());
	
	/**
	 * Number of records before partial commits while doing SYNC with validated or published data (using bulk load).<BR>
	 * Too low means more delays between batches.<BR>
	 * Too high means more memory to load, what could potentially cause the error 'HTTP/1.1 413 Request Entity Too Large' at
	 * ElasticSearch.
	 */
	private static final int BULK_LOAD_BATCH_COMMIT = 10_000;

	/**
	 * Number of records to return at each request from the server.<BR>
	 * Too low means more requests, with more overhead for each one of them.<BR>
	 * Too high means more time waiting response, what could potentially result in response timeout.
	 */
	private static final int MAX_RESULTS_PER_REQUEST = 100_000;
	
	/**
	 * Tells if each Bulk Request should be executed asynchronously. If it's set to TRUE, it will try to execute one Bulk Request
	 * while performing the next request of SYNC data. If it's set to FALSE, it will finish one Bulk Request before issuing the next request.
	 */
	public static final boolean BULK_REQUEST_ASYNC = true;
	
	/**
	 * Tells if it should use PARQUET format for SYNC for validated or for published data. Usually these SYNC operations are greatly improved
	 * by using PARQUET format. The data may be shrunk to about 2% of original size. It does not affect SYNC for other types of data. 
	 * If it's set to FALSE, it will use the standard 'JSON content' for every SYNC operation.
	 */
	public static final boolean SYNC_WITH_PARQUET = true;

	/**
	 * If SYNC_WITH_PARQUET is 'true', this parameter indicates if it should create a temporary file with the Parquet contents (if it's set to TRUE).
	 * Otherwise it will keep all the incoming Parquet file contents in memory (if it's set to FALSE).
	 */
	public static final boolean SYNC_PARQUET_USE_TEMPORARY_FILE = true;
	
	/**
	 * Tells if it should change index settings while performing Bulk operations for fine tuning.
	 */
	public static final boolean SYNC_BULK_LOAD_TUNE_SETTINGS = true;
	
	/**
	 * Number of concurrent Bulk Load request per base it may execute (only if BULK_REQUEST_ASYNC is TRUE).
	 */
	public static final int SYNC_BULK_LOAD_CONCURRENCY = 2;

	/**
	 * Level for additional logging related to profiling internal methods for reading Parquet contents. Should be set to 'FINE' or 'FINEST' for production
	 * environment.
	 */
	public static final Level LEVEL_FOR_PROFILING_PARQUET_READ = Level.FINEST;	

	/**
	 * Default paralelism for SYNC operations
	 */
	public static final int DEFAULT_SYNC_PARALELISM = 1;
	
	private static final int VALIDATE_INACTIVITY_INTERVAL_MS = 30000;
	
	private static final int DEFAULT_RETRIES_PER_REQUEST = 3;

	private static final int DEFAULT_KEEP_ALIVE_TIMEOUT_MS = 10_000;
	
	private static final int MAX_POOLING_SIZE_FOR_CONCURRENT_REQUESTS = 20;
	
	private static final int MARGIN_FOR_THRESHOLD_OVER_ZIP_ENTRIES = 1000;
	
    @Autowired
    private MessageSource messages;

	@Autowired
	private ApplicationContext app;

	@Autowired
	private Environment env;

	@Autowired
	private ConfigSyncService configSyncService;

	@Autowired
	private FieldsConventionsService fieldsConventionsService;

	@Autowired
	private FileSystemStorageService fileSystemStorageService;
	
	@Autowired
	private DocumentTemplateRepository templateRepository;
	
	@Autowired
	private SyncCommitMilestoneRepository syncCommitMilestoneRepository;
	
	@Autowired
	private SyncCommitHistoryRepository syncCommitHistoryRepository;

	@Autowired
	private RestHighLevelClient elasticsearchClient;
	
	@Autowired
	private Collection<Repository<?, ?>> allRepositories;

	@Value("${spring.elasticsearch.rest.connection-timeout}")
	private String elasticSearchConnectionTimeout;

	@Value("${storage.parquet.files.temporary.dir}")
	private String storageParquetFilesTemporaryDirName;

    private RestTemplate restTemplate;
    
    /**
     * Keeps reference to 'Future' object created when we scheduled the SYNC task
     */
    private ScheduledFuture<?> futureSyncScheduled;
    
    private PoolingHttpClientConnectionManager poolingConnectionManager;

	/**
	 * Keeps reference for thread running a SYNC process started locally (at subscriber server)
	 */
	private static final AtomicReference<WeakReference<SyncThread>> runningSyncThread = new AtomicReference<>();
	
	@Autowired
	public SyncAPIService(RestTemplateBuilder builder) {
		// For improved workload in concurrent HTTP requests we will configure a pooling collection manager
		// for dealing with our high concurrent environment
		this.restTemplate = builder
				.requestFactory(()->{
					HttpClientConnectionManager mgr = getPoolingConnectionManager();
					HttpClient client = HttpClientBuilder.create()
							.setConnectionManager(mgr)
							.setKeepAliveStrategy(new KeepAliveStrategy(DEFAULT_KEEP_ALIVE_TIMEOUT_MS))
							.setRetryHandler(new DefaultHttpRequestRetryHandler(
									/*retryCount*/DEFAULT_RETRIES_PER_REQUEST, 
									/*requestSentRetryEnabled*/false))
							.build();
					HttpComponentsClientHttpRequestFactory clientFactory 
						= new HttpComponentsClientHttpRequestFactory(client);
					return clientFactory;
				})
				.setConnectTimeout(Duration.ofMinutes(5))
				.setReadTimeout(Duration.ofMinutes(5))
				.build();
	}
	
	private PoolingHttpClientConnectionManager getPoolingConnectionManager() {
		if (poolingConnectionManager!=null)
			return poolingConnectionManager;
		synchronized (this) {
			if (poolingConnectionManager!=null)
				return poolingConnectionManager;
			
			PoolingHttpClientConnectionManager poolingConnManager = new PoolingHttpClientConnectionManager();
			poolingConnManager.setMaxTotal(MAX_POOLING_SIZE_FOR_CONCURRENT_REQUESTS);
			poolingConnManager.setDefaultMaxPerRoute(MAX_POOLING_SIZE_FOR_CONCURRENT_REQUESTS);
			poolingConnManager.setValidateAfterInactivity(VALIDATE_INACTIVITY_INTERVAL_MS);

			poolingConnectionManager = poolingConnManager;
			return poolingConnManager;
		}
	}

	/**
	 * Call all SYNC methods for all objects.
	 * @param tokenApi Token API registered at master for SYNC requests
	 * @param resumeFromLastSync Indicates if we should care about the last SYNC milestone to avoid requesting the same data again
	 */
	@Transactional
	public void syncAll(String tokenApi, boolean resumeFromLastSync) {
		
		syncSome(tokenApi, resumeFromLastSync, Collections.singletonList(SyncContexts.ALL_ENDPOINTS));
		
	}

	/**
	 * Call SYNC methods relative some some endpoints.
	 * @param tokenApi Token API registered at master for SYNC requests
	 * @param resumeFromLastSync Indicates if we should care about the last SYNC milestone to avoid requesting the same data again
	 * @param endpoints List of endpoints we will try to resolve for objects
	 */
	@Transactional
	public void syncSome(String tokenApi, boolean resumeFromLastSync, List<String> endpoints) {
		
		Optional<Long> end = Optional.of(System.currentTimeMillis());

		final int parallelism = Integer.parseInt(env.getProperty("sync.consumer.threads", String.valueOf(DEFAULT_SYNC_PARALELISM)));
		ExecutorService executor = (parallelism<=1) ? null : Executors.newFixedThreadPool(parallelism);

		final LongAdder bytesReceived = new LongAdder(); 
		final long startTime = System.currentTimeMillis();

		// SYNC original files
		// -------------------------------------------------------------------------------

		try {
			if (SyncContexts.hasContext(endpoints, SyncContexts.ORIGINAL_FILES)) {
				Runnable runSyncOriginalFiles = ()->
					syncOriginalFiles(tokenApi, (resumeFromLastSync)?getStartForNextSync(SyncContexts.ORIGINAL_FILES.getEndpoint(),0L):0L, 
							end, bytesReceived);
				if (executor==null)
					runSyncOriginalFiles.run();
				else
					executor.submit(runSyncOriginalFiles);
			}
		}
		catch (Exception ex) {
			log.log(Level.SEVERE, "Error while performing SYNC for original files", ex);
		}
		
		// SYNC bases (data kept in repositories, except documents)
		// -------------------------------------------------------------------------------
		List<Class<?>> allSyncRepos = getAllSyncRepositories();
		for (Class<?> repository_class: allSyncRepos) {
			if (SyncContexts.hasContext(endpoints, SyncContexts.REPOSITORY_ENTITIES, repository_class.getSimpleName())) {
				Runnable runSyncBase = ()->{
					try {
						syncBase(tokenApi, repository_class, (resumeFromLastSync)?getStartForNextSync(SyncContexts.REPOSITORY_ENTITIES.getEndpoint(repository_class.getSimpleName()),0L):0L, 
									end, bytesReceived);
					}
					catch (Exception ex) {
						log.log(Level.SEVERE, "Error while performing SYNC for "+repository_class.getSimpleName(), ex);
					}
				};
				if (executor==null)
					runSyncBase.run();
				else
					executor.submit(runSyncBase);
			}
		}
				
		// Before we continue, we must wait for completion of previous SYNC operations, because we need an updated list of templates
		if (executor!=null) {
			awaitTerminationAfterShutdown(executor);
			executor = Executors.newFixedThreadPool(parallelism); // we need a new object after shutdown
		}

		// SYNC validated data for all templates and versions
		// -------------------------------------------------------------------------------
		List<DocumentTemplate> templates;
		try {
			templates = StreamSupport.stream(templateRepository.findAll().spliterator(), false)
					.sorted(Comparator.comparing(DocumentTemplate::getName).thenComparing(DocumentTemplate::getVersion))
					.collect(Collectors.toList());
		} catch (Exception ex) {
			if (!ErrorUtils.isErrorNoIndexFound(ex)) 
				throw ex;
			else
				templates = Collections.emptyList();
		}
		
		Set<String> archetypesNames = Collections.synchronizedSet(new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
		for (DocumentTemplate template: templates) {
			if (SyncContexts.hasContext(endpoints, SyncContexts.VALIDATED_DATA, template.getName()+"/"+template.getVersion())) {
				Runnable syncValidated = ()->{
					try {
						syncValidatedData(tokenApi, template, (resumeFromLastSync)?getStartForNextSync(SyncContexts.VALIDATED_DATA.getEndpoint(template.getName(),template.getVersion()),0L):0L, 
									end, bytesReceived);
						if (template.getArchetype()!=null && template.getArchetype().trim().length()>0)
							archetypesNames.add(template.getArchetype());
					}
					catch (Exception ex) {
						log.log(Level.SEVERE, "Error while performing SYNC for parsed docs of template "+template.getName()+"/"+template.getVersion(), ex);
					}
				};
				if (executor==null)
					syncValidated.run();
				else
					executor.submit(syncValidated);
			}
		}

		// Before we continue, we must wait for completion of previous SYNC operations, because we need an updated list of archetypes names
		if (executor!=null) {
			awaitTerminationAfterShutdown(executor);
			executor = Executors.newFixedThreadPool(parallelism); // we need a new object after shutdown
		}

		final List<String> indicesForPublishedData;
		
		if (!archetypesNames.isEmpty()) {
			TemplateArchetype[] archetypes = archetypesNames.stream()
					.map(TemplateArchetypes::getArchetype)
					.filter(Optional::isPresent)
					.map(Optional::get)
					.toArray(TemplateArchetype[]::new);
			Set<String> publishedIndices = TemplateArchetypes.getRelatedPublishedDataIndices(archetypes);
			if (publishedIndices!=null && !publishedIndices.isEmpty()) {
				indicesForPublishedData = new ArrayList<>(publishedIndices);
			}
			else {
				indicesForPublishedData = Collections.emptyList();
			}
		}
		else {
			indicesForPublishedData = Collections.emptyList();
		}

		// SYNC published data for all indices produced by ETL phases
		// -------------------------------------------------------------------------------
		for (String indexname: indicesForPublishedData) {
			if (SyncContexts.hasContext(endpoints, SyncContexts.PUBLISHED_DATA, indexname)) {
				Runnable syncPublished = ()->{
					try {
						syncPublishedData(tokenApi, indexname, (resumeFromLastSync)?getStartForNextSync(SyncContexts.PUBLISHED_DATA.getEndpoint(indexname),0L):0L, 
									end, bytesReceived);
					}
					catch (Exception ex) {
						log.log(Level.SEVERE, "Error while performing SYNC for published data "+indexname, ex);
					}
				};
				if (executor==null)
					syncPublished.run();
				else
					executor.submit(syncPublished);
			}
		}

		try {
			// SYNC Kibana assets
			// -------------------------------------------------------------------------------
			if (SyncContexts.hasContext(endpoints, SyncContexts.KIBANA_ASSETS))
				syncKibana(tokenApi, (resumeFromLastSync)?getStartForNextSync(SyncContexts.KIBANA_ASSETS.getEndpoint(),0L):0L, 
						end, bytesReceived);
		}
		catch (Exception ex) {
			log.log(Level.SEVERE, "Error while performing SYNC for Kibana assets", ex);
		}

		// Before we finish, we must wait for completion of previous SYNC operations
		if (executor!=null) {
			awaitTerminationAfterShutdown(executor);
		}
		
		if (log.isLoggable(Level.INFO)) {
			final long endTime = System.currentTimeMillis();
			final long elapsedTime = endTime - startTime;
			log.log(Level.INFO, "Total bytes received after all SYNC operations: "+bytesReceived.longValue()+"  Elapsed time (ms): "+elapsedTime);
		}
	}

	/**
	 * Retrieves and saves locally all original files created or changed between a time interval
	 */
	@Transactional
	public void syncOriginalFiles(String tokenApi, long start, Optional<Long> end, LongAdder bytesReceived) {
		
		final File originalFilesDir;
		try {
			originalFilesDir = fileSystemStorageService.getRootLocation().toFile();
			if (!originalFilesDir.exists())
				originalFilesDir.mkdirs();
			if (!originalFilesDir.exists())
				throw new FileNotFoundException(originalFilesDir.getAbsolutePath());
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Error while synchronizing with "+SyncContexts.ORIGINAL_FILES.getEndpoint(), ex);
			throw new GeneralException(messages.getMessage("error.failed.sync", null, LocaleContextHolder.getLocale()));
		}
		
		// Expects to find files with this name pattern inside ZIP
		// Let's reject anything else for safety (e.g. 'man in the middle attack' or compromised master would serve malicious files) 
		final Pattern patternNameEntry = Pattern.compile("^(?>original)?[\\/]?(\\d{4})[\\/](\\d{1,2})[\\/]([\\d+\\-A-Za-z]+)$");

		LongAdder counter = new LongAdder();
		LongAdder sumBytes = new LongAdder();
		final long timestamp = System.currentTimeMillis();
		
		syncSomething(tokenApi, SyncContexts.ORIGINAL_FILES.getEndpoint(), start, end, /*limit*/-1, bytesReceived,
		/*parquet*/false,
		/*consumer*/(entry,input)->{
			String entryName = entry.getName();
			Matcher matcherNameEntry = patternNameEntry.matcher(entryName);
			if (!matcherNameEntry.find()) {
				log.log(Level.INFO, "Ignoring entry with unrecognizable name ("+entryName+") received from "+SyncContexts.ORIGINAL_FILES.getEndpoint());
				return;
			}
			File yearDir = new File(originalFilesDir, matcherNameEntry.group(1));
			File monthDir = new File(yearDir, matcherNameEntry.group(2));
			File targetFile = new File(monthDir, matcherNameEntry.group(3));
			if (!targetFile.getParentFile().exists())
				targetFile.getParentFile().mkdirs();			
			Files.copy(input, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			if (entry.getLastModifiedTime()!=null) {
				boolean modified = targetFile.setLastModified(entry.getLastModifiedTime().toMillis());
				if (!modified) {
					log.log(Level.FINEST, "Could not change timestamp for ("+entryName+") received from "+SyncContexts.ORIGINAL_FILES.getEndpoint());					
				}
			}
			counter.increment();
			sumBytes.add(targetFile.length());
		});

		final long elapsed_time = System.currentTimeMillis() - timestamp;
		log.log(Level.INFO, FINISHED_COPYING+counter.longValue()+FILES_FROM+SyncContexts.ORIGINAL_FILES.getEndpoint()+" in "+elapsed_time+" ms and "+sumBytes.longValue()+BYTES);
	}
	
	/**
	 * Retrieves and saves locally all entity instances created or changed between a time interval
	 */
	@Transactional
	public void syncBase(String tokenApi, Class<?> repositoryClass, long start, Optional<Long> end, LongAdder bytesReceived) {
		
		final Class<?> entity = ReflectUtils.getParameterType(repositoryClass);
		if (entity==null) {
			throw new GeneralException("Could not find entity class related to '"+repositoryClass+"'!");
		}
		
		@SuppressWarnings({ "unchecked" })
		CrudRepository<Object, ?> repository = (CrudRepository<Object, ?>)app.getBean(repositoryClass);

		final ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.registerModule(new JavaTimeModule());

		final long timestamp = System.currentTimeMillis();
		final String uri = SyncContexts.REPOSITORY_ENTITIES.getEndpoint(repositoryClass.getSimpleName());
		
		final BatchSave batchToSave = new BatchSave(BULK_LOAD_BATCH_COMMIT, entity.getSimpleName(), repository);
		
		final Class<?> foundInterface = ReflectUtils.getInterfaceWithFilter(repository.getClass(), 
				/*filter*/cl->cl.getAnnotation(Synchronizable.class)!=null);
		final Synchronizable syncAnon = (foundInterface==null) ? null : foundInterface.getAnnotation(Synchronizable.class);
		final String[] uniqueConstraint = (syncAnon==null) ? null : syncAnon.uniqueConstraint();	
		
		// If we have defined unique constraints for this synchronizable entity, we should re-map the incoming 'id's
		// to the corresponding existent instances in order to prevent ambiguity (i.e.: different records with different
		// id's but with the same values for the unique constraint).
		final Map<Constraints, String> mapExistingIds = (uniqueConstraint==null || uniqueConstraint.length==0) ? null
				: getMapOfConstraintsAndIds(entity, uniqueConstraint);	
		final PropertyUtilsBean props = (mapExistingIds==null || mapExistingIds.isEmpty()) ? null : new PropertyUtilsBean();

		long bytesReceivedHere =
		syncSomething(tokenApi, uri, start, end, /*limit*/MAX_RESULTS_PER_REQUEST, bytesReceived,
		/*parquet*/false,
		/*consumer*/(entry,input)->{
			
			// Let's deserialize the JSON contents
			Object instance = mapper.readValue(input, entity);

			if (mapExistingIds!=null && !mapExistingIds.isEmpty()) {
				Constraints constraints = new Constraints(uniqueConstraint, instance, props);
				String existentId = mapExistingIds.get(constraints);
				if (existentId!=null) {
					String incomingId;
					try {
						incomingId = (String)props.getSimpleProperty(instance, "id");
					} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e1) {
						incomingId = null;
					}
					if (!existentId.equals(incomingId)) {
						// Incoming instance has a different ID of an existent record with the same values for the unique constraints
						// Let's keep the same ID
						try {
							props.setSimpleProperty(instance, "id", existentId);
						} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
							log.log(Level.WARNING, "Could not save locally the instance '"+entity.getName()+"' of ID '"+incomingId
									+"' because it violates the unicity constraint "+constraints+" for an existent instance of ID '"+existentId+"'");
							return;
						}
					}
				}				
			}
			
			// Add instance to batch (may also save if batch hits configured size)
			batchToSave.push(instance);
			
		});
		
		// Saves data that remained at batch
		batchToSave.saveBatch();
		
		final long elapsed_time = System.currentTimeMillis() - timestamp;
		log.log(Level.INFO, FINISHED_COPYING+batchToSave.counter.longValue()+FILES_FROM+uri+" in "+elapsed_time+" ms and "+bytesReceivedHere+BYTES);
		if (batchToSave.countErrors.longValue()>0)
			log.log(Level.INFO, "Number of errors while copying "+batchToSave.counter.longValue()+FILES_FROM+uri+": "+batchToSave.countErrors.longValue());
	}
	
	/**
	 * Utility object used for storing batch of elements we want to save
	 * @author Gustavo Figueiredo
	 *
	 */
	private static class BatchSave {
		private final int batchSize;
		private final List<Object> batchToSave;
		private final CrudRepository<Object, ?> repository;
		private final LongAdder countErrors;
		private final LongAdder counter;
		private final String simpleName;
		BatchSave(int batchSize,
				String simpleName,
				CrudRepository<Object, ?> repository) {
			this.batchSize = batchSize;
			this.simpleName = simpleName;
			this.repository = repository;
			batchToSave = new ArrayList<>(batchSize);
			countErrors = new LongAdder();
			counter = new LongAdder();
		}
		void push(Object instance) {
			batchToSave.add(instance);
			if (batchToSave.size()>=batchSize) {
				saveBatch();
			}
		}
		void saveBatch() {
			if (batchToSave.isEmpty())
				return;
			// Let's save this information in repository (either JPA or ElasticSearch)
			try {
				repository.saveAll(batchToSave);
				counter.add(batchToSave.size());
			}
			catch (Exception ex) {
				// In case of error, let's try saving one at a time
				if (batchToSave.size()>1) {
					for (Object record: batchToSave) {
						try {
							repository.save(record);
							counter.increment();
						}
						catch (Throwable ex2) {
							log.log(Level.WARNING, "Error while saving "+simpleName+" with contents "+getSampleInfoForLog(record), ex);
							countErrors.increment();
							return;							
						}
					}
				}
				else {
					log.log(Level.WARNING, "Error while saving "+simpleName+" with contents "+getSampleInfoForLog(batchToSave), ex);
					countErrors.increment();
					return;
				}
			}
			batchToSave.clear();
		}
		static String getSampleInfoForLog(Object target) {
			if ((target instanceof Collection) && ((Collection<?>)target).isEmpty())
				return "<none>";
			if (target==null)
				return "<none>";
			if (target instanceof Collection) {
				Object instance = ((Collection<?>)target).iterator().next();
				ObjectMapper mOutJson = new ObjectMapper();
				mOutJson.registerModule(new JavaTimeModule());
				mOutJson.setSerializationInclusion(Include.NON_NULL);
				String info;
				try {
					info = mOutJson.writeValueAsString(instance);
				} catch (JsonProcessingException e) {
					return "<no information available>";
				}
				if (((Collection<?>)target).size()>1) {
					info += " and "+(((Collection<?>)target).size()-1)+" others";
				}
				return info;
			}
			else {
				ObjectMapper mOutJson = new ObjectMapper();
				mOutJson.setSerializationInclusion(Include.NON_NULL);
				mOutJson.registerModule(new JavaTimeModule());
				try {
					return mOutJson.writeValueAsString(target);
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
	public void syncValidatedData(String tokenApi, DocumentTemplate template, long start, Optional<Long> end, LongAdder bytesReceived) {
		
    	if (template==null) {
    		throw new InvalidParameter("template");
    	}
    	
		final String index_name = IndexNamesUtils.formatIndexNameForValidatedData(template);

        // Creates the index, in case it has not been created yet
        try {
        	boolean hasIndice;
        	try {
        		MappingUtils.hasMappings(elasticsearchClient, index_name);
        		hasIndice = true; // the index may exist and have no mapping
        	}
        	catch (Exception ex) {
        		if (ErrorUtils.isErrorNoIndexFound(ex))
        			hasIndice = false;
        		else
        			throw ex;
        	}
	        if (!hasIndice) {
		        try {
			        ESUtils.createIndex(elasticsearchClient, index_name, /*ignore_malformed*/true);
		        }
		        catch (Exception ex) {
		        	log.log(Level.WARNING, "Ignoring error while creating new index '"+index_name+"' for template '"+template.getName()+"'", ex);
		        }
	        }
        }
        catch (Exception ex) {
        	log.log(Level.WARNING, "Ignoring error while checking existence of index '"+index_name+"' for template '"+template.getName()+"'", ex);
        }

		final ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.registerModule(new JavaTimeModule());

		LongAdder counter = new LongAdder();
		final long timestamp = System.currentTimeMillis();
		final String uri = SyncContexts.VALIDATED_DATA.getEndpoint(template.getName(), template.getVersion());
		
		final Supplier<BulkRequest> bulkRequestFactory = ()->{
			BulkRequest r = new BulkRequest();
			if (elasticSearchConnectionTimeout!=null
					&& elasticSearchConnectionTimeout.trim().length()>0)
					r.timeout(elasticSearchConnectionTimeout);
			return r;
		};
		
		final AtomicReference<BulkRequest> currentBulkRequest = new AtomicReference<>(bulkRequestFactory.get());
		
		final LongAdder recordsForCommit = new LongAdder();
		
		final BulkLoadAsyncCriticalSection semaphoreForAsyncBulkLoad = (BULK_REQUEST_ASYNC) ? new BulkLoadAsyncCriticalSection(SYNC_BULK_LOAD_CONCURRENCY) : null;
		
		if (SYNC_BULK_LOAD_TUNE_SETTINGS) {
			try {
				ESUtils.changeIndexSettingsForFasterBulkLoad(elasticsearchClient, index_name);
			}
			catch (Exception ex) {
				if (!ErrorUtils.isErrorNoIndexFound(ex) && !ErrorUtils.isErrorNotFound(ex)) {
					log.log(Level.WARNING, "Error while changing index settings for faster Bulk Loads at index "+index_name, ex);
				}
			}
		}
		
		try {

			long bytesReceivedHere =
			syncSomething(tokenApi, uri, start, end, /*limit*/MAX_RESULTS_PER_REQUEST, bytesReceived,
			/*parquet*/SYNC_WITH_PARQUET,
			/*consumer*/new ConsumeSyncContents(parsedContents->{
				
				String id = (String)parsedContents.get("id");
				if (id==null)
					return;
				
				if (semaphoreForAsyncBulkLoad!=null && semaphoreForAsyncBulkLoad.isRunningBulkLoadAtMaxLoad()) {
					// If we are running Bulk Load asynchronously, wait until the previous one has finished
					semaphoreForAsyncBulkLoad.waitAnyBulkLoad();
				}
				
				BulkRequest bulkRequest = currentBulkRequest.get();
				bulkRequest.add(new IndexRequest(index_name)
						.id(id)
						.source(parsedContents));
	
				recordsForCommit.increment();
				
				if (recordsForCommit.longValue()>BULK_LOAD_BATCH_COMMIT) {
					if (BULK_REQUEST_ASYNC) {
						commitBulkRequestAsync(bulkRequest, index_name, semaphoreForAsyncBulkLoad);
						if (SYNC_BULK_LOAD_CONCURRENCY>1) {
							// if we may have more than one concurrent bulk load operations, we need to create
							// a new BulkRequest for each, so that they don't interfere with each other
							currentBulkRequest.set(bulkRequestFactory.get());
						}
					}
					else {
						commitBulkRequest(bulkRequest, index_name);
					}
					recordsForCommit.reset();
				}
	
				counter.increment();
			}));
	
			if (BULK_REQUEST_ASYNC) {
				if (semaphoreForAsyncBulkLoad!=null && semaphoreForAsyncBulkLoad.isRunningBulkLoadAtMaxLoad()) {
					// If we are running Bulk Load asynchronously, wait until the previous one has finished
					semaphoreForAsyncBulkLoad.waitAllBulkLoad();
				}
			}
			commitBulkRequest(currentBulkRequest.get(), index_name);

			final long elapsed_time = System.currentTimeMillis() - timestamp;
			log.log(Level.INFO, FINISHED_COPYING+counter.longValue()+FILES_FROM+uri+" in "+elapsed_time+" ms and "+bytesReceivedHere+BYTES);
		}
		finally {
			
			if (SYNC_BULK_LOAD_TUNE_SETTINGS) {
				try {
					ESUtils.changeIndexSettingsForDefaultBulkLoad(elasticsearchClient, index_name);
				}
				catch (Exception ex) {
					if (!ErrorUtils.isErrorNoIndexFound(ex) && !ErrorUtils.isErrorNotFound(ex)) {
						log.log(Level.WARNING, "Error while changing index settings for default Bulk Loads at index "+index_name, ex);
					}
				}
			}

		}

	}

	/**
	 * Retrieves and saves locally all published (denormalized) data created or changed between a time interval
	 */
	@Transactional
	public void syncPublishedData(String tokenApi, String indexname, long start, Optional<Long> end, LongAdder bytesReceived) {
		
    	if (indexname==null || indexname.trim().length()==0 || !indexname.startsWith(IndexNamesUtils.PUBLISHED_DATA_INDEX_PREFIX)) {
    		throw new InvalidParameter("indexname");
    	}
    	
        // Creates the index, in case it has not been created yet
        try {
        	boolean hasIndice;
        	try {
        		MappingUtils.hasMappings(elasticsearchClient, indexname);
        		hasIndice = true; // the index may exist and have no mapping
        	}
        	catch (Exception ex) {
        		if (ErrorUtils.isErrorNoIndexFound(ex))
        			hasIndice = false;
        		else
        			throw ex;
        	}
	        if (!hasIndice) {
		        try {
			        ESUtils.createIndex(elasticsearchClient, indexname, /*ignore_malformed*/true);
		        }
		        catch (Exception ex) {
		        	log.log(Level.WARNING, "Ignoring error while creating new index '"+indexname+"' ", ex);
		        }
	        }
        }
        catch (Exception ex) {
        	log.log(Level.WARNING, "Ignoring error while checking existence of index '"+indexname+"' ", ex);
        }

		LongAdder counter = new LongAdder();
		final long timestamp = System.currentTimeMillis();
		final String uri = SyncContexts.PUBLISHED_DATA.getEndpoint(indexname);

		final Supplier<BulkRequest> bulkRequestFactory = ()->{
			BulkRequest r = new BulkRequest();
			if (elasticSearchConnectionTimeout!=null
					&& elasticSearchConnectionTimeout.trim().length()>0)
					r.timeout(elasticSearchConnectionTimeout);
			return r;
		};

		final AtomicReference<BulkRequest> currentBulkRequest = new AtomicReference<>(bulkRequestFactory.get());

		final LongAdder recordsForCommit = new LongAdder();

		final BulkLoadAsyncCriticalSection semaphoreForAsyncBulkLoad = (BULK_REQUEST_ASYNC) ? new BulkLoadAsyncCriticalSection(SYNC_BULK_LOAD_CONCURRENCY) : null;

		if (SYNC_BULK_LOAD_TUNE_SETTINGS) {
			try {
				ESUtils.changeIndexSettingsForFasterBulkLoad(elasticsearchClient, indexname);
			}
			catch (Exception ex) {
				if (!ErrorUtils.isErrorNoIndexFound(ex) && !ErrorUtils.isErrorNotFound(ex)) {
					log.log(Level.WARNING, "Error while changing index settings for faster Bulk Loads at index "+indexname, ex);
				}
			}
		}
		
		try {

			long bytesReceivedHere =
			syncSomething(tokenApi, uri, start, end, /*limit*/MAX_RESULTS_PER_REQUEST, bytesReceived,
			/*parquet*/SYNC_WITH_PARQUET,
			/*consumer*/new ConsumeSyncContents(parsedContents->{
				
				// We will ignore the entry name because it's supposed to be equal to the entity 'ID', which we
				// also have inside de JSON contents
	
				String id = (String)parsedContents.get("id");
				if (id==null)
					return;
				
				if (semaphoreForAsyncBulkLoad!=null && semaphoreForAsyncBulkLoad.isRunningBulkLoadAtMaxLoad()) {
					// If we are running Bulk Load asynchronously, wait until the previous one has finished
					semaphoreForAsyncBulkLoad.waitAnyBulkLoad();
				}
	
				BulkRequest bulkRequest = currentBulkRequest.get();
				bulkRequest.add(new IndexRequest(indexname)
						.id(id)
						.source(parsedContents));
				
				recordsForCommit.increment();
				
				if (recordsForCommit.longValue()>BULK_LOAD_BATCH_COMMIT) {
					if (BULK_REQUEST_ASYNC) {
						commitBulkRequestAsync(bulkRequest, indexname, semaphoreForAsyncBulkLoad);
						if (SYNC_BULK_LOAD_CONCURRENCY>1) {
							// if we may have more than one concurrent bulk load operations, we need to create
							// a new BulkRequest for each, so that they don't interfere with each other
							currentBulkRequest.set(bulkRequestFactory.get());
						}
					}
					else {
						commitBulkRequest(bulkRequest, indexname);
					}
					recordsForCommit.reset();
				}
	
				counter.increment();
			}));
			
			if (BULK_REQUEST_ASYNC) {
				if (semaphoreForAsyncBulkLoad!=null && semaphoreForAsyncBulkLoad.isRunningBulkLoadAtMaxLoad()) {
					// If we are running Bulk Load asynchronously, wait until the previous one has finished
					semaphoreForAsyncBulkLoad.waitAllBulkLoad();
				}
			}
			commitBulkRequest(currentBulkRequest.get(), indexname);
			
			final long elapsed_time = System.currentTimeMillis() - timestamp;
			log.log(Level.INFO, FINISHED_COPYING+counter.longValue()+FILES_FROM+uri+" in "+elapsed_time+" ms and "+bytesReceivedHere+BYTES);
			
		}
		finally {
			
			if (SYNC_BULK_LOAD_TUNE_SETTINGS) {
				try {
					ESUtils.changeIndexSettingsForDefaultBulkLoad(elasticsearchClient, indexname);
				}
				catch (Exception ex) {
					if (!ErrorUtils.isErrorNoIndexFound(ex) && !ErrorUtils.isErrorNotFound(ex)) {
						log.log(Level.WARNING, "Error while changing index settings for default Bulk Loads at index "+indexname, ex);
					}
				}
			}

		}
	}

	/**
	 * Retrieves and saves locally all Kibana assets created or changed between a time interval
	 */
	@Transactional
	public void syncKibana(String tokenApi, long start, Optional<Long> end, LongAdder bytesReceived) {
		
		final ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.registerModule(new JavaTimeModule());

		LongAdder counter = new LongAdder();
		final long timestamp = System.currentTimeMillis();
		final String uri = SyncContexts.KIBANA_ASSETS.getEndpoint();

		syncSomething(tokenApi, uri, start, end, /*limit*/-1, bytesReceived,
		/*parquet*/false,
		/*consumer*/(entry,input)->{
			
			// The entry name includes the index used for Kibana. We need to keep it. The rest of the entry name
			// is the 'id' converted to something compatible with file syntax. We can't use it because it has been
			// converted.			
			String entryName = entry.getName();
			int sep = entryName.indexOf('/');
			if (sep<=0)
				sep = entryName.indexOf('\\');
			if (sep<=0)
				return; // not expected
			
			String indexName = entryName.substring(0, sep);
			if (!indexName.startsWith(".kibana"))
				return; // not expected
			if (SyncAPIController.KIBANA_IGNORE_PATTERNS.matcher(indexName).find())
				return; // not expected

			// Let's deserialize the JSON contents
			@SuppressWarnings("unchecked")
			Map<String, Object> parsedContents = mapper.readValue(input, Map.class);
			
			String id = (String)parsedContents.remove("id");
			if (id==null)
				return;
			
			for (String fields_should_be_long: new String[] {
			"canvas-workpad-template.template.pages.elements.position.top",
			"canvas-workpad-template.template.pages.elements.position.left",
			"canvas-workpad-template.template.pages.elements.position.width",
			"canvas-workpad-template.template.pages.elements.position.height"
			}) {
				recursiveSet(parsedContents, fields_should_be_long, new ConvertToDouble());
			}
			
			ESUtils.indexWithRetry(elasticsearchClient,
					new IndexRequest(indexName)
					.id(id)
					.source(parsedContents)
					.setRefreshPolicy(RefreshPolicy.IMMEDIATE));

			counter.increment();
		});
		
		final long elapsed_time = System.currentTimeMillis() - timestamp;
		log.log(Level.INFO, FINISHED_COPYING+counter.longValue()+FILES_FROM+uri+" in "+elapsed_time+" ms");
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
	
	private static void recursiveSet(Map<String,Object> parsedContents, String path, Function<Object,Object> changer) {
		int sep = path.indexOf('.');
		if (sep>0) {
			String seekNestedObj = path.substring(0, sep);
			String remainingPath = path.substring(sep+1);
			Object nestedObj = parsedContents.get(seekNestedObj);
			if (nestedObj instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String,Object> nestedMap = (Map<String,Object>)nestedObj;
				recursiveSet(nestedMap, remainingPath, changer);
			}
			else if (nestedObj instanceof List) {
				List<?> list = (List<?>)nestedObj;
				for (Object el:list) {
					if (el instanceof Map) {
						@SuppressWarnings("unchecked")
						Map<String,Object> nestedMap = (Map<String,Object>)el;
						recursiveSet(nestedMap, remainingPath, changer);						
					}
				}
			}
			return;
		}
		else {
			Object value = parsedContents.get(path);
			if (value!=null) {
				Object newValue = changer.apply(value);
				if (newValue!=value) {
					parsedContents.put(path, newValue);
				}
			}
		}
	}

	/**
	 * Process the SYNC request. Automatically resume partial responses. Returns the number of bytes received (considering compression in transfer).
	 */
	private long syncSomething(String tokenApi, String endPoint, long start, Optional<Long> end, int limit, LongAdder bytesReceived,
			boolean parquet,
			ConsumeSyncContentsInterface consumer) {
		
		ConfigSync config = configSyncService.getActiveConfig();
		if (config==null || config.getMaster()==null || config.getMaster().trim().length()==0) 
			throw new GeneralException(messages.getMessage("error.missing.config_sync", null, LocaleContextHolder.getLocale()));
		
		RequestCallback requestCallback = req->{
			req.getHeaders().set(HttpHeaders.AUTHORIZATION, "api-key "+tokenApi);
			req.getHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
		};
		
		final String master = config.getMaster();
		String uri = "https://"+master+endPoint+"?start="+start;
		if (end.isPresent())
			uri +="&end="+end.get();
		if (limit>0)
			uri += "&limit="+limit;
		if (parquet)
			uri += "&format=parquet";
		
		boolean hasMore = false; // will be set to TRUE if there is more data to SYNC for
		
		log.log(Level.INFO, "SYNC started for endpoint '"+endPoint+"'");
		
		OffsetDateTime lastTimeStart = (start==0) ? null : new Date(start).toInstant().atOffset(ZoneOffset.UTC);

		final LongAdder countIncomingObjectsOverall = new LongAdder();
		final LongAdder countBytesOverall = new LongAdder();
		
		final int thresholdEntries = ( (limit>0) ? limit : MAX_RESULTS_PER_REQUEST ) + MARGIN_FOR_THRESHOLD_OVER_ZIP_ENTRIES;
		
		do {
			
			hasMore = false; // resets the flag if we are repeating SYNC
		
			// Make the HTTP request and load its response by streaming data
			
			AtomicReference<SyncDto> syncInfo = new AtomicReference<>();
			
			final OffsetDateTime lastTimeRun = DateTimeUtils.now();
			
			final LongAdder countIncomingObjects = new LongAdder();
						
			boolean successful = false; // will be set to TRUE after 'restTemplate.execute' completes
			
			try {
				
				ResponseExtractor<Boolean> responseExtractor = 
						
					clientHttpResponse->{
		
					// Got the synchronized contents in a local temporary file. Let's open it and read its contents
		
					InputStream fileInput = new BufferedInputStream(clientHttpResponse.getBody());
					CountingInputStream countingInput = new CountingInputStream(fileInput);
					CheckedInputStream chkInput = new CheckedInputStream(countingInput, new Adler32());
					ZipInputStream zipInput = new ZipInputStream(chkInput);

					try {
						
						ZipConsumer.ConsumeContents consumeZipContents = (ze, input)->{
							// If entry corresponds to SyncDto, read and store this information (there should be only one per ZIP file)
							if (SyncDto.DEFAULT_FILENAME.equals(ze.getName())) {
								syncInfo.set(readSyncDto(zipInput));
								return;
							}
							
							// If entry corresponds to Parquet file, read and store this information accordingly
							if (parquet && SyncAPIController.DATA_PARQUET_FILENAME.equals(ze.getName())) {
								
								readParquetFileContents(zipInput, consumer, countIncomingObjects, countIncomingObjectsOverall);
								return;								
							}
							else {
								
								// Populates the local base with incoming data
								
								consumer.load(ze, new NoClosingInputStream(zipInput));
								countIncomingObjects.increment();
								countIncomingObjectsOverall.increment();
								
							}							
						};
						
						ZipConsumer zipConsumer = new ZipConsumer(zipInput::getNextEntry, zipInput, consumeZipContents)
								.threadholdEntries(thresholdEntries);
						
						zipConsumer.run();

					} catch (IOException ex) {
						if (ErrorUtils.isErrorStreamClosed(ex)) {
							// ignore this error silently
							//ex.printStackTrace();
						}
						else {
							//ex.printStackTrace();
							throw ex;
						}
					} finally {
						bytesReceived.add(countingInput.getCount());
						countBytesOverall.add(countingInput.getCount());
					}
						
					return Boolean.TRUE;
					
				};
				
				// May retry the same request multiple times in case of specific errors
				// For example, Java 11 implementation of TLS 1.3 has a bug that results in occasional failures under high concurrency
				// @see https://bugs.openjdk.java.net/browse/JDK-8213202
				
				for (int retry=0; retry<DEFAULT_RETRIES_PER_REQUEST; retry++) {
					try {
						restTemplate.execute(uri, HttpMethod.GET, requestCallback, responseExtractor);
						break;
					}
					catch (Exception ex) {
						if (retry+1>=DEFAULT_RETRIES_PER_REQUEST || !shouldRetryRequest(ex)) {
							throw ex;
						}
					}
				} // LOOP for each retry of the same HTTP request
						
				successful = true;
				
			}
			finally {

				if (syncInfo.get()!=null && log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, "SYNC info for endpoint '"+endPoint+"': "+syncInfo.get());
				}
	
				if (successful || countIncomingObjects.longValue()>0) {
					try {
						// Update SYNC repository with milestone to avoid replay the same SYNC again
						OffsetDateTime lastTimeEnd = (syncInfo.get()!=null && syncInfo.get().hasMore()) ? 
								syncInfo.get().getActualEnd().toInstant().atOffset(ZoneOffset.UTC) : (end.isPresent()) ? 
										new Date(end.get().longValue()).toInstant().atOffset(ZoneOffset.UTC) : lastTimeRun;
						saveSyncMilestone(master, endPoint, lastTimeRun, lastTimeStart, lastTimeEnd, countIncomingObjects.longValue(), successful);
					}
					catch (Exception ex) {
						log.log(Level.SEVERE, "SYNC COMMIT failed for endpoint '"+endPoint+"'", ex);
					}
				}
				
			}

			// Check if there is more to search for
			if (syncInfo.get()!=null && syncInfo.get().hasMore()) {
				// Will do the next SYNC with the next timestamp (should not be NULL if 'hasMore')
				StringBuilder sb = new StringBuilder("https://"+master+endPoint+"?start="+syncInfo.get().getNextStart().getTime());
				if (syncInfo.get().getNextLineStart()!=null && syncInfo.get().getNextLineStart().longValue()>0)
					sb.append("&line_start="+syncInfo.get().getNextLineStart());
				if (end.isPresent())
					sb.append("&end="+end.get());
				if (limit>0)
					sb.append("&limit="+limit);
				if (parquet)
					sb.append("&format=parquet");
				
				uri = sb.toString();
				
				if (log.isLoggable(Level.INFO))
					log.log(Level.INFO, "URI for resuming partial SYNC: "+uri+" (got "+countIncomingObjectsOverall.longValue()+" objects in "+countBytesOverall.longValue()+" bytes so far)");
				hasMore = true; // will repeat SYNC with remaining data
				lastTimeStart = syncInfo.get().getNextStart().toInstant().atOffset(ZoneOffset.UTC);
			}
			
		} while (hasMore);
		
		return countBytesOverall.longValue();
	}
	
	/**
	 * Save SYNC milestone at database for next call and save SYNC history for logging
	 */
	@Transactional
	public void saveSyncMilestone(String master, String endPoint, OffsetDateTime lastTimeRun, OffsetDateTime lastTimeStart, OffsetDateTime lastTimeEnd,
			long countArrivedObjects, boolean successful) {
		
		if (successful) {
			Optional<SyncCommitMilestone> existentMilestone = syncCommitMilestoneRepository.findByEndPoint(endPoint);
			SyncCommitMilestone milestone = existentMilestone.orElseGet(SyncCommitMilestone::new);
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
		Optional<SyncCommitMilestone> existentMilestone = syncCommitMilestoneRepository.findByEndPoint(endPoint);
		return existentMilestone.map(SyncCommitMilestone::getLastTimeEnd).map(off->off.toInstant().toEpochMilli()).orElse(fallback);
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
		catch (Exception ex) {
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
		catch (Exception ex) {
			if (ErrorUtils.isErrorNoIndexFound(ex))
				return false;
			throw ex;
		}		
	}

	/**
	 * Return the list of repositories for all synchronizable instances
	 */
	public List<Class<?>> getAllSyncRepositories() {
		List<Class<?>> synchronizableRepositories = new LinkedList<>();
		for (Repository<?,?> repo: allRepositories) {
			Class<?> foundInterface = ReflectUtils.getInterfaceWithFilter(repo.getClass(), 
					/*filter*/cl->cl.getAnnotation(Synchronizable.class)!=null);
			if (foundInterface!=null) {
				Synchronizable syncAnon = foundInterface.getAnnotation(Synchronizable.class);
				if (syncAnon==null)
					continue;
				synchronizableRepositories.add(foundInterface);
			}
		}
		return synchronizableRepositories;
	}

	/**
	 * Functional interface using internally for reusing code
	 * @author Gustavo Figueiredo
	 *
	 */
	@FunctionalInterface
	public static interface ConsumeSyncContentsInterface {
		
		/**
		 * Method called for each ZIP entry from incoming synchronized data (NOT applicable
		 * when using PARQUET for transport data format)
		 */
		public void load(ZipEntry entry, InputStream input) throws IOException;
		
		/**
		 * Method called for record from incoming synchronized data (applicable when
		 * using PARQUET for transport data format)
		 */
		public default void load(Map<String,Object> record) throws IOException {
			throw new UnsupportedOperationException();
		}
	}
	
	/**
	 * Functional interface to be used together with 'ConsumeSyncContents'
	 */
	@FunctionalInterface
	public static interface ConsumeRecordsInterface {
		public void load(Map<String,Object> record) throws IOException;
	}
	
	/**
	 * Default implementation of ConsumeSyncContentsInterface supporting incoming data either
	 * in PARQUET file format or not.
	 */
	public static class ConsumeSyncContents implements ConsumeSyncContentsInterface {
		
		private final ObjectMapper mapper;
		
		private final ConsumeRecordsInterface consumer;

		public ConsumeSyncContents(ConsumeRecordsInterface consumer) {
			this.mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			mapper.registerModule(new JavaTimeModule());
			this.consumer = consumer;
		}

		public ConsumeSyncContents(ObjectMapper mapper, ConsumeRecordsInterface consumer) {
			this.mapper = mapper;
			this.consumer = consumer;
		}

		@Override
		public void load(ZipEntry entry, InputStream input) throws IOException {
			
			// We will ignore the entry name because it's supposed to be equal to the entity 'ID', which we
			// also have inside de JSON contents

			// Let's deserialize the JSON contents
			@SuppressWarnings("unchecked")
			Map<String, Object> parsedContents = mapper.readValue(input, Map.class);
			load(parsedContents);
		}

		@Override
		public void load(Map<String, Object> record) throws IOException {
			consumer.load(record);
		}
		
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
			throw new GeneralException(e);
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
				catch (Exception ex) {
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
				catch (Exception ex) {
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

	/**
	 * Performs the Elastic 'bulk request'
	 */
	private void commitBulkRequest(BulkRequest bulkRequest, String indexName) {
		if (bulkRequest.numberOfActions()>0) {
			bulkRequest.setRefreshPolicy(RefreshPolicy.NONE);
			try {
				CommonErrors.doESWriteOpWithRetries(
					()->elasticsearchClient.bulk(bulkRequest,
					RequestOptions.DEFAULT));
			}
			catch (Exception ex) {
				try {
					if (null!=ErrorUtils.getIllegalArgumentTypeMismatch(ex)
							|| null!=ErrorUtils.getIllegalArgumentInputString(ex)) {
						// In case of an error relative to type mismatch, lets try again after changing some of the index parameters
						ESUtils.changeBooleanIndexSetting(elasticsearchClient, indexName, ESUtils.SETTING_IGNORE_MALFORMED, true, /*closeAndReopenIndex*/true);
						CommonErrors.doESWriteOpWithRetries(
							()->elasticsearchClient.bulk(bulkRequest,
							RequestOptions.DEFAULT));
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

	/**
	 * Performs the Elastic 'bulk request' asynchronously
	 */
	private void commitBulkRequestAsync(BulkRequest bulkRequest, String indexName, BulkLoadAsyncCriticalSection semaphore) {
		if (bulkRequest.numberOfActions()>0) {
			semaphore.startedBulkLoad();
			bulkRequest.setRefreshPolicy(RefreshPolicy.NONE);
			new Thread("AsyncBulkLoad") {
				@Override
				public void run() {
					try {
						commitBulkRequest(bulkRequest, indexName);
					}
					finally {
						bulkRequest.requests().clear();
						semaphore.finishedBulkLoad();						
					}
				}
			}.start();
		}
	}
	
	/**
	 * Given a persistent entity (expects to find the 'Document' annotation on it) and given a set of field names
	 * to be considered as 'unique constraints', returns a map with the values collected from existent data. For
	 * each tuple of values this map gives the corresponding instance ID.
	 */
	private Map<Constraints, String> getMapOfConstraintsAndIds(Class<?> entity, final String[] uniqueConstraint) {
		Document anDoc = entity.getAnnotation(Document.class);
		String indexName = anDoc.indexName();
		
		Map<Constraints, String> map = new HashMap<>();
		
    	try (@SuppressWarnings({ "rawtypes", "unchecked" })
		Stream<Map<?,?>> stream = (Stream)ScrollUtils.findWithScroll(Map.class, indexName, elasticsearchClient, 
    		/*customizeSearch*/searchSourceBuilder->{
    	    	for (String name: uniqueConstraint)
    	    		searchSourceBuilder.fetchField(name);    			
    		});) {
    		
    		stream.forEach(instance->{
    			
    			String id = (String)instance.get("id");
    			
    			map.put(new Constraints(uniqueConstraint, instance), id);
    			
    		});
    		
    	}
    	
    	return map;
	}
	
	/**
	 * Object used for controlling a critical section related to Bulk Load (only used if configured for asynchronous Bulk Load)
	 */
	private static class BulkLoadAsyncCriticalSection {
		private final AtomicInteger runningBulkLoad;
		private final int concurrent;
		BulkLoadAsyncCriticalSection(int concurrent) {
			runningBulkLoad = new AtomicInteger(0);
			this.concurrent = concurrent;
		}
		public synchronized void startedBulkLoad() {
			runningBulkLoad.incrementAndGet();
		}
		public synchronized void finishedBulkLoad() {
			runningBulkLoad.decrementAndGet();
			this.notifyAll();
		}
		public boolean isRunningBulkLoadAtMaxLoad() {
			return runningBulkLoad.intValue()>=concurrent;
		}
		public synchronized void waitAnyBulkLoad() {
			while (runningBulkLoad.intValue()>=concurrent) {
				try {
					this.wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
		public synchronized void waitAllBulkLoad() {
			while (runningBulkLoad.intValue()>=0) {
				try {
					this.wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	/**
	 * Awaits termination of all submitted threads
	 */
	public static void awaitTerminationAfterShutdown(ExecutorService threadPool) {
	    threadPool.shutdown();
	    try {
	        if (!threadPool.awaitTermination(1, TimeUnit.DAYS)) {
	            threadPool.shutdownNow();
	        }
	    } catch (InterruptedException ex) {
	        threadPool.shutdownNow();
	        Thread.currentThread().interrupt();
	    }
	}

	/**
	 * Read SYNC data stored as Parquet file format
	 */
	public void readParquetFileContents(
			InputStream input,
			ConsumeSyncContentsInterface consumer,
			LongAdder... counters) throws IOException {
		
		// If SYNC_PARQUET_USE_TEMPORARY_FILE is TRUE, we have to save the incoming data at a temporary file in order to read it		
		final File tempFile;
		if (SYNC_PARQUET_USE_TEMPORARY_FILE) {
			File tempDir = new File(storageParquetFilesTemporaryDirName);
			if (!tempDir.exists())
				tempDir.mkdirs();					
			tempFile = File.createTempFile("SYNC", ".TMP", tempDir);
		}
		else {
			tempFile = null;
		}
		
		LoadFromParquet loadParquet = null;

		long timestampBeforeParquetRead = System.currentTimeMillis();
		
		try {
			
			final byte[] inMemoryContents;
			
			if (tempFile!=null) {
				inMemoryContents = null;
				// Read the zip entry entirely and save it as a temporary local file
				try (FileOutputStream out=new FileOutputStream(tempFile)) {
					IOUtils.copy(input, out);
				}
			}
			else {
				// Read the zip entry entirely and save to byte buffer in memory
				inMemoryContents = IOUtils.toByteArray(input);
			}
			
			if (log.isLoggable(LEVEL_FOR_PROFILING_PARQUET_READ)) {
				long timestampAfterParquetRead = System.currentTimeMillis();
				long timeElapsed = timestampAfterParquetRead-timestampBeforeParquetRead;
				log.log(LEVEL_FOR_PROFILING_PARQUET_READ, "Time elapsed saving temporary FILE "+timeElapsed+" ms");
				timestampBeforeParquetRead = timestampAfterParquetRead;
			}

			loadParquet = new LoadFromParquet();
			if (inMemoryContents!=null)
				loadParquet.setInputContents(inMemoryContents);
			else
				loadParquet.setInputFile(tempFile);
			loadParquet.init();
			
			if (log.isLoggable(LEVEL_FOR_PROFILING_PARQUET_READ)) {
				long timestampAfterParquetRead = System.currentTimeMillis();
				long timeElapsed = timestampAfterParquetRead-timestampBeforeParquetRead;
				log.log(LEVEL_FOR_PROFILING_PARQUET_READ, "Time elapsed reading FOOTER "+timeElapsed+" ms");
				timestampBeforeParquetRead = timestampAfterParquetRead;
			}

			Map<String,Object> record;
			while ((record=loadParquet.next())!=null) {
				consumer.load(record);
				for (LongAdder counter: counters) {
					counter.increment();
				}
			}
			
			if (log.isLoggable(LEVEL_FOR_PROFILING_PARQUET_READ)) {
				long timestampAfterParquetRead = System.currentTimeMillis();
				long timeElapsed = timestampAfterParquetRead-timestampBeforeParquetRead;
				log.log(LEVEL_FOR_PROFILING_PARQUET_READ, "Time elapsed iterating PARQUET "+timeElapsed+" ms");
				timestampBeforeParquetRead = timestampAfterParquetRead;
			}

		}
		finally {
			if (loadParquet!=null) {
				try {
					loadParquet.close();
				} catch (Exception ex) {
					log.log(Level.WARNING, "Error closing temporary file with PARQUET format!", ex);
				}
			}
			if (tempFile!=null && !tempFile.delete())
				tempFile.deleteOnExit();
		}
	}
	
	/**
	 * Check if it should try again the same HTTP request that has failed
	 */
	public static boolean shouldRetryRequest(Throwable ex) {
		if (ex==null)
			return false;
		String msg = ex.getMessage();
		if (msg!=null && (msg.contains("peer not authenticated") || msg.contains("No PSK available")))
			return true;
		if (ex.getCause()!=null && ex.getCause()!=ex)
			return shouldRetryRequest(ex.getCause());
		return false;
	}

	/**
	 * Objects that wraps a set of constraints. Useful for verifying unique constraints of incoming data in SYNC operations
	 */
	public static class Constraints {
		private final Object[] constraints;
		Constraints(String[] fieldNames, Map<?,?> record) {
			this.constraints = new Object[fieldNames.length];
			for (int i=0; i<fieldNames.length; i++) {
				this.constraints[i] = record.get(fieldNames[i]);
			}
		}
		Constraints(String[] fieldNames, Object bean, PropertyUtilsBean propsCacheable) {
			this.constraints = new Object[fieldNames.length];
			for (int i=0; i<fieldNames.length; i++) {
				try {
					this.constraints[i] = propsCacheable.getSimpleProperty(bean, fieldNames[i]);
				} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
					this.constraints[i] = null;
				}
			}
		}
		public int hashCode() {
			int hash = 17;
			for (Object c: constraints) {
				if (c!=null)
					hash = 37 * (hash + c.hashCode());
			}
			return hash;
		}
		public boolean equals(Object obj) {
			if (this==obj)
				return true;
			if (!(obj instanceof Constraints))
				return false;
			return Arrays.equals(constraints, ((Constraints)obj).constraints);
		}
		public String toString() {
			return Arrays.toString(constraints);
		}
	}
}
