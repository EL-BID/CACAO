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

import static org.springframework.data.elasticsearch.annotations.FieldType.Integer;
import static org.springframework.data.elasticsearch.annotations.FieldType.Nested;
import static org.springframework.data.elasticsearch.annotations.FieldType.Text;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.springframework.data.elasticsearch.annotations.Field;

/**
 * Possible choices for 'input' of a DocumentTemplate.<BR>
 * 
 * Each DocumentTemplate may accept different choices of DocumentInput. For example,
 * one DocumentTemplate may be related to different formats of XLS file and different
 * formats of CSV file.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class DocumentInput implements Serializable, Cloneable, Comparable<DocumentInput> {

	private static final long serialVersionUID = 1L;

	/**
	 * Sequence number of this input inside a particular DocumentTemplate
	 */
	@Field(type=Integer)
	private int id;

	/**
	 * Identification of this input inside a document. The input name should be unique for
	 * the same document template.
	 */
	@Field(type=Text)
	@NotBlank
	@NotNull
	@NotEmpty
	@Size(min=2, max=120)
	private String inputName;

	/**
	 * The file format related to this input option
	 */
	@Enumerated(EnumType.STRING)
	@Field(type=Text)
	@NotNull
	private DocumentFormat format;
	
	@Field(type=Nested)
	private List<DocumentInputFieldMapping> fields;
	
	public DocumentInput() {		
	}
	
	public DocumentInput(String name) {
		setInputName(name);
	}

	/**
	 * Sequence number of this input inside a particular DocumentTemplate
	 */
	public int getId() {
		return id;
	}

	/**
	 * Sequence number of this input inside a particular DocumentTemplate
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Identification of this input inside a document. The input name should be unique for
	 * the same document template.
	 */
	public String getInputName() {
		return inputName;
	}

	/**
	 * Identification of this input inside a document. The input name should be unique for
	 * the same document template.
	 */
	public void setInputName(String inputName) {
		this.inputName = inputName;
	}

	/**
	 * The file format related to this input option
	 */
	public DocumentFormat getFormat() {
		return format;
	}

	/**
	 * The file format related to this input option
	 */
	public void setFormat(DocumentFormat format) {
		this.format = format;
	}

	public List<DocumentInputFieldMapping> getFields() {
		return fields;
	}

	public void setFields(List<DocumentInputFieldMapping> fields) {
		if (fields==null) {
			this.fields = null;
			return;
		}
		fields.stream().forEach(f -> addField(f));
	}
	
	public void addField(DocumentInputFieldMapping field) {
		if (field==null)
			return;
		if (this.fields==null)
			this.fields = new LinkedList<>();
		this.fields.add(field);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof DocumentInput))
			return false;
		return id==((DocumentInput)o).id;
	}
	
	@Override
	public int hashCode() {
		return 17 + (int) ( 37 * id );
	}

	public DocumentInput clone() {
		try {
			return (DocumentInput)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

    @Override
    public String toString() {
        return "DocumentInput{inputName=" + inputName + '}';
    }

	@Override
	public int compareTo(DocumentInput o) {
		if (inputName==null)
			return -1;
		if (o.inputName==null)
			return 1;
		return String.CASE_INSENSITIVE_ORDER.compare(inputName, o.inputName);
	}

}
