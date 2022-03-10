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
package org.idb.cacao.web.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Utilifies to work with files
 * 
 * @author Rivelino Patrício
 * 
 * @since 10/03/2022
 *
 */
public class FileUtils {

	/**
	 * Test if a file is a compressed file
	 * @param file	A file to be tested
	 * @return	True if file is a compressed file. False if not.
	 */
	public static boolean isCompressedFile(File file) {	
		try {
		    new ZipFile(file).close();
		    return true;
		} catch (IOException e) {
		    return false;
		}
	}

	/**
	 * Test if a stream is a compressed file inputstream
	 * @param file	A file stream to be tested
	 * @return	True if file is a compressed file. False if not.
	 */
	public static boolean isCompressedFile(InputStream inputStream) {		
		try {
			return new ZipInputStream(inputStream).getNextEntry() != null;
		}
		catch (IOException e) {
		    return false;
		}		
	}

}
