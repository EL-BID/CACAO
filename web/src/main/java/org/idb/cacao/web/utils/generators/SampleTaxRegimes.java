/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.utils.generators;

import java.util.Random;

/**
 * Examples of 'tax regimes' for generating random data.
 * 
 * @author Gustavo Figueiredo
 *
 */
public enum SampleTaxRegimes {

	REAL_PROFIT("Real profit"),
	
	PRESUMED_PROFIT("Presumed profit"),
	
	SIMPLE("Simplified");
	
	private final String name;
	
	SampleTaxRegimes(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

	public static SampleTaxRegimes sample(Random random) {
		return values()[random.nextInt(values().length)];
	}

}
