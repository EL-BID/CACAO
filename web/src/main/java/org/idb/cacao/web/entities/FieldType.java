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
 * Enumeration of field types
 * 
 * @author Gustavo Figueiredo
 *
 */
public enum FieldType implements Comparable<FieldType> {

	ANY("field_type.any"),
	TAXPAYER_ID("field_type.tpid", /*required*/true), 	// every document should identify the taxpayer
	TAX_YEAR("field_type.tyear", /*required*/true),		// every document should identify the tax year
	TAX_SEMESTER("field_type.tsemester", /*required*/true),	// if template defines a 'tax semester', every document should have it
	TAX_MONTH("field_type.tmonth", /*required*/true),	// if template defines a 'tax month', every document should have it
	TAX_DAY("field_type.tday", /*required*/true),		// if template defines a 'tax day', every document should have it
	TAX_VALUE("field_type.tvalue"),						// the tax value may be zero if there is a tax credit  
	TAX_CODE("field_type.tcode", /*required*/true),		// if template defines a 'tax code', every document should have it
	TAX_TYPE("field_type.tax", /*required*/true),		// if template defines a 'tax name', every document should have it
	TAXPAYER_ID_OTHERS("field_type.tpid_others"),		// the 'other taxpayers' ids are optional and may vary in earch document
	RECTIFIER("field_type.rectifier");					// if template defines a 'rectifier' boolean field, it should be marked as TRUE for rectifier document

	private final String display;
	
	/**
	 * 'required' is some field that, when present on template, must also be present and not zero
	 * on uploaded file
	 */
	private final boolean required;

	FieldType(String display, boolean required) {
		this.display = display;
		this.required = required;
	}
	
	FieldType(String display) {
		this(display, /*required*/false);
	}

	public boolean isRequired() {
		return required;
	}

	@Override
	public String toString() {
		return display;
	}
	
	public static FieldType parse(String s) {
		if (s==null || s.trim().length()==0)
			return null;
		return Arrays.stream(values()).filter(t->t.name().equalsIgnoreCase(s)).findAny().orElse(null);
	}
	
	public static FieldType[] ordered() {
		return Arrays.stream(values()).sorted(Comparator.comparing(FieldType::name)).toArray(FieldType[]::new);
	}
}
