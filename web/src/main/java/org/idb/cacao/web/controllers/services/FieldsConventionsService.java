/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.services;

import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.utils.ParserUtils;
import org.idb.cacao.web.controllers.rest.DocumentStoreAPIController;
import org.idb.cacao.web.dto.MenuItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.context.support.StandardServletEnvironment;
import org.thymeleaf.expression.Numbers;

/**
 * Service for performing some transformations over collections of fields for
 * proper representations in model and view.
 *  
 * @author Gustavo Figueiredo
 */
@Service("FieldsConventionsService")
public class FieldsConventionsService {

	/**
	 * Maximum length for presenting a field name
	 */
	public static final int MAX_FIELD_NAME_LENGTH = 250;
	
	/**
	 * Maximum length for presenting a sample value for a template field
	 */
	public static final int MAX_SAMPLE_FIELD_LENGTH = 250;
	
	/**
	 * Pattern for 'maximum tax period' informed in DocumentTemplate. E.g.: matches '01/2020'
	 */
	public static final Pattern TAX_PERIOD_MAX_PATTERN = Pattern.compile("^(\\d+)[\\/\\-](\\d+)$");
	
	@Autowired
	Environment env;

	@Autowired
	MessageSource messageSource;
	
	/**
	 * Locale object used for parsing documents in language specific syntax
	 */
	private Locale docsLocale;
	
	/**
	 * Decimal value treatment regarding 'tax due value' whenever generating payment slips
	 */
	private ValueTreatment taxValueTreatment;
	
	/**
	 * Months full names for parsing document contents. Use system wide default language for parsing documents. 
	 */
	private String[] defaultCalendarMonths;
	
	/**
	 * Months short names for parsing document contents. Use system wide default language for parsing documents.  
	 */
	private String[] defaultCalendarMonthsShort;
	
	/**
	 * Patterns for months full names for parsing document contents. Diacritical marks are removed. Use system wide default language for parsing documents.
	 */
	private Pattern[] defaultCalendarMonthsPatterns;
	
	/**
	 * Patterns form months short names for parsing document contents.  Diacritical marks are removed. Use system wide default language for parsing documents.
	 */
	private Pattern[] defaultCalendarMonthsShortPatterns;

	/**
	 * Semesters full names for parsing document contents. Use system wide default language for parsing documents. 
	 */
	private String[] defaultCalendarSemesters;
	
	/**
	 * Object used for formatting decimal numbers according to the system standards
	 */
	private Numbers numbersFormat;

	/**
	 * Returns an object used to convert java.util.Date to a string representing the same timestamp according to localized
	 * date/time format.
	 */
	protected final ThreadLocal<SimpleDateFormat> sdfDisplay = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat(messageSource.getMessage("timestamp.format", null, LocaleContextHolder.getLocale()));
		}
	};

	/**
	 * Returns an object used to convert floating point number to a string representing the same number according to localized
	 * format.
	 */
	protected final ThreadLocal<DecimalFormat> decimalDisplay = new ThreadLocal<DecimalFormat>() {    	  
		@Override
		protected DecimalFormat initialValue() {
			DecimalFormatSymbols sym = new DecimalFormatSymbols();
			sym.setDecimalSeparator(messageSource.getMessage("decimal_char", null, LocaleContextHolder.getLocale()).charAt(0));
			sym.setGroupingSeparator(messageSource.getMessage("decimal_grouping_separator", null, LocaleContextHolder.getLocale()).charAt(0));
			return new DecimalFormat("###,###.#############", sym);
		}
    };

	/**
	 * Given a field name indexed in Elastic Search, returns the same or another name to be presented to user
	 */
	public String formatLabel(String label) {
		if (DocumentStoreAPIController.FIELD_DOC_ID.equals(label)) {
			label = messageSource.getMessage("doc_id", null, LocaleContextHolder.getLocale());
		}
		else if (DocumentStoreAPIController.FIELD_DOC_RECTIFIED.equals(label)) {
			label = messageSource.getMessage("doc_rectified", null, LocaleContextHolder.getLocale());
		}
		return label;
	}

	/**
	 * Given a field value indexed in Elastic Search, returns the same or another value to be presented to user
	 */
	public String formatValue(Object value) {
		if (value==null)
			return "";
		if (((value instanceof String) && "true".equalsIgnoreCase((String)value)) || Boolean.TRUE.equals(value))
			return messageSource.getMessage("yes", null, LocaleContextHolder.getLocale());
		if (((value instanceof String) && "false".equalsIgnoreCase((String)value)) || Boolean.FALSE.equals(value))
			return messageSource.getMessage("no", null, LocaleContextHolder.getLocale());
		if ((value instanceof String) && ParserUtils.isTimestamp((String)value))
			return sdfDisplay.get().format(ParserUtils.parseTimestamp((String)value)).replace(" 00:00:00", "");
		if ((value instanceof String) && ParserUtils.isDecimal((String)value))
			return decimalDisplay.get().format(Double.parseDouble((String)value));
		if (value instanceof Date)
			return sdfDisplay.get().format((Date)value);
		if (value instanceof Double)
			return decimalDisplay.get().format(value);
		if (value instanceof Float)
			return decimalDisplay.get().format(value);
		return String.valueOf(value);
	}
	
	/**
	 * Convert a month name into a month number (1 = JANUARY ... 12 = DECEMBER). Use the default language for parsing
	 * documents according to the application properties. Recognizes both long and short versions of months names. 
	 */
	public int parseMonth(String monthDesc) {
		if (monthDesc==null)
			return 0;
		monthDesc = monthDesc.trim();
		if (monthDesc.length()==0)
			return 0;
		if (ParserUtils.isOnlyNumbers(monthDesc)) {
			// If it's already numeric, returns the corresponding number, unless it's out of possible range
			int i = Integer.parseInt(monthDesc);
			if (i<1 || i>12)
				return 0;
			else
				return i;
		}
		
		monthDesc =
		Normalizer.normalize(monthDesc, Normalizer.Form.NFD).replaceAll("[\\p{InCombiningDiacriticalMarks}]", ""); // e.g.: Março => Marco
		
		Pattern[] fullMonthsPatterns = getDefaultCalendarMonthsPatterns();
		for (int i=0; i<fullMonthsPatterns.length; i++) {
			Pattern p = fullMonthsPatterns[i];
			if (p.matcher(monthDesc).find())
				return i+1;
		}
		Pattern[] shortMonthsPatterns = getDefaultCalendarMonthsShortPatterns();
		for (int i=0; i<shortMonthsPatterns.length; i++) {
			Pattern p = shortMonthsPatterns[i];
			if (p.matcher(monthDesc).find())
				return i+1;
		}
		return 0;
	}
	
	/**
	 * Months full names for parsing document contents.
	 */
	public String[] getDefaultCalendarMonths() {
		if (defaultCalendarMonths!=null)
			return defaultCalendarMonths;
		String monthsNames = messageSource.getMessage("calendar_months", null, getDocsLocale());
		defaultCalendarMonths = Arrays.stream(monthsNames.split(",")).map(String::trim).toArray(String[]::new);
		return defaultCalendarMonths;
	}
	
	/**
	 * Months short names for parsing document contents.  
	 */
	public String[] getDefaultCalendarMonthsShort() {
		if (defaultCalendarMonthsShort!=null)
			return defaultCalendarMonthsShort;
		String monthsShortNames = messageSource.getMessage("calendar_monthsShort", null, getDocsLocale());
		defaultCalendarMonthsShort = Arrays.stream(monthsShortNames.split(",")).map(String::trim).toArray(String[]::new);
		return defaultCalendarMonthsShort;
	}
	
	/**
	 * Patterns for months full names for parsing document contents. Diacritical marks are removed.
	 */
	public Pattern[] getDefaultCalendarMonthsPatterns() {
		if (defaultCalendarMonthsPatterns!=null)
			return defaultCalendarMonthsPatterns;
		String[] months = getDefaultCalendarMonths();
		defaultCalendarMonthsPatterns = Arrays.stream(months).map(month->
			Pattern.compile(
				"^"+Pattern.quote(
					Normalizer.normalize(month, Normalizer.Form.NFD).replaceAll("[\\p{InCombiningDiacriticalMarks}]", "") // e.g.: Março => Marco
					.trim()
					)
				+"$",
				Pattern.CASE_INSENSITIVE))
				.toArray(Pattern[]::new);
		return defaultCalendarMonthsPatterns;
	}
	
	/**
	 * Patterns form months short names for parsing document contents.  Diacritical marks are removed.
	 */
	public Pattern[] getDefaultCalendarMonthsShortPatterns() {
		if (defaultCalendarMonthsShortPatterns!=null)
			return defaultCalendarMonthsShortPatterns;
		String[] months = getDefaultCalendarMonthsShort();
		defaultCalendarMonthsShortPatterns = Arrays.stream(months).map(month->
			Pattern.compile(
				"^"+Pattern.quote(
					Normalizer.normalize(month, Normalizer.Form.NFD).replaceAll("[\\p{InCombiningDiacriticalMarks}]", "") // e.g.: Março => Marco
					.trim()
					)
				+"$",
				Pattern.CASE_INSENSITIVE))
				.toArray(Pattern[]::new);
		return defaultCalendarMonthsShortPatterns;
	}

	/**
	 * Semesters full names for parsing document contents.
	 */
	public String[] getDefaultCalendarSemesters() {
		if (defaultCalendarSemesters!=null)
			return defaultCalendarSemesters;
		String semestersNames = messageSource.getMessage("semesters", null, getDocsLocale());
		defaultCalendarSemesters = Arrays.stream(semestersNames.split(",")).map(String::trim).toArray(String[]::new);
		return defaultCalendarSemesters;
	}

	/**
	 * Locale object used for parsing documents in language specific syntax
	 */
	public Locale getDocsLocale() {
		if (docsLocale!=null)
			return docsLocale;
		String language = env.getProperty("cacao.user.language");
		String country = env.getProperty("cacao.user.country");
		docsLocale = new Locale(language, country);
		return docsLocale;
	}

	/**
	 * Decimal value treatment regarding 'tax due value' whenever generating payment slips
	 */
	public ValueTreatment getTaxValueTreatment() {
		if (taxValueTreatment==null) {
			try {
				String option = env.getProperty("tax.value.treatment");
				taxValueTreatment = ValueTreatment.parse(option);
			}
			catch (Exception ex) {
				taxValueTreatment = ValueTreatment.NONE;
			}
		}
		return taxValueTreatment;
	}
	
	/**
	 * Parse the provided text value into a boolean value. Tries different supported languages
	 */
	public Boolean parseBooleanValue(String value) {
		if (value==null)
			return null;
		value = value.trim();
		value = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("[\\p{InCombiningDiacriticalMarks}]", ""); // e.g.: Não => Nao
		value = value.toUpperCase();
		if ("YES".equals(value) || "SIM".equals(value) || "OUI".equals(value) || "SIN".equals(value) || "SI".equals(value)
				|| "TRUE".equals(value) || "1".equals(value))
			return Boolean.TRUE;
		if ("NOT".equals(value) || "NAO".equals(value) || "NON".equals(value) || "NAU".equals(value) || "NO".equals(value) 
				|| "FALSE".equals(value) || "0".equals(value))
			return Boolean.FALSE;
		return null;
	}
	
	/**
	 * Applies decimal value treatment over tax value. Useful for UI.
	 */
	public Number parseTaxValue(Number taxValue) {
		ValueTreatment treatment = getTaxValueTreatment();
		if (treatment==null)
			return taxValue;
		return treatment.apply(taxValue);
	}
	
	/**
	 * Applies decimal value formatting over tax value. Useful for UI.
	 */
	public String formatTaxValue(Number taxValue) {
		if (numbersFormat==null)
			numbersFormat = new Numbers(getDocsLocale());
		ValueTreatment treatment = getTaxValueTreatment();
		if (treatment==null)
			return ValueTreatment.NONE.format(taxValue, numbersFormat);
		else
			return treatment.format(taxValue, numbersFormat);		
	}

	/**
	 * Applies decimal value formatting over tax value. Useful for UI.
	 */
	public String formatTaxValue(Number taxValue, Numbers numbersFormat) {
		if (numbersFormat==null)
			numbersFormat = new Numbers(getDocsLocale());
		ValueTreatment treatment = getTaxValueTreatment();
		if (treatment==null)
			return ValueTreatment.NONE.format(taxValue, numbersFormat);
		else
			return treatment.format(taxValue, numbersFormat);		
	}
	
	/**
	 * Utility method for converting a tree-hierarchy of objects into a list of MenuItem objects, useful
	 * for presenting in UI with accordeon style 
	 */
	public void convertDocumentFieldIntoMenuItens(Map<String,Object> source, List<MenuItem> output) {
		convertDocumentFieldIntoMenuItens(source, output, null, null);
	}

	/**
	 * Analogous to {@link #convertDocumentFieldIntoMenuItens(Map, List) convertDocumentFieldIntoMenuItens}, but includes
	 * additional filter for indicating with TRUE which items should be considered, based on the fields name. These names
	 * are the full qualified names (includes the parents names).
	 */
	public void convertDocumentFieldIntoMenuItens(Map<String,Object> source, List<MenuItem> output, Predicate<String> filterByItemName) {
		convertDocumentFieldIntoMenuItens(source, output, filterByItemName, "");
	}

	private void convertDocumentFieldIntoMenuItens(Map<String,Object> source,List<MenuItem> output, Predicate<String> filterByItemName, String fieldPrefix) {
		for (Map.Entry<String, Object> entry:source.entrySet()) {
			String label = formatLabel(entry.getKey());
			String fieldName = (filterByItemName==null) ? null
					: (fieldPrefix==null || fieldPrefix.length()==0) ? label : fieldPrefix+DocumentField.GROUP_SEPARATOR+label;
			MenuItem item = new MenuItem(label);
			output.add(item);
			if (entry.getValue() instanceof Map) {
				List<MenuItem> children = new ArrayList<>();
				@SuppressWarnings("unchecked")
				Map<String,Object> innerStruct = (Map<String,Object>)entry.getValue();
				convertDocumentFieldIntoMenuItens(innerStruct,children,filterByItemName,fieldName);
				if (children.size()>1)
					Collections.sort(children);
				item.setChildren(children);
				if (filterByItemName!=null && children.isEmpty()) {
					// If we have applied filters over items and if we are left with not children for this item, 
					// remove this
					output.remove(item);
				}
			}
			else if (entry.getValue() instanceof Collection) {
				List<MenuItem> children = new ArrayList<>();			
				Collection<?> innerList = (Collection<?>)entry.getValue();
				for (Object value:innerList) {
					if (value instanceof Map) {
						@SuppressWarnings("unchecked")
						Map<String,Object> innerStruct = (Map<String,Object>)value;
						convertDocumentFieldIntoMenuItens(innerStruct,children,filterByItemName,fieldName);
					}
					else if (value!=null && (filterByItemName==null || filterByItemName.test(fieldName)) ) {
							children.add(new MenuItem(formatValue(value)));
					}
				}
				item.setChildren(children);
				if (filterByItemName!=null && children.isEmpty()) {
					// If we have applied filters over items and if we are left with not children for this item, 
					// remove this
					output.remove(item);
				}
			}
			else if (entry.getValue()!=null && (entry.getValue().getClass().isArray())) {
				List<MenuItem> children = new ArrayList<>();
				Object innerArray = entry.getValue();
				int size = Array.getLength(innerArray);
				for (int i=0;i<size;i++) {
					Object value = Array.get(innerArray, i);
					if (value==null)
						continue;
					if (value instanceof Map) {
						@SuppressWarnings("unchecked")
						Map<String,Object> innerStruct = (Map<String,Object>)value;
						convertDocumentFieldIntoMenuItens(innerStruct,children,filterByItemName,fieldName);
					}
					else if (value!=null) {
						if (filterByItemName==null || filterByItemName.test(fieldName))
							children.add(new MenuItem(formatValue(value)));
					}
				}
				item.setChildren(children);
				if (filterByItemName!=null && children.isEmpty()) {
					// If we have applied filters over items and if we are left with not children for this item, 
					// remove this
					output.remove(item);
				}
			}
			else if (entry.getValue()!=null) {
				if (filterByItemName==null || filterByItemName.test(fieldName))
					item.addChild(new MenuItem(formatValue(entry.getValue())));
				else {
					// If we have applied filters over items and if this field name did not match the predicate, remove this
					output.remove(item);
				}
			}
			else if (filterByItemName!=null) {
				// If we have applied filters over items and if we have no value informed in this field, remove this
				output.remove(item);
			}
		}
	}
	
	/**
	 * Given a hierarchy of values 'parsed_contents', returns the standard field names (i.e.: concatenating group names into field names)
	 */
	public DocumentTemplate convertDocumentFields(Map<String,Object> parsedContents) {
		DocumentTemplate parsedContentsCanonicalRepresentation = new DocumentTemplate();
		convertDocumentFields(parsedContents, "", parsedContentsCanonicalRepresentation);
		return parsedContentsCanonicalRepresentation;
	}

	/**
	 * Recursive function for storing inside 'DocumentTemplate' several objects of type 'DocumentField' generated from parsed file contents
	 */
	public void convertDocumentFields(Map<String,Object> source, String fieldPrefix, DocumentTemplate output) {
		for (Map.Entry<String, Object> entry:source.entrySet()) {
			String label = formatLabel(entry.getKey());
			String fieldName = (fieldPrefix==null || fieldPrefix.length()==0) ? label : fieldPrefix+DocumentField.GROUP_SEPARATOR+label;
			if (fieldName.length()>MAX_FIELD_NAME_LENGTH)
				continue; // ignore fields with names that are too long
			if (entry.getValue() instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String,Object> innerStruct = (Map<String,Object>)entry.getValue();
				convertDocumentFields(innerStruct,fieldName,output);
			}
			else if (entry.getValue() instanceof Collection) {
				Collection<?> innerList = (Collection<?>)entry.getValue();
				int i = 0;
				for (Object value:innerList) {
					String fieldNameIdx = fieldName+"["+(i+1)+"]";
					if (value instanceof Map) {
						@SuppressWarnings("unchecked")
						Map<String,Object> innerStruct = (Map<String,Object>)value;
						convertDocumentFields(innerStruct,fieldNameIdx,output);
					}
					else if (value!=null) {
						output.addField(fieldNameIdx);
					}
					i++;
				}
			}
			else if (entry.getValue()!=null && (entry.getValue().getClass().isArray())) {
				Object innerArray = entry.getValue();
				int size = Array.getLength(innerArray);
				for (int i=0;i<size;i++) {
					String fieldNameIdx = fieldName+"["+(i+1)+"]";
					Object value = Array.get(innerArray, i);
					if (value==null)
						continue;
					if (value instanceof Map) {
						@SuppressWarnings("unchecked")
						Map<String,Object> innerStruct = (Map<String,Object>)value;
						convertDocumentFields(innerStruct,fieldNameIdx,output);
					}
					else if (value!=null) {
						output.addField(fieldNameIdx);
					}
				}
			}
			else if (entry.getValue()!=null) {
				output.addField(fieldName);
			}
		}
	}

	public FieldsConventionsService forTesting() {
		
		System.setProperty("cacao.user.language", "pt");
		System.setProperty("cacao.user.country", "PT");
		env = new StandardServletEnvironment();
		
		messageSource = new ResourceBundleMessageSource();
		((ResourceBundleMessageSource)messageSource).setDefaultEncoding("UTF-8");
		((ResourceBundleMessageSource)messageSource).setBasename("messages");
		
		return this;
	}
	
	/**
	 * Parse the 'max tax period' informed in 'DocumentTemplate' with the format 'MM/YYYY' and returns this as a number
	 * at the format 'YYYYMM'
	 */
	public static Number parseTemplateTaxPeriodMax(String periodMax) {
		if (periodMax==null || periodMax.trim().length()==0)
			return null;
		Matcher m = TAX_PERIOD_MAX_PATTERN.matcher(periodMax);
		if (!m.find())
			return null;
		int year = Integer.parseInt(m.group(2));
		if (year<20)
			year += 2000;
		int month = Integer.parseInt(m.group(1));
		return year * 100 + month;
	}
}
