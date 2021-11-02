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

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Transient;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.idb.cacao.web.controllers.Views;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;

/**
 * Persistent entity defining a 'user' of this application
 * 
 * @author Gustavo Figueiredo
 *
 */
@Document(indexName="cacao_user")
public class User implements Serializable, Cloneable, Comparable<User> {

	private static final long serialVersionUID = 1L;

	/**
	 * ID's are being generated automatically.
	 * PS: Elasticsearch generates by default 20 character long ID's, that are both URL-safe, base 64 encoded GUID
	 */
	@JsonView(Views.Public.class)
	@Id   
	private String id;

	/**
	 * Date/time this record was created or updated. 
	 * This is important for 'synchronizing' external replicas of this database.
	 */
	@Field(type=Date, store = true, format = DateFormat.date_time)
    private OffsetDateTime timestamp;
	
	/**
	 * Login of this user. This is the same as the user e-mail.
	 */
	@MultiField(
		mainField = @Field(type=Text, fielddata=true),
		otherFields = {
			@InnerField(suffix = "keyword", type=Keyword)
		}
	)
	@JsonView(Views.Public.class)
	@NotBlank
	@NotNull
	@NotEmpty
	@Size(max=60)
	@Email
	private String login;

	/**
	 * User name for displaying on interface
	 */
	@MultiField(
		mainField = @Field(type=Text, fielddata=true),
		otherFields = {
			@InnerField(suffix = "keyword", type=Keyword)
		}
	)
	@JsonView(Views.Public.class)
	@NotBlank
	@NotNull
	@NotEmpty
	@Size(min=4, max=120)
	private String name;
	
	@JsonView(Views.Public.class)
	@Enumerated(EnumType.STRING)
	@Field(type=Keyword)
	@NotNull
	private UserProfile profile;	

	/**
	 * HASH of user password (it's NULL for OAUTH2 login account)
	 */
	@Field(type=Keyword)
	@Size(max=60)
    private String password;
	
	@Transient
	private String confirmPassword;
	
	/**
	 * API TOKEN (for user operations with external system through REST API)
	 */
	@Field(type=Keyword)
	private String apiToken;
	
	@Field(type=Keyword)
	private String taxpayerId;
	
	/**
	 * Page size of last UI request from logged user (not persistent)
	 */
	@Transient
	@JsonIgnore
	private Integer pageSize;
	
	/**
	 * {@link #id}
	 */
	public String getId() {
		return id;
	}

	/**
	 * {@link #id}
	 */
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

	/**
	 * {@link #login}
	 */
	public String getLogin() {
		return login;
	}

	/**
	 * {@link #login}
	 */
	public void setLogin(String login) {
		this.login = login;
	}

	/**
	 * {@link #password}
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * {@link #password}
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * {@link #name}
	 */
	public String getName() {
		return name;
	}

	/**
	 * {@link #name}
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	public UserProfile getProfile() {
		return profile;
	}

	public void setProfile(UserProfile profile) {
		this.profile = profile;
	}

	/**
	 * {@link #apiToken}
	 */
	public String getApiToken() {
		return apiToken;
	}

	/**
	 * {@link #apiToken}
	 */
	public void setApiToken(String apiToken) {
		this.apiToken = apiToken;
	}

	public String getTaxpayerId() {
		return taxpayerId;
	}

	public void setTaxpayerId(String taxpayerId) {
		this.taxpayerId = taxpayerId;
	}
	
	/**
	 * Page size of last UI request from logged user (not persistent)
	 */
	public Integer getPageSize() {
		return pageSize;
	}

	/**
	 * Page size of last UI request from logged user (not persistent)
	 */
	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}

	
	public User clone() {
		try {
			return (User)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}
	
	public int hashCode() {
		return 17 + 37 * (login==null?0:login.hashCode());
	}
	
	public boolean equals(Object o) {
		if (this==o)
			return true;
		if (!(o instanceof User))
			return false;
		User ref = (User)o;
		if (login!=ref.login) {
			if (login==null || ref.login==null)
				return false;
			if (!login.equals(ref.login))
				return false;
		}
		return true;
	}

    @Override
    public String toString() {
        return name;
    }

	@Override
	public int compareTo(User o) {
		if (this==o)
			return 0;
		if (login!=o.login) {
			if (login==null)
				return -1;
			if (o.login==null)
				return 1;
			int comp = login.compareToIgnoreCase(o.login);
			if (comp!=0)
				return comp;
		}
		return 0;
	}

	public String getConfirmPassword() {
		return confirmPassword;
	}

	public void setConfirmPassword(String confirmPassword) {
		this.confirmPassword = confirmPassword;
	}
	
}
