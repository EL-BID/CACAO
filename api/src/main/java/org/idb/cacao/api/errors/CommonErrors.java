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
package org.idb.cacao.api.errors;

import org.elasticsearch.ElasticsearchStatusException;

/**
 * Some methods for identifying common errors regarding all modules.
 * 
 * @author Gustavo Figueiredo
 */
public class CommonErrors {

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

}
