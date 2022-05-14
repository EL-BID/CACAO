/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.api;

/**
 * Enumerates constants for published (denormalized) data fields names. These fields are stored
 * in pos-validated data (output of 'ETL' phase).<BR>
 * 
 * @author Gustavo Figueiredo
 *
 */
public enum PublishedDataFieldNames {
	
	/**
	 * Date/time to be used by default at dashboards with published data
	 */
	TIMESTAMP("timestamp"),

	/**
	 * Date/time the ETL phase started
	 */
	ETL_TIMESTAMP("doc_timestamp"),

	/**
	 * Line number of validated data
	 */
	LINE("line"),

	/**
	 * Line number of validated data saved as timestamp, in order to allow sorting values by
	 * this criteria (e.g. in Dashboard one may want to get the 'last value' of any other particular
	 * field, 'sorted by' the line position in the file. Kibana requires a 'timestamp' for establishing
	 * the sort criteria, so this is way we duplicate the line number as 'timestamps'
	 */
	LINE_SORT("line_sort"),

	/**
	 * TaxPayer ID. Must match the same information stored
	 * in {@link DocumentUploaded#getTaxPayerId() taxPayerId}.
	 */
	TAXPAYER_ID("taxpayer_id"),
	
	/**
	 * Tax period number. Must match the same information stored
	 * in {@link DocumentUploaded#getTaxPeriodNumber() taxPeriodNumber}.
	 */
	TAXPERIOD_NUMBER("taxperiod_number"),
	
	/**
	 * Template name. Must match the same information stored
	 * in {@link DocumentUploaded#getTemplateName() templateName}.
	 */
	TEMPLATE_NAME("template_name"),

	/**
	 * Template version. Must match the same information stored
	 * in {@link DocumentUploaded#getTemplateVersion() templateVersion}.
	 */
	TEMPLATE_VERSION("template_version");

	/**
	 * Internal field name to be stored in ElasticSearch
	 */
	private final String fieldName;
	
	PublishedDataFieldNames(String fieldName) {
		this.fieldName = fieldName;
	}

	/**
	 * Internal field name to be stored in ElasticSearch
	 */
	public String getFieldName() {
		return fieldName;
	}

}
