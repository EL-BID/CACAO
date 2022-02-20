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

import java.io.File;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.http.HttpServletRequest;

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
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.utils.DateTimeUtils;
import org.idb.cacao.api.utils.IndexNamesUtils;
import org.idb.cacao.api.utils.ParserUtils;
import org.idb.cacao.web.controllers.AdvancedSearch;
import org.idb.cacao.web.controllers.services.DocumentTemplateService;
import org.idb.cacao.web.controllers.services.FileUploadedProducer;
import org.idb.cacao.web.controllers.services.MessagesService;
import org.idb.cacao.web.controllers.services.UserService;
import org.idb.cacao.web.dto.FileUploadedEvent;
import org.idb.cacao.web.dto.PaginationData;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.entities.UserProfile;
import org.idb.cacao.web.errors.InsufficientPrivilege;
import org.idb.cacao.web.errors.MissingParameter;
import org.idb.cacao.web.errors.UserNotFoundException;
import org.idb.cacao.web.repositories.DocumentSituationHistoryRepository;
import org.idb.cacao.web.repositories.DocumentTemplateRepository;
import org.idb.cacao.web.repositories.DocumentUploadedRepository;
import org.idb.cacao.web.utils.ErrorUtils;
import org.idb.cacao.web.utils.SearchUtils;
import org.idb.cacao.web.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.transaction.annotation.Transactional;
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

	/**
	 * Endpoint for uploading a document to be parsed
	 */
	@Secured({"ROLE_TAX_DECLARATION_WRITE"})
	@PostMapping(value = "/doc", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation("Endpoint for uploading a document to be parsed")
	public ResponseEntity<Map<String, String>> handleFileUpload(@RequestParam("fileinput") MultipartFile fileinput,
			@RequestParam("template") String templateAndVersion, RedirectAttributes redirectAttributes,
			HttpServletRequest request) {

		log.log(Level.FINE,
				"Incoming file (upload): " + fileinput.getOriginalFilename() + ", size: " + fileinput.getSize());

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

		if (templateAndVersion == null || templateAndVersion.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error",
					messageSource.getMessage("upload.failed.missing.template", null, LocaleContextHolder.getLocale())));
		}

		String templateName = null, templateVersion = null;

		String[] parts = templateAndVersion.split("=");

		if (parts == null || parts.length == 0) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error",
					messageSource.getMessage("upload.failed.missing.template", null, LocaleContextHolder.getLocale())));
		}

		templateName = parts[0] == null ? null : parts[0].trim();

		if (templateName == null || templateName.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error",
					messageSource.getMessage("upload.failed.missing.template", null, LocaleContextHolder.getLocale())));
		}

		if (parts.length > 1)
			templateVersion = parts[1] == null ? null : parts[1].trim();

		if (templateVersion == null || templateVersion.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", messageSource
					.getMessage("upload.failed.missing.template.version", null, LocaleContextHolder.getLocale())));
		}

		List<DocumentTemplate> templateVersions = templateRepository.findByName(templateName);
		if (templateVersions == null || templateVersions.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error",
					messageSource.getMessage("unknown.template", null, LocaleContextHolder.getLocale())));
		}
		if (templateVersions.size() > 1) {
			// if we have more than one possible choice, let's give higher priority to most
			// recent ones
			templateVersions = templateVersions.stream().sorted(DocumentTemplate.TIMESTAMP_COMPARATOR)
					.collect(Collectors.toList());
		}

		// Try to validate template version
		boolean versionFound = false;
		for (DocumentTemplate doc : templateVersions) {
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

		final String remoteIpAddr = (request != null && request.getRemoteAddr() != null
				&& request.getRemoteAddr().trim().length() > 0) ? request.getRemoteAddr() : null;

		final Map<String, String> result;

		try (InputStream inputStream = fileinput.getInputStream()) {
			result = uploadFile(fileinput.getOriginalFilename(), inputStream, /* closeInputStream */true, templateName,
					templateVersion, remoteIpAddr, user);
		} catch (GeneralException ex) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Collections.singletonMap("error", ex.getMessage()));
		} catch (IOException ex) {
			log.log(Level.SEVERE, "Failed upload " + fileinput.getOriginalFilename(), ex);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Collections.singletonMap("error", messageSource.getMessage("upload.failed.file",
							new Object[] { fileinput.getOriginalFilename() }, LocaleContextHolder.getLocale())));
		}
		return ResponseEntity.ok(result);
	}

	/**
	 * Endpoint for uploading many documents to be parsed in one ZIP file
	 */
	@Secured({"ROLE_TAX_DECLARATION_WRITE"})
	@PostMapping(value = "/docs-zip", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation("Endpoint for uploading many documents to be parsed in one ZIP file")
	public ResponseEntity<Map<String, String>> handleFileUploadZIP(@RequestParam("filezip") MultipartFile filezip,
			@RequestParam("template") String template, RedirectAttributes redirectAttributes,
			HttpServletRequest request) {

		log.log(Level.FINE,
				"Incoming files (upload): " + filezip.getOriginalFilename() + ", size: " + filezip.getSize());

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null)
			throw new UserNotFoundException();

		User user = userService.getUser(auth);
		if (user == null)
			throw new UserNotFoundException();

		if (filezip.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error",
					messageSource.getMessage("upload.failed.empty.file", null, LocaleContextHolder.getLocale())));
		}

		if (template == null || template.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error",
					messageSource.getMessage("upload.failed.empty.file", null, LocaleContextHolder.getLocale())));
		}
		List<DocumentTemplate> templateVersions = templateRepository.findByName(template);
		if (templateVersions == null || templateVersions.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error",
					messageSource.getMessage("unknown.template", null, LocaleContextHolder.getLocale())));
		}
		if (templateVersions.size() > 1) {
			// if we have more than one possible choice, let's give higher priority to most
			// recent ones
			templateVersions = templateVersions.stream().sorted(DocumentTemplate.TIMESTAMP_COMPARATOR)
					.collect(Collectors.toList());
		}

		final String remoteIpAddr = (request != null && request.getRemoteAddr() != null
				&& request.getRemoteAddr().trim().length() > 0) ? request.getRemoteAddr() : null;

		final List<Map<String, String>> results = new ArrayList<>();

		try (ZipInputStream zipStream = new ZipInputStream(filezip.getInputStream())) {
			ZipEntry ze;

			while ((ze = zipStream.getNextEntry()) != null) {

				Map<String, String> result = uploadFile(ze.getName(), zipStream, /* closeInputStream */false, template,
						/* template version */ null, remoteIpAddr, user);
				results.add(result);
			}
		} catch (GeneralException ex) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Collections.singletonMap("error", ex.getMessage()));
		} catch (IOException ex) {
			log.log(Level.SEVERE, "Failed upload " + filezip.getOriginalFilename(), ex);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Collections.singletonMap("error", messageSource.getMessage("upload.failed.file",
							new Object[] { filezip.getOriginalFilename() }, LocaleContextHolder.getLocale())));
		}

		// TODO: retornar a situação de todos arquivos
		return ResponseEntity.ok(results.get(0));
	}

	/**
	 * Endpoint for uploading many separate document to be parsed
	 */
	@Secured({"ROLE_TAX_DECLARATION_WRITE"})
	@PostMapping(value = "/docs", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation("Endpoint for uploading many separate document to be parsed")
	public ResponseEntity<Map<String, String>> handleFilesUpload(@RequestParam("files") MultipartFile[] files,
			@RequestParam("template") String template, RedirectAttributes redirectAttributes,
			HttpServletRequest request) {

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null)
			throw new UserNotFoundException();

		User user = userService.getUser(auth);
		if (user == null)
			throw new UserNotFoundException();

		if (files == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error",
					messageSource.getMessage("upload.failed.empty.file", null, LocaleContextHolder.getLocale())));
		}

		final long allSizes = Arrays.stream(files).mapToLong(MultipartFile::getSize).sum();
		log.log(Level.FINE, "Incoming files (upload): " + files.length + ", size: " + allSizes);

		if (files.length == 0) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error",
					messageSource.getMessage("upload.failed.empty.file", null, LocaleContextHolder.getLocale())));
		}

		if (template == null || template.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error",
					messageSource.getMessage("upload.failed.empty.file", null, LocaleContextHolder.getLocale())));
		}
		List<DocumentTemplate> templateVersions = templateRepository.findByName(template);
		if (templateVersions == null || templateVersions.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error",
					messageSource.getMessage("unknown.template", null, LocaleContextHolder.getLocale())));
		}
		if (templateVersions.size() > 1) {
			// if we have more than one possible choice, let's give higher priority to most
			// recent ones
			templateVersions = templateVersions.stream().sorted(DocumentTemplate.TIMESTAMP_COMPARATOR)
					.collect(Collectors.toList());
		}

		final String remoteIpAddr = (request != null && request.getRemoteAddr() != null
				&& request.getRemoteAddr().trim().length() > 0) ? request.getRemoteAddr() : null;

		List<Map<String, String>> results = new ArrayList<>();

		for (MultipartFile fileinput : files) {

			try (InputStream inputStream = fileinput.getInputStream()) {
				Map<String, String> result = uploadFile(fileinput.getOriginalFilename(), inputStream,
						/* closeInputStream */true, template, /* template version */ null, remoteIpAddr, user);
				results.add(result);
			} catch (GeneralException ex) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(Collections.singletonMap("error", ex.getMessage()));
			} catch (IOException ex) {
				log.log(Level.SEVERE, "Failed upload " + fileinput.getOriginalFilename(), ex);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(Collections.singletonMap("error", messageSource.getMessage("upload.failed.file",
								new Object[] { fileinput.getOriginalFilename() }, LocaleContextHolder.getLocale())));
			}

		}
		// TODO: Retornar a situação de cada arquivo
		return ResponseEntity.ok(results.get(0));
	}

	/**
	 * Upload a file
	 */
	@Transactional
	@ApiIgnore
	private Map<String, String> uploadFile(final String originalFilename, final InputStream fileStream,
			final boolean closeInputStream, final String template, final String templateVersion,
			final String remoteIpAddr, final User user) throws IOException, GeneralException {
		File tempFile1 = null;
		List<Runnable> rollbackProcedures = new LinkedList<>(); // hold rollback procedures only to be used in case of
																// error
		try {

			log.log(Level.INFO, "User " + user.getLogin() + " uploading file " + originalFilename + " for template "
					+ template + " from " + remoteIpAddr);

			String fileId = UUID.randomUUID().toString();

			final OffsetDateTime timestamp = DateTimeUtils.now();
			HashingInputStream his = new HashingInputStream(Hashing.sha256(), fileStream);
			String subDir = storageService.store(fileId, his, closeInputStream);

			// Keep this information in history of all uploads
			DocumentUploaded regUpload = new DocumentUploaded();
			regUpload.setTemplateName(template);
			regUpload.setTemplateVersion(templateVersion);
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

			rollbackProcedures.add(() -> documentsUploadedRepository.delete(savedInfo)); // in case of error delete the
																							// DocumentUploaded
			rollbackProcedures.add(() -> documentsSituationHistoryRepository.delete(savedSituation)); // in case of
																										// error delete
																										// the
																										// DocumentUploaded
			Map<String, String> result = new HashMap<>();
			result.put("result", "ok");
			result.put("file_id", fileId);

			FileUploadedEvent event = new FileUploadedEvent();
			event.setFileId(savedInfo.getId());
			fileUploadedProducer.fileUploaded(event);

			return result;
		} catch (GeneralException ex) {
			callRollbackProcedures(rollbackProcedures);
			throw ex;
		} finally {
			if (tempFile1 != null && tempFile1.exists()) {
				if (!tempFile1.delete()) {
					log.log(Level.WARNING, "Could not delete temporary file " + tempFile1.getAbsolutePath()
							+ " created from incoming file " + originalFilename);
				}
			}
		}
	}

	public static void callRollbackProcedures(Collection<Runnable> rollbackProcedures) {
		if (rollbackProcedures == null || rollbackProcedures.isEmpty())
			return;
		for (Runnable proc : rollbackProcedures) {
			try {
				proc.run();
			} catch (Throwable ex) {
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
			@RequestParam("page") Optional<Integer> page, @RequestParam("size") Optional<Integer> size,
			@RequestParam("filter") Optional<String> filter, @RequestParam("sortby") Optional<String> sortBy,
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
				PaginationData<DocumentUploaded> result = new PaginationData<>(docs.getTotalPages(), docs.getContent());
				return result;
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
		PaginationData<DocumentUploaded> result = new PaginationData<>(docs.getTotalPages(), docs.getContent());
		return result;
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
			@ApiParam(value = "Date/time for first upload", required = false) @RequestParam("fromDate") Optional<String> fromDate,
			@ApiParam(value = "Date/time for last upload", required = false) @RequestParam("toDate") Optional<String> toDate,
			@ApiParam(value = "Number of past days (overrides 'from' and 'to' parameters)", required = false) @RequestParam("days") Optional<String> days) {

		SearchRequest searchRequest = new SearchRequest("docs_uploaded");
		BoolQueryBuilder query = QueryBuilders.boolQuery();
		if (days != null && days.isPresent() && days.get().trim().length() > 0) {
			RangeQueryBuilder timestampQuery = new RangeQueryBuilder("timestamp");
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DAY_OF_YEAR, -Integer.parseInt(days.get()));
			timestampQuery.from(ParserUtils.formatTimestampES(cal.getTime()), true);
			query.must(timestampQuery);
		} else if ((fromDate != null && fromDate.isPresent()) || (toDate != null && toDate.isPresent())) {
			RangeQueryBuilder timestampQuery = new RangeQueryBuilder("timestamp");
			if (fromDate != null && fromDate.isPresent())
				timestampQuery.from(ParserUtils.formatTimestampES(ParserUtils.parseFlexibleDate(fromDate.get())), true);
			if (toDate != null && toDate.isPresent())
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
	public ResponseEntity<Object> getDocs(@PathVariable("templateName") String templateName,
			@ApiParam(value = "Date/time for first day/time of document receipt", required = false) @RequestParam("fromDate") Optional<String> fromDate,
			@ApiParam(value = "Date/time for last day/time of document receipt", required = false) @RequestParam("toDate") Optional<String> toDate,
			@ApiParam(value = "Number of past days (overrides 'from' and 'to' parameters)", required = false) @RequestParam("days") Optional<String> days) {

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
		if (days != null && days.isPresent() && days.get().trim().length() > 0) {
			RangeQueryBuilder timestampQuery = new RangeQueryBuilder("timestamp");
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DAY_OF_YEAR, -Integer.parseInt(days.get()));
			timestampQuery.from(ParserUtils.formatTimestampES(cal.getTime()), true);
			query.must(timestampQuery);
		} else if ((fromDate != null && fromDate.isPresent()) || (toDate != null && toDate.isPresent())) {
			RangeQueryBuilder timestampQuery = new RangeQueryBuilder("timestamp");
			if (fromDate != null && fromDate.isPresent())
				timestampQuery.from(ParserUtils.formatTimestampES(ParserUtils.parseFlexibleDate(fromDate.get())), true);
			if (toDate != null && toDate.isPresent())
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
			} catch (Throwable ex) {
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
	public ResponseEntity<Object> getDocSituations(@RequestParam("documentId") String documentId) {

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
			filterTaxpayersIds = userService.getFilteredTaxpayersForUserAsManager(auth);
			
			if ( filterTaxpayersIds != null ) {
				
				if ( filterTaxpayersIds.isEmpty() || !filterTaxpayersIds.contains(refDoc.getTaxPayerId())) {
					throw new ResponseStatusException(HttpStatus.NOT_FOUND,
							messageSource.getMessage("error.accessDenied", null, LocaleContextHolder.getLocale()));	
				}			
			}
		}

		try {
			List<DocumentSituationHistory> situations = documentsSituationHistoryRepository.findByDocumentIdOrderByChangedTimeDesc(documentId);
			return ResponseEntity.ok().body(situations);

		} catch (Exception e) {
			log.log(Level.SEVERE, "Error while searching for situations for document " + documentId, e);
			return ResponseEntity.ok().body(Collections.emptyList());
		}

	}

	@JsonView(Views.Declarant.class)
	@Secured({ "ROLE_TAX_DECLARATION_READ" })
	@GetMapping(value = "/doc/errors", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Return validation error messages via API for a given document. The document may be identified by the internal numerical ID.")
	public PaginationData<DocumentValidationErrorMessage> getDocErrorMessages(
			@RequestParam("documentId") String documentId, @RequestParam("page") Optional<Integer> page,
			@RequestParam("size") Optional<Integer> size, @RequestParam("filter") Optional<String> filter,
			@RequestParam("sortby") Optional<String> sortBy, @RequestParam("sortorder") Optional<String> sortOrder) {

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
			filterTaxpayersIds = userService.getFilteredTaxpayersForUserAsManager(auth);
			
			if ( filterTaxpayersIds != null ) {
				
				if ( filterTaxpayersIds.isEmpty() || !filterTaxpayersIds.contains(refDoc.getTaxPayerId())) {
					throw new ResponseStatusException(HttpStatus.NOT_FOUND,
							messageSource.getMessage("error.accessDenied", null, LocaleContextHolder.getLocale()));	
				}			
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
			log.log(Level.SEVERE, "Error while searching for situations for document " + documentId, e);
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
	public ResponseEntity<Resource> downloadDocument(@RequestParam("documentId") String documentId) {

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
			
			if ( filterTaxpayersIds != null ) {
				
				if ( filterTaxpayersIds.isEmpty() || !filterTaxpayersIds.contains(doc.getTaxPayerId())) {
					throw new ResponseStatusException(HttpStatus.NOT_FOUND,
							messageSource.getMessage("error.accessDenied", null, LocaleContextHolder.getLocale()));	
				}			
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
			log.log(Level.SEVERE, "Error whiling sending file from document " + documentId, e);
		}

		throw new ResponseStatusException(HttpStatus.NOT_FOUND,
				messageSource.getMessage("doc.not.found", null, LocaleContextHolder.getLocale()));
	}
}