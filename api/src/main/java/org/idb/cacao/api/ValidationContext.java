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

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.idb.cacao.api.templates.DocumentTemplate;
import org.springframework.context.MessageSource;

/**
 * This objects wraps up information collected during the VALIDATION phase, before
 * the incoming file is admitted for ETL.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class ValidationContext {

    /**
     * Date format that conforms to ISO 8601
     */
    public static final ThreadLocal<SimpleDateFormat> ISO_8601_DATE = new ThreadLocal<SimpleDateFormat>() {

		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd");
		}
    	
    };
	
    /**
     * Timestamp format that conforms to ISO 8601
     */
    public static final ThreadLocal<SimpleDateFormat> ISO_8601_TIMESTAMP = new ThreadLocal<SimpleDateFormat>() {

		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		}
    	
    };

	/**
	 * Reference to the incoming file
	 */
	private DocumentUploaded documentUploaded;
	
	/**
	 * The document template related to this incoming file
	 */
	private DocumentTemplate documentTemplate;
	
	/**
	 * Object used to resolve errors according to a specific language
	 */
	private MessageSource messageSource;
	
	/**
	 * Reference to the file contents
	 */
	private Path documentPath;

	/**
	 * Warnings produced by the validation phase. Texts informed in braces should be resolved with messages.properties.<BR>
	 * E.g.: If the alert is "{some.message}", it will be resolved to another message according to messages.properties
	 * and the user preference. Parameters for messages.properties entry may be provided in parentheses. For example,
	 * {some.message(param1)} will be resolved with messages.properties using 'some.message' as key and 'param1' as
	 * the first parameter to this message. Additional parameters may be separated by commas.  If the parameter is in braces,
	 * it will also be resolved with messages.properties. For example, "{some.message({some.parameter})}" will first
	 * resolve 'some.parameter' as key to messages.properties, than will use it as a parameter using 'some.message' as key. 
	 */
	private List<String> alerts;

	/**
	 * Raw contents of parsed document.<BR>
	 * Each element corresponds to one record of data. Each record of data is represented as a Map of fields.<BR>
	 * Each key corresponds to a particular DocumentField name.<BR>
	 * Each value may be:
	 * - a primitive type (e.g. String, Date, etc.)<BR>
	 * - may be a list of primitive types (if there are multiple occurrences of the same field at the same file)<BR>
	 * - may be another Map (if the Document Field is of NESTED type).<BR>
	 * - may be a list of Maps (if there are multiple occurrences of the same NESTED field at the same file)<BR>
	 */
	private List<Map<String,Object>> parsedContents;

	/**
	 * Object used to resolve errors according to a specific language
	 */
	public MessageSource getMessageSource() {
		return messageSource;
	}

	/**
	 * Object used to resolve errors according to a specific language
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/**
	 * Reference to the incoming file
	 */
	public DocumentUploaded getDocumentUploaded() {
		return documentUploaded;
	}

	/**
	 * Reference to the incoming file
	 */
	public void setDocumentUploaded(DocumentUploaded documentUploaded) {
		this.documentUploaded = documentUploaded;
	}

	/**
	 * The document template related to this incoming file
	 */
	public DocumentTemplate getDocumentTemplate() {
		return documentTemplate;
	}

	/**
	 * The document template related to this incoming file
	 */
	public void setDocumentTemplate(DocumentTemplate documentTemplate) {
		this.documentTemplate = documentTemplate;
	}

	/**
	 * Reference to the file contents
	 */
	public Path getDocumentPath() {
		return documentPath;
	}

	/**
	 * Reference to the file contents
	 */
	public void setDocumentPath(Path documentPath) {
		this.documentPath = documentPath;
	}

	/**
	 * Warnings produced by the validation phase. Texts informed in braces should be resolved with messages.properties.<BR>
	 * E.g.: If the alert is "{some.message}", it will be resolved to another message according to messages.properties
	 * and the user preference. Parameters for messages.properties entry may be provided in parentheses. For example,
	 * {some.message(param1)} will be resolved with messages.properties using 'some.message' as key and 'param1' as
	 * the first parameter to this message. Additional parameters may be separated by commas.  If the parameter is in braces,
	 * it will also be resolved with messages.properties. For example, "{some.message({some.parameter})}" will first
	 * resolve 'some.parameter' as key to messages.properties, than will use it as a parameter using 'some.message' as key. 
	 */
	public List<String> getAlerts() {
		return alerts;
	}

	/**
	 * Warnings produced by the validation phase. Texts informed in braces should be resolved with messages.properties.<BR>
	 * E.g.: If the alert is "{some.message}", it will be resolved to another message according to messages.properties
	 * and the user preference. Parameters for messages.properties entry may be provided in parentheses. For example,
	 * {some.message(param1)} will be resolved with messages.properties using 'some.message' as key and 'param1' as
	 * the first parameter to this message. Additional parameters may be separated by commas.  If the parameter is in braces,
	 * it will also be resolved with messages.properties. For example, "{some.message({some.parameter})}" will first
	 * resolve 'some.parameter' as key to messages.properties, than will use it as a parameter using 'some.message' as key. 
	 */
	public void setAlerts(List<String> alerts) {
		this.alerts = alerts;
	}
	
	/**
	 * Warnings produced by the validation phase. Texts informed in braces should be resolved with messages.properties.<BR>
	 * E.g.: If the alert is "{some.message}", it will be resolved to another message according to messages.properties
	 * and the user preference. Parameters for messages.properties entry may be provided in parentheses. For example,
	 * {some.message(param1)} will be resolved with messages.properties using 'some.message' as key and 'param1' as
	 * the first parameter to this message. Additional parameters may be separated by commas.  If the parameter is in braces,
	 * it will also be resolved with messages.properties. For example, "{some.message({some.parameter})}" will first
	 * resolve 'some.parameter' as key to messages.properties, than will use it as a parameter using 'some.message' as key. 
	 */
	public void addAlert(String alert) {
		if (alert==null || alert.trim().length()==0)
			return;
		if (this.alerts==null)
			this.alerts = new LinkedList<>();
		this.alerts.add(alert);
	}

	/**
	 * Raw contents of parsed document.<BR>
	 * Each element corresponds to one record of data. Each record of data is represented as a Map of fields.<BR>
	 * Each key corresponds to a particular DocumentField name.<BR>
	 * Each value may be:
	 * - a primitive type (e.g. String, Date, etc.)<BR>
	 * - may be a list of primitive types (if there are multiple occurrences of the same field at the same file)<BR>
	 * - may be another Map (if the Document Field is of NESTED type).<BR>
	 * - may be a list of Maps (if there are multiple occurrences of the same NESTED field at the same file)<BR>
	 */
	public List<Map<String,Object>> getParsedContents() {
		return parsedContents;
	}

	/**
	 * Raw contents of parsed document.<BR>
	 * Each element corresponds to one record of data. Each record of data is represented as a Map of fields.<BR>
	 * Each key corresponds to a particular DocumentField name.<BR>
	 * Each value may be:
	 * - a primitive type (e.g. String, Date, etc.)<BR>
	 * - may be a list of primitive types (if there are multiple occurrences of the same field at the same file)<BR>
	 * - may be another Map (if the Document Field is of NESTED type).<BR>
	 * - may be a list of Maps (if there are multiple occurrences of the same NESTED field at the same file)<BR>
	 */
	public void setParsedContents(List<Map<String,Object>> parsedContents) {
		this.parsedContents = parsedContents;
	}
	
	/**
	 * Returns TRUE if there is no parsed contents yet
	 */
	public boolean isEmpty() {
		return parsedContents==null || parsedContents.isEmpty();
	}
	
	/**
	 * Returns the number of records of parsed contents
	 */
	public int size() {
		return (parsedContents==null) ? 0 : parsedContents.size();
	}
	
	/**
	 * Returns the parsed content related to a given field name.
	 * @param nestedFieldNames Optional parameter with other field names to look in nested structure.
	 */
	public <T> T getParsedContent(int index, String fieldName, String... nestedFieldNames) {
		if (parsedContents==null)
			return null;
		if (parsedContents.size()>=index)
			return null;
		Map<String,Object> record = parsedContents.get(index);
		return getParsedContent(record, fieldName, nestedFieldNames);
	}
	
	/**
	 * Returns the parsed content related to a given field name.
	 * @param nestedFieldNames Optional parameter with other field names to look in nested structure.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getParsedContent(Map<String,Object> record, String fieldName, String... nestedFieldNames) {
		if (record==null)
			return null;
		Object content = record.get(fieldName);
		if (content==null)
			return null;
		if (nestedFieldNames!=null && nestedFieldNames.length>0) {
			for (String nestedFieldName: nestedFieldNames) {
				if (!(content instanceof Map))
					return null;
				content = ((Map<?,?>)content).get(nestedFieldName);
			}
		}
		return (T)content;
	}
	
	/**
	 * Same as {@link #getParsedContent(Map, String, String...) getParsedContent} but also do some casting and conversions
	 * in order to return a java.util.Date object.
	 */
	public static Date getParsedDateContent(Map<String,Object> record, String fieldName, String... nestedFieldNames) {
		Object content = getParsedContent(record, fieldName, nestedFieldNames);
		if (content==null)
			return null;
		if (content instanceof Date)
			return (Date)content;
		if (content instanceof OffsetDateTime)			
			return new Date(((OffsetDateTime)content).toInstant().toEpochMilli());
		if (content instanceof LocalDate)			
			return new Date(((LocalDate)content).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
		else
			return (Date)content; // this will throw ClassCastException for incompatible object
	}

	/**
	 * Try to parse a required field according to a specific type. In case of any failure, includes into 'context'
	 * the corresponding standard warning and returns NULL.
	 * @param context Validation context. Receives warnings.
	 * @param type Field type
	 * @param record Record fields and values
	 * @param fieldName Field name to find
	 * @param nestedFieldNames Optional parameter with other field names to look in nested structure.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getParsedRequiredContent(
			ValidationContext context,
			Class<T> type,
			Map<String,Object> record, 
			String fieldName, 
			String... nestedFieldNames) {
		
		if (Date.class==type) {
			Date date;
			try {
				date = getParsedDateContent(record, fieldName, nestedFieldNames);
			}
			catch (Throwable ex) {
				context.addAlert("{error.invalidField("+fieldName+")}");
				return null;				
			}
			if (date==null) {
				context.addAlert("{error.missingField("+fieldName+")}");
				return null;
			}	
			return (T)date;
		}
		
		T anyvalue;
		try {
			anyvalue = getParsedContent(record, fieldName, nestedFieldNames);
		}
		catch (Throwable ex) {
			context.addAlert("{error.invalidField("+fieldName+")}");
			return null;				
		}
		if (anyvalue==null) {
			context.addAlert("{error.missingField("+fieldName+")}");
			return null;
		}			
		return anyvalue;

	}
}
