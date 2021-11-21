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
package org.idb.cacao.etl.loader;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.idb.cacao.api.ETLContext;

/**
 * Implementation of a 'data loading strategy' used by the ETL process in order to store
 * the published (denormalized) data.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class PublishedDataLoader implements ETLContext.LoadDataStrategy {

    private BulkRequest request;

	private RestHighLevelClient elasticsearchClient;
	
	public PublishedDataLoader(RestHighLevelClient elasticsearchClient) {
		this.elasticsearchClient = elasticsearchClient;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.ETLContext.LoadDataStrategy#start()
	 */
	@Override
	public void start() {
		request = new BulkRequest();
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.ETLContext.LoadDataStrategy#add(org.elasticsearch.action.index.IndexRequest)
	 */
	@Override
	public void add(IndexRequest toIndex) {
		request.add(toIndex);
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.ETLContext.LoadDataStrategy#commit()
	 */
	@Override
	public void commit() throws Exception {
		request.setRefreshPolicy(RefreshPolicy.NONE);
		elasticsearchClient.bulk(request,
				RequestOptions.DEFAULT);
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.ETLContext.LoadDataStrategy#close()
	 */
	@Override
	public void close() {
	}

	
}