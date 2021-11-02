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
