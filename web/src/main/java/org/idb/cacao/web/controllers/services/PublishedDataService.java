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
package org.idb.cacao.web.controllers.services;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.idb.cacao.api.utils.IndexNamesUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service regarding the published (denormalized) data that was produced by ETL
 * 
 * @author Gustavo Figueiredo
 *
 */
@Service
public class PublishedDataService {

	@Autowired
	private RestHighLevelClient elasticsearchClient;

	/**
	 * Returns the list of all indices alread created at ElasticSearch related to published (denormalized) data
	 * produced by ETL
	 */
	public List<String> getIndicesForPublishedData() throws IOException {
		GetIndexRequest request = new GetIndexRequest(IndexNamesUtils.PUBLISHED_DATA_INDEX_PREFIX+"*");
		GetIndexResponse response = elasticsearchClient.indices().get(request, RequestOptions.DEFAULT);
		String[] indices = response.getIndices();
		return Arrays.asList(indices);
	}
}
