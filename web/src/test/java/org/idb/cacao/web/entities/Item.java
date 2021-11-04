
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

import static org.springframework.data.elasticsearch.annotations.FieldType.Date;
import static org.springframework.data.elasticsearch.annotations.FieldType.Keyword;

import java.time.OffsetDateTime;

import javax.swing.JLabel;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.idb.cacao.web.AFieldDescriptor;
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
	@AFieldDescriptor(externalName = "timestamp", editable = false, alignment = JLabel.CENTER, width = 120, tooltip = "timestamp.tooltip")
	@NotNull	
	@Field(type=Date, store = true, format = DateFormat.date_time)
    private OffsetDateTime timestamp;	
    
	@AFieldDescriptor(externalName = "item.name", alignment = JLabel.LEFT, width = 250, tooltip = "item.name.tooltip")
	@Field(type=Keyword)
	@NotNull
	@NotBlank
	@NotEmpty	
	@Size(min=5, max=100)
	private String name;

	@AFieldDescriptor(externalName = "item.category", alignment = JLabel.LEFT, width = 150, tooltip = "item.category.tooltip")
	@Field(type=Keyword)
	@NotNull
	@NotBlank
	@NotEmpty	
	@Size(min=5, max=50)	
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
