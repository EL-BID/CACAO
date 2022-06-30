/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.api.utils;

import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.search.SearchResponse;

/**
 * Generic Utilities
 * 
 * @author Rivelino Patrício
 * 
 * @since 06/03/2022
 *
 */
public class Utils {	

	/**
	 * Given a {@link SearchResponse} identify and return total hits
	 *  
	 * @param sresp	A SearchResponse to 
	 * @return
	 */
	public static long getTotalHits(SearchResponse sresp) {
		if ( sresp == null || sresp.getHits() == null )
			return 0;
		TotalHits hits = sresp.getHits().getTotalHits();		
		if (hits != null )
			return hits.value;
		return 0;
	}

}
