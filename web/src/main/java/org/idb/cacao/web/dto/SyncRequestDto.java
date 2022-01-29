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
