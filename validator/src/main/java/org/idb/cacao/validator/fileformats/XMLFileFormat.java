/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.validator.fileformats;

import static org.idb.cacao.validator.fileformats.FileFormat.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.util.regex.Pattern;

import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.validator.parsers.FileParser;
import org.idb.cacao.validator.parsers.XMLParser;

/**
 * FileFormat for files associated to XML format.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class XMLFileFormat implements FileFormat {
	
	public static Pattern COMMON_FILE_EXTENSION = Pattern.compile("\\.xml$",Pattern.CASE_INSENSITIVE);

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.fileformats.FileFormat#getFormat()
	 */
	@Override
	public DocumentFormat getFormat() {
		return DocumentFormat.XML;
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
			|| JSONFileFormat.COMMON_FILE_EXTENSION.matcher(ext).find())
			return false;

		return COMMON_FILE_EXTENSION.matcher(getFileExtension(filename)).find(); // for any other extension, may be a CSV or may be not
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.fileformats.FileFormat#matchFileHeader(byte[], int)
	 */
	@Override
	public Boolean matchFileHeader(byte[] firstBytes, int firstBytesLength) {
		String contentType;
		try {
			contentType = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(firstBytes, 0, firstBytesLength));
		} catch (IOException e) {
			return null;
		}
		return "application/xml".equalsIgnoreCase(contentType);
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.fileformats.FileFormat#createFileParser()
	 */
	@Override
	public FileParser createFileParser() {
		return new XMLParser();
	}

}
