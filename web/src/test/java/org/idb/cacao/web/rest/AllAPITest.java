package org.idb.cacao.web.rest;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;

import org.idb.cacao.api.DocumentUploaded;
import org.idb.cacao.api.storage.IStorageService;
import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.api.templates.DocumentInput;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.web.repositories.DocumentTemplateRepository;
import org.idb.cacao.web.repositories.DocumentUploadedRepository;
import org.idb.cacao.web.utils.ControllerUtils;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
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
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureJsonTesters
@RunWith(JUnitPlatform.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@SpringBootTest( webEnvironment = WebEnvironment.DEFINED_PORT, classes = {org.idb.cacao.web.WebApplication.class}, properties = {
		"storage.incoming.files.original.dir=${java.io.tmpdir}/cacao/storage"
})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AllAPITest {
    
    private static final String ELASTICSEARCH_VERSION = "7.14.1";
    
    private static final Integer ELASTICSEARCH_PORT = 9200;
    
	@Autowired
	private IStorageService storageService;
	
	@Autowired
	private DocumentTemplateRepository templateRepository;
	
	@Autowired
	private DocumentUploadedRepository documentsUploadedRepository;    
	
	@Autowired
	private MockMvc mockMvc;	

    @Container
    private static ElasticsearchContainer esContainer = 
        new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:" + ELASTICSEARCH_VERSION)
            .withEnv("discovery.type", "single-node")
            .withExposedPorts(ELASTICSEARCH_PORT);


    @BeforeAll
    public static void beforeAll() {
    	
    	ControllerUtils.setRunningTest(true);
    	
        esContainer.setWaitStrategy(Wait.forHttp("/")
            .forPort(9200)
            .forStatusCode(200)
            .withStartupTimeout(Duration.ofSeconds(120)));
        esContainer.start();
        
        int containerPort = esContainer.getMappedPort(ELASTICSEARCH_PORT);
        System.setProperty("es.port", "" + containerPort);
        System.setProperty("es.host", "127.0.0.1");
        System.setProperty("ssl.trust.server", esContainer.getHost());
        System.setProperty("i18n.locale", "en_US");
        System.setProperty("user.country", "en_US");
        System.setProperty("user.language", "en-US");
        System.setProperty("spring.mvc.locale", "en_US");        
    }

	@AfterAll
	public static void afterClass() {
		if (esContainer !=null)
            esContainer.stop();
		ControllerUtils.setRunningTest(false);
	}

    @Test //esShouldBeUpAndRunning    
    void test01() {
        assertTrue(esContainer.isRunning());
    }

	@WithUserDetails(value="admin@admin",userDetailsServiceBeanName="CustomUserDetailsService")
	@Test //HandleFileUpload
	void test02() throws Exception {
		
		// Creates some template for testing
		DocumentTemplate template = new DocumentTemplate();
		template.setName("TEST");
		template.setVersion("1.0");
		DocumentInput input = new DocumentInput();
		input.setInputName("CSV");
		input.setFormat(DocumentFormat.CSV);
		template.setInputs(Arrays.asList(input));
		templateRepository.save(template);
		
		MockMultipartFile multipartFile = new MockMultipartFile("fileinput", "test.txt",
				"text/plain", "Spring Framework".getBytes());
		this.mockMvc.perform(
				multipart("/api/doc")
				.file(multipartFile)
				.with(csrf())
				.param("templateName", "TEST")
				.param("templateVersion", "1.0")
				.param("inputName", "CSV"))
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
