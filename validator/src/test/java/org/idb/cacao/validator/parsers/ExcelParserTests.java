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
package org.idb.cacao.validator.parsers;

import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.AccountCategory;
import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.AccountCode;
import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.AccountDescription;
import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.AccountName;
import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.AccountSubcategory;
import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.TaxPayerId;
import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.TaxYear;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.idb.cacao.account.archetypes.ChartOfAccountsArchetype;
import org.idb.cacao.api.DocumentUploaded;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.api.templates.DocumentInput;
import org.idb.cacao.api.templates.DocumentInputFieldMapping;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.FieldMapping;
import org.idb.cacao.api.templates.FieldType;
import org.idb.cacao.api.templates.TemplateArchetype;
import org.idb.cacao.mock_es.ElasticsearchMockClient;
import org.idb.cacao.validator.validations.Validations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Tests sample files in Excel format with the ExcelParser implemented in VALIDATOR
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

	/**
	 * Test the sample file 'SimpleNamedCells.xlsx' with the input mapping
	 * given as named cells.
	 */
	@Test
	void testSimpleNamedCells() throws Exception {
		
		Resource sampleFile = new ClassPathResource("/samples/SimpleNamedCells.xlsx");
		assertTrue(sampleFile.exists());
		
		DocumentTemplate template = new DocumentTemplate();
		template.setName("Simple Test");
		template.setVersion("1.0");
		template.addField(new DocumentField("Field for Id").withFieldMapping(FieldMapping.TAXPAYER_ID).withFieldType(FieldType.CHARACTER));
		template.addField(new DocumentField("Field for Name").withFieldType(FieldType.CHARACTER));
		
		DocumentInput inputSpec = new DocumentInput();
		inputSpec.setFormat(DocumentFormat.XLS);
		inputSpec.setInputName("Simple Test Excel");
		template.addInput(inputSpec);
		
		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName("Field for Id")
				.withCellName("Id"));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName("Field for Name")
				.withCellName("Name"));
		
		try (ExcelParser parser = new ExcelParser();) {
			
			parser.setPath(sampleFile.getFile().toPath());
			parser.setDocumentInputSpec(inputSpec);
			parser.start();
			
			try (DataIterator iterator = parser.iterator();) {
				
				assertTrue(iterator.hasNext(), "Should find the first record");
				Map<String,Object> record = iterator.next();
				assertEquals("Gustavo", toString(record.get("Field for Name")));
				assertEquals("1234", toString(record.get("Field for Id")));

				assertFalse(iterator.hasNext(), "Should not find any more records!");
				
				ValidationContext context = new ValidationContext();
				context.setDocumentTemplate(template);
				context.setDocumentUploaded(new DocumentUploaded());
				context.addParsedContent(record);
				Validations validations = new Validations(context, /*domainTableRepository*/null);
				
				validations.addTaxPayerInformation();
				assertEquals("1234", context.getDocumentUploaded().getTaxPayerId(), "The taxpayer Id does not correspond to what is expected");
				assertNull(context.getDocumentUploaded().getTaxYear(), "There should not be indication of year");
				assertNull(context.getDocumentUploaded().getTaxMonth(), "There should not be indication of month");
				assertNull(context.getDocumentUploaded().getTaxPeriod(), "There should not be indication of period");
				
				validations.checkForFieldDataTypes();
				validations.checkForRequiredFields();
				
				assertFalse(context.hasAlerts(), "There should be no alerts");
			}
			
		}		
		
	}

	/**
	 * Test the sample file 'ColumnsOfNamedCells.xlsx' with the input mapping
	 * given as named cells.
	 */
	@Test
	void testColumnsOfNamedCells() throws Exception {
		
		Resource sampleFile = new ClassPathResource("/samples/ColumnsOfNamedCells.xlsx");
		assertTrue(sampleFile.exists());
		
		DocumentTemplate template = new DocumentTemplate();
		template.setName("Simple Test");
		template.setVersion("2.0");
		template.addField(new DocumentField("Field for Id").withFieldMapping(FieldMapping.TAXPAYER_ID).withFieldType(FieldType.CHARACTER));
		template.addField(new DocumentField("Field for Name").withFieldType(FieldType.CHARACTER));
		template.addField(new DocumentField("Field for Year").withFieldMapping(FieldMapping.TAX_YEAR).withFieldType(FieldType.INTEGER));
		template.addField(new DocumentField("Field for Product Code").withFieldType(FieldType.CHARACTER).withRequired(Boolean.TRUE));
		template.addField(new DocumentField("Field for Product Name").withFieldType(FieldType.CHARACTER).withRequired(Boolean.TRUE));
		template.addField(new DocumentField("Field for Unity Price").withFieldType(FieldType.DECIMAL).withRequired(Boolean.TRUE));
		
		DocumentInput inputSpec = new DocumentInput();
		inputSpec.setFormat(DocumentFormat.XLS);
		inputSpec.setInputName("Simple Test Excel");
		template.addInput(inputSpec);
		
		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName("Field for Id")
				.withCellName("Id"));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName("Field for Name")
				.withCellName("Name"));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName("Field for Year")
				.withCellName("Year"));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName("Field for Product Code")
				.withCellName("ProductCode"));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName("Field for Product Name")
				.withCellName("ProductName"));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName("Field for Unity Price")
				.withCellName("UnityPrice"));

		try (ExcelParser parser = new ExcelParser();) {
			
			parser.setPath(sampleFile.getFile().toPath());
			parser.setDocumentInputSpec(inputSpec);
			parser.start();
			
			try (DataIterator iterator = parser.iterator();) {
				
				ValidationContext context = new ValidationContext();
				context.setDocumentTemplate(template);
				context.setDocumentUploaded(new DocumentUploaded());

				assertTrue(iterator.hasNext(), "Should find the first record");
				Map<String,Object> record = iterator.next();
				assertEquals("Gustavo", toString(record.get("Field for Name")));
				assertEquals("1234", toString(record.get("Field for Id")));
				assertEquals("2021", toString(record.get("Field for Year")));
				assertEquals("1000", toString(record.get("Field for Product Code")));
				assertEquals("IPhone", toString(record.get("Field for Product Name")));
				assertEquals("1000", toString(record.get("Field for Unity Price")));
				context.addParsedContent(record);

				assertTrue(iterator.hasNext(), "Should find the second record");
				record = iterator.next();
				assertEquals("Gustavo", toString(record.get("Field for Name")));
				assertEquals("1234", toString(record.get("Field for Id")));
				assertEquals("2021", toString(record.get("Field for Year")));
				assertEquals("2000", toString(record.get("Field for Product Code")));
				assertEquals("IPad", toString(record.get("Field for Product Name")));
				assertEquals("800", toString(record.get("Field for Unity Price")));
				context.addParsedContent(record);

				assertTrue(iterator.hasNext(), "Should find the third record");
				record = iterator.next();
				assertEquals("Gustavo", toString(record.get("Field for Name")));
				assertEquals("1234", toString(record.get("Field for Id")));
				assertEquals("2021", toString(record.get("Field for Year")));
				assertEquals("3000", toString(record.get("Field for Product Code")));
				assertEquals("Charger", toString(record.get("Field for Product Name")));
				assertEquals("50", toString(record.get("Field for Unity Price")));
				context.addParsedContent(record);

				assertFalse(iterator.hasNext(), "Should not find any more records!");
				
				Validations validations = new Validations(context, /*domainTableRepository*/null);
				
				validations.addTaxPayerInformation();
				assertEquals("1234", context.getDocumentUploaded().getTaxPayerId(), "The taxpayer Id does not correspond to what is expected");
				assertEquals(2021, context.getDocumentUploaded().getTaxYear(), "The tax year does not correspond to what is expected");
				assertNull(context.getDocumentUploaded().getTaxMonth(), "There should not be indication of month");
				assertEquals(2021, context.getDocumentUploaded().getTaxPeriodNumber(), "The period number does not correspond to what is expected");
				
				validations.checkForFieldDataTypes();
				validations.checkForRequiredFields();
				
				assertFalse(context.hasAlerts(), "There should be no alerts");
			}
			
		}		
		
	}

	/**
	 * Same as 'testColumnsOfNamedCells', but use cell range references instead of cell names.
	 * 
	 * Test the sample file 'ColumnsOfNamedCells.xlsx' with the input mapping given as cell ranges.
	 */
	@Test
	void testColumnsOfNamedCells2() throws Exception {
		
		Resource sampleFile = new ClassPathResource("/samples/ColumnsOfNamedCells.xlsx");
		assertTrue(sampleFile.exists());
		
		DocumentTemplate template = new DocumentTemplate();
		template.setName("Simple Test");
		template.setVersion("2.0");
		template.addField(new DocumentField("Field for Id").withFieldMapping(FieldMapping.TAXPAYER_ID).withFieldType(FieldType.CHARACTER));
		template.addField(new DocumentField("Field for Name").withFieldType(FieldType.CHARACTER));
		template.addField(new DocumentField("Field for Year").withFieldMapping(FieldMapping.TAX_YEAR).withFieldType(FieldType.INTEGER));
		template.addField(new DocumentField("Field for Product Code").withFieldType(FieldType.CHARACTER).withRequired(Boolean.TRUE));
		template.addField(new DocumentField("Field for Product Name").withFieldType(FieldType.CHARACTER).withRequired(Boolean.TRUE));
		template.addField(new DocumentField("Field for Unity Price").withFieldType(FieldType.DECIMAL).withRequired(Boolean.TRUE));
		
		DocumentInput inputSpec = new DocumentInput();
		inputSpec.setFormat(DocumentFormat.XLS);
		inputSpec.setInputName("Simple Test Excel");
		template.addInput(inputSpec);
		
		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName("Field for Id")
				.withCellName("E4"));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName("Field for Name")
				.withCellName("E3"));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName("Field for Year")
				.withCellName("E5"));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName("Field for Product Code")
				.withCellName("C8:C13"));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName("Field for Product Name")
				.withCellName("D8:D13"));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName("Field for Unity Price")
				.withCellName("E8:E13"));

		try (ExcelParser parser = new ExcelParser();) {
			
			parser.setPath(sampleFile.getFile().toPath());
			parser.setDocumentInputSpec(inputSpec);
			parser.start();
			
			try (DataIterator iterator = parser.iterator();) {
				
				ValidationContext context = new ValidationContext();
				context.setDocumentTemplate(template);
				context.setDocumentUploaded(new DocumentUploaded());

				assertTrue(iterator.hasNext(), "Should find the first record");
				Map<String,Object> record = iterator.next();
				assertEquals("Gustavo", toString(record.get("Field for Name")));
				assertEquals("1234", toString(record.get("Field for Id")));
				assertEquals("2021", toString(record.get("Field for Year")));
				assertEquals("1000", toString(record.get("Field for Product Code")));
				assertEquals("IPhone", toString(record.get("Field for Product Name")));
				assertEquals("1000", toString(record.get("Field for Unity Price")));
				context.addParsedContent(record);

				assertTrue(iterator.hasNext(), "Should find the second record");
				record = iterator.next();
				assertEquals("Gustavo", toString(record.get("Field for Name")));
				assertEquals("1234", toString(record.get("Field for Id")));
				assertEquals("2021", toString(record.get("Field for Year")));
				assertEquals("2000", toString(record.get("Field for Product Code")));
				assertEquals("IPad", toString(record.get("Field for Product Name")));
				assertEquals("800", toString(record.get("Field for Unity Price")));
				context.addParsedContent(record);

				assertTrue(iterator.hasNext(), "Should find the third record");
				record = iterator.next();
				assertEquals("Gustavo", toString(record.get("Field for Name")));
				assertEquals("1234", toString(record.get("Field for Id")));
				assertEquals("2021", toString(record.get("Field for Year")));
				assertEquals("3000", toString(record.get("Field for Product Code")));
				assertEquals("Charger", toString(record.get("Field for Product Name")));
				assertEquals("50", toString(record.get("Field for Unity Price")));
				context.addParsedContent(record);

				assertFalse(iterator.hasNext(), "Should not find any more records!");
				
				Validations validations = new Validations(context, /*domainTableRepository*/null);
				
				validations.addTaxPayerInformation();
				assertEquals("1234", context.getDocumentUploaded().getTaxPayerId(), "The taxpayer Id does not correspond to what is expected");
				assertEquals(2021, context.getDocumentUploaded().getTaxYear(), "The tax year does not correspond to what is expected");
				assertNull(context.getDocumentUploaded().getTaxMonth(), "There should not be indication of month");
				assertEquals(2021, context.getDocumentUploaded().getTaxPeriodNumber(), "The period number does not correspond to what is expected");
				
				validations.checkForFieldDataTypes();
				validations.checkForRequiredFields();
				
				assertFalse(context.hasAlerts(), "There should be no alerts");
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
