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
package org.idb.cacao.validator.parsers;

import java.io.Closeable;
import java.nio.file.Path;

import org.idb.cacao.api.templates.DocumentInput;
import org.idb.cacao.api.templates.DocumentTemplate;

/**
 * This is a common interface for processing files of a particular FileFormat.<BR>
 * One instance of this interface should not be used with different threads.<BR>
 * It may be necessary to hold some state regarding one particular file.
 * 
 * @author Gustavo Figueiredo
 *
 */
public interface FileParser extends Closeable {

	/**
	 * Indicates the file that needs to be processed
	 */
	public Path getPath();
	
	/**
	 * Indicates the file that needs to be processed
	 */
	public void setPath(Path path);
	
	/**
	 * Specification for this input according to the DocumentTemplate's DocumentInput
	 */
	public DocumentInput getDocumentInputSpec();
	
	/**
	 * Specification for this input according to the DocumentTemplate's DocumentInput
	 */
	public void setDocumentInputSpec(DocumentInput input);
	
	/**
	 * Specification for this template
	 */
	public default void setDocumentTemplate(DocumentTemplate template) { }
	
	/**
	 * Trigger the start of the file processing. Every information needed for this task
	 * should be set previously to this method call.
	 */
	public void start();
	
	/**
	 * Returns the 'DataIterator' to create on demand and return each record from the file.
	 * It should be invoke only after the {@link #start() start} method.
	 */
	public DataIterator iterator();
	
	/**
	 * Method used at the end of the processing. The implementation should release any resources
	 * that were used up to this point.
	 */
	public void close();
}
