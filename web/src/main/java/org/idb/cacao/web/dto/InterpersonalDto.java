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
