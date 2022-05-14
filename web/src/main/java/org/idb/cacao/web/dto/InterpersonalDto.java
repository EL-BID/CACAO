/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.dto;

import java.io.Serializable;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.idb.cacao.api.Views;
import org.idb.cacao.web.entities.Interpersonal;
import org.idb.cacao.web.entities.RelationshipType;

import com.fasterxml.jackson.annotation.JsonView;

/**
 * Interpersonal relationships Data Transfer Object
 * 
 * @author Luis Kauer
 *
 */
public class InterpersonalDto implements Serializable {

	private static final long serialVersionUID = 1L;

	@JsonView(Views.Authority.class)
	private String id;

	@NotBlank
	@Size(min=6, max=20)
	@JsonView(Views.Authority.class)
	private String personId1;
	
	@JsonView(Views.Authority.class)
	private boolean active=true;
	
	@NotBlank
	@Size(min=6, max=20)
	@JsonView(Views.Authority.class)
	private String personId2;

	@Enumerated(EnumType.STRING)
	@NotNull
	@JsonView(Views.Authority.class)
	private RelationshipType relationshipType;

	public InterpersonalDto() {
	}
	
	public InterpersonalDto(String personId1, String personId2, RelationshipType relationshipType) {
		this.personId1 = personId1;
		this.personId2 = personId2;
		this.relationshipType = relationshipType;
	}
	
	public InterpersonalDto(Interpersonal interpersonal) {
		this.id = interpersonal.getId();
		this.personId1 = interpersonal.getPersonId1();
		this.personId2 = interpersonal.getPersonId2();
		this.relationshipType = interpersonal.getRelationshipType();
		this.active = interpersonal.isActive();
	}
	
	/**
	 * Updates a Interpersonal entity object from this DTO
	 * @param interpersonal
	 */
	public void updateEntity(Interpersonal interpersonal) {
		interpersonal.setPersonId1(personId1);
		interpersonal.setPersonId2(personId2);
		interpersonal.setRelationshipType(relationshipType);
		interpersonal.setActive(active);
	}
	
	public String getId() {
		return id;
	}

	public boolean isActive() {
		return active;
	}

	public String getPersonId1() {
		return personId1;
	}

	public String getPersonId2() {
		return personId2;
	}

	public RelationshipType getRelationshipType() {
		return relationshipType;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public void setActive(boolean active) {
		this.active = active;
	}
	
	public void setPersonId1(String personId1) {
		this.personId1 = personId1;
	}
	
	public void setPersonId2(String personId2) {
		this.personId2 = personId2;
	}

	public void setRelationshipType(RelationshipType relationshipType) {
		this.relationshipType = relationshipType;
	}
}
