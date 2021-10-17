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
	
}
