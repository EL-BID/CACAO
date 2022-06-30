/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.entities;

import static org.springframework.data.elasticsearch.annotations.FieldType.Boolean;
import static org.springframework.data.elasticsearch.annotations.FieldType.Date;
import static org.springframework.data.elasticsearch.annotations.FieldType.Keyword;
import static org.springframework.data.elasticsearch.annotations.FieldType.Text;

import java.io.Serializable;
import java.time.OffsetDateTime;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.idb.cacao.api.Views;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

import com.fasterxml.jackson.annotation.JsonView;

/**
 * Interpersonal relationships
 * 
 * @author Gustavo Figueiredo
 *
 */
@Document(indexName="cacao_interpersonal")
public class Interpersonal implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;

	/**
	 * ID's are being generated automatically.
	 * PS: Elasticsearch generates by default 20 character long ID's, that are both URL-safe, base 64 encoded GUID
	 */
	@Id
	@JsonView(Views.Authority.class)
	private String id;

	@Field(type=Date, store = true, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
    private OffsetDateTime timestamp;
	
	@MultiField(
			mainField = @Field(type=Text, fielddata=true),
			otherFields = {
				@InnerField(suffix = "keyword", type=Keyword)
			}
		)
	private String user;

	@Field(type=Keyword)
	@NotBlank
	@NotNull
	@NotEmpty
	@Size(min=6, max=20)
	@JsonView(Views.Authority.class)
	private String personId1;
	
	@Field(type=Boolean)
	@JsonView(Views.Authority.class)
	private boolean active=true;
	
	@Field(type=Date, store = true, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
	private OffsetDateTime removedTimestamp;

	@Field(type=Keyword)
	@NotBlank
	@NotNull
	@NotEmpty
	@Size(min=6, max=20)
	@JsonView(Views.Authority.class)
	private String personId2;

	/**
	 * Relationship type: what 'person1' *is* for 'person2'
	 */
	@MultiField(
			mainField = @Field(type=Text, fielddata=true),
			otherFields = {
				@InnerField(suffix = "keyword", type=Keyword)
			}
		)
	@Enumerated(EnumType.STRING)
	@NotNull
	@JsonView(Views.Authority.class)
	private RelationshipType relationshipType;

	/**
	 * Date/time of last modification or creation of any part of this object
	 */
	@Field(type=Date, store = true, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
	private OffsetDateTime changedTime;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public OffsetDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(OffsetDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public OffsetDateTime getRemovedTimestamp() {
		return removedTimestamp;
	}

	public void setRemovedTimestamp(OffsetDateTime removedTimestamp) {
		this.removedTimestamp = removedTimestamp;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPersonId1() {
		return personId1;
	}

	public void setPersonId1(String personId1) {
		this.personId1 = personId1;
	}

	public String getPersonId2() {
		return personId2;
	}

	public void setPersonId2(String personId2) {
		this.personId2 = personId2;
	}

	/**
	 * Relationship type: what 'person1' *is* for 'person2'
	 */
	public RelationshipType getRelationshipType() {
		return relationshipType;
	}

	/**
	 * Relationship type: what 'person1' *is* for 'person2'
	 */
	public void setRelationshipType(RelationshipType relationshipType) {
		this.relationshipType = relationshipType;
	}

	public OffsetDateTime getChangedTime() {
		return changedTime;
	}

	public void setChangedTime(OffsetDateTime changedTime) {
		this.changedTime = changedTime;
	}
	
	public int hashCode() {
		return 17 + 37 * (id==null?0:id.hashCode());
	}
	
	public boolean equals(Object o) {
		if (this==o)
			return true;
		if (!(o instanceof Interpersonal))
			return false;
		Interpersonal ref = (Interpersonal)o;
		if (id!=ref.id) {
			if (id==null || ref.id==null)
				return false;
			if (!id.equals(ref.id))
				return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (personId1!=null && personId1.trim().length()>0)
			sb.append(personId1);
		if (relationshipType!=null) {
			sb.append(" is ");
			sb.append(relationshipType);
			sb.append(" of ");
		}
		if (personId2!=null && personId2.trim().length()>0) {
			if (sb.length()>0)
				sb.append(" ");
			sb.append(personId2);
		}
		return sb.toString();
	}
}
