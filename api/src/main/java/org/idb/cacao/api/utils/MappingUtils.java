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
package org.idb.cacao.api.utils;

import java.io.IOException;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.idb.cacao.api.errors.CommonErrors;

/**
 * Utility methods for returning information about elastic search 'mappings'.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class MappingUtils {

	/**
	 * Returns current mappings for a given index
	 */
	public static MappingMetadata getMapping(RestHighLevelClient client, String indexName) throws IOException {
		GetMappingsRequest request = new GetMappingsRequest(); 
		request.indices(indexName);
		return client.indices().getMapping(request, RequestOptions.DEFAULT).mappings().get(indexName);
	}

	/**
	 * Returns TRUE if there are any mappings for the index
	 */
	public static boolean hasMappings(RestHighLevelClient client, String indexName) throws IOException {
		MappingMetadata mappings = getMapping(client, indexName);
		return !mappings.sourceAsMap().isEmpty();
	}

	/**
	 * Do a search in ElasticSearch but avoid propagating errors related to lack of indexed objects
	 * @return Returns the response of the search, or returns NULL if encountered an error due to lack of mapping or index.
	 */
	public static SearchResponse searchIgnoringNoMapError(final RestHighLevelClient elasticsearchClient, final SearchRequest searchRequest, final String indexName) throws IOException {
		SearchResponse sresp = null;
		try {
			sresp = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
		}
		catch (Exception ex) {
			if (CommonErrors.isErrorNoMappingFoundForColumn(ex)) {
				if (!hasMappings(elasticsearchClient, indexName))
					return null;
				else
					throw ex;
			}
			else if (CommonErrors.isErrorNoIndexFound(ex)) {
				return null;
			}
			else {
				throw ex;
			}
		}
		return sresp;
	}

}
