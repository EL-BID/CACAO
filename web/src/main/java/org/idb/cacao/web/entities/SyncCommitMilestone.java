/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.entities;

import static org.springframework.data.elasticsearch.annotations.FieldType.Date;
import static org.springframework.data.elasticsearch.annotations.FieldType.Keyword;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

import javax.persistence.Id;
import javax.validation.constraints.NotNull;

import org.idb.cacao.api.Views;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

import com.fasterxml.jackson.annotation.JsonView;

/**
 * Sync commit tracking information (for subscribers/slaves)
 */
@Document(indexName="cacao_sync_commit_milestone")
public class SyncCommitMilestone implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;

	/**
	 * ID's are being generated automatically.
	 * PS: Elasticsearch generates by default 20 character long ID's, that are both URL-safe, base 64 encoded GUID
	 */
	@JsonView(Views.Public.class)
	@Id   
	private String id;

	/**
	 * Committed endpoint
	 */
	@JsonView(Views.Public.class)
	@Field(type=Keyword)
	@NotNull
	private String endPoint;

	/**
	 * Date/time of last SYNC
	 */
	@JsonView(Views.Public.class)
	@Field(type=Date, store = true, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
    private OffsetDateTime lastTimeRun;
	
	/**
	 * Date/time of start of the period of last SYNC
	 */
	@JsonView(Views.Public.class)
	@Field(type=Date, store = true, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
    private OffsetDateTime lastTimeStart;

	/**
	 * Date/time of end of the period of last SYNC
	 */
	@JsonView(Views.Public.class)
	@Field(type=Date, store = true, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
    private OffsetDateTime lastTimeEnd;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getEndPoint() {
		return endPoint;
	}

	public void setEndPoint(String endPoint) {
		this.endPoint = endPoint;
	}

	public OffsetDateTime getLastTimeRun() {
		return lastTimeRun;
	}

	public void setLastTimeRun(OffsetDateTime lastTimeRun) {
		this.lastTimeRun = lastTimeRun;
	}

	public OffsetDateTime getLastTimeStart() {
		return lastTimeStart;
	}

	public void setLastTimeStart(OffsetDateTime lastTimeStart) {
		this.lastTimeStart = lastTimeStart;
	}

	public OffsetDateTime getLastTimeEnd() {
		return lastTimeEnd;
	}

	public void setLastTimeEnd(OffsetDateTime lastTimeEnd) {
		this.lastTimeEnd = lastTimeEnd;
	}

	public SyncCommitMilestone clone() {
		try {
			return (SyncCommitMilestone)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	public int hashCode() {
		return 17 + 37 * (endPoint==null?0:endPoint.hashCode());
	}
	
	public boolean equals(Object o) {
		if (this==o)
			return true;
		if (!(o instanceof SyncCommitMilestone))
			return false;
		SyncCommitMilestone ref = (SyncCommitMilestone)o;
		return Objects.equals(endPoint, ref.endPoint);
	}
	
    @Override
    public String toString() {
        return endPoint;
    }

}
