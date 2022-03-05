/**
 * 
 */
package org.idb.cacao.validator.validations;

import static org.idb.cacao.api.utils.ParserUtils.getYearMonth;
import static org.idb.cacao.api.utils.ParserUtils.isBoolean;
import static org.idb.cacao.api.utils.ParserUtils.isDMY;
import static org.idb.cacao.api.utils.ParserUtils.isDecimal;
import static org.idb.cacao.api.utils.ParserUtils.isDecimalWithComma;
import static org.idb.cacao.api.utils.ParserUtils.isInteger;
import static org.idb.cacao.api.utils.ParserUtils.isMDY;
import static org.idb.cacao.api.utils.ParserUtils.isMY;
import static org.idb.cacao.api.utils.ParserUtils.isMonthYear;
import static org.idb.cacao.api.utils.ParserUtils.isTimestamp;
import static org.idb.cacao.api.utils.ParserUtils.isYM;
import static org.idb.cacao.api.utils.ParserUtils.isYMD;
import static org.idb.cacao.api.utils.ParserUtils.isYearMonth;
import static org.idb.cacao.api.utils.ParserUtils.parseDMY;
import static org.idb.cacao.api.utils.ParserUtils.parseMDY;
import static org.idb.cacao.api.utils.ParserUtils.parseTimestamp;
import static org.idb.cacao.api.utils.ParserUtils.parseTimestampES;
import static org.idb.cacao.api.utils.ParserUtils.parseTimestampWithMS;
import static org.idb.cacao.api.utils.ParserUtils.parseYMD;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.idb.cacao.api.DocumentUploaded;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.DomainEntry;
import org.idb.cacao.api.templates.DomainTable;
import org.idb.cacao.api.templates.FieldMapping;
import org.idb.cacao.api.templates.FieldType;
import org.idb.cacao.api.utils.ParserUtils;
import org.idb.cacao.validator.repositories.DomainTableRepository;

/**
 * A set of utilities methods for file content validation purpose. <br>
 * 
 * An instance of {@link ValidationContext} must be provided before validations.
 * 
 * @author Rivelino Patrício
 * 
 * @since 17/11/2021
 *
 */
public class Validations {
	
	private static final Logger log = Logger.getLogger(Validations.class.getName());

	/**
	 * Maximum length of field size saved to elasticsearch database
	 */
	private static final int MAX_ES_FIELD_SIZE = 1_000;
	
	private static final Pattern COMMON_TAXPAYER_ID_FIELD_NAME = Pattern.compile("^TaxPayerId$", Pattern.CASE_INSENSITIVE);
	private static final Pattern COMMON_TAX_PERIOD_FIELD_NAME = Pattern.compile("^(?>TaxPeriodNumber|TaxPeriod)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern COMMON_TAX_YEAR_FIELD_NAME = Pattern.compile("^(?>Year|TaxYear)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern COMMON_TAX_MONTH_FIELD_NAME = Pattern.compile("^(?>Month|TaxMonthNumber|TaxMonth)$", Pattern.CASE_INSENSITIVE);


	private final ValidationContext validationContext;
	
	private final DomainTableRepository domainTableRepository;

	public Validations(ValidationContext validationContext, DomainTableRepository domainTableRepository) {
		this.validationContext = validationContext;
		this.domainTableRepository = domainTableRepository;
	}

	/**
	 * Check for required fields in document uploaded. <br>
	 * 
	 * All validations error will be inserted on {@link ValidationContext#addAlert(String)}. <br>
	 * 
	 */
	public void checkForRequiredFields() {
		checkForRequiredFields(/*acceptIncompleteFiles*/false);
	}
	
	/**
	 * Check for required fields in document uploaded. <br>
	 * 
	 * All validations error will be inserted on {@link ValidationContext#addAlert(String)}. <br>
	 * 
	 */
	public void checkForRequiredFields(boolean acceptIncompleteFiles) {

		// Get a list of fields
		List<DocumentField> allFields = validationContext.getDocumentTemplate().getFields();

		if (allFields == null || allFields.isEmpty())
			return;

		// Get a list of required fields
		List<String> requiredFields = allFields.stream().filter(field -> Boolean.TRUE.equals(field.getRequired()))
				.map(field -> field.getFieldName()).collect(Collectors.toList());

		if (requiredFields == null || requiredFields.isEmpty())
			return;

		List<Map<String, Object>> parsedContents = validationContext.getParsedContents();

		if (parsedContents == null || parsedContents.isEmpty())
			return;

		List<Map<String, Object>> toRemove = (acceptIncompleteFiles) ? new LinkedList<>() : null;
		parsedContents.parallelStream().iterator().forEachRemaining(values -> {
			
			boolean markedRecordToRemove = false;

			for (String fieldName : requiredFields) {
				if (values.get(fieldName) == null) {
					addLogError("{field.value.not.found(" + fieldName+ ")}", /*criticalError*/!acceptIncompleteFiles);
					if (acceptIncompleteFiles && !markedRecordToRemove) {
						markedRecordToRemove = true;
						toRemove.add(values);
					}
				}
			}

		});
		
		if (toRemove!=null && !toRemove.isEmpty()) {
			parsedContents.removeAll(toRemove);
		}

	}

	/**
	 * Add alert/error message to application log and {@link ValidationContext} instance.
	 * 
	 * @param message	A message to be logged
	 */
	public void addLogError(String message) {
		addLogError(message, /*criticalError*/true);
	}
	
	/**
	 * Add alert/error message to application log and {@link ValidationContext} instance.
	 * 
	 * @param message	A message to be logged
	 * @param criticalError	 Tells whether this error may or may not prevent the file from being accepted.
	 */
	public synchronized void addLogError(String message, boolean criticalError) {
		if (criticalError) {
			validationContext.addAlert(message);
			log.log(Level.WARNING, "Document Id: " + validationContext.getDocumentUploaded().getId() + " => " +
					message);
		}
		else {
			validationContext.addNonCriticalAlert(message);
			log.log(Level.FINE, "Document Id: " + validationContext.getDocumentUploaded().getId() + " => " +
					message);
		}
	}

	/**
	 * Check for data types in all fields in document uploaded <br>
	 * 
	 * All validations error will be inserted on {@link ValidationContext#addAlert(String)}. <br>
	 * 
	 */
	public void checkForFieldDataTypes() {
		checkForFieldDataTypes(/*acceptIncompleteFiles*/false);
	}
	
	/**
	 * Check for data types in all fields in document uploaded <br>
	 * 
	 * All validations error will be inserted on {@link ValidationContext#addAlert(String)}. <br>
	 * 
	 */
	public void checkForFieldDataTypes(boolean acceptIncompleteFiles) {

		// Get a list of fields
		List<DocumentField> allFields = validationContext.getDocumentTemplate().getFields();

		if (allFields == null || allFields.isEmpty())
			return;

		// Remove fields from validation
		List<DocumentField> fields = allFields.stream().filter(field -> (!FieldType.NESTED.equals(field.getFieldType()))).collect(Collectors.toList());

		if (fields == null || fields.isEmpty())
			return;

		List<Map<String, Object>> parsedContents = validationContext.getParsedContents();

		if (parsedContents == null || parsedContents.isEmpty())
			return;
		
		parsedContents.parallelStream().iterator().forEachRemaining(values -> {

			for (DocumentField field : fields) {

				try {
					Object fieldValue = values.get(field.getFieldName());

					// If field value is null, there is nothing to check
					if (fieldValue == null)
						continue;

					if (FieldType.BOOLEAN.equals(field.getFieldType()))
						fieldValue = checkBooleanValue(field.getFieldName(), fieldValue, !acceptIncompleteFiles && Boolean.TRUE.equals(field.getRequired()));

					else if (FieldType.CHARACTER.equals(field.getFieldType()) || FieldType.DOMAIN.equals(field.getFieldType()) )
						fieldValue = checkCharacterValue(field, fieldValue);

					else if (FieldType.DATE.equals(field.getFieldType()))
						fieldValue = checkDateValue(field.getFieldName(), fieldValue, !acceptIncompleteFiles && Boolean.TRUE.equals(field.getRequired()));

					else if (FieldType.DECIMAL.equals(field.getFieldType()))
						fieldValue = checkDecimalValue(field.getFieldName(), fieldValue, !acceptIncompleteFiles && Boolean.TRUE.equals(field.getRequired()));

					else if (FieldType.GENERIC.equals(field.getFieldType()))
						fieldValue = checkGenericValue(field, fieldValue);

					else if (FieldType.INTEGER.equals(field.getFieldType()))
						fieldValue = checkIntegerValue(field.getFieldName(), fieldValue, !acceptIncompleteFiles && Boolean.TRUE.equals(field.getRequired()));

					else if (FieldType.MONTH.equals(field.getFieldType()))
						fieldValue = checkMonthValue(field.getFieldName(), fieldValue, !acceptIncompleteFiles && Boolean.TRUE.equals(field.getRequired()));

					else if (FieldType.TIMESTAMP.equals(field.getFieldType()))
						fieldValue = checkTimestampValue(field.getFieldName(), fieldValue, !acceptIncompleteFiles && Boolean.TRUE.equals(field.getRequired()));

					// Update field value to it's new representation
					values.replace(field.getFieldName(), fieldValue);
				} catch (Exception e) {
					log.log(Level.SEVERE,"Error parsing record values.", e);					
				}

			}

		});
	}

	/**
	 * Validate and transform field value from {@link FieldType.GENERIC}
	 * 
	 * @param field
	 * @param fieldValue
	 * @return Validated and transformed field value
	 */
	private Object checkGenericValue(DocumentField field, Object fieldValue) {

		if (fieldValue == null)
			return null;

		String value = ValidationContext.toString(fieldValue);

		Integer maxLength = (field.getMaxLength()==null) ? null : Math.min(field.getMaxLength(),value.length());

		if (maxLength != null && maxLength.intValue() > 0) {
			value = value.substring(0, maxLength);
		} else {
			if (value.length() > MAX_ES_FIELD_SIZE)
				value = value.substring(0, MAX_ES_FIELD_SIZE);
		}

		return value;
	}

	/**
	 * Validate and transform field value from {@link FieldType.CHARACTER}
	 * 
	 * @param field
	 * @param fieldValue
	 * @return Validated and transformed field value
	 */
	private Object checkCharacterValue(DocumentField field, Object fieldValue) {
		if (fieldValue == null)
			return null;

		String value = ValidationContext.toString(fieldValue);

		Integer maxLength = (field.getMaxLength()==null) ? null : Math.min(field.getMaxLength(),value.length());

		if (maxLength != null && maxLength.intValue() > 0) {
			value = value.substring(0, maxLength);
		} else {
			if (value.length() > MAX_ES_FIELD_SIZE)
				value = value.substring(0, MAX_ES_FIELD_SIZE);
		}

		return value;
	}

	/**
	 * Validate and transform field value from {@link FieldType.TIMESTAMP}
	 * 
	 * @param fieldName
	 * @param fieldValue
	 * @return Validated and transformed field value
	 */
	private Object checkTimestampValue(String fieldName, Object fieldValue, boolean required) {
		if (fieldValue == null)
			return null;

		if ( (fieldValue instanceof Date) || (fieldValue instanceof OffsetDateTime) )
			return fieldValue;

		String value = ValidationContext.toString(fieldValue);
		if (value==null || value.trim().length()==0)
			return null;

		// Will not try to convert numbers into dates using UNIX EPOCH because it would result
		// in unexpected result if the incoming data is not what it was supposed to be (e.g.: maybe the data
		// came from Excel spreadsheet where some rows contains additional 'sub-total' information)
		//if (isOnlyNumbers(value))
		//	return new Date(Long.parseLong(value));
		
		if ( isTimestamp(value) )
			return parseTimestamp(value);

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

		addLogError("{field.value.invalid(" + value + "," + fieldName + ")}", /*criticalError*/required);
		return null;
	}

	/**
	 * Validate and transform field value from {@link FieldType.MONTH}
	 * 
	 * @param fieldName
	 * @param fieldValue
	 * @return Validated and transformed field value
	 */
	private Object checkMonthValue(String fieldName, Object fieldValue, boolean required) {

		if (fieldValue == null)
			return null;		
		
		if ( fieldValue instanceof Date ) {
			return ParserUtils.formatMonth((Date)fieldValue);
		}
		
		if ( fieldValue instanceof OffsetDateTime ) {
			return ParserUtils.formatMonth((OffsetDateTime)fieldValue);
		}
		
		String value = ValidationContext.toString(fieldValue);
		
		if ( isMY(value) ) {			
			return value.substring(3) + "-" + value.substring(0,2);
		}
		
		if ( isYM(value) ) {
			return value.replace('/', '-').replace('\\', '-');
		}
		
		if ( isMonthYear(value) || isYearMonth(value) ) {
			
			String toRet = getYearMonth(value);
			if ( toRet != null )
				return toRet;
		}		
		
		addLogError("{field.value.invalid(" + value + "," + fieldName + ")}", /*criticalError*/required);
		return null;		
		
	}

	/**
	 * Validate and transform field value from {@link FieldType.INTEGER}
	 * 
	 * @param fieldName
	 * @param fieldValue
	 * @return Validated and transformed field value
	 */
	private Object checkIntegerValue(String fieldName, Object fieldValue, boolean required) {
		if (fieldValue == null)
			return null;

		if (fieldValue instanceof Number || fieldValue.getClass().isAssignableFrom(int.class))
			return fieldValue;

		String value = ValidationContext.toString(fieldValue);
		if (value==null || value.trim().length()==0)
			return null;

		if (isInteger(value)) {
			try {
				return Long.parseLong(value);
			}
			catch (NumberFormatException ex) {
				// Max LONG = 9223372036854775807, so if value is bigger than 18 digits, try to translate it as a BigInteger
				if (value.length()>18) {
					return new BigInteger(value);
				}
				else {
					addLogError("{field.value.invalid(" + value + "," + fieldName + ")}", /*criticalError*/required);
					return null;
				}
			}
		}

		// Check other situations
		try {
			Number parsedNumber = ValidationContext.toNumber(value);
			if (parsedNumber!=null)
				return parsedNumber.longValue();
		} catch (Throwable ex) { }
		
		addLogError("{field.value.invalid(" + value + "," + fieldName + ")}", /*criticalError*/required);
		return null;

	}

	/**
	 * Validate and transform field value from {@link FieldType.DECIMAL}
	 * 
	 * @param fieldName
	 * @param fieldValue
	 * @return Validated and transformed field value
	 */
	private Object checkDecimalValue(String fieldName, Object fieldValue, boolean required) {

		if (fieldValue == null)
			return null;
		
		if (fieldValue instanceof Number || fieldValue.getClass().isAssignableFrom(double.class))
			return fieldValue;

		String value = ValidationContext.toString(fieldValue);
		if (value==null || value.trim().length()==0)
			return null;

		try {
			if (isDecimal(value))
				return Double.parseDouble(value);
	
			if (isDecimalWithComma(value))
				return Double.parseDouble(value.replace(".", "").replace(",", "."));
			
			if (isInteger(value))
				return Double.parseDouble(value);
		}
		catch (NumberFormatException ex) {
			addLogError("{field.value.invalid(" + value + "," + fieldName + ")}", /*criticalError*/required);
			return null;
		}

		addLogError("{field.value.invalid(" + value + "," + fieldName + ")}", /*criticalError*/required);
		return null;
	}

	/**
	 * Validate and transform field value from {@link FieldType.DATE}
	 *
	 * @param fieldName
	 * @param fieldValue
	 * @return Validated and transformed field value
	 */
	private Object checkDateValue(String fieldName, Object fieldValue, boolean required) {

		if (fieldValue == null)
			return null;
		
		if ( (fieldValue instanceof Date) || (fieldValue instanceof OffsetDateTime) )
			return fieldValue;
		
		// Will not try to convert numbers into dates using UNIX EPOCH because it would result
		// in unexpected result if the incoming data is not what it was supposed to be (e.g.: maybe the data
		// came from Excel spreadsheet where some rows contains additional 'sub-total' information)
		//if ( fieldValue instanceof Number )
		//	return new Date(((Number)(fieldValue)).longValue());

		String value = ValidationContext.toString(fieldValue);
		if (value==null || value.trim().length()==0)
			return null;

		if (isMDY(value))
			return parseMDY(value);
		if (isDMY(value))
			return parseDMY(value);
		if (isYMD(value))
			return parseYMD(value);
		
		addLogError("{field.value.invalid(" + value + "," + fieldName + ")}", /*criticalError*/required);
		return null;
	}

	/**
	 * Validate and transform field value from {@link FieldType.BOOLEAN}
	 * 
	 * @param fieldName
	 * @param fieldValue
	 * @return Validated and transformed field value
	 */
	private Object checkBooleanValue(String fieldName, Object fieldValue, boolean required) {
		if (fieldValue == null)
			return null;

		if (fieldValue instanceof Boolean || fieldValue.getClass().isAssignableFrom(boolean.class))
			return fieldValue;

		String value = ValidationContext.toString(fieldValue);
		if (value==null || value.trim().length()==0)
			return null;

		if (isBoolean(value))
			return Boolean.parseBoolean(value);

		addLogError("{field.value.invalid(" + value + "," + fieldName + ")}", /*criticalError*/required);
		return null;

	}

	/**
	 * Check if values provided on fields that points to a domain table are present
	 * on domain tables entries. <br>
	 * 
	 * All validations error will be inserted on {@link ValidationContext#addAlert(String)}. <br>
	 */
	public void checkForDomainTableValues() {
		checkForDomainTableValues(/*acceptIncompleteFiles*/false);
	}
	
	/**
	 * Check if values provided on fields that points to a domain table are present
	 * on domain tables entries. <br>
	 * 
	 * All validations error will be inserted on {@link ValidationContext#addAlert(String)}. <br>
	 */
	public void checkForDomainTableValues(boolean acceptIncompleteFiles) {

		// Get a list of fields
		List<DocumentField> allFields = validationContext.getDocumentTemplate().getFields();

		if (allFields == null || allFields.isEmpty())
			return;

		// Remove CHARACTER fields from validation
		List<DocumentField> fields = allFields.stream().filter(field -> FieldType.DOMAIN.equals(field.getFieldType()))
				.collect(Collectors.toList());

		if (fields == null || fields.isEmpty())
			return;

		// Get a list of previous parsed contents
		List<Map<String, Object>> parsedContents = validationContext.getParsedContents();

		if (parsedContents == null || parsedContents.isEmpty())
			return;

		// Keeps all needed domain tables in memory
		Map<String, DomainTable> domainTables = new HashMap<>();

		// Check all records
		parsedContents.stream().iterator().forEachRemaining(values -> {

			// Check all fields for a specific record
			for (DocumentField field : fields) {

				// Get field value
				Object fieldValue = values.get(field.getFieldName());

				// TODO - check this rule
				// If field value is null, there is nothing to check
				if (fieldValue == null)
					continue;

				// Get domain table
				DomainTable table = getDomainTable(field, domainTables);

				// If domain table wasn't not found, add an error message
				if (table == null) {
					addLogError("{domain.table.not.found(" + field.getDomainTableName() + ")(" + 
							field.getDomainTableVersion() + ")}", /*criticalError*/true);
					continue;
				}

				// Check field value against domain table entries
				// If value is not present on table, add an error message
				Pair<Boolean, String> result = checkDomainValue(fieldValue, table);

				// If value is not present at domain table entries, add error message
				if (!result.getKey()) {
					final boolean required = !acceptIncompleteFiles && Boolean.TRUE.equals(field.getRequired());
					addLogError("{field.domain.value.not.found(" + fieldValue + ")(" + field.getFieldName() + ")}", /*criticalError*/required);
				} else {
					String newValue = result.getValue();
					// If value need to be updated, change value in record
					if (newValue != null)
						values.replace(field.getFieldName(), newValue);
				}

			}
		});
	}

	/**
	 * Check if a specific given value is present on a given table. <br>
	 * 
	 * @param fieldValue
	 * @param table
	 * @return A {@link Pair} where key is a boolean that indicates if value is
	 *         present in domain table, and the value is a key value for a given
	 *         description, in case the provided value is not the key.
	 */
	private Pair<Boolean, String> checkDomainValue(Object fieldValue, DomainTable table) {

		if (fieldValue == null || table == null)
			return Pair.of(Boolean.TRUE, null);

		String value = ValidationContext.toString(fieldValue);

		if (table.getEntry(value) == null) {
			DomainEntry entry = table.getEntries().parallelStream()
					.filter(e -> e.getDescription().equalsIgnoreCase(value)).findFirst().orElse(null);
			if (entry != null) {
				return Pair.of(Boolean.TRUE, entry.getKey());
			}
		} else {
			return Pair.of(Boolean.TRUE, null);
		}

		return Pair.of(Boolean.FALSE, null);
	}

	/**
	 * Find and return a {@link DomainTable} for a given {@link DocumentField}. <br>
	 * 
	 * @param field        The {@link DocumentField} with information about name and
	 *                     version. <br>
	 * @param domainTables A {@link Map} with all domain tables needed. <br>
	 * @return A domain table for name and version specifieds in field or null if
	 *         table doesn't exist. <br>
	 */
	private synchronized DomainTable getDomainTable(DocumentField field, Map<String, DomainTable> domainTables) {

		if (domainTables == null)
			domainTables = new HashMap<>();

		DomainTable table = domainTables.get(field.getFieldName());

		if (table != null)
			return table;

		String tableName = field.getDomainTableName();
		String tableVersion = field.getDomainTableVersion();

		table = domainTableRepository.findByNameAndVersion(tableName, tableVersion).orElse(null);
		domainTables.put(field.getFieldName(), table);
		return table;

	}

	/**
	 * Try to find taxpayer and tax period information on parsed data. <br>
	 * 
	 * All validations error will be inserted on {@link ValidationContext#addAlert(String)}. <br>
	 * 
	 */
	public void addTaxPayerInformation() {
		
		if ( validationContext == null )
			return;
		
		DocumentUploaded doc = validationContext.getDocumentUploaded();
		
		if ( doc == null )
			return;
				
		// Get a list of previous parsed contents
		List<Map<String, Object>> parsedContents = validationContext.getParsedContents();

		if (parsedContents == null || parsedContents.isEmpty())
			return;
		
		// Consider the first line of parsed data
		Map<String,Object> record = parsedContents.get(0);

		// If we have a DocumentField with the corresponding FieldMapping (ex: for TAXPAYERID), let's use it for the identification
		// If we have ambiguity, let's resolve the ambiguity giving priority to the required field, and then considering the given field name.
		
		// Taxpayer Id.....
		DocumentField field_for_taxpayer_id = getFieldForFileIdentification(validationContext.getDocumentTemplate(), FieldMapping.TAXPAYER_ID, COMMON_TAXPAYER_ID_FIELD_NAME);
		Object taxpayer_id = (field_for_taxpayer_id==null) ? null : 
			record.entrySet().stream().filter(entry->entry.getKey().equalsIgnoreCase(field_for_taxpayer_id.getFieldName())).findFirst().map(Map.Entry::getValue).orElse(null);
		if (taxpayer_id!=null) {
			doc.setTaxPayerId(ValidationContext.toString(taxpayer_id));
		}
		
		// Tax year .....
		DocumentField field_for_tax_year = getFieldForFileIdentification(validationContext.getDocumentTemplate(), FieldMapping.TAX_YEAR, COMMON_TAX_YEAR_FIELD_NAME);
		Object tax_year = (field_for_tax_year==null) ? null : 
			record.entrySet().stream().filter(entry->entry.getKey().equalsIgnoreCase(field_for_tax_year.getFieldName())).findFirst().map(Map.Entry::getValue).orElse(null);
		if (tax_year!=null) {
			Integer value = ParserUtils.parseIntegerNE(ValidationContext.toString(tax_year));
			if ( value != null ) {
				doc.setTaxYear(value);
				doc.setTaxPeriodNumber(value);
			}
		}
		
		// Tax month ....
		DocumentField field_for_tax_month = getFieldForFileIdentification(validationContext.getDocumentTemplate(), FieldMapping.TAX_MONTH, COMMON_TAX_MONTH_FIELD_NAME);
		Object tax_month = (field_for_tax_month==null) ? null : 
			record.entrySet().stream().filter(entry->entry.getKey().equalsIgnoreCase(field_for_tax_month.getFieldName())).findFirst().map(Map.Entry::getValue).orElse(null);
		if (tax_month!=null) {
			String tax_month_as_text = ValidationContext.toString(tax_month);
			if (ParserUtils.isOnlyNumbers(tax_month_as_text)) {
				Integer value = ParserUtils.parseIntegerNE(tax_month_as_text);
				if ( value != null ) {
					doc.setTaxMonthNumber(value);
					if (doc.getTaxYear()!=null) {
						doc.setTaxPeriodNumber(doc.getTaxYear()*100+value); // YYYYMM
					}
				}
			}
			else {
				doc.setTaxMonth(tax_month_as_text);
				Integer value = ParserUtils.parseMonth(tax_month_as_text);
				if ( value != null ) {
					doc.setTaxMonthNumber(value);
					if (doc.getTaxYear()!=null) {
						doc.setTaxPeriodNumber(doc.getTaxYear()*100+value); // YYYYMM
					}
				}
			}
		}

		// Tax period.....
		DocumentField field_for_tax_period = getFieldForFileIdentification(validationContext.getDocumentTemplate(), null, COMMON_TAX_PERIOD_FIELD_NAME);
		Object tax_period = (field_for_tax_period==null) ? null : 
			record.entrySet().stream().filter(entry->entry.getKey().equalsIgnoreCase(field_for_tax_period.getFieldName())).findFirst().map(Map.Entry::getValue).orElse(null);
		if (tax_period!=null) {
			String tax_period_as_text = ValidationContext.toString(tax_period);
			if (ParserUtils.isOnlyNumbers(tax_period_as_text)) {
				Integer value = ParserUtils.parseIntegerNE(tax_period_as_text);
				if ( value != null ) {
					doc.setTaxPeriodNumber(value);	
				}
			}
			else {
				YearMonth yearMonth = ParserUtils.parseYearMonth(tax_period_as_text);
				if (yearMonth!=null) {
					doc.setTaxYear(yearMonth.getYear());
					doc.setTaxMonthNumber(yearMonth.getMonthValue());
					doc.setTaxPeriodNumber(yearMonth.getYear()*100+yearMonth.getMonthValue());
				}
			}			
		}
		
		//If TaxPayerId wasn't found, add an error message
		if ( doc.getTaxPayerId() == null ) {
			addLogError("{taxpayerid.not.found}", /*criticalError*/true);
		}
		
		//If TaxPeriod wasn't found, add an error message (unless we don't have any such field)
		if ( doc.getTaxYear() == null && doc.getTaxMonth() == null && doc.getTaxMonthNumber() == null &&
				doc.getTaxPeriod() == null && doc.getTaxPeriodNumber() == null ) {
			if (field_for_tax_year!=null || field_for_tax_month!=null || field_for_tax_period!=null)
				addLogError("{taxperiod.not.found}", /*criticalError*/true);	
		}
		
	}
	
	/**
	 * Given the fields of template, return one particular field according to the following rules:<BR>
	 * - Check for fields with the given field mapping. If there is only one, return it.<BR>
	 * - If there are more fields with the given field mapping, return one given the following priority order: file uniqueness, required field, pattern of common field names, id of the field.<BR>
	 * - If there are no matching fields according to the previous criteria, return one according to the pattern of common field names.<BR>
	 * - Returns NULL if there is no match.
	 */
	public static DocumentField getFieldForFileIdentification(DocumentTemplate template, FieldMapping mapping, Pattern commonFieldNames) {
		List<DocumentField> fields_with_mapping = (mapping==null) ? Collections.emptyList() : template.getFieldsOfTypeSortedById(mapping);
		if (fields_with_mapping.size()==1) {
			return fields_with_mapping.get(0);
		}
		else if (fields_with_mapping.size()>1) {
			DocumentField best_guess = null;
			for (DocumentField ambiguous_field: fields_with_mapping) {
				if (best_guess==null) {
					best_guess = ambiguous_field;
				}
				else if (Boolean.TRUE.equals(ambiguous_field.getFileUniqueness())
					&& !Boolean.TRUE.equals(best_guess.getFileUniqueness())) {
					best_guess = ambiguous_field;
				}
				else if (Boolean.TRUE.equals(ambiguous_field.getRequired())
					&& !Boolean.TRUE.equals(best_guess.getRequired())) {
					best_guess = ambiguous_field;
				}
				else if (commonFieldNames.matcher(ambiguous_field.getFieldName()).find()
					&& !commonFieldNames.matcher(best_guess.getFieldName()).find()) {
					best_guess = ambiguous_field;
				}
			}
			return best_guess;
		}
		else {
			for (DocumentField field: template.getFields()) {
				if (field==null || field.getFieldName()==null)
					continue;
				if (commonFieldNames.matcher(field.getFieldName()).find())
					return field;
			}			
		}
		return null;
	}

}
