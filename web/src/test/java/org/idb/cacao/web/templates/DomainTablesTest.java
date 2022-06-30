/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.templates;

import java.util.Arrays;
import java.util.List;

import org.idb.cacao.api.DomainLanguage;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DomainEntry;
import org.idb.cacao.api.templates.DomainTable;
import org.idb.cacao.api.templates.FieldType;
import org.idb.cacao.api.templates.TemplateArchetype;
import org.idb.cacao.mock_es.ElasticsearchMockClient;
import org.idb.cacao.web.controllers.services.DomainTableService;
import org.idb.cacao.web.repositories.DomainTableRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests regarding basic uses of Domain Tables
 * 
 * @author Gustavo Figueiredo
 *
 */
@RunWith(JUnitPlatform.class)
@SpringBootTest
@DirtiesContext
public class DomainTablesTest {

	private static ElasticsearchMockClient mockElastic;
	
	@Autowired
	private DomainTableRepository domainRepository;
	
	@Autowired
	private DomainTableService domainService;

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
	
	/**
	 * Tests basic functionality of domain tables
	 */
	@Test
	void testDomainTableOperations() {
		
		domainRepository.saveWithTimestamp(new DomainTable("TEST DEBIT/CREDIT","1.0")
				.withEntries(new DomainEntry("D", DomainLanguage.ENGLISH, "Debit"),
						new DomainEntry("C", DomainLanguage.ENGLISH, "Credit")));
		
		List<DomainTable> found = domainRepository.findByName("TEST DEBIT/CREDIT");
		assertNotNull(found);
		assertFalse(found.isEmpty());
		assertEquals(1, found.size());
		DomainTable domain = found.get(0);
		
		assertNotNull(domain.getEntry("D"));
		assertNotNull(domain.getEntry("C"));

		assertNotNull(domain.getEntry("d"));
		assertNotNull(domain.getEntry("c"));
		
		assertEquals("Debit", domain.getEntry("D").getDescription());
		assertEquals("Credit", domain.getEntry("C").getDescription());
	
		assertEquals("Debit", domain.getEntry("D", DomainLanguage.ENGLISH).getDescription());
		assertEquals("Credit", domain.getEntry("C", DomainLanguage.ENGLISH).getDescription());
		
		assertNull(domain.getEntry("D", DomainLanguage.SPANISH));
		assertNull(domain.getEntry("C", DomainLanguage.SPANISH));
	}

	/**
	 * Tests the provision of language-specific domain tables
	 */
	@Test
	void testDomainTableForProvidedLanguages() {

		// This is a 'built-in' domain table
		// Note we don't specify any specific language
		// Instead we inform names to be located inside 'messages.properties' files
		DomainTable builtInTable = new DomainTable("TEST DEBIT/CREDIT","1.0")
				.withEntries(new DomainEntry("D", "account.debit"),
						new DomainEntry("C", "account.credit"));

		// This will try to resolve all the domain table entries into the corresponding messages
		// retrieved from provided 'messages.properties' files
		DomainTable table = domainService.resolveDomainTableForLanguages(builtInTable);
		
		assertEquals(2, table.getNumEntries(DomainLanguage.ENGLISH));
		assertEquals(2, table.getNumEntries(DomainLanguage.SPANISH));
		
	}
	
	/**
	 * Tests the assertion with auto-creation of domain tables according to a given TemplateArchetype
	 * IMPORTANT: this test relies on the presence of a 'messages.properties' files with definitions
	 * for 'account.debit' and 'account.credit'
	 */
	@Test
	void testDomainTableAssertionForArchetypes() {
		
		// Let's test with a specific Archetype with reference to a specific built-in domain table
		
		DomainTable builtInTable = new DomainTable("TEST-2 DEBIT/CREDIT","1.0")
				.withEntries(new DomainEntry("D", "account.debit"),
						new DomainEntry("C", "account.credit"));

		TemplateArchetype arch = new TemplateArchetype() {

			@Override
			public String getName() {
				return "Internal Archetype for Test";
			}

			@Override
			public List<DocumentField> getRequiredFields() {
				return Arrays.asList(
					new DocumentField()
					.withFieldName("DebitCredit")
					.withFieldType(FieldType.DOMAIN)
					.withDomainTableName(builtInTable.getName())
					.withDomainTableName(builtInTable.getVersion())
					.withDescription("This is an indication of whether this entry is a debit or a credit to the account")
					.withMaxLength(32)
					.withRequired(true)
					);
			}

			@Override
			public List<DomainTable> getBuiltInDomainTables() {
				return Arrays.asList(builtInTable);
			}
			
		};
		
		// This should enforce the existence of 'TEST-2 DEBIT/CREDIT' domain table due to the TemplateArchetype's specification
		domainService.assertDomainTablesForAchetype(arch);
		
		List<DomainTable> found = domainRepository.findByName("TEST-2 DEBIT/CREDIT");
		assertNotNull(found);
		assertFalse(found.isEmpty());
		assertEquals(1, found.size());
		DomainTable domain = found.get(0);
		assertTrue(domain.getNumEntries()>0);
	}
}
