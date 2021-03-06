/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
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
import org.idb.cacao.api.AuthenticationMethod;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.utils.IndexNamesUtils;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.repositories.AuditTrailRepository;
import org.idb.cacao.web.sec.ApiKeyAuthenticationToken;
import org.idb.cacao.web.utils.ControllerUtils;
import org.idb.cacao.web.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.annotation.Id;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.idb.cacao.api.utils.ParserUtils.ISO_8601_TIMESTAMP;

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
	
	@Autowired
	@Qualifier("AuditTrailTaskExecutor")
	private TaskExecutor taskExecutor;

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
		if (ControllerUtils.isJUnitTest()) {
			return;
		}
		
		AuditTrail auditTrailEntry = getAuditTrail(joinPoint);
		if (auditTrailEntry==null)
			return;
		
		taskExecutor.execute(()->{
			try {
				auditRepo.saveWithTimestamp(auditTrailEntry);
			}
			catch (Exception ex) {
				// Ignores errors storing this information
			}
		});
		
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
				} catch (Exception ex) { } // ignores NullPointerException

				
				try {
					entry.setUrl(request.getRequest().getRequestURL().toString());
				} catch (Exception ex) { } // ignores NullPointerException

				
				try {
					entry.setHttpMethod(request.getRequest().getMethod());
				} catch (Exception ex) { } // ignores NullPointerException

			} catch (Exception ex) { }

			try {
				entry.setControllerClass(joinPoint.getSignature().getDeclaringType().getSimpleName());
			} catch (Exception ex) { } // ignores NullPointerException

			try {
				entry.setControllerMethod(joinPoint.getSignature().getName());
			} catch (Exception ex) { } // ignores NullPointerException
			
			try {
				Authentication auth = SecurityContextHolder.getContext().getAuthentication();
				
				AuthenticationMethod authMethod = null;
				if (auth!=null) {
					if (auth.getPrincipal() instanceof OAuth2User) {
						authMethod = AuthenticationMethod.OAUTH2;
					}
					else if (auth instanceof UsernamePasswordAuthenticationToken) {
						authMethod = AuthenticationMethod.PASSWORD;
					}
					else if (auth instanceof ApiKeyAuthenticationToken) {
						authMethod = AuthenticationMethod.TOKEN;
					}
				}

				entry.setAuthMethod(authMethod);
				
				User user = UserUtils.getUser(auth);
				if (user!=null) {
					entry.setUserLogin(user.getLogin());
					entry.setUserName(user.getName());
				}
			} catch (Exception ex) { } 

			Object[] args = joinPoint.getArgs();

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
							entry.addParam(parameterName, ISO_8601_TIMESTAMP.get().format((Date)argument));
							continue;
						}

						if (argument instanceof OffsetDateTime) {
							entry.addParam(parameterName, ISO_8601_TIMESTAMP.get().format(ValidationContext.toDate(argument)));
							continue;
						}

						if (argument instanceof LocalDate) {
							entry.addParam(parameterName, ISO_8601_TIMESTAMP.get().format(ValidationContext.toDate(argument)));
							continue;
						}

						AuditTrailParameterCollector collector = getAuditTrailParameterCollectorCacheable(argument.getClass());
						if (collector!=null) {
							collector.feedAuditTrail(entry, parameterName, argument);
						}
					}
				}
				catch (Exception ex) { }
			}
			
		}
		catch (Exception ex) {
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
		private final String _fieldName;
		private final Field field;
		
		public AuditTrailParameterCollectorFromAnnotatedField(Field field) {
			AFieldDescriptor anno = field.getAnnotation(AFieldDescriptor.class);
			if (anno==null)
				fieldName = IndexNamesUtils.formatFieldName(field.getName());
			else
				fieldName = IndexNamesUtils.formatFieldName(anno.externalName());
			_fieldName = "_"+fieldName;
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
			String textValue = ValidationContext.toString(value);
			if (textValue==null)
				return;
			if (fieldName.startsWith(parameterName))
				entry.addParam(fieldName, textValue);
			else
				entry.addParam(parameterName+_fieldName, textValue);
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
		return mapAuditTrailParametersCollectors.computeIfAbsent(type, AuditLogAspect::getAuditTrailParameterCollector);
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
	
}
