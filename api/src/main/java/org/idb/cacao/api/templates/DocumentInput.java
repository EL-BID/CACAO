/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.api.templates;

import static org.springframework.data.elasticsearch.annotations.FieldType.Boolean;
import static org.springframework.data.elasticsearch.annotations.FieldType.Integer;
import static org.springframework.data.elasticsearch.annotations.FieldType.Keyword;
import static org.springframework.data.elasticsearch.annotations.FieldType.Nested;
import static org.springframework.data.elasticsearch.annotations.FieldType.Text;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.idb.cacao.api.utils.StringUtils;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

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
	@MultiField(
			mainField = @Field(type=Text, fielddata=true),
			otherFields = {
				@InnerField(suffix = "keyword", type=Keyword)
			}
		)
	@NotNull
	private DocumentFormat format;
	
	@Field(type=Nested)
	private List<DocumentInputFieldMapping> fields;
	
	/**
	 * If this is TRUE, the validation phase will accept documents for which some lines may
	 * not fulfill all the 'required' fields. If this is FALSE (by default), the validation phase
	 * will reject documents containing lines with missing data for required fields (will declare those files as 'invalid')
	 */
	@Field(type=Boolean)
	private Boolean acceptIncompleteFiles;

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

	/**
	 * If this is TRUE, the validation phase will accept documents for which some lines may
	 * not fulfill all the 'required' fields. If this is FALSE (by default), the validation phase
	 * will reject documents containing lines with missing data for required fields (will declare those files as 'invalid')
	 */
	public Boolean getAcceptIncompleteFiles() {
		return acceptIncompleteFiles;
	}

	/**
	 * If this is TRUE, the validation phase will accept documents for which some lines may
	 * not fulfill all the 'required' fields. If this is FALSE (by default), the validation phase
	 * will reject documents containing lines with missing data for required fields (will declare those files as 'invalid')
	 */
	public void setAcceptIncompleteFiles(Boolean acceptIncompleteFiles) {
		this.acceptIncompleteFiles = acceptIncompleteFiles;
	}

	public List<DocumentInputFieldMapping> getFields() {
		return fields;
	}

	public void setFields(List<DocumentInputFieldMapping> fields) {
		this.fields = null;
		if (fields==null) {
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
	
	public void removeField(DocumentInputFieldMapping field) {
		if (fields==null)
			return;
		fields.remove(field);
		field.setFieldId(0);
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
		return StringUtils.compareCaseInsensitive(inputName, o.inputName);
	}

	/**
	 * Given a template with several DocumentField, updates this objects 'DocumentInputFieldMapping's id's
	 * with matching field names.
	 */
	public void setFieldsIdsMatchingTemplate(DocumentTemplate template) {
		if (template==null || template.getFields()==null || template.getFields().isEmpty()
			|| this.fields==null || this.fields.isEmpty())
			return;
		
		Map<String, Integer> mapFieldsNamesToIds = template.getFields().stream()
			.collect(Collectors.toMap(
				/*keyMapper*/DocumentField::getFieldName, 
				/*valueMapper*/DocumentField::getId, 
				/*mergeFunction*/(a,b)->a));
		
		for (DocumentInputFieldMapping fieldMap: this.fields) {
			if (fieldMap.getFieldName()==null)
				continue;
			Integer id = mapFieldsNamesToIds.get(fieldMap.getFieldName());
			if (id==null)
				continue;
			fieldMap.setFieldId(id);
		}
	}
}
