/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GenericCounts {

	private Long created;
	private Long updated;
	private Long deleted;
	private Long errors;
	
	public Long getCreated() {
		return created;
	}
	public void setCreated(Long created) {
		this.created = created;
	}
	public GenericCounts withCreated(long created) {
		setCreated(created);
		return this;
	}
	public Long getUpdated() {
		return updated;
	}
	public void setUpdated(Long updated) {
		this.updated = updated;
	}
	public GenericCounts withUpdated(long updated) {
		setUpdated(updated);
		return this;
	}
	public boolean hasChanges() {
		return (created!=null && created.longValue()>0)
			|| (updated!=null && updated.longValue()>0)
			|| (deleted!=null && deleted.longValue()>0);
	}
	public Long getDeleted() {
		return deleted;
	}
	public void setDeleted(Long deleted) {
		this.deleted = deleted;
	}
	public GenericCounts withDeleted(long deleted) {
		setDeleted(deleted);
		return this;
	}
	public Long getErrors() {
		return errors;
	}
	public void setErrors(Long errors) {
		this.errors = errors;
	}
	public GenericCounts withErrors(long errors) {
		setErrors(errors);
		return this;
	}
	
	public String toString() {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			return objectMapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return super.toString();
		}
	}
}
