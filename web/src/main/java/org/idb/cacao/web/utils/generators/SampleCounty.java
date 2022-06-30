/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.utils.generators;

import java.util.Random;

/**
 * Examples of 'county' for generating random data.
 * 
 * @author Gustavo Figueiredo
 *
 */
public enum SampleCounty {
	
	EAST("East County"),
	
	WEST("Weast County"),
	
	NORTH("North County"),
	
	SOUTH("South County"),
	
	CENTRAL("Central County");
	
	private final String name;
	
	SampleCounty(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

	public static SampleCounty sample(Random random) {
		return values()[random.nextInt(values().length)];
	}

}
