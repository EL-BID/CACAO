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
package org.idb.cacao.validator.aspects;

import java.util.concurrent.atomic.AtomicReference;

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
	 * Captures the 'save' and 'saveAll' methods of all classes that implements the CrudRepository interface
	 */
	@Pointcut("execution(* org.springframework.data.repository.CrudRepository+.save(..)) || execution(* org.springframework.data.repository.CrudRepository+.saveAll(..))")
	public void pointCutForCrudRepositorySaveMethod() {};

	/**
	 * Perform exception handling action around all methods captured by 'pointCutForCrudRepositorySaveMethod'
	 */
    @Around("pointCutForCrudRepositorySaveMethod()")
    public Object aroundCrudRepositorySaveMethod(ProceedingJoinPoint joinPoint) throws Throwable {
    	
    	// Holds the returned object from the 'save' method
    	AtomicReference<Object> returnedObject = new AtomicReference<>();
    	
    	// Try to run the intercepted method and look for particular class of errors (e.g.: 'Rejected Exception')
    	// Will try again after some delay in case of error. The maximum number of retries and the delay between retries
    	// are configured statically at CommonErrors.
    	CommonErrors.doESWriteOpWithRetries(()->{
    		
    		try {
	    		Object ret = joinPoint.proceed();
	    		returnedObject.set(ret);
    		}
    		catch (Exception|Error ex) {
    			throw ex; // handled by 'CommonErrors.doESWriteOpWithRetries'
    		}
    		catch (Throwable ex) {
    			throw new RuntimeException(ex); // handled by 'CommonErrors.doESWriteOpWithRetries'
    		}
    		
    	});
    	
    	return returnedObject.get();
    }	
}
