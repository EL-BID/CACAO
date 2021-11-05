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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.idb.cacao.web.errors.StorageException;
import org.idb.cacao.web.errors.StorageFileNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

/**
 * Service for save files on file system. <br>
 * 
 * It's necessary to define "storage.incoming.files.original.dir" on application.properties file.
 * 
 * @author Luiz Kauer
 * @author Rivelino Patrício
 *
 */
@Service
public class FileSystemStorageService implements IStorageService {
	
	/**
	 *	Root path for save files.
	 *
	 *  Example: /var/uploaded-files/original
	 */
	private final Path rootLocation;
	
	private static final Logger log = Logger.getLogger(FileSystemStorageService.class.getName());

	/**
	 * 
	 * @param uploadFileDir	An enviroment parameter to indicate default location for uploaded files
	 */
	public FileSystemStorageService(@Value("${storage.incoming.files.original.dir}") String uploadFileDir) {
		
		if ( uploadFileDir == null )			
			throw new StorageException("Path for default storage wasn't defined: " + "storage.incoming.files.original.dir");
		
		this.rootLocation = Paths.get(uploadFileDir);
		init();
	}

	/**
	 * Save file to storage
	 */
	@Override
	public String store(String originalFilename, InputStream inputStream, boolean closeInputStream) {
		try {
			
			String subDir = getSubDir();
			Path location = getLocation(subDir);
			Path destinationFile = location.resolve(Paths.get(originalFilename)).normalize().toAbsolutePath();
			
			if (!destinationFile.getParent().equals(location.toAbsolutePath())) {
				// This is a security check
				throw new StorageException("Cannot store file outside current directory.");
			}
			Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
			return subDir;
		} catch (IOException e) {
			throw new StorageException("Failed to store file.", e);
		} finally {
			if (closeInputStream) {
				try {
					inputStream.close();
				} catch (IOException e) {
					log.log(Level.WARNING, "Couldn't close inputstream for file " + originalFilename, e);	
				}
			}
		}
	}

	/**
	 * 
	 * @return	Specific location for store a file
	 */
	private Path getLocation(String subDir) {
		if ( subDir == null || subDir.isEmpty() )
			return rootLocation;
		
		Path location = rootLocation.resolve(subDir);
		try {
			Files.createDirectories(location);
		} catch (IOException e) {			
			log.log(Level.WARNING, "Couldn't create subdir to store file: " + location.getFileName(), e);
			return rootLocation;
		}
		
		return location;
	}

	/**
	 * @param filename	A filename to store
	 * 
	 * @return The path to save a file indicated on filename against root storage path
	 */
	@Override
	public Path find(String filename) {
		if ( rootLocation == null )			
			throw new StorageException("Path for default storage wasn't defined: " + "storage.incoming.files.original.dir");
		Path p = rootLocation.resolve(filename);
		return Files.exists(p) ? p : null;
	}

	/**
	 * @param filename	A filename to be retrieved
	 * 
	 * @return A {@link Resource} that represents a file
	 */
	@Override
	public Resource load(String filename) {
		try {
			Path file = find(filename);
			Resource resource = new UrlResource(file.toUri());
			if (resource.exists() || resource.isReadable()) {
				return resource;
			} else {
				throw new StorageFileNotFoundException("Could not read file: " + filename);

			}
		} catch (MalformedURLException e) {
			throw new StorageFileNotFoundException("Could not read file: " + filename, e);
		}
	}

	/**
	 * Init the storage
	 */
	private void init() {
		if ( rootLocation == null )			
			throw new StorageException("Path for default storage wasn't defined: " + "storage.incoming.files.original.dir");
		try {
			Files.createDirectories(rootLocation);
		} catch (IOException e) {
			throw new StorageException("Could not initialize storage", e);
		}
	}
}
