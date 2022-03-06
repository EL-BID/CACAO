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

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.idb.cacao.api.utils.ParserUtils;
import org.idb.cacao.web.entities.ConfigSync;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Thread prototype used for execution of SYNCHRONIZE task locally (i.e. the local server
 * will copy data from another server)<BR>
 * 
 * There may be only one instance of SyncThread running locally.
 * 
 * @author Gustavo Figueiredo
 *
 */
@Component
@Scope("prototype")
public class SyncThread implements Runnable {

	private static final Logger log = Logger.getLogger(SyncThread.class.getName());

	private String user;
	private Date start;
	private boolean resumeFromLastSync;
	private List<String> endpoints;
	
	private final AtomicBoolean running;
	
	@Autowired
	private SyncAPIService syncAPIService;

	@Autowired
	private FieldsConventionsService fieldsConventionsService;

	@Autowired
	private ConfigSyncService configSyncService;

	public SyncThread() {
		this.running = new AtomicBoolean(true);
	}
	
	public void setUser(String user) {
		this.user = user;
	}

	public void setStart(Date start) {
		this.start = start;
	}

	public boolean isResumeFromLastSync() {
		return resumeFromLastSync;
	}

	public void setResumeFromLastSync(boolean resumeFromLastSync) {
		this.resumeFromLastSync = resumeFromLastSync;
	}

	public List<String> getEndpoints() {
		return endpoints;
	}

	public void setEndpoints(List<String> endpoints) {
		this.endpoints = endpoints;
	}

	public void run() {
		try {
			ConfigSync config = configSyncService.getActiveConfig();
			if (config==null) {
				log.log(Level.WARNING, "Missing SYNC configuration for SyncThread started by user "+user+" at "+ParserUtils.formatTimestamp(start));
				return;
			}
			
			final String master = config.getMaster();
			if (master==null || master.trim().length()==0) {
				log.log(Level.WARNING, "Missing 'master' at SYNC configuration for SyncThread started by user "+user+" at "+ParserUtils.formatTimestamp(start));
				return;
			}
			
			final String api_token = config.getApiToken();
			if (api_token==null || api_token.trim().length()==0) {
				log.log(Level.WARNING, "Missing 'API token' at SYNC configuration for SyncThread started by user "+user+" at "+ParserUtils.formatTimestamp(start));
				return;
			}
			
			final String api_token_decrypted = configSyncService.decryptToken(api_token);
			
			log.log(Level.INFO, "Start of SYNC (user="+user+", resumeFromLastSync="+resumeFromLastSync+", endpoints="+endpoints+")");

			if (endpoints!=null && !endpoints.isEmpty())
				syncAPIService.syncSome(api_token_decrypted, resumeFromLastSync, endpoints);
			else
				syncAPIService.syncAll(api_token_decrypted, resumeFromLastSync);
			
			log.log(Level.INFO, "End of SYNC");
		}
		catch (Exception ex) {
			log.log(Level.SEVERE, "Error in SyncThread started by user "+user+" at "+ParserUtils.formatTimestamp(start), ex);
		}
		finally {
			running.set(false);
		}
	}

	public String getUser() {
		return user;
	}

	public Date getStart() {
		return start;
	}
	
	public String getStartFormatted() {
		return fieldsConventionsService.formatValue(getStart());
	}
	
	public boolean isRunning() {
		return running.get();
	}

}
