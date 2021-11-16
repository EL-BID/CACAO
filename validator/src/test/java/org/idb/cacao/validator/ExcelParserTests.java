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
package org.idb.cacao.validator;

import org.idb.cacao.account.archetypes.ChartOfAccountsArchetype;
import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.api.templates.DocumentInput;
import org.idb.cacao.api.templates.DocumentInputFieldMapping;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.TemplateArchetype;
import org.idb.cacao.mock_es.ElasticsearchMockClient;
import org.idb.cacao.validator.parsers.DataIterator;
import org.idb.cacao.validator.parsers.ExcelParser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.*;

/**
 * Tests sample files in Excel format with the ExcepParser implemented in VALIDATOR
 * 
 * @author Gustavo Figueiredo
 *
 */
@RunWith(JUnitPlatform.class)
public class ExcelParserTests {
	
	private static ElasticsearchMockClient mockElastic;

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
	 * Test the sample file '20211411 - Pauls Guitar Shop - Chart of Accounts.xlsx' with the input mapping
	 * given as column positions and sheet position.
	 */
	@Test
	void testChartOfAccounts01() throws Exception {
		
		Resource sampleFile = new ClassPathResource("/samples/20211411 - Pauls Guitar Shop - Chart of Accounts.xlsx");
		assertTrue(sampleFile.exists());
		
		TemplateArchetype archetype = new ChartOfAccountsArchetype();
		DocumentTemplate template = new DocumentTemplate();
		template.setArchetype(archetype.getName());
		template.setFields(archetype.getRequiredFields());
		
		DocumentInput inputSpec = new DocumentInput();
		inputSpec.setFormat(DocumentFormat.XLS);
		inputSpec.setInputName("ChartOfAccounts Excel");
		template.addInput(inputSpec);
		
		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(TaxPayerId.name())
				.withColumnIndex(0)
				.withSheetIndex(0));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(TaxYear.name())
				.withColumnIndex(1)
				.withSheetIndex(0));
		
		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(AccountCode.name())
				.withColumnIndex(2)
				.withSheetIndex(0));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(AccountCategory.name())
				.withColumnIndex(3)
				.withSheetIndex(0));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(AccountSubcategory.name())
				.withColumnIndex(4)
				.withSheetIndex(0));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(AccountName.name())
				.withColumnIndex(5)
				.withSheetIndex(0));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(AccountDescription.name())
				.withColumnIndex(6)
				.withSheetIndex(0));

		try (ExcelParser parser = new ExcelParser();) {
			
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
				assertEquals("1.1", toString(record.get(AccountSubcategory.name())));
				assertEquals("Cash", toString(record.get(AccountName.name())));
				assertEquals("Cash and Cash Equivalents", toString(record.get(AccountDescription.name())));

				assertTrue(iterator.hasNext(), "Should find the second record");
				record = iterator.next();
				assertEquals("123456", toString(record.get(TaxPayerId.name())));
				assertEquals("2021", toString(record.get(TaxYear.name())));
				assertEquals("1.2.1", toString(record.get(AccountCode.name())));
				assertEquals("1", toString(record.get(AccountCategory.name())));
				assertEquals("1.2", toString(record.get(AccountSubcategory.name())));
				assertEquals("Accounts Receivable", toString(record.get(AccountName.name())));
				assertEquals("Accounts, Notes And Loans Receivable", toString(record.get(AccountDescription.name())));

				assertTrue(iterator.hasNext(), "Should find the third record");
				record = iterator.next();
				assertEquals("123456", toString(record.get(TaxPayerId.name())));
				assertEquals("2021", toString(record.get(TaxYear.name())));
				assertEquals("1.3.1", toString(record.get(AccountCode.name())));
				assertEquals("1", toString(record.get(AccountCategory.name())));
				assertEquals("1.3", toString(record.get(AccountSubcategory.name())));
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
	
	/**
	 * Test the sample file '20211411 - Pauls Guitar Shop - Chart of Accounts.xlsx' with the input mapping
	 * given as text expressions
	 */
	@Test
	void testChartOfAccounts02() throws Exception {
		
		Resource sampleFile = new ClassPathResource("/samples/20211411 - Pauls Guitar Shop - Chart of Accounts.xlsx");
		assertTrue(sampleFile.exists());
		
		TemplateArchetype archetype = new ChartOfAccountsArchetype();
		DocumentTemplate template = new DocumentTemplate();
		template.setArchetype(archetype.getName());
		template.setFields(archetype.getRequiredFields());
		
		DocumentInput inputSpec = new DocumentInput();
		inputSpec.setFormat(DocumentFormat.XLS);
		inputSpec.setInputName("ChartOfAccounts Excel");
		template.addInput(inputSpec);
		
		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(TaxPayerId.name())
				.withColumnNameExpression("taxpayerid") // case insensitive match (should find "TaxPayerId" column)
				.withSheetNameExpression("chart")); // partial match (should find "Chart of Accounts" sheet)

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(TaxYear.name())
				.withColumnNameExpression("year") // partial match (should find "TaxYear" column)
				.withSheetNameExpression("chart"));
		
		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(AccountCode.name())
				.withColumnNameExpression("code")	// partial match (should find "AccountCode" column)
				.withSheetNameExpression("chart"));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(AccountCategory.name())
				.withColumnNameExpression("accountcategory")	// case insensitive match (should find "AccountCategory" column)
				.withSheetNameExpression("chart"));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(AccountSubcategory.name())
				.withColumnNameExpression(".*subcategory")	// pattern match (should find 'AccountSubcategory' column)
				.withSheetIndex(0));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(AccountName.name())
				.withColumnNameExpression("name")		// partial match (should find "AccountName" column)
				.withSheetNameExpression("chart"));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(AccountDescription.name())
				.withColumnNameExpression("desc")		// partial match (should find "AccountDescription" column)
				.withSheetNameExpression("chart"));

		try (ExcelParser parser = new ExcelParser();) {
			
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
				assertEquals("1.1", toString(record.get(AccountSubcategory.name())));
				assertEquals("Cash", toString(record.get(AccountName.name())));
				assertEquals("Cash and Cash Equivalents", toString(record.get(AccountDescription.name())));

				assertTrue(iterator.hasNext(), "Should find the second record");
				record = iterator.next();
				assertEquals("123456", toString(record.get(TaxPayerId.name())));
				assertEquals("2021", toString(record.get(TaxYear.name())));
				assertEquals("1.2.1", toString(record.get(AccountCode.name())));
				assertEquals("1", toString(record.get(AccountCategory.name())));
				assertEquals("1.2", toString(record.get(AccountSubcategory.name())));
				assertEquals("Accounts Receivable", toString(record.get(AccountName.name())));
				assertEquals("Accounts, Notes And Loans Receivable", toString(record.get(AccountDescription.name())));

				assertTrue(iterator.hasNext(), "Should find the third record");
				record = iterator.next();
				assertEquals("123456", toString(record.get(TaxPayerId.name())));
				assertEquals("2021", toString(record.get(TaxYear.name())));
				assertEquals("1.3.1", toString(record.get(AccountCode.name())));
				assertEquals("1", toString(record.get(AccountCategory.name())));
				assertEquals("1.3", toString(record.get(AccountSubcategory.name())));
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
		if (value instanceof Number) {
			return String.valueOf(((Number)value).longValue());
		}
		else {
			return value.toString();
		}
	}

}
