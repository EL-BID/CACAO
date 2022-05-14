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
import static org.springframework.data.elasticsearch.annotations.FieldType.Long;

import java.io.Serializable;
import java.time.OffsetDateTime;

import org.idb.cacao.api.Views;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Keep history of all SYNC requests made from this application
 * @author Gustavo Figueiredo
 *
 */
@Document(indexName="cacao_sync_history")
public class SyncCommitHistory implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * ID's are being generated automatically.
	 * PS: Elasticsearch generates by default 20 character long ID's, that are both URL-safe, base 64 encoded GUID
	 */
	@JsonView(Views.Public.class)
	@Id   
	private String id;

	@JsonView(Views.Public.class)
	@Field(type=Keyword)
	private String master;

	@JsonView(Views.Public.class)
	@Field(type=Keyword)
	private String endPoint;

	@JsonView(Views.Public.class)
	@Field(type=Date, store = true, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
    private OffsetDateTime timeRun;

	@JsonView(Views.Public.class)
	@Field(type=Date, store = true, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
    private OffsetDateTime timeStart;

	@JsonView(Views.Public.class)
	@Field(type=Date, store = true, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
    private OffsetDateTime timeEnd;
	
	@JsonView(Views.Public.class)
	@Field(type=Long)
	private Long countObjects;
	
	@JsonView(Views.Public.class)
	@Field(type=Boolean)
	private Boolean successful;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getMaster() {
		return master;
	}

	public void setMaster(String master) {
		this.master = master;
	}

	public String getEndPoint() {
		return endPoint;
	}

	public void setEndPoint(String endPoint) {
		this.endPoint = endPoint;
	}

	public OffsetDateTime getTimeRun() {
		return timeRun;
	}

	public void setTimeRun(OffsetDateTime timeRun) {
		this.timeRun = timeRun;
	}

	public OffsetDateTime getTimeStart() {
		return timeStart;
	}

	public void setTimeStart(OffsetDateTime timeStart) {
		this.timeStart = timeStart;
	}

	public OffsetDateTime getTimeEnd() {
		return timeEnd;
	}

	public void setTimeEnd(OffsetDateTime timeEnd) {
		this.timeEnd = timeEnd;
	}

	public Long getCountObjects() {
		return countObjects;
	}

	public void setCountObjects(Long countObjects) {
		this.countObjects = countObjects;
	}

	public Boolean getSuccessful() {
		return successful;
	}

	public void setSuccessful(Boolean successful) {
		this.successful = successful;
	}

	public String toString() {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			return objectMapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return super.toString();
		}
	}

}
