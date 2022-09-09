/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.validator.validation;

import static org.idb.cacao.account.archetypes.GeneralLedgerArchetype.FIELDS_NAMES.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Map;

import org.idb.cacao.account.archetypes.GeneralLedgerArchetype;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.api.templates.DocumentInput;
import org.idb.cacao.api.templates.DocumentInputFieldMapping;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.TemplateArchetype;
import org.idb.cacao.validator.parsers.DataIterator;
import org.idb.cacao.validator.parsers.ExcelParser;
import org.idb.cacao.validator.repositories.DomainTableRepository;
import org.idb.cacao.validator.validations.Validations;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Tests related to validating journal where debits are separated from credits
 * 
 * @author Gustavo Figueiredo
 *
 */
@RunWith(JUnitPlatform.class)
public class JournalWithDebitsSeparatedFromCreditsTest {

	@InjectMocks
	DomainTableRepository domainTableRepository;

	/**
	 * Test the sample file 'JournalWithDebitsSeparatedFromCredits.xlsx' with the input mapping
	 * given as column positions and sheet position.
	 */
	@Test
	void testChartOfAccounts01() throws IOException {
		
		Resource sampleFile = new ClassPathResource("/samples/JournalWithDebitsSeparatedFromCredits.xlsx");
		assertTrue(sampleFile.exists());
		
		TemplateArchetype archetype = new GeneralLedgerArchetype();
		DocumentTemplate template = new DocumentTemplate();
		template.setArchetype(archetype.getName());
		template.setFields(archetype.getRequiredFields());
		
		DocumentInput inputSpec = new DocumentInput();
		inputSpec.setFormat(DocumentFormat.XLS);
		inputSpec.setInputName("GeneralLedger Excel");
		template.addInput(inputSpec);
		
		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(TaxPayerId.name())
				.withCellName("TAXPAYERID"));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(TaxYear.name())
				.withCellName("YEAR"));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(Date.name())
				.withColumnNameExpression("Date"));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(AccountCode.name())
				.withColumnNameExpression("Account"));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(AmountDebitOnly.name())
				.withColumnNameExpression("Debit"));

		inputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName(AmountCreditOnly.name())
				.withColumnNameExpression("Credit"));
		
		try (ExcelParser parser = new ExcelParser();) {
			
			parser.setPath(sampleFile.getFile().toPath());
			parser.setDocumentInputSpec(inputSpec);
			parser.setDocumentTemplate(template);
			parser.start();
			
			DataIterator iterator = parser.iterator();

			ValidationContext validationContext = new ValidationContext();
			validationContext.setDocumentTemplate(template);
			validationContext.setDocumentInput(inputSpec);

			while (iterator.hasNext()) {

				Map<String, Object> dataItem = iterator.next();

				if (dataItem == null || dataItem.isEmpty() )
					continue;
				
				validationContext.addParsedContent(dataItem);
			}
			
			Validations validations = new Validations(validationContext, domainTableRepository);
			
			validations.addTaxPayerInformation();
			assertFalse(validationContext.hasAlerts(), "Did not expect errors so far...");

			validations.checkForFieldDataTypes(/*acceptIncompleteFiles*/true);
			assertFalse(validationContext.hasAlerts(), "Did not expect errors so far...");

			validations.checkForRequiredFields(/*acceptIncompleteFiles*/true);
			assertFalse(validationContext.hasAlerts(), "Did not expect errors so far...");

			boolean ok = archetype.validateDocumentUploaded(validationContext);
			assertTrue(ok, "Expected OK for validateDocumentUploaded");
		}		
	}
	
}
