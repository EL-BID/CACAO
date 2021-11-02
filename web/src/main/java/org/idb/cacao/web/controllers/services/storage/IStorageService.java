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
package org.idb.cacao.web.controllers.services.storage;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.OffsetDateTime;

import org.idb.cacao.web.utils.DateTimeUtils;
import org.springframework.core.io.Resource;

/**
 * An interface to a storage system
 * 
 * @author Luis Kauer
 * @author Rivelino Patrício
 *
 */
public interface IStorageService {

	/**
	 * 
	 * @param originalFilename
	 * @param inputStream
	 * @param closeInputStream
	 * @return
	 */
	String store(String originalFilename, InputStream inputStream, boolean closeInputStream);

	/**
	 * 
	 * @param filename
	 * @return
	 */
	Path find(String filename);
	
	/**
	 * 
	 * @param filename
	 * @return
	 */
	Resource load(String filename);
	
	/**
	 * 
	 * @return	A subdir of storage where a file should be stored
	 */
	public default String getSubDir() {		
		OffsetDateTime now = DateTimeUtils.now();
		return now.getYear() + File.separator + now.getMonthValue();		
	}
	
	/**
	 * 
	 * @param originalFilename	The name of file to store
	 * @return	The full path relative to the root path where the file has to be stored
	 */
	public default String getFilenameWithPath(String originalFilename) {
		if ( originalFilename == null )
			return null;
	
		return getSubDir() + File.separator + originalFilename;
	}

}
