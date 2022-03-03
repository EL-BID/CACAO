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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

import org.idb.cacao.api.DomainLanguage;
import org.idb.cacao.api.templates.DomainEntry;
import org.idb.cacao.api.templates.DomainTable;
import org.idb.cacao.mock_es.ElasticsearchMockClient;
import org.idb.cacao.web.dto.DomainTableDto;
import org.idb.cacao.web.repositories.DomainTableRepository;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.io.IOException;
import java.util.Optional;

@AutoConfigureJsonTesters
@RunWith(JUnitPlatform.class)
@AutoConfigureMockMvc
@SpringBootTest( webEnvironment = WebEnvironment.RANDOM_PORT)
class DomainTableAPIControllerTests {

	private static ElasticsearchMockClient mockElastic;

	@Autowired
	private MockMvc mvc;
	
	@Autowired
	private DomainTableRepository domainTableRepository;
	
	@Autowired
	private JacksonTester<DomainTableDto> json;
	
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

	private void compareDomainTable(DomainTableDto domain, DomainTable saved) {
		assertEquals(domain.getName(), saved.getName());
        assertEquals(domain.getGroup(), saved.getGroup());
        assertEquals(domain.getVersion(), saved.getVersion());
        assertEquals(domain.getNumEntries(), saved.getNumEntries());
        assertEquals(domain.getActive(), saved.getActive());
        domain.getEntries().stream()
            .forEach(t -> assertEquals(t.getDescription(), 
            		saved.getEntry(t.getKey(), t.getLanguage()).getDescription()));
	}
	
	@WithUserDetails(value="admin@admin",userDetailsServiceBeanName="CustomUserDetailsService")
	@Test
	void testHandleCreateDomainTable() throws Exception {
		DomainTableDto domain = new DomainTableDto(null, "Domain test", "test", "0.1", false,
				new DomainEntry("D", DomainLanguage.ENGLISH, "Debit"),
				new DomainEntry("C", DomainLanguage.ENGLISH, "Credit"),
				new DomainEntry("D", DomainLanguage.SPANISH, "Débito"),
				new DomainEntry("C", DomainLanguage.SPANISH, "Crédito"));
		
		DomainTable saved = createDomainTable(domain);
        
        domain.setVersion("0.2");
        domain.setActive(true);
        domain.setEntries(domain.getEntriesOfLanguage(DomainLanguage.ENGLISH));
        String id = saved.getId();
        editDomainTable(id, domain);
		
		deactivateDomainTable(id);
		
		activateDomainTable(id);
		
		//searchDomainTable("test", "Domain Test");
		
		createExistingDomainTable(domain);
	}

	/*
	private void searchDomainTable(String term, String expected) throws Exception {
		MockHttpServletResponse response = mvc.perform(
                get("/api/domaintable-search")
                	.with(csrf())
                	.param("term", term)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andReturn()
            .getResponse();
		assertEquals(response.getStatus(), HttpStatus.OK.value());
		List<String> opcoes = JsonPath.read(response.getContentAsString(), "$[*]");
		opcoes.contains(expected);
	}
	*/

	private void activateDomainTable(String id) throws Exception {
		DomainTable saved;
		MockHttpServletResponse response = mvc.perform(
                get("/api/domaintable/" + id + "/activate")
                	.with(csrf())
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andReturn()
            .getResponse();
		assertEquals(response.getStatus(), HttpStatus.OK.value());
		Optional<DomainTable> existing = domainTableRepository.findById(id);
		assertTrue(existing.isPresent());
		saved = existing.get();
		assertEquals(true, saved.getActive());
	}

	private void deactivateDomainTable(String id) throws Exception {
		DomainTable saved;
		MockHttpServletResponse response = mvc.perform(
                get("/api/domaintable/" + id + "/deactivate")
                	.with(csrf())
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andReturn()
            .getResponse();
		assertEquals(response.getStatus(), HttpStatus.OK.value());
		Optional<DomainTable> existing = domainTableRepository.findById(id);
		assertTrue(existing.isPresent());
		saved = existing.get();
		assertEquals(false, saved.getActive());
	}

	DomainTable createDomainTable(DomainTableDto domain) throws Exception {
		MockHttpServletResponse response = mvc.perform(
                post("/api/domaintable")
                	.with(csrf())
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.write(domain).getJson()
                    )
            )
            .andReturn()
            .getResponse();
		
		assertEquals(response.getStatus(), HttpStatus.OK.value());
		String id = JsonPath.read(response.getContentAsString(), "$.id");
		
        assertNotNull(id);
        
        Optional<DomainTable> existing = domainTableRepository.findById(id);
        assertTrue(existing.isPresent());
        
        DomainTable saved = existing.get();
        
        compareDomainTable(domain, saved);
        return saved;
	}

	void createExistingDomainTable(DomainTableDto domain) throws Exception {
		MockHttpServletResponse response = mvc.perform(
                post("/api/domaintable")
                	.with(csrf())
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.write(domain).getJson()
                    )
            )
            .andReturn()
            .getResponse();
		
		assertEquals(response.getStatus(), HttpStatus.BAD_REQUEST.value());
	}
	
	void editDomainTable(String id, DomainTableDto domain) throws Exception, IOException {
		DomainTable saved;
		MockHttpServletResponse response = mvc.perform(
                put("/api/domaintable/" + id)
                	.with(csrf())
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.write(domain).getJson()
                    )
            )
            .andReturn()
            .getResponse();
		
		assertEquals(response.getStatus(), HttpStatus.OK.value());
		Optional<DomainTable> existing = domainTableRepository.findById(id);
		assertTrue(existing.isPresent());
		saved = existing.get();
		compareDomainTable(domain, saved);
	}

}
