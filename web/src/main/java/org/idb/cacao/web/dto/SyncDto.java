/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.dto;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Data Transfer Object for SYNC response (master communicates summary about the request)<BR>
 * 
 * @author Gustavo Figueiredo
 *
 */
@JsonInclude(NON_NULL)
public class SyncDto {
	
	/**
	 * Default filename included in ZIP for SYNC responses
	 */
	public static final String DEFAULT_FILENAME = "SYNC.INFO";

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
	private Date requestedStart;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
	private Date requestedEnd;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
	private Date actualStart;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
	private Date actualEnd;

	private long count;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
	private Date nextStart;

	private Long nextLineStart;
	
	public Date getRequestedStart() {
		return requestedStart;
	}

	public void setRequestedStart(Date requestedStart) {
		this.requestedStart = requestedStart;
	}

	public Date getRequestedEnd() {
		return requestedEnd;
	}

	public void setRequestedEnd(Date requestedEnd) {
		this.requestedEnd = requestedEnd;
	}

	public Date getActualStart() {
		return actualStart;
	}

	public void setActualStart(Date actualStart) {
		this.actualStart = actualStart;
	}

	public Date getActualEnd() {
		return actualEnd;
	}

	public void setActualEnd(Date actualEnd) {
		this.actualEnd = actualEnd;
	}

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}

	public Date getNextStart() {
		return nextStart;
	}

	public void setNextStart(Date nextStart) {
		this.nextStart = nextStart;
	}

	public Long getNextLineStart() {
		return nextLineStart;
	}

	public void setNextLineStart(Long nextLineStart) {
		this.nextLineStart = nextLineStart;
	}

	public boolean hasMore() {
		return nextStart!=null && nextStart.getTime()>0;
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
