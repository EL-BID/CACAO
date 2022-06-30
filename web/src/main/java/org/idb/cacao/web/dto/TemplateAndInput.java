/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.dto;

/**
 * DTO used to pass document template values to UI
 * 
 * @author Rivelino Patrício
 * 
 * @since 10/03/2022
 *
 */
public class TemplateAndInput {
	
	private String templateName;
	
	private String version;
	
	private String inputName;
	
	private String inputNameDisplay;	

	public String getTemplateName() {
		return templateName;
	}

	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getInputName() {
		return inputName;
	}

	public void setInputName(String inputName) {
		this.inputName = inputName;
	}

	public String getInputNameDisplay() {
		return inputNameDisplay;
	}

	public void setInputNameDisplay(String inputNameDisplay) {
		this.inputNameDisplay = inputNameDisplay;
	}

	@Override
	public String toString() {
		return templateName + "=" + version + "=" + inputName;
	}
	
}
