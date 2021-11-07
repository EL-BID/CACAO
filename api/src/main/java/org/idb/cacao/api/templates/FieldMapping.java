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
package org.idb.cacao.api.templates;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Enumeration of field mappings that have special meaning for some CACAO functionalities.<BR>
 * 
 * @author Gustavo Figueiredo
 *
 */
public enum FieldMapping implements Comparable<FieldMapping> {

	ANY("field.map.any"),
	
	// All built-in generic field mapping options applicable to any context in tax administration
	TAXPAYER_ID("field.map.tpid"), 	
	TAX_YEAR("field.map.tyear"),		
	TAX_SEMESTER("field.map.tsemester"),	
	TAX_MONTH("field.map.tmonth"),	
	TAX_DAY("field.map.tday"),		
	TAX_VALUE("field.map.tvalue"),						  
	TAX_CODE("field.map.tcode"),		
	TAX_TYPE("field.map.tax");

	private final String display;
	
	FieldMapping(String display) {
		this.display = display;
	}
	
	@Override
	public String toString() {
		return display;
	}
	
	public static FieldMapping parse(String s) {
		if (s==null || s.trim().length()==0)
			return null;
		return Arrays.stream(values()).filter(t->t.name().equalsIgnoreCase(s)).findAny().orElse(null);
	}
	
	public static FieldMapping[] ordered() {
		return Arrays.stream(values()).sorted(Comparator.comparing(FieldMapping::name)).toArray(FieldMapping[]::new);
	}
}
