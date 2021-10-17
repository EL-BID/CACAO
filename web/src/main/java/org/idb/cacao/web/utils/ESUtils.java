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
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;

/**
 * Utility methods for Elastic Search
 * 
 * @author Gustavo Figueiredo
 *
 */
public class ESUtils {

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