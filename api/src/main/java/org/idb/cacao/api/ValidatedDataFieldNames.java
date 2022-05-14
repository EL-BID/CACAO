/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.api;

/**
 * Enumerates constants for validated data fields names. These fields are stored
 * in pre-validated data (output of 'validation' phase).
 * 
 * @author Gustavo Figueiredo
 *
 */
public enum ValidatedDataFieldNames {

	/**
	 * Unique identification of the uploaded file. Must match the same information stored
	 * in {@link DocumentUploaded#getFileId() fileId}.
	 */
	FILE_ID("FILE_ID"),
	
	/**
	 * Date/time the validation phase finished
	 */
	TIMESTAMP("TIMESTAMP"),
	
	/**
	 * Line number of validated data
	 */
	LINE("LINE");
	
	/**
	 * Internal field name to be stored in ElasticSearch
	 */
	private final String fieldName;
	
	ValidatedDataFieldNames(String fieldName) {
		this.fieldName = fieldName;
	}

	/**
	 * Internal field name to be stored in ElasticSearch
	 */
	public String getFieldName() {
		return fieldName;
	}
	
}
