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
 * Enumeration of field types for validation of incoming files.<BR>
 * 
 * @author Gustavo Figueiredo
 *
 */
public enum FieldType implements Comparable<FieldType> {

	GENERIC("field.type.generic"), // generic means anything (no validation at all)
	CHARACTER("field.type.char"),	
	INTEGER("field.type.int"), 	// any type of integer (tiny, short, integer or long)
	DECIMAL("field.type.decimal"),	// any type of decimal (float, double, decimal)	
	BOOLEAN("field.type.bool"),	
	TIMESTAMP("field.type.timestamp"),		
	DATE("field.type.date"),						  
	MONTH("field.type.month"),		
	DOMAIN("field.type.domain"),
	NESTED("field.type.nested");
	
	private final String display;
	
	FieldType(String display) {
		this.display = display;
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
