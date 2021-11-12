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

import static org.idb.cacao.validator.fileformats.FileFormat.*;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.validator.parsers.FileParser;
import org.idb.cacao.validator.parsers.PDFParser;

/**
 * FileFormat for files associated to PDF (with forms)
 * 
 * @author Gustavo Figueiredo
 *
 */
public class PDFFileFormat implements FileFormat {

	public static Pattern COMMON_FILE_EXTENSION = Pattern.compile("\\.pdf$",Pattern.CASE_INSENSITIVE);
	
	public static byte[] EXPECTED_FILE_HEADER = "%PDF".getBytes(StandardCharsets.UTF_8);
			
	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.fileformats.FileFormat#getFormat()
	 */
	@Override
	public DocumentFormat getFormat() {
		return DocumentFormat.PDF;
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
		return matchesFileHeader(firstBytes, firstBytesLength, EXPECTED_FILE_HEADER);
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.fileformats.FileFormat#createFileParser()
	 */
	@Override
	public FileParser createFileParser() {
		return new PDFParser();
	}

}
