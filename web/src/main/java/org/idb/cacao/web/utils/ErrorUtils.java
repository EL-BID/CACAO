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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.elasticsearch.ElasticsearchStatusException;

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
}
