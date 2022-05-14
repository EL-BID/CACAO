/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.account.elements;

/**
 * Enumerates 'Debit' and 'Credit' options
 */
public enum DebitCredit {
	
	D("account.debit"),
	C("account.credit");
	
	private final String display;
	
	DebitCredit(String display) {
		this.display = display;
	}

	@Override
	public String toString() {
		return display;
	}
	
	public static DebitCredit parse(String s) {
		if (s==null || s.trim().length()==0)
			return null;
		char c = s.trim().charAt(0);
		if (c=='D' || c=='d')
			return D;
		if (c=='C' || c=='c')
			return C;
		return null;
	}

}
