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
package org.idb.cacao.api.templates;

import static org.springframework.data.elasticsearch.annotations.FieldType.Date;
import static org.springframework.data.elasticsearch.annotations.FieldType.Keyword;
import static org.springframework.data.elasticsearch.annotations.FieldType.Nested;
import static org.springframework.data.elasticsearch.annotations.FieldType.Text;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.idb.cacao.api.DomainLanguage;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * This entity represents any generic structure modeled inside CACAO by the tax administration.<BR>
 * <BR>
 * It is a template with fields definitions (i.e.: contains fields types and fields names).<BR>
 * <BR>
 * This class is intended to be used in two different ways:<BR>
 * BUILT-IN DOMAIN TABLE<BR>
 * ===========================================<BR>
 * Some of the CACAO plugins and modules may declare built-in domain tables. These are domain tables
 * created by the system itself. It may be the case for some 'TemplateArchetype' that defines DocumentField's
 * related to internal built-in domain table.<BR>
 * In this case, the object is defined in code or it's loaded in runtime. Each DomainEntry contains a key,
 * but do not define a specific 'language'. Instead it will assign for each DomainEntry a 'message key'
 * to be searched in 'messages.properties' files according to the provided languages at application classpath.<BR>
 * For example, suppose we have a 'built-in' domain table with the following entries (DomainEntry):<BR>
 * <table>
 * <tr><th>key</th><th>language</th><th>description</th></tr>
 * <tr><td>1</td><td>null</td><td>my.table.code.1</td></tr>
 * <tr><td>2</td><td>null</td><td>my.table.code.2</td></tr>
 * </table>
 * Suppose we have provided to the application a 'messages_en.properties' file with the following entries:<BR>
 * <pre>
 * my.table.code.1=One
 * my.table.code.2=Two
 * </pre>
 * And suppose we have provided to the application a 'messages_es.properties' file with the following entries:<BR>
 * <pre>
 * my.table.code.1=Uno
 * my.table.code.2=Dos
 * </pre>
 * With the above configuration, the application will automatically create and store a new 'resolved' domain table with the
 * following entries:<BR>
 * <table>
 * <tr><th>key</th><th>language</th><th>description</th></tr>
 * <tr><td>1</td><td>ENGLISH</td><td>One</td></tr>
 * <tr><td>2</td><td>ENGLISH</td><td>Two</td></tr>
 * <tr><td>1</td><td>SPANISH</td><td>Uno</td></tr>
 * <tr><td>2</td><td>SPANISH</td><td>Dos</td></tr>
 * </table>
 * <BR>
 * USER PROVIDED DOMAIN TABLE<BR>
 * ===========================================<BR>
 * This is the normal use case of 'domain tables'. The user of CACAO may create, update and delete any domain tables.
 * In this case, the user must provide each DomainEntry with all the required information:<BR>
 * <pre>
 * key
 * language
 * description
 * </pre>
 * Doing this, the same information in the 'domain table' may resolve to different languages as determined by the user.
 * Unlike a 'built-in' table, the user-provided table may not refer to 'messages.properties' files because his definitions
 * are only given at runtime, not at compile time.<BR>
 *
 * <BR>
 * @author Gustavo Figueiredo
 *
 */
@Document(indexName="cacao_domain_tables")
public class DomainTable implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;

	/**
	 * ID's are being generated automatically.
	 * PS: Elasticsearch generates by default 20 character long ID's, that are both URL-safe, base 64 encoded GUID
	 */
	@Id   
	private String id;

	/**
	 * Name of this domain table. Name and version must be unique in the system (for DomainTables).
	 */
	@MultiField(
		mainField = @Field(type=Text, fielddata=true),
		otherFields = {
			@InnerField(suffix = "keyword", type=Keyword)
		}
	)
	@NotBlank
	@NotNull
	@NotEmpty
	@Size(min=2, max=120)
	private String name;

	/**
	 * Optional group of this domain table. Its only purpose is to organize different domain
	 * tables according to a particular group.
	 */
	@MultiField(
		mainField = @Field(type=Text, fielddata=true),
		otherFields = {
			@InnerField(suffix = "keyword", type=Keyword)
		}
	)
	private String group;
	
	/**
	 * Version of this domain table (in case it's necessary to keep a history of
	 * different versions of the same domain table over time). Name and version must be unique in the system (for DomainTables).
	 */
	@MultiField(
		mainField = @Field(type=Text, fielddata=true),
		otherFields = {
			@InnerField(suffix = "keyword", type=Keyword)
		}
	)
	@NotBlank
	@NotNull
	@NotEmpty
	@Size(min=1, max=20)
	private String version;
	
	/**
	 * Date/time the domain table was created
	 */
	@Field(type=Date, store = true, format = DateFormat.date_time)
    private OffsetDateTime domainTableCreateTime;

	/**
	 * Date/time of last modification or creation of any part of this domain table
	 */
	@Field(type=Date, store = true, format = DateFormat.date_time)
	private OffsetDateTime changedTime;

	@Field(type=Nested)
	private List<DomainEntry> entries;
	
	public DomainTable() { }
	
	public DomainTable(String name, String version) {
		this.name = name;
		this.version = version;
	}
	
	/**
	 * Creates a new built-in DomainTable given enumeration constants (this must be resolved at
	 * runtime). The 'keys' are the 'constant names'. The 'descriptions' are the 'toString' results
	 * for each enum constant. IMPORTANT: it's expected that the enum's toString method return
	 * a 'messages.properties' entry, not the description itself.
	 */
	public static <T extends Enum<?>> DomainTable fromEnum(String name, String version, Class<T> enumeration) {
		return fromEnum(name, version, enumeration, /*getKey*/Enum::name, /*getValue*/Object::toString);
	}

	/**
	 * Creates a new built-in DomainTable given enumeration constants (this must be resolved at
	 * runtime). The 'keys' are calculated using the provided 'getKey' function over each enum constant. The constants that evaluates
	 * to NULL key are filtered out.<BR>
	 * The 'descriptions' are the 'toString' results for each enum constant. IMPORTANT: it's expected that the enum's toString method return
	 * a 'messages.properties' entry, not the description itself.
	 */
	public static <T extends Enum<?>> DomainTable fromEnum(String name, String version, Class<T> enumeration, Function<T,String> getKey) {
		return fromEnum(name, version, enumeration, getKey, /*getValue*/Object::toString);
	}
	
	/**
	 * Creates a new built-in DomainTable given enumeration constants (this must be resolved at
	 * runtime). The 'keys' are calculated using the provided 'getKey' function over each enum constant. The constants that evaluates
	 * to NULL or empty key are filtered out.<BR> 
	 * The 'descriptions' are calculated using the provided 'getValue' function over each enum constant. 
	 * IMPORTANT: it's expected that the 'getValue' method return a 'messages.properties' entry, not the description itself.
	 */
	public static <T extends Enum<?>> DomainTable fromEnum(String name, String version, Class<T> enumeration, 
			Function<T,String> getKey,
			Function<T,String> getValue) {
		DomainTable domain = new DomainTable(name, version);
		for (T element: enumeration.getEnumConstants()) {
			String key = getKey.apply(element);
			if (key==null || key.trim().length()==0)
				continue;
			String messagePropertyRef = getValue.apply(element);			
			domain.addBuiltInEntry(key, messagePropertyRef);
		}
		return domain;
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
	
	public DomainTable withName(String name) {
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
	
	public DomainTable withGroup(String group) {
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

	/**
	 * Version of this domain table (in case it's necessary to keep a history of
	 * different versions of the same domain table over time). Name and version must be unique in the system (for DomainTables).
	 */
	public void setVersion(String version) {
		this.version = version;
	}
	
	public DomainTable withVersion(String version) {
		setVersion(version);
		return this;
	}

	/**
	 * Date/time the domain table was created
	 */
	public OffsetDateTime getDomainTableCreateTime() {
		return domainTableCreateTime;
	}

	/**
	 * Date/time the domain table was created
	 */
	public void setDomainTableCreateTime(OffsetDateTime domainTableCreateTime) {
		this.domainTableCreateTime = domainTableCreateTime;
	}
	
	public DomainTable withDomainTableCreateTime(OffsetDateTime domainTableCreateTime) {
		setDomainTableCreateTime(domainTableCreateTime);
		return this;
	}

	/**
	 * Date/time of last modification or creation of any part of this domain table
	 */
	public OffsetDateTime getChangedTime() {
		return changedTime;
	}

	/**
	 * Date/time of last modification or creation of any part of this domain table
	 */
	public void setChangedTime(OffsetDateTime changedTime) {
		this.changedTime = changedTime;
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
	
	public DomainTable withEntries(List<DomainEntry> entries) {
		setEntries(entries);
		return this;
	}
	
	public DomainTable withEntries(DomainEntry... entries) {
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
	
	public DomainTable clone() {
		try {
			DomainTable clone = (DomainTable)super.clone();
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
		if (!(o instanceof DomainTable))
			return false;
		DomainTable ref = (DomainTable)o;
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

}
