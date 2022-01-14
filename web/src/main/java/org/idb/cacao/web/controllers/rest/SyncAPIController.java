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
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import com.google.common.io.Files;
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
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import org.elasticsearch.search.sort.SortOrder;
import org.idb.cacao.api.PublishedDataFieldNames;
import org.idb.cacao.api.ValidatedDataFieldNames;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.Views;
import org.idb.cacao.api.errors.GeneralException;
import org.idb.cacao.api.storage.FileSystemStorageService;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.utils.IndexNamesUtils;
import org.idb.cacao.api.utils.ParserUtils;
import org.idb.cacao.web.Synchronizable;
import org.idb.cacao.web.controllers.AdvancedSearch;
import org.idb.cacao.web.controllers.dto.PaginationData;
import org.idb.cacao.web.controllers.dto.SyncDto;
import org.idb.cacao.web.controllers.dto.SyncRequestDto;
import org.idb.cacao.web.controllers.services.ConfigSyncService;
import org.idb.cacao.web.controllers.services.SyncAPIService;
import org.idb.cacao.web.controllers.services.SyncThread;
import org.idb.cacao.web.controllers.services.UserService;
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
import org.idb.cacao.web.utils.ESUtils;
import org.idb.cacao.web.utils.ErrorUtils;
import org.idb.cacao.web.utils.ReflectUtils;
import org.idb.cacao.web.utils.SearchUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
	private Collection<Repository<?, ?>> all_repositories;
	
	@Autowired
	private SyncCommitHistoryRepository syncHistoryRepository;

	@Autowired
	private SyncCommitMilestoneRepository syncMilestoneRepository;

	@Autowired
	private RestHighLevelClient elasticsearchClient;

	@Autowired
	private DocumentTemplateRepository templateRepository;
	
	private static final Map<String, Class<?>> map_repositories_classes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	
	/**
	 * Downloads recent original files for synchronization purpose with other CACAO Server.<BR>
	 * The 'start' parameter is the 'unix epoch' of starting instant. <BR>
	 * The 'end' parameter is the 'unix epoch' of end instant.<BR>
	 * Should return files received in this time interval<BR>
	 */
	@Secured({"ROLE_SYNC_OPS"})
	@GetMapping(value = "/sync/original_files", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ApiOperation("Downloads recent original files for synchronization purpose with other CACAO Server.")
	public ResponseEntity<StreamingResponseBody> getOriginalFiles(
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
				String entry_name = p.subpath(p.getNameCount()-4, p.getNameCount()).toString();	// keep three level subdirs names (year/month/day)
				ZipEntry ze = new ZipEntry(entry_name);
				
				if (actual_start_timestamp.longValue()==0 || actual_start_timestamp.longValue()>timestamp)
					actual_start_timestamp.set(timestamp);
				if (actual_end_timestamp.longValue()==0 || actual_end_timestamp.longValue()<timestamp)
					actual_end_timestamp.set(timestamp);
				ze.setTime(timestamp);
				
				try {
					zip_out.putNextEntry(ze);
					Files.copy(file, zip_out);
					counter.increment();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		
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
	 * Downloads recently created or changed objects stored in database for synchronization purpose with other CACAO Server.<BR>
	 * The 'type' indicates the class name of a known repository. <BR>
	 * The 'start' parameter is the 'unix epoch' of starting instant. <BR>
	 * The 'end' parameter is the 'unix epoch' of end instant.<BR>
	 * Should return files received in this time interval<BR>
	 */
	@Secured({"ROLE_SYNC_OPS"})
	@GetMapping(value = "/sync/base/{type}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ApiOperation("Downloads recently created or changed objects stored in database for synchronization purpose with other CACAO Server.")
	public ResponseEntity<StreamingResponseBody> getBase(
			@ApiParam("The 'type' indicates the class name of a known repository.") @PathVariable("type") String type,
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
    	
    	if (type==null || type.trim().length()==0 || Pattern.compile("[^A-Za-z_]").matcher(type).find()) {
    		throw new InvalidParameter("type="+type);
    	}
    	
    	Class<?> repository_class = findSyncRepository(type);
    	if (repository_class==null) {
    		throw new InvalidParameter("type="+type);
    	}
    	
    	final long end = opt_end.orElseGet(System::currentTimeMillis);
		final String remote_ip_addr = (request!=null && request.getRemoteAddr()!=null && request.getRemoteAddr().trim().length()>0) ? request.getRemoteAddr() : null;

		if (!isSyncPublisherEnabled()) {
			log.log(Level.INFO, "User "+user.getLogin()+" sync request for "+type+" records starting from timestamp "+start
					+" ("+ParserUtils.formatTimestamp(new Date(start))
					+") and ending at timestamp "+end
					+" ("+ParserUtils.formatTimestamp(new Date(end))
					+") IP ADDRESS: "
					+remote_ip_addr
					+". But it's DISABLED at configuration!");
			throw new GeneralException("SYNC service is disabled!");
		}

		if (!matchSyncPublisherFilterHost(remote_ip_addr)) {
			log.log(Level.INFO, "User "+user.getLogin()+" sync request for "+type+" records starting from timestamp "+start
					+" ("+ParserUtils.formatTimestamp(new Date(start))
					+") and ending at timestamp "+end
					+" ("+ParserUtils.formatTimestamp(new Date(end))
					+") IP ADDRESS: "
					+remote_ip_addr
					+". But has been REJECTED by the IP address filter!");
			throw new InsufficientPrivilege();			
		}

		log.log(Level.INFO, "User "+user.getLogin()+" sync request for "+type+" records starting from timestamp "+start
				+" ("+ParserUtils.formatTimestamp(new Date(start))
				+") and ending at timestamp "+end
				+" ("+ParserUtils.formatTimestamp(new Date(end))
				+") IP ADDRESS: "
				+remote_ip_addr);

    	final String streaming_out_filename = (type.toLowerCase())+"_"+start.toString()+".zip";
		StreamingResponseBody responseBody = outputStream -> {
			CheckedOutputStream checksum = new CheckedOutputStream(outputStream, new CRC32());
			ZipOutputStream zip_out = new ZipOutputStream(checksum);
			long copied_files = syncCopyBaseRecords(repository_class, start, end, zip_out);
			zip_out.flush();
			zip_out.finish();
			response.flushBuffer();
			log.log(Level.INFO, "User "+user.getLogin()+" sync request for "+type+" records starting from timestamp "+start
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
	 * Copies all records of some type stored in database submitted between two timestamps
	 */
	@SuppressWarnings("unchecked")
	public long syncCopyBaseRecords(Class<?> repository_class,
			long start, long end, ZipOutputStream zip_out) throws IOException {
		
		// Previous validations
		
		Class<?> entity = ReflectUtils.getParameterType(repository_class);
		if (entity==null) {
			log.log(Level.WARNING, "Could not find entity class related to '"+repository_class+"'!");
			return 0L;
		}
		Synchronizable sync_anon = repository_class.getAnnotation(Synchronizable.class);
		if (sync_anon==null) {
			log.log(Level.WARNING, "Could not find 'Synchronizable' annotation in '"+repository_class+"'!");
			return 0L;			
		}
		String timestamp_field = sync_anon.timestamp();
		if (timestamp_field==null || timestamp_field.trim().length()==0) {
			log.log(Level.WARNING, "Missing 'timestamp' field in 'Synchronizable' annotation in '"+repository_class+"'!");
			return 0L;						
		}
		Class<?> timestamp_field_type = ReflectUtils.getMemberType(entity, timestamp_field);
		if (timestamp_field_type==null) {
			log.log(Level.WARNING, "Not found '"+timestamp_field+"' field in '"+entity.getName()+"' class. This field name was informed in 'Synchronizable' annotation in '"+repository_class+"'!");
			return 0L;									
		}
		final Function<Object,Object> timestamp_field_getter = ReflectUtils.getMemberGetter(entity, timestamp_field);
		if (timestamp_field_getter==null) {
			log.log(Level.WARNING, "Not found '"+timestamp_field+"' field in '"+entity.getName()+"' class. This field name was informed in 'Synchronizable' annotation in '"+repository_class+"'!");
			return 0L;									
		}
		if (!Date.class.isAssignableFrom(timestamp_field_type) && !OffsetDateTime.class.isAssignableFrom(timestamp_field_type)) {
			log.log(Level.WARNING, "Wrong type for '"+timestamp_field+"' field in '"+entity.getName()+"' class ("+timestamp_field_type.getName()+"). This field name was informed in 'Synchronizable' annotation in '"+repository_class+"'!");
			return 0L;												
		}

		String id_field = sync_anon.id();
		if (id_field==null || id_field.trim().length()==0) {
			log.log(Level.WARNING, "Missing 'id' field in 'Synchronizable' annotation in '"+repository_class+"'!");
			return 0L;						
		}
		final Function<Object,Object> id_field_getter = ReflectUtils.getMemberGetter(entity, id_field);
		if (id_field_getter==null) {
			log.log(Level.WARNING, "Not found '"+id_field+"' field in '"+entity.getName()+"' class. This field name was informed in 'Synchronizable' annotation in '"+repository_class+"'!");
			return 0L;									
		}

    	final String entity_simple_name = entity.getSimpleName().toLowerCase();
    	
    	final String[] ignorableFieldNames = sync_anon.dontSync();
    	final BiConsumer<Object,Object> ignorableFieldSetters[] = (ignorableFieldNames==null || ignorableFieldNames.length==0) ? null
    			: Arrays.stream(ignorableFieldNames).map(name->ReflectUtils.getMemberSetter(entity, name)).filter(f->f!=null).toArray(BiConsumer[]::new);
    	
		// Build and run the query to match the time constraints
		
		Stream<?> query_results;
		
		if (isESDocument(entity)) {
			query_results = queryESEntity(entity, timestamp_field, start, end);
		}
		else {
			log.log(Level.WARNING, "Class '"+entity.getName()+"' is not an Entity and neither a Document!");
			return 0L;
		}
		
		SyncData sync_data = new SyncData(entity_simple_name,
				id_field_getter,
				timestamp_field_getter,
				ignorableFieldSetters,
				zip_out,
				isFullDebugEnabled());
		
		try {
			
			sync_data.iterateResults(query_results, /*checkLimit*/true);
			
		}
		finally {
			query_results.close();
		}

		// If the requested start timestamp is 0L, we will also consider all those instances that have no timestamp information
		if (start==0L) {
			Stream<?> query_more_results = null;
			try {
				query_more_results = queryESEntityWithNullTimestamp(entity, timestamp_field);

				sync_data.iterateResults(query_more_results, /*checkLimit*/false);
				
			}
			catch (Throwable ex) {
				log.log(Level.WARNING, "Error while searching for "+entity.getSimpleName()+" instances with no timestamp information", ex);
			}
			finally {
				if (query_more_results!=null)
					query_more_results.close();
			}
		}

		SyncDto sync_info = new SyncDto();
		sync_info.setRequestedStart(new Date(start));
		sync_info.setRequestedEnd(new Date(end));
		sync_info.setCount(sync_data.counter.longValue());
		if (sync_data.actual_start_timestamp.longValue()>0)
			sync_info.setActualStart(new Date(sync_data.actual_start_timestamp.longValue()));
		if (sync_data.actual_end_timestamp.longValue()>0)
			sync_info.setActualEnd(new Date(sync_data.actual_end_timestamp.longValue()));
		if (sync_data.pending_timestamp.longValue()>0) {
			sync_info.setNextStart(new Date(sync_data.pending_timestamp.longValue()));
		}
		saveSyncDto(sync_info, zip_out);

		return sync_data.counter.longValue();
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
    	private final AtomicLong actual_start_timestamp;
    	private final AtomicLong actual_end_timestamp;
    	private final AtomicLong pending_timestamp;
    	private final String entity_simple_name;
    	private final Function<Object,Object> id_field_getter;
    	private final Function<Object,Object> timestamp_field_getter;
    	private final BiConsumer<Object,Object> ignorableFieldSetters[];
		private final ObjectMapper mapper;
		private final ZipOutputStream zip_out;
		private final Set<String> included_entries;
		private final boolean full_debug_info;

		SyncData(String entity_simple_name,
				Function<Object,Object> id_field_getter,
				Function<Object,Object> timestamp_field_getter,
				BiConsumer<Object,Object> ignorableFieldSetters[],
				ZipOutputStream zip_out,
				boolean full_debug_info) {
			
	    	counter = new LongAdder();
	    	
			actual_start_timestamp = new AtomicLong();
			actual_end_timestamp = new AtomicLong();
			pending_timestamp = new AtomicLong();
			
			mapper = new ObjectMapper();
			mapper.setSerializationInclusion(Include.NON_NULL);
			mapper.registerModule(new JavaTimeModule());
			
			this.entity_simple_name = entity_simple_name;
			this.id_field_getter = id_field_getter;
			this.timestamp_field_getter = timestamp_field_getter;
			this.ignorableFieldSetters = ignorableFieldSetters;
			this.zip_out = zip_out;
			this.included_entries = new HashSet<>();
			this.full_debug_info = full_debug_info;
			
		}

		void iterateResults(final Stream<?> query_results, final boolean checkLimit) {
			Iterator<?> iterator = query_results.iterator();
			Date prev_timestamp = null;
			while (iterator.hasNext()) {
				
				Object record = iterator.next();
				
	    		Object id = id_field_getter.apply(record);
	    		if (id==null) {
	    			if (full_debug_info) {
	    				log.log(Level.INFO, "SYNC iterating results of "+entity_simple_name+" found record with no id!");
	    			}
	    			continue;
	    		}
	    		
				String entry_name = entity_simple_name+File.separator+id;
				if (included_entries.contains(entry_name)) {
					// avoid duplicates
	    			if (full_debug_info) {
	    				log.log(Level.INFO, "SYNC iterating results of "+entity_simple_name+" found record with duplicate id: "+id);
	    			}
					continue; 
				}
				included_entries.add(entry_name); 

	    		Object timestamp_obj = timestamp_field_getter.apply(record);
	    		Date timestamp = ValidationContext.toDate(timestamp_obj);
				if (checkLimit && counter.intValue()+1>=MAX_RESULTS_PER_REQUEST) {
					if (prev_timestamp!=null && prev_timestamp.equals(timestamp)) {
						// If we reached the limit, but the timestamp is still the same as before, we
						// need to keep going on. Otherwise we won't be able to continue later from this point in another request.						
					}
					else {
						if (timestamp!=null) {
							pending_timestamp.set(timestamp.getTime());
						}
		    			if (full_debug_info) {
		    				log.log(Level.INFO, "SYNC iterating results of "+entity_simple_name+" reached the records limit of: "+MAX_RESULTS_PER_REQUEST+", current count is "+counter.intValue()+", last timestamp: "+ParserUtils.formatTimestamp(timestamp));
		    			}
						return;
					}
				}

				ZipEntry ze = new ZipEntry(entry_name);
	    		
	    		if (timestamp!=null) {
	    			long t = timestamp.getTime();
					if (actual_start_timestamp.longValue()==0 || actual_start_timestamp.longValue()>t)
						actual_start_timestamp.set(t);
					if (actual_end_timestamp.longValue()==0 || actual_end_timestamp.longValue()<t)
						actual_end_timestamp.set(t);
					ze.setTime(t);
	    		}
	    		
				try {
					zip_out.putNextEntry(ze);
					if (record!=null && ignorableFieldSetters!=null && ignorableFieldSetters.length>0) {
						for (BiConsumer<Object, Object> setter: ignorableFieldSetters) {
							setter.accept(record, null); // clears all fields that we should not copy
						}
					}
					String json = mapper.writeValueAsString(record);
					IOUtils.copy(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), zip_out);
					counter.increment();
				} catch (IOException e) {
	    			if (full_debug_info) {
	    				log.log(Level.INFO, "SYNC iterating results of "+entity_simple_name+" got exception on instance with ID "+id, e);
	    			}
					if (ErrorUtils.isErrorThreadInterrupted(e))
						break;
					throw new RuntimeException(e);
				}
				
				if (timestamp!=null)
					prev_timestamp = timestamp;
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
	public Stream<?> queryESEntity(Class<?> entity, String timestamp_field, long start, long end) throws IOException {
		
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
    	
    	return SearchUtils.findWithScroll(entity, indexName, elasticsearchClient, searchSourceBuilder->{
    		searchSourceBuilder.query(query);
            searchSourceBuilder.sort(timestamp_field, SortOrder.ASC);    		
    		searchSourceBuilder.size(MAX_RESULTS_PER_REQUEST); // since we are 'scrolling', this is only for determining a 'batch size'
    	});
	}
	
	/**
	 * Queries for ElasticSearch entities with no timestamp information (NULL values)
	 */
	public Stream<?> queryESEntityWithNullTimestamp(Class<?> entity, String timestamp_field) throws IOException {
		
		Document doc_anon = entity.getAnnotation(Document.class);
		String indexName = doc_anon.indexName();
		
		return SearchUtils.findWithScroll(entity, indexName, elasticsearchClient, searchSourceBuilder->{
	    	BoolQueryBuilder query = QueryBuilders.boolQuery();
	    	query = query.mustNot(QueryBuilders.existsQuery(timestamp_field));
	    	searchSourceBuilder.query(query); 
    		searchSourceBuilder.size(MAX_RESULTS_PER_REQUEST); // since we are 'scrolling', this is only for determining a 'batch size'
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
			
			for (Repository<?,?> repo: all_repositories) {
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
	 * The 'template' indicates the template name associated to the index where the validated documents are stored. <BR>
	 * The 'version' indicates the template version.<BR>
	 * The 'start' parameter is the 'unix epoch' of starting instant. <BR>
	 * The 'end' parameter is the 'unix epoch' of end instant.<BR>
	 * Should return files received in this time interval<BR>
	 */
	@Secured({"ROLE_SYNC_OPS"})
	@GetMapping(value = "/sync/validated/{template}/{version}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ApiOperation("Downloads recently validated documents stored in database for synchronization purpose with other CACAO Server.")
	public ResponseEntity<StreamingResponseBody> getValidatedDocuments(
			@ApiParam("The 'template' indicates the template name associated to the index where the documents are stored.") @PathVariable("template") String template_name,
			@ApiParam("The 'version' indicates the template version.") @PathVariable("version") String version,
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
    	
    	if (template_name==null || template_name.trim().length()==0) {
    		throw new InvalidParameter("template="+template_name);
    	}
    	
    	final DocumentTemplate template;
    	
    	if (version!=null && version.trim().length()>0) {
        	Optional<DocumentTemplate> template_version = templateRepository.findByNameAndVersion(template_name, version);
    		if (template_version==null || !template_version.isPresent()) {
    			throw new InvalidParameter("template="+template_name+", version="+version);				
    		}    	
    		template = template_version.get();
    	}
    	else {
    		List<DocumentTemplate> template_versions = templateRepository.findByName(template_name);
    		if (template_versions==null || template_versions.isEmpty()) {
    			throw new InvalidParameter("template="+template_name);				
    		}    		
    		if (template_versions.size()>1) {
    			// if we have more than one possible choice, let's give higher priority to most recent ones
    			template_versions = template_versions.stream().sorted(DocumentTemplate.TIMESTAMP_COMPARATOR).collect(Collectors.toList());
    		}
    		template = template_versions.get(0);
    	}
		
		final String index_name = IndexNamesUtils.formatIndexNameForValidatedData(template);

    	final long end = opt_end.orElseGet(System::currentTimeMillis);
		final String remote_ip_addr = (request!=null && request.getRemoteAddr()!=null && request.getRemoteAddr().trim().length()>0) ? request.getRemoteAddr() : null;

		if (!isSyncPublisherEnabled()) {
			log.log(Level.INFO, "User "+user.getLogin()+" sync request for stored "+template.getName()+" documents starting from timestamp "+start
					+" ("+ParserUtils.formatTimestamp(new Date(start))
					+") and ending at timestamp "+end
					+" ("+ParserUtils.formatTimestamp(new Date(end))
					+") IP ADDRESS: "
					+remote_ip_addr
					+". But it's DISABLED at configuration!");
			throw new GeneralException("SYNC service is disabled!");
		}

		if (!matchSyncPublisherFilterHost(remote_ip_addr)) {
			log.log(Level.INFO, "User "+user.getLogin()+" sync request for stored "+template.getName()+" documents starting from timestamp "+start
					+" ("+ParserUtils.formatTimestamp(new Date(start))
					+") and ending at timestamp "+end
					+" ("+ParserUtils.formatTimestamp(new Date(end))
					+") IP ADDRESS: "
					+remote_ip_addr
					+". But has been REJECTED by the IP address filter!");
			throw new InsufficientPrivilege();			
		}

		log.log(Level.INFO, "User "+user.getLogin()+" sync request for stored "+template.getName()+" documents starting from timestamp "+start
				+" ("+ParserUtils.formatTimestamp(new Date(start))
				+") and ending at timestamp "+end
				+" ("+ParserUtils.formatTimestamp(new Date(end))
				+") IP ADDRESS: "
				+remote_ip_addr);

    	final String streaming_out_filename = index_name+"_"+start.toString()+".zip";
		StreamingResponseBody responseBody = outputStream -> {
			CheckedOutputStream checksum = new CheckedOutputStream(outputStream, new CRC32());
			ZipOutputStream zip_out = new ZipOutputStream(checksum);
			long copied_files = syncIndexedData(index_name, /*timestampFieldName*/ValidatedDataFieldNames.TIMESTAMP.getFieldName(),
					start, end, zip_out);
			zip_out.flush();
			zip_out.finish();
			response.flushBuffer();
			log.log(Level.INFO, "User "+user.getLogin()+" sync request for stored "+template.getName()+" documents starting from timestamp "+start
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
	 * Copies all stored data submitted or changed between two timestamps. May be data produced by the 'validation' phase or by 'ETL' phase
	 */
	public long syncIndexedData(String index_name, String timestampFieldName, long start, long end, ZipOutputStream zip_out) throws IOException {
		
    	SearchRequest searchRequest = new SearchRequest(index_name);
    	BoolQueryBuilder query = QueryBuilders.boolQuery();
		RangeQueryBuilder b = new RangeQueryBuilder(timestampFieldName)
				.from(ParserUtils.formatTimestampES(new Date(start)), /*includeLower*/true)
				.to(ParserUtils.formatTimestampES(new Date(end)), /*includeUpper*/false);
		query = query.must(b);

    	SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
    			.query(query); 
        searchSourceBuilder.from(0);
        searchSourceBuilder.size(MAX_RESULTS_PER_REQUEST);
        searchSourceBuilder.sort(timestampFieldName, SortOrder.ASC);

    	searchRequest.source(searchSourceBuilder);
    	SearchResponse sresp = ESUtils.searchIgnoringNoMapError(elasticsearchClient, searchRequest, index_name);    	

		final ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.registerModule(new JavaTimeModule());

		LongAdder counter = new LongAdder();
		AtomicLong actual_start_timestamp = new AtomicLong();
		AtomicLong actual_end_timestamp = new AtomicLong();
		AtomicLong pending_timestamp = new AtomicLong();
		
		if (sresp!=null) {
			for (SearchHit hit : sresp.getHits()) {
				Map<String, Object> map = hit.getSourceAsMap();
				map.put("id", hit.getId());
				map.remove("_class");
				Object timestamp = map.get(timestampFieldName);
				Date timestamp_as_date = ValidationContext.toDate(timestamp);
				
				if (counter.intValue()>=MAX_RESULTS_PER_REQUEST-1) {
					// Actually we stop at 'MAX_RESULTS_PER_REQUEST-1' in this SYNC block because we need to
					// store the timestamp for the next unread register and we can't search for MAX_RESULTS_PER_REQUEST+1
					// elements
					if (timestamp_as_date!=null) {
						pending_timestamp.set((timestamp_as_date).getTime());
					}
					break;
				}
	
				String entry_name = hit.getId();
				ZipEntry ze = new ZipEntry(entry_name);
				
	    		if (timestamp_as_date!=null) {
	    			long t = timestamp_as_date.getTime();
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
		saveSyncDto(sync_info, zip_out);

		return counter.longValue();
	}

	/**
	 * Downloads recently published (denormalized) data stored in database for synchronization purpose with other CACAO Server.<BR>
	 * The 'indexname' indicates index name associated to published (denormalized) data. <BR>
	 * The 'start' parameter is the 'unix epoch' of starting instant. <BR>
	 * The 'end' parameter is the 'unix epoch' of end instant.<BR>
	 * Should return files received in this time interval<BR>
	 */
	@Secured({"ROLE_SYNC_OPS"})
	@GetMapping(value = "/sync/published/{indexname}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ApiOperation("Downloads recently published (denormalized) data stored in database for synchronization purpose with other CACAO Server.")
	public ResponseEntity<StreamingResponseBody> getPublishedData(
			@ApiParam("The 'indexname' indicates index name associated to published (denormalized) data.") @PathVariable("indexname") String indexname,
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
    	
    	if (indexname==null || indexname.trim().length()==0 || !indexname.startsWith(IndexNamesUtils.PUBLISHED_DATA_INDEX_PREFIX)) {
    		throw new InvalidParameter("indexname="+indexname);
    	}
    	
    	final long end = opt_end.orElseGet(System::currentTimeMillis);
		final String remote_ip_addr = (request!=null && request.getRemoteAddr()!=null && request.getRemoteAddr().trim().length()>0) ? request.getRemoteAddr() : null;

		if (!isSyncPublisherEnabled()) {
			log.log(Level.INFO, "User "+user.getLogin()+" sync request for published "+indexname+" data starting from timestamp "+start
					+" ("+ParserUtils.formatTimestamp(new Date(start))
					+") and ending at timestamp "+end
					+" ("+ParserUtils.formatTimestamp(new Date(end))
					+") IP ADDRESS: "
					+remote_ip_addr
					+". But it's DISABLED at configuration!");
			throw new GeneralException("SYNC service is disabled!");
		}

		if (!matchSyncPublisherFilterHost(remote_ip_addr)) {
			log.log(Level.INFO, "User "+user.getLogin()+" sync request for published "+indexname+" data starting from timestamp "+start
					+" ("+ParserUtils.formatTimestamp(new Date(start))
					+") and ending at timestamp "+end
					+" ("+ParserUtils.formatTimestamp(new Date(end))
					+") IP ADDRESS: "
					+remote_ip_addr
					+". But has been REJECTED by the IP address filter!");
			throw new InsufficientPrivilege();			
		}

		log.log(Level.INFO, "User "+user.getLogin()+" sync request for published "+indexname+" data starting from timestamp "+start
				+" ("+ParserUtils.formatTimestamp(new Date(start))
				+") and ending at timestamp "+end
				+" ("+ParserUtils.formatTimestamp(new Date(end))
				+") IP ADDRESS: "
				+remote_ip_addr);

    	final String streaming_out_filename = indexname+"_"+start.toString()+".zip";
		StreamingResponseBody responseBody = outputStream -> {
			CheckedOutputStream checksum = new CheckedOutputStream(outputStream, new CRC32());
			ZipOutputStream zip_out = new ZipOutputStream(checksum);
			long copied_files = syncIndexedData(indexname, /*timestampFieldName*/PublishedDataFieldNames.ETL_TIMESTAMP.getFieldName(), start, end, zip_out);
			zip_out.flush();
			zip_out.finish();
			response.flushBuffer();
			log.log(Level.INFO, "User "+user.getLogin()+" sync request for published "+indexname+" data starting from timestamp "+start
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
	        	sresp = ESUtils.searchIgnoringNoMapError(elasticsearchClient, searchRequest, index_name);    	
	    	}
	    	catch (Throwable ex) {
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
				} catch (Throwable ex) { return false; }
			}
			else {
				// The filter has some simple form of wildcards, so let's add the regex form
				try {
					return Pattern.compile(filter
							.replace(".","\\.") // treat dots as literals
							.replace("*", ".*"),// treat wildcards as regex wildcards 
							Pattern.CASE_INSENSITIVE).matcher(ip_address).find();
				} catch (Throwable ex) { return false; }				
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
				query_results = queryESEntityWithNullTimestamp(entity, timestamp_field);
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
			} catch (Throwable ex) {
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
	    	SearchResponse sresp = ESUtils.searchIgnoringNoMapError(elasticsearchClient, searchRequest, index_name);
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
	public PaginationData<SyncCommitHistory> getSyncHistory(Model model, @RequestParam("page") Optional<Integer> page,
			@RequestParam("size") Optional<Integer> size, @RequestParam("q") Optional<String> filters_as_json) {
		int currentPage = page.orElse(1);
		int pageSize = ControllerUtils.getPageSizeForUser(size, env);
		Optional<AdvancedSearch> filters = SearchUtils.fromJSON(filters_as_json);
		Page<SyncCommitHistory> commits;
		if (filters.isPresent() && !filters.get().isEmpty()) {
			commits = searchCommitHistory(filters, page, size);
		} else {
			commits = searchPage(() -> syncHistoryRepository
					.findAll(PageRequest.of(currentPage - 1, pageSize, Sort.by("endPoint").ascending())));
		}
		PaginationData<SyncCommitHistory> result = new PaginationData<>(commits.getTotalPages(), commits.getContent());
		return result;
	}

	@JsonView(Views.Public.class)
	@Secured({"ROLE_SYNC_OPS"})
	@GetMapping(value="/sync/milestone", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Method used for listing current stats of synchronizable resources using pagination")
	public PaginationData<SyncCommitMilestone> getSyncMilestone(Model model, @RequestParam("page") Optional<Integer> page,
			@RequestParam("size") Optional<Integer> size, @RequestParam("q") Optional<String> filters_as_json) {
		int currentPage = page.orElse(1);
		int pageSize = ControllerUtils.getPageSizeForUser(size, env);
		Optional<AdvancedSearch> filters = SearchUtils.fromJSON(filters_as_json);
		Page<SyncCommitMilestone> commits;
		if (filters.isPresent() && !filters.get().isEmpty()) {
			commits = searchCommitMilestone(filters, page, size);
		} else {
			commits = searchPage(() -> syncMilestoneRepository
					.findAll(PageRequest.of(currentPage - 1, pageSize, Sort.by("endPoint").ascending())));
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
			Optional<Integer> size) {
		try {
			return SearchUtils.doSearch(filters.get().wiredTo(messageSource), SyncCommitHistory.class, elasticsearchClient, page, size, Optional.of("endPoint"), Optional.of(SortOrder.ASC));
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
			Optional<Integer> size) {
		try {
			return SearchUtils.doSearch(filters.get().wiredTo(messageSource), SyncCommitMilestone.class, elasticsearchClient, page, size, Optional.of("endPoint"), Optional.of(SortOrder.ASC));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}

}
