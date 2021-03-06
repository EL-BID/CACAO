/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
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

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

import com.fasterxml.jackson.annotation.JsonView;

/**
 * An error message about validation of a specific document 
 * 
 * @author Rivelino Patrício
 * 
 * @since 15/11/2021
 *
 */
@Document(indexName="cacao_docs_error_message")
public class DocumentValidationErrorMessage implements Serializable, Cloneable {

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
	 * Referenced document for this error message
	 */
	@JsonView(Views.Declarant.class)
	@Field(type=Keyword)
	@AFieldDescriptor(externalName = "doc.error.documentId")
	private String documentId;
	
	/**
	 * Original file name. The name used to store in disk is the fileId.
	 */
	@JsonView(Views.Declarant.class)
	@Field(type=Keyword)
	@AFieldDescriptor(externalName = "doc.error.documentFilename")
	private String documentFilename;	
	
	/**
	 * The error message
	 */	
	@JsonView(Views.Public.class)
	@AFieldDescriptor(externalName = "doc.error.message")
	private String errorMessage; 

	/**
	 * Date/time of last modification or creation of any part of this object
	 */
	@Field(type=Date, store = true, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
	@AFieldDescriptor(externalName = "changed.time")
	private OffsetDateTime changedTime;
	
	public static DocumentValidationErrorMessage create() {
		return new DocumentValidationErrorMessage();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public String getTemplateName() {
		return templateName;
	}

	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}
	
	public DocumentValidationErrorMessage withTemplateName(String templateName) {
		this.templateName = templateName;
		return this;
	}
	
	public String getTaxPayerId() {
		return taxPayerId;
	}

	public void setTaxPayerId(String taxPayerId) {
		this.taxPayerId = taxPayerId;
	}
	
	public DocumentValidationErrorMessage withTaxPayerId(String taxPayerId) {
		this.taxPayerId = taxPayerId;
		return this;
	}

	public String getDocumentId() {
		return documentId;
	}

	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}
	
	public DocumentValidationErrorMessage withDocumentId(String documentId) {
		this.documentId = documentId;
		return this;
	}	

	public String getDocumentFilename() {
		return documentFilename;
	}

	public void setDocumentFilename(String documentFilename) {
		this.documentFilename = documentFilename;
	}
	
	public DocumentValidationErrorMessage withDocumentFilename(String documentFilename) {
		this.documentFilename = documentFilename;
		return this;
	}	
	
	public Integer getTaxPeriodNumber() {
		return taxPeriodNumber;
	}

	public void setTaxPeriodNumber(Integer taxPeriodNumber) {
		this.taxPeriodNumber = taxPeriodNumber;
	}
	
	public DocumentValidationErrorMessage withTaxPeriodNumber(Integer taxPeriodNumber) {
		this.taxPeriodNumber = taxPeriodNumber;
		return this;
	}	

	public OffsetDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(OffsetDateTime timestamp) {
		this.timestamp = timestamp;
	}
	
	public DocumentValidationErrorMessage withTimestamp(OffsetDateTime timestamp) {
		this.timestamp = timestamp;
		return this;
	}	

	public String getErrorMessage() {		
		return errorMessage;		
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	
	public DocumentValidationErrorMessage withErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
		return this;
	}	

	public DocumentValidationErrorMessage clone() {
		try {
			return (DocumentValidationErrorMessage)super.clone();
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
