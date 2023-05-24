/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.entities;

/**
 * Enumeration of simple user profiles.
 * In this application we are allowing only one profile to each user.
 * 
 * @author Gustavo Figueiredo
 */
public enum UserProfile {
	
	SYSADMIN("user.profile.sysadmin"),
	MASTER("user.profile.master"),
	SUPPORT("user.profile.support"),
	AUTHORITY("user.profile.authority"),
	DECLARANT("user.profile.declarant"),
	READONLY("user.profile.readonly");

	private final String display;
	
	UserProfile(String display) {
		this.display = display;
	}
	
	/**
	 * Return the role name associated to this user profile
	 */
	public String getRole() {
		// IMPORTANT:
		// All role names must be prefixed with 'ROLE_' since it's the Spring standard (otherwise
		// we would need to add more customizations to Spring configuration)
		return "ROLE_"+name();
	}

	@Override
	public String toString() {
		return display;
	}
	
	public static UserProfile parse(String text) {
		if (text==null || text.length()==0)
			return null;
		for (UserProfile p: values()) {
			if (p.name().equalsIgnoreCase(text)
				|| p.display.equalsIgnoreCase(text)
				|| p.getRole().equalsIgnoreCase(text))
				return p;
		}
		return null;
	}
}
