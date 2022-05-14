/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.dto;

import java.util.List;

/**
 * Data Transfer Object for the result of a search on for use with 
 * Fomantic UI
 * @author Luis Kauer
 *
 */
public class SearchResult<T> {
	
	/**
	 * The search was successfull
	 */
	private boolean success=true;
	
	private List<T> results;
	
	public SearchResult(List<T> results) {
		this.results = results;
	}
	
	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public List<T> getResults() {
		return results;
	}

	public void setResults(List<T> results) {
		this.results = results;
	}
}
