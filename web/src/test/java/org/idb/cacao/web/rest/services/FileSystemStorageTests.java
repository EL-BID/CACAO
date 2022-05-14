/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.rest.services;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;

import org.idb.cacao.api.errors.StorageException;
import org.idb.cacao.api.errors.StorageFileNotFoundException;
import org.idb.cacao.api.storage.FileSystemStorageService;
import org.idb.cacao.mock_es.ElasticsearchMockClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

@RunWith(JUnitPlatform.class)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@SpringBootTest( webEnvironment = WebEnvironment.RANDOM_PORT,
properties = {
"storage.incoming.files.original.dir=${java.io.tmpdir}/cacao/storage"
})
class FileSystemStorageTests {

	private static ElasticsearchMockClient mockElastic;

	private FileSystemStorageService service;
	
	@BeforeAll
	public static void beforeClass() throws Exception {

		int port = ElasticsearchMockClient.findRandomPort();
		mockElastic = new ElasticsearchMockClient(port);
		System.setProperty("es.port", String.valueOf(port));
	}
	
	@AfterAll
	public static void afterClass() {
		if (mockElastic!=null)
			mockElastic.stop();
	}

	@BeforeEach
	public void init(@TempDir Path uploadFileDir) {
		service = new FileSystemStorageService(uploadFileDir.toString());
	}
	
	@Test
	void loadNonExistent() {
		assertThrows(StorageFileNotFoundException.class, () -> service.load("foo.txt"));
	}
	
	@Test 
	void saveAndFind() {
		String name="foo.txt";
		InputStream inputStream = new ByteArrayInputStream("sample".getBytes());
		String subdir = service.store(name, inputStream, true);
		assertEquals("foo.txt", service.find(subdir+File.separator+"foo.txt").getFileName().toString());
		
	}
	
	@Test
	void saveRelativePathNotPermitted() {
		String name="../foo.txt";
		InputStream inputStream = new ByteArrayInputStream("sample".getBytes());
		assertThrows(StorageException.class, () -> {
			service.store(name, inputStream, true);
		});
	}
	
	@Test
	void saveAbsolutePathNotPermitted() {
		String name="/etc/foo.txt";
		InputStream inputStream = new ByteArrayInputStream("sample".getBytes());
		assertThrows(StorageException.class, () -> {
			service.store(name, inputStream, true);
		});
	}
	
	@Test
	void savePermitted() {
		String name="bar/../foo.txt";
		InputStream inputStream = new ByteArrayInputStream("sample".getBytes());
		assertNotNull(service.store(name, inputStream, true));
	}

}
