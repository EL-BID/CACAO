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
package org.idb.cacao.validator.parsers;

import java.util.HashMap;
import java.util.Map;

import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.templates.DocumentInput;
import org.idb.cacao.api.templates.DocumentInputFieldMapping;

/**
 * Generic parser for any tabulated data
 *  
 * @author Gustavo Figueiredo
 * 
 */
public class TabulatedData {

	/**
	 * Document with field specifications
	 */
	private final DocumentInput documentInputSpec;

	/**
	 * Field positions relative to column positions
	 */
	private Map<String,Integer> fieldPositions;
	
	public TabulatedData(DocumentInput documentInputSpec) {
		this.documentInputSpec = documentInputSpec;
	}

	/**
	 * Parse the first line of the tabulated data, supposedly containing column headers
	 */
	public void parseColumnNames(Object[] parts) {
		
		//Get original column positions
		Map<String,Integer> columnPositions = new HashMap<>();
		fieldPositions = new HashMap<>();
		
		for ( int i = 0; i < parts.length; i++ )
			columnPositions.put(ValidationContext.toString(parts[i]), i);
		
		//Check all field mappings and set it's corresponding column
		for ( DocumentInputFieldMapping fieldMapping : documentInputSpec.getFields() ) {
			
			if ( fieldMapping.getColumnIndex() != null && fieldMapping.getColumnIndex() >= 0 ) {
				fieldPositions.put(fieldMapping.getFieldName(), fieldMapping.getColumnIndex());
				continue;
			}
				
			String expression = fieldMapping.getColumnNameExpression();
			if ( expression != null && !expression.isEmpty() ) {							
				Integer position = ValidationContext.matchExpression(columnPositions.entrySet(), Map.Entry::getKey, expression).map(Map.Entry::getValue).orElse(null);
				if ( position != null )
					fieldPositions.put(fieldMapping.getFieldName(), position);
			}					
			
		}

	}
	
	/**
	 * Parse one line of data regarding one record
	 */
	public Map<String,Object> parseLine(Object[] parts) {
		
		Map<String,Object> toRet = new HashMap<>();
		
		for ( DocumentInputFieldMapping fieldMapping : documentInputSpec.getFields() ) {
			
			int position =  fieldPositions.getOrDefault(fieldMapping.getFieldName(), -1);
			
			if ( position < 0 ) {
				toRet.put(fieldMapping.getFieldName(), null);
			}
			else {
				toRet.put(fieldMapping.getFieldName(), parts.length > position ? parts[position] : null);
			}
			
		}
		
		return toRet;
	}
}
