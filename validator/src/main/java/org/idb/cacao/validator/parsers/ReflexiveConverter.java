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
package org.idb.cacao.validator.parsers;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.idb.cacao.api.ValidationContext;

/**
 * Base class for a generic traversal of object hierarchy
 *  
 * @author Gustavo Figueiredo
 *
 */
public abstract class ReflexiveConverter {

	/**
	 * For each visited field type enlists the internal fields
	 */
	private final Map<Class<?>,List<FieldDescriptor>> map_fields;
	
	/**
	 * For each visited field type enlists the internal fields of primitive type
	 */
	private final Map<Class<?>,List<FieldDescriptor>> map_fields_primitive;
	
	/**
	 * For each visited field type enlists the internal fields of complex type
	 */	
	private final Map<Class<?>,List<FieldDescriptor>> map_fields_complex;

	/**
	 * For each visited field type enlists the internal fields of multiple-valued type (e.g. arrays and lists)
	 */
	private final Map<Class<?>,List<FieldDescriptor>> map_fields_multiple;
	
	public ReflexiveConverter() {
		map_fields = new HashMap<>();
		map_fields_primitive = new HashMap<>();
		map_fields_complex = new HashMap<>();
		map_fields_multiple = new HashMap<>();
	}
	
	/**
	 * Called at the start of lookup of fields of a given instance of arbitary type
	 */
	protected abstract void visitFieldsStart(Object obj);
	
	/**
	 * Called at the end of lookup of fields of a given instance of arbitary type
	 */
	protected abstract void visitFieldsStop(Object obj);
	
	/**
	 * Called for each instance of a primitive type
	 */
	protected abstract void visitValuePrimitive(Object obj);

	/**
	 * Called for each field of a primitive type
	 */
	protected abstract void visitFieldPrimitive(Object obj,FieldDescriptor campo,Object valor);

	/**
	 * Called at the start of lookup of fields of a complex type for a give instance
	 */
	protected abstract void visitFieldStart(Object obj,FieldDescriptor campo,Object valor);

	/**
	 * Called at the end of lookup of fields of a complex type for a give instance
	 */
	protected abstract void visitFieldStop(Object obj,FieldDescriptor campo,Object valor);
	
	/**
	 * Called at the start of lookup of a field of multiple values
	 */
	protected abstract void visitManyValuesStart(Object obj,int size);

	/**
	 * Called at the end of lookup of a field of multiple values
	 */
	protected abstract void visitManyValuesStop(Object obj);
	
	/**
	 * Called for each element of a field of multiple values
	 */
	protected abstract void visitManyValuesElement(Object obj,int index,Object el);
	
	/**
	 * Recursive method for inspecting all the structure of a hierarchy of arbitrary objects
	 */
	public void parse(Object obj) {
		if (obj==null)
			return;
		if (obj.getClass().isArray()) {
			int len = Array.getLength(obj);
			visitManyValuesStart(obj, len);
			for (int i=0;i<len;i++) {
				Object el = Array.get(obj, i);
				visitManyValuesElement(obj,i,el);
				parse(el);
			}
			visitManyValuesStop(obj);
		}
		else if (obj instanceof Collection) {
			Collection<?> col = (Collection<?>)obj;
			visitManyValuesStart(obj, col.size());
			int i = 0;
			for (Object el:col) {
				visitManyValuesElement(obj,i++,el);					
				parse(el);
			}
			visitManyValuesStop(obj);
		}
		else if (obj instanceof Map) {
			// In this implementation we will treat any 'Map' as a collection of 'fields'
			// It's important for reading an arbitrary object (such as a JSON)
			Map<?,?> map = (Map<?,?>)obj;
			visitFieldsStart(obj);
			// First classifies the values according to their 'types': primitives, complex and multiple
			List<Map.Entry<?, ?>> fields_primitive = new LinkedList<>();
			List<Map.Entry<?, ?>> fields_complex = new LinkedList<>();
			List<Map.Entry<?, ?>> fields_multiple = new LinkedList<>();
			for (Map.Entry<?,?> entry:map.entrySet()) {
				Object valor = entry.getValue();
				if (valor==null) {
					fields_primitive.add(entry);
					//continue;
				}
				else if (isPrimitiveType(valor.getClass()))
					fields_primitive.add(entry);
				else if (isArrayType(valor.getClass())) {
					if ( valor instanceof Object[] ) {
						Object[] valores = (Object[])valor;
						if ( valores.length == 0 || valores[0] == null || valores[0].toString().isEmpty() ) {
							entry.setValue(null);
							fields_primitive.add(entry);
						}
						else {
							fields_multiple.add(entry);
						}
					}
					else {
						fields_multiple.add(entry);							
					}
				}
				else {
					fields_complex.add(entry);
				}
			}
			// Insert the values in order (first the primitive types, followed by complex type, and lastly by multiple-valued types)
			for (Map.Entry<?,?> entry:fields_primitive) {
				String nome_campo = ValidationContext.toString(entry.getKey());
				if (nome_campo==null)
					continue;
				Object valor = entry.getValue();
				FieldDescriptor campo;
				if ( valor == null )
					campo = new FieldDescriptor(nome_campo, String.class);
				else
					campo = new FieldDescriptor(nome_campo, valor.getClass());
				visitFieldPrimitive(obj, campo, valor);						
			}
			for (Map.Entry<?,?> entry:fields_complex) {
				String nome_campo = ValidationContext.toString(entry.getKey());
				if (nome_campo==null)
					continue;
				Object valor = entry.getValue();
				FieldDescriptor campo = new FieldDescriptor(nome_campo, valor.getClass());
				visitFieldStart(obj, campo, valor);
				if (valor!=null) {
					parse(valor);
				}
				visitFieldStop(obj, campo, valor);
			}
			for (Map.Entry<?,?> entry:fields_multiple) {
				String nome_campo = ValidationContext.toString(entry.getKey());
				
				if (nome_campo==null)
					continue;
				Object valor = entry.getValue();
				FieldDescriptor campo = new FieldDescriptor(nome_campo, valor.getClass());
				visitFieldStart(obj, campo, valor);
				if (valor!=null) {
					parse(valor);
				}
				visitFieldStop(obj, campo, valor);
			}
			visitFieldsStop(obj);
		}
		else if (isPrimitiveType(obj.getClass())) {
			visitValuePrimitive(obj);
		}
		else {
			visitFieldsStart(obj);
			// First fill all the primitive fields
			List<FieldDescriptor> fields = getFieldsPrimitive(obj.getClass());
			for (FieldDescriptor campo:fields) {
				Object valor = campo.getValueOf(obj);
				visitFieldPrimitive(obj, campo, valor);						
			}
			// Fills all complex fields
			fields = getFieldsComplex(obj.getClass());
			for (FieldDescriptor campo:fields) {
				Object valor = campo.getValueOf(obj);
				visitFieldStart(obj, campo, valor);
				if (valor!=null) {
					parse(valor);
				}
				visitFieldStop(obj, campo, valor);
			}
			// Fills all the multiple-valued fields
			fields = getFieldsMultiple(obj.getClass());
			for (FieldDescriptor campo:fields) {
				Object valor = campo.getValueOf(obj);
				visitFieldStart(obj, campo, valor);
				if (valor!=null) {
					parse(valor);
				}
				visitFieldStop(obj, campo, valor);
			}
			visitFieldsStop(obj);
		}
	}
	
	/**
	 * Returns all the fields related to an arbitrary type
	 */
	protected List<FieldDescriptor> getFields(Class<?> type) {
		List<FieldDescriptor> fields = map_fields.get(type);
		if (fields!=null)
			return fields;
		List<Field> java_fields = getFields(type, /*statics*/false, /*finals*/true, /*transients*/true);
		if (java_fields==null)
			java_fields = Collections.emptyList();
		List<FieldDescriptor> custom_fields = java_fields.stream()
			.map(FieldDescriptor::new)
			.sorted(new Comparator<FieldDescriptor>() {
				@Override
				public int compare(FieldDescriptor f1, FieldDescriptor f2) {
					return String.CASE_INSENSITIVE_ORDER.compare(f1.fieldName, f2.fieldName);
				}
			})
			.collect(Collectors.toList());
		map_fields.put(type,custom_fields);
		return custom_fields;
	}
	
	/**
	 * Returns all the fields of primitive type related to an arbitrary type
	 */
	protected List<FieldDescriptor> getFieldsPrimitive(Class<?> type) {
		List<FieldDescriptor> fields = map_fields_primitive.get(type);
		if (fields!=null) 
			return fields;
		List<FieldDescriptor> todos_fields = getFields(type);
		fields = todos_fields.stream().filter(campo->isPrimitiveType(campo.fieldType)).collect(Collectors.toList());
		map_fields_primitive.put(type,fields);
		return fields;
	}

	/**
	 * Returns all the fields of complex type related to an arbitrary type
	 */
	protected List<FieldDescriptor> getFieldsComplex(Class<?> type) {
		List<FieldDescriptor> fields = map_fields_complex.get(type);
		if (fields!=null) 
			return fields;
		List<FieldDescriptor> todos_fields = getFields(type);
		fields = todos_fields.stream().filter(campo->isComplexType(campo.fieldType)).collect(Collectors.toList());
		map_fields_complex.put(type,fields);
		return fields;
	}
	
	/**
	 * Returns all the fields of multiple-valued type related to an arbitrary type
	 */
	protected List<FieldDescriptor> getFieldsMultiple(Class<?> type) {
		List<FieldDescriptor> fields = map_fields_multiple.get(type);
		if (fields!=null) 
			return fields;
		List<FieldDescriptor> todos_fields = getFields(type);
		fields = todos_fields.stream().filter(campo->isArrayType(campo.fieldType)).collect(Collectors.toList());
		map_fields_multiple.put(type,fields);
		return fields;
	}

	/**
	 * Check if type is a primitive type
	 */
	public static boolean isPrimitiveType(Class<?> type) {
		if (type==null) return false;
		if (type.isArray()) return false;
		if (type.isPrimitive()) return true;
		if (Number.class.isAssignableFrom(type)) return true;
		if (Byte.class.isAssignableFrom(type)) return true;
		if (Boolean.class.isAssignableFrom(type)) return true;
		if (Character.class.isAssignableFrom(type)) return true;
		if (java.util.Date.class.isAssignableFrom(type)) return true;
		if (java.sql.Date.class.isAssignableFrom(type)) return true;
		if (Temporal.class.isAssignableFrom(type)) return true;
		if (String.class.isAssignableFrom(type)) return true;
		if (Enum.class.isAssignableFrom(type)) return true;
		if (Object.class.equals(type)) return true;
		return false;
	}

	/**
	 * Check if type is a complex type
	 */
	public static boolean isComplexType(Class<?> type) {
		return !Collection.class.isAssignableFrom(type) && !type.isArray()
				&& !isPrimitiveType(type);
	}
	
	/**
	 * Check if type is an array-like type
	 */
	public static boolean isArrayType(Class<?> type) {
		return Collection.class.isAssignableFrom(type) || type.isArray();
	}
	
	/**
	 * Wraps some information about a particular field
	 * @author Gustavo Figueiredo
	 */
	public static class FieldDescriptor {
		private final String fieldName;
		private final Class<?> fieldType;
		private final Field field;
		public FieldDescriptor(Field field) {
			this.fieldName = field.getName();
			this.fieldType = field.getType();
			this.field = field;
			field.setAccessible(true);
		}
		public FieldDescriptor(String fieldName) {
			this.fieldName = fieldName;
			this.fieldType = String.class;
			this.field = null;
		}
		public FieldDescriptor(String fieldName, Class<?> fieldType) {
			this.fieldName = fieldName;
			this.fieldType = fieldType;
			this.field = null;
		}
		public Object getValueOf(Object instance) {
			if (instance==null) {
				return null;
			}
			if (field!=null) {
				try {
					return field.get(instance);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}
			return null;
		}
		public String getFieldName() {
			return fieldName;
		}
		public Class<?> getFieldType() {
			return fieldType;
		}
		public Field getField() {
			return field;
		}
		public String toString() {
			return fieldName;
		}
	}

	public static List<Field> getFields(Class<?> type, boolean statics, boolean finals, boolean transients) {
		Class<?> inspectClass = type;
		List<Field> fields = new LinkedList<Field>();
		while (inspectClass != null) {
			Field[] reflected_fields = inspectClass.getDeclaredFields();
			if (reflected_fields != null && reflected_fields.length > 0) {
				for (Field field : reflected_fields) {
					int mod = field.getModifiers();
					if (!statics && Modifier.isStatic(mod))
						continue; // Não considera os fields estáticos
					if (!finals && Modifier.isFinal(mod))
						continue; // Não considera os fields do type "final"
					if (!transients && Modifier.isTransient(mod))
						continue; // Não considera os fields do type "transient"

					if (!Modifier.isPublic(mod))
						field.setAccessible(true);

					fields.add(field);
				}
			}
			inspectClass = inspectClass.getSuperclass(); // procura também nas super-classes
		}
		return fields;
	}
}
