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
package org.idb.cacao.web.entities;

import static org.springframework.data.elasticsearch.annotations.FieldType.*;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

/**
 * Interpersonal relationships
 * 
 * @author Gustavo Figueiredo
 *
 */
@Document(indexName="interpersonal")
public class Interpersonal implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;

	/**
	 * ID's are being generated automatically.
	 * PS: Elasticsearch generates by default 20 character long ID's, that are both URL-safe, base 64 encoded GUID
	 */
	@Id   
	private String id;

	@Field(type=Date, store = true, format = DateFormat.custom, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
    private Date timestamp;
	
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
	@Size(min=9, max=9)
	private String personId1;
	
	@Field(type=Boolean)
	private boolean removed;
	
	@Field(type=Date, store = true, format = DateFormat.custom, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
	private Date removedTimestamp;

	@Field(type=Keyword)
	@NotBlank
	@NotNull
	@NotEmpty
	@Size(min=9, max=9)
	private String personId2;

	/**
	 * Relationship type: what 'person1' *is* for 'person2'
	 */
	@Field(type=Text)
	@Enumerated(EnumType.STRING)
	@NotNull
	private RelationshipType relationshipType;

	/**
	 * Date/time of last modification or creation of any part of this object
	 */
	@Field(type=Date, store = true, format = DateFormat.custom, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
	private Date changedTime;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public Date getRemovedTimestamp() {
		return removedTimestamp;
	}

	public void setRemovedTimestamp(Date removedTimestamp) {
		this.removedTimestamp = removedTimestamp;
	}

	public boolean isRemoved() {
		return removed;
	}

	public void setRemoved(boolean removed) {
		this.removed = removed;
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

	public Date getChangedTime() {
		return changedTime;
	}

	public void setChangedTime(Date changedTime) {
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
