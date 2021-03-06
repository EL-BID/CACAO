/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.elasticsearch.ElasticsearchStatusException;
import org.idb.cacao.api.errors.CommonErrors;

/**
 * Utility methods for error management
 * 
 * @author Gustavo Figueiredo
 *
 */
public class ErrorUtils {
		
	/**
	 * In the case of Elasticsearch exception [type=illegal_argument_exception], extract the part where it's told what is the expected type<BR>
	 * E.g.: '... of type [long] in document with id ...'
	 */
	public static final Pattern pIllegalArgumentExpectedType = Pattern.compile("of type (\\S+) in document with id ",Pattern.CASE_INSENSITIVE);
	
	/**
	 * In the case of Elasticsearch exception [type=illegal_argument_exception], extract the part where it's told the input string that caused
	 * the problem.<BR>
	 * E.g.: 'reason=For input string: "xxxx"'
	 */
	public static final Pattern pIllegalArgumentInputString = Pattern.compile("reason=For input string: \"([^\"]+)\"",Pattern.CASE_INSENSITIVE);
	
	/**
	 * In the case of Elasticsearch exception [type=illegal_argument_exception], extract the part where it's told the input string that caused
	 * the problem.<BR>
	 * E.g.: '[type=illegal_argument_exception, reason=mapper [xxxx]'
	 */
	public static final Pattern pIllegalArgumentInputStringAlt = Pattern.compile("reason=mapper \\[([^\\]]+)\\]",Pattern.CASE_INSENSITIVE);

	/**
	 * In the case of Elasticsearch exception [type=illegal_argument_exception], extract the part where the input types are confronted.<BR>
	 * E.g.: 'cannot be changed from type [float] to [long]'
	 */
	public static final Pattern pCannotBeChanged = Pattern.compile("cannot be changed from type \\[([^\\]]+)\\] to \\[([^\\]]+)\\]",Pattern.CASE_INSENSITIVE);
	
	/**
	 * Error of type 'Stream closed'. E.g.: may appear while consuming input stream from HTTP response.
	 */
	public static final Pattern pStreamClosed = Pattern.compile("Stream closed",Pattern.CASE_INSENSITIVE);

	/**
	 * Error of type 'Unique index or primary key violation'. E.g.: attempt to create two users with the same login
	 */
	public static final Pattern pUniqueConstraintViolation = Pattern.compile("Unique index or primary key violation",Pattern.CASE_INSENSITIVE);
	
	/**
	 * Error of type 'FORBIDDEN/5/index read-only'
	 */
	public static final Pattern pReadOnly = Pattern.compile("index read-only",Pattern.CASE_INSENSITIVE);

	/**
	 * Error of type '404 Not Found'
	 */
	public static final Pattern pNotFound = Pattern.compile("Not.Found",Pattern.CASE_INSENSITIVE);

	/**
	 * Returns TRUE if the error is something like 'illegal_argument_exception ...'
	 */
	public static boolean isErrorStreamClosed(Throwable ex) {
		if (ex!=null && ex.getMessage()!=null && pStreamClosed.matcher(ex.getMessage()).find())
			return true;
		if (ex!=null && ex.getCause()!=null && ex.getCause()!=ex)
			return isErrorStreamClosed(ex.getCause());
		if (ex instanceof ElasticsearchStatusException) {
			Throwable[] suppressed = ((ElasticsearchStatusException)ex).getSuppressed();
			if (suppressed!=null && suppressed.length>0) {
				for (Throwable sup:suppressed) {
					if (isErrorStreamClosed(sup))
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
		return CommonErrors.isErrorThreadInterrupted(ex);
	}
	
	/**
	 * Returns TRUE if the error is something like 'No mapping found for ...'
	 */
	public static boolean isErrorNoMappingFoundForColumn(Throwable ex) {
		return CommonErrors.isErrorNoMappingFoundForColumn(ex);
	}

	/**
	 * Returns TRUE if the error is something like 'index_not_found_exception ...'
	 */
	public static boolean isErrorNoIndexFound(Throwable ex) {
		return CommonErrors.isErrorNoIndexFound(ex);
	}

	/**
	 * Returns TRUE if the error is something like 'FORBIDDEN/5/index read-only ...'
	 */
	public static boolean isErrorIndexReadOnly(Throwable ex) {
		if (ex!=null && ex.getMessage()!=null && pReadOnly.matcher(ex.getMessage()).find())
			return true;
		if (ex!=null && ex.getCause()!=null && ex.getCause()!=ex)
			return isErrorIndexReadOnly(ex.getCause());
		if (ex instanceof ElasticsearchStatusException) {
			Throwable[] suppressed = ((ElasticsearchStatusException)ex).getSuppressed();
			if (suppressed!=null && suppressed.length>0) {
				for (Throwable sup:suppressed) {
					if (isErrorIndexReadOnly(sup))
						return true;
				}
			}
		}
		return false;		
	}

	/**
	 * Returns TRUE if the error is something like 'illegal_argument_exception ...' with the reason 'fields are not optimised for...'
	 */
	public static boolean isErrorIllegalArgumentFieldsNotOptimized(Throwable ex) {
		if (ex!=null && ex.getMessage()!=null && ex.getMessage().contains("illegal_argument_exception")
				&& ex.getMessage().contains("fields are not optimised"))
			return true;
		if (ex!=null && ex.getCause()!=null && ex.getCause()!=ex)
			return isErrorIllegalArgumentFieldsNotOptimized(ex.getCause());
		if (ex instanceof ElasticsearchStatusException) {
			Throwable[] suppressed = ((ElasticsearchStatusException)ex).getSuppressed();
			if (suppressed!=null && suppressed.length>0) {
				for (Throwable sup:suppressed) {
					if (isErrorIllegalArgumentFieldsNotOptimized(sup))
						return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns TRUE if the error is something like 'illegal_argument_exception ...'
	 */
	public static boolean isErrorIllegalArgument(Throwable ex) {
		if (ex!=null && ex.getMessage()!=null && ex.getMessage().contains("illegal_argument_exception"))
			return true;
		if (ex!=null && ex.getCause()!=null && ex.getCause()!=ex)
			return isErrorIllegalArgument(ex.getCause());
		if (ex instanceof ElasticsearchStatusException) {
			Throwable[] suppressed = ((ElasticsearchStatusException)ex).getSuppressed();
			if (suppressed!=null && suppressed.length>0) {
				for (Throwable sup:suppressed) {
					if (isErrorIllegalArgument(sup))
						return true;
				}
			}
		}
		return false;		
	}
	
	public static String getIllegalArgumentExpectedType(Throwable ex) {
		if (ex!=null && ex.getMessage()!=null) {
			Matcher m = pIllegalArgumentExpectedType.matcher(ex.getMessage());
			if (m.find())
				return m.group(1);
			m = pCannotBeChanged.matcher(ex.getMessage());
			if (m.find())
				return m.group(2);
		}
		if (ex!=null && ex.getCause()!=null && ex.getCause()!=ex)
			return getIllegalArgumentExpectedType(ex.getCause());
		if (ex instanceof ElasticsearchStatusException) {
			Throwable[] suppressed = ((ElasticsearchStatusException)ex).getSuppressed();
			if (suppressed!=null && suppressed.length>0) {
				for (Throwable sup:suppressed) {
					String argument = getIllegalArgumentExpectedType(sup);
					if (argument!=null)
						return argument;
				}
			}
		}
		return null;				
	}

	public static String getIllegalArgumentInputString(Throwable ex) {
		if (ex!=null && ex.getMessage()!=null) {
			Matcher m = pIllegalArgumentInputString.matcher(ex.getMessage());
			if (m.find())
				return m.group(1);
			m = pIllegalArgumentInputStringAlt.matcher(ex.getMessage());
			if (m.find())
				return m.group(1);
		}
		if (ex!=null && ex.getCause()!=null && ex.getCause()!=ex)
			return getIllegalArgumentInputString(ex.getCause());
		if (ex instanceof ElasticsearchStatusException) {
			Throwable[] suppressed = ((ElasticsearchStatusException)ex).getSuppressed();
			if (suppressed!=null && suppressed.length>0) {
				for (Throwable sup:suppressed) {
					String argument = getIllegalArgumentInputString(sup);
					if (argument!=null)
						return argument;
				}
			}
		}
		return null;				
	}
	
	public static String[] getIllegalArgumentTypeMismatch(Throwable ex) {
		if (ex!=null && ex.getMessage()!=null) {
			Matcher m = pCannotBeChanged.matcher(ex.getMessage());
			if (m.find())
				return new String[] {m.group(1), m.group(2)};
		}
		if (ex!=null && ex.getCause()!=null && ex.getCause()!=ex)
			return getIllegalArgumentTypeMismatch(ex.getCause());
		if (ex instanceof ElasticsearchStatusException) {
			Throwable[] suppressed = ((ElasticsearchStatusException)ex).getSuppressed();
			if (suppressed!=null && suppressed.length>0) {
				for (Throwable sup:suppressed) {
					String[] argument = getIllegalArgumentTypeMismatch(sup);
					if (argument!=null)
						return argument;
				}
			}
		}
		return null;						
	}

	/**
	 * Returns if the ERROR is something like 'Unique index or primary key violation'. 
	 */
	public static boolean isErrorUniqueConstraintViolation(Throwable ex) {
		if (ex==null)
			return false;
		String msg = ex.getMessage();
		if (msg!=null && pUniqueConstraintViolation.matcher(msg).find())
			return true;
		if (ex.getCause()!=null && ex.getCause()!=ex)
			return isErrorUniqueConstraintViolation(ex.getCause());
		return false;
	}
	
	/**
	 * Returns if the ERROR is something like '404 Not Found'
	 */
	public static boolean isErrorNotFound(Throwable ex) {
		if (ex==null)
			return false;
		String msg = ex.getMessage();
		if (msg!=null && pNotFound.matcher(msg).find())
			return true;
		if (pNotFound.matcher(ex.getClass().getSimpleName()).find())
			return true;
		if (ex.getCause()!=null && ex.getCause()!=ex)
			return isErrorNotFound(ex.getCause());
		return false;
	}
}
