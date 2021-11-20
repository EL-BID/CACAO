/*******************************************************************************
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in the Software without 
 * restriction, including without limitation the rights to use, copy, modify, merge, publish, 
 * distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or 
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS 
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR 
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN 
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.api;

/**
 * Enumerates constants for published (denormalized) data fields names. These fields are stored
 * in pos-validated data (output of 'ETL' phase).
 * 
 * @author Gustavo Figueiredo
 *
 */
public enum PublishedDataFieldNames {
	
	/**
	 * Date/time the ETL phase started
	 */
	TIMESTAMP("_timestamp"),

	/**
	 * TaxPayer ID. Must match the same information stored
	 * in {@link DocumentUploaded#getTaxPayerId() taxPayerId}.
	 */
	TAXPAYER_ID("_taxpayer_id"),
	
	/**
	 * Tax period number. Must match the same information stored
	 * in {@link DocumentUploaded#getTaxPeriodNumber() taxPeriodNumber}.
	 */
	TAXPERIOD_NUMBER("_tax_period_number"),
	
	/**
	 * Template name. Must match the same information stored
	 * in {@link DocumentUploaded#getTemplateName() templateName}.
	 */
	TEMPLATE_NAME("_template_name"),

	/**
	 * Template version. Must match the same information stored
	 * in {@link DocumentUploaded#getTemplateVersion() templateVersion}.
	 */
	TEMPLATE_VERSION("_template_version");

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
