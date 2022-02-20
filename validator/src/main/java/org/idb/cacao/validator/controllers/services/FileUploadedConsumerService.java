package org.idb.cacao.validator.controllers.services;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
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
import org.idb.cacao.api.templates.TemplateArchetype;
import org.idb.cacao.api.templates.TemplateArchetypes;
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
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

/**
 * 
 * @author leon
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

	public FileUploadedConsumerService(StreamBridge streamBridge) {
		this.streamBridge = streamBridge;
	}

	@Bean
	public Consumer<String> receiveAndValidateFile() {
		return documentId -> {
			log.log(Level.INFO, "Received message with documentId " + documentId);

			try {
				Boolean validated = validateDocument(documentId);

				if (validated) {
					log.log(Level.INFO, "Sending a message to ETL with documentId " + documentId);

					streamBridge.send("receiveAndValidateFile-out-0", documentId);
				}

			} catch (MissingConfigurationException e) {
				log.log(Level.INFO, "Configuration is missing for document " + documentId, e);
			} catch (Exception e) {
				log.log(Level.SEVERE, "Something went wrong with document " + documentId + " Exception: " + e, e);
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

		log.log(Level.INFO, "Received a message with documentId " + documentId);

		List<Runnable> rollbackProcedures = new LinkedList<>(); // hold rollback procedures only to be used in case of
																// error

		ValidationContext validationContext = new ValidationContext();

		try {
			// Recovering the document from the database
			DocumentUploaded doc = documentsUploadedRepository.findById(documentId).orElse(null);

			// Tests if the real document exists 
			if (doc == null)
				throw new DocumentNotFoundException("Document with id " + documentId + " wasn't found in database.");

			// Sets the document to validation context
			validationContext.setDocumentUploaded(doc);
			
			Validations validations = new Validations(validationContext, domainTableRepository);

			Optional<DocumentTemplate> template = documentTemplateRepository.findByNameAndVersion(doc.getTemplateName(),
					doc.getTemplateVersion());
			if (template == null || !template.isPresent()) {
				doc = setSituation(doc, DocumentSituation.INVALID);
				validations.addLogError("{doc.error.template.not.found}");
				saveValidationMessages(validationContext);
				throw new TemplateNotFoundException("Template with name " + doc.getTemplateName() + " and version "
						+ doc.getTemplateVersion() + " wasn't found in database.");
			}

			validationContext.setDocumentTemplate(template.get());

			String fullPath = doc.getFileIdWithPath();

			Path filePath = fileSystemStorageService.find(fullPath);
			validationContext.setDocumentPath(filePath);

			doc = setSituation(doc, DocumentSituation.ACCEPTED);
			validationContext.setDocumentUploaded(doc);

			// Check the DocumentInput related to this file

			List<DocumentInput> possibleInputs = template.get().getInputs();
			DocumentInput docInputExpected;
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

			// Given the DocumentInput, get the corresponding FileFormat object
			DocumentFormat format = docInputExpected.getFormat();
			if (format == null) {
				doc = setSituation(doc, DocumentSituation.INVALID);
				validations.addLogError("{doc.error.template.not.found}");
				saveValidationMessages(validationContext);
				throw new MissingConfigurationException("Template with name " + doc.getTemplateName() + " and version "
						+ doc.getTemplateVersion() + " was not configured with proper input format!");
			}
			FileFormat fileFormat = FileFormatFactory.getFileFormat(format);

			// Given the FileFormat, create a new FileParser
			FileParser parser = fileFormat.createFileParser();

			// Initializes the FileParser for processing
			parser.setPath(filePath);
			parser.setDocumentInputSpec(docInputExpected);
			// TODO: more setup ???

			// Let's start parsing the file contents
			try {
				parser.start();
			}
			catch (Throwable ex) {
				setSituation(doc, DocumentSituation.INVALID);
				validations.addLogError("{doc.error.parse}");
				saveValidationMessages(validationContext);
				log.log(Level.SEVERE, "Exception while parsing record for file " + documentId, ex);				
				throw new ValidationException(
						"An error ocurred while attempting to read data in file " + doc.getFilename() + ".", ex);				
			}
			
			DataIterator iterator = null;

			// If the template defines any field to be used as criteria of 'file uniqueness', we should
			// gather this information as well			
			List<DocumentField> fileUniquenessFields = template.get().getFileUniquenessFields();
			Map<String, Object> fileUniquenessValues = (fileUniquenessFields.isEmpty()) ? null : new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			Set<String> fileUniquenessViolationFieldNames = null;

			try {

				iterator = parser.iterator();
				
				if ( iterator == null ) {
					setSituation(doc, DocumentSituation.INVALID);
					validations.addLogError("{doc.error.no.records.found}");
					saveValidationMessages(validationContext);
					log.log(Level.SEVERE, "Impossible to read fields in file " + documentId);
					throw new ValidationException(
							"An error ocurred while attempting to read data in file " + doc.getFilename() + ".");
				}

				long added = 0;
				while (iterator.hasNext()) {

					Map<String, Object> record = iterator.next();

					if (record == null)
						continue;
					
					if (fileUniquenessValues!=null && fileUniquenessValues.size()<fileUniquenessFields.size()) {
						// Gather values for 'file uniqueness' criteria
						for (DocumentField fUniqueField: fileUniquenessFields) {
							Object value = record.get(fUniqueField.getFieldName());
							if (value==null)
								continue;
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

					validationContext.addParsedContent(record);
					added++;

				} // LOOP over each parsed record
				
				if ( added == 0 ) { //Records not found
					setSituation(doc, DocumentSituation.INVALID);
					validations.addLogError("{doc.error.no.records.found}");
					saveValidationMessages(validationContext);
					log.log(Level.SEVERE, "No records found on file " + documentId);
					throw new ValidationException(
							"No records found on file file " + doc.getFilename() + ".");					
				}					
				
				log.log(Level.INFO, added + " records added to validator from " + doc.getFilename());

			} catch (Exception e) {
				setSituation(doc, DocumentSituation.INVALID);
				validations.addLogError("{doc.error.parse}");
				saveValidationMessages(validationContext);
				log.log(Level.SEVERE, "Exception while parsing record for file " + documentId, e);				
				throw new ValidationException(
						"An error ocurred while attempting to read data in file " + doc.getFilename() + ".", e);
			} finally {

				if (iterator != null)
					iterator.close();

				parser.close();			
			}

			// Fetch information from file name according to the configuration
			fetchInformationFromFileName(docInputExpected, validationContext);

			// Add TaxPayerId and TaxPeriod to document on database
			validations.addTaxPayerInformation();
			
			if (doc.getTaxPayerId()!=null && doc.getTaxPayerId().trim().length()>0) {
				AtomicReference<String> userTaxpayerId = new AtomicReference<>();
				try {
					boolean has_access = usersTaxpayersService.isUserRepresentativeOf(doc.getUser(), doc.getTaxPayerId(), userTaxpayerId);
					if (!has_access) {
						// Reset the taxpayer Id indication in the DocumentUploaded because we don't want to list this occurrence for him
						String taxpayerId = doc.getTaxPayerId();
						doc.setTaxPayerId(userTaxpayerId.get());
						// Inform the user about the violation
						setSituation(doc, DocumentSituation.INVALID);
						validations.addLogError("{doc.error.user.not.representative("+doc.getUser().replaceAll("[\\,\\(\\)\\{\\}]", "")+","+taxpayerId.replaceAll("[\\,\\(\\)\\{\\}]", "")+")}");
						saveValidationMessages(validationContext);
						log.log(Level.SEVERE, "Exception while parsing record for file " + documentId + ": the user "+doc.getUser()+" does not represent the taxpayer "+taxpayerId);				
						throw new ValidationException(
								"User has no permission for uploading file " + doc.getFilename() + ".");
					}
				}
				catch (ValidationException ex) {
					throw ex;
				}
				catch (Throwable ex) {
					setSituation(doc, DocumentSituation.INVALID);
					String error_msg = (ex.getMessage()==null) ? "" : ex.getMessage().replaceAll("[\\,\\(\\)\\{\\}]", "");
					validations.addLogError("{error.internal.server("+error_msg+")}");
					saveValidationMessages(validationContext);
					log.log(Level.SEVERE, "Exception while checking user permission for uploading file " + documentId, ex);				
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
			
			//TODO
			//Check if uploader has rights to upload file for this taxpayer

			// Should perform generic validations:
			// check for required fields
			validations.checkForRequiredFields();

			// check for mismatch in field types (should try to automatically convert some
			// field types, e.g. String -> Date)
			validations.checkForFieldDataTypes();

			// check for domain table fields
			validations.checkForDomainTableValues();

			if (!validationContext.getAlerts().isEmpty()) {
				setSituation(doc, DocumentSituation.INVALID);
				log.log(Level.SEVERE, "Not all field values are provided or compatible with specified field types on document "
						+ documentId + ". " + "Please check document error messagens for details.");
				saveValidationMessages(validationContext);
				throw new ValidationException("There are errors on file " + doc.getFilename() + ". Please check.");
			}

			// Check for domain-specific validations related to a built-in archetype
			if (template.get().getArchetype() != null && template.get().getArchetype().trim().length() > 0) {
				Optional<TemplateArchetype> archetype = TemplateArchetypes.getArchetype(template.get().getArchetype());
				if (archetype != null && archetype.isPresent()) {

					boolean ok = archetype.get().validateDocumentUploaded(validationContext);
					if (!ok) {

						if (validationContext.getAlerts().isEmpty()) {
							// If the validation failed but we got no specific warning message, we will use a generic one
							validationContext.addAlert("{error.invalid.file}");
						}
						setSituation(doc, DocumentSituation.INVALID);
						log.log(Level.SEVERE, "The validation check of "
								+ documentId + " does not conform to the archetype "+archetype.get().getName()+". Please check document error messagens for details.");
						saveValidationMessages(validationContext);
						throw new ValidationException("There are errors on file " + doc.getFilename() + ". Please check.");
					}

				}
			}

			if (log.isLoggable(Level.INFO)) {
				log.log(Level.INFO, "Finished validation of a message with documentId " + documentId + ", stored file: "+filePath.getFileName()+", original file: "
					+doc.getFilename()+", template: "+doc.getTemplateName()+", taxpayer: "+doc.getTaxPayerId()+", year: "+doc.getTaxYear());
			}

			// Stores validated data at Elastic Search
			validatedDataStorageService.storeValidatedData(validationContext);

			doc = setSituation(doc, DocumentSituation.VALID);
			validationContext.setDocumentUploaded(doc);

			return true;

		} catch (GeneralException ex) {
			callRollbackProcedures(rollbackProcedures);
			ex.printStackTrace();
			throw ex;
		} finally {
			
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
							log.log(Level.WARNING, "Can't update document situation history for document id " + 
									validationContext.getDocumentUploaded().getId(), e);
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

		List<String> alerts = validationContext.getAlerts();

		if (alerts == null || alerts.isEmpty())
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

		alerts.stream().forEach(alert -> {
			DocumentValidationErrorMessage newMessage = message.clone();
			newMessage.setErrorMessage(alert);
			documentValidationErrorMessageRepository.saveWithTimestamp(newMessage);
		});

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
		// rollbackProcedures.add(()->documentsUploadedRepository.delete(savedDoc)); //
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
		int header_length;
		try (InputStream inputStream = new BufferedInputStream(new FileInputStream(path.toFile()))) {
			header_length = inputStream.read(header);
		} catch (IOException ex) {
			log.log(Level.SEVERE, "Error reading file contents " + path.toString(), ex);
			return null;
		}

		List<DocumentInput> matchingDocumentInputs2ndRound = new LinkedList<>();
		for (DocumentInput input : matchingDocumentInputs) {
			FileFormat fileFormat = mapFileFormats.get(input);
			if (Boolean.FALSE.equals(fileFormat.matchFileHeader(header, header_length)))
				continue;
			// This is a valid candidate, but let's try others
			matchingDocumentInputs2ndRound.add(input);
		} // LOOP over DocumentInput's

		if (matchingDocumentInputs2ndRound.isEmpty())
			return null;
		if (matchingDocumentInputs2ndRound.size() == 1)
			return matchingDocumentInputs2ndRound.get(0);

		// If we got more than one option, it's not possible to proceed
		log.log(Level.SEVERE, "There is ambiguity to resolve file " + path.toString() + " into one of the "
				+ matchingDocumentInputs2ndRound.size() + " possible file format options!");
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
			} catch (Throwable ex) {
				// TODO Add logging
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
			} catch (Throwable ex) {
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
