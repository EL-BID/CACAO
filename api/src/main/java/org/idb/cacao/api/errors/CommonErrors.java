/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.api.errors;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.elasticsearch.ElasticsearchStatusException;

/**
 * Some methods for identifying common errors regarding all modules.
 * 
 * @author Gustavo Figueiredo
 */
public class CommonErrors {
	
	/**
	 * Default maximum number of retires for {@link #doESWriteOpWithRetries(RunnableThrowing) doESWriteOpWithRetries}
	 */
	public static int DEFAULT_MAX_RETRIES = 5;
	
	/**
	 * Default delay between retries in milliseconds for {@link #doESWriteOpWithRetries(RunnableThrowing) doESWriteOpWithRetries}
	 */
	public static int DEFAULT_DELAY_MS_BETWEEN_RETRIES = 1000;

	/**
	 * Returns TRUE if the error is something like 'No mapping found for ...'
	 */
	public static boolean isErrorNoMappingFoundForColumn(Throwable ex) {
		if (ex!=null && ex.getMessage()!=null && ex.getMessage().contains("No mapping found"))
			return true;
		if (ex!=null && ex.getCause()!=null && ex.getCause()!=ex)
			return isErrorNoMappingFoundForColumn(ex.getCause());
		if (ex instanceof ElasticsearchStatusException) {
			Throwable[] suppressed = ((ElasticsearchStatusException)ex).getSuppressed();
			if (suppressed!=null && suppressed.length>0) {
				for (Throwable sup:suppressed) {
					if (isErrorNoMappingFoundForColumn(sup))
						return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns TRUE if the error is something like 'index_not_found_exception ...'
	 */
	public static boolean isErrorNoIndexFound(Throwable ex) {
		if (ex!=null && ex.getMessage()!=null && ex.getMessage().contains("index_not_found_exception"))
			return true;
		if (ex!=null && ex.getCause()!=null && ex.getCause()!=ex)
			return isErrorNoIndexFound(ex.getCause());
		if (ex instanceof ElasticsearchStatusException) {
			Throwable[] suppressed = ((ElasticsearchStatusException)ex).getSuppressed();
			if (suppressed!=null && suppressed.length>0) {
				for (Throwable sup:suppressed) {
					if (isErrorNoIndexFound(sup))
						return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Returns TRUE if the error is something like 'The current thread was interrupted ...'
	 */
	public static boolean isErrorThreadInterrupted(Throwable ex) {
		if (ex!=null && ex.getMessage()!=null && ex.getMessage().contains("current thread was interrupted"))
			return true;
		if (ex!=null && ex.getCause()!=null && ex.getCause()!=ex)
			return isErrorThreadInterrupted(ex.getCause());
		if (ex instanceof ElasticsearchStatusException) {
			Throwable[] suppressed = ((ElasticsearchStatusException)ex).getSuppressed();
			if (suppressed!=null && suppressed.length>0) {
				for (Throwable sup:suppressed) {
					if (isErrorThreadInterrupted(sup))
						return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns TRUE if the error is something like 'es_rejected_execution_exception ...' (e.g.: HTTP/1.1 429 Too Many Requests)
	 */
	public static boolean isErrorRejectedExecution(Throwable ex) {
		if (ex!=null && ex.getMessage()!=null && ex.getMessage().contains("es_rejected_execution_exception"))
			return true;
		if (ex!=null && ex.getCause()!=null && ex.getCause()!=ex)
			return isErrorRejectedExecution(ex.getCause());
		if (ex instanceof ElasticsearchStatusException) {
			Throwable[] suppressed = ((ElasticsearchStatusException)ex).getSuppressed();
			if (suppressed!=null && suppressed.length>0) {
				for (Throwable sup:suppressed) {
					if (isErrorRejectedExecution(sup))
						return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns TRUE if the error is something like 'Connection refused'
	 */
	public static boolean isErrorConnectionRefused(Throwable ex) {
		if (ex!=null && ex.getMessage()!=null && ex.getMessage().contains("Connection refused"))
			return true;
		if (ex!=null && ex.getCause()!=null && ex.getCause()!=ex)
			return isErrorConnectionRefused(ex.getCause());
		if (ex instanceof ElasticsearchStatusException) {
			Throwable[] suppressed = ((ElasticsearchStatusException)ex).getSuppressed();
			if (suppressed!=null && suppressed.length>0) {
				for (Throwable sup:suppressed) {
					if (isErrorConnectionRefused(sup))
						return true;
				}
			}
		}
		return false;
	}

	/**
	 * Any Runnable that also throws checked exceptions
	 */
	@FunctionalInterface
	public static interface RunnableThrowing<T extends Exception> {
		public void run() throws T;
	}
	
	/**
	 * Call some arbitrary routine. Check for exceptions. May try again the same routine several times in case of failure based on the predicate indication.
	 */
	public static <T extends Exception> void doWithRetries(RunnableThrowing<T> runnable, long delayBetweenRetries, Predicate<Throwable> tryAgain) throws Exception {
		while (true) {
			try {
				runnable.run();
				return;
			}
			catch (Exception|Error ex) {
				if (!tryAgain.test(ex)) {
					throw ex;
				}				
				else {
					Thread.sleep(delayBetweenRetries);
				}
			}
		}
	}
	
	/**
	 * Call some write operation on ElasticSearch. Check for exceptions. May try again the same routine several times in case of temporary runtime failure (e.g.: too many requests).
	 * @param delayBetweenRetries Delay between retries, in milliseconds
	 * @param maxRetries Maximum retries in case of checked exceptions (not all of them, but just those ones that may be fixed eventually)
	 */
	public static <T extends Exception> void doESWriteOpWithRetries(RunnableThrowing<T> runnable, long delayBetweenRetries, int maxRetries) throws Exception {
		AtomicInteger countDownRetries = new AtomicInteger(maxRetries);
		doWithRetries(runnable, delayBetweenRetries, e->countDownRetries.getAndDecrement()>0 
				&& (isErrorRejectedExecution(e) || isErrorConnectionRefused(e)));
	}

	/**
	 * Call some write operation on ElasticSearch. Check for exceptions. May try again the same routine several times in case of temporary runtime failure (e.g.: too many requests).
	 * Use some defaults for maximum retries and delay between retries.
	 */
	public static <T extends Exception> void doESWriteOpWithRetries(RunnableThrowing<T> runnable) throws Exception {
		doESWriteOpWithRetries(runnable, DEFAULT_DELAY_MS_BETWEEN_RETRIES, DEFAULT_MAX_RETRIES);
	}

	/**
	 * Any method returning object that also throws checked exceptions
	 */
	@FunctionalInterface
	public static interface MethodThrowing<T extends Exception> {
		public Object run() throws Throwable;
	}

	/**
	 * Call some write operation on ElasticSearch. Returns the outcome of the operation. Check for exceptions. May try again the same routine several times in case of temporary runtime failure (e.g.: too many requests).
	 * Use some defaults for maximum retries and delay between retries.
	 */
	public static <T extends Exception> Object doESMethodWithRetries(MethodThrowing<T> method) throws Exception {
    	// Holds the returned object from the 'save' method
    	AtomicReference<Object> returnedObject = new AtomicReference<>();
    	
    	// Try to run the intercepted method and look for particular class of errors (e.g.: 'Rejected Exception')
    	// Will try again after some delay in case of error. The maximum number of retries and the delay between retries
    	// are configured statically at CommonErrors.
    	CommonErrors.doESWriteOpWithRetries(()->{
    		
    		try {
	    		Object ret = method.run();
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
	
	/**
	 * Returns TRUE if the error is something like 'Result window is too large ...'
	 */
	public static boolean isErrorWindowTooLarge(Throwable ex) {
		if (ex!=null && ex.getMessage()!=null && ex.getMessage().contains("Result window is too large"))
			return true;
		if (ex!=null && ex.getCause()!=null && ex.getCause()!=ex)
			return isErrorWindowTooLarge(ex.getCause());
		if (ex instanceof ElasticsearchStatusException) {
			Throwable[] suppressed = ((ElasticsearchStatusException)ex).getSuppressed();
			if (suppressed!=null && suppressed.length>0) {
				for (Throwable sup:suppressed) {
					if (isErrorWindowTooLarge(sup))
						return true;
				}
			}
		}
		return false;
	}
}
