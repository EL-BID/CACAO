package org.idb.cacao.web.rest.services;

import static org.junit.jupiter.api.Assertions.*;

import org.idb.cacao.web.controllers.services.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

@AutoConfigureMockMvc
@SpringBootTest( webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = {
		"storage.incoming.files.original.dir=${java.io.tmpdir}/cacao/storage"
})
class DocumentStoreAPIControllerTests {

	@Autowired
	private StorageService storageService;
	
	@Autowired
	private MockMvc mockMvc;
	
	@BeforeEach
	void setUp() throws Exception {
	}

	@WithMockUser(value = "admin@admin")
	@Test
	void testHandleFileUpload() throws Exception {
		MockMultipartFile multipartFile = new MockMultipartFile("fileinput", "test.txt",
				"text/plain", "Spring Framework".getBytes());
		this.mockMvc.perform(
				multipart("/api/doc")
				.file(multipartFile)
				.param("template", ""))
				.andExpect(status().isOk());
		Path path = storageService.find("test.txt");
		System.out.println(path);
		assertNotNull(storageService.find("test.txt"));
		
	}
}
