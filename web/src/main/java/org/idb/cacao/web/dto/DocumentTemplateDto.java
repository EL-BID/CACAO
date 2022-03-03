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
package org.idb.cacao.web.dto;

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.idb.cacao.api.Periodicity;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DocumentInput;
import org.idb.cacao.api.templates.DocumentTemplate;

/**
 * This represents a Document Transfer Object for a DocumentTemplate entity
 * @author Luis Kauer
 *
 */
public class DocumentTemplateDto implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * ID's are being generated automatically.
	 * PS: Elasticsearch generates by default 20 character long ID's, that are both URL-safe, base 64 encoded GUID
	 */
	private String id;
	
	/**
	 * The TemplateArchetype from where this DocumentTemplate was generated
	 */
	private String archetype;

	/**
	 * Name of this template. Name and version must be unique in the system (for DocumentTemplates).
	 */
	@NotBlank
	@NotNull
	@NotEmpty
	@Size(min=2, max=120)
	private String name;

	/**
	 * Optional group of this template. All templates belonging to the same group are considered
	 * together for validation and ETL purpose.
	 */
	private String group;

	/**
	 * Version of this template (in case it's necessary to keep a history of
	 * different versions of the same template over time). Name and version must be unique in the system (for DocumentTemplates).
	 */
	@NotBlank
	@NotNull
	@NotEmpty
	@Size(min=1, max=20)
	private String version;
	
	private Periodicity periodicity;

	/**
	 * If this template is part of a group of templates, informs this is a required file (the other
	 * ones without this indication are considered optional)
	 */
	private Boolean required;
	
	private List<DocumentField> fields;
	
	private List<DocumentInput> inputs;
	
	/**
	 * Indicates this document template is available for sending documents.
	 */
	private Boolean active;
	
	public DocumentTemplateDto() {
	}
	
	public DocumentTemplateDto(DocumentTemplate template) {
		this.id = template.getId();
		this.archetype = template.getArchetype();
		this.name = template.getName();
		this.group = template.getGroup();
		this.version = template.getVersion();
		this.periodicity = template.getPeriodicity();
		this.required = template.getRequired();
		this.fields = template.getFields();
		this.inputs = template.getInputs();
		this.active = template.getActive();
	}
	
	public void updateEntity(DocumentTemplate template) {
		if (template.getId()==null) {
			template.setArchetype(archetype);
			template.setGroup(group);
		}
		template.setName(name);
		template.setVersion(version);
		template.setPeriodicity(periodicity);
		template.setRequired(required);
		template.setFields(fields);
		template.setInputs(inputs);
		template.setActive(active);
	}
	
	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

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

	public List<DocumentField> getFields() {
		return fields;
	}
	
	public void setFields(List<DocumentField> fields) {
		this.fields = fields;
	}
	
	public List<DocumentInput> getInputs() {
		return inputs;
	}
	
	public void setInputs(List<DocumentInput> inputs) {
		this.inputs = inputs;
	}
	
    @Override
    public String toString() {
        return "DocumentTemplate{" + "id=" + id + ", name=" + name +", version=" + version + '}';
    }
}
