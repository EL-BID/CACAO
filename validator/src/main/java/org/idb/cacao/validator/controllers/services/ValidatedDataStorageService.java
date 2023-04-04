/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.validator.controllers.services;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.idb.cacao.api.ValidatedDataFieldNames;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.errors.CommonErrors;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.utils.IndexNamesUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service that stores the validated data, making it available for the final ETL stages.
 * 
 * @author Gustavo Figueiredo
 *
 */
@Service
public class ValidatedDataStorageService {

	private static final Logger log = Logger.getLogger(ValidatedDataStorageService.class.getName());

	@Autowired
	private RestHighLevelClient elasticsearchClient;

	@Value("${spring.elasticsearch.rest.connection-timeout}")
	private String elasticSearchConnectionTimeout;

	/**
	 * Default 'batch size' for performing partial commits over large files
	 */
	private static final int DEFAULT_BATCH_SIZE = 10_000;

	/**
	 * Bulk loads validated data into ElasticSearch index
	 */
	public void storeValidatedData(ValidationContext context) {
		
		DocumentTemplate template = context.getDocumentTemplate();
		List<Map<String,Object>> parsedContents = context.getParsedContents();
		if (parsedContents==null) 
			parsedContents = new LinkedList<>();
		if (parsedContents.isEmpty()) {
			parsedContents.add(new HashMap<>()); // at least one empty record
		}
		
		String fileId = context.getDocumentUploaded().getFileId();
		
		final String index_name = IndexNamesUtils.formatIndexNameForValidatedData(template);
		
        BulkRequest request = new BulkRequest();
        
        int count = 0;
        
        OffsetDateTime timestamp = context.getDocumentUploaded().getTimestamp();

		final int batchSize = DEFAULT_BATCH_SIZE;
		final LongAdder countInBatch = new LongAdder();

		for (Map<String,Object> dataRecord: parsedContents) {	
			
			String rowId = String.format("%s.%014d", fileId, ++count);
			
			// Formats all field names according to ElasticSearch standards
			Map<String,Object> normalizedRecord = IndexNamesUtils.normalizeAllKeysForES(dataRecord);
			
			// Includes additional metadata
			normalizedRecord.put(ValidatedDataFieldNames.FILE_ID.name(), fileId);
			normalizedRecord.put(ValidatedDataFieldNames.TIMESTAMP.name(), timestamp);
			normalizedRecord.put(ValidatedDataFieldNames.LINE.name(), count);
			
			// Add this record to index
			request.add(new IndexRequest(index_name)
				.id(rowId)
				.source(normalizedRecord));

			countInBatch.increment();
			if (countInBatch.intValue()>=batchSize) {
				try {
					request.timeout(elasticSearchConnectionTimeout);
					request.setRefreshPolicy(RefreshPolicy.NONE);
					CommonErrors.doESWriteOpWithRetries(
							()->elasticsearchClient.bulk(request,
							RequestOptions.DEFAULT));
					request.requests().clear();
				}
				catch (Exception ex) {
					log.log(Level.SEVERE, String.format("Error while storing %d rows for file %s for index '%s' for template '%s %s' ", countInBatch.intValue(), fileId, index_name, template.getName(), template.getVersion()), ex);
					throw new RuntimeException(ex);
				}
				countInBatch.reset();
			}

		} // LOOP over parsed data records
		
		if (countInBatch.intValue()>0) {
			request.timeout(elasticSearchConnectionTimeout);
			request.setRefreshPolicy(RefreshPolicy.NONE);
			try {
				CommonErrors.doESWriteOpWithRetries(
					()->elasticsearchClient.bulk(request,
					RequestOptions.DEFAULT));
			}
			catch (Exception ex) {
				String message = String.format("Error while storing %d rows for file %s for index '%s' for template '%s %s' ", count, fileId, index_name, template.getName(), template.getVersion());
				log.log(Level.SEVERE, message, ex);
			}
			request.requests().clear();
		}

		try {
			elasticsearchClient.indices().refresh(new RefreshRequest(index_name), RequestOptions.DEFAULT);
		}
		catch (IOException ex) {
			String message = String.format("Error while refreshing after insertion of %d rows for file %s for index '%s' for template '%s %s", count, fileId, index_name, template.getName(), template.getVersion());
			log.log(Level.SEVERE, message, ex);			
		}
	}
}
