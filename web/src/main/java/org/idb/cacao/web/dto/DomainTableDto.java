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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
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
public class DomainTableDto implements Serializable, Cloneable, Comparable<DomainTableDto> {

	private static final long serialVersionUID = 1L;

	/**
	 * ID's are being generated automatically.
	 * PS: Elasticsearch generates by default 20 character long ID's, that are both URL-safe, base 64 encoded GUID
	 */
	private String id;

	/**
	 * Name of this domain table. Name and version must be unique in the system (for DomainTables).
	 */
	@JsonView(Views.Selection.class)
	@NotBlank
	@NotNull
	@NotEmpty
	@Size(min=2, max=120)
	private String name;

	/**
	 * Optional group of this domain table. Its only purpose is to organize different domain
	 * tables according to a particular group.
	 */
	private String group;
	
	/**
	 * Version of this domain table (in case it's necessary to keep a history of
	 * different versions of the same domain table over time). Name and version must be unique in the system (for DomainTables).
	 */
	@NotBlank
	@NotNull
	@NotEmpty
	@Size(min=1, max=20)
	@JsonView(Views.Selection.class)
	private String version;
	
	private List<DomainEntry> entries;
	
	/**
	 * Indicates this document template is available for sending documents.
	 */
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
	
	/**
	 * ID's are being generated automatically.
	 * PS: Elasticsearch generates by default 20 character long ID's, that are both URL-safe, base 64 encoded GUID
	 */
	public String getId() {
		return id;
	}

	/**
	 * ID's are being generated automatically.
	 * PS: Elasticsearch generates by default 20 character long ID's, that are both URL-safe, base 64 encoded GUID
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Name of this domain table. Name and version must be unique in the system (for DomainTables).
	 */
	public String getName() {
		return name;
	}

	/**
	 * Name of this domain table. Name and version must be unique in the system (for DomainTables).
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	public DomainTableDto withName(String name) {
		setName(name);
		return this;
	}

	/**
	 * Optional group of this domain table. Its only purpose is to organize different domain
	 * tables according to a particular group.
	 */
	public String getGroup() {
		return group;
	}

	/**
	 * Optional group of this domain table. Its only purpose is to organize different domain
	 * tables according to a particular group.
	 */
	public void setGroup(String group) {
		this.group = group;
	}
	
	public DomainTableDto withGroup(String group) {
		setGroup(group);
		return this;
	}

	/**
	 * Version of this domain table (in case it's necessary to keep a history of
	 * different versions of the same domain table over time). Name and version must be unique in the system (for DomainTables).
	 */
	public String getVersion() {
		return version;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	/**
	 * Version of this domain table (in case it's necessary to keep a history of
	 * different versions of the same domain table over time). Name and version must be unique in the system (for DomainTables).
	 */
	public void setVersion(String version) {
		this.version = version;
	}
	
	public DomainTableDto withVersion(String version) {
		setVersion(version);
		return this;
	}

	public List<DomainEntry> getEntries() {
		return entries;
	}
	
	/**
	 * Find a Entry with the given key and language. If the provided language is NULL, return Entry
	 * for any Language. The key comparison is case insensitive.
	 */
	public DomainEntry getEntry(String key, DomainLanguage language) {
		if (entries==null || key==null)
			return null;
		if (language==null)
			return entries.stream().filter(f->key.equalsIgnoreCase(f.getKey())).findAny().orElse(null);
		else
			return entries.stream().filter(f->language.equals(f.getLanguage()) && key.equalsIgnoreCase(f.getKey())).findAny().orElse(null);
	}
	
	/**
	 * Find entries with the given key, ignoring languages (return any one of the existent languages).
	 */
	@JsonIgnore
	public DomainEntry getEntry(String key) {
		if (entries==null || key==null)
			return null;
		return entries.stream().filter(f->key.equalsIgnoreCase(f.getKey())).findAny().orElse(null);
	}
	
	/**
	 * Returns all the languages defined in this domain table
	 */
	@JsonIgnore
	public Set<DomainLanguage> getLanguages() {
		if (entries==null)
			return Collections.emptySet();
		return entries.stream().filter(e->e.getLanguage()!=null).map(DomainEntry::getLanguage).collect(Collectors.toSet());
	}
	
	/**
	 * Transforms this object into a Map for easier lookup of values according to a specific language.
	 */
	@JsonIgnore
	public Map<String,DomainEntry> toMap(DomainLanguage language) {
		if (entries==null || entries.isEmpty())
			return Collections.emptyMap();
		return entries.stream()
			.filter(f->f.getKey()==null && ( language==null || language.equals(f.getLanguage())) )
			.collect(Collectors.toMap(
				/*keyMapper*/DomainEntry::getKey, 
				/*valueMapper*/Function.identity(), 
				/*mergeFunction*/(a,b)->a, 
				/*mapSupplier*/()->new TreeMap<>(String.CASE_INSENSITIVE_ORDER)));
	}

	/**
	 * Transforms this object into a Map for easier lookup of values in every language.
	 */
	@JsonIgnore
	public MultiLingualMap toMultiLingualMap() {
		if (entries==null || entries.isEmpty())
			return new MultiLingualMap();
		MultiLingualMap map = new MultiLingualMap();
		for (DomainEntry entry: entries) {
			if (entry.getKey()==null)
				continue;
			Map<DomainLanguage,DomainEntry> entry_map = map.computeIfAbsent(entry.getKey(), k->new HashMap<>());
			if (entry.getLanguage()==null)
				entry_map.put(DomainLanguage.ENGLISH, entry);
			else
				entry_map.put(entry.getLanguage(), entry);
		}
		return map;
	}

	/**
	 * Find entries with the given key, ignoring languages.
	 */
	@JsonIgnore
	public List<DomainEntry> getEntryAllLanguages(String key) {
		if (entries==null || key==null)
			return Collections.emptyList();
		return entries.stream().filter(f->key.equalsIgnoreCase(f.getKey())).collect(Collectors.toList());
	}

	/**
	 * Returns all the entries in this template of a given language
	 */
	@JsonIgnore
	public List<DomainEntry> getEntriesOfLanguage(DomainLanguage language) {
		if (entries==null)
			return Collections.emptyList();
		if (language==null)
			return entries.stream().filter(f->f.getLanguage()==null).collect(Collectors.toList());
		else
			return entries.stream().filter(f->language.equals(f.getLanguage())).collect(Collectors.toList());		
	}

	public void setEntries(List<DomainEntry> entries) {
		this.entries = entries;
	}
	
	public DomainTableDto withEntries(List<DomainEntry> entries) {
		setEntries(entries);
		return this;
	}
	
	public DomainTableDto withEntries(DomainEntry... entries) {
		List<DomainEntry> new_entries = new LinkedList<>();
		for (DomainEntry e: entries) {
			new_entries.add(e);
		}		
		setEntries(new_entries);
		return this;
	}

	public void clearEntries() {
		if (entries!=null)
			entries.clear();
	}
	
	public void addEntry(DomainEntry entry) {
		if (entries==null)
			entries = new LinkedList<>();
		entries.add(entry);
	}
	
	public void addEntry(String name, DomainLanguage language, String description) {
		addEntry(new DomainEntry(name, language, description));
	}
	
	public void addEntry(String name, DomainLanguage language, String description, Boolean locked) {
		addEntry(new DomainEntry(name, language, description).withLocked(locked));
	}

	public void addBuiltInEntry(String name, String messagePropertyReference) {
		addEntry(new DomainEntry(name, messagePropertyReference));
	}
	
	public void removeEntry(DomainEntry entry) {
		if (entries==null)
			return;
		entries.remove(entry);
	}
	
	public void sortEntries() {
		if (entries==null || entries.size()<2)
			return;
		Collections.sort(entries);
	}
	
	@JsonIgnore
	public int getNumEntries() {
		if (entries==null)
			return 0;
		else
			return entries.size();
	}

	@JsonIgnore
	public int getNumEntries(DomainLanguage language) {
		if (entries==null)
			return 0;
		else if (language==null)
			return (int)entries.stream().filter(s->s.getLanguage()==null).count();
		else
			return (int)entries.stream().filter(s->language.equals(s.getLanguage())).count();
	}
	
	public DomainTableDto clone() {
		try {
			DomainTableDto clone = (DomainTableDto)super.clone();
			if (this.entries!=null)
				clone.setEntries(this.entries.stream().map(DomainEntry::clone).collect(Collectors.toCollection(LinkedList::new)));
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if (this==o)
			return true;
		if (!(o instanceof DomainTableDto))
			return false;
		DomainTableDto ref = (DomainTableDto)o;
		if (name!=ref.name) {
			if (name==null || ref.name==null)
				return false;
			if (!name.equalsIgnoreCase(ref.name))
				return false;
		}
		if (version!=ref.version) {
			if (version==null || ref.version==null)
				return false;
			if (!version.equalsIgnoreCase(ref.version))
				return false;			
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		return 17 + 37 * ( (name==null?0:name.hashCode()) + 37 * (version==null?0:version.hashCode()) );
	}

    @Override
    public String toString() {
        return "DomainTable{" + "id=" + id + ", name=" + name +", version=" + version + '}';
    }

	@Override
	public int compareTo(DomainTableDto o) {
		if (name!=o.name) {
			if (name==null)
				return -1;
			if (o.name==null)
				return 1;
			int comp = String.CASE_INSENSITIVE_ORDER.compare(name, o.name);
			if (comp!=0)
				return comp;
		}

		if (group!=o.group) {
			if (group==null)
				return -1;
			if (o.group==null)
				return 1;
			int comp = String.CASE_INSENSITIVE_ORDER.compare(group, o.group);
			if (comp!=0)
				return comp;
		}
		
		if (version!=o.version) {
			if (version==null)
				return -1;
			if (o.version==null)
				return 1;
			int comp = String.CASE_INSENSITIVE_ORDER.compare(version, o.version);
			if (comp!=0)
				return comp;
		}
		
		return 0;
	}

	/**
	 * Auxiliary representation of the same domain table for easier lookup of values.
	 */
	public static class MultiLingualMap extends TreeMap<String, Map<DomainLanguage, DomainEntry>> {
		
		private static final long serialVersionUID = 1L;
		
		public MultiLingualMap() {
			super(String.CASE_INSENSITIVE_ORDER);
		}
	}
}
