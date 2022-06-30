/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.validator.parsers;

import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.AccountCategory;
import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.AccountCode;
import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.AccountDescription;
import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.AccountName;
import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.AccountSubcategory;
import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.TaxPayerId;
import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.TaxYear;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.idb.cacao.account.archetypes.ChartOfAccountsArchetype;
import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.api.templates.DocumentInput;
import org.idb.cacao.api.templates.DocumentInputFieldMapping;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.TemplateArchetype;
import org.idb.cacao.mock_es.ElasticsearchMockClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Tests sample files in WOED format with the WORDParser implemented in VALIDATOR
 * 
 * @author Rivelino Patrício
 *
 * @since 15/12/2021
 */
@RunWith(JUnitPlatform.class)
public class WordParserTests {
	
	private static ElasticsearchMockClient mockElastic;
	
	private static String[] resources = { "/samples/20211411 - Pauls Guitar Shop - Chart of Accounts.docx",
			"/samples/20211411 - Pauls Guitar Shop - Chart of Accounts - 2 Tables.docx"};

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
	 * Test the sample file 'Chart of Accounts.pdf' with the input mapping
	 * given as column names.
	 */
	@Test
	void testChartOfAccounts() throws Exception {		
		
		TemplateArchetype archetype = new ChartOfAccountsArchetype();
		DocumentTemplate template = new DocumentTemplate();
		template.setArchetype(archetype.getName());
		template.setFields(archetype.getRequiredFields());
		
		DocumentInput inputSpec = new DocumentInput();
		inputSpec.setFormat(DocumentFormat.DOC);
		inputSpec.setInputName("ChartOfAccounts WORD");
		template.addInput(inputSpec);
		
		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(TaxPayerId.name())
				.withColumnNameExpression("TaxPayerId"));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(TaxYear.name())
				.withColumnNameExpression("TaxYear"));
		
		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(AccountCode.name())
				.withColumnNameExpression("AccountCode"));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(AccountCategory.name())
				.withColumnNameExpression("AccountCategory"));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(AccountSubcategory.name())
				.withColumnNameExpression("AccountSubcategory"));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(AccountName.name())
				.withColumnNameExpression("AccountName"));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(AccountDescription.name())
				.withColumnNameExpression("AccountDescription"));
		
		Resource sampleFile = new ClassPathResource(resources[1]);
		assertTrue(sampleFile.exists());
		
		try (WordParser parser = new WordParser()) {
			
			parser.setPath(sampleFile.getFile().toPath());
			parser.setDocumentInputSpec(inputSpec);
			parser.start();
			
			try (DataIterator iterator = parser.iterator();) {
				
				assertTrue(iterator.hasNext(), "Should find the first record");
				Map<String,Object> record = iterator.next();
				assertEquals("123456", toString(record.get(TaxPayerId.name())));
				assertEquals("2021", toString(record.get(TaxYear.name())));
				assertEquals("1.1.1", toString(record.get(AccountCode.name())));
				assertEquals("1", toString(record.get(AccountCategory.name())));
				assertEquals("1.10", toString(record.get(AccountSubcategory.name())));
				assertEquals("Cash", toString(record.get(AccountName.name())));
				assertEquals("Cash and Cash Equivalents", toString(record.get(AccountDescription.name())));

				assertTrue(iterator.hasNext(), "Should find the second record");
				record = iterator.next();
				assertEquals("123456", toString(record.get(TaxPayerId.name())));
				assertEquals("2021", toString(record.get(TaxYear.name())));
				assertEquals("1.2.1", toString(record.get(AccountCode.name())));
				assertEquals("1", toString(record.get(AccountCategory.name())));
				assertEquals("1.9", toString(record.get(AccountSubcategory.name())));
				assertEquals("Accounts Receivable", toString(record.get(AccountName.name())));
				assertEquals("Accounts, Notes And Loans Receivable", toString(record.get(AccountDescription.name())));

				assertTrue(iterator.hasNext(), "Should find the third record");
				record = iterator.next();
				assertEquals("123456", toString(record.get(TaxPayerId.name())));
				assertEquals("2021", toString(record.get(TaxYear.name())));
				assertEquals("1.3.1", toString(record.get(AccountCode.name())));
				assertEquals("1", toString(record.get(AccountCategory.name())));
				assertEquals("1.7", toString(record.get(AccountSubcategory.name())));
				assertEquals("Inventory", toString(record.get(AccountName.name())));
				assertEquals("Merchandise in Inventory", toString(record.get(AccountDescription.name())));

				for (int i=4; i<=14; i++) {
					assertTrue(iterator.hasNext(), "Should find the "+i+"th record");
					record = iterator.next();
					assertEquals("123456", toString(record.get(TaxPayerId.name())));
					assertEquals("2021", toString(record.get(TaxYear.name())));					
				}

				assertFalse(iterator.hasNext(), "Should not find any more records!");
				
			}		
		}
	}

	public static String toString(Object value) {
		if ( value == null )
			return null;
		if (value instanceof Number) {
			return String.valueOf(((Number)value).longValue());
		}
		else {
			return value.toString();
		}
	}

}
