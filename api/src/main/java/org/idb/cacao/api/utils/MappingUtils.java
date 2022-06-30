/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
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
