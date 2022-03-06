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
package org.idb.cacao.web.dto;

import java.io.Serializable;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;

import org.idb.cacao.api.AFieldDescriptor;
import org.idb.cacao.web.entities.ConfigEMail;
import org.idb.cacao.web.entities.ConfigEMail.EmailProtocol;

/**
 * JavaMail Configuration
 */
public class ConfigEMailDto implements Serializable {

	private static final long serialVersionUID = 1L;
	
	public static final long ID_ACTIVE_CONFIG = 1;
	public static final int DEFAULT_PORT = 465;
	public static final EmailProtocol DEFAULT_PROTOCOL = EmailProtocol.SMTP;

	private long id;
	
	@Email
	@NotNull
	@AFieldDescriptor(externalName = "email.support", width = 200)
	private String supportEmail;
	
	@NotNull
	@AFieldDescriptor(externalName = "email.smtp.host", width = 150)
	private String host;
	
	@NotNull
	@AFieldDescriptor(externalName = "email.smtp.port", width = 80)
	private int port;
	
	@NotNull
	@AFieldDescriptor(externalName = "email.smtp.protocol", width = 80)
	private EmailProtocol protocol;
	
	@AFieldDescriptor(externalName = "email.smtp.user.name", width = 150)
	private String username;
	
	@AFieldDescriptor(externalName = "email.smtp.user.password", width = 150)
	private String password;
	
	@NotNull
	@AFieldDescriptor(externalName = "email.smtp.auth", width = 50)
	private boolean auth;
	
	@NotNull
	@AFieldDescriptor(externalName = "email.smtp.tls", width = 50)
	private boolean tls;
	
	@NotNull
	@AFieldDescriptor(externalName = "email.smtp.timeout", width = 150)
	private int timeout;
	
	public ConfigEMailDto() {
		port = DEFAULT_PORT;
		protocol = DEFAULT_PROTOCOL;
	}

	public void updateEntity(ConfigEMail config) {
		config.setId(id);
		config.setSupportEmail(supportEmail);
		config.setHost(host);
		config.setPort(port);
		config.setProtocol(protocol);
		config.setUsername(username);
		config.setPassword(password);
		config.setAuth(auth);
		config.setTls(tls);
		config.setTimeout(timeout);
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

}
