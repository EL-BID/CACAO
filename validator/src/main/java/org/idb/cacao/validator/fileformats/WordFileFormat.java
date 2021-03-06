/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.validator.fileformats;

import static org.idb.cacao.validator.fileformats.FileFormat.*;

import java.util.regex.Pattern;

import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.validator.parsers.FileParser;
import org.idb.cacao.validator.parsers.WordParser;

/**
 * FileFormat for files associated to MS Word (e.g.: DOC, DOCX).
 * 
 * @author Gustavo Figueiredo
 *
 */
public class WordFileFormat implements FileFormat {

	public static Pattern COMMON_FILE_EXTENSION = Pattern.compile("\\.doc[x]?$",Pattern.CASE_INSENSITIVE);
			
	public static byte[][] EXPECTED_FILE_HEADER = {
		new byte[] { (byte)0xD0, (byte)0xCF, (byte)0x11, (byte)0xE0, (byte)0xA1, (byte)0xB1, (byte)0x1A, (byte)0xE1 }, // DOC
		new byte[] { (byte)0x50, (byte)0x4B, (byte)0x03, (byte)0x04 } // DOCX (actually any ZIP)
	};

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.fileformats.FileFormat#getFormat()
	 */
	@Override
	public DocumentFormat getFormat() {
		return DocumentFormat.DOC;
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
		return new WordParser();
	}

}
