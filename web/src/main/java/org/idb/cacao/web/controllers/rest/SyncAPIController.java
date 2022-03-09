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
package org.idb.cacao.web.controllers.rest;

import static org.idb.cacao.web.utils.ControllerUtils.searchPage;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.idb.cacao.api.PublishedDataFieldNames;
import org.idb.cacao.api.ValidatedDataFieldNames;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.Views;
import org.idb.cacao.api.errors.GeneralException;
import org.idb.cacao.api.storage.FileSystemStorageService;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.utils.IndexNamesUtils;
import org.idb.cacao.api.utils.MappingUtils;
import org.idb.cacao.api.utils.ParserUtils;
import org.idb.cacao.api.utils.ReflectUtils;
import org.idb.cacao.api.utils.ScrollUtils;
import org.idb.cacao.web.Synchronizable;
import org.idb.cacao.web.controllers.AdvancedSearch;
import org.idb.cacao.web.controllers.services.ConfigSyncService;
import org.idb.cacao.web.controllers.services.SyncAPIService;
import org.idb.cacao.web.controllers.services.SyncThread;
import org.idb.cacao.web.controllers.services.UserService;
import org.idb.cacao.web.dto.PaginationData;
import org.idb.cacao.web.dto.SyncDto;
import org.idb.cacao.web.dto.SyncRequestDto;
import org.idb.cacao.web.entities.ConfigSync;
import org.idb.cacao.web.entities.SyncCommitHistory;
import org.idb.cacao.web.entities.SyncCommitMilestone;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.errors.InsufficientPrivilege;
import org.idb.cacao.web.errors.InvalidParameter;
import org.idb.cacao.web.errors.UserNotFoundException;
import org.idb.cacao.web.repositories.DocumentTemplateRepository;
import org.idb.cacao.web.repositories.SyncCommitHistoryRepository;
import org.idb.cacao.web.repositories.SyncCommitMilestoneRepository;
import org.idb.cacao.web.utils.ControllerUtils;
import org.idb.cacao.web.utils.ErrorUtils;
import org.idb.cacao.web.utils.SaveToParquet;
import org.idb.cacao.web.utils.SearchUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.Repository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller class for all RESTful endpoints related to 'synchronization' with other CACAO servers.
 * 
 * @author Gustavo Figueiredo
 *
 */
@RestController
@RequestMapping("/api")
@Tag(name="sync-api-controller", description="Controller class for all RESTful endpoints related to 'synchronization' with other CACAO servers.")
public class SyncAPIController {

	private static final Logger log = Logger.getLogger(SyncAPIController.class.getName());

	public static final int MAX_RESULTS_PER_REQUEST = 10_000;
	
	/**
	 * Field name where Kibana stores the last timestamp for each asset stored at index
	 */
	public static final String KIBANA_TIMESTAMP_FIELD_NAME = "updated_at";
	
	/**
	 * Ignore Kibana indices with these names
	 */
	public static final Pattern KIBANA_IGNORE_PATTERNS = Pattern.compile("^\\.kibana(?>-event-log|_task_manager)");
	
	/**
	 * Default filename for storing SYNC data in Parquet file format
	 */
	public static final String DATA_PARQUET_FILENAME = "data.snappy.parquet";


	@Autowired
	private Environment env;

	@Autowired
	private ApplicationContext app;

	@Autowired
	private MessageSource messageSource;

	@Autowired
	private UserService userService;
	
	@Autowired
	private ConfigSyncService configSyncService;
	
	@Autowired
	private SyncAPIService syncAPIService;
	
	@Autowired
	private FileSystemStorageService fileSystemStorageService;
	
	@Autowired
	private Collection<Repository<?, ?>> allRepositories;
	
	@Autowired
	private SyncCommitHistoryRepository syncHistoryRepository;

	@Autowired
	private SyncCommitMilestoneRepository syncMilestoneRepository;

	@Autowired
	private RestHighLevelClient elasticsearchClient;

	@Autowired
	private DocumentTemplateRepository templateRepository;
	
	@Value("${storage.parquet.files.temporary.dir}")
	private String storageParquetFilesTemporaryDirName;
	
	private static final Map<String, Class<?>> map_repositories_classes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	
	/**
	 * Downloads recent original files for synchronization purpose with other CACAO Server.<BR>
	 * The 'start' parameter is the 'unix epoch' of starting instant. <BR>
	 * The 'end' parameter is the 'unix epoch' of end instant.<BR>
	 * Should return files received in this time interval<BR>
	 */
	@Secured({"ROLE_SYNC_OPS"})
	@GetMapping(value = "/sync/original-files", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ApiOperation("Downloads recent original files for synchronization purpose with other CACAO Server.")
	public ResponseEntity<StreamingResponseBody> getOriginalFiles(
			@ApiParam("The 'start' parameter is the 'unix epoch' of starting instant.") 
			@RequestParam("start") Long start,
			@ApiParam(value="The 'end' parameter is the 'unix epoch' of end instant.",required=false) 
			@RequestParam("end") Optional<Long> optEnd,
			HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();

    	User user = userService.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();

    	final long end = optEnd.orElseGet(System::currentTimeMillis);
		final String remote_ip_addr = (request!=null && request.getRemoteAddr()!=null && request.getRemoteAddr().trim().length()>0) ? request.getRemoteAddr() : null;
		
		if (!isSyncPublisherEnabled()) {
			log.log(Level.INFO, "User "+user.getLogin()+" sync request for original files starting from timestamp "+start
					+" ("+ParserUtils.formatTimestamp(new Date(start))
					+") and ending at timestamp "+end
					+" ("+ParserUtils.formatTimestamp(new Date(end))
					+") IP ADDRESS: "
					+remote_ip_addr
					+". But it's DISABLED at configuration!");
			throw new GeneralException("SYNC service is disabled!");
		}

		if (!matchSyncPublisherFilterHost(remote_ip_addr)) {
			log.log(Level.INFO, "User "+user.getLogin()+" sync request for original files starting from timestamp "+start
					+" ("+ParserUtils.formatTimestamp(new Date(start))
					+") and ending at timestamp "+end
					+" ("+ParserUtils.formatTimestamp(new Date(end))
					+") IP ADDRESS: "
					+remote_ip_addr
					+". But has been REJECTED by the IP address filter!");
			throw new InsufficientPrivilege();			
		}

		log.log(Level.INFO, "User "+user.getLogin()+" sync request for original files starting from timestamp "+start
				+" ("+ParserUtils.formatTimestamp(new Date(start))
				+") and ending at timestamp "+end
				+" ("+ParserUtils.formatTimestamp(new Date(end))
				+") IP ADDRESS: "
				+remote_ip_addr);

    	final String streaming_out_filename = "original_"+start.toString()+".zip";
		StreamingResponseBody responseBody = outputStream -> {
			CheckedOutputStream checksum = new CheckedOutputStream(outputStream, new CRC32());
			ZipOutputStream zip_out = new ZipOutputStream(checksum);
			long copied_files = syncCopyOriginalFiles(start, end, zip_out);
			zip_out.flush();
			zip_out.finish();
			response.flushBuffer();
			log.log(Level.INFO, "User "+user.getLogin()+" sync request for original files starting from timestamp "+start
					+" ("+ParserUtils.formatTimestamp(new Date(start))
					+") and ending at timestamp "+end
					+" ("+ParserUtils.formatTimestamp(new Date(end))
					+") IP ADDRESS: "
					+remote_ip_addr+
					" FINISHED! Copied "+copied_files+" files");

		};
		return ResponseEntity.ok()
	            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename="+streaming_out_filename)
	            .contentType(MediaType.APPLICATION_OCTET_STREAM)
	            .body(responseBody);
	}
	
	/**
	 * Copies all original files submitted between two timestamps
	 */
	public long syncCopyOriginalFiles(long start, long end, ZipOutputStream zip_out) throws IOException {

		// File storage
		LongAdder counter = new LongAdder();
		AtomicLong actual_start_timestamp = new AtomicLong();
		AtomicLong actual_end_timestamp = new AtomicLong();
		AtomicLong pending_timestamp = new AtomicLong();

		fileSystemStorageService.listOriginalFiles(start, end, 
			/*interrupt*/()->counter.intValue()>=MAX_RESULTS_PER_REQUEST,
			/*consumer*/file->{
				long timestamp = file.lastModified();
				if (counter.intValue()>=MAX_RESULTS_PER_REQUEST) {
					pending_timestamp.set(timestamp);
					return;
				}
				Path p = file.toPath();
				String entryName = p.subpath(p.getNameCount()-4, p.getNameCount()).toString();	// keep three level subdirs names (year/month/day)
				ZipEntry ze = new ZipEntry(entryName);
				
				if (actual_start_timestamp.longValue()==0 || actual_start_timestamp.longValue()>timestamp)
					actual_start_timestamp.set(timestamp);
				if (actual_end_timestamp.longValue()==0 || actual_end_timestamp.longValue()<timestamp)
					actual_end_timestamp.set(timestamp);
				ze.setTime(timestamp);
				
				try {
					zip_out.putNextEntry(ze);
					com.google.common.io.Files.copy(file, zip_out);
					counter.increment();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		
		SyncDto syncInfo = new SyncDto();
		syncInfo.setRequestedStart(new Date(start));
		syncInfo.setRequestedEnd(new Date(end));
		syncInfo.setCount(counter.longValue());
		if (actual_start_timestamp.longValue()>0)
			syncInfo.setActualStart(new Date(actual_start_timestamp.longValue()));
		if (actual_end_timestamp.longValue()>0)
			syncInfo.setActualEnd(new Date(actual_end_timestamp.longValue()));
		if (pending_timestamp.longValue()>0) {
			syncInfo.setNextStart(new Date(pending_timestamp.longValue()));
		}
		saveSyncDto(syncInfo, zip_out);
		
		return counter.longValue();
	}

	/**
	 * Downloads recently created or changed objects stored in database for synchronization purpose with other CACAO Server.<BR>
	 * The 'type' indicates the class name of a known repository. <BR>
	 * The 'start' parameter is the 'unix epoch' of starting instant. <BR>
	 * The 'end' parameter is the 'unix epoch' of end instant.<BR>
	 * The 'limit' parameter is the maximum number of records to return from this request.<BR>
	 * Should return files received in this time interval<BR>
	 */
	@Secured({"ROLE_SYNC_OPS"})
	@GetMapping(value = "/sync/base/{type}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ApiOperation("Downloads recently created or changed objects stored in database for synchronization purpose with other CACAO Server.")
	public ResponseEntity<StreamingResponseBody> getBase(
			@ApiParam("The 'type' indicates the class name of a known repository.") @PathVariable("type") String type,
			@ApiParam("The 'start' parameter is the 'unix epoch' of starting instant.") @RequestParam("start") Long start,
			@ApiParam(value="The 'end' parameter is the 'unix epoch' of end instant.",required=false) @RequestParam("end") Optional<Long> opt_end,
			@ApiParam(value="The 'limit' parameter is the maximum number of records to return from this request.",required=false) @RequestParam("limit") Optional<Long> opt_limit,
			HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();

    	User user = userService.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
    	
    	if (type==null || type.trim().length()==0 || Pattern.compile("[^A-Za-z_]").matcher(type).find()) {
    		throw new InvalidParameter("type="+type);
    	}
    	
    	Class<?> repositoryClass = findSyncRepository(type);
    	if (repositoryClass==null) {
    		throw new InvalidParameter("type="+type);
    	}
    	
    	final long end = opt_end.orElseGet(System::currentTimeMillis);
		final String remoteIpAddr = (request!=null && request.getRemoteAddr()!=null && request.getRemoteAddr().trim().length()>0) ? request.getRemoteAddr() : null;

		if (!isSyncPublisherEnabled()) {
			log.log(Level.INFO, "User "+user.getLogin()+" sync request for "+type+" records starting from timestamp "+start
					+" ("+ParserUtils.formatTimestamp(new Date(start))
					+") and ending at timestamp "+end
					+" ("+ParserUtils.formatTimestamp(new Date(end))
					+") IP ADDRESS: "
					+remoteIpAddr
					+". But it's DISABLED at configuration!");
			throw new GeneralException("SYNC service is disabled!");
		}

		if (!matchSyncPublisherFilterHost(remoteIpAddr)) {
			log.log(Level.INFO, "User "+user.getLogin()+" sync request for "+type+" records starting from timestamp "+start
					+" ("+ParserUtils.formatTimestamp(new Date(start))
					+") and ending at timestamp "+end
					+" ("+ParserUtils.formatTimestamp(new Date(end))
					+") IP ADDRESS: "
					+remoteIpAddr
					+". But has been REJECTED by the IP address filter!");
			throw new InsufficientPrivilege();			
		}

		log.log(Level.INFO, "User "+user.getLogin()+" sync request for "+type+" records starting from timestamp "+start
				+" ("+ParserUtils.formatTimestamp(new Date(start))
				+") and ending at timestamp "+end
				+" ("+ParserUtils.formatTimestamp(new Date(end))
				+") IP ADDRESS: "
				+remoteIpAddr				
				+" LIMIT: "+((opt_limit.isPresent()) ? opt_limit.get() : MAX_RESULTS_PER_REQUEST));

    	final String streaming_out_filename = (type.toLowerCase())+"_"+start.toString()+".zip";
		StreamingResponseBody responseBody = outputStream -> {
			CheckedOutputStream checksum = new CheckedOutputStream(outputStream, new CRC32());
			ZipOutputStream zipOut = new ZipOutputStream(checksum);
			long copiedFiles = syncCopyBaseRecords(repositoryClass, start, end, zipOut,
					(opt_limit.isPresent()) ? opt_limit.get() : MAX_RESULTS_PER_REQUEST);
			zipOut.flush();
			zipOut.finish();
			response.flushBuffer();
			log.log(Level.INFO, "User "+user.getLogin()+" sync request for "+type+" records starting from timestamp "+start
					+" ("+ParserUtils.formatTimestamp(new Date(start))
					+") and ending at timestamp "+end
					+" ("+ParserUtils.formatTimestamp(new Date(end))
					+") IP ADDRESS: "
					+remoteIpAddr+
					" FINISHED! Copied "+copiedFiles+" files");

		};
		return ResponseEntity.ok()
	            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename="+streaming_out_filename)
	            .contentType(MediaType.APPLICATION_OCTET_STREAM)
	            .body(responseBody);
	}

	/**
	 * Copies all records of some type stored in database submitted between two timestamps
	 */
	@SuppressWarnings("unchecked")
	public long syncCopyBaseRecords(Class<?> repositoryClass,
			long start, long end, ZipOutputStream zipOut,
			long limit) throws IOException {
		
		// Previous validations
		
		Class<?> entity = ReflectUtils.getParameterType(repositoryClass);
		if (entity==null) {
			log.log(Level.WARNING, "Could not find entity class related to '"+repositoryClass+"'!");
			return 0L;
		}
		Synchronizable syncAnon = repositoryClass.getAnnotation(Synchronizable.class);
		if (syncAnon==null) {
			log.log(Level.WARNING, "Could not find 'Synchronizable' annotation in '"+repositoryClass+"'!");
			return 0L;			
		}
		String timestampField = syncAnon.timestamp();
		if (timestampField==null || timestampField.trim().length()==0) {
			log.log(Level.WARNING, "Missing 'timestamp' field in 'Synchronizable' annotation in '"+repositoryClass+"'!");
			return 0L;						
		}
		Class<?> timestampFieldType = ReflectUtils.getMemberType(entity, timestampField);
		if (timestampFieldType==null) {
			log.log(Level.WARNING, "Not found '"+timestampField+"' field in '"+entity.getName()+"' class. This field name was informed in 'Synchronizable' annotation in '"+repositoryClass+"'!");
			return 0L;									
		}
		final Function<Object,Object> timestampFieldGetter = ReflectUtils.getMemberGetter(entity, timestampField);
		if (timestampFieldGetter==null) {
			log.log(Level.WARNING, "Not found '"+timestampField+"' field in '"+entity.getName()+"' class. This field name was informed in 'Synchronizable' annotation in '"+repositoryClass+"'!");
			return 0L;									
		}
		if (!Date.class.isAssignableFrom(timestampFieldType) && !OffsetDateTime.class.isAssignableFrom(timestampFieldType)) {
			log.log(Level.WARNING, "Wrong type for '"+timestampField+"' field in '"+entity.getName()+"' class ("+timestampFieldType.getName()+"). This field name was informed in 'Synchronizable' annotation in '"+repositoryClass+"'!");
			return 0L;												
		}

		String idField = syncAnon.id();
		if (idField==null || idField.trim().length()==0) {
			log.log(Level.WARNING, "Missing 'id' field in 'Synchronizable' annotation in '"+repositoryClass+"'!");
			return 0L;						
		}
		final Function<Object,Object> idFieldGetter = ReflectUtils.getMemberGetter(entity, idField);
		if (idFieldGetter==null) {
			log.log(Level.WARNING, "Not found '"+idField+"' field in '"+entity.getName()+"' class. This field name was informed in 'Synchronizable' annotation in '"+repositoryClass+"'!");
			return 0L;									
		}

    	final String entitySimpleName = entity.getSimpleName().toLowerCase();
    	
    	final String[] ignorableFieldNames = syncAnon.dontSync();
    	final BiConsumer<Object,Object> ignorableFieldSetters[] = (ignorableFieldNames==null || ignorableFieldNames.length==0) ? null
    			: Arrays.stream(ignorableFieldNames).map(name->ReflectUtils.getMemberSetter(entity, name)).filter(Objects::nonNull).toArray(BiConsumer[]::new);
    	
		// Build and run the query to match the time constraints
		
		Stream<?> queryResults;
		
		if (isESDocument(entity)) {
			queryResults = queryESEntity(entity, timestampField, start, end, limit);
		}
		else {
			log.log(Level.WARNING, "Class '"+entity.getName()+"' is not an Entity and neither a Document!");
			return 0L;
		}
		
		SyncData syncData = new SyncData(entitySimpleName,
				idFieldGetter,
				timestampFieldGetter,
				ignorableFieldSetters,
				zipOut,
				isFullDebugEnabled(),
				limit);
		
		try {
			
			syncData.iterateResults(queryResults, /*checkLimit*/true);
			
		}
		finally {
			queryResults.close();
		}

		// If the requested start timestamp is 0L, we will also consider all those instances that have no timestamp information
		if (start==0L) {
			Stream<?> queryMoreResults = null;
			try {
				queryMoreResults = queryESEntityWithNullTimestamp(entity, timestampField, limit);

				syncData.iterateResults(queryMoreResults, /*checkLimit*/false);
				
			}
			catch (Exception ex) {
				log.log(Level.WARNING, "Error while searching for "+entity.getSimpleName()+" instances with no timestamp information", ex);
			}
			finally {
				if (queryMoreResults!=null)
					queryMoreResults.close();
			}
		}

		SyncDto syncInfo = new SyncDto();
		syncInfo.setRequestedStart(new Date(start));
		syncInfo.setRequestedEnd(new Date(end));
		syncInfo.setCount(syncData.counter.longValue());
		if (syncData.actualStartTimestamp.longValue()>0)
			syncInfo.setActualStart(new Date(syncData.actualStartTimestamp.longValue()));
		if (syncData.actualEndTimestamp.longValue()>0)
			syncInfo.setActualEnd(new Date(syncData.actualEndTimestamp.longValue()));
		if (syncData.pendingTimestamp.longValue()>0) {
			syncInfo.setNextStart(new Date(syncData.pendingTimestamp.longValue()));
		}
		saveSyncDto(syncInfo, zipOut);

		return syncData.counter.longValue();
	}
	
	/**
	 * Helper class used for iterating all results from a query over instances in a local repository. Fetches
	 * some information (the 'ID' and the 'timestamp' for each instance).
	 * 
	 * This class is used by 'syncCopyBaseRecords'.
	 * 
	 * @author Gustavo Figueiredo
	 *
	 */
	private static class SyncData {
    	private final LongAdder counter;    	
    	private final AtomicLong actualStartTimestamp;
    	private final AtomicLong actualEndTimestamp;
    	private final AtomicLong pendingTimestamp;
    	private final String entitySimpleName;
    	private final Function<Object,Object> idFieldGetter;
    	private final Function<Object,Object> timestampFieldGetter;
    	private final BiConsumer<Object,Object> ignorableFieldSetters[];
		private final ObjectMapper mapper;
		private final ZipOutputStream zipOut;
		private final Set<String> includedEntries;
		private final boolean fullDebugInfo;
		private final long limit;

		SyncData(String entitySimpleName,
				Function<Object,Object> idFieldGetter,
				Function<Object,Object> timestampFieldGetter,
				BiConsumer<Object,Object> ignorableFieldSetters[],
				ZipOutputStream zipOut,
				boolean fullDebugInfo,
				long limit) {
			
	    	counter = new LongAdder();
	    	
			actualStartTimestamp = new AtomicLong();
			actualEndTimestamp = new AtomicLong();
			pendingTimestamp = new AtomicLong();
			
			mapper = new ObjectMapper();
			mapper.setSerializationInclusion(Include.NON_NULL);
			mapper.registerModule(new JavaTimeModule());
			
			this.entitySimpleName = entitySimpleName;
			this.idFieldGetter = idFieldGetter;
			this.timestampFieldGetter = timestampFieldGetter;
			this.ignorableFieldSetters = ignorableFieldSetters;
			this.zipOut = zipOut;
			this.includedEntries = new HashSet<>();
			this.fullDebugInfo = fullDebugInfo;
			this.limit = limit;
			
		}

		void iterateResults(final Stream<?> queryResults, final boolean checkLimit) {
			Iterator<?> iterator = queryResults.iterator();
			Date prevTimestamp = null;
			while (iterator.hasNext()) {
				
				Object record = iterator.next();
				
	    		Object id = idFieldGetter.apply(record);
	    		if (id==null) {
	    			if (fullDebugInfo) {
	    				log.log(Level.INFO, "SYNC iterating results of "+entitySimpleName+" found record with no id!");
	    			}
	    			continue;
	    		}
	    		
				String entryName = entitySimpleName+File.separator+id;
				if (includedEntries.contains(entryName)) {
					// avoid duplicates
	    			if (fullDebugInfo) {
	    				log.log(Level.INFO, "SYNC iterating results of "+entitySimpleName+" found record with duplicate id: "+id);
	    			}
					continue; 
				}
				includedEntries.add(entryName); 

	    		Object timestampObj = timestampFieldGetter.apply(record);
	    		Date timestamp = ValidationContext.toDate(timestampObj);
				if (checkLimit && counter.longValue()+1>=limit) {
					if (prevTimestamp!=null && prevTimestamp.equals(timestamp)) {
						// If we reached the limit, but the timestamp is still the same as before, we
						// need to keep going on. Otherwise we won't be able to continue later from this point in another request.						
					}
					else {
						if (timestamp!=null) {
							pendingTimestamp.set(timestamp.getTime());
						}
		    			if (fullDebugInfo) {
		    				log.log(Level.INFO, "SYNC iterating results of "+entitySimpleName+" reached the records limit of: "+limit+", current count is "+counter.intValue()+", last timestamp: "+ParserUtils.formatTimestamp(timestamp));
		    			}
						return;
					}
				}

				ZipEntry ze = new ZipEntry(entryName);
	    		
	    		if (timestamp!=null) {
	    			long t = timestamp.getTime();
					if (actualStartTimestamp.longValue()==0 || actualStartTimestamp.longValue()>t)
						actualStartTimestamp.set(t);
					if (actualEndTimestamp.longValue()==0 || actualEndTimestamp.longValue()<t)
						actualEndTimestamp.set(t);
					ze.setTime(t);
	    		}
	    		
				try {
					zipOut.putNextEntry(ze);
					if (record!=null && ignorableFieldSetters!=null && ignorableFieldSetters.length>0) {
						for (BiConsumer<Object, Object> setter: ignorableFieldSetters) {
							setter.accept(record, null); // clears all fields that we should not copy
						}
					}
					String json = mapper.writeValueAsString(record);
					IOUtils.copy(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), zipOut);
					counter.increment();
				} catch (IOException e) {
	    			if (fullDebugInfo) {
	    				log.log(Level.INFO, "SYNC iterating results of "+entitySimpleName+" got exception on instance with ID "+id, e);
	    			}
					if (ErrorUtils.isErrorThreadInterrupted(e))
						break;
					throw new RuntimeException(e);
				}
				
				if (timestamp!=null)
					prevTimestamp = timestamp;
			}
		}
	}
	
	/**
	 * Returns indication that a given class corresponds to an ElasticSearch annotated entity
	 */
	public static boolean isESDocument(Class<?> entity) {
		return entity.getAnnotation(Document.class)!=null;
	}
	
	/**
	 * Queries for ElasticSearch entities with timestamp informed in a given interval
	 */
	public Stream<?> queryESEntity(Class<?> entity, String timestamp_field, long start, long end, long limit) throws IOException {
		
		Document doc_anon = entity.getAnnotation(Document.class);
		String indexName = doc_anon.indexName();
		
    	final BoolQueryBuilder query = QueryBuilders.boolQuery();
		RangeQueryBuilder b = new RangeQueryBuilder(timestamp_field)
				.from(ParserUtils.formatTimestampES(new Date(start)), /*includeLower*/true)
				.to(ParserUtils.formatTimestampES(new Date(end)), /*includeUpper*/false);
		query.must(b);

    	if (isFullDebugEnabled()) {
    		log.log(Level.INFO, "SYNC entity: "+entity.getSimpleName()+", timestamp_field:"+timestamp_field+", start:"+start+", end:"+end+", ES query: "+query.toString());
    	}

    	// We will not do an usual search (with implicit 10_000 limit) because it may be possible to return more than the maximum limit in case we have
    	// several records created at the same time (e.g. thousands of records with the same timestamp).
    	// For this reason, we also need to use 'SCROLL' for doing this search.
    	
    	return ScrollUtils.findWithScroll(entity, indexName, elasticsearchClient, searchSourceBuilder->{
    		searchSourceBuilder.query(query);
            searchSourceBuilder.sort(timestamp_field, SortOrder.ASC);
            if (limit<=MAX_RESULTS_PER_REQUEST)
            	searchSourceBuilder.size((int)limit); // since we are 'scrolling', this is only for determining a 'batch size'
            else
            	searchSourceBuilder.size(MAX_RESULTS_PER_REQUEST); // batch size
    	});
	}
	
	/**
	 * Queries for ElasticSearch entities with no timestamp information (NULL values)
	 */
	public Stream<?> queryESEntityWithNullTimestamp(Class<?> entity, String timestamp_field, long limit) throws IOException {
		
		Document doc_anon = entity.getAnnotation(Document.class);
		String indexName = doc_anon.indexName();
		
		return ScrollUtils.findWithScroll(entity, indexName, elasticsearchClient, searchSourceBuilder->{
	    	BoolQueryBuilder query = QueryBuilders.boolQuery();
	    	query = query.mustNot(QueryBuilders.existsQuery(timestamp_field));
	    	searchSourceBuilder.query(query); 
            if (limit<=MAX_RESULTS_PER_REQUEST)
            	searchSourceBuilder.size((int)limit); // since we are 'scrolling', this is only for determining a 'batch size'
            else
            	searchSourceBuilder.size(MAX_RESULTS_PER_REQUEST); // batch size
		});
	}

	/**
	 * Given a repository name, returns the class implementing it. Only considers classes annotated with 'Synchronizable' annotation.<BR>
	 * For example, if searching for 'DocumentTemplate', returns the class 'DocumentTemplateRepository', as long as it has the annotation
	 * 'Repository' and is declared inside one of the standard repository packages in this application.
	 */
	public Class<?> findSyncRepository(String typeName) {
		
		synchronized (map_repositories_classes) {
		
			Class<?> repo_found_previously = map_repositories_classes.get(typeName);
			if (repo_found_previously!=null)
				return repo_found_previously;
			
			final String alt_name = (typeName.endsWith("Repository")) ? null : typeName+"Repository";
			Set<String> lookup_names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
			lookup_names.add(typeName);
			if (alt_name!=null)
				lookup_names.add(alt_name);
			
			for (Repository<?,?> repo: allRepositories) {
				Class<?> found_interface = ReflectUtils.getInterfaceWithName(repo.getClass(), lookup_names);
				if (found_interface!=null) {
					Synchronizable sync_anon = found_interface.getAnnotation(Synchronizable.class);
					if (sync_anon==null)
						continue;
					map_repositories_classes.put(typeName, found_interface);
					return found_interface;
				}
			}
			
			return null;
		}
	}
	
	/**
	 * Downloads recently validated documents stored in database for synchronization purpose with other CACAO Server.<BR>
	 * The 'template' indicates the template name associated to the index where the validated documents are stored. Optionally admits the index name corresponding to the validated documents.<BR>
	 * The 'version' indicates the template version.<BR>
	 * The 'start' parameter is the 'unix epoch' of starting instant. <BR>
	 * The 'end' parameter is the 'unix epoch' of end instant.<BR>
	 * The 'line_start' parameter is number of the line to start, in case we stopped before at the middle of a file and we want to resume.<BR>
	 * The 'limit' parameter is the maximum number of records to return from this request.<BR>
	 * The 'format' parameter is used to inform the format of output data. It can be either 'parquet' or 'json' (default is 'json').<BR>
	 * Should return files received in this time interval<BR>
	 */
	@Secured({"ROLE_SYNC_OPS"})
	@GetMapping(value = "/sync/validated/{template}/{version}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ApiOperation("Downloads recently validated documents stored in database for synchronization purpose with other CACAO Server.")
	public ResponseEntity<StreamingResponseBody> getValidatedDocuments(
			@ApiParam("The 'template' indicates the template name associated to the index where the documents are stored. Optionally admits the index name corresponding to the validated documents.") @PathVariable("template") String templateName,
			@ApiParam("The 'version' indicates the template version.") @PathVariable("version") String version,
			@ApiParam("The 'start' parameter is the 'unix epoch' of starting instant.") @RequestParam("start") Long start,
			@ApiParam(value="The 'end' parameter is the 'unix epoch' of end instant.",required=false) @RequestParam("end") Optional<Long> opt_end,
			@ApiParam(value="The 'line_start' parameter is number of the line to start, in case we stopped before at the middle of a file and we want to resume.",required=false) @RequestParam("line_start") Optional<Long> opt_line_start,
			@ApiParam(value="The 'limit' parameter is the maximum number of records to return from this request.",required=false) @RequestParam("limit") Optional<Long> opt_limit,
			@ApiParam(value="The 'format' parameter is used to inform the format of output data. It can be either 'parquet' or 'json' (default is 'json').",required=false) @RequestParam("format") Optional<String> opt_format,
			HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();

    	User user = userService.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
    	
    	if (templateName==null || templateName.trim().length()==0) {
    		throw new InvalidParameter("template="+templateName);
    	}
    	
    	final String indexName;
    	
    	if (templateName.startsWith(IndexNamesUtils.VALIDATED_DATA_INDEX_PREFIX)
    		&& !templateName.contains(" ")
    		&& templateName.contains("_v_")
    		&& templateName.equals(IndexNamesUtils.formatIndexName(templateName))) {
    		
    		// If the provided 'template_name' looks like an index name, let's treat it this way
    		indexName = templateName;
    	}
    	else {
    	
	    	final DocumentTemplate template;
	    	
	    	if (version!=null && version.trim().length()>0) {
	        	Optional<DocumentTemplate> templateVersion = templateRepository.findByNameAndVersion(templateName, version);
	    		if (!templateVersion.isPresent()) {
	    			throw new InvalidParameter("template="+templateName+", version="+version);				
	    		}    	
	    		template = templateVersion.get();
	    	}
	    	else {
	    		List<DocumentTemplate> templateVersions = templateRepository.findByName(templateName);
	    		if (templateVersions==null || templateVersions.isEmpty()) {
	    			throw new InvalidParameter("template="+templateName);				
	    		}    		
	    		if (templateVersions.size()>1) {
	    			// if we have more than one possible choice, let's give higher priority to most recent ones
	    			templateVersions = templateVersions.stream().sorted(DocumentTemplate.TIMESTAMP_COMPARATOR).collect(Collectors.toList());
	    		}
	    		template = templateVersions.get(0);
	    	}
			
			indexName = IndexNamesUtils.formatIndexNameForValidatedData(template);			
    	}

    	final long end = opt_end.orElseGet(System::currentTimeMillis);
		final String remote_ip_addr = (request!=null && request.getRemoteAddr()!=null && request.getRemoteAddr().trim().length()>0) ? request.getRemoteAddr() : null;

		if (!isSyncPublisherEnabled()) {
			log.log(Level.INFO, "User "+user.getLogin()+" sync request for stored "+templateName+" documents starting from timestamp "+start
					+" ("+ParserUtils.formatTimestampWithMS(new Date(start))
					+") and ending at timestamp "+end
					+" ("+ParserUtils.formatTimestampWithMS(new Date(end))
					+") IP ADDRESS: "
					+remote_ip_addr
					+". But it's DISABLED at configuration!");
			throw new GeneralException("SYNC service is disabled!");
		}

		if (!matchSyncPublisherFilterHost(remote_ip_addr)) {
			log.log(Level.INFO, "User "+user.getLogin()+" sync request for stored "+templateName+" documents starting from timestamp "+start
					+" ("+ParserUtils.formatTimestampWithMS(new Date(start))
					+") and ending at timestamp "+end
					+" ("+ParserUtils.formatTimestampWithMS(new Date(end))
					+") IP ADDRESS: "
					+remote_ip_addr
					+". But has been REJECTED by the IP address filter!");
			throw new InsufficientPrivilege();			
		}

		log.log(Level.INFO, "User "+user.getLogin()+" sync request for stored "+templateName+" documents starting from timestamp "+start
				+" ("+ParserUtils.formatTimestampWithMS(new Date(start))
				+") "
				+(opt_line_start.isPresent()?("and line_start ("+opt_line_start.get()+") "):"")
				+"and ending at timestamp "+end
				+" ("+ParserUtils.formatTimestampWithMS(new Date(end))
				+") IP ADDRESS: "
				+remote_ip_addr
				+" LIMIT: "+((opt_limit.isPresent()) ? opt_limit.get() : MAX_RESULTS_PER_REQUEST));

    	final String streaming_out_filename = indexName+"_"+start.toString()+".zip";
		StreamingResponseBody responseBody = outputStream -> {
			CheckedOutputStream checksum = new CheckedOutputStream(outputStream, new CRC32());
			ZipOutputStream zip_out = new ZipOutputStream(checksum);
			long copied_files = syncIndexedData(indexName, /*timestampFieldName*/ValidatedDataFieldNames.TIMESTAMP.getFieldName(),
					start, end,
					ValidatedDataFieldNames.LINE.getFieldName(),
					(opt_line_start.isPresent()) ? opt_line_start.get() : 0,
					zip_out,
					(opt_limit.isPresent()) ? opt_limit.get() : MAX_RESULTS_PER_REQUEST,
					/*parquet*/"parquet".equalsIgnoreCase(opt_format.orElse("json")));
			zip_out.flush();
			zip_out.finish();
			response.flushBuffer();
			log.log(Level.INFO, "User "+user.getLogin()+" sync request for stored "+templateName+" documents starting from timestamp "+start
					+" ("+ParserUtils.formatTimestampWithMS(new Date(start))
					+") and ending at timestamp "+end
					+" ("+ParserUtils.formatTimestampWithMS(new Date(end))
					+") IP ADDRESS: "
					+remote_ip_addr+
					" FINISHED! Copied "+copied_files+" files");

		};
		return ResponseEntity.ok()
	            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename="+streaming_out_filename)
	            .contentType(MediaType.APPLICATION_OCTET_STREAM)
	            .body(responseBody);
	}

	/**
	 * Copies all stored data submitted or changed between two timestamps. May be data produced by the 'validation' phase or by 'ETL' phase
	 * @param parquet If TRUE will save all data in one PARQUET file inside ZIP. If FALSE will save all data in multiple JSON files inside ZIP.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public long syncIndexedData(String index_name, 
			String timestampFieldName, long start, long end,
			String lineFieldName, long lineStart,
			ZipOutputStream zip_out,
			long limit,
			boolean parquet) throws IOException {
		
    	BoolQueryBuilder query = QueryBuilders.boolQuery();
		RangeQueryBuilder b = new RangeQueryBuilder(timestampFieldName)
				.from(ParserUtils.formatTimestampES(new Date(start)), /*includeLower*/true)
				.to(ParserUtils.formatTimestampES(new Date(end)), /*includeUpper*/false);
		query = query.must(b);
		
		if (lineStart>0 && lineFieldName!=null) {
			// If the line start parameter has been informed, we will consider this as an additional filter if and only if
			// the timestamp of the indexed record is the same as the start date/time parameter. Because if the timestamp of the record is higher than the
			// start date/time parameter we will accept any line number
			// This additional query will become something like this:
			//    doc[${timestamp}].value > ${param.start}   OR   doc[${line}].value > ${param.lineStart}
			BoolQueryBuilder additional_query = QueryBuilders.boolQuery();
			additional_query.should(new RangeQueryBuilder(timestampFieldName)
					.from(ParserUtils.formatTimestampES(new Date(start)), /*includeLower*/false));
			additional_query.should(new RangeQueryBuilder(lineFieldName).from(lineStart, /*includeLower*/true));
			additional_query.minimumShouldMatch(1);
			query = query.must(additional_query);
		}
		
		Stream<Map<?,?>> stream = null;
		
		if (limit<=MAX_RESULTS_PER_REQUEST) {
			
			// If we have just a few data to return, do a simple search
		
	    	SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
	    			.query(query); 
	        searchSourceBuilder.from(0);
	        searchSourceBuilder.size((int)limit);
	        if (lineFieldName!=null) {
	        	searchSourceBuilder.sort(Arrays.asList(
	        		SortBuilders.fieldSort(timestampFieldName).order(SortOrder.ASC),
	        		SortBuilders.fieldSort(lineFieldName).order(SortOrder.ASC)));
	        }
	        else {
	        	searchSourceBuilder.sort(timestampFieldName, SortOrder.ASC);
	        }
	        if (log.isLoggable(Level.FINE))
	        	log.log(Level.FINE, "Index: "+index_name+", Search: "+searchSourceBuilder.toString());
	        
	    	SearchRequest searchRequest = new SearchRequest(index_name);
	    	searchRequest.source(searchSourceBuilder);
	    	SearchResponse sresp = MappingUtils.searchIgnoringNoMapError(elasticsearchClient, searchRequest, index_name);    	

	        if (log.isLoggable(Level.FINE))
	        	log.log(Level.FINE, "Index: "+index_name+", Search: "+searchSourceBuilder.toString()+", Replied hits: "+sresp.getHits().getHits().length+" total: "+sresp.getHits().getTotalHits().value);

			if (sresp==null) {
				stream = Collections.<Map<?,?>>emptyList().stream();
			}
			else {
				stream = StreamSupport.stream(sresp.getHits().spliterator(), /*parallel*/false)
					.map(hit->{
						Map<String,Object> map = hit.getSourceAsMap();
						map.put("id", hit.getId());
						map.remove("_class");
						return map;
					});
			}

		}
		else {
			
			// If we have a lot of records to return, do a SCROLL
			
			final BoolQueryBuilder QUERY = query;
			stream = (Stream<Map<?,?>>)(Stream)ScrollUtils.findWithScroll(/*entity*/Map.class, index_name, elasticsearchClient, 
				/*customizeSearch*/searchSourceBuilder->{
					searchSourceBuilder.query(QUERY);
			        if (lineFieldName!=null) {
			        	searchSourceBuilder.sort(Arrays.asList(
			        		SortBuilders.fieldSort(timestampFieldName).order(SortOrder.ASC),
			        		SortBuilders.fieldSort(lineFieldName).order(SortOrder.ASC)));
			        }
			        else {
			        	searchSourceBuilder.sort(timestampFieldName, SortOrder.ASC);
			        }
			        searchSourceBuilder.size(MAX_RESULTS_PER_REQUEST); // batch size
			        if (log.isLoggable(Level.FINE))
			        	log.log(Level.FINE, "Index: "+index_name+", Scroll: "+searchSourceBuilder.toString());
				});
			
		}		

		final ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.registerModule(new JavaTimeModule());

		LongAdder counter = new LongAdder();
		AtomicLong actual_start_timestamp = new AtomicLong();
		AtomicLong actual_end_timestamp = new AtomicLong();
		AtomicLong pending_timestamp = new AtomicLong();
		AtomicLong pending_line = new AtomicLong();
		
		if (stream!=null) {
			final AtomicBoolean shouldBreak = new AtomicBoolean(false);
			File tempFile = null;
			Closeable finalization = null;
			
			try {
				
				final SaveToParquet saveToParquet;
				if (parquet) {
					// We need a temporary local file to store Parquet data. Later we will move it to the zip file after we finish.
					saveToParquet = new SaveToParquet();
					File tempDir = new File(storageParquetFilesTemporaryDirName);
					if (!tempDir.exists())
						tempDir.mkdirs();					
					tempFile = File.createTempFile("SYNC", ".TMP", tempDir);
					saveToParquet.setOutputFile(tempFile);
					try {
						Map<String,Object> mappings = MappingUtils.getMapping(elasticsearchClient, index_name).getSourceAsMap();
						Map<Object,Object> properties = (Map<Object,Object>)mappings.get("properties");
						if (properties==null) {
							properties = new TreeMap<>();
						}
						else if (!(properties instanceof TreeMap)) {
							properties = new TreeMap<>(properties);
						}
						if (!properties.containsKey("id")) {
							properties.put("id", Collections.singletonMap("type", "text"));
						}
						saveToParquet.setSchemaFromProperties(properties);
						saveToParquet.init();
						finalization = saveToParquet;
					}
					catch (Exception ex) {
						if (ErrorUtils.isErrorNoIndexFound(ex) || ErrorUtils.isErrorNoMappingFoundForColumn(ex)) {
							saveToParquet.setSchemaFromProperties(Collections.singletonMap("id", Collections.singletonMap("type", "text")));
							saveToParquet.init();
							finalization = saveToParquet;
						}
						else {
							throw ex;
						}
					}
				}
				else {
					saveToParquet = null;
				}
				
				Spliterator<Map<?,?>> spliterator = stream.spliterator();
				boolean hadNext = true;
				while (hadNext && !shouldBreak.get()) {
					hadNext = spliterator.tryAdvance(map->{
						Object timestamp = map.get(timestampFieldName);
						Date timestamp_as_date = ValidationContext.toDate(timestamp);
						Object lineNumber = (lineFieldName!=null) ? map.get(lineFieldName) : null;
						
						if (counter.intValue()==0 && log.isLoggable(Level.FINE)) {
							log.log(Level.FINE, "First record of index "+index_name+" with id: "+map.get("id")
								+", timestamp: "+ParserUtils.formatTimestampWithMS(timestamp_as_date)
								+", line: "+ValidationContext.toNumber(lineNumber));
						}
						
						if (counter.intValue()>=limit-1) {
							// Actually we stop at 'MAX_RESULTS_PER_REQUEST-1' in this SYNC block because we need to
							// store the timestamp for the next unread register and we can't search for MAX_RESULTS_PER_REQUEST+1
							// elements
							if (timestamp_as_date!=null) {
								pending_timestamp.set((timestamp_as_date).getTime());
							}
							if (lineNumber!=null) {
								pending_line.set(ValidationContext.toNumber(lineNumber).longValue());
							}
							shouldBreak.set(true);
							return;
						}
			
						String entry_name = (String)map.get("id");

			    		if (timestamp_as_date!=null) {
			    			long t = timestamp_as_date.getTime();
							if (actual_start_timestamp.longValue()==0 || actual_start_timestamp.longValue()>t)
								actual_start_timestamp.set(t);
							if (actual_end_timestamp.longValue()==0 || actual_end_timestamp.longValue()<t)
								actual_end_timestamp.set(t);
			    		}

						try {
				    		if (parquet) {
				    			saveToParquet.write(map);
								counter.increment();
				    		}
				    		else {
								ZipEntry ze = new ZipEntry(entry_name);
								
					    		if (timestamp_as_date!=null) {
					    			long t = timestamp_as_date.getTime();
									ze.setTime(t);
					    		}
					    		
								zip_out.putNextEntry(ze);
								String json = mapper.writeValueAsString(map);
								IOUtils.copy(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), zip_out);
								counter.increment();
				    		}
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					});
				}
			}
			finally {
				stream.close();
				if (finalization!=null) {
					try {
						finalization.close();
					} catch (Exception ex) {
						log.log(Level.WARNING, "Error closing temporary file with PARQUET format!", ex);
					}
				}
				if (tempFile!=null) {
					if (tempFile.length()>0) {
						try {
							ZipEntry ze = new ZipEntry(DATA_PARQUET_FILENAME);
							zip_out.putNextEntry(ze);
							try (InputStream fileInput = new FileInputStream(tempFile)) {
								IOUtils.copy(fileInput, zip_out);
							}
						} catch (Exception ex) {
							if (ex.getMessage()!=null && ex.getMessage().contains("Broken pipe")) {
								// peer has disconnected, do not need to write to LOG
							}
							else {
								log.log(Level.WARNING, "Error writing PARQUET contents into ZIP file!", ex);
							}
						}
					}
					if (!tempFile.delete())
						tempFile.deleteOnExit();
				}
			}
		}

		SyncDto sync_info = new SyncDto();
		sync_info.setRequestedStart(new Date(start));
		sync_info.setRequestedEnd(new Date(end));
		sync_info.setCount(counter.longValue());
		if (actual_start_timestamp.longValue()>0)
			sync_info.setActualStart(new Date(actual_start_timestamp.longValue()));
		if (actual_end_timestamp.longValue()>0)
			sync_info.setActualEnd(new Date(actual_end_timestamp.longValue()));
		if (pending_timestamp.longValue()>0) {
			sync_info.setNextStart(new Date(pending_timestamp.longValue()));
		}
		if (pending_line.longValue()>0) {
			sync_info.setNextLineStart(pending_line.longValue());
		}
		saveSyncDto(sync_info, zip_out);

		return counter.longValue();
	}

	/**
	 * Downloads recently published (denormalized) data stored in database for synchronization purpose with other CACAO Server.<BR>
	 * The 'indexname' indicates index name associated to published (denormalized) data. <BR>
	 * The 'start' parameter is the 'unix epoch' of starting instant. <BR>
	 * The 'end' parameter is the 'unix epoch' of end instant.<BR>
	 * The 'line_start' parameter is number of the line to start, in case we stopped before at the middle of a file and we want to resume.<BR>
	 * The 'limit' parameter is the maximum number of records to return from this request.<BR>
	 * The 'format' parameter is used to inform the format of output data. It can be either 'parquet' or 'json' (default is 'json').<BR>
	 * Should return files received in this time interval<BR>
	 */
	@Secured({"ROLE_SYNC_OPS"})
	@GetMapping(value = "/sync/published/{indexname}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ApiOperation("Downloads recently published (denormalized) data stored in database for synchronization purpose with other CACAO Server.")
	public ResponseEntity<StreamingResponseBody> getPublishedData(
			@ApiParam("The 'indexname' indicates index name associated to published (denormalized) data.") @PathVariable("indexname") String indexname,
			@ApiParam("The 'start' parameter is the 'unix epoch' of starting instant.") @RequestParam("start") Long start,
			@ApiParam(value="The 'end' parameter is the 'unix epoch' of end instant.",required=false) @RequestParam("end") Optional<Long> opt_end,
			@ApiParam(value="The 'line_start' parameter is number of the line to start, in case we stopped before at the middle of a file and we want to resume.",required=false) @RequestParam("line_start") Optional<Long> opt_line_start,
			@ApiParam(value="The 'limit' parameter is the maximum number of records to return from this request.",required=false) @RequestParam("limit") Optional<Long> opt_limit,
			@ApiParam(value="The 'format' parameter is used to inform the format of output data. It can be either 'parquet' or 'json' (default is 'json').",required=false) @RequestParam("format") Optional<String> opt_format,
			HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();

    	User user = userService.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
    	
    	if (indexname==null || indexname.trim().length()==0 || !indexname.startsWith(IndexNamesUtils.PUBLISHED_DATA_INDEX_PREFIX)) {
    		throw new InvalidParameter("indexname="+indexname);
    	}
    	
    	final long end = opt_end.orElseGet(System::currentTimeMillis);
		final String remote_ip_addr = (request!=null && request.getRemoteAddr()!=null && request.getRemoteAddr().trim().length()>0) ? request.getRemoteAddr() : null;

		if (!isSyncPublisherEnabled()) {
			log.log(Level.INFO, "User "+user.getLogin()+" sync request for published "+indexname+" data starting from timestamp "+start
					+" ("+ParserUtils.formatTimestampWithMS(new Date(start))
					+") and ending at timestamp "+end
					+" ("+ParserUtils.formatTimestampWithMS(new Date(end))
					+") IP ADDRESS: "
					+remote_ip_addr
					+". But it's DISABLED at configuration!");
			throw new GeneralException("SYNC service is disabled!");
		}

		if (!matchSyncPublisherFilterHost(remote_ip_addr)) {
			log.log(Level.INFO, "User "+user.getLogin()+" sync request for published "+indexname+" data starting from timestamp "+start
					+" ("+ParserUtils.formatTimestampWithMS(new Date(start))
					+") and ending at timestamp "+end
					+" ("+ParserUtils.formatTimestampWithMS(new Date(end))
					+") IP ADDRESS: "
					+remote_ip_addr
					+". But has been REJECTED by the IP address filter!");
			throw new InsufficientPrivilege();			
		}

		log.log(Level.INFO, "User "+user.getLogin()+" sync request for published "+indexname+" data starting from timestamp "+start
				+" ("+ParserUtils.formatTimestampWithMS(new Date(start))
				+") "
				+(opt_line_start.isPresent()?("and line_start ("+opt_line_start.get()+") "):"")
				+"and ending at timestamp "+end
				+" ("+ParserUtils.formatTimestampWithMS(new Date(end))
				+") IP ADDRESS: "
				+remote_ip_addr
				+" LIMIT: "+((opt_limit.isPresent()) ? opt_limit.get() : MAX_RESULTS_PER_REQUEST));

    	final String streaming_out_filename = indexname+"_"+start.toString()+".zip";
		StreamingResponseBody responseBody = outputStream -> {
			CheckedOutputStream checksum = new CheckedOutputStream(outputStream, new CRC32());
			ZipOutputStream zip_out = new ZipOutputStream(checksum);
			long copied_files = syncIndexedData(indexname, 
				/*timestampFieldName*/PublishedDataFieldNames.ETL_TIMESTAMP.getFieldName(), start, end, 
				/*lineFieldName*/PublishedDataFieldNames.LINE.getFieldName(),
				(opt_line_start.isPresent()) ? opt_line_start.get() : 0,
				zip_out,
				(opt_limit.isPresent()) ? opt_limit.get() : MAX_RESULTS_PER_REQUEST,
				/*parquet*/"parquet".equalsIgnoreCase(opt_format.orElse("json")));
			zip_out.flush();
			zip_out.finish();
			response.flushBuffer();
			log.log(Level.INFO, "User "+user.getLogin()+" sync request for published "+indexname+" data starting from timestamp "+start
					+" ("+ParserUtils.formatTimestampWithMS(new Date(start))
					+") and ending at timestamp "+end
					+" ("+ParserUtils.formatTimestampWithMS(new Date(end))
					+") IP ADDRESS: "
					+remote_ip_addr+
					" FINISHED! Copied "+copied_files+" files");

		};
		return ResponseEntity.ok()
	            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename="+streaming_out_filename)
	            .contentType(MediaType.APPLICATION_OCTET_STREAM)
	            .body(responseBody);
	}

	/**
	 * Downloads recently created or changed Kibana assets for synchronization purpose with other CACAO Server.<BR>
	 * The 'start' parameter is the 'unix epoch' of starting instant. <BR>
	 * The 'end' parameter is the 'unix epoch' of end instant.<BR>
	 * Should return files received in this time interval<BR>
	 */
	@Secured({"ROLE_SYNC_OPS"})
	@GetMapping(value = "/sync/kibana", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ApiOperation("Downloads recently created or changed Kibana assets for synchronization purpose with other CACAO Server.")
	public ResponseEntity<StreamingResponseBody> getKibana(
			@ApiParam("The 'start' parameter is the 'unix epoch' of starting instant.") @RequestParam("start") Long start,
			@ApiParam(value="The 'end' parameter is the 'unix epoch' of end instant.",required=false) @RequestParam("end") Optional<Long> opt_end,
			HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();

    	User user = userService.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
    	
    	final long end = opt_end.orElseGet(System::currentTimeMillis);
		final String remote_ip_addr = (request!=null && request.getRemoteAddr()!=null && request.getRemoteAddr().trim().length()>0) ? request.getRemoteAddr() : null;

		if (!isSyncPublisherEnabled()) {
			log.log(Level.INFO, "User "+user.getLogin()+" sync request for Kibana assets starting from timestamp "+start
					+" ("+ParserUtils.formatTimestamp(new Date(start))
					+") and ending at timestamp "+end
					+" ("+ParserUtils.formatTimestamp(new Date(end))
					+") IP ADDRESS: "
					+remote_ip_addr
					+". But it's DISABLED at configuration!");
			throw new GeneralException("SYNC service is disabled!");
		}

		if (!matchSyncPublisherFilterHost(remote_ip_addr)) {
			log.log(Level.INFO, "User "+user.getLogin()+" sync request for Kibana assets starting from timestamp "+start
					+" ("+ParserUtils.formatTimestamp(new Date(start))
					+") and ending at timestamp "+end
					+" ("+ParserUtils.formatTimestamp(new Date(end))
					+") IP ADDRESS: "
					+remote_ip_addr
					+". But has been REJECTED by the IP address filter!");
			throw new InsufficientPrivilege();			
		}

		log.log(Level.INFO, "User "+user.getLogin()+" sync request for Kibana assets starting from timestamp "+start
				+" ("+ParserUtils.formatTimestamp(new Date(start))
				+") and ending at timestamp "+end
				+" ("+ParserUtils.formatTimestamp(new Date(end))
				+") IP ADDRESS: "
				+remote_ip_addr);

    	final String streaming_out_filename = "kibana_"+start.toString()+".zip";
		StreamingResponseBody responseBody = outputStream -> {
			CheckedOutputStream checksum = new CheckedOutputStream(outputStream, new CRC32());
			ZipOutputStream zip_out = new ZipOutputStream(checksum);
			long copied_files = syncCopyKibanaData(start, end, zip_out);
			zip_out.flush();
			zip_out.finish();
			response.flushBuffer();
			log.log(Level.INFO, "User "+user.getLogin()+" sync request for Kibana assets starting from timestamp "+start
					+" ("+ParserUtils.formatTimestamp(new Date(start))
					+") and ending at timestamp "+end
					+" ("+ParserUtils.formatTimestamp(new Date(end))
					+") IP ADDRESS: "
					+remote_ip_addr+
					" FINISHED! Copied "+copied_files+" files");

		};
		return ResponseEntity.ok()
	            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename="+streaming_out_filename)
	            .contentType(MediaType.APPLICATION_OCTET_STREAM)
	            .body(responseBody);
	}

	/**
	 * Copies all Kibana data submitted or changed between two timestamps
	 */
	public long syncCopyKibanaData(long start, long end, ZipOutputStream zip_out) throws IOException {
		
		// First we need to know what are the kibana indices (usually something like '.kibana_1')
		GetIndexRequest get_indices = new GetIndexRequest(".kibana*");
		GetIndexResponse response = elasticsearchClient.indices().get(get_indices, RequestOptions.DEFAULT);
		String[] kibana_indices = response.getIndices();
		if (kibana_indices==null || kibana_indices.length==0)
			return 0L;
		
		// Filter out those indices we don't want to copy (such as 'event log' and 'task manager')
		kibana_indices = Arrays.stream(kibana_indices).filter(i->!KIBANA_IGNORE_PATTERNS.matcher(i).find()).toArray(String[]::new);
		if (kibana_indices.length==0)
			return 0L;
		
		// Let's copy recent/all data for each kibana index

		LongAdder counter = new LongAdder();
		AtomicLong actual_start_timestamp = new AtomicLong();
		AtomicLong actual_end_timestamp = new AtomicLong();
		AtomicLong pending_timestamp = new AtomicLong();

		for (String index_name: kibana_indices) {
		
	    	SearchRequest searchRequest = new SearchRequest(index_name);
	    	BoolQueryBuilder query = QueryBuilders.boolQuery();
			RangeQueryBuilder b = new RangeQueryBuilder(KIBANA_TIMESTAMP_FIELD_NAME)
					.from(ParserUtils.formatTimestampES(new Date(start)), /*includeLower*/true)
					.to(ParserUtils.formatTimestampES(new Date(end)), /*includeUpper*/false);
			query = query.must(b);
	
	    	SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
	    			.query(query); 
	        searchSourceBuilder.from(0);
	        searchSourceBuilder.size(MAX_RESULTS_PER_REQUEST);
	        searchSourceBuilder.sort(KIBANA_TIMESTAMP_FIELD_NAME, SortOrder.ASC);
	
	    	searchRequest.source(searchSourceBuilder);
	    	SearchResponse sresp = null;
	    	try {
	        	sresp = MappingUtils.searchIgnoringNoMapError(elasticsearchClient, searchRequest, index_name);    	
	    	}
	    	catch (Exception ex) {
	    		log.log(Level.SEVERE, "Error while querying data from "+index_name, ex);
	    		continue;
	    	}
	    	if (sresp==null) {
	    		continue;
	    	}
	
			final ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			mapper.registerModule(new JavaTimeModule());
	
			for (SearchHit hit : sresp.getHits()) {
				Map<String, Object> map = hit.getSourceAsMap();
				map.put("id", hit.getId());
				map.remove("_class");
				Object timestamp = map.get(KIBANA_TIMESTAMP_FIELD_NAME);
				
				if (counter.intValue()>=MAX_RESULTS_PER_REQUEST-1) {
					// Actually we stop at 'MAX_RESULTS_PER_REQUEST-1' in this SYNC block because we need to
					// store the timestamp for the next unread register and we can't search for MAX_RESULTS_PER_REQUEST+1
					// elements
					if (timestamp instanceof Date) {
						if (pending_timestamp.get()==0L || pending_timestamp.get()>((Date) timestamp).getTime())
							pending_timestamp.set(((Date) timestamp).getTime());
					}
					break;
				}
	
				String entry_name = index_name+File.separator+hit.getId().replaceAll("[^A-Za-z\\d_\\.]", "_");
				ZipEntry ze = new ZipEntry(entry_name);
				
	    		if (timestamp instanceof Date) {
	    			long t = ((Date) timestamp).getTime();
					if (actual_start_timestamp.longValue()==0 || actual_start_timestamp.longValue()>t)
						actual_start_timestamp.set(t);
					if (actual_end_timestamp.longValue()==0 || actual_end_timestamp.longValue()<t)
						actual_end_timestamp.set(t);
					ze.setTime(t);
	    		}
	
				try {
					zip_out.putNextEntry(ze);
					String json = mapper.writeValueAsString(map);
					IOUtils.copy(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), zip_out);
					counter.increment();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
	
		} // LOOP each Kibana index

		SyncDto sync_info = new SyncDto();
		sync_info.setRequestedStart(new Date(start));
		sync_info.setRequestedEnd(new Date(end));
		sync_info.setCount(counter.longValue());
		if (actual_start_timestamp.longValue()>0)
			sync_info.setActualStart(new Date(actual_start_timestamp.longValue()));
		if (actual_end_timestamp.longValue()>0)
			sync_info.setActualEnd(new Date(actual_end_timestamp.longValue()));
		if (pending_timestamp.longValue()>0) {
			sync_info.setNextStart(new Date(pending_timestamp.longValue()));
		}
		saveSyncDto(sync_info, zip_out);

		return counter.longValue();
	}

	/**
	 * Returns TRUE if the SYNC publisher service is enabled by this application
	 */
	public boolean isSyncPublisherEnabled() {
		return "true".equalsIgnoreCase(env.getProperty("sync.publisher.enabled"));
	}
	
	/**
	 * Returns the configured filter for IP address for SYNC API requests
	 */
	public String getSyncPublisherFilterHost() {
		return env.getProperty("sync.publisher.filter.host");
	}
	
	/**
	 * Applies the configured IP address filter over the actual IP address
	 */
	public boolean matchSyncPublisherFilterHost(String ip_address) {
		String filter = getSyncPublisherFilterHost();
		if (filter==null || filter.trim().length()==0 || "*".equals(filter.trim()))
			return true;
		if (ip_address==null || ip_address.trim().length()==0)
			return false;
		if (filter.contains("*") || filter.contains("|")) {
			if (filter.contains(".*")) {
				// The filter has some form of regex wildcards, so let's treat as such
				try {
					return Pattern.compile(filter, Pattern.CASE_INSENSITIVE).matcher(ip_address).find();
				} catch (Exception ex) { return false; }
			}
			else {
				// The filter has some simple form of wildcards, so let's add the regex form
				try {
					return Pattern.compile(filter
							.replace(".","\\.") // treat dots as literals
							.replace("*", ".*"),// treat wildcards as regex wildcards 
							Pattern.CASE_INSENSITIVE).matcher(ip_address).find();
				} catch (Exception ex) { return false; }				
			}
		}
		else {
			return filter.equalsIgnoreCase(ip_address);
		}
	}
	
	/**
	 * Save SYNC extra information in ZIP file
	 */
	public static void saveSyncDto(SyncDto sync_info, ZipOutputStream zip_out) throws IOException {
		ZipEntry ze = new ZipEntry(SyncDto.DEFAULT_FILENAME);
		zip_out.putNextEntry(ze);
		final ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_NULL);
		mapper.registerModule(new JavaTimeModule());
		String json = mapper.writeValueAsString(sync_info);
		IOUtils.copy(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), zip_out);
	}
	
	/**
	 * Endpoint for checking if there is a SYNC process running right now
	 */
	@Secured({"ROLE_SYNC_OPS"})
	@GetMapping(value = "/sync/running",
			produces = MediaType.TEXT_PLAIN_VALUE)
	@ApiOperation("Endpoint for checking if there is a SYNC process running right now")
	public ResponseEntity<String> isSyncRunning() throws Exception {
		
		SyncThread running_thread = syncAPIService.getRunningSyncThread();
		return ResponseEntity.ok().body((running_thread!=null && running_thread.isRunning())?"RUNNING":"STOPPED");
		
	}
	
	/**
	 * Endpoint for starting a SYNC process (this endpoint is for a subscriber to start communicating with a master)
	 */
	@Secured({"ROLE_SYNC_OPS"})
	@PostMapping(value = "/sync/now",
			produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation("Endpoint for starting a SYNC process (this endpoint is for a subscriber to start communicating with a master)")
	public ResponseEntity<Object> putSyncNow(@RequestBody SyncRequestDto request) throws Exception {

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();

		log.log(Level.FINE, "Start manual SYNC by user "+auth.getName());

		ConfigSync config = configSyncService.getActiveConfig();
		if (config==null || config.getMaster()==null || config.getMaster().trim().length()==0) 
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", 
					messageSource.getMessage("error.missing.config_sync", null, LocaleContextHolder.getLocale())));
		if (config.getApiToken()==null || config.getApiToken().trim().length()==0)
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", 
					messageSource.getMessage("error.missing.token_sync", null, LocaleContextHolder.getLocale())));
		
		boolean resumeFromLastSync = request==null || !Boolean.TRUE.equals(request.getFromStart());
		List<String> endpoints = (request!=null) ? request.getEndpoints() : null;

		try {
			syncAPIService.startSyncThread(auth.getName(), resumeFromLastSync, endpoints);
		}
		catch (RuntimeException ex) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", 
					ex.getMessage()));							
		}
		
		return ResponseEntity.ok().body(Collections.singletonMap("message", "OK"));
	}
	
	/**
	 * Global check over all entities looking for those that have 'NULL' at the 'changedtime' field.<BR>
	 * It should not happen, unless it's a test environment used before this field was defined.<BR>
	 * Will initialize with present timestamp for all.
	 */
	@Transactional
	public void initializeNullTimestamps() throws IOException {
		
		log.log(Level.INFO, "Checking all instances stored for 'null' timestamps...");
		
		// Do this for all instances stored at repositories, except 'documents' (we will consider this later at this method)
		List<Class<?>> all_sync_repos = syncAPIService.getAllSyncRepositories();
		final Date timestamp_to_fill_nulls = new Date(); 
		for (Class<?> repository_class: all_sync_repos) {
			@SuppressWarnings({ "unchecked" })
			CrudRepository<Object, ?> repository = (CrudRepository<Object, ?>)app.getBean(repository_class);
			if (repository==null)
				continue;
			Synchronizable sync_anon = repository_class.getAnnotation(Synchronizable.class);
			if (sync_anon==null) 
				continue;
			String timestamp_field = sync_anon.timestamp();
			if (timestamp_field==null || timestamp_field.trim().length()==0) 
				continue;
			Class<?> entity = ReflectUtils.getParameterType(repository_class);
			if (entity==null) 
				continue;
			Class<?> timestamp_field_type = ReflectUtils.getMemberType(entity, timestamp_field);
			if (timestamp_field_type==null) 
				continue;
			final Function<Object,Date> timestamp_field_getter = ReflectUtils.getMemberGetter(entity, timestamp_field);
			if (timestamp_field_getter==null) 
				continue;
			final BiConsumer<Object,Date> timestamp_field_setter = ReflectUtils.getMemberSetter(entity, timestamp_field);
			if (timestamp_field_setter==null) 
				continue;
			
			log.log(Level.INFO, "Checking all instances of '"+entity.getSimpleName()+"' for 'null' timestamps...");
			
			Stream<?> query_results;
			
			if (SyncAPIController.isESDocument(entity)) {
				query_results = queryESEntityWithNullTimestamp(entity, timestamp_field, MAX_RESULTS_PER_REQUEST);
			}
			else {
				continue;
			}
			final List<Object> instances_to_change = new LinkedList<>();
			try {
		    	query_results.forEach(record->{
		    		Date timestamp = timestamp_field_getter.apply(record);
		    		if (timestamp==null) {
		    			instances_to_change.add(record);
		    		}
		    	});
			} catch (Exception ex) {
				log.log(Level.WARNING, "Error while checking instances of '"+entity.getSimpleName()+"' for 'null' timestamps...", ex);
			}
	    	if (!instances_to_change.isEmpty()) {
	    		for (Object record: instances_to_change) {
	    			timestamp_field_setter.accept(record, timestamp_to_fill_nulls);
	    			repository.save(record);
	    		}
	    		log.log(Level.INFO, "Initialized "+instances_to_change.size()+" instances of '"+entity.getSimpleName()+"' that had 'null' timestamps");
	    	}
		}
		
		// Now repeat this verification for all instances of 'parsed documents'. We need their 'template names'
		Set<String> indices = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		templateRepository.findAll().forEach(t->indices.add(IndexNamesUtils.formatIndexNameForValidatedData(t)));
		for (String index_name: indices) {
	    	SearchRequest searchRequest = new SearchRequest(index_name);
	    	BoolQueryBuilder query = QueryBuilders.boolQuery();
	    	query = query.mustNot(QueryBuilders.existsQuery(ValidatedDataFieldNames.TIMESTAMP.getFieldName()));

	    	SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
	    			.query(query); 
	        searchSourceBuilder.from(0);
	        searchSourceBuilder.size(SyncAPIController.MAX_RESULTS_PER_REQUEST);

	    	searchRequest.source(searchSourceBuilder);
	    	SearchResponse sresp = MappingUtils.searchIgnoringNoMapError(elasticsearchClient, searchRequest, index_name);
	    	if (sresp==null) {
	    		continue;
	    	}

			final LongAdder count_changes = new LongAdder();
			for (SearchHit hit : sresp.getHits()) {
				Map<String, Object> parsed_contents = hit.getSourceAsMap();
				Object timestamp = parsed_contents.get(ValidatedDataFieldNames.TIMESTAMP.getFieldName());
				if (timestamp==null) {
					parsed_contents.put(ValidatedDataFieldNames.TIMESTAMP.getFieldName(), timestamp_to_fill_nulls);
					parsed_contents.remove("_class");
					elasticsearchClient.index(new IndexRequest(index_name)
							.id(hit.getId())
							.source(parsed_contents)
							.setRefreshPolicy(RefreshPolicy.IMMEDIATE),
							RequestOptions.DEFAULT);
					count_changes.increment();
				}
			}
			
			if (count_changes.longValue()>0) {
				log.log(Level.INFO, "Initialized "+count_changes.longValue()+" instances of '"+index_name+"' that had 'null' timestamps");
			}
		}
	}
	
	public boolean isFullDebugEnabled() {
		return "true".equalsIgnoreCase(env.getProperty("sync.full.debug"));
	}

	@JsonView(Views.Public.class)
	@Secured({"ROLE_SYNC_OPS"})
	@GetMapping(value="/sync/history", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Method used for listing history of synchronization operations using pagination")
	@Transactional
	public PaginationData<SyncCommitHistory> getSyncHistory(Model model, 
			@RequestParam("endpoint") Optional<String> endpoint,
			@ApiParam(name = "Number of page to retrieve", allowEmptyValue = true, allowMultiple = false, required = false, type = "Integer")
			@RequestParam("page") Optional<Integer> page, 
			@ApiParam(name = "Page size", allowEmptyValue = true, allowMultiple = false, required = false, type = "Integer")
			@RequestParam("size") Optional<Integer> size,
			@ApiParam(name = "Fields and values to filer data", allowEmptyValue = true, allowMultiple = false, required = false, type = "String")
			@RequestParam("filter") Optional<String> filter, 
			@ApiParam(name = "Field name to sort data", allowEmptyValue = true, allowMultiple = false, required = false, type = "String")
			@RequestParam("sortby") Optional<String> sortBy,
			@ApiParam(name = "Order to sort. Can be asc or desc", allowEmptyValue = true, allowMultiple = false, required = false, type = "String")
			@RequestParam("sortorder") Optional<String> sortOrder) {
		int currentPage = page.orElse(1);
		int pageSize = ControllerUtils.getPageSizeForUser(size, env);
		Sort.Direction direction = sortOrder.orElse("desc").equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
		Optional<AdvancedSearch> filters = SearchUtils.fromTabulatorJSON(filter);
		Page<SyncCommitHistory> commits;
		if (endpoint.isPresent()) {
			if (filters.isPresent() && !filters.get().isEmpty()) {
				Optional<AdvancedSearch> filtersWithEndpoint = Optional.of(filters.get().clone().withFilter("endpoint", endpoint.get()));
				commits = searchCommitHistory(filtersWithEndpoint, page, size, sortBy, sortOrder);
			} else {
				commits = searchPage(() -> syncHistoryRepository
						.findByEndPoint(endpoint.get(), PageRequest.of(currentPage - 1, pageSize, Sort.by(direction, sortBy.orElse("timeRun")))));
			}			
		}
		else {
			if (filters.isPresent() && !filters.get().isEmpty()) {
				commits = searchCommitHistory(filters, page, size, sortBy, sortOrder);
			} else {
				commits = searchPage(() -> syncHistoryRepository
						.findAll(PageRequest.of(currentPage - 1, pageSize, Sort.by(sortBy.orElse("timeRun")).descending())));
			}
		}
		return new PaginationData<>(commits.getTotalPages(), commits.getContent());
	}

	@JsonView(Views.Public.class)
	@Secured({"ROLE_SYNC_OPS"})
	@GetMapping(value="/sync/milestone", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Method used for listing current stats of synchronizable resources using pagination")
	@Transactional
	public PaginationData<SyncCommitMilestone> getSyncMilestone(Model model, 
			@ApiParam(name = "Number of page to retrieve", allowEmptyValue = true, allowMultiple = false, required = false, type = "Integer")
			@RequestParam("page") Optional<Integer> page, 
			@ApiParam(name = "Page size", allowEmptyValue = true, allowMultiple = false, required = false, type = "Integer")
			@RequestParam("size") Optional<Integer> size,
			@ApiParam(name = "Fields and values to filer data", allowEmptyValue = true, allowMultiple = false, required = false, type = "String")
			@RequestParam("filter") Optional<String> filter, 
			@ApiParam(name = "Field name to sort data", allowEmptyValue = true, allowMultiple = false, required = false, type = "String")
			@RequestParam("sortby") Optional<String> sortBy,
			@ApiParam(name = "Order to sort. Can be asc or desc", allowEmptyValue = true, allowMultiple = false, required = false, type = "String")
			@RequestParam("sortorder") Optional<String> sortOrder) {
		int currentPage = page.orElse(1);
		int pageSize = ControllerUtils.getPageSizeForUser(size, env);
		Sort.Direction direction = sortOrder.orElse("asc").equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
		Optional<AdvancedSearch> filters = SearchUtils.fromTabulatorJSON(filter);
		Page<SyncCommitMilestone> commits;
		if (filters.isPresent() && !filters.get().isEmpty()) {
			commits = searchCommitMilestone(filters, page, size, sortBy, sortOrder);
		} else {
			commits = searchPage(() -> syncMilestoneRepository
					.findAll(PageRequest.of(currentPage - 1, pageSize, Sort.by(direction, sortBy.orElse("endPoint")))));
		}
		PaginationData<SyncCommitMilestone> result = new PaginationData<>(commits.getTotalPages(), commits.getContent());
		return result;
	}

    /**
     * Search SyncCommitHistory objects using AdvancedSearch filters
     */
	@Transactional(readOnly=true)
	public Page<SyncCommitHistory> searchCommitHistory(Optional<AdvancedSearch> filters,
			Optional<Integer> page, 
			Optional<Integer> size,
			Optional<String> sortBy,
			Optional<String> sortOrder) {
		try {
			if ( filters.isPresent() )
				return SearchUtils.doSearch(filters.get().wiredTo(messageSource), SyncCommitHistory.class, elasticsearchClient, page, size, 
					Optional.of(sortBy.orElse("timeRun")), 
					Optional.of(sortOrder.orElse("desc").equals("asc") ? SortOrder.ASC : SortOrder.DESC));
			return null;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}

    /**
     * Search SyncCommitMilestone objects using AdvancedSearch filters
     */
	@Transactional(readOnly=true)
	public Page<SyncCommitMilestone> searchCommitMilestone(Optional<AdvancedSearch> filters,
			Optional<Integer> page, 
			Optional<Integer> size,
			Optional<String> sortBy,
			Optional<String> sortOrder) {
		try {
			if ( filters.isPresent() )
				return SearchUtils.doSearch(filters.get().wiredTo(messageSource), SyncCommitMilestone.class, elasticsearchClient, page, size, 
					Optional.of(sortBy.orElse("endPoint")), 
					Optional.of(sortOrder.orElse("asc").equals("asc") ? SortOrder.ASC : SortOrder.DESC));
			return null;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}

}
