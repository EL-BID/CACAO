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
	
	/**
	 * Sequence number of this field inside a particular DocumentTemplate
	 */
	@Field(type=Integer)
	private int id;

	/**
	 * Identification of this field inside a document. The field name should be unique inside
	 * the same document. Array elements should include positional suffixes (e.g.: [1])<BR>
	 * Grouping information should be separated with pipes.
	 */
	@Field(type=Text)
	private String fieldName;

	/**
	 * Field mapping in case this field has a special meaning for specific CACAO features
	 */
	@Enumerated(EnumType.STRING)
	@Field(type=Text)
	private FieldMapping fieldMapping;
	
	/**
	 * Field type (for validating incoming files accordingly)
	 */
	@Enumerated(EnumType.STRING)
	@Field(type=Text)
	private FieldType fieldType;
	
	/**
	 * Maximum field length (only applies to Text fields)
	 */
	@Field(type=Integer)
	private Integer maxLength;
	
	/**
	 * Optional description of this field for documentation purpose
	 */
	@Field(type=Text)
	private String description;
	
	/**
	 * Indicates this field corresponds to 'personal data' (according to the Information privacy legislation)
	 */
	@Field(type=Boolean)
	private Boolean personalData;
	
	public DocumentField() { }

	public DocumentField(String fieldName) {
		this.fieldName = fieldName;
	}

	public DocumentField(String fieldName, FieldMapping fieldMapping) {
		this.fieldName = fieldName;
		this.fieldMapping = fieldMapping;
	}

	public DocumentField(String fieldName, FieldType fieldType) {
		this.fieldName = fieldName;
		this.fieldType = fieldType;
	}

	/**
	 * Sequence number of this field inside a particular DocumentTemplate
	 */
	public int getId() {
		return id;
	}

	/**
	 * Sequence number of this field inside a particular DocumentTemplate
	 */
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

	/**
	 * Field mapping in case this field has a special meaning for specific CACAO features
	 */
	public FieldMapping getFieldMapping() {
		if (fieldMapping==null)
			return FieldMapping.ANY;
		return fieldMapping;
	}

	/**
	 * Field mapping in case this field has a special meaning for specific CACAO features
	 */
	public void setFieldMapping(FieldMapping fieldMapping) {
		this.fieldMapping = fieldMapping;
	}
	
	/**
	 * Field type (for validating incoming files accordingly)
	 */
	public FieldType getFieldType() {
		return fieldType;
	}

	/**
	 * Field type (for validating incoming files accordingly)
	 */
	public void setFieldType(FieldType fieldType) {
		this.fieldType = fieldType;
	}

	/**
	 * Maximum field length (only applies to Text fields)
	 */
	public Integer getMaxLength() {
		return maxLength;
	}

	/**
	 * Maximum field length (only applies to Text fields)
	 */
	public void setMaxLength(Integer maxLength) {
		this.maxLength = maxLength;
	}

	/**
	 * Optional description of this field for documentation purpose
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Optional description of this field for documentation purpose
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Indicates this field corresponds to 'personal data' (according to the Information privacy legislation)
	 */
	public Boolean getPersonalData() {
		return personalData;
	}

	/**
	 * Indicates this field corresponds to 'personal data' (according to the Information privacy legislation)
	 */
	public void setPersonalData(Boolean personalData) {
		this.personalData = personalData;
	}

	public boolean isAssigned() {
		return fieldMapping!=null && !FieldMapping.ANY.equals(fieldMapping);
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
