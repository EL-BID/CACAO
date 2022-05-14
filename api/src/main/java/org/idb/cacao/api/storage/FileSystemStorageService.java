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
import java.net.MalformedURLException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.idb.cacao.api.errors.StorageException;
import org.idb.cacao.api.errors.StorageFileNotFoundException;
import org.idb.cacao.api.utils.ParserUtils;
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
 * @author Gustavo Figueiredo
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
	
	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.storage.IStorageService#getRootLocation()
	 */
	@Override
	public Path getRootLocation() {
		return rootLocation;
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
	@Override
	public Path getLocation(String subDir) {
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
	 * Look for a file with the given filename in the current file storage. Returns NULL if absent.
	 * @param filename Filename including relative sub directories if any. 
	 * @return Returns the resolved Path object if the file could be found. Returns NULL if file is not found.
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
			if (file==null)
				throw new StorageFileNotFoundException("Could not find file: " + filename);
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

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.storage.IStorageService#deleteAll()
	 */
	@Override
	public int deleteAll() {
		if (rootLocation==null)
			return 0;
		final LongAdder count_files = new LongAdder();
		try {
			Files.walkFileTree(rootLocation, new FileVisitor<Path>() {

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (file.toFile().delete())
					count_files.increment();
				else
					log.log(Level.WARNING, "Could not delete file "+file.toFile().getAbsolutePath());
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				if (!dir.toFile().delete())
					log.log(Level.WARNING, "Could not remove directory "+dir.toFile().getAbsolutePath());
				return FileVisitResult.CONTINUE;
			}
			// Called after a directory visit is complete.
			});
		}
		catch (IOException ex) {
			log.log(Level.SEVERE, "Error while deleting files in file storage", ex);
		}
		
		return count_files.intValue();
	}
	
	/**
	 * Lists all original files stored in a time range
	 * @param startingTimestamp Starting date/time (in unix epoch)
	 * @param endingTimestamp Ending date/time (in unix epoch)
	 * @param interrupt If different than NULL, the provided function may return TRUE if it should interrupt
	 * @param consumer Callback for each file
	 */
	@Override
	public void listOriginalFiles(
			final long startingTimestamp, 
			final long endingTimestamp, 
			final BooleanSupplier interrupt,
			final Consumer<File> consumer) throws IOException {
		if (rootLocation==null || !Files.exists(rootLocation))
			return;
		
		Files.walkFileTree(rootLocation, new FileVisitorTimeHierarchy(startingTimestamp, endingTimestamp, interrupt, consumer));
	}

	/**
	 * Object used for traversing a hierarchy of subdirectories with the following structure:<BR>
	 * 1st level: years<BR>
	 * 2nd level: months<BR>
	 * 
	 * @author Gustavo Figueiredo
	 *
	 */
	public static class FileVisitorTimeHierarchy implements FileVisitor<Path> {
		
		private final LongAdder inc_hierarchy;
		private final AtomicInteger searching_year;
		private final AtomicInteger searching_month;
		
		private final BooleanSupplier interrupt;
		private final Consumer<File> consumer;
		
		private final int start_year;
		private final int start_month;
		
		private final int end_year;
		private final int end_month;

		private final long startingTimestamp;
		private final long endingTimestamp;
		
		public FileVisitorTimeHierarchy(
				final long startingTimestamp, 
				final long endingTimestamp,
				BooleanSupplier interrupt,
				final Consumer<File> consumer) {
			
			this.interrupt = interrupt;
			this.consumer = consumer;
			
			inc_hierarchy = new LongAdder();
			searching_year = new AtomicInteger();
			searching_month = new AtomicInteger();
			
			final Calendar cal = Calendar.getInstance();
			
			cal.setTimeInMillis(startingTimestamp);
			this.start_year = cal.get(Calendar.YEAR);
			this.start_month = cal.get(Calendar.MONTH)+1;
			
			cal.setTimeInMillis(endingTimestamp);
			this.end_year = cal.get(Calendar.YEAR);
			this.end_month = cal.get(Calendar.MONTH)+1;
			
			this.startingTimestamp = startingTimestamp;
			this.endingTimestamp = endingTimestamp;
		}

		// Called after a directory visit is complete.
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                throws IOException {
        	inc_hierarchy.decrement();
        	return FileVisitResult.CONTINUE;
        }
        
        // called before a directory visit.
        @Override
        public FileVisitResult preVisitDirectory(Path dir,
                BasicFileAttributes attrs) throws IOException {
        	if (interrupt!=null && interrupt.getAsBoolean())
        		return FileVisitResult.TERMINATE;
        	File subdir = dir.toFile();
        	String subdir_name = subdir.getName();
        	final int hierarchy_level = inc_hierarchy.intValue();
        	switch (hierarchy_level) {
    			case 0: // first level: the root dir
    				inc_hierarchy.increment();
    				return FileVisitResult.CONTINUE;        				
        		case 1: // second level: year of date/time of upload
        			if (!ParserUtils.isOnlyNumbers(subdir_name))
        				return FileVisitResult.SKIP_SUBTREE;
        			else {
        				int year = Integer.parseInt(subdir_name);
        				if (year<start_year || year>end_year)
        					return FileVisitResult.SKIP_SUBTREE;
        				searching_year.set(year);
        				inc_hierarchy.increment();
        				return FileVisitResult.CONTINUE;
        			}
        		case 2: // third level: month of date/time of upload
        			if (!ParserUtils.isOnlyNumbers(subdir_name))
        				return FileVisitResult.SKIP_SUBTREE;
        			else {
        				int month = Integer.parseInt(subdir_name);
        				if (month<1 || month>12)
        					return FileVisitResult.SKIP_SUBTREE;
        				if (searching_year.get()==start_year && month<start_month)
        					return FileVisitResult.SKIP_SUBTREE;
        				if (searching_year.get()==end_year && month>end_month)
        					return FileVisitResult.SKIP_SUBTREE;
        				searching_month.set(month);
        				inc_hierarchy.increment();
        				return FileVisitResult.CONTINUE;
        			}
        		default: // forth level and beyond: don't care about subdirs
        			return FileVisitResult.SKIP_SUBTREE;
        	}
        }
        
        // This method is called for each file visited. The basic attributes of the files are also available.
        @Override
        public FileVisitResult visitFile(Path file,
                BasicFileAttributes attrs) throws IOException {
        	if (interrupt!=null && interrupt.getAsBoolean())
        		return FileVisitResult.TERMINATE;
        	File f = file.toFile();
        	long file_timestamp = f.lastModified();
        	if (file_timestamp>=startingTimestamp && file_timestamp<endingTimestamp) {
        		consumer.accept(f);
        	}
        	return FileVisitResult.CONTINUE;
        }
        
        // if the file visit fails for any reason, the visitFileFailed method is called.
        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc)
                throws IOException {
        	return FileVisitResult.CONTINUE;
        }		
	}
	
}
