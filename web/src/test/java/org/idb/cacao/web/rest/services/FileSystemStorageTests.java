package org.idb.cacao.web.rest.services;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;

import org.idb.cacao.web.controllers.services.storage.FileSystemStorageService;
import org.idb.cacao.web.errors.StorageException;
import org.idb.cacao.web.errors.StorageFileNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemStorageTests {

	private FileSystemStorageService service;
	
	@BeforeEach
	public void init(@TempDir Path uploadFileDir) {
		service = new FileSystemStorageService(uploadFileDir.toString());
	}
	
	@Test
	public void loadNonExistent() {
		assertThrows(StorageFileNotFoundException.class, () -> service.load("foo.txt"));
	}
	
	@Test 
	void saveAndFind() {
		String name="foo.txt";
		InputStream inputStream = new ByteArrayInputStream("sample".getBytes());
		service.store(name, inputStream, true);
		assertEquals("foo.txt", service.find("foo.txt").getFileName().toString());
		
	}
	
	@Test
	public void saveRelativePathNotPermitted() {
		String name="../foo.txt";
		InputStream inputStream = new ByteArrayInputStream("sample".getBytes());
		assertThrows(StorageException.class, () -> {
			service.store(name, inputStream, true);
		});
	}
	
	@Test
	public void saveAbsolutePathNotPermitted() {
		String name="/etc/foo.txt";
		InputStream inputStream = new ByteArrayInputStream("sample".getBytes());
		assertThrows(StorageException.class, () -> {
			service.store(name, inputStream, true);
		});
	}
	
	@Test
	public void savePermitted() {
		String name="bar/../foo.txt";
		InputStream inputStream = new ByteArrayInputStream("sample".getBytes());
		service.store(name, inputStream, true);
	}

}
