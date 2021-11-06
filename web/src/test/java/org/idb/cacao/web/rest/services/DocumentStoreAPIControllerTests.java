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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.nio.file.Path;

import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.mock_es.ElasticsearchMockClient;
import org.idb.cacao.web.controllers.services.storage.IStorageService;
import org.idb.cacao.web.entities.DocumentUploaded;
import org.idb.cacao.web.repositories.DocumentTemplateRepository;
import org.idb.cacao.web.repositories.DocumentUploadedRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(JUnitPlatform.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@SpringBootTest( webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = {
		"storage.incoming.files.original.dir=${java.io.tmpdir}/cacao/storage"
})
class DocumentStoreAPIControllerTests {

	private static ElasticsearchMockClient mockElastic;

	@Autowired
	private IStorageService storageService;
	
	@Autowired
	private DocumentTemplateRepository templateRepository;
	
	@Autowired
	private DocumentUploadedRepository documentsUploadedRepository;
	
	@Autowired
	private MockMvc mockMvc;
	
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
	void setUp() throws Exception {
	}

	@WithUserDetails(value="admin@admin",userDetailsServiceBeanName="CustomUserDetailsService")
	@Test
	void testHandleFileUpload() throws Exception {
		
		// Creates some template for testing
		DocumentTemplate template = new DocumentTemplate();
		template.setName("TEST");
		template.setVersion("1.0");
		templateRepository.save(template);
		
		MockMultipartFile multipartFile = new MockMultipartFile("fileinput", "test.txt",
				"text/plain", "Spring Framework".getBytes());
		this.mockMvc.perform(
				multipart("/api/doc")
				.file(multipartFile)
				.param("template", "TEST"))
				.andExpect(status().isOk());
		
		Page<DocumentUploaded> match_uploads = documentsUploadedRepository.findByFilename("test.txt", PageRequest.ofSize(1));
		assertFalse(match_uploads.isEmpty());
		DocumentUploaded match_upload = match_uploads.getContent().get(0);
		String subdir_and_filename = match_upload.getSubDir()+File.separator+match_upload.getFileId();
		
		Path path = storageService.find(subdir_and_filename);
		assertNotNull(path);
		assertTrue(storageService.delete(subdir_and_filename));
	}
}
