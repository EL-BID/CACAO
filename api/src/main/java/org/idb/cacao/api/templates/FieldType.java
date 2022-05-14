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
 * Enumeration of field types for validation of incoming files.<BR>
 * 
 * @author Gustavo Figueiredo
 *
 */
public enum FieldType implements Comparable<FieldType> {

	GENERIC("field.type.generic"), // generic means anything (no validation at all)
	CHARACTER("field.type.char"),	
	INTEGER("field.type.int"), 	// any type of integer (tiny, short, integer or long)
	DECIMAL("field.type.decimal"),	// any type of decimal (float, double, decimal)	
	BOOLEAN("field.type.bool"),	
	TIMESTAMP("field.type.timestamp"),		
	DATE("field.type.date"),						  
	MONTH("field.type.month"),		
	DOMAIN("field.type.domain"),
	NESTED("field.type.nested");
	
	private final String display;
	
	FieldType(String display) {
		this.display = display;
	}
	
	@Override
	public String toString() {
		return display;
	}
	
	public static FieldType parse(String s) {
		if (s==null || s.trim().length()==0)
			return null;
		return Arrays.stream(values()).filter(t->t.name().equalsIgnoreCase(s)).findAny().orElse(null);
	}
	
	public static FieldType[] ordered() {
		return Arrays.stream(values()).sorted(Comparator.comparing(FieldType::name)).toArray(FieldType[]::new);
	}

}
