/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.dto;

import java.util.Map;

import org.idb.cacao.api.utils.ParserUtils;
import org.idb.cacao.web.controllers.AdvancedSearch;
import org.idb.cacao.web.controllers.AdvancedSearch.QueryFilter;

public class TabulatorFilter {

	private String field;
	private String type;
	private Object value;
	public String getField() {
		return field;
	}
	public void setField(String field) {
		this.field = field;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Object getValue() {
		return value;
	}
	public String getStringValue() {
		return value.toString();
	}
	public Double getDoubleValue() {
		if ( value == null )
			return Double.NaN;
		if ( !ParserUtils.isOnlyNumbers(value.toString()))
			return Double.NaN;
		return Double.parseDouble(value.toString());
	}
	public boolean isString() {
		return value instanceof String;
	}
	public void setValue(Object value) {
		this.value = value;
	}
	public String getProperty(String name) {
		if (value!=null && value instanceof Map) {
			return (String) ((Map<?,?>)value).get(name);
		}
		return null;
	}
	
	public QueryFilter getQueryFilter() {
		if (value instanceof Boolean)
			return new AdvancedSearch.QueryFilterBoolean(field, getStringValue());
		if (value instanceof Map)
		    return new AdvancedSearch.QueryFilterDate(field, getProperty("start"), getProperty("end"));
		if ( "=".equals(type) )
			return new AdvancedSearch.QueryFilterValue(field, getDoubleValue(), getDoubleValue());
		return new AdvancedSearch.QueryFilterTerm(field, getStringValue() + "*");
	}
	
}
