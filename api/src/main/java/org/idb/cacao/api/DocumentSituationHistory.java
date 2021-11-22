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
package org.idb.cacao.api;

import static org.springframework.data.elasticsearch.annotations.FieldType.Date;
import static org.springframework.data.elasticsearch.annotations.FieldType.Integer;
import static org.springframework.data.elasticsearch.annotations.FieldType.Keyword;
import static org.springframework.data.elasticsearch.annotations.FieldType.Text;

import java.io.Serializable;
import java.time.OffsetDateTime;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

import com.fasterxml.jackson.annotation.JsonView;

/**
 * Representation of history situation for a given document across time.
 * Every time that situation changes, a record with that situation is stored. 
 * 
 * @author Rivelino Patrício
 * 
 * @since 06/11/2021
 *
 */
@Document(indexName="cacao_docs_situation_history")
public class DocumentSituationHistory implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;

	/**
	 * ID's are being generated automatically.
	 * PS: Elasticsearch generates by default 20 character long ID's, that are both URL-safe, base 64 encoded GUID
	 */
	@JsonView(Views.Declarant.class)
	@Id   
	@AFieldDescriptor(externalName = "doc.id")
	private String id;

	/**
	 * Date and time situation is modified
	 */
	@JsonView(Views.Declarant.class)
	@Field(type=Date, store = true, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
	@AFieldDescriptor(externalName = "doc.timestamp")
    private OffsetDateTime timestamp;

	@JsonView(Views.Declarant.class)
	@MultiField(
		mainField = @Field(type=Text, fielddata=true),
		otherFields = {
			@InnerField(suffix = "keyword", type=Keyword)
		}
	)
	@AFieldDescriptor(externalName = "taxpayer.id")
	private String taxPayerId;
	
	@Field(type=Integer)
	@AFieldDescriptor(externalName = "tax.period.number")
	private Integer taxPeriodNumber;		
	
	@JsonView(Views.Declarant.class)
	@MultiField(
		mainField = @Field(type=Text, fielddata=true),
		otherFields = {
			@InnerField(suffix = "keyword", type=Keyword)
		}
	)
	@AFieldDescriptor(externalName = "doc.template")
	private String templateName;	
	
	/**
	 * Referenced document for this situation
	 */
	@JsonView(Views.Declarant.class)
	@Field(type=Keyword)
	@AFieldDescriptor(externalName = "doc.situation.documentId")
	private String documentId;
	
	/**
	 * Original file name. The name used to store in disk is the fileId.
	 */
	@JsonView(Views.Declarant.class)
	@Field(type=Keyword)
	@AFieldDescriptor(externalName = "doc.situation.documentFilename")
	private String documentFilename;	
	
	/**
	 * The situation
	 */	
	@JsonView(Views.Public.class)
	@Enumerated(EnumType.STRING)
	@Field(type=Keyword)
	@AFieldDescriptor(externalName = "doc.situation")
	private DocumentSituation situation; 

	/**
	 * Date/time of last modification or creation of any part of this object
	 */
	@JsonView(Views.Public.class)	
	@Field(type=Date, store = true, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
	@AFieldDescriptor(externalName = "changed.time")
	private OffsetDateTime changedTime;	
	
	public static DocumentSituationHistory create() {
		return new DocumentSituationHistory();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public String getTaxPayerId() {
		return taxPayerId;
	}

	public void setTaxPayerId(String taxPayerId) {
		this.taxPayerId = taxPayerId;
	}	
	
	public DocumentSituationHistory withTaxPayerId(String taxPayerId) {
		this.taxPayerId = taxPayerId;
		return this;
	}	
	
	public Integer getTaxPeriodNumber() {
		return taxPeriodNumber;
	}

	public void setTaxPeriodNumber(Integer taxPeriodNumber) {
		this.taxPeriodNumber = taxPeriodNumber;
	}
	
	public DocumentSituationHistory withTaxPeriodNumber(Integer taxPeriodNumber) {
		this.taxPeriodNumber = taxPeriodNumber;
		return this;
	}		
	
	public String getTemplateName() {
		return templateName;
	}

	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}
	
	public DocumentSituationHistory withTemplateName(String templateName) {
		this.templateName = templateName;
		return this;
	}	

	public String getDocumentId() {
		return documentId;
	}

	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}
	
	public DocumentSituationHistory withDocumentId(String documentId) {
		this.documentId = documentId;
		return this;
	}	

	public String getDocumentFilename() {
		return documentFilename;
	}

	public void setDocumentFilename(String documentFilename) {
		this.documentFilename = documentFilename;
	}
	
	public DocumentSituationHistory withDocumentFilename(String documentFilename) {
		this.documentFilename = documentFilename;
		return this;
	}		

	public OffsetDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(OffsetDateTime timestamp) {
		this.timestamp = timestamp;
	}
	
	public DocumentSituationHistory withTimestamp(OffsetDateTime timestamp) {
		this.timestamp = timestamp;
		return this;
	}	

	public DocumentSituation getSituation() {
		if ( situation == null )
			situation = DocumentSituation.RECEIVED;
		return situation;
	}

	public void setSituation(DocumentSituation situation) {
		this.situation = situation;
	}
	
	public DocumentSituationHistory withSituation(DocumentSituation situation) {
		this.situation = situation;
		return this;
	}		

	public DocumentSituationHistory clone() {
		try {
			return (DocumentSituationHistory)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}
	
	public OffsetDateTime getChangedTime() {
		return changedTime;
	}

	public void setChangedTime(OffsetDateTime changedTime) {
		this.changedTime = changedTime;
	}

}
