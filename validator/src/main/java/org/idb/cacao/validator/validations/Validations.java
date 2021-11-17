/**
 * 
 */
package org.idb.cacao.validator.validations;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.FieldType;

/**
 * A set of utilities for file content validation purpose. 
 * 
 * @author Rivelino Patrício
 * 
 * @since 17/11/2021
 *
 */
public class Validations {
	
	private static final String FIELD_VALUE_NOT_FOUND = "Field value not found";

	/**
	 * Check for required fields in document uploaded
	 * 
	 * All validations error will be inserted on {@link ValidationContext#addAlert(String)}
	 * 
	 * @param validationContext
	 * 
	 */
	public static void checkForRequiredFields(ValidationContext validationContext) {
		
		//Get a list of required fields
		List<String> requiredFields = validationContext.getDocumentTemplate().getFields().stream().
				filter(d->d.getRequired()).map(d->d.getFieldName()).collect(Collectors.toList());
		
		if ( requiredFields == null || requiredFields.isEmpty() )
			return;
		
		List<Map<String,Object>> parsedContents = validationContext.getParsedContents();
		
		if ( parsedContents == null || parsedContents.isEmpty() )
			return;
		
		parsedContents.parallelStream().iterator().forEachRemaining(values-> {
			
			for( String fieldName : requiredFields ) {
				if ( values.get(fieldName) == null )
					addLogError(validationContext, fieldName, FIELD_VALUE_NOT_FOUND);							
			}
			
		});		 
		
	}

	private static void addLogError(ValidationContext validationContext, String field, String message) {
		validationContext.addAlert(message + ":" + field);
	}

	/**
	 * Check for data types in all fields in document uploaded 
	 * 
	 * @param validationContext
	 * @return	Boolean.TRUE if all data are compatible with field types. Boolean.FALSE if not.
	 */
	public static void checkForFieldDataTypes(ValidationContext validationContext) {
		
		//Get a list of fields
		List<DocumentField> fields = validationContext.getDocumentTemplate().getFields();
		
		if ( fields == null || fields.isEmpty() )
			return;
		
		//Remove CHARACTER fields from validation
		fields.iterator().forEachRemaining(field->{
			if ( FieldType.CHARACTER.equals(field.getFieldType()) || FieldType.GENERIC.equals(field.getFieldType()) ||
					FieldType.DOMAIN.equals(field.getFieldType()) || FieldType.NESTED.equals(field.getFieldType()) ) {
				fields.remove(field);
			}					
		});
		
		List<Map<String,Object>> parsedContents = validationContext.getParsedContents();
		
		if ( parsedContents == null || parsedContents.isEmpty() )
			return;
		
		parsedContents.parallelStream().iterator().forEachRemaining(values-> {
		
			for ( DocumentField field : fields ) {
				
				Object fieldValue = values.get(field.getFieldName());
				
				//If field value is null, there is nothing to check
				if ( fieldValue == null )
					continue;

				if ( FieldType.BOOLEAN.equals(field.getFieldType()) )
					fieldValue = checkBooleanValue(fieldValue, validationContext);
				
				if ( FieldType.DATE.equals(field.getFieldType()) )
					fieldValue = checkDateValue(fieldValue, validationContext);
				
				if ( FieldType.DECIMAL.equals(field.getFieldType()) )
					fieldValue = checkDecimalValue(fieldValue, validationContext);
				
				if ( FieldType.INTEGER.equals(field.getFieldType()) )
					fieldValue = checkIntegerValue(fieldValue, validationContext);
				
				if ( FieldType.MONTH.equals(field.getFieldType()) )
					fieldValue = checkMonthValue(fieldValue, validationContext);
				
				if ( FieldType.TIMESTAMP.equals(field.getFieldType()) )
					fieldValue = checkTimestampValue(fieldValue, validationContext);
			
				//Update field value to it's new representation
				values.replace(field.getFieldName(), fieldValue, validationContext);
				
			}
			
		});
	}

	private static Object checkTimestampValue(Object fieldValue, ValidationContext validationContext) {
		// TODO Auto-generated method stub
		return fieldValue;
	}

	private static Object checkMonthValue(Object fieldValue, ValidationContext validationContext) {
		// TODO Auto-generated method stub
		return fieldValue;
	}

	private static Object checkIntegerValue(Object fieldValue, ValidationContext validationContext) {
		// TODO Auto-generated method stub
		return fieldValue;
	}

	private static Object checkDecimalValue(Object fieldValue, ValidationContext validationContext) {
		// TODO Auto-generated method stub
		return fieldValue;
	}

	private static Object checkDateValue(Object fieldValue, ValidationContext validationContext) {
		// TODO Auto-generated method stub
		return fieldValue;
	}

	private static Object checkBooleanValue(Object fieldValue, ValidationContext validationContext) {
		// TODO Auto-generated method stub
		return fieldValue;
	}

	public static void checkForDomainTableValues(ValidationContext validationContext) {
		// TODO Auto-generated method stub
	}

}
