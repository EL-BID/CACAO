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
package org.idb.cacao.validator.fileformats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.idb.cacao.api.templates.DocumentFormat;

/**
 * Factory of 'FileFormat' objects
 * 
 * @author Gustavo Figueiredo
 *
 */
public class FileFormatFactory {
	
	private static final Map<DocumentFormat, FileFormat> allFormats;
	
	// Registers all built-in FileFormat implementations
	static {
		allFormats = new HashMap<>();
		registerFileFormat(new CSVFileFormat());
		registerFileFormat(new WordFileFormat());
		registerFileFormat(new JSONFileFormat());
		registerFileFormat(new PDFFileFormat());
		registerFileFormat(new ExcelFileFormat());
		registerFileFormat(new XMLFileFormat());
	}
	
	/**
	 * Register a 'FileFormat' to be used in this application. For each DocumentFormat
	 * there should be only one FileFormat.
	 */
	public static void registerFileFormat(FileFormat fileFormat) {
		if (fileFormat!=null)
			allFormats.put(fileFormat.getFormat(), fileFormat);
	}
	
	/**
	 * Unregister a previously registered 'FileFormat' associated to the DocumentFormat.
	 */
	public static void unregisterFileFormat(DocumentFormat format) {
		if (format!=null)
			allFormats.remove(format);
	}
	
	/**
	 * Returns all registered FileFormat's 
	 */
	public static List<FileFormat> getAllFileFormats() {
		return new ArrayList<>(allFormats.values());
	}
	
	/**
	 * Returns whether exists a FileFormat object related to the DocumentFormat
	 */
	public static boolean hasFileFormat(DocumentFormat format) {
		return format!=null && allFormats.containsKey(format);
	}

	/**
	 * Returns a 'FileFormat' object related to a 'DocumentFormat'
	 */
	public static FileFormat getFileFormat(DocumentFormat format) {
		FileFormat fileFormat = allFormats.get(format);
		if (fileFormat==null)
			throw new UnsupportedOperationException("Not implemented a FileFormat for DocumentFormat "+format);
		return fileFormat;
	}
	
}
