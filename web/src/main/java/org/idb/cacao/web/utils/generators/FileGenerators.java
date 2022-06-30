/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.utils.generators;

import java.util.EnumMap;
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
		allFormats = new EnumMap<>(DocumentFormat.class);
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
