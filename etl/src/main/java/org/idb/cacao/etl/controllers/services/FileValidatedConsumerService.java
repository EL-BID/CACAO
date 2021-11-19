package org.idb.cacao.etl.controllers.services;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.idb.cacao.api.DocumentSituation;
import org.idb.cacao.api.DocumentSituationHistory;
import org.idb.cacao.api.DocumentUploaded;
import org.idb.cacao.api.errors.DocumentNotFoundException;
import org.idb.cacao.api.errors.GeneralException;
import org.idb.cacao.api.storage.FileSystemStorageService;
import org.idb.cacao.etl.repositories.DocumentSituationHistoryRepository;
import org.idb.cacao.etl.repositories.DocumentValidatedRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

/**
 * 
 * @author leon
 *
 */

@Service
public class FileValidatedConsumerService {
	private static final Logger log = Logger.getLogger(FileValidatedConsumerService.class.getName());

	
	@Autowired
	private DocumentValidatedRepository documentValidatedRepository;
	
	@Autowired
	private DocumentSituationHistoryRepository documentsSituationHistoryRepository;
	
	@Autowired
	private FileSystemStorageService fileSystemStorageService;
	
	
	@Bean
	public Consumer<String> receiveValidatedFile() {
		return documentId -> {
			processDocument(documentId);			
		};
	}
	
	
	/**
	 * Try to ETL a given uploaded document
	 * 
	 * @param documentId	The ID of {@link DocumentUploaded} that needs to be validated
	 * @return	DocumentId if the document has been validated. NULL if it doesn't.
	 */
	private String processDocument(String documentId) throws GeneralException, DocumentNotFoundException {
		
		log.log(Level.INFO, "Received a message with documentId " + documentId);
		
		List<Runnable> rollbackProcedures = new LinkedList<>(); // hold rollback procedures only to be used in case of error
		
		try {
		
			DocumentUploaded doc = documentValidatedRepository.findById(documentId).orElse(null);
			
			if ( doc == null )
				throw new DocumentNotFoundException("Document with id " + documentId + " wasn't found in database.");
			
			String fullPath = doc.getFileIdWithPath();
			
			Path filePath = fileSystemStorageService.find(fullPath);
			
			System.out.println("File: " + filePath.getFileName());
			System.out.println("Original file: " + doc.getFilename());
			System.out.println("Template: " + doc.getTemplateName());
			
			doc.setSituation(DocumentSituation.PROCESSED);
			
			//DocumentUploaded savedDoc = documentsUploadedRepository.saveWithTimestamp(doc);			
			//rollbackProcedures.add(()->documentsUploadedRepository.delete(savedDoc)); // in case of error delete the DocumentUploaded
			
			DocumentSituationHistory situation = new DocumentSituationHistory();
			situation.setDocumentId(documentId);
			situation.setSituation(DocumentSituation.PROCESSED);
			situation.setTimestamp(doc.getChangedTime());
			situation.setDocumentFilename(doc.getFilename());
			situation.setTemplateName(doc.getTemplateName());
			DocumentSituationHistory savedSituation = documentsSituationHistoryRepository.save(situation);
			
			rollbackProcedures.add(()->documentsSituationHistoryRepository.delete(savedSituation)); // in case of error delete the DocumentUploaded
			
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
