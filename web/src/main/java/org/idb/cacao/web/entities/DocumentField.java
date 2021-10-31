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

import static org.springframework.data.elasticsearch.annotations.FieldType.*;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import org.springframework.data.elasticsearch.annotations.Field;

/**
 * Recognized fields for a document template. This is related to a 'DocumentTemplate'
 * (one DocumentTemplate refers to multiple DocumentField's).<BR>
 * 
 * @author Gustavo Figueiredo
 *
 */
public class DocumentField implements Serializable, Cloneable, Comparable<DocumentField> {

	private static final long serialVersionUID = 1L;
	
	public static final String GROUP_SEPARATOR = "|";
	
	public static final Pattern PATTERN_ARRAY_INDEX = Pattern.compile("\\[(\\d+)\\]$");
	
	@Field(type=Integer)
	private int id;

	/**
	 * Identification of this field inside a document. The field name should be unique inside
	 * the same document. Array elements should include positional suffixes (e.g.: [1])<BR>
	 * Grouping information should be separated with pipes.
	 */
	@Field(type=Text)
	private String fieldName;

	@Field(type=Text)
	private String sampleValue;
	
	@Enumerated(EnumType.STRING)
	@Field(type=Text)
	private FieldMapping fieldType;
	
	public DocumentField() { }

	public DocumentField(String fieldName) {
		this.fieldName = fieldName;
	}

	public DocumentField(String fieldName, String sampleValue) {
		this.fieldName = fieldName;
		this.sampleValue = sampleValue;
	}

	public DocumentField(String fieldName, FieldMapping fieldType) {
		this.fieldName = fieldName;
		this.fieldType = fieldType;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Identification of this field inside a document. The field name should be unique inside
	 * the same document. Array elements should include positional suffixes (e.g.: [1])<BR>
	 * Grouping information should be separated with pipes.
	 */
	public String getFieldName() {
		return fieldName;
	}
	
	/**
	 * The same as {@link #getFieldName() getFieldName}, but removes array indexes if they are included
	 * as part of field name.
	 */
	public String getFieldNameIgnoringArrayIndex() {
		return getFieldNameIgnoringArrayIndex(fieldName);
	}
	
	public static String getFieldNameIgnoringArrayIndex(String fieldName) {
		if (fieldName==null)
			return null;
		Matcher mArrayIndex = PATTERN_ARRAY_INDEX.matcher(fieldName);
		if (mArrayIndex.find()) {
			return fieldName.substring(0, mArrayIndex.start());
		}
		else {
			return fieldName;
		}
	}
	
	/**
	 * Returns indication that the provided field name has a index number
	 */
	public static boolean hasArrayIndex(String fieldName) {
		return PATTERN_ARRAY_INDEX.matcher(fieldName).find();
	}

	/**
	 * Identification of this field inside a document. The field name should be unique inside
	 * the same document. Array elements should include positional suffixes (e.g.: [1])<BR>
	 * Grouping information should be separated with pipes.
	 */
	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}
	
	public DocumentField withFieldName(String fieldName) {
		setFieldName(fieldName);
		return this;
	}
	
	public String getGroup() {
		if (fieldName==null || fieldName.trim().length()==0)
			return null;
		int sep = fieldName.lastIndexOf('|');
		if (sep<0)
			return null;
		return fieldName.substring(0, sep);
	}
	
	public String getSimpleFieldName() {
		if (fieldName==null || fieldName.trim().length()==0)
			return fieldName;
		int sep = fieldName.lastIndexOf('|');
		if (sep<0)
			return fieldName;
		return fieldName.substring(sep+1);		
	}

	public String getSampleValue() {
		return sampleValue;
	}

	public void setSampleValue(String sampleValue) {
		this.sampleValue = sampleValue;
	}

	public FieldMapping getFieldType() {
		if (fieldType==null)
			return FieldMapping.ANY;
		return fieldType;
	}

	public void setFieldType(FieldMapping fieldType) {
		this.fieldType = fieldType;
	}
	
	public boolean isAssigned() {
		return fieldType!=null && !FieldMapping.ANY.equals(fieldType);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof DocumentField))
			return false;
		return id==((DocumentField)o).id;
	}
	
	@Override
	public int hashCode() {
		return 17 + (int) ( 37 * id );
	}

	public DocumentField clone() {
		try {
			return (DocumentField)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

    @Override
    public String toString() {
        return "DocumentField{fieldName=" + fieldName + '}';
    }

	@Override
	public int compareTo(DocumentField o) {
		if (fieldName==null)
			return -1;
		if (o.fieldName==null)
			return 1;
		return String.CASE_INSENSITIVE_ORDER.compare(fieldName, o.fieldName);
	}

}
