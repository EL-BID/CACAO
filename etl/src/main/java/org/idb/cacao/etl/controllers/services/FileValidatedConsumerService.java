package org.idb.cacao.etl.controllers.services;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.elasticsearch.client.RestHighLevelClient;
import org.idb.cacao.api.DocumentSituation;
import org.idb.cacao.api.DocumentSituationHistory;
import org.idb.cacao.api.DocumentUploaded;
import org.idb.cacao.api.ETLContext;
import org.idb.cacao.api.ETLContext.ValidatedDataRepository;
import org.idb.cacao.api.errors.DocumentNotFoundException;
import org.idb.cacao.api.errors.GeneralException;
import org.idb.cacao.api.errors.TemplateNotFoundException;
import org.idb.cacao.api.storage.FileSystemStorageService;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.TemplateArchetype;
import org.idb.cacao.api.templates.TemplateArchetypes;
import org.idb.cacao.etl.loader.PublishedDataLoader;
import org.idb.cacao.etl.repositories.DocumentSituationHistoryRepository;
import org.idb.cacao.etl.repositories.DocumentTemplateRepository;
import org.idb.cacao.etl.repositories.DocumentValidatedRepository;
import org.idb.cacao.etl.repositories.DomainTableRepository;
import org.idb.cacao.etl.repositories.TaxpayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
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
	
	/**
	 * Object used to resolve errors according to a specific language
	 */
	@Autowired
	private MessageSource messageSource;
	
	@Autowired
	private ValidatedDataRepository validatedDataRepository;

	@Autowired
	private DocumentTemplateRepository documentTemplateRepository;
	
	@Autowired
	private DomainTableRepository domainTableRepository;
	
	@Autowired
	private RestHighLevelClient elasticsearchClient;
	
	@Autowired
	private TaxpayerRepository taxpayerRepository;

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
		
		ETLContext etlContext = new ETLContext();
		etlContext.setMessageSource(messageSource);
		etlContext.setValidatedDataRepository(validatedDataRepository);
		etlContext.setDomainTableRepository(domainTableRepository);
		etlContext.setTaxpayerRepository(taxpayerRepository);
		
		try {
		
			DocumentUploaded doc = documentValidatedRepository.findById(documentId).orElse(null);
			
			if ( doc == null )
				throw new DocumentNotFoundException("Document with id " + documentId + " wasn't found in database.");
			
			etlContext.setDocumentUploaded(doc);
			
			Optional<DocumentTemplate> template = documentTemplateRepository.findByNameAndVersion(doc.getTemplateName(),
					doc.getTemplateVersion());
			if (template == null || !template.isPresent()) {
				throw new TemplateNotFoundException("Template with name " + doc.getTemplateName() + " and version "
						+ doc.getTemplateVersion() + " wasn't found in database.");
			}

			etlContext.setDocumentTemplate(template.get());
			
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
			
			final PublishedDataLoader publishedDataLoader = new PublishedDataLoader(elasticsearchClient);
			etlContext.setLoadDataStrategy(publishedDataLoader);

			// Check for domain-specific validations related to a built-in archetype
			if (template.get().getArchetype() != null && template.get().getArchetype().trim().length() > 0) {
				Optional<TemplateArchetype> archetype = TemplateArchetypes.getArchetype(template.get().getArchetype());
				if (archetype != null && archetype.isPresent()) {
					
					boolean ok = archetype.get().performETL(etlContext);
					if (!ok) {
						
						// TODO:

						log.log(Level.SEVERE, "The ETL of "
								+ documentId + " does not conform to the archetype "+archetype.get().getName()+". Please check document error messagens for details.");
						//throw new ValidationException("There are errors on file " + doc.getFilename() + ". Please check.");
					}

				}
			}
			
			// TODO: should fall back to a default ETL behaviour in case there is no archetype for this job
			// For example, should generated denormalized view of parsed contents, expanding all references
			// to domain tables and expanding taxpayers records
			
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
