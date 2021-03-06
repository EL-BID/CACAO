/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.entities;

import static org.springframework.data.elasticsearch.annotations.FieldType.Boolean;
import static org.springframework.data.elasticsearch.annotations.FieldType.Date;
import static org.springframework.data.elasticsearch.annotations.FieldType.Keyword;
import static org.springframework.data.elasticsearch.annotations.FieldType.Text;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Transient;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.idb.cacao.api.AFieldDescriptor;
import org.idb.cacao.api.Views;
import org.idb.cacao.api.utils.StringUtils;
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
	@AFieldDescriptor(externalName = "doc.id")
	private String id;

	/**
	 * Date/time this record was created or updated. 
	 * This is important for 'synchronizing' external replicas of this database.
	 */
	@Field(type=Date, store = true, format = DateFormat.date_time)
	@AFieldDescriptor(externalName = "saved.time")
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
	@AFieldDescriptor(externalName = "user.login", audit=true)
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
	@AFieldDescriptor(externalName = "user.name", audit=true)
	private String name;
	
	@JsonView(Views.Public.class)
	@Enumerated(EnumType.STRING)
	@MultiField(
			mainField = @Field(type=Text, fielddata=true),
			otherFields = {
				@InnerField(suffix = "keyword", type=Keyword)
			}
		)
	@NotNull
	@AFieldDescriptor(externalName = "user.profile")
	private UserProfile profile;	

	/**
	 * HASH of user password (it's NULL for OAUTH2 login account)
	 */
	@Field(type=Keyword)
	@Size(max=60)
	@AFieldDescriptor(externalName = "user.password")
    private String password;
	
	@Transient
	@AFieldDescriptor(externalName = "user.password.confirm")
	private String confirmPassword;
	
	/**
	 * API TOKEN (for user operations with external system through REST API)
	 */
	@Field(type=Keyword)
	@AFieldDescriptor(externalName = "user.token")
	private String apiToken;
	
	/**
	 * Token granting access to the Kibana User Interface. It's actually a randomly
	 * generated password created at the Kibana user management internal space and
	 * assigned automatically to this user.
	 */
	@Field(type=Keyword)
	@AFieldDescriptor(externalName = "user.kibana.token")
	private String kibanaToken;

	/**
	 * The name (id) of the internal Kibana Space created for this user (his private space
	 * at Kibana interface)
	 */
	@Field(type=Keyword)
	@AFieldDescriptor(externalName = "user.kibana.space")
	private String kibanaSpace;

	@JsonView(Views.Public.class)
	@Field(type=Keyword)
	@AFieldDescriptor(externalName = "taxpayer.id")
	private String taxpayerId;
	
	/**
	 * Date/time of last modification or creation of any part of this object
	 */
	@Field(type=Date, store = true, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
	@AFieldDescriptor(externalName = "changed.time")
	private OffsetDateTime changedTime;	
	
	/**
	 * Page size of last UI request from logged user (not persistent)
	 */
	@Transient
	@JsonIgnore
	private Integer pageSize;
	
	@Field(type=Boolean)
	@JsonView(Views.Public.class)
	private boolean active=true;
	
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
		if (profile==null)
			return UserProfile.DECLARANT;
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

	/**
	 * Token granting access to the Kibana User Interface. It's actually a randomly
	 * generated password created at the Kibana user management internal space and
	 * assigned automatically to this user.
	 */
	public String getKibanaToken() {
		return kibanaToken;
	}

	/**
	 * Token granting access to the Kibana User Interface. It's actually a randomly
	 * generated password created at the Kibana user management internal space and
	 * assigned automatically to this user.
	 */
	public void setKibanaToken(String kibanaToken) {
		this.kibanaToken = kibanaToken;
	}

	/**
	 * The name (id) of the internal Kibana Space created for this user (his private space
	 * at Kibana interface)
	 */
	public String getKibanaSpace() {
		return kibanaSpace;
	}

	/**
	 * The name (id) of the internal Kibana Space created for this user (his private space
	 * at Kibana interface)
	 */
	public void setKibanaSpace(String kibanaSpace) {
		this.kibanaSpace = kibanaSpace;
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
		if (!Objects.equals(login, ref.login)) {
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
		int c = StringUtils.compareCaseInsensitive(login, o.login);
		if (c != 0)
			return c;
		return 0;
	}

	public String getConfirmPassword() {
		return confirmPassword;
	}

	public void setConfirmPassword(String confirmPassword) {
		this.confirmPassword = confirmPassword;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}
