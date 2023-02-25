/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.api;

import static org.springframework.data.elasticsearch.annotations.FieldType.*;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

import com.fasterxml.jackson.annotation.JsonView;

/**
 * Information required for audit trail over user actions on this application
 * 
 * @author Gustavo Figueiredo
 *
 */
@Document(indexName="cacao_audit_trail")
public class AuditTrail implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;

	/**
	 * ID's are being generated automatically.
	 * PS: Elasticsearch generates by default 20 character long ID's, that are both URL-safe, base 64 encoded GUID
	 */
	@Id	
	private String id;

	/**
	 * Timestamp of this log entry
	 */
	@JsonView(Views.Authority.class)
	@Field(type=Date, store = true, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
	@AFieldDescriptor(externalName = "audit.log.timestamp")
    private OffsetDateTime timestamp;

	/**
	 * Authenticated user login
	 */
	@JsonView(Views.Authority.class)
	@MultiField(
			mainField = @Field(type=Text, fielddata=true),
			otherFields = {
				@InnerField(suffix = "keyword", type=Keyword)
			}
		)
	@AFieldDescriptor(externalName = "audit.log.user.login")
	private String userLogin;

	/**
	 * Authenticated user name
	 */
	@JsonView(Views.Authority.class)
	@MultiField(
			mainField = @Field(type=Text, fielddata=true),
			otherFields = {
				@InnerField(suffix = "keyword", type=Keyword)
			}
		)
	@AFieldDescriptor(externalName = "audit.log.user.name")
	private String userName;

	/**
	 * IP Address of the user
	 */
	@JsonView(Views.Authority.class)
	@Field(type=Keyword, store = true)
	@AFieldDescriptor(externalName = "audit.log.ip.address")
	private String ipAddress;

	/**
	 * Requested HTTP Method
	 */
	@JsonView(Views.Authority.class)
	@Field(type=Keyword, store = true)
	@AFieldDescriptor(externalName = "audit.log.http.method")
	private String httpMethod;

	/**
	 * Requested URL
	 */
	@JsonView(Views.Authority.class)
	@Field(type=Keyword, store = true)
	@AFieldDescriptor(externalName = "audit.log.url")
	private String url;

	/**
	 * Controller class internal name
	 */
	@JsonView(Views.Authority.class)
	@MultiField(
			mainField = @Field(type=Text, fielddata=true),
			otherFields = {
				@InnerField(suffix = "keyword", type=Keyword)
			}
		)
	@AFieldDescriptor(externalName = "audit.log.controller.class")
	private String controllerClass;

	/**
	 * Controller method internal name
	 */
	@JsonView(Views.Authority.class)
	@MultiField(
			mainField = @Field(type=Text, fielddata=true),
			otherFields = {
				@InnerField(suffix = "keyword", type=Keyword)
			}
		)
	@AFieldDescriptor(externalName = "audit.log.controller.method")
	private String controllerMethod;

	@Enumerated(EnumType.STRING)
	@MultiField(
			mainField = @Field(type=Text, fielddata=true),
			otherFields = {
				@InnerField(suffix = "keyword", type=Keyword)
			}
		)
	@AFieldDescriptor(externalName = "audit.log.auth.method")
	private AuthenticationMethod authMethod;

	/**
	 * Parameters of the user request
	 */
	@Field(type=Nested, store = true)
	private Map<String, String> param;

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

	public String getUserLogin() {
		return userLogin;
	}

	public void setUserLogin(String userLogin) {
		this.userLogin = userLogin;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getHttpMethod() {
		return httpMethod;
	}

	public void setHttpMethod(String httpMethod) {
		this.httpMethod = httpMethod;
	}

	public String getControllerClass() {
		return controllerClass;
	}

	public void setControllerClass(String controllerClass) {
		this.controllerClass = controllerClass;
	}

	public String getControllerMethod() {
		return controllerMethod;
	}

	public void setControllerMethod(String controllerMethod) {
		this.controllerMethod = controllerMethod;
	}

	public AuthenticationMethod getAuthMethod() {
		return authMethod;
	}

	public void setAuthMethod(AuthenticationMethod authMethod) {
		this.authMethod = authMethod;
	}

	public Map<String, String> getParam() {
		return param;
	}

	public void setParam(Map<String, String> param) {
		this.param = param;
	}
	
	public void addParam(String paramName, String paramValue) {
		if (paramName==null || paramName.trim().length()==0)
			return;
		if (param==null)
			param = new HashMap<>();
		param.put(paramName.trim(), paramValue);
	}
	
	public AuditTrail clone() {
		try {
			return (AuditTrail)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		return String.join(" ", userLogin, ipAddress, httpMethod, url);
	}
	
	public int hashCode() {
		return 17 + 37 * (id==null ? 0 : id.hashCode());
	}
	
	public boolean equals(Object o) {
		if (this==o)
			return true;
		if (!(o instanceof AuditTrail))
			return false;
		AuditTrail ref = (AuditTrail)o;
		return Objects.equals(id, ref.id);
	}

}
