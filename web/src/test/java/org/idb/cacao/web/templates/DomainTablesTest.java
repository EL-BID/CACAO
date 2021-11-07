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
package org.idb.cacao.web.templates;

import java.util.List;

import org.idb.cacao.api.DomainLanguage;
import org.idb.cacao.api.templates.DomainEntry;
import org.idb.cacao.api.templates.DomainTable;
import org.idb.cacao.mock_es.ElasticsearchMockClient;
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

}
