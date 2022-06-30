/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
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
