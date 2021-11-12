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

import java.io.File;

import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.validator.parsers.FileParser;

/**
 * Interface for all known file formats that may be parsed by this application<BR>
 * The implementation must be THREAD-SAFE.
 * 
 * @author Gustavo Figueiredo
 *
 */
public interface FileFormat {
	
	/**
	 * Returns the format of this file according to the {@link DocumentFormat DocumentFormat} enumeration.
	 */
	public DocumentFormat getFormat();
	
	/**
	 * Returns TRUE if the given filename matches the expected filename for this FileFormat (usually check the
	 * file extension).<BR>
	 * Should return FALSE if the filename does not match this object at all.<BR>
	 * Should return NULL if it's not possible to determine this based on the file name or file extension.<BR>
	 * This method should not try to read the file contents!
	 */
	public Boolean matchFilename(String filename);
	
	/**
	 * Returns TRUE if the given head of file contents matches the extected contents for this FileFormat.<BR>
	 * IMPORTANT: the provided content is just a small part of the beginning of the file. It's not the entire file contents!<BR>
	 * Should return FALSE if this file contents does not match this object at all.<BR>
	 * Should return NULL it it's not possible to determine this based on this small sample.<BR>
	 * @param firstBytes The bytes of the beginning of the file. Should only consider up to 'firstBytesLength' bytes.
	 * @param firstBytesLength The actual size of 'firstBytes'. Don't rely on the array length!
	 */
	public Boolean matchFileHeader(byte[] firstBytes, int firstBytesLength);

	/**
	 * Returns a new instance of a object that may be used for parsing files of this format.<BR>
	 * The returned instance may hold some state regarding one file processing.<BR>
	 * It should not be shared with different threads.
	 */
	public FileParser createFileParser();
	
	/**
	 * Given a filename, returns the file extension (including the dot). Returns empty if the
	 * extension was not found
	 */
	public static String getFileExtension(String filename) {
		if (filename==null)
			return "";
		int sep = filename.lastIndexOf(File.separator);
		if (sep>=0)
			filename = filename.substring(sep+1);
		int dot = filename.lastIndexOf(".");
		if (dot>0)
			return filename.substring(dot).trim();
		return "";
	}
	
	/**
	 * Given the first bytes of a file, returns TRUE if this contents match the expected binary header
	 */
	public static boolean matchesFileHeader(byte[] fileHeader, int header_length, byte[] expected_header) {
		if (expected_header!=null 
				&& header_length>=expected_header.length) {
			for (int i=0; i<expected_header.length; i++) {
				if (fileHeader[i]!=expected_header[i])
					break; // header does not match extected_header
				if (i==expected_header.length-1)
					return true;
			}
		}
		return false;
	}

}
