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
package org.idb.cacao.web.entities;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Enumeration of field mappings that have special meaning for some CACAO functionalities.<BR>
 * All built-in generic field mapping options applicable to any context in tax administration
 * 
 * @author Gustavo Figueiredo
 *
 */
public enum FieldMapping implements Comparable<FieldMapping> {

	ANY("field.type.any"),
	TAXPAYER_ID("field.type.tpid", /*required*/true), 	
	TAX_YEAR("field.type.tyear", /*required*/true),		
	TAX_SEMESTER("field.type.tsemester", /*required*/true),	
	TAX_MONTH("field.type.tmonth", /*required*/true),	
	TAX_DAY("field.type.tday", /*required*/true),		
	TAX_VALUE("field.type.tvalue"),						  
	TAX_CODE("field.type.tcode", /*required*/true),		
	TAX_TYPE("field.type.tax", /*required*/true);		

	private final String display;
	
	/**
	 * 'required' is some field that, when present on template, must also be present and not zero
	 * on uploaded file
	 */
	private final boolean required;

	FieldMapping(String display, boolean required) {
		this.display = display;
		this.required = required;
	}
	
	FieldMapping(String display) {
		this(display, /*required*/false);
	}

	public boolean isRequired() {
		return required;
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
