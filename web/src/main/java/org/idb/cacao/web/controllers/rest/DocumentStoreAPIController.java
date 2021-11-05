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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.web.IncomingFileStorage;
import org.idb.cacao.web.controllers.AdvancedSearch;
import org.idb.cacao.web.controllers.Views;
import org.idb.cacao.web.controllers.dto.PaginationData;
import org.idb.cacao.web.controllers.services.DocumentTemplateService;
import org.idb.cacao.web.controllers.services.UserService;
import org.idb.cacao.web.controllers.services.storage.IStorageService;
import org.idb.cacao.web.entities.DocumentUploaded;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.entities.UserProfile;
import org.idb.cacao.web.errors.DocumentNotFoundException;
import org.idb.cacao.web.errors.GeneralException;
import org.idb.cacao.web.errors.InsufficientPrivilege;
import org.idb.cacao.web.errors.MissingParameter;
import org.idb.cacao.web.errors.UserNotFoundException;
import org.idb.cacao.web.repositories.DocumentTemplateRepository;
import org.idb.cacao.web.repositories.DocumentUploadedRepository;
import org.idb.cacao.web.utils.ControllerUtils;
import org.idb.cacao.web.utils.ErrorUtils;
import org.idb.cacao.web.utils.ParserUtils;
import org.idb.cacao.web.utils.SearchUtils;
import org.idb.cacao.web.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * Controller class for all endpoints related to 'document' object interacting by a REST interface
 * 
 * @author Gustavo Figueiredo
 * @author Luis Kauer
 * @author Rivelino Patrício
 *
 */
@RestController
@RequestMapping("/api")
@Api(description="Controller class for all endpoints related to 'document' object interacting by a REST interface")
public class DocumentStoreAPIController {
	
	private static final Logger log = Logger.getLogger(DocumentStoreAPIController.class.getName());
	
	/**
	 * Name of indexed field with ID for documents. Other documents with same ID are considered 'rectifying documents'
	 */
	public static final String FIELD_DOC_ID = "file_id";
	
	/**
	 * Name of indexed field with boolean telling if documents were rectified by others.
	 */
	public static final String FIELD_DOC_RECTIFIED = "rectified";
	
	/**
	 * Name of indexed field with date/time of record creation or update
	 */
	public static final String FIELD_DOC_TIMESTAMP = "timestamp";
	
	public static final int MAX_RESULTS_PER_REQUEST = 10_000;

	@Autowired
	private MessageSource messageSource;
	
	@Autowired
	private ApplicationContext app;

	@Autowired
	private Environment env;
	
	@Autowired
	private DocumentTemplateRepository templateRepository;
	
	@Autowired
	private DocumentTemplateService templateService;
	
    @Autowired
    private IStorageService storageService;

	@Autowired
	private RestHighLevelClient elasticsearchClient;
	
	@Autowired
	private DocumentUploadedRepository documentsUploadedRepository;

	@Autowired
	private UserService userService;

    /**
     * Endpoint for uploading a document to be parsed
     */
	@PostMapping(value="/doc",produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation("Endpoint for uploading a document to be parsed")
	public ResponseEntity<Map<String,String>> handleFileUpload(
			@RequestParam("fileinput") MultipartFile fileinput,
			@RequestParam("template") String template,
			RedirectAttributes redirectAttributes,
			HttpServletRequest request) {

		log.log(Level.FINE, "Incoming file (upload): "+fileinput.getOriginalFilename()+", size: "+fileinput.getSize());

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();

    	User user = userService.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();

		if (fileinput.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", 
					messageSource.getMessage("upload.failed.empty.file", null, LocaleContextHolder.getLocale())));
		}
		
		if (template==null || template.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", 
					messageSource.getMessage("upload.failed.empty.file", null, LocaleContextHolder.getLocale())));			
		}
		List<DocumentTemplate> template_versions = templateRepository.findByName(template);
		if (template_versions==null || template_versions.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", 
					messageSource.getMessage("unknown_template", null, LocaleContextHolder.getLocale())));						
		}
		if (template_versions.size()>1) {
			// if we have more than one possible choice, let's give higher priority to most recent ones
			template_versions = template_versions.stream().sorted(DocumentTemplate.TIMESTAMP_COMPARATOR).collect(Collectors.toList());
		}
		
		final String remote_ip_addr = (request!=null && request.getRemoteAddr()!=null && request.getRemoteAddr().trim().length()>0) ? request.getRemoteAddr() : null;
		
		final Map<String, String> result;
			
		try (InputStream input_stream=fileinput.getInputStream()) {
			result = uploadFile(fileinput.getOriginalFilename(), input_stream, /*closeInputStream*/true, template, remote_ip_addr, user);
		}
		catch (GeneralException ex) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", ex.getMessage()));	
		}
		catch (IOException ex) {
			log.log(Level.SEVERE, "Failed upload "+fileinput.getOriginalFilename(), ex);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", 
					messageSource.getMessage("upload.failed.file", new Object[] {fileinput.getOriginalFilename()}, LocaleContextHolder.getLocale())));
		}
		return ResponseEntity.ok(result);
	}

    /**
     * Endpoint for uploading many documents to be parsed in one ZIP file
     */
	@PostMapping(value="/docs_zip",produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation("Endpoint for uploading many documents to be parsed in one ZIP file")
	public ResponseEntity<Map<String,String>> handleFileUploadZIP(
			@RequestParam("filezip") MultipartFile filezip,
			@RequestParam("template") String template,
			RedirectAttributes redirectAttributes,
			HttpServletRequest request) {

		log.log(Level.FINE, "Incoming files (upload): "+filezip.getOriginalFilename()+", size: "+filezip.getSize());

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();

    	User user = userService.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();

		if (filezip.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", 
					messageSource.getMessage("upload.failed.empty.file", null, LocaleContextHolder.getLocale())));
		}
		
		if (template==null || template.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", 
					messageSource.getMessage("upload.failed.empty.file", null, LocaleContextHolder.getLocale())));			
		}
		List<DocumentTemplate> template_versions = templateRepository.findByName(template);
		if (template_versions==null || template_versions.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", 
					messageSource.getMessage("unknown_template", null, LocaleContextHolder.getLocale())));						
		}
		if (template_versions.size()>1) {
			// if we have more than one possible choice, let's give higher priority to most recent ones
			template_versions = template_versions.stream().sorted(DocumentTemplate.TIMESTAMP_COMPARATOR).collect(Collectors.toList());
		}
		
		final String remote_ip_addr = (request!=null && request.getRemoteAddr()!=null && request.getRemoteAddr().trim().length()>0) ? request.getRemoteAddr() : null;

		final List<Map<String, String>> results = new ArrayList<>();
		
		try (ZipInputStream zip_stream=new ZipInputStream(filezip.getInputStream())){
			ZipEntry ze;
			
			while ((ze = zip_stream.getNextEntry()) != null) {
				
				Map<String, String> result = uploadFile(ze.getName(), zip_stream, /*closeInputStream*/false, template, remote_ip_addr, user);
				results.add(result);
			}
		}
		catch (GeneralException ex) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", ex.getMessage()));	
		}
		catch (IOException ex) {
			log.log(Level.SEVERE, "Failed upload "+filezip.getOriginalFilename(), ex);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", 
					messageSource.getMessage("upload.failed.file", new Object[] {filezip.getOriginalFilename()}, LocaleContextHolder.getLocale())));
		}
		
		// TODO: retornar a situação de todos arquivos
		return ResponseEntity.ok(results.get(0));
	}
	
    /**
     * Endpoint for uploading many separate document to be parsed
     */
	@PostMapping(value="/docs",produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation("Endpoint for uploading many separate document to be parsed")
	public ResponseEntity<Map<String,String>> handleFilesUpload(
			@RequestParam("files") MultipartFile[] files,
			@RequestParam("template") String template,
			RedirectAttributes redirectAttributes,
			HttpServletRequest request) {

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();

    	User user = userService.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();

		if (files==null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", 
					messageSource.getMessage("upload.failed.empty.file", null, LocaleContextHolder.getLocale())));			
		}

		final long all_sizes = Arrays.stream(files).mapToLong(MultipartFile::getSize).sum();
		log.log(Level.FINE, "Incoming files (upload): "+files.length+", size: "+all_sizes);
		
		if (files.length==0) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", 
					messageSource.getMessage("upload.failed.empty.file", null, LocaleContextHolder.getLocale())));			
		}
		
		if (template==null || template.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", 
					messageSource.getMessage("upload.failed.empty.file", null, LocaleContextHolder.getLocale())));			
		}
		List<DocumentTemplate> template_versions = templateRepository.findByName(template);
		if (template_versions==null || template_versions.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", 
					messageSource.getMessage("unknown_template", null, LocaleContextHolder.getLocale())));						
		}
		if (template_versions.size()>1) {
			// if we have more than one possible choice, let's give higher priority to most recent ones
			template_versions = template_versions.stream().sorted(DocumentTemplate.TIMESTAMP_COMPARATOR).collect(Collectors.toList());
		}
		
		final String remote_ip_addr = (request!=null && request.getRemoteAddr()!=null && request.getRemoteAddr().trim().length()>0) ? request.getRemoteAddr() : null;

		List<Map<String, String>> results = new ArrayList<>();
		
		for (MultipartFile fileinput: files) {
			
			try (InputStream input_stream=fileinput.getInputStream()) {
				Map<String,String> result = uploadFile(fileinput.getOriginalFilename(), input_stream, /*closeInputStream*/true, template, remote_ip_addr, user);
				results.add(result);
			}
			catch (GeneralException ex) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", ex.getMessage()));	
			}
			catch (IOException ex) {
				log.log(Level.SEVERE, "Failed upload "+fileinput.getOriginalFilename(), ex);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", 
						messageSource.getMessage("upload.failed.file", new Object[] {fileinput.getOriginalFilename()}, LocaleContextHolder.getLocale())));
			}

		}
		// TODO: Retornar a situação de cada arquivo
		return ResponseEntity.ok(results.get(0));
	}
	
	/**
	 * Upload a file 
	 */
	@Transactional
	private Map<String, String> uploadFile(final String originalFilename, 
			final InputStream fileStream, 
			final boolean closeInputStream,
			final String template, 
			final String remoteIpAddr,
			final User user) throws IOException, GeneralException {
		File temp_file1 = null;
		List<Runnable> rollback_procedures = new LinkedList<>(); // hold rollback procedures only to be used in case of error
		try {
			
			log.log(Level.INFO, "User "+user.getLogin()+" uploading file "+originalFilename+" for template "+template+" from "+remoteIpAddr);

			String fileId = UUID.randomUUID().toString();
			
			final long timestamp = System.currentTimeMillis();
			HashingInputStream his = new HashingInputStream(Hashing.sha256(),fileStream);
			String subDir = storageService.store(fileId, his, closeInputStream);
								
			// Keep this information in history of all uploads
			DocumentUploaded regUpload = new DocumentUploaded();
			regUpload.setTemplateName(template);
			regUpload.setFileId(fileId);
			regUpload.setFilename(originalFilename);
			regUpload.setSubDir(subDir);
			regUpload.setTimestamp(OffsetDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()));
			regUpload.setIpAddress(remoteIpAddr);
			regUpload.setHash(his.hash().toString()); 
	    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
	    	if (auth!=null) {
	    		regUpload.setUser(String.valueOf(auth.getName()));
	    	}
			DocumentUploaded saved_info = documentsUploadedRepository.saveWithTimestamp(regUpload);
			
			rollback_procedures.add(()->documentsUploadedRepository.delete(saved_info)); // in case of error delete the DocumentUploaded
			Map<String, String> result = new HashMap<>();
			result.put("result", "ok");
			result.put("file_id", fileId);
			
			return result; 
		}
		catch (GeneralException ex) {
			callRollbackProcedures(rollback_procedures);
			throw ex;
		}
		finally {
			if (temp_file1!=null && temp_file1.exists()) {
				if (!temp_file1.delete()) {
					log.log(Level.WARNING, "Could not delete temporary file "+temp_file1.getAbsolutePath()+" created from incoming file "+originalFilename);
				}
			}
		}
	}
	
	public static void callRollbackProcedures(Collection<Runnable> rollback_procedures) {
		if (rollback_procedures==null || rollback_procedures.isEmpty())
			return;
		for (Runnable proc: rollback_procedures) {
			try {
				proc.run();
			}
			catch (Throwable ex) {
				log.log(Level.SEVERE, "Could not rollback", ex);
			}
		}
	}
	
	/**
	 * Downloads original file given the id stored in ElasticSearch
	 */
	@GetMapping(value = "/doc_contents/{id}/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ApiOperation("Downloads original file given the id stored in ElasticSearch")
	public FileSystemResource getFile(@PathVariable("id") String id, HttpServletResponse response) throws Exception {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = userService.getUser(auth);
    	
    	Optional<DocumentUploaded> ref_doc = documentsUploadedRepository.findById(id);
    	if (!ref_doc.isPresent()) {
    		throw new DocumentNotFoundException();
    	}
    	
    	// Check permission
    	
    	Collection<? extends GrantedAuthority> roles = auth.getAuthorities();
    	boolean is_sysadmin_or_officer = roles.stream().anyMatch(a->UserProfile.SYSADMIN.getRole().equalsIgnoreCase(a.getAuthority())
    			|| UserProfile.MASTER.getRole().equalsIgnoreCase(a.getAuthority())
    			|| UserProfile.SUPPORT.getRole().equalsIgnoreCase(a.getAuthority())
    			|| UserProfile.AUTHORITY.getRole().equalsIgnoreCase(a.getAuthority()));
    	if (!is_sysadmin_or_officer && ref_doc.get().getUser()!=null) {
    		if (!ref_doc.get().getUser().equalsIgnoreCase(String.valueOf(auth.getName()))
    			&& !userService.isUserAuthorizedForSubject(user, ref_doc.get().getTaxPayerId()))
    			throw new InsufficientPrivilege();
    	}
    	
    	// Locate file in file repository
    	
    	String filename = ref_doc.get().getFilename();
    	if (filename==null || filename.trim().length()==0) {
    		throw new FileNotFoundException("");
    	}
    	
    	OffsetDateTime timestamp = ref_doc.get().getTimestamp();

		IncomingFileStorage file_storage = app.getBean(IncomingFileStorage.class);
		File file = file_storage.getOriginalFile(filename, timestamp);
		if (file==null || !file.exists()) {
			throw new FileNotFoundException(filename);
		}

	    response.setHeader("Content-Disposition", "attachment; filename=" + file.getName());
	    return new FileSystemResource(file);
	}
	
	/**
	 * Get a proper index name for using in ElasticSearch.
	 * This implementations removes diacritical marks, replace spaces with underlines, turn everyting lower case.
	 */
	public static String formatIndexName(String indexName) {
		if (indexName==null || indexName.trim().length()==0)
			return "generic";
		indexName =
		Normalizer.normalize(indexName, Normalizer.Form.NFD).replaceAll("[\\p{InCombiningDiacriticalMarks}]", "") // remove acentuação
			.trim()
			.replaceAll("[^A-Za-z\\d ]", " ") // remove tudo que não é letra, nem número, nem espaço em branco
			.replaceAll("\\s+", "_")
			.toLowerCase();
		if (indexName==null || indexName.trim().length()==0)
			return "generic";
		return indexName;
	}
	
	/**
	 * Search documents upload with pagination and filters
	 * @return
	 */
	@JsonView(Views.Declarant.class)
	@GetMapping(value="/docs_search", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Method used for listing documents uploaded using pagination")
	public PaginationData<DocumentUploaded> getDocsWithPagination(Model model, @RequestParam("page") Optional<Integer> page,
			@RequestParam("size") Optional<Integer> size, @RequestParam("q") Optional<String> filters_as_json) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();

    	// Only SYSADMIN users may see every documents. Other users are restricted to their relationships
    	final Set<String> filter_taxpayers_ids;
    	try {
    		filter_taxpayers_ids = userService.getFilteredTaxpayersForUserAsManager(auth);
    	}
    	catch (MissingParameter missing) {
    		/*
			model.addAttribute("message",messageSource.getMessage("user_missing_taxpayerid", null, LocaleContextHolder.getLocale()));
			model.addAttribute("docs", Page.empty());
			model.addAttribute("filter_options", new AdvancedSearch());
			model.addAttribute("applied_filters", Optional.empty());
			*/
			return null;
    	}

		Optional<AdvancedSearch> filters = SearchUtils.fromJSON(filters_as_json);
		Page<DocumentUploaded> docs;
		try {
			if (filter_taxpayers_ids!=null && filter_taxpayers_ids.isEmpty())
				docs = Page.empty();
			else if (filter_taxpayers_ids==null)
				docs = SearchUtils.doSearch(filters.orElse(new AdvancedSearch()), DocumentUploaded.class, elasticsearchClient, page, size, Optional.of("timestamp"), Optional.of(SortOrder.DESC));
			else
				docs = SearchUtils.doSearch(filters.orElse(new AdvancedSearch()).clone()
						.withFilter(new AdvancedSearch.QueryFilterList("taxPayerId.keyword", filter_taxpayers_ids)), 
						DocumentUploaded.class, elasticsearchClient, page, size, Optional.of("timestamp"), Optional.of(SortOrder.DESC));
		}
		catch (Exception ex) {
			log.log(Level.SEVERE, "Error while searching for all documents", ex);
			docs = Page.empty();
		}		
		PaginationData<DocumentUploaded> result = new PaginationData<>(docs.getTotalPages(), docs.getContent());
		return result;
	}
	
	
	/**
	 * Return document uploads records via API. Admits some optional parameters:<BR>
	 * from: Date/time for first upload<BR>
	 * to: Date/time for last upload<BR>
	 * days: Number of past days (overrides 'from' and 'to' parameters)
	 */
	@Secured({"ROLE_SYSADMIN","ROLE_SUPPORT"})
	@GetMapping(value="/docs_uploads",produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Return document uploads records via API",response=DocumentUploaded[].class)
	public ResponseEntity<Object> getDocsUploads(
			@ApiParam(value="Date/time for first upload",required=false) @RequestParam("fromDate") Optional<String> fromDate,
			@ApiParam(value="Date/time for last upload",required=false) @RequestParam("toDate") Optional<String> toDate,
			@ApiParam(value="Number of past days (overrides 'from' and 'to' parameters)",required=false) @RequestParam("days") Optional<String> days) {
		
    	SearchRequest searchRequest = new SearchRequest("docs_uploaded");
    	BoolQueryBuilder query = QueryBuilders.boolQuery();
    	if (days!=null && days.isPresent() && days.get().trim().length()>0) {
	    	RangeQueryBuilder timestamp_query = new RangeQueryBuilder("timestamp");
	    	Calendar cal = Calendar.getInstance();
	    	cal.add(Calendar.DAY_OF_YEAR, -Integer.parseInt(days.get()));
    		timestamp_query.from(ParserUtils.formatTimestampES(cal.getTime()), true);
	    	query.must(timestamp_query);
    	}
    	else if ((fromDate!=null && fromDate.isPresent()) || (toDate!=null && toDate.isPresent())) {
	    	RangeQueryBuilder timestamp_query = new RangeQueryBuilder("timestamp");
	    	if (fromDate!=null && fromDate.isPresent())
	    		timestamp_query.from(ParserUtils.formatTimestampES(ParserUtils.parseFlexibleDate(fromDate.get())), true);
	    	if (toDate!=null && toDate.isPresent())
	    		timestamp_query.to(ParserUtils.formatTimestampES(ParserUtils.parseFlexibleDate(toDate.get())), true);
	    	query.must(timestamp_query);
    	}
    	
    	SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
    			.query(query); 
    	searchSourceBuilder.size(MAX_RESULTS_PER_REQUEST);    	
    	searchSourceBuilder.sort("timestamp", SortOrder.DESC);
    	
    	searchRequest.source(searchSourceBuilder);
    	SearchResponse sresp = null;    	
		try {
			sresp = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
		} catch (IOException ex) {
			log.log(Level.SEVERE,"Return documents uploads failed", ex);
			return ResponseEntity.badRequest().body(messageSource.getMessage("op.failed", null, LocaleContextHolder.getLocale()));
		}
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.setSerializationInclusion(Include.NON_NULL);

		List<DocumentUploaded> docs = new ArrayList<>(sresp.getHits().getHits().length);
		for (SearchHit hit : sresp.getHits()) {
			Map<String, Object> map = hit.getSourceAsMap();
			DocumentUploaded doc = mapper.convertValue(map, DocumentUploaded.class);
			docs.add(doc);
		}

		return ResponseEntity.ok().body(docs);
	}

	/**
	 * Return documents (their parsed contents) via API for a given template. The template may be identified by 
	 * the internal numerical ID, or by its name (in this case, replace all spaces and symbols with underlines). 
	 * Admits some optional parameters:<BR>
	 * from: Date/time for first day/time of document receipt<BR>
	 * to: Date/time for last day/time of document receipt<BR>
	 * days: Number of past days (overrides 'from' and 'to' parameters)
	 */
	@Secured({"ROLE_SYSADMIN","ROLE_SUPPORT"})
	@GetMapping(value="/docs/{template_name}",produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Return documents (their parsed contents) via API for a given template. The template may be identified by the internal numerical ID, "
			+ "or by its name (in this case, replace all spaces and symbols with underlines).")
	public ResponseEntity<Object> getDocs(
			@PathVariable("template_name") String template_name,
			@ApiParam(value="Date/time for first day/time of document receipt",required=false) @RequestParam("fromDate") Optional<String> fromDate,
			@ApiParam(value="Date/time for last day/time of document receipt",required=false) @RequestParam("toDate") Optional<String> toDate,
			@ApiParam(value="Number of past days (overrides 'from' and 'to' parameters)",required=false) @RequestParam("days") Optional<String> days) {
		
		// Parse the 'template_name' informed at request path
		if (template_name==null || template_name.trim().length()==0) {
			throw new MissingParameter("template_name");
		}
		List<DocumentTemplate> requested_templates = templateService.findTemplateWithNameOrId(template_name);
		if (requested_templates.isEmpty()) {
			return ResponseEntity.ok().body(Collections.emptyList());
		}
		
		// Parse the optional temporal query parameters
    	BoolQueryBuilder query = QueryBuilders.boolQuery();
    	if (days!=null && days.isPresent() && days.get().trim().length()>0) {
	    	RangeQueryBuilder timestamp_query = new RangeQueryBuilder("timestamp");
	    	Calendar cal = Calendar.getInstance();
	    	cal.add(Calendar.DAY_OF_YEAR, -Integer.parseInt(days.get()));
    		timestamp_query.from(ParserUtils.formatTimestampES(cal.getTime()), true);
	    	query.must(timestamp_query);
    	}
    	else if ((fromDate!=null && fromDate.isPresent()) || (toDate!=null && toDate.isPresent())) {
	    	RangeQueryBuilder timestamp_query = new RangeQueryBuilder("timestamp");
	    	if (fromDate!=null && fromDate.isPresent())
	    		timestamp_query.from(ParserUtils.formatTimestampES(ParserUtils.parseFlexibleDate(fromDate.get())), true);
	    	if (toDate!=null && toDate.isPresent())
	    		timestamp_query.to(ParserUtils.formatTimestampES(ParserUtils.parseFlexibleDate(toDate.get())), true);
	    	query.must(timestamp_query);
    	}
    	
    	Set<String> indices_verified = new HashSet<>(); // avoids redundancy (may happen if we have different versions of the same template name)
    	
    	// Build the query for each template of interest
    	List<Object> allDocs = new LinkedList<>();
    	for (DocumentTemplate template: requested_templates) {
    		
    		if (allDocs.size()>=MAX_RESULTS_PER_REQUEST)
    			break;
    		
        	SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
        			.query(query); 
        	searchSourceBuilder.size(MAX_RESULTS_PER_REQUEST - allDocs.size());    	
        	searchSourceBuilder.sort("timestamp", SortOrder.DESC);
        	
        	String index_name = "doc_"+formatIndexName(/*indexName*/template.getName());
        	
        	if (indices_verified.contains(index_name))
        		continue;
        	indices_verified.add(index_name);
        	
        	SearchRequest searchRequest = new SearchRequest(index_name);
        	searchRequest.source(searchSourceBuilder);
        	SearchResponse sresp = null;    	
    		try {
    			sresp = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
    		} catch (Throwable ex) {
    			if (ErrorUtils.isErrorNoIndexFound(ex))
    				continue;
    			boolean fixed = false;
    			if (ErrorUtils.isErrorNoMappingFoundForColumn(ex)) {
    				// maybe the 'timestamp' is missing in this index (probably a 'legacy index' at test environment)
    				// let's try again without the timestamp sorting
    				searchSourceBuilder.sorts().clear();
    				try {
    					sresp = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
    					fixed = true;
    				}
    				catch (IOException ex2) {
    					// will throw original exception
    				}
    			}
    			if (!fixed) {
	    			log.log(Level.SEVERE,"Return documents uploads failed", ex);
	    			return ResponseEntity.badRequest().body(messageSource.getMessage("op.failed", null, LocaleContextHolder.getLocale()));
    			}
    		}
    		
    		for (SearchHit hit : sresp.getHits()) {
    			Map<String, Object> docAsMapOfFields = hit.getSourceAsMap();
    			docAsMapOfFields.put("docId", hit.getId());
    			allDocs.add(docAsMapOfFields);
    		}
    		
    	} // LOOP for each DocumentTemplate

		return ResponseEntity.ok().body(allDocs);
	}

}