/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
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
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
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

	private void assertEqualsSaved(String id, DomainTableDto domain) {
		Optional<DomainTable> existent = domainTableRepository.findById(id);
		assertTrue(existent.isPresent());
		DomainTable saved = existent.get();
		assertEquals(domain.getName(), saved.getName());
        assertEquals(domain.getGroup(), saved.getGroup());
        assertEquals(domain.getVersion(), saved.getVersion());
        assertEquals(domain.getNumEntries(), saved.getNumEntries());
        assertEquals(domain.getActive(), saved.getActive());
        domain.getEntries().stream()
            .forEach(t -> assertEquals(t.getDescription(), 
            		saved.getEntry(t.getKey(), t.getLanguage()).getDescription()));
	}
	
	private DomainTableDto createSampleDomainTable(String name) {
		return new DomainTableDto(null, name, "test", "0.1", false,
				new DomainEntry("D", DomainLanguage.ENGLISH, "Debit"),
				new DomainEntry("C", DomainLanguage.ENGLISH, "Credit"),
				new DomainEntry("D", DomainLanguage.SPANISH, "Débito"),
				new DomainEntry("C", DomainLanguage.SPANISH, "Crédito"));
	}
	
	private DomainTableDto save(DomainTableDto domain) {
		DomainTable entity = new DomainTable();
		domain.updateEntity(entity);
		return new DomainTableDto(domainTableRepository.saveWithTimestamp(entity));
	}
	
	/*
	@WithUserDetails(value="admin@admin",userDetailsServiceBeanName="CustomUserDetailsService")
	@Test
	public void searchDomainTable() throws Exception {
		String expected = "Test Domain Search";
		save(createSampleDomainTable(expected));
		String term="Test";
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

	@WithUserDetails(value="admin@admin",userDetailsServiceBeanName="CustomUserDetailsService")
	@Test
	public void activateDomainTable() throws Exception {
		DomainTableDto domain = createSampleDomainTable("Test Domain Activate");
		domain.setActive(false);
		domain = save(domain);
		String id = domain.getId();
		MockHttpServletResponse response = mvc.perform(
                get("/api/domaintable/" + id + "/activate")
                	.with(csrf())
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andReturn()
            .getResponse();
		assertEquals(response.getStatus(), HttpStatus.OK.value());
		domain.setActive(true);
		assertEqualsSaved(id, domain);
	}

	@WithUserDetails(value="admin@admin",userDetailsServiceBeanName="CustomUserDetailsService")
	@Test
	public void deactivateDomainTable() throws Exception {
		DomainTableDto domain = createSampleDomainTable("Test Domain deactivate");
		domain.setActive(true);
		domain = save(domain);
		String id = domain.getId();
		MockHttpServletResponse response = mvc.perform(
                get("/api/domaintable/" + id + "/deactivate")
                	.with(csrf())
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andReturn()
            .getResponse();
		assertEquals(response.getStatus(), HttpStatus.OK.value());
		domain.setActive(false);
		assertEqualsSaved(id, domain);
	}

	@WithUserDetails(value="admin@admin",userDetailsServiceBeanName="CustomUserDetailsService")
	@Test
	public void testCreateDomainTable() throws Exception {
		DomainTableDto domain = createSampleDomainTable("Test Domain Create");
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
        
        assertEqualsSaved(id, domain);
	}

	@WithUserDetails(value="admin@admin",userDetailsServiceBeanName="CustomUserDetailsService")
	@Test
	public void testCreateExistingDomainTable() throws Exception {
		DomainTableDto domain = save(createSampleDomainTable("Test Domain Create Existing"));
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
	
	@WithUserDetails(value="admin@admin",userDetailsServiceBeanName="CustomUserDetailsService")
	@Test
	public void testEditDomainTable() throws Exception, IOException {
		DomainTableDto domain = save(createSampleDomainTable("Test Domain Edit"));
        domain.setVersion("0.2");
        domain.setActive(true);
        domain.setEntries(domain.getEntriesOfLanguage(DomainLanguage.ENGLISH));
        String id = domain.getId();

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
		assertEqualsSaved(id, domain);
	}

}
