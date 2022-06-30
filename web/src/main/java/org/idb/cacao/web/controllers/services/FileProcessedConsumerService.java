/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.services;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.idb.cacao.api.DocumentSituation;
import org.idb.cacao.api.DocumentUploaded;
import org.idb.cacao.api.errors.DocumentNotFoundException;
import org.idb.cacao.api.errors.TemplateNotFoundException;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.utils.IndexNamesUtils;
import org.idb.cacao.web.repositories.DocumentTemplateRepository;
import org.idb.cacao.web.repositories.DocumentUploadedRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

/**
 * 
 * @author leon
 *
 */
@Service
public class FileProcessedConsumerService {

	private static final Logger log = Logger.getLogger(FileProcessedConsumerService.class.getName());

	@Autowired
	private DocumentUploadedRepository documentUploadedRepository;
	
	@Autowired
	private DocumentTemplateRepository documentTemplateRepository;
	
	@Autowired
	private KibanaSpacesService kibanaSpacesService;

	@Bean
	@CacheEvict(value={"years","accounts","qualifierValues"})
	public Consumer<String> receiveProcessedFile() {
		return documentId -> {
			log.log(Level.INFO, "Received a message with documentId " + documentId);	
			etlFinished(documentId);
		};
	}

	/**
	 * Do some final check-ups after the ETL has completed processing file
	 * 
	 * @param documentId	The ID of {@link DocumentUploaded} that finished processing
	 */
	private void etlFinished(String documentId) {
		
		try {
			
			DocumentUploaded doc = documentUploadedRepository.findById(documentId).orElse(null);
			if (doc==null)
				throw new DocumentNotFoundException("Document with id " + documentId + " wasn't found in database.");
			
			// If the document did not finish in PROCESSED state, let's ignore this
			if (!DocumentSituation.PROCESSED.equals(doc.getSituation())) {
				log.log(Level.FINE, "Document with id " + documentId + " finished in "+doc.getSituation()+" situation");
				return;
			}
			
			Optional<DocumentTemplate> template = documentTemplateRepository.findByNameAndVersion(doc.getTemplateName(),
					doc.getTemplateVersion());
			if (!template.isPresent()) {
				throw new TemplateNotFoundException("Template with name " + doc.getTemplateName() + " and version "
						+ doc.getTemplateVersion() + " wasn't found in database.");
			}
			
			String archetype = template.get().getArchetype();
			if (archetype!=null && archetype.trim().length()>0) {

				try {
					// Check if the index-patterns related to the archetype published data indices have already been created
					// If not created yet, creates automatically and synchronizes with other Kibana Spaces
					if (kibanaSpacesService.getMinimumDocumentsForAutoCreateIndexPattern()>0)
						kibanaSpacesService.syncKibanaIndexPatterns(/*avoidRedundantChecks*/true, archetype);
				}
				catch (Exception ex) {
					log.log(Level.SEVERE, "Failed to synchronize KIBANA spaces with index patterns related to the archetype "+archetype+" (may be ignored if there is no Kibana service online)", ex);
				}
				
			}
			else {

				String index = IndexNamesUtils.formatIndexNameForPublishedData(template.get().getName());
				try {
					// Check if the index-patterns related to the index of published data have already been created
					// If not created yet, creates automatically and synchronizes with other Kibana Spaces
					if (kibanaSpacesService.getMinimumDocumentsForAutoCreateIndexPattern()>0) {
						kibanaSpacesService.syncKibanaIndexPatternForGenericTemplate(/*avoidRedundantChecks*/true, index);
					}
				}
				catch (Exception ex) {
					log.log(Level.SEVERE, "Failed to synchronize KIBANA spaces with index patterns related to the index "+index+" (may be ignored if there is no Kibana service online)", ex);
				}

			}
			
		}
		catch (Exception ex) {
			log.log(Level.SEVERE, "Error checking status about the processed document with id "+documentId, ex);
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
			catch (Exception ex) {
				log.log(Level.SEVERE, "Could not rollback", ex);
			}
		}
	}
	
	
}
