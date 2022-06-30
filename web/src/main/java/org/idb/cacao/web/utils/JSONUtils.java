/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

/**
 * Utility methods for JSON operations
 * 
 * @author Gustavo Figueiredo
 *
 */
public class JSONUtils {

	public static String someFields(Collection<?> objects, String... fields) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.setSerializationInclusion(Include.NON_NULL);
			
			if (fields!=null && fields.length>0) {
				final Set<String> accept_fields = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
				Arrays.stream(fields).forEach(accept_fields::add);
				mapper.setAnnotationIntrospector(new JacksonAnnotationIntrospector(){
					private static final long serialVersionUID = 1L;
		
					@Override
				    public boolean hasIgnoreMarker(final AnnotatedMember m) {
						if (m instanceof com.fasterxml.jackson.databind.introspect.AnnotatedField) {
							boolean ignore = !accept_fields.contains(m.getName()) || super.hasIgnoreMarker(m);
							return ignore;							
						}
						else {
							return super.hasIgnoreMarker(m);
						}
				    }
				});
			}
			
			return mapper.writeValueAsString(objects);
		}
		catch (JsonProcessingException ex) {
			return "";
		}
	}
}
