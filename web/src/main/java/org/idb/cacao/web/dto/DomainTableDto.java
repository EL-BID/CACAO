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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.idb.cacao.api.DomainLanguage;
import org.idb.cacao.api.Views;
import org.idb.cacao.api.templates.DomainEntry;
import org.idb.cacao.api.templates.DomainTable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;

/**
 * Domain Table Data Transfer Object
 * <BR>
 * @author Gustavo Figueiredo
 *
 */
public class DomainTableDto implements Serializable {

	private static final long serialVersionUID = 1L;

	private String id;

	@JsonView(Views.Selection.class)
	@NotBlank
	@Size(min=2, max=120)
	private String name;

	private String group;
	
	@NotBlank
	@Size(min=1, max=20)
	@JsonView(Views.Selection.class)
	private String version;
	
	private List<DomainEntry> entries;
	
	private Boolean active = true;
	
	public DomainTableDto() { }
	
	public DomainTableDto(DomainTable domainTable) {
		this.id = domainTable.getId();
		this.name = domainTable.getName();
		this.version = domainTable.getVersion();
		this.group = domainTable.getGroup();
		this.entries = domainTable.getEntries();
		this.active = domainTable.getActive();
	}
	
	public DomainTableDto(String id, String name, String version, String group, boolean active, DomainEntry... entries ) {
		this.id = id;
		this.name = name;
		this.version = version;
		this.group = group;
		this.active = active;
		this.entries = Arrays.asList(entries);
	}
	
	public void updateEntity(DomainTable domainTable) {
		domainTable.setName(name);
		domainTable.setVersion(version);
		domainTable.setGroup(group);
		domainTable.setActive(active);
		domainTable.setEntries(entries);
	}
	
	public String getId() {
		return id;
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

	public Boolean getActive() {
		return active;
	}
	
	public List<DomainEntry> getEntries() {
		return entries;
	}
	
	public int getNumEntries() {
		if (entries==null)
			return 0;
		else
			return entries.size();
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setGroup(String group) {
		this.group = group;
	}
	
	public void setActive(Boolean active) {
		this.active = active;
	}

	public void setVersion(String version) {
		this.version = version;
	}
	
	public DomainEntry getEntry(String key, DomainLanguage language) {
		if (entries==null || key==null)
			return null;
		if (language==null)
			return entries.stream().filter(f->key.equalsIgnoreCase(f.getKey())).findAny().orElse(null);
		else
			return entries.stream().filter(f->language.equals(f.getLanguage()) && key.equalsIgnoreCase(f.getKey())).findAny().orElse(null);
	}
	
	public void setEntries(List<DomainEntry> entries) {
		this.entries = entries;
	}
	
	@JsonIgnore
	public List<DomainEntry> getEntriesOfLanguage(DomainLanguage language) {
		if (entries==null)
			return Collections.emptyList();
		if (language==null)
			return entries.stream().filter(f->f.getLanguage()==null).collect(Collectors.toList());
		else
			return entries.stream().filter(f->language.equals(f.getLanguage())).collect(Collectors.toList());		
	}

    @Override
    public String toString() {
        return "DomainTable{" + "id=" + id + ", name=" + name +", version=" + version + '}';
    }
}
