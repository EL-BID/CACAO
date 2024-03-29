/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.api;

import static org.springframework.data.elasticsearch.annotations.FieldType.Boolean;
import static org.springframework.data.elasticsearch.annotations.FieldType.Date;
import static org.springframework.data.elasticsearch.annotations.FieldType.Integer;
import static org.springframework.data.elasticsearch.annotations.FieldType.Keyword;
import static org.springframework.data.elasticsearch.annotations.FieldType.Text;

import java.io.File;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;

@Document(indexName="cacao_docs_uploaded")
public class DocumentUploaded implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;

	/**
	 * ID's are being generated automatically.
	 * PS: Elasticsearch generates by default 20 character long ID's, that are both URL-safe, base 64 encoded GUID
	 */
	@JsonView(Views.Declarant.class)
	@Id	
	@AFieldDescriptor(externalName = "doc.id")
	private String id;

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
	@AFieldDescriptor(externalName = "doc.user")
	private String user;

	@JsonView(Views.Declarant.class)
	@MultiField(
			mainField = @Field(type=Text, fielddata=true),
			otherFields = {
				@InnerField(suffix = "keyword", type=Keyword)
			}
		)
	@AFieldDescriptor(externalName = "doc.user.login")
	private String userLogin;

	@JsonView(Views.Declarant.class)
	@MultiField(
		mainField = @Field(type=Text, fielddata=true),
		otherFields = {
			@InnerField(suffix = "keyword", type=Keyword)
		}
	)
	@AFieldDescriptor(externalName = "taxpayer.id")
	private String taxPayerId;

	@JsonView(Views.Declarant.class)
	@MultiField(
		mainField = @Field(type=Text, fielddata=true),
		otherFields = {
			@InnerField(suffix = "keyword", type=Keyword)
		}
	)
	@AFieldDescriptor(externalName = "doc.template")
	private String templateName;
	
	@Field(type=Keyword)
	@AFieldDescriptor(externalName = "template.version")
	private String templateVersion;
	
	@Field(type=Keyword)
	@AFieldDescriptor(externalName = "input.name")
	private String inputName;	
	
	/**
	 * Original file name. The name used to store in disk is the fileId.
	 */
	@Field(type=Keyword)
	@AFieldDescriptor(externalName = "doc.file.name")
	private String filename;
	
	/**
	 * FileId is a random UUID generated for the file by the time it was uploaded. Each
	 * uploaded file has a different FileId
	 */
	@Field(type=Keyword)
	@AFieldDescriptor(externalName = "doc.file.id")
	private String fileId;
	
	/**
	 * Concatenation of values related to fields that have the 'file uniqueness' mark. If different
	 * uploaded files have the same uniqueId, they should be considered as replacements (the last one
	 * replaces the previous one). Usually the 'uniqueId' contains the 'taxpayer Id' and the 'year', but
	 * the template may define other fields to be used as 'uniqueId'.
	 */
	@Field(type=Keyword)
	private String uniqueId;

	/**
	 * A folder where the file was stored in system storage.
	 */
	@Field(type=Keyword)
	@AFieldDescriptor(externalName = "doc.file.subdir")
	private String subDir;

	@Field(type=Integer)
	@AFieldDescriptor(externalName = "tax.year")
	private Integer taxYear;
	
	@MultiField(
		mainField = @Field(type=Text, fielddata=true),
		otherFields = {
			@InnerField(suffix = "keyword", type=Keyword)
		}
	)
	@AFieldDescriptor(externalName = "tax.month")
	private String taxMonth;
	
	@Field(type=Integer)
	@AFieldDescriptor(externalName = "tax.month.number")
	private Integer taxMonthNumber;

	/**
	 * Usually YYYYMM (YYYY = year, MM = month) for MONTHLY, or simply YYYY for YEARLY, or YYYYS (YYYY = year, S = semester) for SEMIANNUALLY
	 */
	@JsonView(Views.Declarant.class)
	@Field(type=Integer)
	@AFieldDescriptor(externalName = "tax.period.number")
	private Integer taxPeriodNumber;

	@Field(type=Keyword)
	@AFieldDescriptor(externalName = "doc.ipAddr")
	private String ipAddress;

	/**
	 * Tells if this document rectified another
	 */
	@Field(type=Boolean)
	@AFieldDescriptor(externalName = "doc.rectifying")
	private Boolean rectifying;

	/**
	 * Tells if this document was rectified by another (what means it's inactive)
	 */
	@Field(type=Boolean)
	@AFieldDescriptor(externalName = "doc.rectified")
	private Boolean rectified;
	
	/**
	 * Hash code of file
	 */
	@Field(type=Keyword)
	@AFieldDescriptor(externalName = "doc.file.hash")
	private String hash;
	
	@JsonView(Views.Public.class)
	@Enumerated(EnumType.STRING)
	@MultiField(
			mainField = @Field(type=Text, fielddata=true),
			otherFields = {
				@InnerField(suffix = "keyword", type=Keyword)
			}
		)
	@AFieldDescriptor(externalName = "doc.situation")
	private DocumentSituation situation; 

	/**
	 * Date/time of last modification or creation of any part of this object
	 */
	@Field(type=Date, store = true, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
	@AFieldDescriptor(externalName = "changed.time")
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

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getUserLogin() {
		return userLogin;
	}

	public void setUserLogin(String userLogin) {
		this.userLogin = userLogin;
	}

	public String getTaxPayerId() {
		return taxPayerId;
	}

	public void setTaxPayerId(String taxPayerId) {
		this.taxPayerId = taxPayerId;
	}

	public String getTemplateName() {
		return templateName;
	}

	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}

	public String getTemplateVersion() {
		return templateVersion;
	}

	public void setTemplateVersion(String templateVersion) {
		this.templateVersion = templateVersion;
	}

	public String getInputName() {
		return inputName;
	}

	public void setInputName(String inputName) {
		this.inputName = inputName;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	/**
	 * FileId is a random UUID generated for the file by the time it was uploaded. Each
	 * uploaded file has a different FileId
	 */
	public String getFileId() {
		return fileId;
	}

	/**
	 * FileId is a random UUID generated for the file by the time it was uploaded. Each
	 * uploaded file has a different FileId
	 */
	public void setFileId(String fileId) {
		this.fileId = fileId;
	}	
	
	/**
	 * Concatenation of values related to fields that have the 'file uniqueness' mark. If different
	 * uploaded files have the same uniqueId, they should be considered as replacements (the last one
	 * replaces the previous one). Usually the 'uniqueId' contains the 'taxpayer Id' and the 'year', but
	 * the template may define other fields to be used as 'uniqueId'.
	 */
	public String getUniqueId() {
		return uniqueId;
	}

	/**
	 * Concatenation of values related to fields that have the 'file uniqueness' mark. If different
	 * uploaded files have the same uniqueId, they should be considered as replacements (the last one
	 * replaces the previous one). Usually the 'uniqueId' contains the 'taxpayer Id' and the 'year', but
	 * the template may define other fields to be used as 'uniqueId'.
	 */
	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}

	public String getSubDir() {
		return subDir;
	}

	public void setSubDir(String subDir) {
		this.subDir = subDir;
	}
	
	public String getFileIdWithPath() {
		return getSubDir() + File.separator + getFileId();
	}

	public Integer getTaxYear() {
		return taxYear;
	}

	public void setTaxYear(Integer taxYear) {
		this.taxYear = taxYear;
	}

	public String getTaxMonth() {
		return taxMonth;
	}

	public void setTaxMonth(String taxMonth) {
		this.taxMonth = taxMonth;
	}

	public Integer getTaxMonthNumber() {
		return taxMonthNumber;
	}

	public void setTaxMonthNumber(Integer taxMonthNumber) {
		this.taxMonthNumber = taxMonthNumber;
	}

	public Integer getTaxPeriodNumber() {
		return taxPeriodNumber;
	}

	public void setTaxPeriodNumber(Integer taxPeriodNumber) {
		this.taxPeriodNumber = taxPeriodNumber;
	}
	
	@JsonIgnore
	public String getTaxPeriod() {
		if (taxPeriodNumber!=null) {
			return Periodicity.getFormattedPeriod(taxPeriodNumber);
		}
		if (taxMonth!=null && taxYear!=null) {
			return taxMonth+"-"+taxYear;
		}
		return null;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	/**
	 * Tells if this document rectified another
	 */
	public Boolean getRectifying() {
		return rectifying;
	}

	/**
	 * Tells if this document rectified another
	 */
	public void setRectifying(Boolean rectifying) {
		this.rectifying = rectifying;
	}

	/**
	 * Tells if this document was rectified by another (what means it's inactive)
	 */
	public Boolean getRectified() {
		return rectified;
	}
	
	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public DocumentSituation getSituation() {
		if ( situation == null )
			situation = DocumentSituation.RECEIVED;
		return situation;
	}

	public void setSituation(DocumentSituation situation) {
		this.situation = situation;
	}

	/**
	 * Tells if this document was rectified by another (what means it's inactive)
	 */
	public void setRectified(Boolean rectified) {
		this.rectified = rectified;
	}

	public OffsetDateTime getChangedTime() {
		return changedTime;
	}

	public void setChangedTime(OffsetDateTime changedTime) {
		this.changedTime = changedTime;
	}

	public DocumentUploaded clone() {
		try {
			return (DocumentUploaded)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		return filename;
	}
	
	public int hashCode() {
		return 17 + 37 * (id==null ? 0 : id.hashCode());
	}
	
	public boolean equals(Object o) {
		if (this==o)
			return true;
		if (!(o instanceof DocumentUploaded))
			return false;
		DocumentUploaded ref = (DocumentUploaded)o;
		return Objects.equals(id, ref.id);
	}
}
