/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.utils;

/**
 * Encapsulate a {@link org.elasticsearch.script.Script}
 * 
 * @author Rivelino Patrício
 * 
 * @since 07/02/2022
 *
 */
public class Script {
	
	private org.elasticsearch.script.Script internalScript;
	
	private String id;
	
	public Script(org.elasticsearch.script.Script script, String id) {
		super();
		this.internalScript = script;
		this.id = id;
	}

	public org.elasticsearch.script.Script getScript() {
		return internalScript;
	}

	public void setScript(org.elasticsearch.script.Script script) {
		this.internalScript = script;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

}
