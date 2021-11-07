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
import static org.springframework.data.elasticsearch.annotations.FieldType.Text;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotNull;

import org.idb.cacao.api.utils.DateTimeUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

/**
 * General object for communications inside the application.
 * 
 * @author Gustavo Figueiredo
 *
 */
@Document(indexName="cacao_communications")
public class Communication implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Maximum number of days to qualify a communication as a new message
	 */
	public static final int MAX_DAYS_NEW_MESSAGE = 7;

	/**
	 * ID's are being generated automatically.
	 * PS: Elasticsearch generates by default 20 character long ID's, that are both URL-safe, base 64 encoded GUID
	 */
	@Id   
	private String id;

	/**
	 * Communication type
	 */
	@Field(type=Text)
	@Enumerated(EnumType.STRING)
	@NotNull
	private CommunicationType type;
	
	/**
	 * Communication contents
	 */
	@Field(type=Text)
	private String message;

	/**
	 * Title of this message
	 */
	@Field(type=Text)
	private String title;
	
	/**
	 * Optional group of this communication
	 */
	@Field(type=Text)
	private String group;

	@MultiField(
		mainField = @Field(type=Text, fielddata=true),
		otherFields = {
			@InnerField(suffix = "keyword", type=Keyword)
		}
	)
	private String taxPayerId;
	
	@Field(type=Text)
	private String taxPayerName;
	
	/**
	 * Alternative text for Telegram notification
	 */
	@Field(type=Text)
	private String telegramMessage;
	
	/**
	 * Alternative text for e-mail notification
	 */
	@Field(type=Text)
	private String emailMessage;

	/**
	 * Date/time of record creation
	 */
	@Field(type=Date, store = true, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
    private OffsetDateTime createTimestamp;

	/**
	 * User that created this record
	 */
	@MultiField(
			mainField = @Field(type=Text, fielddata=true),
			otherFields = {
				@InnerField(suffix = "keyword", type=Keyword)
			}
		)
	private String createUser;

	/**
	 * Date/time of last modification to the communication
	 */
	@Field(type=Date, store = true, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
    private OffsetDateTime updateTimestamp;
	
	/**
	 * User that updated this record
	 */
	@MultiField(
			mainField = @Field(type=Text, fielddata=true),
			otherFields = {
				@InnerField(suffix = "keyword", type=Keyword)
			}
		)
	private String updateUser;
	
	/**
	 * Date/time of last presentation of the communication
	 */
	@Field(type=Date, store = true, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
	private OffsetDateTime endViewTimestamp;
	
	/**
	 * Target audience of the communication (depends on communication type)<BR>
	 * The audience may be users names, users logins, users taxpayers ID or users profile names.<BR>
	 * It's also possible to inform multiple audiences separating them by commas.
	 */
	@MultiField(
			mainField = @Field(type=Text, fielddata=true),
			otherFields = {
				@InnerField(suffix = "keyword", type=Keyword)
			}
		)
	private String audience;
	
	/**
	 * Date/time of first view access (depends on communication type and audience)
	 */
	@Field(type=Date, store = true, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
    private OffsetDateTime viewTimestamp;

	/**
	 * User that first viewed this message (depends on communication type and audience)
	 */
	@MultiField(
			mainField = @Field(type=Text, fielddata=true),
			otherFields = {
				@InnerField(suffix = "keyword", type=Keyword)
			}
		)
	private String viewUser;

	@Field(type=Keyword)
	private String viewIpAddress;

	/**
	 * Date/time of last modification or creation of any part of this object
	 */
	@Field(type=Date, store = true, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
	private OffsetDateTime changedTime;

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
	 * Communication type
	 */
	public CommunicationType getType() {
		return type;
	}

	/**
	 * Communication type
	 */
	public void setType(CommunicationType type) {
		this.type = type;
	}

	/**
	 * Communication contents
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Communication contents
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Optional group of this communication
	 */
	public String getGroup() {
		return group;
	}

	/**
	 * Optional group of this communication
	 */
	public void setGroup(String group) {
		this.group = group;
	}

	/**
	 * Taxpayer ID regarding this communication
	 */
	public String getTaxPayerId() {
		return taxPayerId;
	}

	/**
	 * Taxpayer ID regarding this communication
	 */
	public void setTaxPayerId(String taxPayerId) {
		this.taxPayerId = taxPayerId;
	}

	/**
	 * Taxpayer Name regarding this communication
	 */
	public String getTaxPayerName() {
		return taxPayerName;
	}

	/**
	 * Taxpayer Name regarding this communication
	 */
	public void setTaxPayerName(String taxPayerName) {
		this.taxPayerName = taxPayerName;
	}

	/**
	 * Alternative text for Telegram notification
	 */
	public String getTelegramMessage() {
		return telegramMessage;
	}

	/**
	 * Alternative text for Telegram notification
	 */
	public void setTelegramMessage(String telegramMessage) {
		this.telegramMessage = telegramMessage;
	}

	/**
	 * Alternative text for e-mail notification
	 */
	public String getEmailMessage() {
		return emailMessage;
	}

	/**
	 * Alternative text for e-mail notification
	 */
	public void setEmailMessage(String emailMessage) {
		this.emailMessage = emailMessage;
	}

	/**
	 * Date/time of record creation
	 */
	public OffsetDateTime getCreateTimestamp() {
		return createTimestamp;
	}

	/**
	 * Date/time of record creation
	 */
	public void setCreateTimestamp(OffsetDateTime createTimestamp) {
		this.createTimestamp = createTimestamp;
	}
	
	public boolean getNew() {
		if (createTimestamp==null)
			return false;
		return ChronoUnit.DAYS.between(createTimestamp.toInstant(), DateTimeUtils.now().toInstant()) < MAX_DAYS_NEW_MESSAGE;
	}

	/**
	 * User that created this record
	 */
	public String getCreateUser() {
		return createUser;
	}

	/**
	 * User that created this record
	 */
	public void setCreateUser(String createUser) {
		this.createUser = createUser;
	}

	/**
	 * Date/time of last modification to the communication
	 */
	public OffsetDateTime getUpdateTimestamp() {
		return updateTimestamp;
	}

	/**
	 * Date/time of last modification to the communication
	 */
	public void setUpdateTimestamp(OffsetDateTime updateTimestamp) {
		this.updateTimestamp = updateTimestamp;
	}

	/**
	 * User that updated this record
	 */
	public String getUpdateUser() {
		return updateUser;
	}

	/**
	 * User that updated this record
	 */
	public void setUpdateUser(String updateUser) {
		this.updateUser = updateUser;
	}

	/**
	 * Date/time of last presentation of the communication
	 */
	public OffsetDateTime getEndViewTimestamp() {
		return endViewTimestamp;
	}

	/**
	 * Date/time of last presentation of the communication
	 */
	public void setEndViewTimestamp(OffsetDateTime endViewTimestamp) {
		this.endViewTimestamp = endViewTimestamp;
	}

	/**
	 * Target audience of the communication (depends on communication type)<BR>
	 * The audience may be users names, users logins, users taxpayers ID or users profile names.<BR>
	 * It's also possible to inform multiple audiences separating them by commas.
	 */
	public String getAudience() {
		return audience;
	}

	/**
	 * Target audience of the communication (depends on communication type)<BR>
	 * The audience may be users names, users logins, users taxpayers ID or users profile names.<BR>
	 * It's also possible to inform multiple audiences separating them by commas.
	 */
	public void setAudience(String audience) {
		this.audience = audience;
	}

	/**
	 * Date/time of first view access (depends on communication type and audience)
	 */
	public OffsetDateTime getViewTimestamp() {
		return viewTimestamp;
	}

	/**
	 * Date/time of first view access (depends on communication type and audience)
	 */
	public void setViewTimestamp(OffsetDateTime viewTimestamp) {
		this.viewTimestamp = viewTimestamp;
	}

	/**
	 * User that first viewed this message (depends on communication type and audience)
	 */
	public String getViewUser() {
		return viewUser;
	}

	/**
	 * User that first viewed this message (depends on communication type and audience)
	 */
	public void setViewUser(String viewUser) {
		this.viewUser = viewUser;
	}

	public String getViewIpAddress() {
		return viewIpAddress;
	}

	public void setViewIpAddress(String viewIpAddress) {
		this.viewIpAddress = viewIpAddress;
	}

	/**
	 * Date/time of last modification or creation of any part of this object
	 */
	public OffsetDateTime getChangedTime() {
		return changedTime;
	}

	/**
	 * Date/time of last modification or creation of any part of this object
	 */
	public void setChangedTime(OffsetDateTime changedTime) {
		this.changedTime = changedTime;
	}

	public Communication clone() {
		try {
			return (Communication)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}
}
