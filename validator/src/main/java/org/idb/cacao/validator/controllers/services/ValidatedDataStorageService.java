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
package org.idb.cacao.validator.controllers.services;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.utils.IndexNamesUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

		for (Map<String,Object> record: parsedContents) {	
			
			String rowId = String.format("%s.%014d", fileId, ++count);
			
			// Formats all field names according to ElasticSearch standards
			IndexNamesUtils.normalizeAllKeysForES(record);
			
			// Includes additional metadata
			record.put("_file_id", fileId);
			record.put("_timestamp", timestamp);
			
			// Add this record to index
        	request.add(new IndexRequest(index_name)
				.id(rowId)
				.source(record));

		} // LOOP over parsed data records
		
		request.setRefreshPolicy(RefreshPolicy.NONE);
		try {
			elasticsearchClient.bulk(request,
				RequestOptions.DEFAULT);
		}
		catch (Throwable ex) {
			log.log(Level.SEVERE, "Error while storing "+count+" rows for file "+fileId+" for index '"+index_name+"' for template '"+template.getName()+"' "+template.getVersion(), ex);
		}

	}
}
