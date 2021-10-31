package org.idb.cacao.web.entities;

import static org.springframework.data.elasticsearch.annotations.FieldType.Date;
import static org.springframework.data.elasticsearch.annotations.FieldType.Keyword;

import java.time.OffsetDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

@Document(indexName = "test_cacao_item")
public class Item {
    
	@Id
	private String id;
	
	/**
	 * Date/time this record was created or updated. 
	 * This is important for 'synchronizing' external replicas of this database.
	 */
	@Field(type=Date, store = true, format = DateFormat.date_time)
    private OffsetDateTime timestamp;	
    
	@Field(type=Keyword)
	private String name;

	@Field(type=Keyword)	
	private String category;

    public Item() {
    }
    
    public Item(String name, String category) {
        this.name = name;
        this.category = category;
    }    

    public Item(String id, String name, String category) {
    	this.id = id;
        this.name = name;
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
	/**
	 * {@link #timestamp}
	 */
	public OffsetDateTime getTimestamp() {
		return timestamp;
	}

	/**
	 * {@link #timestamp}
	 */
	public void setTimestamp(OffsetDateTime timestamp) {
		this.timestamp = timestamp;
	}    

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

	@Override
	public String toString() {
		return "Item [id=" + id + ", name=" + name + ", category=" + category + "]";
	}
    
}
