/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.rest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.OffsetDateTime;
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
import java.util.zip.ZipInputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FilenameUtils;
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
import org.idb.cacao.api.DocumentSituation;
import org.idb.cacao.api.DocumentSituationHistory;
import org.idb.cacao.api.DocumentUploaded;
import org.idb.cacao.api.DocumentValidationErrorMessage;
import org.idb.cacao.api.Views;
import org.idb.cacao.api.errors.DocumentNotFoundException;
import org.idb.cacao.api.errors.GeneralException;
import org.idb.cacao.api.storage.IStorageService;
import org.idb.cacao.api.templates.DocumentInput;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.utils.DateTimeUtils;
import org.idb.cacao.api.utils.IndexNamesUtils;
import org.idb.cacao.api.utils.ParserUtils;
import org.idb.cacao.web.controllers.AdvancedSearch;
import org.idb.cacao.web.controllers.services.DocumentTemplateService;
import org.idb.cacao.web.controllers.services.FileUploadedProducer;
import org.idb.cacao.web.controllers.services.MessagesService;
import org.idb.cacao.web.controllers.services.SaveDataToParquet;
import org.idb.cacao.web.controllers.services.UserService;
import org.idb.cacao.web.dto.FileUploadedEvent;
import org.idb.cacao.web.dto.PaginationData;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.entities.UserProfile;
import org.idb.cacao.web.errors.InsufficientPrivilege;
import org.idb.cacao.web.errors.MissingParameter;
import org.idb.cacao.web.errors.PresentationDisabledFeature;
import org.idb.cacao.web.errors.UserNotFoundException;
import org.idb.cacao.web.repositories.DocumentSituationHistoryRepository;
import org.idb.cacao.web.repositories.DocumentTemplateRepository;
import org.idb.cacao.web.repositories.DocumentUploadedRepository;
import org.idb.cacao.web.utils.ErrorUtils;
import org.idb.cacao.web.utils.SearchUtils;
import org.idb.cacao.web.utils.UserUtils;
import org.idb.cacao.web.utils.ZipConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.tags.Tag;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Controller class for all endpoints related to 'document' object interacting
 * by a REST interface
 * 
 * @author Gustavo Figueiredo
 * @author Luis Kauer
 * @author Rivelino Patrício
 *
 */
@RestController
@RequestMapping("/api")
@Tag(name="document-store-api-controller", description="Controller class for all endpoints related to 'document' object interacting by a REST interface")
public class DocumentStoreAPIController {

	private static final Logger log = Logger.getLogger(DocumentStoreAPIController.class.getName());

	/**
	 * Name of indexed field with ID for documents. Other documents with same ID are
	 * considered 'rectifying documents'
	 */
	public static final String FIELD_DOC_ID = "file_id";

	/**
	 * Name of indexed field with boolean telling if documents were rectified by
	 * others.
	 */
	public static final String FIELD_DOC_RECTIFIED = "rectified";

	public static final int MAX_RESULTS_PER_REQUEST = 10_000;

	@Autowired
	private MessageSource messageSource;

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
	private DocumentSituationHistoryRepository documentsSituationHistoryRepository;

	@Autowired
	private UserService userService;

	@Autowired
	private FileUploadedProducer fileUploadedProducer;

	@Autowired
	private MessagesService messagesService;
	
	@Autowired SaveDataToParquet saveDataToParquet;
	
	/**
	 * Maximum number of entries it expects to find in incoming ZIP file
	 */
	@Value("${max.entries.per.uploaded.zip}")
	private int maxEntriesPerUploadedZip;
	
	@Value("${presentation.mode}")
	private Boolean presentationMode;

	/**
	 * Endpoint for uploading a document to be parsed
	 */
	@Secured({"ROLE_TAX_DECLARATION_WRITE"})
	@PostMapping(value = "/doc", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation("Endpoint for uploading a document to be parsed")
	public ResponseEntity<Map<String, String>> handleFileUpload(
			@RequestParam("fileinput") MultipartFile fileinput,
			@ApiParam(name = "Template name for file being uploaded", allowEmptyValue = false, allowMultiple = false, example = "Chart of Accunts", required = true, type = "String")
			@RequestParam("templateName") String templateName, 
			@ApiParam(name = "Template version for file being uploaded", allowEmptyValue = false, allowMultiple = false, example = "1.0", required = true, type = "String")
			@RequestParam("templateVersion") String templateVersion,
			@ApiParam(name = "Input name for file being uploaded", allowEmptyValue = false, allowMultiple = false, example = "CSV", required = true, type = "String")
			@RequestParam("inputName") String inputName,			
			RedirectAttributes redirectAttributes,
			HttpServletRequest request) {
		
		if (Boolean.TRUE.equals(presentationMode)) {
			throw new PresentationDisabledFeature();
		}

		if (log.isLoggable(Level.FINE)) {
			log.log(Level.FINE,
				String.format("Incoming file (upload): %s, size: %d", fileinput.getOriginalFilename(), fileinput.getSize()));
		}

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null)
			throw new UserNotFoundException();

		User user = userService.getUser(auth);
		if (user == null)
			throw new UserNotFoundException();

		if (fileinput.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error",
					messageSource.getMessage("upload.failed.empty.file", null, LocaleContextHolder.getLocale())));
		}

		if (templateName == null || templateName.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error",
					messageSource.getMessage("upload.failed.missing.template", null, LocaleContextHolder.getLocale())));
		}
		
		if (templateVersion == null || templateVersion.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error",
					messageSource.getMessage("upload.failed.missing.template.version", null, LocaleContextHolder.getLocale())));
		}
		
		if (inputName == null || inputName.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error",
					messageSource.getMessage("upload.failed.missing.input.name", null, LocaleContextHolder.getLocale())));
		}		

		List<DocumentTemplate> templates = templateRepository.findByName(templateName);
		if (templates == null || templates.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error",
					messageSource.getMessage("unknown.template", null, LocaleContextHolder.getLocale())));
		}
		if (templates.size() > 1) {
			// if we have more than one possible choice, let's give higher priority to most
			// recent ones
			templates = templates.stream().sorted(DocumentTemplate.TIMESTAMP_COMPARATOR)
					.collect(Collectors.toList());
		}

		// Try to validate template version
		boolean versionFound = false;
		for (DocumentTemplate doc : templates) {
			if (templateVersion.equalsIgnoreCase(doc.getVersion())) {
				versionFound = true;
				break;
			}
		}

		// If version is not found, return error
		if (!versionFound) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", messageSource
					.getMessage("upload.failed.missing.template.version", null, LocaleContextHolder.getLocale())));
		}
		
		//Try to validade document input
		boolean inputFound = false;
		for (DocumentTemplate doc : templates) {
			
			List<DocumentInput> inputs = doc.getInputs();
			
			if (inputs != null && !inputs.isEmpty()) {
				
				for ( DocumentInput input : inputs ) {
			
					if (inputName.equalsIgnoreCase(input.getInputName())) {
						inputFound = true;
						break;
					}
				}
			}
		}

		// If inputName is not found, return error
		if (!inputFound) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", messageSource
					.getMessage("upload.failed.missing.input.name", null, LocaleContextHolder.getLocale())));
		}

		final String remoteIpAddr = (request != null && request.getRemoteAddr() != null
				&& request.getRemoteAddr().trim().length() > 0) ? request.getRemoteAddr() : null;

		List<Map<String, String>> results = new LinkedList<>();		
		
		final String originalFileName = fileinput.getOriginalFilename();
		
		try {
		
			//Upload a file(s) within zip file
			if ( originalFileName != null && FilenameUtils.isExtension(originalFileName.toLowerCase(), "zip") ) {
				
				//Need to reopen stream				
				try (ZipInputStream zipStream = new ZipInputStream(fileinput.getInputStream())) {
					
					ZipConsumer.ConsumeContents consumeZipContents = (ze, input)->
						results.add(uploadFile(ze.getName(), zipStream, /* closeInputStream */false, 
								templateName, templateVersion, inputName, remoteIpAddr, user));
	
					ZipConsumer zipConsumer = new ZipConsumer(zipStream::getNextEntry, zipStream, consumeZipContents)
							.threadholdEntries(maxEntriesPerUploadedZip);
					
					zipConsumer.run();
	
				}
				
			}
			//Upload a single file
			else {
				results.add(uploadFile(originalFileName, fileinput.getInputStream(), /* closeInputStream */true, 
							templateName, templateVersion, inputName, remoteIpAddr, user));
			}
			
		}
		catch (GeneralException ex) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Collections.singletonMap("error", ex.getMessage()));
		}
		catch (IOException ex) {
			log.log(Level.SEVERE, String.format("Failed upload %s", fileinput.getOriginalFilename()), ex);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Collections.singletonMap("error", messageSource.getMessage("upload.failed.file",
							new Object[] { fileinput.getOriginalFilename() }, LocaleContextHolder.getLocale())));
		}
		
		return ResponseEntity.ok(results.get(0));
	}

	/**
	 * Upload a file to database
	 * @param originalFilename	File name
	 * @param fileStream		File stream contents
	 * @param closeInputStream	Indicates if stream must be closed after method finishes
	 * @param template			Template name
	 * @param templateVersion	Template version
	 * @param inputName			Input mapping name
	 * @param remoteIpAddr		Remote ip address (who uploaded file)
	 * @param user				User name who upload file
	 * @return					An indication if file was saved correctly
	 * @throws GeneralException
	 */
	@ApiIgnore
	private Map<String, String> uploadFile(final String originalFilename, final InputStream fileStream,
			final boolean closeInputStream, final String template, final String templateVersion,
			final String inputName, final String remoteIpAddr, final User user) throws GeneralException {
		// Hold rollback procedures only to be used in case of error
		List<Runnable> rollbackProcedures = new LinkedList<>(); 
		try {
			if (log.isLoggable(Level.INFO)) {
				log.log(Level.INFO, String.format("User %s uploading file %s for template %s from %s",
						user.getLogin(), originalFilename, template, remoteIpAddr));
			}

			String fileId = UUID.randomUUID().toString();

			final OffsetDateTime timestamp = DateTimeUtils.now();
			HashingInputStream his = new HashingInputStream(Hashing.sha256(), fileStream);
			String subDir = storageService.store(fileId, his, closeInputStream);

			// Keep this information in history of all uploads
			DocumentUploaded regUpload = new DocumentUploaded();
			regUpload.setTemplateName(template);
			regUpload.setTemplateVersion(templateVersion);
			regUpload.setInputName(inputName);
			regUpload.setFileId(fileId);
			regUpload.setFilename(originalFilename);
			regUpload.setSubDir(subDir);
			regUpload.setTimestamp(timestamp);
			regUpload.setIpAddress(remoteIpAddr);
			regUpload.setHash(his.hash().toString());
			regUpload.setSituation(DocumentSituation.RECEIVED);
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth != null) {
				regUpload.setUser(String.valueOf(auth.getName()));
			}
			if (user != null) {
				regUpload.setUserLogin(user.getLogin());
				regUpload.setTaxPayerId(user.getTaxpayerId());
			}
			DocumentUploaded savedInfo = documentsUploadedRepository.saveWithTimestamp(regUpload);

			DocumentSituationHistory situationHistory = new DocumentSituationHistory();
			situationHistory.setDocumentId(savedInfo.getId());
			situationHistory.setDocumentFilename(savedInfo.getFilename());
			situationHistory.setTemplateName(savedInfo.getTemplateName());
			situationHistory.setSituation(DocumentSituation.RECEIVED);
			situationHistory.setTimestamp(timestamp);

			DocumentSituationHistory savedSituation = documentsSituationHistoryRepository
					.saveWithTimestamp(situationHistory);

			// in case of error delete the DocumentUploaded
			rollbackProcedures.add(() -> documentsUploadedRepository.delete(savedInfo)); 
			// in case of error delete the DocumentUploaded
			rollbackProcedures.add(() -> documentsSituationHistoryRepository.delete(savedSituation)); 
			Map<String, String> result = new HashMap<>();
			result.put("result", "ok");
			result.put(FIELD_DOC_ID, fileId);

			FileUploadedEvent event = new FileUploadedEvent();
			event.setFileId(savedInfo.getId());
			fileUploadedProducer.fileUploaded(event);

			return result;
		} catch (GeneralException ex) {
			callRollbackProcedures(rollbackProcedures);
			throw ex;
		}
	}

	public static void callRollbackProcedures(Collection<Runnable> rollbackProcedures) {
		if (rollbackProcedures == null || rollbackProcedures.isEmpty())
			return;
		for (Runnable proc : rollbackProcedures) {
			try {
				proc.run();
			} catch (Exception ex) {
				log.log(Level.SEVERE, "Could not rollback", ex);
			}
		}
	}

	/**
	 * Get a proper index name for using in ElasticSearch. This implementations
	 * removes diacritical marks, replace spaces with underlines, turn everyting
	 * lower case.
	 */
	public static String formatIndexName(String indexName) {
		if (indexName == null || indexName.trim().length() == 0)
			return "generic";
		indexName = Normalizer.normalize(indexName, Normalizer.Form.NFD)
				.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "") // remove acentuação
				.trim().replaceAll("[^A-Za-z\\d ]", " ") // remove tudo que não é letra, nem número, nem espaço em
															// branco
				.replaceAll("\\s+", "_").toLowerCase();
		if (indexName == null || indexName.trim().length() == 0)
			return "generic";
		return indexName;
	}

	/**
	 * Check if user can access any uploaded document 
	 */
	private boolean canReadAll(Authentication auth) {
		return auth.getAuthorities().stream().anyMatch( 
				r -> { return r.getAuthority().equals("ROLE_TAX_DECLARATION_READ_ALL");});
	}
	
	/**
	 * Search documents upload with pagination and filters
	 * 
	 * @return
	 */
	@JsonView(Views.Declarant.class)
	@Secured({"ROLE_TAX_DECLARATION_READ"})
	@GetMapping(value = "/docs-search", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Method used for listing documents uploaded using pagination")
	public PaginationData<DocumentUploaded> getDocsWithPagination(Model model,
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
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null)
			throw new UserNotFoundException();
		User user = UserUtils.getUser(auth);
		if (user == null)
			throw new UserNotFoundException();

		boolean readAll = canReadAll(auth);
		
		Optional<String> sortField = Optional.of(sortBy.orElse("timestamp"));
		Optional<SortOrder> direction = Optional
				.of(sortOrder.orElse("asc").equals("asc") ? SortOrder.ASC : SortOrder.DESC);

		Optional<AdvancedSearch> filters = SearchUtils.fromTabulatorJSON(filter);

		final Set<String> filterTaxpayersIds;
		if (!readAll) {
			// Only SYSADMIN users may see every documents. Other users are restricted to
			// their relationships
			
			if (UserProfile.DECLARANT.equals(user.getProfile())) {
				filterTaxpayersIds = userService.getFilteredTaxpayersForUserAsTaxpayer(auth);
			}
			else {
				filterTaxpayersIds = userService.getFilteredTaxpayersForUserAsManager(auth);
			}
			
			if ( filterTaxpayersIds == null || filterTaxpayersIds.isEmpty()) {
				Page<DocumentUploaded> docs;
				try {
					docs = SearchUtils.doSearch(
						filters.orElse(new AdvancedSearch()).clone().withFilter(
							new AdvancedSearch.QueryFilterOr(Arrays.asList(
								new AdvancedSearch.QueryFilterList("taxPayerId.keyword", filterTaxpayersIds),
								new AdvancedSearch.QueryFilterTerm("userLogin.keyword", user.getLogin())
								), messageSource) ),
						DocumentUploaded.class, elasticsearchClient, page, size, sortField, direction);
				} catch (Exception ex) {
					log.log(Level.SEVERE, "Error while searching for all documents", ex);
					docs = Page.empty();
				}
				return new PaginationData<>(docs.getTotalPages(), docs.getContent());
			}
		}
		else {
			filterTaxpayersIds = null;
		}

		Page<DocumentUploaded> docs;
		try {
			if (readAll)
				docs = SearchUtils.doSearch(filters.orElse(new AdvancedSearch()), DocumentUploaded.class,
						elasticsearchClient, page, size, sortField, direction);
			else
				docs = SearchUtils.doSearch(
						filters.orElse(new AdvancedSearch()).clone().withFilter(
								new AdvancedSearch.QueryFilterOr(Arrays.asList(
									new AdvancedSearch.QueryFilterList("taxPayerId.keyword", filterTaxpayersIds),
									new AdvancedSearch.QueryFilterTerm("userLogin.keyword", user.getLogin())
									), messageSource) ),
						DocumentUploaded.class, elasticsearchClient, page, size, sortField, direction);
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Error while searching for all documents", ex);
			docs = Page.empty();
		}
		return new PaginationData<>(docs.getTotalPages(), docs.getContent());
	}

	/**
	 * Return document uploads records via API. Admits some optional parameters:<BR>
	 * from: Date/time for first upload<BR>
	 * to: Date/time for last upload<BR>
	 * days: Number of past days (overrides 'from' and 'to' parameters)
	 */
	@Secured({ "ROLE_TAX_DECLARATION_READ_ALL" })
	@GetMapping(value = "/docs-uploads", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Return document uploads records via API", response = DocumentUploaded[].class)
	public ResponseEntity<Object> getDocsUploads(
			@ApiParam(value = "Date/time for first upload", required = false) 
			@RequestParam("fromDate") Optional<String> fromDate,
			@ApiParam(value = "Date/time for last upload", required = false) 
			@RequestParam("toDate") Optional<String> toDate,
			@ApiParam(value = "Number of past days (overrides 'from' and 'to' parameters)", required = false) 
			@RequestParam("days") Optional<String> days) {

		SearchRequest searchRequest = new SearchRequest("docs_uploaded");
		BoolQueryBuilder query = QueryBuilders.boolQuery();
		if (days.isPresent() && days.get().trim().length() > 0) {
			RangeQueryBuilder timestampQuery = new RangeQueryBuilder("timestamp");
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DAY_OF_YEAR, -Integer.parseInt(days.get()));
			timestampQuery.from(ParserUtils.formatTimestampES(cal.getTime()), true);
			query.must(timestampQuery);
		} else if (fromDate.isPresent() || toDate.isPresent()) {
			RangeQueryBuilder timestampQuery = new RangeQueryBuilder("timestamp");
			if (fromDate.isPresent())
				timestampQuery.from(ParserUtils.formatTimestampES(ParserUtils.parseFlexibleDate(fromDate.get())), true);
			if (toDate.isPresent())
				timestampQuery.to(ParserUtils.formatTimestampES(ParserUtils.parseFlexibleDate(toDate.get())), true);
			query.must(timestampQuery);
		}

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(query);
		searchSourceBuilder.size(MAX_RESULTS_PER_REQUEST);
		searchSourceBuilder.sort("timestamp", SortOrder.DESC);

		searchRequest.source(searchSourceBuilder);
		SearchResponse sresp = null;
		try {
			sresp = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
		} catch (IOException ex) {
			log.log(Level.SEVERE, "Return documents uploads failed", ex);
			return ResponseEntity.badRequest()
					.body(messageSource.getMessage("op.failed", null, LocaleContextHolder.getLocale()));
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
	 * Return documents (their parsed contents) via API for a given template. The
	 * template may be identified by the internal numerical ID, or by its name (in
	 * this case, replace all spaces and symbols with underlines). Admits some
	 * optional parameters:<BR>
	 * from: Date/time for first day/time of document receipt<BR>
	 * to: Date/time for last day/time of document receipt<BR>
	 * days: Number of past days (overrides 'from' and 'to' parameters)
	 */
	@Secured({ "ROLE_TAX_DECLARATION_READ_ALL" })
	@GetMapping(value = "/docs/{templateName}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Return documents (their parsed contents) via API for a given template. The template may be identified by the internal numerical ID, "
			+ "or by its name (in this case, replace all spaces and symbols with underlines).")
	public ResponseEntity<Object> getDocs(
			@PathVariable("templateName") String templateName,
			@ApiParam(value = "Date/time for first day/time of document receipt", required = false) 
			@RequestParam("fromDate") Optional<String> fromDate,
			@ApiParam(value = "Date/time for last day/time of document receipt", required = false) 
			@RequestParam("toDate") Optional<String> toDate,
			@ApiParam(value = "Number of past days (overrides 'from' and 'to' parameters)", required = false) 
			@RequestParam("days") Optional<String> days) {

		// Parse the 'templateName' informed at request path
		if (templateName == null || templateName.trim().length() == 0) {
			throw new MissingParameter("templateName");
		}
		List<DocumentTemplate> requestedTemplates = templateService.findTemplateWithNameOrId(templateName);
		if (requestedTemplates.isEmpty()) {
			return ResponseEntity.ok().body(Collections.emptyList());
		}

		// Parse the optional temporal query parameters
		BoolQueryBuilder query = QueryBuilders.boolQuery();
		if (days.isPresent() && days.get().trim().length() > 0) {
			RangeQueryBuilder timestampQuery = new RangeQueryBuilder("timestamp");
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DAY_OF_YEAR, -Integer.parseInt(days.get()));
			timestampQuery.from(ParserUtils.formatTimestampES(cal.getTime()), true);
			query.must(timestampQuery);
		} else if (fromDate.isPresent() || toDate.isPresent()) {
			RangeQueryBuilder timestampQuery = new RangeQueryBuilder("timestamp");
			if (fromDate.isPresent())
				timestampQuery.from(ParserUtils.formatTimestampES(ParserUtils.parseFlexibleDate(fromDate.get())), true);
			if (toDate.isPresent())
				timestampQuery.to(ParserUtils.formatTimestampES(ParserUtils.parseFlexibleDate(toDate.get())), true);
			query.must(timestampQuery);
		}

		Set<String> indicesVerified = new HashSet<>(); // avoids redundancy (may happen if we have different versions of
														// the same template name)

		// Build the query for each template of interest
		List<Object> allDocs = new LinkedList<>();
		for (DocumentTemplate template : requestedTemplates) {

			if (allDocs.size() >= MAX_RESULTS_PER_REQUEST)
				break;

			SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(query);
			searchSourceBuilder.size(MAX_RESULTS_PER_REQUEST - allDocs.size());
			searchSourceBuilder.sort("timestamp", SortOrder.DESC);

			String indexName = IndexNamesUtils.formatIndexNameForValidatedData(template);

			if (indicesVerified.contains(indexName))
				continue;
			indicesVerified.add(indexName);

			SearchRequest searchRequest = new SearchRequest(indexName);
			searchRequest.source(searchSourceBuilder);
			SearchResponse sresp = null;
			try {
				sresp = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
			} catch (Exception ex) {
				if (ErrorUtils.isErrorNoIndexFound(ex))
					continue;
				boolean fixed = false;
				if (ErrorUtils.isErrorNoMappingFoundForColumn(ex)) {
					// maybe the 'timestamp' is missing in this index (probably a 'legacy index' at
					// test environment)
					// let's try again without the timestamp sorting
					searchSourceBuilder.sorts().clear();
					try {
						sresp = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
						fixed = true;
					} catch (IOException ex2) {
						// will throw original exception
					}
				}
				if (!fixed) {
					log.log(Level.SEVERE, "Return documents uploads failed", ex);
					return ResponseEntity.badRequest()
							.body(messageSource.getMessage("op.failed", null, LocaleContextHolder.getLocale()));
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

	@JsonView(Views.Declarant.class)
	@Secured({ "ROLE_TAX_DECLARATION_READ" })
	@GetMapping(value = "/doc/situations", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Return situation history via API for a given document. The document may be identified by the internal numerical ID.")
	public ResponseEntity<Object> getDocSituations(
			@ApiParam(name = "Document ID of document to retrive", allowEmptyValue = false, allowMultiple = false, required = true, type = "String")
			@RequestParam("documentId") String documentId) {

		// Parse the 'templateName' informed at request path
		if (documentId == null || documentId.trim().length() == 0) {
			throw new MissingParameter("documentId");
		}

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null)
			throw new UserNotFoundException();
		User user = UserUtils.getUser(auth);
		if (user == null)
			throw new UserNotFoundException();

		DocumentUploaded refDoc = documentsUploadedRepository.findById(documentId).orElse(null);
		if (refDoc==null) {
			throw new DocumentNotFoundException();
		}

		boolean readAll = canReadAll(auth);
		
		if (!readAll) {
			if (refDoc.getUser() != null && (!refDoc.getUser().equalsIgnoreCase(String.valueOf(auth.getName()))
					&& !userService.isUserAuthorizedForSubject(user, refDoc.getTaxPayerId())))
				throw new InsufficientPrivilege();
			
			// Only SYSADMIN users may see every documents. Other users are restricted to
			// their relationships
			final Set<String> filterTaxpayersIds;
			filterTaxpayersIds = userService.getFilteredTaxpayersForUserAsAnyRelationship(auth);
			
			if (( filterTaxpayersIds != null ) && ( filterTaxpayersIds.isEmpty() || !filterTaxpayersIds.contains(refDoc.getTaxPayerId()))) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND,
							messageSource.getMessage("error.accessDenied", null, LocaleContextHolder.getLocale()));	
			}
		}

		try {
			List<DocumentSituationHistory> situations = documentsSituationHistoryRepository.findByDocumentIdOrderByChangedTimeDesc(documentId);
			return ResponseEntity.ok().body(situations);

		} catch (Exception e) {
			log.log(Level.SEVERE, e, () -> "Error while searching for situations for document " + documentId);
			return ResponseEntity.ok().body(Collections.emptyList());
		}

	}

	@JsonView(Views.Declarant.class)
	@Secured({ "ROLE_TAX_DECLARATION_READ" })
	@GetMapping(value = "/doc/errors", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Return validation error messages via API for a given document. The document may be identified by the internal numerical ID.")
	public PaginationData<DocumentValidationErrorMessage> getDocErrorMessages(
			@ApiParam(name = "Document ID of document to retrive", allowEmptyValue = false, allowMultiple = false, required = true, type = "String")
			@RequestParam("documentId") String documentId, 
			@RequestParam("page") Optional<Integer> page, 
			@ApiParam(name = "Page size", allowEmptyValue = true, allowMultiple = false, required = false, type = "Integer")
			@RequestParam("size") Optional<Integer> size,
			@ApiParam(name = "Fields and values to filer data", allowEmptyValue = true, allowMultiple = false, required = false, type = "String")
			@RequestParam("filter") Optional<String> filter, 
			@ApiParam(name = "Field name to sort data", allowEmptyValue = true, allowMultiple = false, required = false, type = "String")
			@RequestParam("sortby") Optional<String> sortBy,
			@ApiParam(name = "Order to sort. Can be asc or desc", allowEmptyValue = true, allowMultiple = false, required = false, type = "String")
			@RequestParam("sortorder") Optional<String> sortOrder) {

		if (documentId == null || documentId.trim().length() == 0) {
			throw new MissingParameter("documentId");
		}

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null)
			throw new UserNotFoundException();
		User user = UserUtils.getUser(auth);
		if (user == null)
			throw new UserNotFoundException();

		DocumentUploaded refDoc = documentsUploadedRepository.findById(documentId).orElse(null);
		if (refDoc==null) {
			throw new DocumentNotFoundException();
		}

		boolean readAll = canReadAll(auth);
		
		if (!readAll) {
			if (refDoc.getUser() != null && (!refDoc.getUser().equalsIgnoreCase(String.valueOf(auth.getName()))
					&& !userService.isUserAuthorizedForSubject(user, refDoc.getTaxPayerId())))
				throw new InsufficientPrivilege();
			
			// Only SYSADMIN users may see every documents. Other users are restricted to
			// their relationships
			final Set<String> filterTaxpayersIds;
			filterTaxpayersIds = userService.getFilteredTaxpayersForUserAsAnyRelationship(auth);
			
			if (( filterTaxpayersIds != null ) && ( filterTaxpayersIds.isEmpty() || !filterTaxpayersIds.contains(refDoc.getTaxPayerId()))) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND,
							messageSource.getMessage("error.accessDenied", null, LocaleContextHolder.getLocale()));	
			}
		}

		Optional<AdvancedSearch> filters = SearchUtils.fromTabulatorJSON(filter);
		Optional<String> sortField = Optional.of(sortBy.orElse("timestamp"));
		Optional<SortOrder> direction = Optional
				.of(sortOrder.orElse("asc").equals("asc") ? SortOrder.ASC : SortOrder.DESC);

		Page<DocumentValidationErrorMessage> messages;

		try {
			messages = SearchUtils
				.doSearch(
					filters.orElse(new AdvancedSearch()).clone()
						.withFilter(new AdvancedSearch.QueryFilterList("documentId", documentId)),
					DocumentValidationErrorMessage.class, elasticsearchClient, page, size, sortField,
					direction);

		} catch (Exception e) {
			log.log(Level.SEVERE, String.format("Error while searching for situations for document %s", documentId), e);
			messages = Page.empty();
		}

		for (DocumentValidationErrorMessage message : messages.getContent()) {
			message.setErrorMessage(messagesService.getMessage(message.getErrorMessage()));
		}

		return new PaginationData<>(messages.getTotalPages(), messages.getContent());

	}

	@Secured({"ROLE_TAX_DECLARATION_READ"})
	@GetMapping(value = "/doc/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ApiOperation(value = "Return the document. The document may be identified by the internal numerical ID.")
	public ResponseEntity<Resource> downloadDocument(
			@ApiParam(name = "Document ID of document to retrive", allowEmptyValue = false, allowMultiple = false, required = true, type = "String")
			@RequestParam("documentId") String documentId) {

		// Parse the 'templateName' informed at request path
		if (documentId == null || documentId.trim().length() == 0) {
			throw new MissingParameter("documentId");
		}

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null)
			throw new UserNotFoundException();
		User user = UserUtils.getUser(auth);
		if (user == null)
			throw new UserNotFoundException();

		DocumentUploaded doc = documentsUploadedRepository.findById(documentId).orElse(null);
		if (doc==null) {
			throw new DocumentNotFoundException();
		}

		boolean readAll = canReadAll(auth);
		
		if (!readAll) {
			if (doc.getUser() != null && (!doc.getUser().equalsIgnoreCase(String.valueOf(auth.getName()))
					&& !userService.isUserAuthorizedForSubject(user, doc.getTaxPayerId())))
				throw new InsufficientPrivilege();
			
			// Only SYSADMIN users may see every documents. Other users are restricted to
			// their relationships
			final Set<String> filterTaxpayersIds;
			filterTaxpayersIds = userService.getFilteredTaxpayersForUserAsManager(auth);
			
			if (( filterTaxpayersIds != null ) && ( filterTaxpayersIds.isEmpty() || !filterTaxpayersIds.contains(doc.getTaxPayerId()))) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND,
							messageSource.getMessage("error.accessDenied", null, LocaleContextHolder.getLocale()));	
			}
		}

		try {
			Path path = storageService.find(doc.getFileIdWithPath());
			ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

			HttpHeaders headers = new HttpHeaders();
			headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + doc.getFilename());
			headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
			headers.add("Pragma", "no-cache");
			headers.add("Expires", "0");

			return ResponseEntity.ok().headers(headers).contentLength(path.toFile().length())
					.contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);
		} catch (IOException e) {
			log.log(Level.SEVERE, String.format("Error whiling sending file from document %s", documentId), e);
		}

		throw new ResponseStatusException(HttpStatus.NOT_FOUND,
				messageSource.getMessage("doc.not.found", null, LocaleContextHolder.getLocale()));
	}
	
	@Secured({"ROLE_TAX_DECLARATION_READ"})
	@GetMapping(value = "/doc/saveAll", produces = MediaType.TEXT_HTML_VALUE)
	@ApiOperation(value = "Return the document. The document may be identified by the internal numerical ID.")
	public Boolean saveAllDocuments() {
		saveDataToParquet.saveData();
		return Boolean.TRUE;
	}
}