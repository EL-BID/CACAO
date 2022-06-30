/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.idb.cacao.api.errors.CommonErrors;
import org.springframework.stereotype.Component;

/**
 * Injects exception handling for some common problems, such as the 'HTTP/1.1 429 Too Many Requests' that may be returned while
 * trying to save objects at repository.<BR>
 * <BR>
 * We define some 'pointcuts' that will target some application methods for which we
 * need exception handling.<BR>
 * <BR>
 * We define some 'advices' that will that action over each matching pointcut in order
 * to retry failed methods in some situations.<BR>
 * <BR>
 * 
 * @author Gustavo Figueiredo
 *
 */
@Aspect
@Component
public class DAOExceptionRetryAspect {

	/**
	 * Captures the 'save' and 'saveAll' and 'saveWithTimestamp' methods of all classes that implements the CrudRepository interface
	 */
	@Pointcut("execution(* org.springframework.data.repository.CrudRepository+.save(..))"
			+ " || execution(* org.springframework.data.repository.CrudRepository+.saveAll(..))"
			+ " || execution(* org.springframework.data.repository.CrudRepository+.saveWithTimestamp(..))")
	public void pointCutForCrudRepositorySaveMethod() {};

	/**
	 * Perform exception handling action around all methods captured by 'pointCutForCrudRepositorySaveMethod'
	 */
    @Around("pointCutForCrudRepositorySaveMethod()")
    public Object aroundCrudRepositorySaveMethod(ProceedingJoinPoint joinPoint) throws Throwable {
    	
    	return CommonErrors.doESMethodWithRetries(joinPoint::proceed);
    	
    }	
}
