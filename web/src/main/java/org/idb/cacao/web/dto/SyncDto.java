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

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
	private Date nextStart;

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
