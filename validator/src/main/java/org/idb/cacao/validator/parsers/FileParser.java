/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
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
