/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.dto;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Data Transfer Object for SYNC request (user interacting with a backup environment in order to start SYNC operation)<BR>
 * 
 * @author Gustavo Figueiredo
 *
 */
@JsonInclude(NON_NULL)
public class SyncRequestDto {

	/**
	 * If equals to TRUE, tells to ignore previous committed data and start SYNC all over again. It DOES NOT
	 * deletes old data. The new data will simply overwrite existing one.
	 * Default is FALSE, which means we will consider previous committed data and SYNC will try to fetch recent
	 * data only.
	 */
	private Boolean fromStart;
	
	/**
	 * Optional parameter indicating the names of endpoints we should consider in this SYNC. If not provided,
	 * will SYNC all possible endpoints.
	 */
	private List<String> endpoints;

	/**
	 * If equals to TRUE, tells to ignore previous committed data and start SYNC all over again. It DOES NOT
	 * deletes old data. The new data will simply overwrite existing one.
	 * Default is FALSE, which means we will consider previous committed data and SYNC will try to fetch recent
	 * data only.
	 */
	public Boolean getFromStart() {
		return fromStart;
	}

	/**
	 * If equals to TRUE, tells to ignore previous committed data and start SYNC all over again. It DOES NOT
	 * deletes old data. The new data will simply overwrite existing one.
	 * Default is FALSE, which means we will consider previous committed data and SYNC will try to fetch recent
	 * data only.
	 */
	public void setFromStart(Boolean fromStart) {
		this.fromStart = fromStart;
	}

	/**
	 * Optional parameter indicating the names of endpoints we should consider in this SYNC. If not provided,
	 * will SYNC all possible endpoints.
	 */
	public List<String> getEndpoints() {
		return endpoints;
	}

	/**
	 * Optional parameter indicating the names of endpoints we should consider in this SYNC. If not provided,
	 * will SYNC all possible endpoints.
	 */
	public void setEndpoints(List<String> endpoints) {
		this.endpoints = endpoints;
	}
	
	
}
