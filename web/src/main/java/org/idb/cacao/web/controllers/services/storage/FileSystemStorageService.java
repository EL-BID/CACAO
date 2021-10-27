package org.idb.cacao.web.controllers.services.storage;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

@Service
public class FileSystemStorageService implements StorageService {

	private final Path rootLocation;

	public FileSystemStorageService(@Value("${storage.incoming.files.original.dir}") String uploadFileDir) {
		this.rootLocation = Paths.get(uploadFileDir);
		init();
	}
	
	@Override
	public Path store(String originalFilename, InputStream inputStream, boolean closeInputStream) {
		try {
			Path destinationFile = this.rootLocation.resolve(
					Paths.get(originalFilename))
					.normalize().toAbsolutePath();
			if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
				// This is a security check
				throw new StorageException(
						"Cannot store file outside current directory.");
			}
			Files.copy(inputStream, destinationFile,
				StandardCopyOption.REPLACE_EXISTING);
			return destinationFile;
		}
		catch (IOException e) {
			throw new StorageException("Failed to store file.", e);
		}
		finally {
			if(closeInputStream) {
				try {
					inputStream.close();
				} catch (IOException e) {
					// TODO: raise error or continue? 
				}
			}
		}
	}

	@Override
	public Path find(String filename) {
		return rootLocation.resolve(filename);
	}
	
	@Override
	public Resource load(String filename) {
		try {
			Path file = find(filename);
			Resource resource = new UrlResource(file.toUri());
			if (resource.exists() || resource.isReadable()) {
				return resource;
			}
			else {
				throw new StorageFileNotFoundException(
						"Could not read file: " + filename);

			}
		}
		catch (MalformedURLException e) {
			throw new StorageFileNotFoundException("Could not read file: " + filename, e);
		}
	}

	private void init() {
		try {
			Files.createDirectories(rootLocation);
		}
		catch (IOException e) {
			throw new StorageException("Could not initialize storage", e);
		}
	}
}
