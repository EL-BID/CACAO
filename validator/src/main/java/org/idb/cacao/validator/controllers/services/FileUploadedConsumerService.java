/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.validator.controllers.services;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.validation.ValidationException;

import org.idb.cacao.api.DocumentSituation;
import org.idb.cacao.api.DocumentSituationHistory;
import org.idb.cacao.api.DocumentUploaded;
import org.idb.cacao.api.DocumentValidationErrorMessage;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.errors.DocumentNotFoundException;
import org.idb.cacao.api.errors.GeneralException;
import org.idb.cacao.api.errors.MissingConfigurationException;
import org.idb.cacao.api.errors.TemplateNotFoundException;
import org.idb.cacao.api.errors.UnknownFileFormatException;
import org.idb.cacao.api.storage.FileSystemStorageService;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.api.templates.DocumentInput;
import org.idb.cacao.api.templates.DocumentInputFieldMapping;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.FieldMapping;
import org.idb.cacao.api.templates.TemplateArchetype;
import org.idb.cacao.api.templates.TemplateArchetypes;
import org.idb.cacao.api.utils.DateTimeUtils;
import org.idb.cacao.validator.fileformats.FileFormat;
import org.idb.cacao.validator.fileformats.FileFormatFactory;
import org.idb.cacao.validator.parsers.DataIterator;
import org.idb.cacao.validator.parsers.FileParser;
import org.idb.cacao.validator.repositories.DocumentSituationHistoryRepository;
import org.idb.cacao.validator.repositories.DocumentTemplateRepository;
import org.idb.cacao.validator.repositories.DocumentUploadedRepository;
import org.idb.cacao.validator.repositories.DocumentValidationErrorMessageRepository;
import org.idb.cacao.validator.repositories.DomainTableRepository;
import org.idb.cacao.validator.validations.Validations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

/**
 * Consumes KAFKA for incoming file requests and perform the validation of the file contents according
 * to the configured DocumentTemplate and DocumentInput.
 *
 */
@Service
public class FileUploadedConsumerService {

	private static final Logger log = Logger.getLogger(FileUploadedConsumerService.class.getName());

	/**
	 * Size in bytes of the small sample to read from the file head
	 */
	private static final int SAMPLE_SIZE = 32;

	@Autowired
	private DocumentUploadedRepository documentsUploadedRepository;

	@Autowired
	private DocumentTemplateRepository documentTemplateRepository;

	@Autowired
	private DocumentSituationHistoryRepository documentsSituationHistoryRepository;

	@Autowired
	private DocumentValidationErrorMessageRepository documentValidationErrorMessageRepository;

	@Autowired
	private FileSystemStorageService fileSystemStorageService;

	@Autowired
	private ValidatedDataStorageService validatedDataStorageService;

	@Autowired
	private DomainTableRepository domainTableRepository;
	
	@Autowired
	private UsersTaxpayersService usersTaxpayersService;

	@Autowired
	private final StreamBridge streamBridge;

	@Value("${validation.max.errors.per.upload}")
	private long maxValidationErrorsPerUpload;

	private static final ConcurrentHashMap<String, Long> processingDocument = new ConcurrentHashMap<>();

	public FileUploadedConsumerService(StreamBridge streamBridge) {
		this.streamBridge = streamBridge;
	}

	@Bean
	public Consumer<String> receiveAndValidateFile() {
		return documentId -> {
			log.log(Level.INFO, "Received message with documentId {0}", documentId);

			try {
				Boolean validated = validateDocument(documentId);

				if (validated.booleanValue()) {
					log.log(Level.INFO, "Sending a message to ETL with documentId {0}", documentId);

					streamBridge.send("receiveAndValidateFile-out-0", documentId);
				}

			} catch (MissingConfigurationException e) {
				log.log(Level.INFO, String.format("Configuration is missing for document %s", documentId), e);
			} catch (Exception e) {
				log.log(Level.SEVERE, String.format("Something went wrong with document %s. Exception: %s", documentId, e), e);
			}
		};
	}

	/**
	 * Try to validate a given uploaded document
	 *
	 * @param documentId The ID of {@link DocumentUploaded} that needs to be
	 *                   validated
	 * @return DocumentId if the document has been validated. NULL if it doesn't.
	 */
	public Boolean validateDocument(String documentId) throws GeneralException, DocumentNotFoundException {

		log.log(Level.INFO, "Received a message with documentId {0}", documentId);

		List<Runnable> rollbackProcedures = new LinkedList<>(); // hold rollback procedures only to be used in case of
																// error

		ValidationContext validationContext = new ValidationContext();

		// Avoid redundant process by replay
		Long mark = processingDocument.compute(documentId, (id,prev)->(prev==null)?System.currentTimeMillis() : -1);
		if (mark<0) {
			log.log(Level.INFO, "Ignoring this message because there is a concurrent process of the same document id");
			return false;
		}
		try {
			// Recovering the document from the database
			DocumentUploaded doc = documentsUploadedRepository.findById(documentId).orElse(null);

			// Tests if the real document exists 
			if (doc == null)
				throw new DocumentNotFoundException("Document with id " + documentId + " wasn't found in database.");

			// Sets the document to validation context
			validationContext.setDocumentUploaded(doc);
			
			Validations validations = new Validations(validationContext, domainTableRepository);

			Optional<DocumentTemplate> opTemplate = documentTemplateRepository.findByNameAndVersion(doc.getTemplateName(),
					doc.getTemplateVersion());
			if (!opTemplate.isPresent()) {
				doc = setSituation(doc, DocumentSituation.INVALID);
				validations.addLogError("{doc.error.template.not.found}");
				saveValidationMessages(validationContext);
				StringBuilder msg = new StringBuilder("Template with name ").append(doc.getTemplateName())
						.append(" and version ").append(doc.getTemplateVersion()).append(" wasn't found in database.");
				throw new TemplateNotFoundException(msg.toString());
			}
			
			DocumentTemplate template = opTemplate.get();
			final String inputName = doc.getInputName();
			
			//If input name is provided, validate it
			if ( inputName != null && !inputName.isEmpty()) {
				
				boolean inputValid = false;				
				
				List<DocumentInput> inputs = template.getInputs();
				
				if ( inputs != null && !inputs.isEmpty() ) {
					inputValid = inputs.stream().anyMatch(item->item.getInputName().equals(inputName));
				}
				
				if (!inputValid) {
					doc = setSituation(doc, DocumentSituation.INVALID);
					validations.addLogError("{doc.error.input.not.found}");
					saveValidationMessages(validationContext);
					StringBuilder msg = new StringBuilder("Input name ").append(doc.getInputName())
							.append(" for template ").append(doc.getTemplateName()).append(" wasn't found in database.");
					throw new TemplateNotFoundException(msg.toString());
				}				
				
			}
			
			validationContext.setDocumentTemplate(template);

			String fullPath = doc.getFileIdWithPath();

			Path filePath = fileSystemStorageService.find(fullPath);			
			validationContext.setDocumentPath(filePath);

			doc = setSituation(doc, DocumentSituation.ACCEPTED);
			validationContext.setDocumentUploaded(doc);

			// Check the DocumentInput related to this file
			
			DocumentInput docInputExpected = null;			
			if ( doc.getInputName() == null || doc.getInputName().isEmpty() ) {
				List<DocumentInput> possibleInputs = opTemplate.get().getInputs();
				if (possibleInputs == null || possibleInputs.isEmpty()) {
					doc = setSituation(doc, DocumentSituation.INVALID);
					validations.addLogError("{doc.error.template.not.found}");
					saveValidationMessages(validationContext);
					throw new MissingConfigurationException("Template with name " + doc.getTemplateName() + " and version "
							+ doc.getTemplateVersion() + " was not configured with proper input format!");
				} else if (possibleInputs.size() == 1) {
					docInputExpected = possibleInputs.get(0);
				} else {
					// If we have more than one possible input for the same DocumentTemplate, we
					// need to choose one
					docInputExpected = chooseFileInput(filePath, doc.getFilename(), possibleInputs);
					if (docInputExpected == null) {
						doc = setSituation(doc, DocumentSituation.INVALID);
						validations.addLogError("{doc.error.file.format.not.found}");
						saveValidationMessages(validationContext);
						throw new UnknownFileFormatException(
								"The file did not match any of the expected file formats for template "
										+ doc.getTemplateName() + " and version " + doc.getTemplateVersion());
					}
				}				
			}
			else {
				Optional<DocumentInput> opInput = 
						opTemplate.get().getInputs().stream().filter(item->item.getInputName().equals(inputName)).findFirst();
				if ( opInput.isPresent() )
					docInputExpected = opInput.get();
				else  {
					doc = setSituation(doc, DocumentSituation.INVALID);
					validations.addLogError("{doc.error.file.format.not.found}");
					saveValidationMessages(validationContext);
					throw new UnknownFileFormatException(
							"The file did not match any of the expected file formats for template "
									+ doc.getTemplateName() + " and version " + doc.getTemplateVersion());
				}					
			}

			// Given the DocumentInput, get the corresponding FileFormat object
			DocumentFormat format = docInputExpected.getFormat();
			if (format == null) {
				doc = setSituation(doc, DocumentSituation.INVALID);
				validations.addLogError("{doc.error.template.not.found}");
				saveValidationMessages(validationContext);
				throw new MissingConfigurationException("Template with name " + doc.getTemplateName() + " and version "
						+ doc.getTemplateVersion() + " was not configured with proper input format!");
			}
			
			validationContext.setDocumentInput(docInputExpected);
			validationContext.setParsedContentsListFactory(LinkedList::new);
			
			FileFormat fileFormat = FileFormatFactory.getFileFormat(format);

			// Given the FileFormat, create a new FileParser
			FileParser parser = fileFormat.createFileParser();

			// Initializes the FileParser for processing
			parser.setPath(filePath);
			parser.setDocumentInputSpec(docInputExpected);
			parser.setDocumentTemplate(opTemplate.get());
			
			long timestamp = System.currentTimeMillis();

			// Let's start parsing the file contents
			try {
				parser.start();
			}
			catch (Exception ex) {
				setSituation(doc, DocumentSituation.INVALID);
				validations.addLogError("{doc.error.parse}");
				saveValidationMessages(validationContext);
				log.log(Level.SEVERE, String.format("Exception while parsing record for file %s", documentId), ex);				
				throw new ValidationException(
						String.format("An error ocurred while attempting to read data in file %s.", doc.getFilename()), ex);				
			}
			
			final long elapsed_time_prepare_parser = System.currentTimeMillis() - timestamp;
			timestamp = System.currentTimeMillis();
			
			DataIterator iterator = null;

			// If the template defines any field to be used as criteria of 'file uniqueness', we should
			// gather this information as well			
			List<DocumentField> fileUniquenessFields = opTemplate.get().getFileUniquenessFields();
			Map<String, Object> fileUniquenessValues = (fileUniquenessFields.isEmpty()) ? null : new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			Set<String> fileUniquenessViolationFieldNames = null;

			try {

				iterator = parser.iterator();
				
				if ( iterator == null ) {
					setSituation(doc, DocumentSituation.INVALID);
					validations.addLogError("{doc.error.no.records.found}");
					saveValidationMessages(validationContext);
					log.log(Level.SEVERE, "Impossible to read fields in file {0}", documentId);
					throw new ValidationException(
							"An error ocurred while attempting to read data in file " + doc.getFilename() + ".");
				}

				long added = 0;
				while (iterator.hasNext()) {

					Map<String, Object> dataItem = iterator.next();

					if (dataItem == null || dataItem.isEmpty() )
						continue;
					
					if (fileUniquenessValues!=null && fileUniquenessValues.size()<fileUniquenessFields.size()) {
						// Gather values for 'file uniqueness' criteria
						for (DocumentField fUniqueField: fileUniquenessFields) {
							Object value = dataItem.get(fUniqueField.getFieldName());
							if (value==null)
								continue;
							if (FieldMapping.TAXPAYER_ID.equals(fUniqueField.getFieldMapping())) {
								if (value instanceof Double) {
									if (value instanceof Double)
										value = ((Double)value).longValue();
									if (value instanceof Float)
										value = ((Float)value).longValue();
									if (value instanceof Number)
										value = ValidationContext.toString(value);
								}
							}
							if (FieldMapping.TAX_YEAR.equals(fUniqueField.getFieldMapping())) {
								if (value instanceof Date) {
									value = DateTimeUtils.getYear((Date)value);
								}
							}
							if (FieldMapping.TAX_MONTH.equals(fUniqueField.getFieldMapping())) {
								if (value instanceof Date) {
									OffsetDateTime odt = OffsetDateTime.from(((Date)value).toInstant().atZone(ZoneOffset.systemDefault()));
									value = odt.getMonthValue();
								}
							}
							Object previousValue = fileUniquenessValues.get(fUniqueField.getFieldName());
							if (previousValue==null) {
								fileUniquenessValues.put(fUniqueField.getFieldName(), value);
							}
							else if (!value.equals(previousValue)) {
								if (fileUniquenessViolationFieldNames!=null 
										&& fileUniquenessViolationFieldNames.contains(fUniqueField.getFieldName())) {
									// do not repeat this same warning
								}
								else {
									// Include a warning about the uniqueness constraint
									if (fileUniquenessViolationFieldNames==null)
										fileUniquenessViolationFieldNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
									fileUniquenessViolationFieldNames.add(fUniqueField.getFieldName());
									validations.addLogError(String.format("{doc.error.uniqueness.violation(%s,%s,%s)}",
										ValidationContext.toString(previousValue),
										ValidationContext.toString(value),
										fUniqueField.getFieldName()));
								}
							}
						}
					}

					validationContext.addParsedContent(dataItem);
					added++;

				} // LOOP over each parsed record
				
				if ( added == 0 ) { //Records not found
					setSituation(doc, DocumentSituation.INVALID);
					validations.addLogError("{doc.error.no.records.found}");
					saveValidationMessages(validationContext);
					log.log(Level.SEVERE, "No records found on file {0}", documentId);
					throw new ValidationException(
							"No records found on file file " + doc.getFilename() + ".");					
				}					
				
				String message = String.format("%d records added to validator from %s",added, doc.getFilename());
				log.log(Level.INFO, message);

			} catch (Exception e) {
				setSituation(doc, DocumentSituation.INVALID);
				validations.addLogError("{doc.error.parse}");
				saveValidationMessages(validationContext);
				log.log(Level.SEVERE, String.format("Exception while parsing record for file %s", documentId), e);				
				throw new ValidationException(
						"An error ocurred while attempting to read data in file " + doc.getFilename() + ".", e);
			} finally {

				if (iterator != null)
					iterator.close();

				parser.close();				
				
			}

			final long elapsed_time_parsing = System.currentTimeMillis() - timestamp;
			timestamp = System.currentTimeMillis();

			// Fetch information from file name according to the configuration
			fetchInformationFromFileName(docInputExpected, validationContext);

			// Add TaxPayerId and TaxPeriod to document on database
			validations.addTaxPayerInformation();
			
			final long elapsed_time_filling_taxpayer_info = System.currentTimeMillis() - timestamp;
			timestamp = System.currentTimeMillis();

			if (doc.getTaxPayerId()!=null && doc.getTaxPayerId().trim().length()>0) {
				AtomicReference<String> userTaxpayerId = new AtomicReference<>();
				try {
					boolean hasAccess = usersTaxpayersService.isUserRepresentativeOf(doc.getUser(), doc.getTaxPayerId(), userTaxpayerId);
					if (!hasAccess) {
						// Reset the taxpayer Id indication in the DocumentUploaded because we don't want to list this occurrence for him
						String taxpayerId = doc.getTaxPayerId();
						doc.setTaxPayerId(userTaxpayerId.get());
						// Inform the user about the violation
						setSituation(doc, DocumentSituation.INVALID);
						if (doc.getUser()==null)
							validations.addLogError("{doc.error.user.not.representative(undefined,"+taxpayerId.replaceAll("[\\,\\(\\)\\{\\}]", "")+")}");
						else
							validations.addLogError("{doc.error.user.not.representative("+doc.getUser().replaceAll("[\\,\\(\\)\\{\\}]", "")+","+taxpayerId.replaceAll("[\\,\\(\\)\\{\\}]", "")+")}");
						saveValidationMessages(validationContext);
						String message = String.format("Exception while parsing record for file %s: the user %s does not represent the taxpayer %s", documentId, doc.getUser(), taxpayerId);
						log.log(Level.SEVERE, message);				
						throw new ValidationException(
								"User has no permission for uploading file " + doc.getFilename() + ".");
					}
				}
				catch (ValidationException ex) {
					throw ex;
				}
				catch (Exception ex) {
					setSituation(doc, DocumentSituation.INVALID);
					String errorMessage = (ex.getMessage()==null) ? "" : ex.getMessage().replaceAll("[\\,\\(\\)\\{\\}]", "");
					validations.addLogError("{error.internal.server("+errorMessage+")}");
					saveValidationMessages(validationContext);
					log.log(Level.SEVERE, String.format("Exception while checking user permission for uploading file %s", documentId), ex);				
					throw new ValidationException(
							"An error ocurred while attempting to check user permission for uplading file " + doc.getFilename() + ".", ex);					
				}
			}
			
			// Add unique Id according to file uniqueness criteria
			if (fileUniquenessValues!=null && !fileUniquenessValues.isEmpty()) {
				String uniqueId = fileUniquenessFields.stream().map(f->ValidationContext.toString(fileUniquenessValues.get(f.getFieldName()))).collect(Collectors.joining("_"));
				doc.setUniqueId(uniqueId);
			}
			// If the template does not define any uniqueness criteria, use the combination of taxpayerId with tax period as uniqueness criteria
			else {
				doc.setUniqueId(String.format("%s_%d", doc.getTaxPayerId(), doc.getTaxPeriodNumber()));
			}
			
			// Update document on database
			doc = documentsUploadedRepository.saveWithTimestamp(doc);
			validationContext.setDocumentUploaded(doc);
			
			final long elapsed_time_saving = System.currentTimeMillis() - timestamp;
			timestamp = System.currentTimeMillis();

			boolean acceptIncompleteFiles = Boolean.TRUE.equals(docInputExpected.getAcceptIncompleteFiles()) ;

			// Should perform generic validations:
			
			// check for mismatch in field types (should try to automatically convert some
			// field types, e.g. String -> Date)
			validations.checkForFieldDataTypes(acceptIncompleteFiles);

			final long elapsed_time_chk_field_types = System.currentTimeMillis() - timestamp;
			timestamp = System.currentTimeMillis();

			// check for required fields (should be performed after type validation, because the type
			// validation may result in more 'null' values)
			validations.checkForRequiredFields(acceptIncompleteFiles);

			final long elapsed_time_chk_required_fields = System.currentTimeMillis() - timestamp;
			timestamp = System.currentTimeMillis();

			// check for domain table fields
			validations.checkForDomainTableValues(acceptIncompleteFiles);

			final long elapsed_time_chk_domain_tables = System.currentTimeMillis() - timestamp;
			timestamp = System.currentTimeMillis();

			if (validationContext.hasAlerts()) {
				setSituation(doc, DocumentSituation.INVALID);
				log.log(Level.SEVERE, "Not all field values are provided or compatible with specified field types on document {0}. Please check document error messagens for details.", documentId);
				saveValidationMessages(validationContext);
				throw new ValidationException("There are errors on file " + doc.getFilename() + ". Please check.");
			}
			else if (validationContext.hasNonCriticalAlerts()) {
				saveValidationMessages(validationContext);				
			}

			final long elapsed_time_saving_validation_alerts = System.currentTimeMillis() - timestamp;
			timestamp = System.currentTimeMillis();

			// Check for domain-specific validations related to a built-in archetype
			if (opTemplate.get().getArchetype() != null && opTemplate.get().getArchetype().trim().length() > 0) {
				Optional<TemplateArchetype> archetype = TemplateArchetypes.getArchetype(opTemplate.get().getArchetype());
				if (archetype.isPresent()) {

					boolean ok = archetype.get().validateDocumentUploaded(validationContext);
					if (!ok) {

						if (!validationContext.hasAlerts()) {
							// If the validation failed but we got no specific warning message, we will use a generic one
							validationContext.addAlert("{error.invalid.file}");
						}
						setSituation(doc, DocumentSituation.INVALID);
						String message = String.format("The validation check of %s does not conform to the archetype %s. Please check document error messagens for details.", documentId, archetype.get().getName());
						log.log(Level.SEVERE, message);
						saveValidationMessages(validationContext);
						throw new ValidationException("There are errors on file " + doc.getFilename() + ". Please check.");
					}

				}
			}

			final long elapsed_time_domain_specific = System.currentTimeMillis() - timestamp;
			timestamp = System.currentTimeMillis();

			if (log.isLoggable(Level.INFO)) {
				String msg = String.format("Finished validation of a message with documentId %s, stored file: %s, original file: %s, template: %s, taxpayer: %s, year: %d. "
				+ "Time elapsed: prep: %d ms , parse: %d ms , info: %d ms , save: %d ms , req: %d ms , types: %d ms , domain: %d ms , alerts: %d ms , specific: %d ms",
						documentId,
						filePath.getFileName(),
						doc.getFilename(),
						doc.getTemplateName(),
						doc.getTaxPayerId(),
						doc.getTaxYear(),
						elapsed_time_prepare_parser,
						elapsed_time_parsing,
						elapsed_time_filling_taxpayer_info,
						elapsed_time_saving,
						elapsed_time_chk_required_fields,
						elapsed_time_chk_field_types,
						elapsed_time_chk_domain_tables,
						elapsed_time_saving_validation_alerts,
						elapsed_time_domain_specific);
				log.log(Level.INFO, msg);
			}

			// Stores validated data at Elastic Search
			validatedDataStorageService.storeValidatedData(validationContext);

			doc = setSituation(doc, DocumentSituation.VALID);
			validationContext.setDocumentUploaded(doc);

			return true;

		} catch (GeneralException ex) {
			callRollbackProcedures(rollbackProcedures);
			log.log(Level.SEVERE, ex.getMessage(), ex);
			throw ex;
		} finally {
			
			processingDocument.remove(documentId);

			//After save document, check if threre is a situation with no Taxpayer Id information and, if found, update
			
			List<DocumentSituationHistory> situations = 
					documentsSituationHistoryRepository.findByDocumentId(validationContext.getDocumentUploaded().getId());
			
			if ( situations != null && !situations.isEmpty() ) {
				
				for ( DocumentSituationHistory situation : situations ) {
					
					if ( situation.getTaxPayerId() == null ) {
						
						try {
							situation.setTaxPayerId(validationContext.getDocumentUploaded().getTaxPayerId());
							situation.setTaxPeriodNumber(validationContext.getDocumentUploaded().getTaxPeriodNumber());
							documentsSituationHistoryRepository.save(situation);
						} catch (Exception e) {
							log.log(Level.WARNING, String.format("Can't update document situation history for document id %s", 
									validationContext.getDocumentUploaded().getId()), e);
						}
						
					}
					
				}
				
			}
			
		}

	}

	/**
	 * Save validation error/alert messages to database
	 * 
	 * @param validationContext The context on the validation
	 */
	private void saveValidationMessages(ValidationContext validationContext) {

		if (validationContext == null)
			return;

		if (!validationContext.hasAlerts() && !validationContext.hasNonCriticalAlerts())
			return;

		DocumentUploaded doc = validationContext.getDocumentUploaded();

		if (doc == null)
			return;

		DocumentValidationErrorMessage message = DocumentValidationErrorMessage.create()
				.withTemplateName(doc.getTemplateName())
				.withDocumentId(doc.getId())
				.withDocumentFilename(doc.getFilename())
				.withTimestamp(doc.getTimestamp())
				.withTaxPayerId(doc.getTaxPayerId())
				.withTimestamp(doc.getTimestamp())
				.withTaxPeriodNumber(doc.getTaxPeriodNumber());

		List<String> alerts = validationContext.getAlerts();

		if (alerts != null && !alerts.isEmpty()) {
			List<DocumentValidationErrorMessage> messages =
			alerts.stream()
				.limit(maxValidationErrorsPerUpload<=0?Integer.MAX_VALUE:maxValidationErrorsPerUpload*10)
				.distinct()
				.limit(maxValidationErrorsPerUpload<=0?Integer.MAX_VALUE:maxValidationErrorsPerUpload)
				.map(alert -> {
				DocumentValidationErrorMessage newMessage = message.clone();
				newMessage.setErrorMessage(alert);				
				return newMessage;
			}).collect(Collectors.toList());
			if (!messages.isEmpty())
				documentValidationErrorMessageRepository.saveAllWithTimestamp(messages);
		}
		
		alerts = validationContext.getNonCriticalAlerts();

		if (alerts != null && !alerts.isEmpty()) {
			List<DocumentValidationErrorMessage> messages =			
			alerts.stream()
				.limit(maxValidationErrorsPerUpload<=0?Integer.MAX_VALUE:maxValidationErrorsPerUpload*10)
				.distinct()
				.limit(maxValidationErrorsPerUpload<=0?Integer.MAX_VALUE:maxValidationErrorsPerUpload)
				.map(alert -> {
				DocumentValidationErrorMessage newMessage = message.clone();
				newMessage.setErrorMessage(alert);
				return newMessage;
			}).collect(Collectors.toList());
			if (!messages.isEmpty())
				documentValidationErrorMessageRepository.saveAllWithTimestamp(messages);
		}
	}

	/**
	 * Changes the situation for a given DocumentUploaded and saves new situation on
	 * DocumentSituationHistory
	 *
	 * @param doc          Document to be updated
	 * @param docSituation Document Situation to be saved
	 */
	private DocumentUploaded setSituation(DocumentUploaded doc, DocumentSituation docSituation) {

		doc.setSituation(docSituation);

		DocumentUploaded savedDoc = documentsUploadedRepository.saveWithTimestamp(doc);
		// in case of error delete the DocumentUploaded

		DocumentSituationHistory situation = DocumentSituationHistory.create()
		.withDocumentId(savedDoc.getId())
		.withSituation(docSituation)
		.withTimestamp(doc.getChangedTime())
		.withDocumentFilename(doc.getFilename())
		.withTemplateName(doc.getTemplateName())
		.withTaxPeriodNumber(doc.getTaxPeriodNumber())
		.withTaxPayerId(doc.getTaxPayerId());
		documentsSituationHistoryRepository.saveWithTimestamp(situation);

		return savedDoc;

	}

	/**
	 * Given multiple possible choices of DocumentInput for an incoming file, tries
	 * to figure out which one should be used.
	 */
	public DocumentInput chooseFileInput(Path path, String filename, List<DocumentInput> possibleInputs) {

		// First let's try to find a particular DocumentInput matching the filename
		// (e.g. looking the file extension)
		List<DocumentInput> matchingDocumentInputs = new LinkedList<>();
		Map<DocumentInput, FileFormat> mapFileFormats = new IdentityHashMap<>();
		for (DocumentInput input : possibleInputs) {
			DocumentFormat format = input.getFormat();
			if (format == null)
				continue;
			FileFormat fileFormat = FileFormatFactory.getFileFormat(format);
			if (fileFormat == null)
				continue;
			if (Boolean.FALSE.equals(fileFormat.matchFilename(filename)))
				continue;
			// This is a valid candidate, but let's try others
			matchingDocumentInputs.add(input);
			mapFileFormats.put(input, fileFormat);
		} // LOOP over DocumentInput's

		// If got only one option, this is the one
		if (matchingDocumentInputs.isEmpty())
			return null;
		if (matchingDocumentInputs.size() == 1)
			return matchingDocumentInputs.get(0);

		// Let's read the beginning of the file and try to match according to some
		// 'magic number'
		byte[] header = new byte[SAMPLE_SIZE]; // let's read just this much from the file beginning
		int headerLength;
		try (InputStream inputStream = new BufferedInputStream(new FileInputStream(path.toFile()))) {
			headerLength = inputStream.read(header);
		} catch (IOException ex) {
			log.log(Level.SEVERE, String.format("Error reading file contents %s", path.toString()), ex);
			return null;
		}

		List<DocumentInput> matchingDocumentInputs2ndRound = new LinkedList<>();
		for (DocumentInput input : matchingDocumentInputs) {
			FileFormat fileFormat = mapFileFormats.get(input);
			if (Boolean.FALSE.equals(fileFormat.matchFileHeader(header, headerLength)))
				continue;
			// This is a valid candidate, but let's try others
			matchingDocumentInputs2ndRound.add(input);
		} // LOOP over DocumentInput's

		if (matchingDocumentInputs2ndRound.isEmpty())
			return null;
		if (matchingDocumentInputs2ndRound.size() == 1)
			return matchingDocumentInputs2ndRound.get(0);

		// If we got more than one option, it's not possible to proceed
		String message = String.format("There is ambiguity to resolve file %s into one of the %s possible file format options!", path.toString(), matchingDocumentInputs2ndRound.size());
		log.log(Level.SEVERE, message);
		return null;
	}

	/**
	 * Try to rollback any transactions that wasn't finished correctly
	 *
	 * @param rollbackProcedures A list ou {@link Runnable} with data to be rolled
	 *                           back.
	 */
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
	 * Fetch information from filename according to the configurations in
	 * DocumentInput. Feed this information in the records stored in
	 * ValidationContext
	 */
	public static void fetchInformationFromFileName(DocumentInput docInputExpected,
			ValidationContext validationContext) {

		final String filename = validationContext.getDocumentUploaded().getFilename();

		for (DocumentInputFieldMapping field : docInputExpected.getFields()) {

			String expr = field.getFileNameExpression();
			if (expr == null || expr.trim().length() == 0)
				continue;

			Pattern p;
			try {
				p = Pattern.compile(expr, Pattern.CASE_INSENSITIVE);
			} catch (Exception ex) {
				continue;
			}

			Matcher m = p.matcher(filename);
			if (m.find()) {
				String capturedInformation;
				if (m.groupCount() > 0) {
					capturedInformation = m.group(1);
				} else {
					capturedInformation = m.group();
				}
				validationContext.setFieldInParsedContents(field.getFieldName(), capturedInformation);
			}

		} // LOOP over DocumentInputFieldMapping's

	}
}
