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
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.idb.cacao.api.DocumentSituation;
import org.idb.cacao.api.DocumentSituationHistory;
import org.idb.cacao.api.DocumentUploaded;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.errors.DocumentNotFoundException;
import org.idb.cacao.api.errors.GeneralException;
import org.idb.cacao.api.errors.MissingConfigurationException;
import org.idb.cacao.api.errors.TemplateNotFoundException;
import org.idb.cacao.api.errors.UnknownFileFormatException;
import org.idb.cacao.api.storage.FileSystemStorageService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

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
    private FileSystemStorageService fileSystemStorageService;

    @Bean
    public Consumer<String> receiveAndValidateFile() {
        return documentId -> {
            log.log(Level.INFO, "Received message with documentId " + documentId);

            try {
                String validationResults = validateDocument(documentId);
                System.out.println(validationResults);
            } catch (MissingConfigurationException e) {
                log.log(Level.INFO, "Configuration is missing for document " + documentId);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Something went wrong with document " + documentId + " Exception: " + e);
            }
        };
    }

    /**
     * Try to validate a given uploaded document
     *
     * @param documentId The ID of {@link DocumentUploaded} that needs to be validated
     * @return DocumentId if the document has been validated. NULL if it doesn't.
     */
    private String validateDocument(String documentId) throws GeneralException, DocumentNotFoundException {

        log.log(Level.INFO, "Received a message with documentId " + documentId);

        List<Runnable> rollbackProcedures = new LinkedList<>(); // hold rollback procedures only to be used in case of error

        ValidationContext validationContext = new ValidationContext();

        try {

            DocumentUploaded doc = documentsUploadedRepository.findById(documentId).orElse(null);

            if (doc == null)
                throw new DocumentNotFoundException("Document with id " + documentId + " wasn't found in database.");

            validationContext.setDocumentUploaded(doc);

            Optional<DocumentTemplate> template = documentTemplateRepository.findByNameAndVersion(doc.getTemplateName(), doc.getTemplateVersion());
            if (template == null || !template.isPresent()) {
                setSituation(doc, DocumentSituation.INVALID);
                throw new TemplateNotFoundException("Template with name " + doc.getTemplateName() + " and version " + doc.getTemplateVersion() + " wasn't found in database.");
            }

            validationContext.setDocumentTemplate(template.get());

            String fullPath = doc.getFileIdWithPath();

            Path filePath = fileSystemStorageService.find(fullPath);
            validationContext.setDocumentPath(filePath);

//            System.out.println("File: " + filePath.getFileName());
//            System.out.println("Original file: " + doc.getFilename());
//            System.out.println("Template: " + doc.getTemplateName());

            setSituation(doc, DocumentSituation.ACCEPTED);

            // Check the DocumentInput related to this file

            List<DocumentInput> possibleInputs = template.get().getInputs();
            DocumentInput docInputExpected;
            if (possibleInputs == null || possibleInputs.isEmpty()) {
                setSituation(doc, DocumentSituation.INVALID);
                throw new MissingConfigurationException("Template with name " + doc.getTemplateName() + " and version " + doc.getTemplateVersion() + " was not configured with proper input format!");
            } else if (possibleInputs.size() == 1) {
                docInputExpected = possibleInputs.get(0);
            } else {
                // If we have more than one possible input for the same DocumentTemplate, we need to choose one
                docInputExpected = chooseFileInput(filePath, doc.getFilename(), possibleInputs);
                if (docInputExpected == null) {
                    setSituation(doc, DocumentSituation.INVALID);
                    throw new UnknownFileFormatException("The file did not match any of the expected file formats for template " + doc.getTemplateName() + " and version " + doc.getTemplateVersion());
                }
            }

            // Given the DocumentInput, get the corresponding FileFormat object
            DocumentFormat format = docInputExpected.getFormat();
            if (format == null) {
                setSituation(doc, DocumentSituation.INVALID);
                throw new MissingConfigurationException("Template with name " + doc.getTemplateName() + " and version " + doc.getTemplateVersion() + " was not configured with proper input format!");
            }
            FileFormat fileFormat = FileFormatFactory.getFileFormat(format);

            // Given the FileFormat, create a new FileParser
            FileParser parser = fileFormat.createFileParser();

            // Initializes the FileParser for processing
            parser.setPath(filePath);
            parser.setDocumentInputSpec(docInputExpected);
            // TODO: more setup ???

            // Let's start parsing the file contents
            parser.start();
            DataIterator iterator = null;

            try {

                iterator = parser.iterator();

                while (iterator.hasNext()) {

                    Map<String, Object> record = iterator.next();
                    if (record == null)
                        continue;
                    
                    for ( Map.Entry<String, Object> value : record.entrySet() ) {
                    	System.out.println(value.getKey() + " => " + value.getValue());
                    }

                    validationContext.addParsedContent(record);

                } // LOOP over each parsed record

            } catch (Exception e) {
                setSituation(doc, DocumentSituation.INVALID);
                log.log(Level.SEVERE, "Exception while parsing record for file " + documentId);
            } finally {

                if (iterator != null)
                    iterator.close();

                parser.close();
            }

            // Fetch information from file name according to the configuration
            fetchInformationFromFileName(docInputExpected, validationContext);

            // TODO:
            // Should perform generic validations:
            // ....
            // check for required fields
            // check for mismatch in field types (should try to automatically convert some field types, e.g. String -> Date)
            // check for domain table fields

            // Check for domain-specific validations related to a built-in archetype
            if (template.get().getArchetype() != null && template.get().getArchetype().trim().length() > 0) {
                Optional<TemplateArchetype> archetype = TemplateArchetypes.getArchetype(template.get().getArchetype());
                if (archetype != null && archetype.isPresent()) {

                    boolean ok = archetype.get().validateDocumentUploaded(validationContext);
                    if (!ok) {

                        // TODO: should report the warnings back to the ElasticSearch, to be displayed to the user !!!!
                        // ...

                    }

                }
            }

            return documentId;

        } catch (GeneralException ex) {
            callRollbackProcedures(rollbackProcedures);
            ex.printStackTrace();
            throw ex;
        } finally {
            //TODO Add logging
        }

    }

    /**
     * Changes the situation for a given DocumentUploaded and saves new situation on DocumentSituationHistory
     *
     * @param doc          Document to be updated
     * @param docSituation Document Situation to be saved
     */
    private void setSituation(DocumentUploaded doc, DocumentSituation docSituation) {

        doc.setSituation(docSituation);

        DocumentUploaded savedDoc = documentsUploadedRepository.saveWithTimestamp(doc);
        //rollbackProcedures.add(()->documentsUploadedRepository.delete(savedDoc)); // in case of error delete the DocumentUploaded

        DocumentSituationHistory situation = new DocumentSituationHistory();
        situation.setDocumentId(savedDoc.getId());
        situation.setSituation(docSituation);
        situation.setTimestamp(doc.getChangedTime());
        situation.setDocumentFilename(doc.getFilename());
        situation.setTemplateName(doc.getTemplateName());
        documentsSituationHistoryRepository.save(situation);

    }

    /**
     * Given multiple possible choices of DocumentInput for an incoming file, tries to figure out which one
     * should be used.
     */
    public DocumentInput chooseFileInput(Path path, String filename, List<DocumentInput> possibleInputs) {

        // First let's try to find a particular DocumentInput matching the filename (e.g. looking the file extension)
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

        // Let's read the beginning of the file and try to match according to some 'magic number'
        byte[] header = new byte[SAMPLE_SIZE];    // let's read just this much from the file beginning
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
        log.log(Level.SEVERE, "There is ambiguity to resolve file " + path.toString() + " into one of the " + matchingDocumentInputs2ndRound.size() + " possible file format options!");
        return null;
    }

    /**
     * Try to rollback any transactions that wasn't finished correctly
     *
     * @param rollbackProcedures A list ou {@link Runnable} with data to be rolled back.
     */
    public static void callRollbackProcedures(Collection<Runnable> rollbackProcedures) {
        if (rollbackProcedures == null || rollbackProcedures.isEmpty())
            return;
        for (Runnable proc : rollbackProcedures) {
            try {
                proc.run();
            } catch (Throwable ex) {
                //TODO Add logging
                log.log(Level.SEVERE, "Could not rollback", ex);
            }
        }
    }

    /**
     * Fetch information from filename according to the configurations in DocumentInput. Feed this information in the records
     * stored in ValidationContext
     */
    public static void fetchInformationFromFileName(DocumentInput docInputExpected, ValidationContext validationContext) {
    	
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
