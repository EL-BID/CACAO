/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.dto;

import java.util.List;

import org.idb.cacao.api.Views;

import com.fasterxml.jackson.annotation.JsonView;

/**
 * Pagination data to return to Tabulator
 * @author Luis Kauer
 *
 */
public class PaginationData<E> {
	// Total number of pages
	@JsonView(Views.Public.class)
	private int last_page;
	
	// List with data objects
	@JsonView(Views.Public.class)
	private List<E> data;

	public PaginationData(int lastPage, List<E> data) {
		this.last_page = lastPage;;
		this.data = data;
	}
	
	public int getLast_page() {
		return last_page;
	}

	public void setLast_page(int lastPage) {
		this.last_page = lastPage;
	}

	public List<E> getData() {
		return data;
	}

	public void setData(List<E> data) {
		this.data = data;
	}
	
	
}
