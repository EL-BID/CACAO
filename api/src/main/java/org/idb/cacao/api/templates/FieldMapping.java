/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.api.templates;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Enumeration of field mappings that have special meaning for some CACAO functionalities.<BR>
 * 
 * @author Gustavo Figueiredo
 *
 */
public enum FieldMapping implements Comparable<FieldMapping> {

	ANY("field.map.any"),
	
	// All built-in generic field mapping options applicable to any context in tax administration
	TAXPAYER_ID("field.map.tpid"), 	
	TAX_YEAR("field.map.tyear"),		
	TAX_SEMESTER("field.map.tsemester"),	
	TAX_MONTH("field.map.tmonth"),	
	TAX_DAY("field.map.tday"),		
	TAX_VALUE("field.map.tvalue"),						  
	TAX_CODE("field.map.tcode"),		
	TAX_TYPE("field.map.tax");

	private final String display;
	
	FieldMapping(String display) {
		this.display = display;
	}
	
	@Override
	public String toString() {
		return display;
	}
	
	public static FieldMapping parse(String s) {
		if (s==null || s.trim().length()==0)
			return null;
		return Arrays.stream(values()).filter(t->t.name().equalsIgnoreCase(s)).findAny().orElse(null);
	}
	
	public static FieldMapping[] ordered() {
		return Arrays.stream(values()).sorted(Comparator.comparing(FieldMapping::name)).toArray(FieldMapping[]::new);
	}
}
