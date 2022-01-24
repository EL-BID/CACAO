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
package org.idb.cacao.api.templates;

import java.io.Closeable;
import java.util.Map;

/**
 * This is a generic interface for providing a custom implementation for generating random data to be used 
 * as 'samples' of a given template and file format.
 * 
 * @author Gustavo Figueiredo
 *
 */
public interface CustomDataGenerator extends Closeable {
	
	/**
	 * Returns some internally generated 'filename' to be used as 'original filename'. Returns NULL if the filename
	 * may be any arbitrary name.
	 */
	default public String getFileName() {
		return null;
	}
	
	/**
	 * Returns some internally generated 'taxpayer ID' to be used for identification of the generated data. Returns NULL if
	 * the taxpayer ID may be any arbitrary number
	 */
	default public String getTaxpayerId() {
		return null;
	}
	
	/**
	 * Define the fixed 'tax year' to be used with data generated. If NULL, the year may be any arbitrary number. Should be
	 * defined before calling the 'start' method. If the year is not defined, the year may be any arbitrary number.
	 */
	default public void setTaxYear(Number year) { }

	/**
	 * Returns some internally generated 'tax year' to be used for identification of the generated data. Returns NULL if
	 * the year may be any arbitrary number
	 */
	default public Number getTaxYear() {
		return null;
	}

	/**
	 * Method called at the start of the procedure. Should initialize internal state.
	 */
	default public void start() { }
	
	/**
	 * Method called for each generated record. NULL means there is no more record to generate.
	 */
	public Map<String,Object> nextRecord();
}
