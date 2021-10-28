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
package org.idb.cacao.web.controllers.services;

import java.util.Collection;

/**
 * Different contexts for SYNC operations
 * 
 * @author Gustavo Figueiredo
 *
 */
public enum SyncContexts {
	
	ORIGINAL_FILES("/api/sync/original_files", false),
	
	TEMPLATE_FILES("/api/sync/template_files", false),
	
	SAMPLE_FILES("/api/sync/sample_files", false),

	GENERIC_FILES("/api/sync/generic_files", false),

	REPOSITORY_ENTITIES("/api/sync/base/", true),
	
	PARSED_DOCUMENTS("/api/sync/docs/", true),
	
	KIBANA_ASSETS("/api/sync/kibana", false);

	private final String endpoint;
	private final boolean requestPath;
	
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
		for (String name: names) {
			if (lookFor.name().equalsIgnoreCase(name) || lookFor.getEndpoint().equalsIgnoreCase(name))
				return true;
		}
		return false;
	}
	
	public static boolean hasContext(Collection<String> names, SyncContexts lookFor, String requestParameter) {
		if (names==null || names.isEmpty() || lookFor==null)
			return false;
		for (String name: names) {
			if (String.join(".",lookFor.name(), requestParameter).equalsIgnoreCase(name) || lookFor.getEndpoint(requestParameter).equalsIgnoreCase(name))
				return true;
		}
		return false;
	}

}
