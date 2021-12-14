/**
 * 
 */
package org.idb.cacao.validator.validations;

import static org.idb.cacao.api.utils.ParserUtils.isBoolean;
import static org.idb.cacao.api.utils.ParserUtils.isDMY;
import static org.idb.cacao.api.utils.ParserUtils.isDecimal;
import static org.idb.cacao.api.utils.ParserUtils.isDecimalWithComma;
import static org.idb.cacao.api.utils.ParserUtils.isInteger;
import static org.idb.cacao.api.utils.ParserUtils.isMDY;
import static org.idb.cacao.api.utils.ParserUtils.isMY;
import static org.idb.cacao.api.utils.ParserUtils.isYM;
import static org.idb.cacao.api.utils.ParserUtils.isOnlyNumbers;
import static org.idb.cacao.api.utils.ParserUtils.isYMD;
import static org.idb.cacao.api.utils.ParserUtils.parseDMY;
import static org.idb.cacao.api.utils.ParserUtils.parseMDY;
import static org.idb.cacao.api.utils.ParserUtils.*;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.idb.cacao.api.DocumentUploaded;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DomainEntry;
import org.idb.cacao.api.templates.DomainTable;
import org.idb.cacao.api.templates.FieldType;
import org.idb.cacao.api.utils.ParserUtils;
import org.idb.cacao.api.utils.StringUtils;
import org.idb.cacao.validator.repositories.DomainTableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
@Component
public class Validations {
	
	private static final Logger log = Logger.getLogger(Validations.class.getName());

	/**
	 * Maximum length of field size saved to elasticsearch database
	 */
	private static final int MAX_ES_FIELD_SIZE = 1_000;

	@Autowired
	private DomainTableRepository domainTableRepository;
	
	private ValidationContext validationContext;

	public void setValidationContext(ValidationContext validationContext) {
		this.validationContext = validationContext;
	}

	/**
	 * Check for required fields in document uploaded. <br>
	 * 
	 * All validations error will be inserted on {@link ValidationContext#addAlert(String)}. <br>
	 * 
	 */
	public void checkForRequiredFields() {

		// Get a list of fields
		List<DocumentField> allFields = validationContext.getDocumentTemplate().getFields();

		if (allFields == null || allFields.isEmpty())
			return;

		// Get a list of required fields
		List<String> requiredFields = allFields.stream().filter(field -> field.getRequired())
				.map(field -> field.getFieldName()).collect(Collectors.toList());

		if (requiredFields == null || requiredFields.isEmpty())
			return;

		List<Map<String, Object>> parsedContents = validationContext.getParsedContents();

		if (parsedContents == null || parsedContents.isEmpty())
			return;

		parsedContents.parallelStream().iterator().forEachRemaining(values -> {

			for (String fieldName : requiredFields) {
				if (values.get(fieldName) == null)
					addLogError("{field.value.not.found(" + fieldName+ ")}");
			}

		});

	}

	/**
	 * Add alert/error message to application log and {@link ValidationContext} instance.
	 * 
	 * @param message	A message to be logged
	 */
	public synchronized void addLogError(String message) {
		validationContext.addAlert(message);
		log.log(Level.WARNING, "Document Id: " + validationContext.getDocumentUploaded().getId() + " => " +
				message);
	}

	/**
	 * Check for data types in all fields in document uploaded <br>
	 * 
	 * All validations error will be inserted on {@link ValidationContext#addAlert(String)}. <br>
	 * 
	 */
	public void checkForFieldDataTypes() {

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
						fieldValue = checkBooleanValue(field.getFieldName(), fieldValue);

					if (FieldType.CHARACTER.equals(field.getFieldType()) || FieldType.DOMAIN.equals(field.getFieldType()) )
						fieldValue = checkCharacterValue(field, fieldValue);

					if (FieldType.DATE.equals(field.getFieldType()))
						fieldValue = checkDateValue(field.getFieldName(), fieldValue);

					if (FieldType.DECIMAL.equals(field.getFieldType()))
						fieldValue = checkDecimalValue(field.getFieldName(), fieldValue);

					if (FieldType.GENERIC.equals(field.getFieldType()))
						fieldValue = checkGenericValue(field, fieldValue);

					if (FieldType.INTEGER.equals(field.getFieldType()))
						fieldValue = checkIntegerValue(field.getFieldName(), fieldValue);

					if (FieldType.MONTH.equals(field.getFieldType()))
						fieldValue = checkMonthValue(field.getFieldName(), fieldValue);

					if (FieldType.TIMESTAMP.equals(field.getFieldType()))
						fieldValue = checkTimestampValue(field.getFieldName(), fieldValue);

					// Update field value to it's new representation
					values.replace(field.getFieldName(), fieldValue);
				} catch (Exception e) {
					log.log(Level.SEVERE,"Error parsing record values.", e);
					e.printStackTrace();
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

		Integer maxLength = Math.min(field.getMaxLength(),value.length());

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

		Integer maxLength = Math.min(field.getMaxLength(),value.length());

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
	private Object checkTimestampValue(String fieldName, Object fieldValue) {
		if (fieldValue == null)
			return null;

		if ( fieldValue instanceof Date || fieldValue instanceof OffsetDateTime )
			return fieldValue;

		String value = ValidationContext.toString(fieldValue);

		if (isOnlyNumbers(value))
			return new Date(Long.parseLong(value));
		
		if ( isTimestamp(value) )
			return parseTimestamp(value);

		// TODO check other situations
		addLogError("{field.value.invalid(" + value + ")(" + fieldName + ")}");
		return value;
	}

	/**
	 * Validate and transform field value from {@link FieldType.MONTH}
	 * 
	 * @param fieldName
	 * @param fieldValue
	 * @return Validated and transformed field value
	 */
	private Object checkMonthValue(String fieldName, Object fieldValue) {

		if (fieldValue == null)
			return null;		
		
		if ( fieldValue instanceof Date ) {
			return StringUtils.formatMonth((Date)fieldValue);
		}
		
		if ( fieldValue instanceof OffsetDateTime ) {
			return StringUtils.formatMonth((OffsetDateTime)fieldValue);
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
		
		addLogError("{field.value.invalid(" + value + ")(" + fieldName + ")}");
		return value;		
		
	}

	/**
	 * Validate and transform field value from {@link FieldType.INTEGER}
	 * 
	 * @param fieldName
	 * @param fieldValue
	 * @return Validated and transformed field value
	 */
	private Object checkIntegerValue(String fieldName, Object fieldValue) {
		if (fieldValue == null)
			return null;

		if (fieldValue instanceof Number || fieldValue.getClass().isAssignableFrom(int.class))
			return fieldValue;

		String value = ValidationContext.toString(fieldValue);

		if (isInteger(value))
			return Long.parseLong(value);

		// TODO check other situations
		addLogError("{field.value.invalid(" + value + ")(" + fieldName + ")}");
		return value;

	}

	/**
	 * Validate and transform field value from {@link FieldType.DECIMAL}
	 * 
	 * @param fieldName
	 * @param fieldValue
	 * @return Validated and transformed field value
	 */
	private Object checkDecimalValue(String fieldName, Object fieldValue) {

		if (fieldValue == null)
			return null;
		
		if (fieldValue instanceof Number || fieldValue.getClass().isAssignableFrom(double.class))
			return fieldValue;

		String value = ValidationContext.toString(fieldValue);

		if (isDecimal(value))
			return Double.parseDouble(value);

		if (isDecimalWithComma(value))
			return Double.parseDouble(value.replace(".", "").replace(",", "."));

		// TODO check other situations
		addLogError("{field.value.invalid(" + value + ")(" + fieldName + ")}");
		return fieldValue;
	}

	/**
	 * Validate and transform field value from {@link FieldType.DATE}
	 *
	 * @param fieldName
	 * @param fieldValue
	 * @return Validated and transformed field value
	 */
	private Object checkDateValue(String fieldName, Object fieldValue) {

		if (fieldValue == null)
			return null;
		
		if ( fieldValue instanceof Date || fieldValue instanceof OffsetDateTime )
			return fieldValue;
		
		if ( fieldValue instanceof Number )
			return new Date(((Number)(fieldValue)).longValue());

		String value = ValidationContext.toString(fieldValue);

		if (isMDY(value))
			return parseMDY(value);
		if (isDMY(value))
			return parseDMY(value);
		if (isYMD(value))
			return parseYMD(value);
		// TODO check other situations
		addLogError("{field.value.invalid(" + value + ")(" + fieldName + ")}");
		return value;
	}

	/**
	 * Validate and transform field value from {@link FieldType.BOOLEAN}
	 * 
	 * @param fieldName
	 * @param fieldValue
	 * @return Validated and transformed field value
	 */
	private Object checkBooleanValue(String fieldName, Object fieldValue) {
		if (fieldValue == null)
			return null;

		if (fieldValue instanceof Boolean || fieldValue.getClass().isAssignableFrom(boolean.class))
			return fieldValue;

		String value = ValidationContext.toString(fieldValue);

		// TODO check other situations
		if (isBoolean(value))
			return Boolean.parseBoolean(value);

		addLogError("{field.value.invalid(" + value + ")(" + fieldName + ")}");
		return fieldValue;

	}

	/**
	 * Check if values provided on fields that points to a domain table are present
	 * on domain tables entries. <br>
	 * 
	 * All validations error will be inserted on {@link ValidationContext#addAlert(String)}. <br>
	 */
	public void checkForDomainTableValues() {

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
							field.getDomainTableVersion() + ")}");
					continue;
				}

				// Check field value against domain table entries
				// If value is not present on table, add an error message
				Pair<Boolean, String> result = checkDomainValue(fieldValue, table);

				// If value is not present at domain table entries, add error message
				if (!result.getKey()) {
					addLogError("{field.domain.value.not.found(" + fieldValue + ")(" + field.getFieldName() + ")}");
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
		
		Map<String,Object> record = parsedContents.get(0);

		String[] fields =  { "TaxPayerId", "TaxPeriod", "TaxYear", "TaxMonth", "TaxPeriodNumber", "TaxMonthNumber" };
		
		for ( String field : fields ) {
			
			Map.Entry<String,Object> found = record.entrySet().stream().filter(entry->entry.getKey().equalsIgnoreCase(field)).findFirst().orElse(null);
			
			if ( found != null ) {
				
				if ( found.getValue() == null )
					continue;
				
				if ( "TaxPayerId".equals(field) ) {
					doc.setTaxPayerId(ValidationContext.toString(found.getValue()));
				}
				else if ( "TaxYear".equals(field) ) {
					Integer value = ParserUtils.parseIntegerNE(ValidationContext.toString(found.getValue()));
					if ( value != null ) {
						doc.setTaxYear(value);
						doc.setTaxPeriodNumber(value);
					}
				}
				else if ( "TaxMonth".equals(field) ) {
					doc.setTaxMonth(ValidationContext.toString(found.getValue()));	
				}
				else if ( "TaxPeriodNumber".equals(field) ) {
					Integer value = ParserUtils.parseIntegerNE(ValidationContext.toString(found.getValue()));
					if ( value != null ) {
						doc.setTaxPeriodNumber(value);	
					}
				}				
				else if ( "TaxMonthNumber".equals(field) ) {
					Integer value = ParserUtils.parseIntegerNE(ValidationContext.toString(found.getValue()));
					if ( value != null ) {
						doc.setTaxMonthNumber(value);
						doc.setTaxPeriodNumber(value);
					}
				}				
				
			}
			
		}
		
		//If TaxPayerId wasn't found, add an error message
		if ( doc.getTaxPayerId() == null ) {
			addLogError("{taxpayerid.not.found}");
		}
		
		//If TaxPeriod wasn't found, add an error message
		if ( doc.getTaxYear() == null && doc.getTaxMonth() == null && doc.getTaxMonthNumber() == null &&
				doc.getTaxPeriod() == null && doc.getTaxPeriodNumber() == null ) {
			addLogError("{taxperiod.not.found}");	
		}
		
	}

}
