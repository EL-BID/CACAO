/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.utils.generators;

import java.util.Random;

/**
 * Examples of 'legal entities' for generating random data.
 * 
 * @author Gustavo Figueiredo
 *
 */
public enum SampleLegalEntities {

	SOLE("Sole Proprietorship"),
	
	PARTNER("Partnership"),
	
	CORP("Corporation"),
	
	SCORP("S Corporation"),
	
	LLC("Limited Liability Company");
	
	private final String name;
	
	SampleLegalEntities(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public static SampleLegalEntities sample(Random random) {
		return values()[random.nextInt(values().length)];
	}

}
