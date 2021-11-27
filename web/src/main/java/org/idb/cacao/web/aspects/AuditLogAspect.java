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
package org.idb.cacao.web.aspects;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.idb.cacao.api.AFieldDescriptor;
import org.idb.cacao.api.AuditTrail;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.utils.IndexNamesUtils;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.repositories.AuditTrailRepository;
import org.idb.cacao.web.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Injects logging features for auditing application usage.<BR>
 * <BR>
 * We define some 'pointcuts' that will target some application methods for which we
 * need logging.<BR>
 * <BR>
 * We define some 'advices' that will that action over each matching pointcut in order
 * to collect logging material.<BR>
 * <BR>
 * 
 * @author Gustavo Figueiredo
 *
 */
@Aspect
@Component
public class AuditLogAspect {
	
	/**
	 * For each class, maps the corresponding 'parameter collector' for 'AuditTrail' objects
	 */
	private final Map<Class<?>, AuditTrailParameterCollector> mapAuditTrailParametersCollectors;

	/**
	 * Repository for storing all audit trail entries
	 */
	@Autowired
	private AuditTrailRepository auditRepo;

	//@Pointcut("execution(* org.idb.cacao.web.controllers.rest.*.*(..))")
	//public void pointCutForRestControllerMethods() {};
	
	public AuditLogAspect() {
		mapAuditTrailParametersCollectors = new ConcurrentHashMap<>();
	}

	/**
	 * Captures all methods within classes annotated by either 'Controller' or 'RestController' annotations.
	 */
	@Pointcut("within(@org.springframework.stereotype.Controller *) || within(@org.springframework.web.bind.annotation.RestController *)")
	public void pointCutForControllerMethods() {};
	
	
	//@Around("pointCutForControllerMethods()")
	//public Object adviceLogControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable { 

	/**
	 * Perform LOG action before all methods captured by 'pointCutForControllerMethods'
	 */
	@Before("pointCutForControllerMethods()")
	public void adviceLogControllerMethods(JoinPoint joinPoint) throws Throwable {

		// Ignores auditing while running inside JUnit test cases
		if (isJUnitTest()) {
			return;
		}
		
		AuditTrail auditTrailEntry = getAuditTrail(joinPoint);
		if (auditTrailEntry==null)
			return;
		
		try {
			auditRepo.saveWithTimestamp(auditTrailEntry);
		}
		catch (Throwable ex) {
			// Ignores errors storing this information
		}
		
		//Object returnValue = joinPoint.proceed();
		//return returnValue;
		
	}
	
	/**
	 * Fetch information about a user request and wrap it inside an AuditTrail object
	 */
	public AuditTrail getAuditTrail(JoinPoint joinPoint) {
		
		if (joinPoint==null)
			return null;
		
		AuditTrail entry = new AuditTrail();
		
		try {
			
			try {
				ServletRequestAttributes request = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
				
				try {
					entry.setIpAddress(request.getRequest().getRemoteAddr());
				} catch (Throwable ex) { } // ignores NullPointerException

				
				try {
					entry.setHttpMethod(request.getRequest().getRequestURL().toString());
				} catch (Throwable ex) { } // ignores NullPointerException

				
				try {
					entry.setUrl(request.getRequest().getMethod());
				} catch (Throwable ex) { } // ignores NullPointerException

			} catch (Throwable ex) { }

			try {
				entry.setControllerClass(joinPoint.getSignature().getDeclaringType().getSimpleName());
			} catch (Throwable ex) { } // ignores NullPointerException

			try {
				entry.setControllerMethod(joinPoint.getSignature().getName());
			} catch (Throwable ex) { } // ignores NullPointerException
			
			try {
				Authentication auth = SecurityContextHolder.getContext().getAuthentication();
				User user = UserUtils.getUser(auth);
				if (user!=null) {
					entry.setUserLogin(user.getLogin());
					entry.setUserName(user.getName());
				}
			} catch (Throwable ex) { } 

			Object args[] = joinPoint.getArgs();

			if (args!=null && args.length>0) {
				String[] parameterNames = ((CodeSignature)joinPoint.getSignature()).getParameterNames();
				try {					
					for (int i=0; i<args.length; i++) {
						Object argument = args[i];
						if (argument==null)
							continue;
						
						String parameterName = (parameterNames!=null && parameterNames.length>i) ? parameterNames[i] : null;

						if (argument instanceof String) {
							entry.addParam(parameterName, (String)argument);
							continue;
						}

						if (argument instanceof Number) {
							entry.addParam(parameterName, ValidationContext.toString(argument));
							continue;
						}

						if (argument.getClass().isPrimitive()) {
							entry.addParam(parameterName, ValidationContext.toString(argument));
							continue;
						}

						if (argument instanceof Boolean) {
							entry.addParam(parameterName, argument.toString());
							continue;
						}

						if (argument instanceof Date) {
							entry.addParam(parameterName, ValidationContext.ISO_8601_TIMESTAMP.get().format((Date)argument));
							continue;
						}

						if (argument instanceof OffsetDateTime) {
							entry.addParam(parameterName, ValidationContext.ISO_8601_TIMESTAMP.get().format(ValidationContext.toDate(argument)));
							continue;
						}

						if (argument instanceof LocalDate) {
							entry.addParam(parameterName, ValidationContext.ISO_8601_TIMESTAMP.get().format(ValidationContext.toDate(argument)));
							continue;
						}

						AuditTrailParameterCollector collector = getAuditTrailParameterCollectorCacheable(argument.getClass());
						if (collector!=null) {
							collector.feedAuditTrail(entry, parameterName, argument);
						}
					}
				}
				catch (Throwable ex) { }
			}
			
		}
		catch (Throwable ex) {
			// Ignores errors while parsing request parameters
		}

		return entry;
	}
	
	/**
	 * Generic interface for collecting additional data to be informed at AuditTrail entries
	 * 
	 * @author Gustavo Figueiredo
	 *
	 */
	@FunctionalInterface
	public static interface AuditTrailParameterCollector {
		
		/**
		 * The implementation should feed 'entry' parameters with additional information collected from instance
		 * @param entry The object to store additional information into
		 * @param parameterName The parameter name that should prefix any other additional names
		 * @param instance The object instance from where to collect additional information
		 */
		public void feedAuditTrail(AuditTrail entry, String parameterName, Object instance);
		
	}
	
	/**
	 * Implementation of AuditTrailParameterCollector that collects the value of one field using Java reflection
	 * 
	 * @author Gustavo Figueiredo
	 *
	 */
	public static class AuditTrailParameterCollectorFromAnnotatedField implements AuditTrailParameterCollector {

		private final String fieldName;
		private final Field field;
		
		public AuditTrailParameterCollectorFromAnnotatedField(Field field) {
			AFieldDescriptor anno = field.getAnnotation(AFieldDescriptor.class);
			if (anno==null)
				fieldName = "_"+IndexNamesUtils.formatFieldName(field.getName());
			else
				fieldName = "_"+IndexNamesUtils.formatFieldName(anno.externalName());	
			field.setAccessible(true);
			this.field = field;
		}

		@Override
		public void feedAuditTrail(AuditTrail entry, String parameterName, Object instance) {
			Object value;
			try {
				value = field.get(instance);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				return;
			}
			if (value==null)
				return;
			String text_value = ValidationContext.toString(value);
			if (text_value==null)
				return;
			entry.addParam(parameterName+fieldName, text_value);
		}
		
	}
	
	/**
	 * Implementation of AuditTrailParameterCollector that wraps a list of other AuditTrailParameterCollector
	 * @author Gustavo Figueiredo
	 */
	public static class AuditTrailParameterCollectorCollection implements AuditTrailParameterCollector {
		private final Collection<AuditTrailParameterCollector> collectors;
		public AuditTrailParameterCollectorCollection(Collection<AuditTrailParameterCollector> collectors) {
			this.collectors = collectors;
		}
		@Override
		public void feedAuditTrail(AuditTrail entry, String parameterName, Object instance) {
			if (collectors==null || collectors.isEmpty())
				return;
			for (AuditTrailParameterCollector collector: collectors) {
				collector.feedAuditTrail(entry, parameterName, instance);
			}
		}
	}
	
	/**
	 * Given any type of argument, return the corresponding 'AuditTrailParameterCollector' used to fetch additional
	 * information from it. Make use of an internal cache of possible implementations for different types.
	 */
	public AuditTrailParameterCollector getAuditTrailParameterCollectorCacheable(Class<?> type) {
		return mapAuditTrailParametersCollectors.computeIfAbsent(type, (k)->getAuditTrailParameterCollector(k));
	}
	
	/**
	 * Given any type of argument, return the corresponding 'AuditTrailParameterCollector' used to fetch additional
	 * information from it.
	 */
	public static AuditTrailParameterCollector getAuditTrailParameterCollector(Class<?> type) {
		
		List<AuditTrailParameterCollector> feedAuditField = new LinkedList<>();

		Class<?> t = type;
		while (t!=null) {
			Field[] fields = t.getDeclaredFields();
			if (fields!=null && fields.length>0) {
				for (Field field: fields) {
					if ( field.isAnnotationPresent(AFieldDescriptor.class) ) {
						AFieldDescriptor anno = field.getAnnotation(AFieldDescriptor.class);
						if (anno.audit()) {
							feedAuditField.add(new AuditTrailParameterCollectorFromAnnotatedField(field));
						}
					}
					else if (field.isAnnotationPresent(Id.class)) {
						feedAuditField.add(new AuditTrailParameterCollectorFromAnnotatedField(field));						
					}
				}
			}
			t = t.getSuperclass();
		}
		
		return new AuditTrailParameterCollectorCollection(feedAuditField);
	}
	
	/**
	 * Check if it's running inside JUnit test
	 */
	public static boolean isJUnitTest() {  
	  for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
	    if (element.getClassName().startsWith("org.junit.")) {
	      return true;
	    }           
	  }
	  return false;
	}
}
