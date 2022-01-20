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
package org.idb.cacao.web.utils.generators;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.Map;

import org.idb.cacao.api.templates.DocumentInput;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.utils.RandomDataGenerator.DomainTableRepository;

/**
 * This is a common interface for generating random data of a particular FileFormat.<BR>
 * One instance of this interface should not be used with different threads.<BR>
 * It may be necessary to hold some state regarding one particular file.
 * 
 * @author Gustavo Figueiredo
 *
 */
public interface FileGenerator extends Closeable {

	/**
	 * Indicates the file that needs to be generated
	 */
	public Path getPath();
	
	/**
	 * Indicates the file that needs to be generated
	 */
	public void setPath(Path path);
	
	/**
	 * Initial seed used for random number generation
	 */
	public long getRandomSeed();
	
	/**
	 * Initial seed used for random number generation
	 */
	public void setRandomSeed(long seed);
	
	/**
	 * Template of document to be generated
	 */
	public DocumentTemplate getDocumentTemplate();
	
	/**
	 * Template of document to be generated
	 */
	public void setDocumentTemplate(DocumentTemplate template);

	/**
	 * Specification for generating random data according to the DocumentTemplate's DocumentInput
	 */
	public DocumentInput getDocumentInputSpec();
	
	/**
	 * Specification for generating random data according to the DocumentTemplate's DocumentInput
	 */
	public void setDocumentInputSpec(DocumentInput input);

	/**
	 * Object used for retrieving registered domain tables given a domain table name and version
	 */
	public DomainTableRepository getDomainTableRepository();
	
	/**
	 * Object used for retrieving registered domain tables given a domain table name and version
	 */
	public void setDomainTableRepository(DomainTableRepository domainTableRepository);

	/**
	 * Start creating file with random data. Every information needed for this task
	 * should be set previously to this method call. After inserting data, the 'close' method
	 * must be called in order to finish the file creation.
	 */
	public void start() throws Exception;
	
	/**
	 * Returns some internally generated 'filename' to be used as 'original filename', regardless of the
	 * actual filename informed at {@link #setPath(Path) setPath}
	 */
	public String getOriginalFileName();

	/**
	 * Add a new record to the file using random number generator
	 */
	public void addRandomRecord();
	
	/**
	 * Add a new record to the file using the provided fields
	 */
	public void addRecord(Map<String,Object> record);
}
