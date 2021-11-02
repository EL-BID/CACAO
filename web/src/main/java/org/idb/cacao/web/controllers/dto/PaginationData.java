package org.idb.cacao.web.controllers.dto;

import java.util.List;

import org.idb.cacao.web.controllers.Views;

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
