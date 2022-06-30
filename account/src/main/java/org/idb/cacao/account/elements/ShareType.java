/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.account.elements;

/**
 * Enumerates share type options
 */
public enum ShareType {

	ORDINARY("share.type.ord"),
	PREFERENCE("share.type.prf");
	
	private final String display;
	
	ShareType(String display) {
		this.display = display;
	}

	@Override
	public String toString() {
		return display;
	}
	
	public String getKey() {
		return name().substring(0,1);
	}
	
	public static ShareType parse(String s) {
		if (s==null || s.trim().length()==0)
			return null;
		char c = s.trim().charAt(0);
		if (c=='O' || c=='o')
			return ORDINARY;
		if (c=='P' || c=='p')
			return PREFERENCE;
		return null;
	}

}
