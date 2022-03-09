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
package org.idb.cacao.api.utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Utility methods for Java Reflection
 * 
 * @author Gustavo Figueiredo
 *
 */
public class ReflectUtils {

	/**
	 * Search internal fields and methods of an arbitrary entity for a given name.<BR>
	 * Ignores static fields or methods.<BR>
	 * Ignores private or package protected fields or methods.<BR>
	 * For the methods, considers only those ones with common 'getter' prefixes ('get', 'is' or 'has').<BR>
	 * The search is case insensitive.
	 */
	public static Member findMember(Class<?> entity, String name) {
		
		final String getterForm1 = "get"+name;
		final String getterForm2 = "is"+name;
		final String getterForm3 = "has"+name;
		
		Class<?> cl = entity;
		while (cl!=null) {
			
			Field[] fields = cl.getFields();
			if (fields!=null && fields.length>0) {
				for (Field field:fields) {
					if (Modifier.isStatic(field.getModifiers()))
						continue;
					if (name.equalsIgnoreCase(field.getName()))
						return field;
				}
			}
			
			Method[] methods = cl.getMethods();
			if (methods!=null && methods.length>0) {
				for (Method method:methods) {
					if (Modifier.isStatic(method.getModifiers()))
						continue;
					if (method.getParameterCount()>0)
						continue;
					Class<?> return_type = method.getReturnType();
					if (return_type==null || return_type==void.class)
						continue;
					if (getterForm1.equalsIgnoreCase(method.getName())
							|| getterForm2.equalsIgnoreCase(method.getName())
							|| getterForm3.equalsIgnoreCase(method.getName()))
						return method;
				}
			}
			
			cl = cl.getSuperclass();
		}
		
		return null;
	}
	
	/**
	 * Search internal fields and methods of an arbitrary entity for a given name.<BR>
	 * Ignores static fields or methods.<BR>
	 * Ignores private or package protected fields or methods.<BR>
	 * For the methods, considers only those ones with common 'setter' prefixes (only 'set').<BR>
	 * The search is case insensitive.
	 */
	public static Member findMemberSetter(Class<?> entity, String name) {
		
		final String setterForm = "set"+name;
		
		Class<?> cl = entity;
		while (cl!=null) {
			
			Field[] fields = cl.getFields();
			if (fields!=null && fields.length>0) {
				for (Field field:fields) {
					if (Modifier.isStatic(field.getModifiers()))
						continue;
					if (name.equalsIgnoreCase(field.getName()))
						return field;
				}
			}
			
			Method[] methods = cl.getMethods();
			if (methods!=null && methods.length>0) {
				for (Method method:methods) {
					if (Modifier.isStatic(method.getModifiers()))
						continue;
					if (method.getParameterCount()!=1)
						continue;
					Class<?> return_type = method.getReturnType();
					if (return_type!=null && return_type!=void.class)
						continue;
					if (setterForm.equalsIgnoreCase(method.getName()))
						return method;
				}
			}
			
			cl = cl.getSuperclass();
		}
		
		return null;
	}

	/**
	 * Search internal fields and methods of an arbitrary entity for a given name.<BR>
	 * Returns the type of this field. If it's a method, returns the returned type of the method.<BR>
	 * Returns NULL if no field or method is found.
	 */
	public static Class<?> getMemberType(Class<?> entity, String name) {
		Member member = findMember(entity, name);
		if (member==null)
			return null;
		if (member instanceof Field)
			return ((Field)member).getType();
		if (member instanceof Method)
			return ((Method)member).getReturnType();
		return null;
	}
	
	/**
	 * Search internal fields and methods of an arbitrary entity for a given name.<BR>
	 * Returns a function that may be used to access this same field from a instance of the same type. If it's a method, returns the returned value of the method execution.<BR>
	 * Returns NULL if no field or method is found.
	 */
	@SuppressWarnings("unchecked")
	public static <OBJ,FIELD_TYPE> Function<OBJ,FIELD_TYPE> getMemberGetter(Class<?> entity, String name) {
		Member member = findMember(entity, name);
		if (member==null)
			return null;
		if (member instanceof Field) {
			Field f = (Field)member;
			f.setAccessible(true);
			return (obj)->{
				try {
					return (FIELD_TYPE)f.get(obj);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					return null;
				}
			};
		}
		if (member instanceof Method) {
			Method m = (Method)member;
			m.setAccessible(true);
			return (obj)->{
				try {
					return (FIELD_TYPE)m.invoke(obj);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					return null;
				}
			};
		}
		return null;		
	}
	
	/**
	 * Search internal fields and methods of an arbitrary entity for a given name.<BR>
	 * Returns a function that may be used to alter this same field value for a instance of the same type. If it's a method, invokes the method with the value to set.<BR>
	 * Returns NULL if no field or method is found.
	 */
	public static <OBJ,FIELD_TYPE> BiConsumer<OBJ,FIELD_TYPE> getMemberSetter(Class<?> entity, String name) {
		Member member = findMemberSetter(entity, name);
		if (member==null)
			return null;
		if (member instanceof Field) {
			Field f = (Field)member;
			f.setAccessible(true);
			return (obj, value)->{
				try {
					f.set(obj,  value);
				} catch (IllegalArgumentException | IllegalAccessException e) {
				}
			};
		}
		if (member instanceof Method) {
			Method m = (Method)member;
			m.setAccessible(true);
			return (obj, value)->{
				try {
					m.invoke(obj, value);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				}
			};
		}
		return null;		
	}

	/**
	 * Given some class, returns the parameterized type of its superclass, and repeat
	 * this search in superclasses hierarchy, util it finds one. Returns NULL if found none. 
	 */
	public static Class<?> getParameterType(Class<?> some_class) {
		Class<?> cl = some_class;
		while (cl!=null) {
			Type[] ginterfaces = cl.getGenericInterfaces();
			if (ginterfaces!=null && ginterfaces.length>0) {
				for (Type i:ginterfaces) {
					if (i instanceof ParameterizedType) {
						return (Class<?>)((ParameterizedType)i).getActualTypeArguments()[0];
					}
				}
			}
			if (cl.getGenericSuperclass() instanceof ParameterizedType) {
				return (Class<?>)((ParameterizedType)cl.getGenericSuperclass()).getActualTypeArguments()[0];
			}
			Class<?> [] interfaces = cl.getInterfaces();
			if (interfaces!=null && interfaces.length>0) {
				for (Class<?> i:interfaces) {
					Class<?> ptype = getParameterType(i);
					if (ptype!=null)
						return ptype;
				}
			}
			cl = cl.getSuperclass();
		}
		return null;
	}
	
	/**
	 * Given some class, returns a map of all its fields (except the 'static' ones) and corresponding
	 * functions to get their values. Will not consider any methods.
	 * Includes fields from superclasses (except if the name conflicts with other fields from subclasses).
	 */
	public static <OBJ> Map<String,Function<OBJ,?>> getAllFieldsFunctions(Class<?> some_class) {
		Map<String,Function<OBJ,?>> map_all_fields = new HashMap<>();
		Class<?> cl = some_class;
		while (cl!=null) {
			
			Field[] fields = cl.getDeclaredFields();
			if (fields!=null && fields.length>0) {
				for (Field field:fields) {
					if (Modifier.isStatic(field.getModifiers()))
						continue;
					if (map_all_fields.containsKey(field.getName()))
						continue;
					field.setAccessible(true);
					Function<OBJ,?> getter = (obj)->{
						try {
							return field.get(obj);
						} catch (IllegalArgumentException | IllegalAccessException e) {
							return null;
						}
					};
					map_all_fields.put(field.getName(), getter);
				}
			}
			
			cl = cl.getSuperclass();
		}
		return map_all_fields;
	}

	/**
	 * Return the interface with one of the given names. Ignores the package names. Only considers the 'simple name'.
	 * Look all hierarchy.		
	 */
	public static Class<?> getInterfaceWithName(Class<?> some_class, Collection<String> some_names) {
		return getInterfaceWithFilter(some_class,
				/*filter*/cl->some_names.contains(cl.getSimpleName()));
	}
	
	/**
	 * Return the interface that matches a filter. 
	 * Look all hierarchy.		
	 */
	public static Class<?> getInterfaceWithFilter(Class<?> some_class, Predicate<Class<?>> filter) {
		Class<?> cl = some_class;
		while (cl!=null) {
			if (filter.test(cl)) {
				return cl;
			}
			Class<?> [] interfaces = cl.getInterfaces();
			if (interfaces!=null && interfaces.length>0) {
				for (Class<?> i:interfaces) {
					if (filter.test(i)) {
						return i;
					}
				}
			}
			cl = cl.getSuperclass();
		}
		return null;		
	}

	/**
	 * Copies all internal fields from 'src' to 'target', as long as those fields are not
	 * null or empty at source neither at target.
	 */
	public static void complementFields(Object src, Object target) {
		
		if (src==null || target==null || src==target || !src.getClass().equals(target.getClass()))
			return;
		
		Class<?> cl = src.getClass();
		while (cl!=null) {
			
			Field fields[] = cl.getDeclaredFields();
			if (fields!=null && fields.length>0) {
				
				for (Field field: fields) {
					int mod = field.getModifiers();
					if (Modifier.isStatic(mod) || Modifier.isFinal(mod))
						continue;
					field.setAccessible(true);
					try {
						Object tgt_value = field.get(target);
						if (tgt_value!=null && !"".equals(tgt_value))
							continue;
						
						Object src_value = field.get(src);
						if (src_value==null || "".equals(src_value))
							continue;
						
						field.set(target, src_value);
						
					} catch (IllegalArgumentException | IllegalAccessException e) {
						continue;
					}					
				}
				
			}
			
			cl = cl.getSuperclass();
		}
		
	}
}
