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

import static org.springframework.data.elasticsearch.annotations.FieldType.Boolean;
import static org.springframework.data.elasticsearch.annotations.FieldType.Date;
import static org.springframework.data.elasticsearch.annotations.FieldType.Integer;
import static org.springframework.data.elasticsearch.annotations.FieldType.Keyword;
import static org.springframework.data.elasticsearch.annotations.FieldType.Text;

import java.io.Serializable;
import java.time.OffsetDateTime;

import org.idb.cacao.api.Periodicity;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Document(indexName="cacao_docs_uploaded")
public class DocumentUploaded implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;

	/**
	 * ID's are being generated automatically.
	 * PS: Elasticsearch generates by default 20 character long ID's, that are both URL-safe, base 64 encoded GUID
	 */
	@Id   
	private String id;

	@Field(type=Date, store = true, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
    private OffsetDateTime timestamp;
	
	@MultiField(
			mainField = @Field(type=Text, fielddata=true),
			otherFields = {
				@InnerField(suffix = "keyword", type=Keyword)
			}
		)
	private String user;

	@MultiField(
		mainField = @Field(type=Text, fielddata=true),
		otherFields = {
			@InnerField(suffix = "keyword", type=Keyword)
		}
	)
	private String taxPayerId;

	@MultiField(
		mainField = @Field(type=Text, fielddata=true),
		otherFields = {
			@InnerField(suffix = "keyword", type=Keyword)
		}
	)
	private String templateName;
	
	@Field(type=Keyword)
	private String templateVersion;
	
	/**
	 * Original file name. The name used to store in disk is the fileId.
	 */
	@Field(type=Keyword)
	private String filename;
	
	@Field(type=Keyword)
	private String fileId;
	
	/**
	 * A folder where the file was stored in system storage.
	 */
	@Field(type=Keyword)
	private String subDir;

	@Field(type=Integer)
	private Integer taxYear;
	
	@MultiField(
		mainField = @Field(type=Text, fielddata=true),
		otherFields = {
			@InnerField(suffix = "keyword", type=Keyword)
		}
	)
	private String taxMonth;
	
	@Field(type=Integer)
	private Integer taxMonthNumber;

	/**
	 * Usually YYYYMM (YYYY = year, MM = month) for MONTHLY, or simply YYYY for YEARLY, or YYYYS (YYYY = year, S = semester) for SEMIANNUALLY
	 */
	@Field(type=Integer)
	private Integer taxPeriodNumber;

	@Field(type=Keyword)
	private String ipAddress;

	/**
	 * Tells if this document rectified another
	 */
	@Field(type=Boolean)
	private Boolean rectifying;

	/**
	 * Tells if this document was rectified by another (what means it's inactive)
	 */
	@Field(type=Boolean)
	private Boolean rectified;
	
	/**
	 * Hash code of file
	 */
	@Field(type=Keyword)
	private String hash;

	/**
	 * Date/time of last modification or creation of any part of this object
	 */
	@Field(type=Date, store = true, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
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

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getFileId() {
		return fileId;
	}

	public void setFileId(String fileId) {
		this.fileId = fileId;
	}	
	
	public String getSubDir() {
		return subDir;
	}

	public void setSubDir(String subDir) {
		this.subDir = subDir;
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
}
