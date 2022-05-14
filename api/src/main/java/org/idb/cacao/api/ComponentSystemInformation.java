/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.api;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * System information related to this component
 * 
 * @author Gustavo Figueiredo
 *
 */
@JsonInclude(NON_NULL)
public class ComponentSystemInformation {
	
	private String javaVersion;
	
	private String javaHome;
	
	private String osVersion;
	
	private int processorsCount;
	
	private String processorsArch;

	private Long heapUsed;
	
	private Long heapFree;
	
	private Long memUsed;
	
	private Long memFree;
	
	private List<String> installedPlugins;

	public String getJavaVersion() {
		return javaVersion;
	}

	public void setJavaVersion(String javaVersion) {
		this.javaVersion = javaVersion;
	}

	public String getJavaHome() {
		return javaHome;
	}

	public void setJavaHome(String javaHome) {
		this.javaHome = javaHome;
	}

	public String getOsVersion() {
		return osVersion;
	}

	public void setOsVersion(String osVersion) {
		this.osVersion = osVersion;
	}

	public int getProcessorsCount() {
		return processorsCount;
	}

	public void setProcessorsCount(int processorsCount) {
		this.processorsCount = processorsCount;
	}

	public String getProcessorsArch() {
		return processorsArch;
	}

	public void setProcessorsArch(String processorsArch) {
		this.processorsArch = processorsArch;
	}

	public Long getHeapUsed() {
		return heapUsed;
	}

	public void setHeapUsed(Long heapUsed) {
		this.heapUsed = heapUsed;
	}

	public Long getHeapFree() {
		return heapFree;
	}

	public void setHeapFree(Long heapFree) {
		this.heapFree = heapFree;
	}

	public Long getMemUsed() {
		return memUsed;
	}

	public void setMemUsed(Long memUsed) {
		this.memUsed = memUsed;
	}

	public Long getMemFree() {
		return memFree;
	}

	public void setMemFree(Long memFree) {
		this.memFree = memFree;
	}

	public List<String> getInstalledPlugins() {
		return installedPlugins;
	}

	public void setInstalledPlugins(List<String> installedPlugins) {
		this.installedPlugins = installedPlugins;
	}
	
}
