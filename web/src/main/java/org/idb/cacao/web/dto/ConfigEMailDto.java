/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
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

	public void setId(long id) {
		this.id = id;
	}

	public long getId() {
		return id;
	}

	public void setSupportEmail(String supportEmail) {
		this.supportEmail = supportEmail;
	}

	public String getSupportEmail() {
		return supportEmail;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getHost() {
		return host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getPort() {
		return port;
	}

	public void setProtocol(EmailProtocol protocol) {
		this.protocol = protocol;
	}

	public EmailProtocol getProtocol() {
		return protocol;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getUsername() {
		return username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPassword() {
		return password;
	}

	public void setAuth(boolean auth) {
		this.auth = auth;
	}

	public boolean isAuth() {
		return auth;
	}

	public void setTls(boolean tls) {
		this.tls = tls;
	}

	public boolean isTls() {
		return tls;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public int getTimeout() {
		return timeout;
	}

}
