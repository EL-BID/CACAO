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

import static org.springframework.data.elasticsearch.annotations.FieldType.Boolean;
import static org.springframework.data.elasticsearch.annotations.FieldType.Date;
import static org.springframework.data.elasticsearch.annotations.FieldType.Keyword;
import static org.springframework.data.elasticsearch.annotations.FieldType.Nested;
import static org.springframework.data.elasticsearch.annotations.FieldType.Text;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.idb.cacao.api.Periodicity;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * This entity represents any generic structure modeled inside CACAO by the tax administration.<BR>
 * <BR>
 * It is a template with fields definitions (i.e.: contains fields types and fields names).<BR>
 * <BR>
 * @author Gustavo Figueiredo
 *
 */
@Document(indexName="cacao_templates")
public class DocumentTemplate implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;

	/**
	 * ID's are being generated automatically.
	 * PS: Elasticsearch generates by default 20 character long ID's, that are both URL-safe, base 64 encoded GUID
	 */
	@Id   
	private String id;
	
	/**
	 * The TemplateArchetype from where this DocumentTemplate was generated
	 */
	@MultiField(
		mainField = @Field(type=Text, fielddata=true),
		otherFields = {
			@InnerField(suffix = "keyword", type=Keyword)
		}
	)
	private String archetype;

	/**
	 * Name of this template. Name and version must be unique in the system (for DocumentTemplates).
	 */
	@MultiField(
		mainField = @Field(type=Text, fielddata=true),
		otherFields = {
			@InnerField(suffix = "keyword", type=Keyword)
		}
	)
	@NotBlank
	@NotNull
	@NotEmpty
	@Size(min=2, max=120)
	private String name;

	/**
	 * Optional group of this template. All templates belonging to the same group are considered
	 * together for validation and ETL purpose.
	 */
	@MultiField(
		mainField = @Field(type=Text, fielddata=true),
		otherFields = {
			@InnerField(suffix = "keyword", type=Keyword)
		}
	)
	private String group;

	/**
	 * Version of this template (in case it's necessary to keep a history of
	 * different versions of the same template over time). Name and version must be unique in the system (for DocumentTemplates).
	 */
	@MultiField(
		mainField = @Field(type=Text, fielddata=true),
		otherFields = {
			@InnerField(suffix = "keyword", type=Keyword)
		}
	)
	@NotBlank
	@NotNull
	@NotEmpty
	@Size(min=1, max=20)
	private String version;
	
	/**
	 * Date/time the template was created
	 */
	@Field(type=Date, store = true, format = DateFormat.date_time)
    private OffsetDateTime templateCreateTime;

	/**
	 * Date/time of last modification or creation of any part of this object
	 */
	@Field(type=Date, store = true, format = DateFormat.date_time)
	private OffsetDateTime changedTime;
	
	@Enumerated(EnumType.STRING)
	@MultiField(
			mainField = @Field(type=Text, fielddata=true),
			otherFields = {
				@InnerField(suffix = "keyword", type=Keyword)
			}
		)
	private Periodicity periodicity;

	/**
	 * If this template is part of a group of templates, informs this is a required file (the other
	 * ones without this indication are considered optional)
	 */
	@Field(type=Boolean)
	private Boolean required;
	
	@Field(type=Nested)
	private List<DocumentField> fields;
	
	@Field(type=Nested)
	private List<DocumentInput> inputs;
	
	/**
	 * Unique identifier of this template (20 character long, URL-safe, base 64 encoded GUID)
	 */
	public String getId() {
		return id;
	}

	/**
	 * Unique identifier of this template (20 character long, URL-safe, base 64 encoded GUID)
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * The TemplateArchetype from where this DocumentTemplate was generated
	 */
	public String getArchetype() {
		return archetype;
	}

	/**
	 * The TemplateArchetype from where this DocumentTemplate was generated
	 */
	public void setArchetype(String archetype) {
		this.archetype = archetype;
	}

	/**
	 * Name of this template
	 */
	public String getName() {
		return name;
	}

	/**
	 * Name of this template
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Optional group of this template. All templates belonging to the same group are considered
	 * together for validation and ETL purpose.
	 */
	public String getGroup() {
		return group;
	}

	/**
	 * Optional group of this template. All templates belonging to the same group are considered
	 * together for validation and ETL purpose.
	 */
	public void setGroup(String group) {
		this.group = group;
	}

	/**
	 * Version of this template (in case it's necessary to keep a history of
	 * different versions of the same template over time)
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Version of this template (in case it's necessary to keep a history of
	 * different versions of the same template over time)
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	public Periodicity getPeriodicity() {
		if (periodicity==null)
			return Periodicity.UNKNOWN;
		return periodicity;
	}

	public void setPeriodicity(Periodicity periodicity) {
		this.periodicity = periodicity;
	}

	/**
	 * If this template is part of a group of templates, informs this is a required file (the other
	 * ones without this indication are considered optional)
	 */
	public Boolean getRequired() {
		return required;
	}

	/**
	 * If this template is part of a group of templates, informs this is a required file (the other
	 * ones without this indication are considered optional)
	 */
	public void setRequired(Boolean required) {
		this.required = required;
	}

	/**
	 * Date/time the template was created
	 */
	public OffsetDateTime getTemplateCreateTime() {
		return templateCreateTime;
	}

	/**
	 * Date/time the template was created
	 */
	public void setTemplateCreateTime(OffsetDateTime templateCreateTime) {
		this.templateCreateTime = templateCreateTime;
	}

	/**
	 * Date/time of last modification or creation of any part of this object
	 */
	public OffsetDateTime getChangedTime() {
		return changedTime;
	}

	/**
	 * Date/time of last modification or creation of any part of this object
	 */
	public void setChangedTime(OffsetDateTime changedTime) {
		this.changedTime = changedTime;
	}

	public List<DocumentField> getFields() {
		return fields;
	}
	
	/**
	 * Find a field with the given name
	 */
	public DocumentField getField(String name) {
		if (fields==null)
			return null;
		return fields.stream().filter(f->name.equalsIgnoreCase(f.getFieldName())).findAny().orElse(null);
	}
	
	/**
	 * Find fields with the given name, ignoring array indexes, if any.
	 */
	@JsonIgnore
	public List<DocumentField> getFieldIgnoringArrayIndex(String name) {
		if (fields==null)
			return Collections.emptyList();
		return fields.stream().filter(f->name.equalsIgnoreCase(f.getFieldNameIgnoringArrayIndex())).collect(Collectors.toList());
	}

	/**
	 * Returns all the 'assigned' fields in this template (i.e. all fields with 'fieldType' different of 'ANY')
	 */
	@JsonIgnore
	public List<DocumentField> getAssignedFields() {
		if (fields==null)
			return Collections.emptyList();
		return fields.stream().filter(DocumentField::isAssigned).collect(Collectors.toList());
	}
	
	/**
	 * Returns all the 'assigned' fields in this template of a given type
	 */
	@JsonIgnore
	public List<DocumentField> getFieldsOfType(FieldMapping type) {
		if (fields==null)
			return Collections.emptyList();
		return fields.stream().filter(f->type.equals(f.getFieldMapping())).collect(Collectors.toList());		
	}

	/**
	 * Returns all the 'required' fields in this template
	 */
	@JsonIgnore
	public List<DocumentField> getRequiredFields() {
		if (fields==null)
			return Collections.emptyList();
		return fields.stream().filter(f->java.lang.Boolean.TRUE.equals(f.getRequired())).collect(Collectors.toList());		
	}

	/**
	 * Returns all the 'personal data' fields in this template
	 */
	@JsonIgnore
	public List<DocumentField> getPersonalDataFields() {
		if (fields==null)
			return Collections.emptyList();
		return fields.stream().filter(f->java.lang.Boolean.TRUE.equals(f.getPersonalData())).collect(Collectors.toList());		
	}

	/**
	 * Returns all the 'file uniqueness' fields in this template. The fields are sorted by ID.
	 */
	@JsonIgnore
	public List<DocumentField> getFileUniquenessFields() {
		if (fields==null)
			return Collections.emptyList();
		return fields.stream().filter(f->java.lang.Boolean.TRUE.equals(f.getFileUniqueness()))
				.sorted(Comparator.comparing(DocumentField::getId))
				.collect(Collectors.toList());		
	}

	public void setFields(List<DocumentField> fields) {
		this.fields = fields;
		if (fields!=null && !fields.isEmpty()) {
			for (DocumentField field: fields) {
				if (field.getId()==0)
					field.setId(getNextUnassignedFieldId());
			}
		}
	}
	
	public void clearFields() {
		if (fields!=null)
			fields.clear();
	}
	
	public void addField(DocumentField field) {
		if (fields==null)
			fields = new LinkedList<>();
		fields.add(field);
		field.setId(getNextUnassignedFieldId());
	}
	
	public void addField(String name) {
		addField(new DocumentField(name));
	}
	
	public void removeField(DocumentField field) {
		if (fields==null)
			return;
		fields.remove(field);
		field.setId(0);
	}
	
	public void sortFields() {
		if (fields==null || fields.size()<2)
			return;
		Collections.sort(fields);
	}
	
	@JsonIgnore
	public int getNumAssignedFields() {
		if (fields==null)
			return 0;
		return (int)fields.stream().filter(DocumentField::isAssigned).count();
	}

	@JsonIgnore
	public int getNumTotalFields() {
		if (fields==null)
			return 0;
		else
			return fields.size();
	}
	
	@JsonIgnore
	public int getNextUnassignedFieldId() {
		if (fields==null || fields.isEmpty())
			return 1;
		return 1 + fields.stream().mapToInt(DocumentField::getId).max().orElse(0);
	}

	public List<DocumentInput> getInputs() {
		return inputs;
	}
	
	/**
	 * Find a DocumentInput with the given name
	 */
	public DocumentInput getInput(String name) {
		if (inputs==null)
			return null;
		return inputs.stream().filter(f->name.equalsIgnoreCase(f.getInputName())).findAny().orElse(null);
	}
	
	/**
	 * Returns DocumentInput associated to this template with a given DocumentFormat
	 */
	@JsonIgnore
	public DocumentInput getInputOfFormat(DocumentFormat format) {
		if (inputs==null)
			return null;
		return inputs.stream().filter(f->format.equals(f.getFormat())).findAny().orElse(null);		
	}

	public void setInputs(List<DocumentInput> inputs) {
		this.inputs = inputs;
	}
	
	public void clearInputs() {
		if (inputs!=null)
			inputs.clear();
	}
	
	public void addInput(DocumentInput input) {
		if (inputs==null)
			inputs = new LinkedList<>();
		inputs.add(input);
		input.setId(getNextUnassignedInputId());
	}
	
	public void addInput(String name) {
		addInput(new DocumentInput(name));
	}
	
	public void removeInput(DocumentInput input) {
		if (inputs==null)
			return;
		inputs.remove(input);
		input.setId(0);
	}
	
	public void sortInputs() {
		if (inputs==null || inputs.size()<2)
			return;
		Collections.sort(inputs);
	}
	
	@JsonIgnore
	public int getNumTotalInputs() {
		if (inputs==null)
			return 0;
		else
			return inputs.size();
	}
	
	@JsonIgnore
	public int getNextUnassignedInputId() {
		if (inputs==null || inputs.isEmpty())
			return 1;
		return 1 + inputs.stream().mapToInt(DocumentInput::getId).max().orElse(0);
	}

	public DocumentTemplate clone() {
		try {
			return (DocumentTemplate)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

    @Override
    public String toString() {
        return "DocumentTemplate{" + "id=" + id + ", name=" + name +", version=" + version + '}';
    }

    /**
     * Put DocumentTemplate instances in reverse chronological order according to their timestamps
     */
    public static final Comparator<DocumentTemplate> TIMESTAMP_COMPARATOR = new Comparator<DocumentTemplate>() {

		@Override
		public int compare(DocumentTemplate o1, DocumentTemplate o2) {
			OffsetDateTime d1 = o1.getTemplateCreateTime();
			OffsetDateTime d2 = o2.getTemplateCreateTime();
			if (d1!=d2) {
				if (d1==null)
					return 1;
				if (d2==null)
					return -1;
				int comp = d1.compareTo(d2);
				if (comp!=0)
					return - comp;
			}
			return 0;
		}
    	
    };
}
