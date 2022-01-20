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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.idb.cacao.api.templates.DocumentFormat;

/**
 * Factory of 'FileGenerator' objects
 * 
 * @author Gustavo Figueiredo
 *
 */
public class FileGenerators {

	private static final Map<DocumentFormat, Supplier<FileGenerator>> allFormats;
	
	// Registers all built-in FileFormat implementations
	static {
		allFormats = new HashMap<>();
		registerFileGenerator(DocumentFormat.XLS, ExcelGenerator::new);
	}
	
	/**
	 * Register a 'FileGenerator' to be used in this application. For each DocumentFormat
	 * there should be only one FileGenerator.
	 */
	public static void registerFileGenerator(DocumentFormat format, Supplier<FileGenerator> generator) {
		if (format!=null && generator!=null)
			allFormats.put(format, generator);
	}
	
	/**
	 * Unregister a previously registered 'FileGenerator' associated to the DocumentFormat.
	 */
	public static void unregisterFileGenerator(DocumentFormat format) {
		if (format!=null)
			allFormats.remove(format);
	}
	
	/**
	 * Returns all registered FileGenerator 
	 */
	public static Map<DocumentFormat, Supplier<FileGenerator>> getAllFileGenerators() {
		return allFormats;
	}
	
	/**
	 * Returns whether exists a FileGenerator object related to the DocumentFormat
	 */
	public static boolean hasFileGenerator(DocumentFormat format) {
		return format!=null && allFormats.containsKey(format);
	}

	/**
	 * Returns a new instance of 'FileGenerator' object related to a 'DocumentFormat'
	 */
	public static FileGenerator getFileGenerator(DocumentFormat format) {
		Supplier<FileGenerator> fileGeneratorFactory = allFormats.get(format);
		if (fileGeneratorFactory==null)
			throw new UnsupportedOperationException("Not implemented a FileGenerator for DocumentFormat "+format);
		return fileGeneratorFactory.get();
	}

}
