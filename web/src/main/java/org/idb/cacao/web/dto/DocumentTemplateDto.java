/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.dto;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import javax.validation.constraints.NotBlank;
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

	private String id;
	
	private String archetype;

	@NotBlank
	@Size(min=2, max=120)
	private String name;

	private String group;

	@NotBlank
	@Size(min=1, max=20)
	private String version;
	
	private Periodicity periodicity;

	private Boolean required;
	
	private List<DocumentField> fields;
	
	private List<DocumentInput> inputs;
	
	private Boolean active;
	
	public DocumentTemplateDto() {
	}
	
	public DocumentTemplateDto(String id, String archetype, String name, String group,
			String version, Periodicity periodicity, boolean required, boolean active,
			DocumentField... fields) {
		this.id = id;
		this.archetype = archetype;
		this.name = name;
		this.group = group;
		this.version = version;
		this.periodicity = periodicity;
		this.required = required;
		this.active = active;
		this.fields = Arrays.asList(fields);
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
		template.setArchetype(archetype);
		template.setGroup(group);
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

	public String getId() {
		return id;
	}

	public String getArchetype() {
		return archetype;
	}
	
	public String getName() {
		return name;
	}
	
	public String getGroup() {
		return group;
	}
	
	public String getVersion() {
		return version;
	}
	
	public Periodicity getPeriodicity() {
		if (periodicity==null)
			return Periodicity.UNKNOWN;
		return periodicity;
	}
	
	public Boolean getRequired() {
		return required;
	}
	
	public List<DocumentField> getFields() {
		return fields;
	}
	
	public List<DocumentInput> getInputs() {
		return inputs;
	}
	
	public void setActive(Boolean active) {
		this.active = active;
	}
	
	public void setId(String id) {
		this.id = id;
	}

	public void setArchetype(String archetype) {
		this.archetype = archetype;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public void setPeriodicity(Periodicity periodicity) {
		this.periodicity = periodicity;
	}

	public void setRequired(Boolean required) {
		this.required = required;
	}

	public void setFields(List<DocumentField> fields) {
		this.fields = fields;
	}
	
	public void setInputs(List<DocumentInput> inputs) {
		this.inputs = inputs;
	}
	
    @Override
    public String toString() {
        return "DocumentTemplate{" + "id=" + id + ", name=" + name +", version=" + version + '}';
    }
}
