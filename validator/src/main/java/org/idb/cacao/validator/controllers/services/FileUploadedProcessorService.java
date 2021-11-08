package org.idb.cacao.validator.controllers.services;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.idb.cacao.api.DocumentSituation;
import org.idb.cacao.api.DocumentSituationHistory;
import org.idb.cacao.api.DocumentUploaded;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.errors.DocumentNotFoundException;
import org.idb.cacao.api.errors.GeneralException;
import org.idb.cacao.api.storage.FileSystemStorageService;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.TemplateArchetype;
import org.idb.cacao.api.templates.TemplateArchetypes;
import org.idb.cacao.validator.repositories.DocumentSituationHistoryRepository;
import org.idb.cacao.validator.repositories.DocumentTemplateRepository;
import org.idb.cacao.validator.repositories.DocumentUploadedRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
public class FileUploadedProcessorService {
	
	private static final Logger log = Logger.getLogger(FileUploadedProcessorService.class.getName());
	
	@Autowired
	private DocumentUploadedRepository documentsUploadedRepository;
	
	@Autowired
	private DocumentTemplateRepository documentTemplateRepository;
	
	@Autowired
	private DocumentSituationHistoryRepository documentsSituationHistoryRepository;
	
	@Autowired
	private FileSystemStorageService fileSystemStorageService;

	@Bean
	public Function<String, String> receiveAndValidateFile() {		
		return documentId -> {
			return validateDocument(documentId);			
		};
	}
	
	/**
	 * Try to validate a given uploaded document
	 * 
	 * @param documentId	The ID of {@link DocumentUploaded} that needs to be validated
	 * @return	DocumentId if the document has been validated. NULL if it doesn't.
	 */
	private String validateDocument(String documentId) throws GeneralException, DocumentNotFoundException {
		
		log.log(Level.INFO, "Received a message with documentId " + documentId);
		
		List<Runnable> rollbackProcedures = new LinkedList<>(); // hold rollback procedures only to be used in case of error
		
		ValidationContext validationContext = new ValidationContext();

		try {
		
			DocumentUploaded doc = documentsUploadedRepository.findById(documentId).orElse(null);
			
			if ( doc == null )
				throw new DocumentNotFoundException("Document with id " + documentId + " wasn't found in database.");
			
			validationContext.setDocumentUploaded(doc);
			
			Optional<DocumentTemplate> template = documentTemplateRepository.findByNameAndVersion(doc.getTemplateName(), doc.getTemplateVersion());
			if (template==null || !template.isPresent()) {
				throw new DocumentNotFoundException("Template with name " + doc.getTemplateName() + " and version " + doc.getTemplateVersion() + " wasn't found in database.");
			}
			
			validationContext.setDocumentTemplate(template.get());
			
			String fullPath = doc.getFileIdWithPath();
			
			Path filePath = fileSystemStorageService.find(fullPath);
			validationContext.setDocumentPath(filePath);

			System.out.println("File: " + filePath.getFileName());
			System.out.println("Original file: " + doc.getFilename());
			System.out.println("Template: " + doc.getTemplateName());
			
			doc.setSituation(DocumentSituation.ACCEPTED);
			
			//DocumentUploaded savedDoc = documentsUploadedRepository.saveWithTimestamp(doc);			
			//rollbackProcedures.add(()->documentsUploadedRepository.delete(savedDoc)); // in case of error delete the DocumentUploaded
			
			DocumentSituationHistory situation = new DocumentSituationHistory();
			situation.setDocumentId(documentId);
			situation.setSituation(DocumentSituation.ACCEPTED);
			situation.setTimestamp(doc.getChangedTime());
			situation.setDocumentFilename(doc.getFilename());
			situation.setTemplateName(doc.getTemplateName());
			DocumentSituationHistory savedSituation = documentsSituationHistoryRepository.save(situation);
			
			rollbackProcedures.add(()->documentsSituationHistoryRepository.delete(savedSituation)); // in case of error delete the DocumentUploaded
			
			// TODO:
			// Should fill validationContext.parsedContents with the parsed contents from the incoming file !!!!!
			// ....
			
			
			// Check for domain-specific validations related to a built-in archetype
			if (template.get().getArchetype()!=null && template.get().getArchetype().trim().length()>0) {
				Optional<TemplateArchetype> archetype = TemplateArchetypes.getArchetype(template.get().getArchetype());
				if (archetype!=null && archetype.isPresent()) {
					
					boolean ok = archetype.get().validateDocumentUploaded(validationContext);
					if (!ok) {
						
						// TODO: should report the warnings back to the ElasticSearch, to be displayed to the user !!!!
						// ...
						
					}
					
				}
			}
			
			return documentId;
		
		}
		catch (GeneralException ex) {
			callRollbackProcedures(rollbackProcedures);
			throw ex;
		}
		finally {
			//TODO Add logging
		}
		
	}
	
	/**
	 * Try to rollback any transactions that wasn't finished correctly 
	 * 
	 * @param rollbackProcedures	A list ou {@link Runnable} with data to be rolled back.
	 */
	public static void callRollbackProcedures(Collection<Runnable> rollbackProcedures) {
		if (rollbackProcedures==null || rollbackProcedures.isEmpty())
			return;
		for (Runnable proc: rollbackProcedures) {
			try {
				proc.run();
			}
			catch (Throwable ex) {
				//TODO Add logging
				log.log(Level.SEVERE, "Could not rollback", ex);
			}
		}
	}	
}
