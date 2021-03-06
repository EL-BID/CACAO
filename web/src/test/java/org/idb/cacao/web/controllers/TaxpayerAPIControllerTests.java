/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;

import org.idb.cacao.api.Taxpayer;
import org.idb.cacao.api.utils.RandomDataGenerator;
import org.idb.cacao.mock_es.ElasticsearchMockClient;
import org.idb.cacao.web.dto.TaxpayerDto;
import org.idb.cacao.web.repositories.TaxpayerRepository;
import org.idb.cacao.web.utils.TestDataGenerator;
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

@AutoConfigureJsonTesters
@RunWith(JUnitPlatform.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@SpringBootTest( webEnvironment = WebEnvironment.RANDOM_PORT)
class TaxpayerAPIControllerTests {

	private static ElasticsearchMockClient mockElastic;
	
	private static Random seedGenerator = new Random(TestDataGenerator.generateSeed("TEST TAXPAYER"));

	@Autowired
	private MockMvc mvc;
	
	@Autowired
	private TaxpayerRepository taxpayerRepository;
	
	@Autowired
	private JacksonTester<TaxpayerDto> json;
	
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

	private List<Taxpayer> insertTaxpayers(Random seedGenerator, int numDigits, int amountOfTaxpayers, Consumer<Taxpayer> function) {
		List<Taxpayer> taxpayers = new ArrayList<>(amountOfTaxpayers);
		for (int i=0; i<amountOfTaxpayers; i++) {
			long seed = seedGenerator.nextLong();
			Taxpayer taxpayer = TestDataGenerator.generateTaxpayer(seed, numDigits);
			if (function!=null) {
				function.accept(taxpayer);
			}
			taxpayerRepository.saveWithTimestamp(taxpayer);
			taxpayers.add(taxpayerRepository.saveWithTimestamp(taxpayer));
		}
		return taxpayers;
	}
	
	private void compareTaxpayer(TaxpayerDto domain, Taxpayer saved) {
		assertEquals(domain.getName(), saved.getName());
        assertEquals(domain.getTaxPayerId(), saved.getTaxPayerId());
        assertEquals(domain.getAddress(), saved.getAddress());
        assertEquals(domain.getZipCode(), saved.getZipCode());
        assertEquals(domain.getQualifier1(), saved.getQualifier1());
        assertEquals(domain.getQualifier2(), saved.getQualifier2());
        assertEquals(domain.getQualifier3(), saved.getQualifier3());
        assertEquals(domain.getQualifier4(), saved.getQualifier4());
        assertEquals(domain.getQualifier5(), saved.getQualifier5());
        assertEquals(domain.isActive(), saved.isActive());
	}
	
	@WithUserDetails(value="admin@admin",userDetailsServiceBeanName="CustomUserDetailsService")
	@Test
	void testHandleCreateTaxpayer() throws Exception {
		for (int i=0; i<100; i++) {
			long seed = seedGenerator.nextLong();
			Taxpayer entity = TestDataGenerator.generateTaxpayer(seed, 11);
			TaxpayerDto taxpayer = new TaxpayerDto(entity);
			MockHttpServletResponse response = mvc.perform(
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
	        
	        Taxpayer saved = existing.get();
	        
	        compareTaxpayer(taxpayer, saved);
		}
	}
/*		
	@WithUserDetails(value="admin@admin",userDetailsServiceBeanName="CustomUserDetailsService")
	@Test
	public void testAutocompleteTaxpayer(String term, String expected) throws Exception {
		MockHttpServletResponse response = mvc.perform(
                post("/api/taxpayer/autocomplete")
                	.with(csrf())
                	.param("term", term)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andReturn()
            .getResponse();
		assertEquals(response.getStatus(), HttpStatus.OK.value());
		List<String> opcoes = JsonPath.read(response.getContentAsString(), "$.results[*].id");
		opcoes.contains(expected);
	}
*/
	@WithUserDetails(value="admin@admin",userDetailsServiceBeanName="CustomUserDetailsService")
	@Test
	public void testActivateTaxpayer() throws Exception {
		List<Taxpayer> taxpayers = insertTaxpayers(seedGenerator, 11, 2, t -> t.setActive(false));
		for(Taxpayer taxpayer: taxpayers) {
			MockHttpServletResponse response = mvc.perform(
	                get("/api/taxpayer/" + taxpayer.getId() + "/activate")
	                	.with(csrf())
	                    .accept(MediaType.APPLICATION_JSON)
	            )
	            .andReturn()
	            .getResponse();
			assertEquals(response.getStatus(), HttpStatus.OK.value());
			Optional<Taxpayer> existing = taxpayerRepository.findById(taxpayer.getId());
			assertTrue(existing.isPresent());
			Taxpayer saved = existing.get();
			assertEquals(true, saved.isActive());
		}
	}

	@WithUserDetails(value="admin@admin",userDetailsServiceBeanName="CustomUserDetailsService")
	@Test
	public void testDeactivateTaxpayer() throws Exception {
		List<Taxpayer> taxpayers = insertTaxpayers(seedGenerator, 11, 2, t -> t.setActive(true));
		for(Taxpayer taxpayer: taxpayers) {
			MockHttpServletResponse response = mvc.perform(
	                get("/api/taxpayer/" + taxpayer.getId() + "/deactivate")
	                	.with(csrf())
	                    .accept(MediaType.APPLICATION_JSON)
	            )
	            .andReturn()
	            .getResponse();
			assertEquals(response.getStatus(), HttpStatus.OK.value());
			Optional<Taxpayer> existing = taxpayerRepository.findById(taxpayer.getId());
			assertTrue(existing.isPresent());
			Taxpayer saved = existing.get();
			assertEquals(false, saved.isActive());
		}
	}

	@WithUserDetails(value="admin@admin",userDetailsServiceBeanName="CustomUserDetailsService")
	@Test
	public void testCreateExistingTaxpayer() throws Exception {
		List<Taxpayer> taxpayers = insertTaxpayers(seedGenerator, 11, 2, null);
		for (int i=0; i<taxpayers.size(); i++) {
			Taxpayer entity = taxpayers.get(i);
			TaxpayerDto taxpayer = new TaxpayerDto(entity);
			
			MockHttpServletResponse response = mvc.perform(
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
			assertEquals(response.getStatus(), HttpStatus.BAD_REQUEST.value());
			assertTrue(response.getContentAsString().contains(taxpayer.getTaxPayerId()));
		}
		
	}
	
	@WithUserDetails(value="admin@admin",userDetailsServiceBeanName="CustomUserDetailsService")
	@Test
	public void testEditTaxpayer() throws Exception, IOException {
		List<Taxpayer> taxpayers = insertTaxpayers(seedGenerator, 11, 10, null);
		
		for (int i=0; i<taxpayers.size(); i++) {
			Taxpayer entity = taxpayers.get(i);
			RandomDataGenerator randomDataGenerator = new RandomDataGenerator(seedGenerator.nextLong());
			TestDataGenerator.generateTaxpayer(entity, randomDataGenerator);
	        
	        String id = entity.getId();
	        TaxpayerDto taxpayer = new TaxpayerDto(entity);
	
			MockHttpServletResponse response = mvc.perform(
	                put("/api/taxpayer/" + id)
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
			Optional<Taxpayer> existing = taxpayerRepository.findById(id);
			assertTrue(existing.isPresent());
			entity = existing.get();
			compareTaxpayer(taxpayer, entity);
		}
	}

	@SuppressWarnings("unused")
	private void sampleUpdate(TaxpayerDto taxpayer, long seed) {
		Taxpayer other = TestDataGenerator.generateTaxpayer(seed, 11);
		taxpayer.setName(other.getName());
		taxpayer.setAddress(other.getAddress());
		taxpayer.setZipCode(other.getZipCode());
		taxpayer.setQualifier1(other.getQualifier1());
		// do not update the following fields
		taxpayer.setQualifier2(null);
		taxpayer.setQualifier3(null);
		taxpayer.setQualifier4(null);
		taxpayer.setQualifier5(null);
	}
	/*
	@WithUserDetails(value="admin@admin",userDetailsServiceBeanName="CustomUserDetailsService")
	@Test
	public void testAddTaxpayers() throws Exception {
		
		Map<String, TaxpayerDto> existentTaxpayers = insertTaxpayers(seedGenerator, 11, 10, null)
				.stream()
				.map(t -> new TaxpayerDto(t))
				.collect( Collectors.toMap(TaxpayerDto::getTaxPayerId, Function.identity()));
		Map<String, TaxpayerDto> toUpdate = existentTaxpayers.values()
			.stream()
			.peek( t -> sampleUpdate(t, seedGenerator.nextLong()) )
			.collect( Collectors.toMap(TaxpayerDto::getTaxPayerId, Function.identity()));;
		Map<String, TaxpayerDto> toInsert = IntStream.range(0, 10)
			.mapToObj(i -> new TaxpayerDto(TestDataGenerator.generateTaxpayer(seedGenerator.nextLong(), 11)))
			.collect(Collectors.toMap(TaxpayerDto::getTaxPayerId, Function.identity()));
		
		List<TaxpayerDto> taxpayers = Stream.concat(toUpdate.values().stream(), toInsert.values().stream())
			.collect(Collectors.toList());
		
		MockHttpServletResponse response = mvc.perform(
                post("/api/taxpayers/")
                	.with(csrf())
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        jsonList.write(taxpayers).getJson()
                    )
            )
            .andReturn()
            .getResponse();
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		toInsert.values().stream().forEach(t -> {
	        Optional<Taxpayer> existing = taxpayerRepository.findByTaxPayerId(t.getTaxPayerId());
	        assertTrue(existing.isPresent());
	        Taxpayer saved = existing.get();
	        compareTaxpayer(t, saved);
		});
		
		toUpdate.values().stream().forEach(t -> {
			Optional<Taxpayer> existing = taxpayerRepository.findByTaxPayerId(t.getTaxPayerId());
	        assertTrue(existing.isPresent());
	        Taxpayer saved = existing.get();
	        TaxpayerDto previous = existentTaxpayers.get(t.getTaxPayerId());
			assertEquals(t.getName(), saved.getName());
	        assertEquals(t.getTaxPayerId(), saved.getTaxPayerId());
	        assertEquals(t.getAddress(), saved.getAddress());
	        assertEquals(t.getZipCode(), saved.getZipCode());
	        assertEquals(t.getQualifier1(), saved.getQualifier1());
	        assertEquals(t.isActive(), saved.isActive());
	        // The following fields should not be updated
	        assertEquals(previous.getQualifier2(), saved.getQualifier2());
	        assertEquals(previous.getQualifier3(), saved.getQualifier3());
	        assertEquals(previous.getQualifier4(), saved.getQualifier4());
	        assertEquals(previous.getQualifier5(), saved.getQualifier5());
		});
	}
	*/
}
