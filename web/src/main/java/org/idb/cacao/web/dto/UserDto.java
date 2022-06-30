/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.dto;

import java.io.Serializable;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.idb.cacao.api.AFieldDescriptor;
import org.idb.cacao.api.Views;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.entities.UserProfile;

import com.fasterxml.jackson.annotation.JsonView;

/**
 * Persistent entity defining a 'user' of this application
 * 
 * @author Gustavo Figueiredo
 * @author Luis Kauer
 *
 */
public class UserDto implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * ID's are being generated automatically. PS: Elasticsearch generates by
	 * default 20 character long ID's, that are both URL-safe, base 64 encoded GUID
	 */
	@JsonView(Views.Public.class)
	@AFieldDescriptor(externalName = "doc.id")
	private String id;

	/**
	 * Login of this user. This is the same as the user e-mail.
	 */
	@JsonView(Views.Public.class)
	@NotBlank
	@NotNull
	@NotEmpty
	@Size(max = 60)
	@Email
	@AFieldDescriptor(externalName = "user.login", audit = true)
	private String login;

	/**
	 * User name for displaying on interface
	 */
	@JsonView(Views.Public.class)
	@NotBlank
	@NotNull
	@NotEmpty
	@Size(min = 4, max = 120)
	@AFieldDescriptor(externalName = "user.name", audit = true)
	private String name;

	@JsonView(Views.Public.class)
	@NotNull
	@AFieldDescriptor(externalName = "user.profile")
	private UserProfile profile;

	@Size(max = 60)
	@AFieldDescriptor(externalName = "user.password")
	private String password;

	@AFieldDescriptor(externalName = "user.password.confirm")
	private String confirmPassword;

	@JsonView(Views.Public.class)
	@AFieldDescriptor(externalName = "taxpayer.id")
	private String taxpayerId;

	@JsonView(Views.Public.class)
	private boolean active = true;

	public UserDto() {
	}

	public UserDto(User user) {
		this.id = user.getId();
		this.login = user.getLogin();
		this.name = user.getName();
		this.profile = user.getProfile();
		this.taxpayerId = user.getTaxpayerId();
		this.active = user.isActive();
	}

	public void updateEntity(User entity) {
		entity.setName(name);
		entity.setLogin(login);
		entity.setProfile(profile);
		entity.setTaxpayerId(taxpayerId);
		entity.setActive(active);
	}

	public String getId() {
		return id;
	}

	public String getLogin() {
		return login;
	}

	public String getPassword() {
		return password;
	}

	public String getName() {
		return name;
	}

	public String getTaxpayerId() {
		return taxpayerId;
	}
	
	public String getConfirmPassword() {
		return confirmPassword;
	}
	
	public boolean isActive() {
		return active;
	}
	
	public UserProfile getProfile() {
		if (profile == null)
			return UserProfile.DECLARANT;
		return profile;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public void setLogin(String login) {
		this.login = login;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setProfile(UserProfile profile) {
		this.profile = profile;
	}

	public void setTaxpayerId(String taxpayerId) {
		this.taxpayerId = taxpayerId;
	}

	@Override
	public String toString() {
		return name;
	}

	public void setConfirmPassword(String confirmPassword) {
		this.confirmPassword = confirmPassword;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

}
