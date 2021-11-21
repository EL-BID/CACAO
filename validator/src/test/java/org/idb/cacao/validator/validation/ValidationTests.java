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
package org.idb.cacao.validator.validation;

import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.AccountCategory;
import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.AccountCode;
import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.AccountDescription;
import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.AccountName;
import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.AccountSubcategory;
import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.TaxPayerId;
import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.TaxYear;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

import org.idb.cacao.account.elements.AccountCategory;
import org.idb.cacao.account.elements.AccountSubcategory;
import org.idb.cacao.api.DocumentUploaded;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.api.templates.DocumentInput;
import org.idb.cacao.api.templates.DocumentInputFieldMapping;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.DomainTable;
import org.idb.cacao.api.templates.FieldMapping;
import org.idb.cacao.api.templates.FieldType;
import org.idb.cacao.mock_es.ElasticsearchMockClient;
import org.idb.cacao.validator.parsers.CSVParser;
import org.idb.cacao.validator.parsers.DataIterator;
import org.idb.cacao.validator.repositories.DomainTableRepository;
import org.idb.cacao.validator.validations.Validations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Tests validations against document templates and provided values
 * 
 * @author Rivelino Patrício
 * 
 * @since 20/11/2021
 *
 */
@RunWith(JUnitPlatform.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ValidationTests {
	
	private static ElasticsearchMockClient mockElastic;
	
	@Autowired
	private Validations validations;
	
	@Autowired
	private DomainTableRepository domainTableRepository;	
	
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
	 * Test the sample file '20211411 - Pauls Guitar Shop - Chart of Accounts.csv' with the input mapping
	 * given as column positions.
	 */
	@Test
	void testWithCSV() throws Exception {		
		
		DomainTable accountCategory = fromEnum("Account Category IFRS", /*version*/"1.0", 
				/*enumeration with values*/AccountCategory.class, 
				/*getKey*/org.idb.cacao.account.elements.AccountCategory::getIfrsNumber,Object::toString);
		
		domainTableRepository.saveWithTimestamp(accountCategory);
		
		DomainTable accountSubcategory = fromEnum("Account Subcategory IFRS", /*version*/"1.0", 
				/*enumeration with values*/AccountSubcategory.class, 
				/*getKey*/org.idb.cacao.account.elements.AccountSubcategory::getIfrsNumber,Object::toString);
		
		domainTableRepository.saveWithTimestamp(accountSubcategory);
		
		DocumentTemplate template = new DocumentTemplate();
		template.setFields(
				Arrays.asList(
						new DocumentField()
							.withFieldName(TaxPayerId.name())
							.withFieldType(FieldType.CHARACTER)
							.withFieldMapping(FieldMapping.TAXPAYER_ID)
							.withDescription("Taxpayer Identification Number")
							.withMaxLength(128)
							.withRequired(true),
						new DocumentField()
							.withFieldName(TaxYear.name())
							.withFieldType(FieldType.INTEGER)
							.withFieldMapping(FieldMapping.TAX_YEAR)
							.withDescription("Fiscal year of this financial reporting")
							.withRequired(true),
						new DocumentField()
							.withFieldName(AccountCode.name())
							.withFieldType(FieldType.CHARACTER)
							.withDescription("Account code")
							.withMaxLength(256)
							.withRequired(true),
						new DocumentField()
							.withFieldName(AccountCategory.name())
							.withFieldType(FieldType.DOMAIN)
							.withDomainTableName(accountCategory.getName())
							.withDomainTableVersion(accountCategory.getVersion())
							.withDescription("Category of this account")
							.withMaxLength(256)
							.withRequired(true),
						new DocumentField()
							.withFieldName(AccountSubcategory.name())
							.withFieldType(FieldType.DOMAIN)
							.withDomainTableName(accountSubcategory.getName())
							.withDomainTableVersion(accountSubcategory.getVersion())
							.withDescription("Sub-category of this account")
							.withMaxLength(256)
							.withRequired(true),
						new DocumentField()
							.withFieldName(AccountName.name())
							.withFieldType(FieldType.CHARACTER)
							.withDescription("Account name for displaying alongside the account code in different financial reports")
							.withMaxLength(256)
							.withRequired(true),
						new DocumentField()
							.withFieldName(AccountDescription.name())
							.withFieldType(FieldType.CHARACTER)
							.withDescription("Account description")
							.withMaxLength(1024)
							.withRequired(false)
					));
		
		DocumentInput inputSpec = new DocumentInput();
		inputSpec.setFormat(DocumentFormat.CSV);
		inputSpec.setInputName("ChartOfAccounts CSV");
		template.addInput(inputSpec);
		
		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(TaxPayerId.name())
				.withColumnIndex(0));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(TaxYear.name())
				.withColumnIndex(1));
		
		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(AccountCode.name())
				.withColumnIndex(2));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(AccountCategory.name())
				.withColumnIndex(3));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(AccountSubcategory.name())
				.withColumnIndex(4));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(AccountName.name())
				.withColumnIndex(5));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(AccountDescription.name())
				.withColumnIndex(6));
		
		Resource sampleFile = new ClassPathResource("/samples/20211411 - Pauls Guitar Shop - Chart of Accounts_validations.csv");
		assertTrue(sampleFile.exists());
		
		DocumentUploaded doc = new DocumentUploaded();
		doc.setTemplateName(template.getName());
		
		ValidationContext validationContext = new ValidationContext();
		validationContext.setDocumentUploaded(doc);
		validationContext.setDocumentTemplate(template);
		validationContext.setDocumentPath(Path.of(sampleFile.getURI()));		
		
		try (CSVParser parser = new CSVParser();) {
			
			parser.setPath(sampleFile.getFile().toPath());
			parser.setDocumentInputSpec(inputSpec);
			parser.start();

			int added = 0;
			try (DataIterator iterator = parser.iterator();) {
				
				while ( iterator.hasNext() ) {
				
					Map<String,Object> record = iterator.next();
					
					if (record == null)
						continue;
	
					validationContext.addParsedContent(record);
					added++;				
					
				}				

			}
			assertEquals(14, added, "Number of records are different from expected");
			
		}
		
		//Configure object validations
		validations.setValidationContext(validationContext);
		// Add TaxPayerId and TaxPeriod to document on database
		validations.addTaxPayerInformation();
		// Update document on database
		
		// Should perform generic validations:
		// check for required fields
		validations.checkForRequiredFields();

		// check for mismatch in field types (should try to automatically convert some
		// field types, e.g. String -> Date)
		validations.checkForFieldDataTypes();

		// check for domain table fields
		validations.checkForDomainTableValues();
		
		assertEquals("123456", validationContext.getDocumentUploaded().getTaxPayerId(), "Taxpayer Id is incorrect.");
		
		assertEquals(2021, validationContext.getDocumentUploaded().getTaxYear(), "Tax year is incorrect.");
		
		assertEquals(9, validationContext.getAlerts().size(), "Alerts count wasn't as expected.");
		
		assertEquals(5, validationContext.getAlerts().stream().filter(message->message.contains("field.value.not.found")).count(), 
				"Alerts about required field values are incorrect.");
		
		assertEquals(4, validationContext.getAlerts().stream().filter(message->message.contains("field.domain.value.not.found")).count(), 
				"Alerts about required domain field values are incorrect.");
		
	}
	
	/**
	 * Creates a new built-in DomainTable given enumeration constants (this must be resolved at
	 * runtime). The 'keys' are calculated using the provided 'getKey' function over each enum constant. The constants that evaluates
	 * to NULL or empty key are filtered out.<BR> 
	 * The 'descriptions' are calculated using the provided 'getValue' function over each enum constant. 
	 * IMPORTANT: it's expected that the 'getValue' method return a 'messages.properties' entry, not the description itself.
	 */
	private <T extends Enum<?>> DomainTable fromEnum(String name, String version, Class<T> enumeration, 
			Function<T,String> getKey,
			Function<T,String> getValue) {
		DomainTable domain = new DomainTable(name, version);
		for (T element: enumeration.getEnumConstants()) {
			String key = getKey.apply(element);
			if (key==null || key.trim().length()==0)
				continue;
			String messagePropertyRef = getValue.apply(element);			
			domain.addBuiltInEntry(key, messagePropertyRef);
		}
		return domain;
	}

}
