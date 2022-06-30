/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.validator.fileformats;

import static org.idb.cacao.validator.fileformats.FileFormat.getFileExtension;

import java.util.regex.Pattern;

import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.validator.parsers.FileParser;
import org.idb.cacao.validator.parsers.JSONParser;

/**
 * FileFormat for files associated to JSON format.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class JSONFileFormat implements FileFormat {
	public static Pattern COMMON_FILE_EXTENSION = Pattern.compile("\\.json",Pattern.CASE_INSENSITIVE);

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.fileformats.FileFormat#getFormat()
	 */
	@Override
	public DocumentFormat getFormat() {
		return DocumentFormat.JSON;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.fileformats.FileFormat#matchFilename(java.lang.String)
	 */
	@Override
	public Boolean matchFilename(String filename) {
		final String ext = getFileExtension(filename);
		if (ExcelFileFormat.COMMON_FILE_EXTENSION.matcher(ext).find()
			|| PDFFileFormat.COMMON_FILE_EXTENSION.matcher(ext).find()
			|| WordFileFormat.COMMON_FILE_EXTENSION.matcher(ext).find()
			|| XMLFileFormat.COMMON_FILE_EXTENSION.matcher(ext).find())
			return false;

		return COMMON_FILE_EXTENSION.matcher(getFileExtension(filename)).find(); // for any other extension, may be a CSV or may be not
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.fileformats.FileFormat#matchFileHeader(byte[], int)
	 */
	@Override
	public Boolean matchFileHeader(byte[] firstBytes, int firstBytesLength) {
		// Expects a JSON to start with a '{' or a '['. Ignores the heading spaces.
		for (int i=0; i<firstBytesLength; i++) {
			byte b = firstBytes[i];
			if (b==' ' || b=='\t' || b=='\r' || b=='\n')
				continue;
			return b=='{' || b=='[';				
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.fileformats.FileFormat#createFileParser()
	 */
	@Override
	public FileParser createFileParser() {
		return new JSONParser();
	}

}
