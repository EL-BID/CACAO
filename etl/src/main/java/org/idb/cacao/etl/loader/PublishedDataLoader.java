/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.etl.loader;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.idb.cacao.api.ETLContext;
import org.idb.cacao.api.PublishedDataFieldNames;
import org.idb.cacao.api.errors.CommonErrors;


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
	
	private String timeout;
	
	public PublishedDataLoader(RestHighLevelClient elasticsearchClient) {
		this.elasticsearchClient = elasticsearchClient;
	}

	public String getTimeout() {
		return timeout;
	}

	public void setTimeout(String timeout) {
		this.timeout = timeout;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.ETLContext.LoadDataStrategy#delete(java.lang.String, java.lang.String, java.lang.Integer)
	 */
	@Override
	public void delete(String indexName, String taxPayerId, Integer taxPeriodNumber) throws Exception {
		BoolQueryBuilder query = QueryBuilders.boolQuery();
		if (taxPayerId!=null)
			query.must(new TermQueryBuilder(PublishedDataFieldNames.TAXPAYER_ID.toString()+".keyword", taxPayerId));
		if (taxPeriodNumber!=null)
			query.must(new TermQueryBuilder(PublishedDataFieldNames.TAXPERIOD_NUMBER.toString(), taxPeriodNumber));
		DeleteByQueryRequest request = new DeleteByQueryRequest(indexName)
				.setQuery(query);
		CommonErrors.doESWriteOpWithRetries(()->{
			try {
				elasticsearchClient.deleteByQuery(request, RequestOptions.DEFAULT);
			}
			catch (Exception ex) {
				if (CommonErrors.isErrorNoIndexFound(ex) || CommonErrors.isErrorNoMappingFoundForColumn(ex))
					return; // ignore these errors
				else
					throw ex;
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.ETLContext.LoadDataStrategy#start()
	 */
	@Override
	public void start() {
		request = new BulkRequest();
		if (timeout!=null)
			request.timeout(timeout);
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
		if (request.numberOfActions()>0) {
			request.setRefreshPolicy(RefreshPolicy.NONE);
			CommonErrors.doESWriteOpWithRetries(
				()->elasticsearchClient.bulk(request,
				RequestOptions.DEFAULT));
			request.requests().clear();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.ETLContext.LoadDataStrategy#close()
	 */
	@Override
	public void close() {
	}

	
}
