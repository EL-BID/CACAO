package org.idb.cacao.validator.parsers;

import org.idb.cacao.account.archetypes.ChartOfAccountsArchetype;
import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.api.templates.DocumentInput;
import org.idb.cacao.api.templates.DocumentInputFieldMapping;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.TemplateArchetype;
import org.idb.cacao.validator.fileformats.FileFormatFactory;
import org.idb.cacao.validator.utils.JSONUtils;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Map;

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

/**
 * Tests sample files in JSON format with the JSONParser implemented in VALIDATOR
 *
 * @author Leon Silva
 */
@RunWith(JUnitPlatform.class)
public class JSONParserTests {

    /**
     * Tests wether sample file '20211411 - Pauls Guitar Shop - Chart of Accounts.json' is JSON Valid
     */
    @Test
    void testChartOfAccountsJSONValid() throws Exception {
        Resource sampleFile = new ClassPathResource("/samples/20211411 - Pauls Guitar Shop - Chart of Accounts - IFRS.json");
        assertTrue(sampleFile.exists());

        BufferedReader br = new BufferedReader(new FileReader(sampleFile.getFile()));

        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            String jsonContent = sb.toString();

            assertTrue(FileFormatFactory.getFileFormat(DocumentFormat.JSON).matchFilename(sampleFile.getFilename()));
            assertTrue(JSONUtils.isJSONValid(jsonContent));
        } finally {
            br.close();
        }

    }

	/**
	 * Test the sample file '20211411 - Pauls Guitar Shop - Chart of Accounts.json' with the input mapping
	 * given as column positions.
	 */
	@Test
	void testChartOfAccounts01() throws Exception {		
		
		TemplateArchetype archetype = new ChartOfAccountsArchetype();
		DocumentTemplate template = new DocumentTemplate();
		template.setArchetype(archetype.getName());
		template.setFields(archetype.getRequiredFields());
		
		DocumentInput inputSpec = new DocumentInput();
		inputSpec.setFormat(DocumentFormat.JSON);
		inputSpec.setInputName("ChartOfAccounts JSON");
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
				.withColumnNameExpression("Subcategory"));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(AccountName.name())
				.withColumnNameExpression("AccountName"));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(AccountDescription.name())
				.withColumnNameExpression("Description"));
		
		for ( String resource : new String[] {
				"/samples/20211411 - Pauls Guitar Shop - Chart of Accounts - IFRS.json"
		} ) {
			
			System.out.println("Testing with: " + resource);
			
			Resource sampleFile = new ClassPathResource(resource);
			assertTrue(sampleFile.exists());
			
			try (JSONParser parser = new JSONParser();) {
				
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
	}

	/**
	 * Test the sample file '20211412 - Pauls Guitar Shop - Chart of Accounts.json' with the input mapping
	 * given as column positions.
	 */
	@Test
	void testChartOfAccounts02() throws Exception {		
		
		TemplateArchetype archetype = new ChartOfAccountsArchetype();
		DocumentTemplate template = new DocumentTemplate();
		template.setArchetype(archetype.getName());
		template.setFields(archetype.getRequiredFields());
		
		DocumentInput inputSpec = new DocumentInput();
		inputSpec.setFormat(DocumentFormat.JSON);
		inputSpec.setInputName("ChartOfAccounts JSON");
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
				.withColumnNameExpression("Subcategory"));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(AccountName.name())
				.withColumnNameExpression("AccountName"));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(AccountDescription.name())
				.withColumnNameExpression("Description"));
		
		for ( String resource : new String[] {
				"/samples/20211412 - Pauls Guitar Shop - Chart of Accounts - IFRS.json"
		} ) {
			
			System.out.println("Testing with: " + resource);
			
			Resource sampleFile = new ClassPathResource(resource);
			assertTrue(sampleFile.exists());
			
			try (JSONParser parser = new JSONParser();) {
				
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
	
					assertFalse(iterator.hasNext(), "Should not find any more records!");
				}
				
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
