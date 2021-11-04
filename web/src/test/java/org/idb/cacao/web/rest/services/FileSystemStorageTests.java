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
