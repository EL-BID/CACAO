/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.api;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.idb.cacao.api.utils.ResourceMonitor;
import org.springframework.core.env.Environment;

/**
 * Common application definitions for CACAO micro-services
 */
public class CommonApplication {

	static final Logger log = Logger.getLogger(CommonApplication.class.getName());

	protected Environment env;
	
	protected ResourceMonitor<? extends SystemMetrics> resourceMonitorService;
	
	public CommonApplication(Environment env, ResourceMonitor<? extends SystemMetrics> resourceMonitorService) {
		this.env = env;
		this.resourceMonitorService = resourceMonitorService;
	}

	/**
	 * Initialization code for the web application
	 */
	public void runStartupCodeAsync() {
		
	    new Thread("StartupThread") {
	    	{	setDaemon(true); }
	    	
	    	@Override
	    	public void run() {
    			startupCode();
	    	}
	    }.start();
	}

	/**
	 * Do some initialization here
	 */
	public void startupCode() {
		
		try {
			if ("true".equalsIgnoreCase(env.getProperty("resource.monitor"))) {
				resourceMonitorService.start();
			}
		}
		catch (Exception ex) {
			log.log(Level.SEVERE, "Error during initialization", ex);
		}

	}

}
