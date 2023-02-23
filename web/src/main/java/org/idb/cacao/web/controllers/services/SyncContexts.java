/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.services;

import java.util.Collection;

/**
 * Different contexts for SYNC operations
 * 
 * @author Gustavo Figueiredo
 *
 */
public enum SyncContexts {
	
	ORIGINAL_FILES("/api/sync/original-files", false),
	
	REPOSITORY_ENTITIES("/api/sync/base/", true),
	
	VALIDATED_DATA("/api/sync/validated/", true),

	PUBLISHED_DATA("/api/sync/published/", true),

	KIBANA_ASSETS("/api/sync/kibana", false);

	private final String endpoint;
	private final boolean requestPath;
	
	/**
	 * Special value used as 'endpoint' meaning that 'all contexts' are implied (i.e.: it should SYNC on every context available)
	 */
	public static final String ALL_ENDPOINTS = "ALL";
	
	SyncContexts(String endpoint, boolean requestPath) {
		this.endpoint = endpoint;
		this.requestPath = requestPath;
	}
	
	/**
	 * API endpoint for this context in case it's fixed (i.e. without some additional path indicating a variation in this context) 
	 */
	public String getEndpoint() {
		return endpoint;
	}

	/**
	 * API endpoint for this context including some additional path indicating a variation in this contexts (unless this context doesn't requires one)
	 * @param pathParameter Additional path component. May be ignored if the context doesn't require this.
	 */
	public String getEndpoint(String pathParameter) {
		if (!requestPath || pathParameter==null || pathParameter.trim().length()==0)
			return endpoint;
		else
			return endpoint+pathParameter;
	}

	/**
	 * API endpoint for this context including some additional path indicating a variation in this contexts (unless this context doesn't requires one)
	 * @param pathParameter1 First additional path component.
	 * @param pathParameter2 Second additional path component.
	 */
	public String getEndpoint(String pathParameter1, String pathParameter2) {
		if (!requestPath)
			return endpoint;
		StringBuilder sb = new StringBuilder(endpoint);
		sb.append(pathParameter1);
		if (!pathParameter1.endsWith("/") && !pathParameter2.startsWith("/"))
			sb.append("/");
		sb.append(pathParameter2);
		return sb.toString();
	}

	/**
	 * Indicates if this context requires an additional path component indicating a variation in this context
	 */
	public boolean hasRequestPath() {
		return requestPath;
	}

	public String toString() {
		return endpoint;
	}
	
	public static SyncContexts parse(String name) {
		if (name==null || name.trim().length()==0)
			return null;
		for (SyncContexts c: values()) {
			if (c.name().equalsIgnoreCase(name) || c.getEndpoint().equalsIgnoreCase(name))
				return c;
		}
		return null;
	}
	
	public static boolean hasContext(Collection<String> names, SyncContexts lookFor) {
		if (names==null || names.isEmpty() || lookFor==null)
			return false;
		if (names.size()==1 && ALL_ENDPOINTS.equals(names.iterator().next()))
			return true;
		for (String name: names) {
			if (lookFor.name().equalsIgnoreCase(name) || lookFor.getEndpoint().equalsIgnoreCase(name))
				return true;
		}
		return false;
	}
	
	public static boolean hasContext(Collection<String> names, SyncContexts lookFor, String requestParameter) {
		if (names==null || names.isEmpty() || lookFor==null)
			return false;
		if (names.size()==1 && ALL_ENDPOINTS.equals(names.iterator().next()))
			return true;
		for (String name: names) {
			if (String.join(".",lookFor.name(), requestParameter).equalsIgnoreCase(name) || lookFor.getEndpoint(requestParameter).equalsIgnoreCase(name))
				return true;
		}
		return false;
	}

}
