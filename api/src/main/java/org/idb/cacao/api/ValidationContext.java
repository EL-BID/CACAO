/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.api;

import static org.idb.cacao.api.utils.ParserUtils.isDMY;
import static org.idb.cacao.api.utils.ParserUtils.isMDY;
import static org.idb.cacao.api.utils.ParserUtils.isYMD;
import static org.idb.cacao.api.utils.ParserUtils.parseDMY;
import static org.idb.cacao.api.utils.ParserUtils.parseMDY;
import static org.idb.cacao.api.utils.ParserUtils.parseTimestamp;
import static org.idb.cacao.api.utils.ParserUtils.parseTimestampES;
import static org.idb.cacao.api.utils.ParserUtils.parseTimestampWithMS;
import static org.idb.cacao.api.utils.ParserUtils.parseYMD;

import java.nio.file.Path;
import java.text.Normalizer;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.idb.cacao.api.templates.DocumentInput;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.utils.ParserUtils;
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
	 * Reference to the incoming file
	 */
	private DocumentUploaded documentUploaded;
	
	/**
	 * The document template related to this incoming file
	 */
	private DocumentTemplate documentTemplate;
	
	/**
	 * The document input used for this validation
	 */
	private DocumentInput documentInput;
	
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
	 * Same as 'alerts', but do not prevent the file from being processed
	 */
	private List<String> nonCriticalAlerts;

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
	 * A {@link Supplier} function to provide implementation for a {@link List} that will keep all parsed data
	 */
	private Supplier<List<Map<String,Object>>> parsedContentsListFactory = LinkedList::new;	
	
	public ValidationContext() {
		this.alerts = new LinkedList<>();
		this.nonCriticalAlerts = new LinkedList<>();
	}

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
	 * The document input used for this validation
	 */
	public DocumentInput getDocumentInput() {
		return documentInput;
	}

	/**
	 * The document input used for this validation
	 */
	public void setDocumentInput(DocumentInput documentInput) {
		this.documentInput = documentInput;
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
	 * Same as 'alerts', but do not prevent the file from being processed
	 */
	public List<String> getNonCriticalAlerts() {
		return nonCriticalAlerts;
	}
	
	public boolean hasAlerts() {
		return alerts!=null && !alerts.isEmpty();
	}

	/**
	 * Same as 'alerts', but do not prevent the file from being processed
	 */
	public boolean hasNonCriticalAlerts() {
		return nonCriticalAlerts!=null && !nonCriticalAlerts.isEmpty();
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
	 * Same as 'alerts', but do not prevent the file from being processed
	 */
	public void setNonCriticalAlerts(List<String> nonCriticalAlerts) {
		this.nonCriticalAlerts = nonCriticalAlerts;
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
		synchronized (this.alerts) {
			this.alerts.add(alert);
		}
	}

	/**
	 * Same as 'alerts', but do not prevent the file from being processed
	 */
	public void addNonCriticalAlert(String alert) {
		if (alert==null || alert.trim().length()==0)
			return;
		if (this.nonCriticalAlerts==null)
			this.nonCriticalAlerts = new LinkedList<>();
		synchronized (this.nonCriticalAlerts) {
			this.nonCriticalAlerts.add(alert);
		}
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
	
	public Supplier<List<Map<String, Object>>> getParsedContentsListFactory() {
		return parsedContentsListFactory;
	}

	public void setParsedContentsListFactory(Supplier<List<Map<String, Object>>> parsedContentsListFactory) {
		this.parsedContentsListFactory = parsedContentsListFactory;
	}

	public void addParsedContent(Map<String,Object> dataItem) {
		if (dataItem==null)
			return;
		if (this.parsedContents==null)
			this.parsedContents = parsedContentsListFactory.get();
		this.parsedContents.add(dataItem);
	}
	
	/**
	 * Fills in all the records with the provided field fixed contents. If there is no contents,
	 * creates a new record with only the provided information.
	 */
	public void setFieldInParsedContents(String fieldName, Object fixedContents) {
		if (this.parsedContents==null)
			this.parsedContents = parsedContentsListFactory.get();
		if (this.parsedContents.isEmpty()) {
			this.parsedContents.add(new HashMap<>());
		}
		for (Map<String, Object> dataItem: this.parsedContents) {
			dataItem.put(fieldName, fixedContents);
		}
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
	 * Removes the parsed contents
	 */
	public void clearParsedContents() {
		if (parsedContents!=null)
			parsedContents.clear();
	}
	
	/**
	 * Returns the parsed content related to a given field name.
	 * @param nestedFieldNames Optional parameter with other field names to look in nested structure.
	 */
	public <T> T getParsedContent(int index, String fieldName, String... nestedFieldNames) {
		if (parsedContents==null)
			return null;
		if (parsedContents.size()<=index)
			return null;
		Map<String,Object> dataItem = parsedContents.get(index);
		return getParsedContent(dataItem, fieldName, nestedFieldNames);
	}
	
	/**
	 * Returns the parsed content related to a given field name.
	 * @param nestedFieldNames Optional parameter with other field names to look in nested structure.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getParsedContent(Map<String,Object> dataItem, String fieldName, String... nestedFieldNames) {
		if (dataItem==null)
			return null;
		Object content = dataItem.get(fieldName);
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
	public static Date getParsedDateContent(Map<String,Object> dataItem, String fieldName, String... nestedFieldNames) {
		Object content = getParsedContent(dataItem, fieldName, nestedFieldNames);
		return toDate(content);
	}

	/**
	 * Try to parse a required field according to a specific type. In case of any failure, includes into 'context'
	 * the corresponding standard warning and returns NULL.
	 * @param context Validation context. Receives warnings.
	 * @param type Field type
	 * @param dataItem Record fields and values
	 * @param fieldName Field name to find
	 * @param nestedFieldNames Optional parameter with other field names to look in nested structure.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getParsedRequiredContent(
			ValidationContext context,
			Class<T> type,
			Map<String,Object> dataItem, 
			String fieldName, 
			String... nestedFieldNames) {
		
		if (Date.class==type) {
			Date date;
			try {
				date = getParsedDateContent(dataItem, fieldName, nestedFieldNames);
			}
			catch (RuntimeException e) {
				context.addAlert("{error.invalidField("+fieldName+")}");
				return null;				
			}
			if (date==null) {
				if ( !(context.getDocumentInput() != null && 
						context.getDocumentInput().getAcceptIncompleteFiles() != null &&
						context.getDocumentInput().getAcceptIncompleteFiles() ))
					context.addAlert("{error.missingField("+fieldName+")}");
				return null;
			}	
			return (T)date;
		}
		
		Object anyvalue;
		try {
			anyvalue = getParsedContent(dataItem, fieldName, nestedFieldNames);
		}
		catch (RuntimeException e) {
			context.addAlert("{error.invalidField("+fieldName+")}");
			return null;				
		}
		if (anyvalue==null) {
			if ( !(context.getDocumentInput() != null && 
					context.getDocumentInput().getAcceptIncompleteFiles() != null &&
					context.getDocumentInput().getAcceptIncompleteFiles() ))
				context.addAlert("{error.missingField("+fieldName+")}");
			return null;
		}
		if (!type.isInstance(anyvalue)) {
			if (String.class.equals(type)) {
				anyvalue = toString(anyvalue);
			}
			if (Number.class.equals(type)) {
				anyvalue = toNumber(anyvalue);
			}
		}
		return (T)anyvalue;

	}
	
	/**
	 * Convert a value of any type to Date in a conventional way
	 */
	public static Date toDate(Object content) {
		if (content==null)
			return null;
		if (content instanceof Date)
			return (Date)content;
		if (content instanceof OffsetDateTime)			
			return new Date(((OffsetDateTime)content).toInstant().toEpochMilli());
		if (content instanceof LocalDate)			
			return new Date(((LocalDate)content).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
		if (content instanceof String) {
			String value = (String) content;
			if (isMDY(value))
				return parseMDY(value);
			if (isDMY(value))
				return parseDMY(value);
			if (isYMD(value))
				return parseYMD(value);
			Date d = parseTimestampWithMS(value);
			if (d!=null)
				return d;
			d = parseTimestamp(value);
			if (d!=null)
				return d;
			d = parseTimestampES(value);
			if (d!=null)
				return d;
		}
		return (Date)content; // this will throw ClassCastException for incompatible object
	}
	
	/**
	 * Convert a value of any type to OffsetDateTime in a conventional way
	 */
	public static OffsetDateTime toOffsetDateTime(Object content) {
		if (content==null)
			return null;
		if (content instanceof OffsetDateTime)
			return (OffsetDateTime)content;
		if (content instanceof java.sql.Date)			
			return ((java.sql.Date)content).toLocalDate().atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
		if (content instanceof Date)			
			return ((Date)content).toInstant().atOffset(ZoneId.systemDefault().getRules().getOffset(Instant.now()));
		if (content instanceof LocalDate)			
			return ((LocalDate)content).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
		if (content instanceof String) {
			String value = (String) content;
			if (isMDY(value))
				return toOffsetDateTime(parseMDY(value));
			if (isDMY(value))
				return toOffsetDateTime(parseDMY(value));
			if (isYMD(value))
				return toOffsetDateTime(parseYMD(value));			
			Date d = parseTimestamp(value);
			if (d!=null)
				return toOffsetDateTime(d);	
			d = parseTimestampES(value);
			if (d!=null)
				return toOffsetDateTime(d);	
		}
		return (OffsetDateTime)content; // this will throw ClassCastException for incompatible object
	}

	/**
	 * Convert a value of any type to String in a conventional way.
	 */
	public static String toString(Object value) {
		if (value==null)
			return null;
		if (value instanceof String) {
			return (String)value;
		}
		else if (value instanceof Double) {
			value = value.toString();
			if (((String)value).endsWith(".0"))
				value = ((String)value).replace(".0", "");
			return (String)value;
		}
		else {
			return value.toString().trim();
		}
	}

    public static Number parseDecimal(String value) throws ParseException {
    	if (value==null || value.trim().length()==0)
    		return null;
    	return ParserUtils.parseDecimal(value);
    }

    public static Number parseDecimalGrouping(String value) throws ParseException {
    	if (value==null || value.trim().length()==0)
    		return null;
    	return ParserUtils.parseDecimalGrouping(value);
    }

    public static Number parseDecimalWithComma(String value) throws ParseException {
    	if (value==null || value.trim().length()==0)
    		return null;
    	return ParserUtils.parseDecimalWithComma(value);
    }

	/**
	 * Convert a value of any type to Number in a conventional way.
	 */
	public static Number toNumber(Object value) {
		if (value==null)
			return null;
		if (value instanceof Number) {
			return (Number)value;
		}
		else if (value instanceof String) {
			String txt = (String)value;
	    	if (txt.isEmpty())
	    		return null;
	    	txt = txt.trim();
	    	int commaPosition = txt.indexOf(",");
	    	int dotPosition = txt.indexOf(".");
	    	try {
		    	if (commaPosition>=0 && dotPosition>=0) {
		    		if (commaPosition<dotPosition) {
		    	    	try {
		    	    		return parseDecimalGrouping(txt);
		    	    	}
		    	    	catch (Exception ex) {
		    	    		return parseDecimalWithComma(txt);
		    	    	}  
		    		}
		    		else {
		    	    	try {
		    	    		return parseDecimalWithComma(txt);
		    	    	}
		    	    	catch (Exception ex) {
		    	    		return parseDecimal(txt);
		    	    	}    			
		    		}
		    	}
		    	else if (commaPosition>=0) {
			    	try {
			    		return parseDecimalWithComma(txt);
			    	}
			    	catch (Exception ex) {
			    		return parseDecimal(txt);
			    	}    			    		
		    	}
		    	else if (dotPosition>=0) {
		    		if (txt.indexOf('.', dotPosition+1)>0) {
		    			// If we have more dots, probably dots are grouping separators
		    	    	try {
		    	    		return parseDecimalWithComma(txt);
		    	    	}
		    	    	catch (Exception ex) {
		    	    		return parseDecimal(txt);
		    	    	}    			    		
		    		}
		    		else if (Pattern.compile("\\d{4}\\.").matcher(txt).find()
		    			|| !Pattern.compile("\\d\\.\\d{3}").matcher(txt).find()) {
		    			// If we have four or more digits before any dot, or if we don't have
		    			// three digits after any dot, probably dots are decimal separators
		    	    	try {
		    	    		return parseDecimal(txt);
		    	    	}
		    	    	catch (Exception ex) {
		    	    		return parseDecimalWithComma(txt);
		    	    	}    			    		    			
		    		}
			    	try {
			    		// For other cases (e.g.: only one dot and three digits after the dot)
			    		// we will assume it's a grouping separator
			    		return parseDecimalWithComma(txt);
			    	}
			    	catch (Exception ex) {
			    		return parseDecimal(txt);
			    	}    			    		
		    	}
		    	else {
		    		return parseDecimalWithComma(txt);
		    	}
	    	}
	    	catch (ParseException|NumberFormatException ex) {
	    		return null;
	    	}
		}
		else {
			return null;
		}
	}

	/**
	 * Tries to match an expression in different ways.
	 * @param options Options with values to compare
	 * @param toText Function to extract a comparable text value out of one of the 'options'
	 * @param expression Expression to test for.
	 */
	public static <R> Optional<R> matchExpression(Iterable<R> options, Function<R,String> toText, String expression) {
		
		if (options==null || toText==null || expression==null || expression.trim().length()==0)
			return Optional.empty();
		
		expression = expression.trim();
		
		// First trial: compares ignoring cases

		for (R option: options) {
			if (option==null)
				continue;
			String text = toText.apply(option);
			if (text==null)
				continue;
			if (expression.equalsIgnoreCase(text.trim()))
				return Optional.of(option);
		}
		
		// Second trial: removes diacritics'

		String normalizedExpression =
				Normalizer.normalize(expression, Normalizer.Form.NFD).replaceAll("[\\p{InCombiningDiacriticalMarks}]", "") // removes all diacritics
				.toUpperCase();
		for (R option: options) {
			if (option==null)
				continue;
			String text = toText.apply(option);
			if (text==null)
				continue;
			String normalizedText =
					Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("[\\p{InCombiningDiacriticalMarks}]", "") // removes all diacritics
					.toUpperCase();
			if (normalizedExpression.equalsIgnoreCase(normalizedText.trim()))
				return Optional.of(option);
		}
		
		// Third trial: consider expression as a 'regular expression'
		Pattern patternExpression;
		try {
			patternExpression = Pattern.compile(normalizedExpression, Pattern.CASE_INSENSITIVE);
		}
		catch (Exception ex) {
			patternExpression = null;
		}
		
		if (patternExpression!=null) {
			for (R option: options) {
				if (option==null)
					continue;
				String text = toText.apply(option);
				if (text==null)
					continue;
				String normalizedText =
						Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("[\\p{InCombiningDiacriticalMarks}]", "") // removes all diacritics
						.toUpperCase();
				if (patternExpression.matcher(normalizedText).find())
					return Optional.of(option);
			}			
		}
		
		return Optional.empty();
	}
}
