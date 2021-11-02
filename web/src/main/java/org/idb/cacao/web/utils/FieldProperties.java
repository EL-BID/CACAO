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
package org.idb.cacao.web.utils;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * A representation of an entity field with properties
 *  
 * @author Rivelino Patrício
 *
 * @since 02/11/2021
 */
public class FieldProperties {
	
	/**
	 * Field name as defined on entity definition
	 */
	private String name;
	
	/**
	 * Field type, example: String, int, double, etc.
	 */
	private String type;
	
	/**
	 * Description displayed on tables, forms and reports
	 */
	private String externalName;
	
	/**
	 * Field width in pixels
	 */
	private int width;
	
	/**
	 * Field alignment on tables, forms and reports.
	 */
	private int alignment;
	
	/**
	 * An indication if the field can be editable
	 */
	private boolean editable;
	
	/**
	 * A tooltipo to show on UI
	 */
	private String tooltip;
	
	/**
	 * An indication if the field can be null or not
	 */
	private boolean notNull;
	
	/**
	 * An indication if the field can be blank or not
	 */
	private boolean notBlank;
	
	/**
	 * Indicates the minimal decimals for the field
	 */
	private int minDecimals;
	
	/**
	 * Indicates the maximum decimals for the field
	 */
	private int maxDecimals;
	
	/**
	 * Indicates if the field represents an email address
	 */
	private boolean email;
	
	/**
	 * Minimal length for field contents
	 */
	private int minLength;
	
	/**
	 * Maximum length for field contents
	 */
	private int maxLength;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getExternalName() {
		return externalName;
	}

	public void setExternalName(String externalName) {
		this.externalName = externalName;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getAlignment() {
		return alignment;
	}

	public void setAlignment(int alignment) {
		this.alignment = alignment;
	}

	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	public String getTooltip() {
		return tooltip;
	}

	public void setTooltip(String tooltip) {
		this.tooltip = tooltip;
	}

	public boolean isNotNull() {
		return notNull;
	}

	public void setNotNull(boolean notNull) {
		this.notNull = notNull;
	}

	public boolean isNotBlank() {
		return notBlank;
	}

	public void setNotBlank(boolean notBlank) {
		this.notBlank = notBlank;
	}

	public int getMinDecimals() {
		return minDecimals;
	}

	public void setMinDecimals(int minDecimals) {
		this.minDecimals = minDecimals;
	}

	public int getMaxDecimals() {
		return maxDecimals;
	}

	public void setMaxDecimals(int maxDecimals) {
		this.maxDecimals = maxDecimals;
	}

	public boolean isEmail() {
		return email;
	}

	public void setEmail(boolean email) {
		this.email = email;
	}

	public int getMinLength() {
		return minLength;
	}

	public void setMinLength(int minLength) {
		this.minLength = minLength;
	}

	public int getMaxLength() {
		return maxLength;
	}

	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
	}
	
	/**
	 * Creates and returns an instance of {@link FieldProperties} for the given {@link Field}
	 * @param field	A {@link Field} to be parsed
	 * @return	An {@link FieldProperties} object
	 */
	public FieldProperties toFieldProperties(Field field) {
		return null;
	}
	
	/**
	 * Creates and returns a {@link Map} of {@link FieldProperties} for a given {@link Class}.
	 * Field name will be used as {@link Map} key.
	 * All fields, except static and transients, will be parsed and returned.
	 * @param clazz	A {@link Class} to be parsed.
	 * @return	A {@link Map} with {@link FieldProperties} for all fields in {@link Class}
	 */
	public Map<String,FieldProperties> toFieldProperties(Class<?> clazz) {
		return null;
	}

}
