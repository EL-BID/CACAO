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
