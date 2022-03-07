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
package org.idb.cacao.web.controllers;

import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.TaxPayerId;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

import org.idb.cacao.api.Periodicity;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.FieldMapping;
import org.idb.cacao.api.templates.FieldType;
import org.idb.cacao.mock_es.ElasticsearchMockClient;
import org.idb.cacao.web.dto.DocumentTemplateDto;
import org.idb.cacao.web.repositories.DocumentTemplateRepository;
import org.idb.cacao.web.utils.CreateDocumentTemplatesSamples;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

@AutoConfigureJsonTesters
@RunWith(JUnitPlatform.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@SpringBootTest( webEnvironment = WebEnvironment.RANDOM_PORT)
class DocumentTemplateAPIControllerTests {

	private static ElasticsearchMockClient mockElastic;

	@Autowired
	private MockMvc mvc;
	
	@Autowired
	private DocumentTemplateRepository documentTemplateRepository;
	
	@Autowired
	private JacksonTester<DocumentTemplateDto> json;
	
	@Autowired MessageSource messageSource;
	
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

	private void assertEqualsSaved(String id, DocumentTemplateDto template) {
		Optional<DocumentTemplate> existent = documentTemplateRepository.findById(id);
		assertTrue(existent.isPresent());
		DocumentTemplate saved = existent.get();
		assertEquals(template.getName(), saved.getName());
        assertEquals(template.getGroup(), saved.getGroup());
        assertEquals(template.getVersion(), saved.getVersion());
        assertEquals(template.getArchetype(), saved.getArchetype());
        assertEquals(template.getPeriodicity(), saved.getPeriodicity());
        assertEquals(template.getRequired(), saved.getRequired());
        assertEquals(template.getActive(), saved.getActive());
        if(template.getFields()!=null)
        	template.getFields().stream()
            	.forEach(t -> assertEquals(t,saved.getField(t.getFieldName())));
        if(template.getInputs()!=null)
        	template.getInputs().stream()
        		.forEach(t -> assertEquals(t,saved.getInput(t.getInputName())));
	}
	
	private DocumentTemplateDto createSampleTemplate() {
		return new DocumentTemplateDto(null, null, "TEST", "TEST GROUP", "1.0", Periodicity.YEARLY, 
			true, true, 
			new DocumentField()
			.withFieldName(TaxPayerId.name())
			.withFieldType(FieldType.CHARACTER)
			.withFieldMapping(FieldMapping.TAXPAYER_ID)
			.withDescription("Taxpayer Identification Number")
			.withMaxLength(128)
			.withRequired(true)
			.withFileUniqueness(true)
			.withPersonalData(true));
	}
	
	private DocumentTemplateDto save(DocumentTemplateDto template) {
		DocumentTemplate entity = new DocumentTemplate();
		template.updateEntity(entity);
		return new DocumentTemplateDto(documentTemplateRepository.saveWithTimestamp(entity));
	}
	
	@WithUserDetails(value="admin@admin",userDetailsServiceBeanName="CustomUserDetailsService")
	@Test
	void testHandleCreateDocumentTemplate() throws Exception {
		for(DocumentTemplate entity: CreateDocumentTemplatesSamples.getSampleTemplates(messageSource, Locale.getDefault())) {
			DocumentTemplateDto template = new DocumentTemplateDto(entity);
			
			MockHttpServletResponse response = mvc.perform(
	                post("/api/template")
	                	.with(csrf())
	                    .accept(MediaType.APPLICATION_JSON)
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content(
	                        json.write(template).getJson()
	                    )
	            )
	            .andReturn()
	            .getResponse();
			
			assertEquals(HttpStatus.OK.value(), response.getStatus());
			String id = JsonPath.read(response.getContentAsString(), "$.id");

	        assertNotNull(id);
	        
	        assertEqualsSaved(id, template);
        }
    }

	@WithUserDetails(value="admin@admin",userDetailsServiceBeanName="CustomUserDetailsService")
	@Test
	public void activateTemplate() throws Exception {
		DocumentTemplateDto template = createSampleTemplate();
		template.setActive(false);
		template = save(template);
		String id = template.getId();
		MockHttpServletResponse response = mvc.perform(
                get("/api/template/" + id + "/activate")
                	.with(csrf())
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andReturn()
            .getResponse();
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		template.setActive(true);
		assertEqualsSaved(template.getId(), template);
	}

	@WithUserDetails(value="admin@admin",userDetailsServiceBeanName="CustomUserDetailsService")
	@Test
	public void deactivateDomainTable() throws Exception {
		DocumentTemplateDto template = createSampleTemplate();
		template.setActive(true);
		template = save(template);
		String id = template.getId();
		MockHttpServletResponse response = mvc.perform(
                get("/api/template/" + id + "/deactivate")
                	.with(csrf())
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andReturn()
            .getResponse();
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		template.setActive(false);
		assertEqualsSaved(template.getId(), template);
	}

	@WithUserDetails(value="admin@admin",userDetailsServiceBeanName="CustomUserDetailsService")
	@Test
	public void testCreateExistingTemplate() throws Exception {
		DocumentTemplateDto template = save(createSampleTemplate());
		MockHttpServletResponse response = mvc.perform(
                post("/api/template")
                	.with(csrf())
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.write(template).getJson()
                    )
            )
            .andReturn()
            .getResponse();
		
		assertEquals(response.getStatus(), HttpStatus.BAD_REQUEST.value());
	}
	
	@WithUserDetails(value="admin@admin",userDetailsServiceBeanName="CustomUserDetailsService")
	@Test
	public void testEditTemplate() throws Exception, IOException {
		DocumentTemplateDto template = save(createSampleTemplate());
        template.setVersion("0.2");
        template.setActive(true);
        String id = template.getId();

		MockHttpServletResponse response = mvc.perform(
                put("/api/template/" + id)
                	.with(csrf())
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.write(template).getJson()
                    )
            )
            .andReturn()
            .getResponse();
		
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		assertEqualsSaved(id, template);
	}

}
