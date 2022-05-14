/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.api.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

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
	 * Look for a file with the given filename in the current file storage. Returns NULL if absent.
	 * @param filename Filename including relative sub directories if any. 
	 * @return Returns the resolved Path object if the file could be found. Returns NULL if file is not found.
	 */
	Path find(String filename);
	
	/**
	 * Returns a specific location to store a file
	 */
	Path getLocation(String subDir);

	/**
	 * Deletes the file from the storage
	 */
	default boolean delete(String filename) {
		Path p = find(filename);
		if (p==null)
			return false;
		try {
			Files.delete(p);
			return true;
		} catch (IOException ex) {
			return false;
		}
	}
	
	/**
	 * 
	 * @param filename
	 * @return
	 */
	Resource load(String filename);
	
	/**
	 * Deletes all files stored.
	 * @return Return the number of files deleted
	 */
	int deleteAll();
	
	/**
	 * Lists all original files stored in a time range
	 * @param startingTimestamp Starting date/time (in unix epoch)
	 * @param endingTimestamp Ending date/time (in unix epoch)
	 * @param interrupt If different than NULL, the provided function may return TRUE if it should interrupt
	 * @param consumer Callback for each file
	 */
	public void listOriginalFiles(
			final long startingTimestamp, 
			final long endingTimestamp, 
			final BooleanSupplier interrupt,
			final Consumer<File> consumer) throws IOException;
	
	/**
	 * Returns the root location for all the subdirectories containing original files
	 */
	public Path getRootLocation();
	
	/**
	 * 
	 * @return	A subdir of storage where a file should be stored
	 */
	public default String getSubDir() {		
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		return now.getYear() + File.separator + now.getMonthValue();		
	}
	
	/**
	 * Returns the filename with subdirectory reference. The subdirectory if take from
	 * the current system clock. So different calls of this method with the same filename
	 * at different times may have different outcomes.
	 * @param originalFilename	The name of file to store
	 * @return	The full path relative to the root path where the file has to be stored
	 */
	public default String getFilenameWithPath(String originalFilename) {
		if ( originalFilename == null )
			return null;
	
		return getSubDir() + File.separator + originalFilename;
	}

}
