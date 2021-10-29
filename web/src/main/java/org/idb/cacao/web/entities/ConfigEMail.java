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

import static org.springframework.data.elasticsearch.annotations.FieldType.Text;

import java.io.Serializable;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;

import org.idb.cacao.web.utils.AFieldDescriptor;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

/**
 * JavaMail Configuration
 */
@Document(indexName="cacao_config_email")
public class ConfigEMail implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;
	
	public static final long ID_ACTIVE_CONFIG = 1;
	public static final int DEFAULT_PORT = 465;
	public static final EmailProtocol DEFAULT_PROTOCOL = EmailProtocol.SMTP;

	@Id   
	private long id;
	
	@Email
	@Field(type=Text)
	@NotNull
	@AFieldDescriptor(externalName = "email.support", width = 200)
	private String supportEmail;
	
	@Field(type=Text)
	@NotNull
	@AFieldDescriptor(externalName = "email.smtp.host", width = 150)
	private String host;
	
	@Field(type=Text)
	@NotNull
	@AFieldDescriptor(externalName = "email.smtp.port", width = 80)
	private int port;
	
	@Enumerated(EnumType.STRING)
	@NotNull
	@Field(type=Text)
	@AFieldDescriptor(externalName = "email.smtp.protocol", width = 80)
	private EmailProtocol protocol;
	
	@Field(type=Text)
	@AFieldDescriptor(externalName = "email.smtp.user.name", width = 150)
	private String username;
	
	@Field(type=Text)
	@AFieldDescriptor(externalName = "email.smtp.user.password", width = 150)
	private String password;
	
	@Field(type=Text)
	@NotNull
	@AFieldDescriptor(externalName = "email.smtp.auth", width = 50)
	private boolean auth;
	
	@Field(type=Text)
	@NotNull
	@AFieldDescriptor(externalName = "email.smtp.tls", width = 50)
	private boolean tls;
	
	@Field(type=Text)
	@NotNull
	@AFieldDescriptor(externalName = "email.smtp.timeout", width = 150)
	private int timeout;
	
	public ConfigEMail() {
		port = DEFAULT_PORT;
		protocol = DEFAULT_PROTOCOL;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getSupportEmail() {
		return supportEmail;
	}

	public void setSupportEmail(String supportEmail) {
		this.supportEmail = supportEmail;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public EmailProtocol getProtocol() {
		return protocol;
	}

	public void setProtocol(EmailProtocol protocol) {
		this.protocol = protocol;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isAuth() {
		return auth;
	}

	public void setAuth(boolean auth) {
		this.auth = auth;
	}

	public boolean isTls() {
		return tls;
	}

	public void setTls(boolean tls) {
		this.tls = tls;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public static enum EmailProtocol {
		
		SMTP("smtp"),
		SMTPS("smtps");

		private final String protocol;
		
		EmailProtocol(String protocol) {
			this.protocol = protocol;
		}

		@Override
		public String toString() {
			return protocol;
		}
		
	}
}
