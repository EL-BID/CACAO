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
