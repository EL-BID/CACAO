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
package org.idb.cacao.web.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CloseIndexRequest;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;

/**
 * Utility methods for Elastic Search
 * 
 * @author Gustavo Figueiredo
 *
 */
public class ESUtils {
	
	/**
	 * The ignore_malformed parameter, if set to true, allows the exception to be ignored. The malformed field is not indexed, but other fields in the document are processed normally.
	 * @see https://www.elastic.co/guide/en/elasticsearch/reference/current/ignore-malformed.html
	 */
	public static final String SETTING_IGNORE_MALFORMED = "index.mapping.ignore_malformed";
	
	/**
	 * Change index-scoped boolean setting
	 * @param elasticsearchClient Object for RESTful communication with ElasticSearch
	 * @param index_name Index name
	 * @param setting_name Name of the setting to change
	 * @param setting_value Boolean value of the setting
	 * @param closeAndReopenIndex Tells if the index should be closed before changing settings and reopened afterwards
	 */
	public static void changeBooleanIndexSetting(RestHighLevelClient elasticsearchClient, String index_name, String setting_name, boolean setting_value, boolean closeAndReopenIndex) throws IOException {
		changeIndexSettings(elasticsearchClient,
				index_name,
				Settings.builder()
					.put(setting_name, setting_value)
					.build(),
				closeAndReopenIndex);
	}	
	
	/**
	 * Change index-scoped settings
	 * @param elasticsearchClient Object for RESTful communication with ElasticSearch
	 * @param index_name Index name
	 * @param settings Settings to provide
	 * @param closeAndReopenIndex Tells if the index should be closed before changing settings and reopened afterwards
	 */
	public static void changeIndexSettings(RestHighLevelClient elasticsearchClient, String index_name, Settings settings, boolean closeAndReopenIndex) throws IOException {
		UpdateSettingsRequest request = new UpdateSettingsRequest(index_name);
		request.settings(settings);
		
		if (closeAndReopenIndex)
			elasticsearchClient.indices().close(new CloseIndexRequest(index_name), RequestOptions.DEFAULT);
		
		try {
			
			elasticsearchClient.indices().putSettings(request, RequestOptions.DEFAULT);
			
		}
		finally {
			
			if (closeAndReopenIndex)
				elasticsearchClient.indices().open(new OpenIndexRequest(index_name), RequestOptions.DEFAULT);
			
		}

	}	

	/**
	 * Get ElasticSearch cluster health information
	 */
	public static ClusterHealthResponse getClusterStatus(RestHighLevelClient elasticsearchClient) throws IOException {
		ClusterHealthRequest request = new ClusterHealthRequest();
		request.timeout(TimeValue.timeValueSeconds(30));
		return elasticsearchClient.cluster().health(request, RequestOptions.DEFAULT);
	}

	/**
	 * Returns summaries about all the indices in ElasticSearch
	 */
	public static List<IndexSummary> catIndices(RestHighLevelClient elasticsearchClient) throws IOException {
		Request request = new Request("GET","/_cat/indices?v&s=index&h=health,status,index,pri,rep,docs.count,docs.deleted,store.size,pri.store.size");
		Response response = elasticsearchClient.getLowLevelClient().performRequest(request);
		List<IndexSummary> summary = new LinkedList<>();
		try (InputStream is = response.getEntity().getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(is,StandardCharsets.UTF_8))) {
			while (reader.ready()) {
				String line = reader.readLine();
				if (line==null || line.length()==0)
					continue;
				if (line.startsWith("health"))
					continue;
				String[] parts = line.split("\\s+");
				if (parts.length<9)
					continue;
				IndexSummary s = new IndexSummary();
				s.setHealth(parts[0]);
				s.setStatus(parts[1]);
				s.setIndex(parts[2]);
				s.setPri(Integer.parseInt(parts[3]));
				s.setRep(Integer.parseInt(parts[4]));
				s.setDocsCount(Long.parseLong(parts[5]));
				s.setDocsDeleted(Long.parseLong(parts[6]));
				s.setStoreSize(parts[7]);
				s.setPriStoreSize(parts[8]);
				summary.add(s);
			}
		}
		return summary;
	}

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
    	catch (Throwable ex) {
    		if (ErrorUtils.isErrorNoMappingFoundForColumn(ex)) {
    			if (!ESUtils.hasMappings(elasticsearchClient, indexName))
    				return null;
    			else
    				throw ex;
    		}
    		else if (ErrorUtils.isErrorNoIndexFound(ex)) {
    			return null;
    		}
    		else {
    			throw ex;
    		}
    	}
    	return sresp;
	}

	/**
	 * Invoke a index request. In case of an error relative to type mismatch, try to fix it by changing an index parameter (index.mapping.ignore_malformed).<BR>
	 * Warning: this configuration needs to be done with the index closed. So it's possible that this operation disables the index temporarily. 
	 */
	public static void indexWithRetry(RestHighLevelClient client, IndexRequest indexRequest) throws IOException {
		try {
			client.index(indexRequest, RequestOptions.DEFAULT);
		}
		catch (Throwable ex) {
			if (null!=ErrorUtils.getIllegalArgumentTypeMismatch(ex)
					|| null!=ErrorUtils.getIllegalArgumentInputString(ex)) {
				// In case of an error relative to type mismatch, lets try again after chaging some of the index parameters
				changeBooleanIndexSetting(client, indexRequest.index(), SETTING_IGNORE_MALFORMED, true, /*closeAndReopenIndex*/true);
				client.index(indexRequest, RequestOptions.DEFAULT);
			}
			else {
				throw ex;
			}
		}
	}	
	
	/**
	 * Creates a new index
	 * @param elasticsearchClient Object for RESTful communication with ElasticSearch
	 * @param index_name Index name
	 * @param settings Settings for the new index
	 */
	public static void createIndex(RestHighLevelClient elasticsearchClient, String index_name, Settings settings) throws IOException {
		CreateIndexRequest request = new CreateIndexRequest(index_name);
		request.settings(settings);
		elasticsearchClient.indices().create(request, RequestOptions.DEFAULT);
	}

	/**
	 * Creates a new index initialized with some settings
	 * @param elasticsearchClient Object for RESTful communication with ElasticSearch
	 * @param index_name Index name
	 * @param ignore_malformed If set to true, allows the 'malformed' exception to be ignored.
	 */
	public static void createIndex(RestHighLevelClient elasticsearchClient, String index_name, boolean ignore_malformed) throws IOException {
		Settings settings = Settings.builder()
			.put(SETTING_IGNORE_MALFORMED, ignore_malformed)
			.build();
		createIndex(elasticsearchClient, index_name, settings);
	}
	
	public static class IndexSummary {
		private String health;
		private String status;
		private String index;
		private int pri;
		private int rep;
		private long docsCount;
		private long docsDeleted;
		private String storeSize;
		private String priStoreSize;
		public String getHealth() {
			return health;
		}
		public void setHealth(String health) {
			this.health = health;
		}
		public String getStatus() {
			return status;
		}
		public void setStatus(String status) {
			this.status = status;
		}
		public String getIndex() {
			return index;
		}
		public void setIndex(String index) {
			this.index = index;
		}
		public int getPri() {
			return pri;
		}
		public void setPri(int pri) {
			this.pri = pri;
		}
		public int getRep() {
			return rep;
		}
		public void setRep(int rep) {
			this.rep = rep;
		}
		public long getDocsCount() {
			return docsCount;
		}
		public void setDocsCount(long docsCount) {
			this.docsCount = docsCount;
		}
		public long getDocsDeleted() {
			return docsDeleted;
		}
		public void setDocsDeleted(long docsDeleted) {
			this.docsDeleted = docsDeleted;
		}
		public String getStoreSize() {
			return storeSize;
		}
		public void setStoreSize(String storeSize) {
			this.storeSize = storeSize;
		}
		public String getPriStoreSize() {
			return priStoreSize;
		}
		public void setPriStoreSize(String priStoreSize) {
			this.priStoreSize = priStoreSize;
		}		
	}
}
