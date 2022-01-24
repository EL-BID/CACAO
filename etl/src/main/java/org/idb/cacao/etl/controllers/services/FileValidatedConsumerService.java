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
package org.idb.cacao.etl.controllers.services;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.elasticsearch.client.RestHighLevelClient;
import org.idb.cacao.api.DocumentSituation;
import org.idb.cacao.api.DocumentSituationHistory;
import org.idb.cacao.api.DocumentUploaded;
import org.idb.cacao.api.DocumentValidationErrorMessage;
import org.idb.cacao.api.ETLContext;
import org.idb.cacao.api.ETLContext.ValidatedDataRepository;
import org.idb.cacao.api.errors.DocumentNotFoundException;
import org.idb.cacao.api.errors.GeneralException;
import org.idb.cacao.api.errors.TemplateNotFoundException;
import org.idb.cacao.api.storage.FileSystemStorageService;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.TemplateArchetype;
import org.idb.cacao.api.templates.TemplateArchetypes;
import org.idb.cacao.etl.loader.GenericDataPublisher;
import org.idb.cacao.etl.loader.PublishedDataLoader;
import org.idb.cacao.etl.repositories.DocumentSituationHistoryRepository;
import org.idb.cacao.etl.repositories.DocumentTemplateRepository;
import org.idb.cacao.etl.repositories.DocumentValidatedRepository;
import org.idb.cacao.etl.repositories.DomainTableRepository;
import org.idb.cacao.etl.repositories.TaxpayerRepository;
import org.idb.cacao.etl.repositories.DocumentValidationErrorMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
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
	
	@Value("${spring.elasticsearch.rest.connection-timeout}")
	private String elasticSearchConnectionTimeout;

	@Autowired
	private TaxpayerRepository taxpayerRepository;

	@Autowired
	private DocumentValidationErrorMessageRepository documentValidationErrorMessageRepository;

	@Autowired
	private final StreamBridge streamBridge;

	public FileValidatedConsumerService(StreamBridge streamBridge) {
		this.streamBridge = streamBridge;
	}
	
	@Bean
	public Consumer<String> receiveValidatedFile() {
		return documentId -> {
			Boolean result = processDocument(documentId);
			
			if (result) {
				log.log(Level.INFO, "Sending a message to WEB with documentId " + documentId);

				streamBridge.send("receiveValidatedFile-out-0", documentId);
			}
		};
	}
	
	
	/**
	 * Try to ETL a given uploaded document
	 * 
	 * @param documentId	The ID of {@link DocumentUploaded} that needs to be validated
	 * @return	DocumentId if the document has been validated. NULL if it doesn't.
	 */
	private Boolean processDocument(String documentId) throws GeneralException, DocumentNotFoundException {
		
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
			
			if (log.isLoggable(Level.INFO)) {
				log.log(Level.INFO, "Received a message with documentId " + documentId + ", stored file: "+filePath.getFileName()+", original file: "
					+doc.getFilename()+", template: "+doc.getTemplateName()+", taxpayer: "+doc.getTaxPayerId()+", year: "+doc.getTaxYear());
			}

			final PublishedDataLoader publishedDataLoader = new PublishedDataLoader(elasticsearchClient);
			publishedDataLoader.setTimeout(elasticSearchConnectionTimeout);
			etlContext.setLoadDataStrategy(publishedDataLoader);

			// Unless we have a specific ETL procedure, we shall perform the 'general purpose' ETL
			boolean should_perform_general_etl = true;
			
			// Check for domain-specific validations related to a built-in archetype
			if (template.get().getArchetype() != null && template.get().getArchetype().trim().length() > 0) {
				Optional<TemplateArchetype> archetype = TemplateArchetypes.getArchetype(template.get().getArchetype());
				if (archetype != null && archetype.isPresent()) {
					
					should_perform_general_etl = false; // prevents 'two ETL's over the same document
					
					boolean ok = archetype.get().performETL(etlContext);
					if (!ok) {
						
						log.log(Level.SEVERE, "The ETL of "
								+ documentId + " does not conform to the archetype "+archetype.get().getName()+". Please check document error messagens for details.");
					}

					if (ok && !etlContext.hasOutcomeSituations()) {
						// If it was successful but there was no outcome situation produced by the 'performETL' method, then we will consider 'fulfilled'
						setSituation(doc, DocumentSituation.PROCESSED);
					}
					else if (!ok && !etlContext.hasOutcomeSituations()) {
						// If there was an error and there was no outcome situation produced by the 'performETL' method, then we will consider 'pending'
						// For example, the system administration may have missed some required configuration. The file may be still valid, but it's impossible
						// to proceed.
						setSituation(doc, DocumentSituation.PENDING);
					}
					else {
						// In case of error or in case of success, write the outcome status
						for (Map.Entry<DocumentUploaded, DocumentSituation> entry: etlContext.getOutcomeSituations().entrySet()) {
							setSituation(entry.getKey(), entry.getValue());
						}
					}
				}
			}

			if (should_perform_general_etl) {
				
				// Fall back to a default ETL behavior in case there is no archetype for this job
				// For example, should generated denormalized view of parsed contents, expanding all references
				// to domain tables and expanding taxpayers records
				
				boolean ok = GenericDataPublisher.performETL(etlContext);
				if (!ok) {
					
					log.log(Level.SEVERE, "The ETL of "
							+ documentId + " failed to generate a denormalized view of the validated data. Please check document error messagens for details.");
				}

				if (ok && !etlContext.hasOutcomeSituations()) {
					// If it was successful but there was no outcome situation produced by the 'performETL' method, then we will consider 'fulfilled'
					setSituation(doc, DocumentSituation.PROCESSED);
				}
				else if (!ok && !etlContext.hasOutcomeSituations()) {
					// If there was an error and there was no outcome situation produced by the 'performETL' method, then we will consider 'pending'
					// For example, the system administration may have missed some required configuration. The file may be still valid, but it's impossible
					// to proceed.
					setSituation(doc, DocumentSituation.PENDING);
				}
				else {
					// In case of error or in case of success, write the outcome status
					for (Map.Entry<DocumentUploaded, DocumentSituation> entry: etlContext.getOutcomeSituations().entrySet()) {
						setSituation(entry.getKey(), entry.getValue());
					}
				}
				
			}
			
			saveETLMessages(etlContext);
			
			return true;
		
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
	 * Save ETL error/alert messages to database
	 * 
	 * @param etlContext The context on the ETL
	 */
	private void saveETLMessages(ETLContext etlContext) {

		if (etlContext == null)
			return;

		Map<DocumentUploaded,List<String>> alerts = etlContext.getAlerts();

		if (alerts == null || alerts.isEmpty())
			return;

		for (Map.Entry<DocumentUploaded,List<String>> entry: alerts.entrySet()) {
			
			DocumentUploaded doc = entry.getKey();
			
			DocumentValidationErrorMessage message = DocumentValidationErrorMessage.create()
					.withTemplateName(doc.getTemplateName())
					.withDocumentId(doc.getId())
					.withTimestamp(doc.getTimestamp())
					.withTaxPayerId(doc.getTaxPayerId())
					.withTaxPeriodNumber(doc.getTaxPeriodNumber())
					.withDocumentFilename(doc.getFilename());
	
			entry.getValue().parallelStream().forEach(alert -> {
				DocumentValidationErrorMessage newMessage = message.clone();
				newMessage.setErrorMessage(alert);
				documentValidationErrorMessageRepository.saveWithTimestamp(newMessage);
			});

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

		DocumentUploaded savedDoc = documentValidatedRepository.saveWithTimestamp(doc);
		// rollbackProcedures.add(()->documentsUploadedRepository.delete(savedDoc)); //
		// in case of error delete the DocumentUploaded

		DocumentSituationHistory situation = DocumentSituationHistory.create()
		.withDocumentId(savedDoc.getId())
		.withSituation(docSituation)
		.withTimestamp(doc.getChangedTime())
		.withDocumentFilename(doc.getFilename())
		.withTaxPayerId(doc.getTaxPayerId())
		.withTaxPeriodNumber(doc.getTaxPeriodNumber())
		.withTemplateName(doc.getTemplateName());
		documentsSituationHistoryRepository.saveWithTimestamp(situation);

		return savedDoc;

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
