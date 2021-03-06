/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.validator.fileformats;

import static org.idb.cacao.validator.fileformats.FileFormat.*;

import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.validator.parsers.CSVParser;
import org.idb.cacao.validator.parsers.FileParser;

/**
 * FileFormat for text files associated to the 'Comma Separated Value' format (not only 'commas' may be used
 * as a separator).
 * 
 * @author Gustavo Figueiredo
 *
 */
public class CSVFileFormat implements FileFormat {

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.fileformats.FileFormat#getFormat()
	 */
	@Override
	public DocumentFormat getFormat() {
		return DocumentFormat.CSV;
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
			|| XMLFileFormat.COMMON_FILE_EXTENSION.matcher(ext).find()
			|| JSONFileFormat.COMMON_FILE_EXTENSION.matcher(ext).find())
			return false;

		return null; // for any other extension, may be a CSV or may be not
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.fileformats.FileFormat#matchFileHeader(byte[], int)
	 */
	@Override
	public Boolean matchFileHeader(byte[] firstBytes, int firstBytesLength) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.fileformats.FileFormat#createFileParser()
	 */
	@Override
	public FileParser createFileParser() {
		return new CSVParser();
	}

}
