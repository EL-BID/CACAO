/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.validator.fileformats;

import java.util.regex.Pattern;

import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.validator.parsers.ExcelParser;
import org.idb.cacao.validator.parsers.FileParser;

import static org.idb.cacao.validator.fileformats.FileFormat.*;

/**
 * FileFormat for files associated to MS Excel (e.g.: XLS, XLSX, XLSM).
 * 
 * @author Gustavo Figueiredo
 *
 */
public class ExcelFileFormat implements FileFormat {

	public static Pattern COMMON_FILE_EXTENSION = Pattern.compile("\\.xls[xm]?$",Pattern.CASE_INSENSITIVE);
			
	public static byte[][] EXPECTED_FILE_HEADER = {
		new byte[] { (byte)0xD0, (byte)0xCF, (byte)0x11, (byte)0xE0, (byte)0xA1, (byte)0xB1, (byte)0x1A, (byte)0xE1 }, // XLS
		new byte[] { (byte)0x50, (byte)0x4B, (byte)0x03, (byte)0x04 } // XLSX or XLSM (actually any ZIP)
	};

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.fileformats.FileFormat#getFormat()
	 */
	@Override
	public DocumentFormat getFormat() {
		return DocumentFormat.XLS;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.fileformats.FileFormat#matchFilename(java.lang.String)
	 */
	@Override
	public Boolean matchFilename(String filename) {
		return COMMON_FILE_EXTENSION.matcher(getFileExtension(filename)).find();
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.fileformats.FileFormat#matchFileHeader(byte[], int)
	 */
	@Override
	public Boolean matchFileHeader(byte[] firstBytes, int firstBytesLength) {
		return matchesFileHeader(firstBytes, firstBytesLength, EXPECTED_FILE_HEADER[0])
			|| matchesFileHeader(firstBytes, firstBytesLength, EXPECTED_FILE_HEADER[1]);
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.fileformats.FileFormat#createFileParser()
	 */
	@Override
	public FileParser createFileParser() {
		return new ExcelParser();
	}

}
