package org.idb.cacao.web.rest;
/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software 
 * son los programados por los desarrolladores y no necesariamente reflejan el punto 
 * de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import org.idb.cacao.api.DocumentUploaded;
import org.idb.cacao.api.Taxpayer;
import org.idb.cacao.api.storage.IStorageService;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.web.dto.TaxpayerDto;
import org.idb.cacao.web.dto.UserDto;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.entities.UserProfile;
import org.idb.cacao.web.repositories.DocumentTemplateRepository;
import org.idb.cacao.web.repositories.DocumentUploadedRepository;
import org.idb.cacao.web.repositories.TaxpayerRepository;
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
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.jayway.jsonpath.JsonPath;


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
	private DocumentTemplateRepository documentTemplateRepository;
	
	@Autowired
	private DocumentUploadedRepository documentsUploadedRepository;
	
	@Autowired
	private TaxpayerRepository taxpayerRepository;	
	
	@Autowired
	private MockMvc mockMvc;
	
	@Autowired
	private JacksonTester<Object> json;	

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

    @Test    
    void test00esShouldBeUpAndRunning() {
        assertTrue(esContainer.isRunning());
    }
    
	@WithUserDetails(value="admin@admin",userDetailsServiceBeanName="CustomUserDetailsService")
	@Test
	void test01HandleCreateTaxpayer() throws Exception {
		
		String[][] taxPayerData = { 
				{"29847163744173200000","Diana Mullis","Consumer Staples","Limited Liability Company","North County","Small","Presumed profit"},
				{"80465967848774987165","Donald Elliott","Materials","Limited Liability Company","Central County", "Medium","Presumed profit"},
				{"85509634237883761680","Loretta Camp","Financials","Limited Liability Company","South County","Large","Presumed profit"}
		};
		
		for (String[] item : taxPayerData) {			
			Taxpayer entity = new Taxpayer();
			entity.setId(item[0]);
			entity.setName(item[1]);
			entity.setQualifier1(item[2]);
			entity.setQualifier2(item[3]);
			entity.setQualifier3(item[4]);
			entity.setQualifier4(item[5]);
			entity.setQualifier5(item[5]);
			
			TaxpayerDto taxpayer = new TaxpayerDto(entity);
			MockHttpServletResponse response = mockMvc.perform(
	                post("/api/taxpayer")
	                	.with(csrf())
	                    .accept(MediaType.APPLICATION_JSON)
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content(
	                        json.write(taxpayer).getJson()
	                    )
	            )
	            .andReturn()
	            .getResponse();
			
			assertEquals(response.getStatus(), HttpStatus.OK.value());
			String id = JsonPath.read(response.getContentAsString(), "$.id");
			
	        assertNotNull(id);
	        
	        Optional<Taxpayer> existing = taxpayerRepository.findById(id);
	        assertTrue(existing.isPresent());	        
		}
	}    
	
	@WithUserDetails(value="admin@admin",userDetailsServiceBeanName="CustomUserDetailsService")
	@Test
	void test02CreateUser() throws Exception {

		String[][] userData = { 
				{ "Diana Mullis", "diana@mullis.com", "29847163744173218067", "123456" },		
				{ "Donald Elliott", "donald@elliott.com", "80465967848774987165", "123456" },
				{ "Loretta Camp", "loretta@camp.com", "85509634237883761680", "123456" }
		};
		
		for(String[] item : userData) {
			
			User user = new User();
			user.setActive(true);
			user.setName(item[0]);
			user.setLogin(item[1]);
			user.setProfile(UserProfile.DECLARANT);
			user.setTaxpayerId(item[2]);
			user.setPassword(item[3]);
			UserDto userDto = new UserDto(user);
			
			MockHttpServletResponse response = mockMvc.perform(
	                post("/api/user")
	                	.with(csrf())
	                    .accept(MediaType.APPLICATION_JSON)
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content(
	                        json.write(userDto).getJson()
	                    )
	            )
	            .andReturn()
	            .getResponse();
			
			assertEquals(HttpStatus.OK.value(),response.getStatus());
			String id = JsonPath.read(response.getContentAsString(), "$.id");
	        assertNotNull(id);
		}
	}
	
	@WithUserDetails(value="admin@admin",userDetailsServiceBeanName="CustomUserDetailsService")
	@Test
	void test03CreateTemplates() throws Exception {
		
		MockHttpServletResponse response = mockMvc.perform(
                post("/api/op")
                	.with(csrf())
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("command=samples -t")
            )
            .andReturn()
            .getResponse();
		
		assertEquals(HttpStatus.OK.value(),response.getStatus());		
		
		String[] templates = { "Chart Of Accounts", "Income Statement", "Journal", "Opening Balance", "Shareholding" };
		
		for ( String templateName : templates ) {

			Optional<DocumentTemplate> template = documentTemplateRepository.findByNameAndVersion(templateName, "1.0");
			assertTrue(template.isPresent());
			
		}
		
	}

	@WithUserDetails(value="admin@admin",userDetailsServiceBeanName="CustomUserDetailsService")
	@Test
	void test04HandleFileUpload() throws Exception {
		
		String[] taxpayers = { "29847163744173200000", "80465967848774987165", "85509634237883761680" };
		String[] years = { "2020", "2021" };
		String[] templates = { "Chart Of Accounts", "Income Statement", "Journal", "Opening Balance", "Shareholding" };
		String baseRes = "api_test";
		String res = "file_to_upload";
		
		for ( String taxpayer : taxpayers ) {
			
			for ( String year : years ) {
				
				for ( String templateName : templates ) {					
					 
					String extension = ".XLSX";		
					String fileName = year + "_" + templateName + extension;
					String resName = baseRes + File.pathSeparator + taxpayer + File.pathSeparator + res + File.pathSeparator + fileName;
					
					InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resName);
					
					MockMultipartFile multipartFile = new MockMultipartFile("fileinput", stream);
					this.mockMvc.perform(
							multipart("/api/doc")
							.file(multipartFile)
							.with(csrf())
							.param("templateName", templateName)
							.param("templateVersion", "1.0")
							.param("inputName", "XLS"))
							.andExpect(status().isOk());
					
					Page<DocumentUploaded> matchUploads = documentsUploadedRepository.findByFilename(fileName, PageRequest.ofSize(1));
					assertFalse(matchUploads.isEmpty());
					DocumentUploaded matchUpload = matchUploads.getContent().get(0);
					String subdirAndFilename = matchUpload.getSubDir()+File.separator+matchUpload.getFileId();
					
					Path path = storageService.find(subdirAndFilename);
					assertNotNull(path);
					assertTrue(storageService.delete(subdirAndFilename));					
				}
			}
			
		}		

	}
	
	@WithUserDetails(value="admin@admin",userDetailsServiceBeanName="CustomUserDetailsService")
	@Test
	void test05CheckFileUploaded() throws Exception {
		
		String[] years = { "2020", "2021" };
		String[] templates = { "Chart Of Accounts", "Income Statement", "Journal", "Opening Balance", "Shareholding" };
		
		for ( String year : years ) {
			
			for ( String templateName : templates ) {					
				 
				String extension = ".XLSX";		
				String fileName = year + "_" + templateName + extension;
				
				Page<DocumentUploaded> matchUploads = documentsUploadedRepository.findByFilename(fileName, PageRequest.ofSize(3));
				assertFalse(matchUploads.isEmpty());
				
				for ( DocumentUploaded matchUpload : matchUploads ) {					
					String subdirAndFilename = matchUpload.getSubDir()+File.separator+matchUpload.getFileId();
					Path path = storageService.find(subdirAndFilename);
					assertNotNull(path);					
				}

			}
			
		}		

	}
	
}