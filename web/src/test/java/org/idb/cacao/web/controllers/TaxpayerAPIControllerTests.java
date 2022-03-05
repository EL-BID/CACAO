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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;

@AutoConfigureJsonTesters
@RunWith(JUnitPlatform.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@SpringBootTest( webEnvironment = WebEnvironment.RANDOM_PORT)
class TaxpayerAPIControllerTests {

	private static ElasticsearchMockClient mockElastic;

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
		Random seedGenerator = new Random(TestDataGenerator.generateSeed("CREATE"));
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
                get("/api/taxpayer/autocomplete")
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
		Random seedGenerator = new Random(TestDataGenerator.generateSeed("ACTIVATE"));
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
		Random seedGenerator = new Random(TestDataGenerator.generateSeed("DEACTIVATE"));
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
		Random seedGenerator = new Random(TestDataGenerator.generateSeed("CREATE2"));
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
		Random seedGenerator = new Random(TestDataGenerator.generateSeed("TEST"));
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

}
