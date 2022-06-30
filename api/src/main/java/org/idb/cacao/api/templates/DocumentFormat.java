/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.api.templates;

/**
 * Enumeration of document formats
 * @author Gustavo Figueiredo
 *
 */
public enum DocumentFormat {

	XLS("Excel (XLS/XLSX)"),
	DOC("Word (DOC/DOCX)"),
	JSON("JSON"),
	XML("XML"),
	PDF("PDF"),
	CSV("TXT/CSV");

	private final String display;
	
	DocumentFormat(String display) {
		this.display = display;
	}

	@Override
	public String toString() {
		return display;
	}

}
