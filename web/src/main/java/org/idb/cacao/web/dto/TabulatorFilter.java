/*******************************************************************************
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in the Software without 
 * restriction, including without limitation the rights to use, copy, modify, merge, publish, 
 * distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or 
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS 
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR 
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN 
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.dto;

import java.util.Map;

import org.idb.cacao.web.controllers.AdvancedSearch;
import org.idb.cacao.web.controllers.AdvancedSearch.QueryFilter;
import org.idb.cacao.web.controllers.AdvancedSearch.QueryFilterTerm;

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
		return new AdvancedSearch.QueryFilterTerm(field, getStringValue() + "*");
	}
	
}
