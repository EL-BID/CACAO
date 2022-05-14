/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.validator.fileformats;

import java.util.ArrayList;
import java.util.EnumMap;
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
	
	private FileFormatFactory() {		
	}
	
	// Registers all built-in FileFormat implementations
	static {
		allFormats = new EnumMap<>(DocumentFormat.class);		
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
