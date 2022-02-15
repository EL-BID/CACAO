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
package org.idb.cacao.web.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.text.CaseUtils;
import org.idb.cacao.account.archetypes.AccountBuiltInDomainTables;
import org.idb.cacao.account.archetypes.ChartOfAccountsArchetype;
import org.idb.cacao.account.archetypes.GeneralLedgerArchetype;
import org.idb.cacao.account.archetypes.OpeningBalanceArchetype;
import org.idb.cacao.account.archetypes.ShareholdingArchetype;
import org.idb.cacao.account.elements.StatementComprehensiveIncome;
import org.idb.cacao.api.Periodicity;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.api.templates.DocumentInput;
import org.idb.cacao.api.templates.DocumentInputFieldMapping;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.FieldMapping;
import org.idb.cacao.api.templates.FieldType;
import org.springframework.context.MessageSource;

/**
 * Creates sample templates given the archetype definitions
 * 
 */
public class CreateDocumentTemplatesSamples {

	/**
	 * Returns new DocumentTemplate given the archetype definitions
	 */
	public static List<DocumentTemplate> getSampleTemplates(MessageSource messages, Locale defaultLocale) {

		List<DocumentTemplate> toRet = new ArrayList<>(5);

		// General Ledger
		DocumentTemplate docTemplate = new DocumentTemplate();
		docTemplate.setName("General Ledger");
		docTemplate.setGroup("Accounting");
		docTemplate.setPeriodicity(Periodicity.YEARLY);
		docTemplate.setRequired(true);
		docTemplate.setVersion("1.0");
		docTemplate.setArchetype("accounting.general.ledger");
		docTemplate.setActive(true);

		toRet.add(docTemplate);
		new GeneralLedgerArchetype().getRequiredFields().forEach(docTemplate::addField);
		addInputsGeneralLedger(docTemplate);

		// IFRS Chart Of Accounts
		docTemplate = new DocumentTemplate();
		docTemplate.setName("Chart Of Accounts");
		docTemplate.setGroup("Accounting");
		docTemplate.setPeriodicity(Periodicity.YEARLY);
		docTemplate.setRequired(true);
		docTemplate.setVersion("1.0");
		docTemplate.setArchetype("accounting.chart.accounts");
		docTemplate.setActive(true);

		toRet.add(docTemplate);
		new ChartOfAccountsArchetype().getRequiredFields().forEach(docTemplate::addField);
		addInputsChartOfAccounts(docTemplate);

		// Opening Balance
		docTemplate = new DocumentTemplate();
		docTemplate.setName("Opening Balance");
		docTemplate.setGroup("Accounting");
		docTemplate.setPeriodicity(Periodicity.YEARLY);
		docTemplate.setRequired(true);
		docTemplate.setVersion("1.0");
		docTemplate.setArchetype("accounting.opening.balance");
		docTemplate.setActive(true);

		toRet.add(docTemplate);
		new OpeningBalanceArchetype().getRequiredFields().forEach(docTemplate::addField);
		addInputsOpeningBalance(docTemplate);

		// Lalur
		docTemplate = new DocumentTemplate();
		docTemplate.setName("Lalur");
		docTemplate.setGroup("Accounting");
		docTemplate.setPeriodicity(Periodicity.YEARLY);
		docTemplate.setRequired(false);
		docTemplate.setVersion("1.0");
		docTemplate.setActive(true);

		toRet.add(docTemplate);
		int fieldIndex = 1;
		List<DocumentField> fields = Arrays.asList(
				new DocumentField().withFieldName("TaxPayerId").withFieldType(FieldType.CHARACTER)
						.withId(fieldIndex++)
						.withFieldMapping(FieldMapping.TAXPAYER_ID).withDescription("Taxpayer Identification Number")
						.withMaxLength(128).withRequired(true)
						.withFileUniqueness(true)
						.withPersonalData(true),
				new DocumentField().withFieldName("TaxYear").withFieldType(FieldType.INTEGER)
						.withId(fieldIndex++)
						.withFieldMapping(FieldMapping.TAX_YEAR)
						.withDescription("Fiscal year of this financial reporting").withRequired(true)
						.withFileUniqueness(true),
				new DocumentField().withFieldName("AccountCode").withFieldType(FieldType.CHARACTER)
						.withId(fieldIndex++)
						.withDescription("Account code (reference to Chart of Account)").withMaxLength(256)
						.withRequired(true),
				new DocumentField().withFieldName("InitialDate").withFieldType(FieldType.DATE)
						.withId(fieldIndex++)
						.withDescription("The final date of the period when this account was created")
						.withRequired(true),
				new DocumentField().withFieldName("FinalDate").withFieldType(FieldType.DATE)
						.withId(fieldIndex++)
						.withDescription("The date until this balance can be used").withRequired(true),
				new DocumentField().withFieldName("FinalBalance").withFieldType(FieldType.DECIMAL)
						.withId(fieldIndex++)
						.withDescription("The monetary amount of final balance for this account").withRequired(true),
				new DocumentField().withFieldName("DebitCredit").withFieldType(FieldType.DOMAIN)
						.withId(fieldIndex++)
						.withDomainTableName(AccountBuiltInDomainTables.DEBIT_CREDIT.getName())
						.withDomainTableVersion(AccountBuiltInDomainTables.DEBIT_CREDIT.getVersion())
						.withDescription("This is an indication of whether this balance is debit or credit")
						.withMaxLength(32).withRequired(true));

		docTemplate.setFields(fields);
		addInputsLalur(docTemplate);

		// Income Statement
		docTemplate = new DocumentTemplate();
		docTemplate.setName("Income Statement");
		docTemplate.setGroup("Financial Report");
		docTemplate.setPeriodicity(Periodicity.YEARLY);
		docTemplate.setRequired(false);
		docTemplate.setVersion("1.0");
		docTemplate.setArchetype("accounting.income.statement");
		docTemplate.setActive(true);

		toRet.add(docTemplate);
		fieldIndex = 1;
		fields = new ArrayList<>();
		fields.add(new DocumentField().withFieldName("TaxPayerId").withFieldType(FieldType.CHARACTER)
				.withId(fieldIndex++)
				.withFieldMapping(FieldMapping.TAXPAYER_ID).withDescription("Taxpayer Identification Number")
				.withMaxLength(128).withRequired(true)
				.withFileUniqueness(true)
				.withPersonalData(true));
		fields.add(new DocumentField().withFieldName("TaxYear").withFieldType(FieldType.INTEGER)
				.withId(fieldIndex++)
				.withFieldMapping(FieldMapping.TAX_YEAR)
				.withDescription("Fiscal year of this financial reporting").withRequired(true)
				.withFileUniqueness(true));
		for (StatementComprehensiveIncome stmt: StatementComprehensiveIncome.values()) {
			DocumentField field = new DocumentField()
			.withId(fieldIndex++)
			.withFieldName(CaseUtils.toCamelCase(stmt.name(), true, '_'))
			.withFieldType(FieldType.DECIMAL);
			try {
				field.withDescription(messages.getMessage(stmt.toString(), null, defaultLocale));
			} catch (Throwable ex) { }
			fields.add(field);
		}

		docTemplate.setFields(fields);
		addInputsIncomeStatement(docTemplate);

		// Shareholding
		docTemplate = new DocumentTemplate();
		docTemplate.setName("Shareholding");
		docTemplate.setGroup("Financial Report");
		docTemplate.setPeriodicity(Periodicity.YEARLY);
		docTemplate.setRequired(false);
		docTemplate.setVersion("1.0");
		docTemplate.setArchetype("accounting.shareholding");
		docTemplate.setActive(true);

		toRet.add(docTemplate);
		new ShareholdingArchetype().getRequiredFields().forEach(docTemplate::addField);
		addShareholding(docTemplate);

		return toRet;

	}
	
	/**
	 * Given a document template and its DocumentField's definitions, returns a list of DocumentInputFieldMapping
	 * considering as input columns at fixed positions.
	 */
	public static List<DocumentInputFieldMapping> getMappingsWithFixedColumnPositions(DocumentTemplate docTemplate) {
		List<DocumentInputFieldMapping> mappings = new ArrayList<>(docTemplate.getFields().size());
		for (DocumentField field: docTemplate.getFields()) {
			mappings.add(new DocumentInputFieldMapping().withFieldName(field.getFieldName()).withColumnIndex(mappings.size()));
		}
		return mappings;
	}

	/**
	 * Given a document template and its DocumentField's definitions, returns a list of DocumentInputFieldMapping
	 * considering as input columns at fixed positions at a specific sheet index.
	 */
	public static List<DocumentInputFieldMapping> getMappingsWithFixedColumnPositionsAndSheetIndex(DocumentTemplate docTemplate, int sheetIndex) {
		List<DocumentInputFieldMapping> mappings = new ArrayList<>(docTemplate.getFields().size());
		for (DocumentField field: docTemplate.getFields()) {
			mappings.add(new DocumentInputFieldMapping().withFieldName(field.getFieldName()).withColumnIndex(mappings.size()).withSheetIndex(sheetIndex));
		}
		return mappings;
	}

	/**
	 * Add sample {@link DocumentInput} for a given {@link DocumentTemplate}
	 *
	 * @param docTemplate
	 */
	private static void addInputsLalur(DocumentTemplate docTemplate) {

		if (docTemplate == null)
			return;

		DocumentInput input = new DocumentInput("CSV Lalur");
		input.setFormat(DocumentFormat.CSV);
		docTemplate.addInput(input);

		input.setFields(getMappingsWithFixedColumnPositions(docTemplate));
		input.setFieldsIdsMatchingTemplate(docTemplate);

		input = new DocumentInput("XLS Lalur");
		input.setFormat(DocumentFormat.XLS);
		docTemplate.addInput(input);

		input.setFields(getMappingsWithFixedColumnPositionsAndSheetIndex(docTemplate, 0));
		input.setFieldsIdsMatchingTemplate(docTemplate);

		input = new DocumentInput("PDF Lalur");
		input.setFormat(DocumentFormat.PDF);
		docTemplate.addInput(input);

		input.setFields(getMappingsWithFixedColumnPositions(docTemplate));
		input.setFieldsIdsMatchingTemplate(docTemplate);

		input = new DocumentInput("JSON Lalur");
		input.setFormat(DocumentFormat.JSON);
		docTemplate.addInput(input);

		input.setFields(getMappingsWithFixedColumnPositions(docTemplate));
		input.setFieldsIdsMatchingTemplate(docTemplate);

	}

	/**
	 * Add sample {@link DocumentInput} for a given {@link DocumentTemplate}
	 *
	 * @param docTemplate
	 */
	private static void addInputsOpeningBalance(DocumentTemplate docTemplate) {

		if (docTemplate == null)
			return;

		DocumentInput input = new DocumentInput("CSV Opening Balance");
		input.setFormat(DocumentFormat.CSV);
		docTemplate.addInput(input);

		input.setFields(getMappingsWithFixedColumnPositions(docTemplate));
		input.setFieldsIdsMatchingTemplate(docTemplate);

		input = new DocumentInput("XLS Opening Balance");
		input.setFormat(DocumentFormat.XLS);
		docTemplate.addInput(input);

		input.setFields(getMappingsWithFixedColumnPositionsAndSheetIndex(docTemplate, 0));
		input.setFieldsIdsMatchingTemplate(docTemplate);

		input = new DocumentInput("PDF Opening Balance");
		input.setFormat(DocumentFormat.PDF);
		docTemplate.addInput(input);

		input.setFields(getMappingsWithFixedColumnPositions(docTemplate));
		input.setFieldsIdsMatchingTemplate(docTemplate);

		input = new DocumentInput("DOC Opening Balance");
		input.setFormat(DocumentFormat.DOC);
		docTemplate.addInput(input);

		input.setFields(getMappingsWithFixedColumnPositions(docTemplate));
		input.setFieldsIdsMatchingTemplate(docTemplate);

	}

	/**
	 * Add sample {@link DocumentInput} for a given {@link DocumentTemplate}
	 *
	 * @param docTemplate
	 */
	private static void addInputsChartOfAccounts(DocumentTemplate docTemplate) {

		if (docTemplate == null)
			return;

		DocumentInput input = new DocumentInput("CSV Chart Of Accounts");
		input.setFormat(DocumentFormat.CSV);
		docTemplate.addInput(input);

		input.setFields(getMappingsWithFixedColumnPositions(docTemplate));
		input.setFieldsIdsMatchingTemplate(docTemplate);

		input = new DocumentInput("XLS Chart Of Accounts");
		input.setFormat(DocumentFormat.XLS);
		docTemplate.addInput(input);

		input.setFields(getMappingsWithFixedColumnPositionsAndSheetIndex(docTemplate, 0));
		input.setFieldsIdsMatchingTemplate(docTemplate);

		input = new DocumentInput("JSON Chart Of Accounts");
		input.setFormat(DocumentFormat.JSON);
		docTemplate.addInput(input);

		input.setFields(getMappingsWithFixedColumnPositions(docTemplate));
		input.setFieldsIdsMatchingTemplate(docTemplate);

		input = new DocumentInput("PDF Chart Of Accounts");
		input.setFormat(DocumentFormat.PDF);
		docTemplate.addInput(input);

		input = new DocumentInput("XML Chart Of Accounts");
		input.setFormat(DocumentFormat.XML);
		docTemplate.addInput(input);

		List<DocumentInputFieldMapping> mappings = Arrays.asList(
				new DocumentInputFieldMapping().withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.TaxPayerId.name())
						.withColumnNameExpression("TaxPayer"),
				new DocumentInputFieldMapping().withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.TaxYear.name())
						.withColumnNameExpression("TaxYear"),
				new DocumentInputFieldMapping().withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountCode.name())
						.withColumnNameExpression("Code"),
				new DocumentInputFieldMapping()
						.withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountCategory.name())
						.withColumnNameExpression("Category"),
				new DocumentInputFieldMapping()
						.withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountSubcategory.name())
						.withColumnNameExpression("Subcategory"),
				new DocumentInputFieldMapping().withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountName.name())
						.withColumnNameExpression("Name"),
				new DocumentInputFieldMapping()
						.withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountDescription.name())
						.withColumnNameExpression("Description"));

		input.setFields(mappings);
		input.setFieldsIdsMatchingTemplate(docTemplate);

		input = new DocumentInput("DOC Chart Of Accounts");
		input.setFormat(DocumentFormat.DOC);
		docTemplate.addInput(input);

		input.setFields(getMappingsWithFixedColumnPositions(docTemplate));
		input.setFieldsIdsMatchingTemplate(docTemplate);

	}

	/**
	 * Add sample {@link DocumentInput} for a given {@link DocumentTemplate}
	 *
	 * @param docTemplate
	 */
	private static void addInputsGeneralLedger(DocumentTemplate docTemplate) {

		if (docTemplate == null)
			return;

		DocumentInput input = new DocumentInput("CSV General Ledger");
		input.setFormat(DocumentFormat.CSV);
		docTemplate.addInput(input);

		input.setFields(getMappingsWithFixedColumnPositions(docTemplate));
		input.setFieldsIdsMatchingTemplate(docTemplate);

		input = new DocumentInput("XLS General Ledger");
		input.setFormat(DocumentFormat.XLS);
		docTemplate.addInput(input);

		input.setFields(getMappingsWithFixedColumnPositionsAndSheetIndex(docTemplate, 0));
		input.setFieldsIdsMatchingTemplate(docTemplate);

		input = new DocumentInput("PDF General Ledger");
		input.setFormat(DocumentFormat.PDF);
		docTemplate.addInput(input);

		input.setFields(getMappingsWithFixedColumnPositions(docTemplate));
		input.setFieldsIdsMatchingTemplate(docTemplate);

		input = new DocumentInput("DOC General Ledger");
		input.setFormat(DocumentFormat.DOC);
		docTemplate.addInput(input);

		input.setFields(getMappingsWithFixedColumnPositions(docTemplate));
		input.setFieldsIdsMatchingTemplate(docTemplate);
	}
	
	/**
	 * Add sample {@link DocumentInput} for a given {@link DocumentTemplate}
	 *
	 * @param docTemplate
	 */
	private static void addInputsIncomeStatement(DocumentTemplate docTemplate) {

		if (docTemplate == null)
			return;

		DocumentInput input = new DocumentInput("CSV Income Statement");
		input.setFormat(DocumentFormat.CSV);
		docTemplate.addInput(input);
		
		input.setFields(getMappingsWithFixedColumnPositions(docTemplate));
		input.setFieldsIdsMatchingTemplate(docTemplate);

		input = new DocumentInput("XLS Income Statement");
		input.setFormat(DocumentFormat.XLS);
		docTemplate.addInput(input);

		List<DocumentInputFieldMapping> mappings = new LinkedList<>();
		mappings.add(new DocumentInputFieldMapping().withFieldName(GeneralLedgerArchetype.FIELDS_NAMES.TaxPayerId.name())
						.withCellName("TaxPayer").withSheetIndex(0));
		mappings.add(new DocumentInputFieldMapping().withFieldName(GeneralLedgerArchetype.FIELDS_NAMES.TaxYear.name())
						.withCellName("Year").withSheetIndex(0));
		for (StatementComprehensiveIncome stmt: StatementComprehensiveIncome.values()) {
			DocumentInputFieldMapping field = new DocumentInputFieldMapping().withFieldName(CaseUtils.toCamelCase(stmt.name(), true, '_'));
			field.withCellName(field.getFieldName());
			field.withSheetIndex(0);
			mappings.add(field);
		}

		input.setFields(mappings);
		input.setFieldsIdsMatchingTemplate(docTemplate);

		input = new DocumentInput("PDF Income Statement");
		input.setFormat(DocumentFormat.PDF);
		docTemplate.addInput(input);

		mappings = new LinkedList<>();
		mappings.add(new DocumentInputFieldMapping().withFieldName(GeneralLedgerArchetype.FIELDS_NAMES.TaxPayerId.name())
						.withColumnNameExpression("TaxPayer"));
		mappings.add(new DocumentInputFieldMapping().withFieldName(GeneralLedgerArchetype.FIELDS_NAMES.TaxYear.name())
						.withColumnNameExpression("Year"));
		for (StatementComprehensiveIncome stmt: StatementComprehensiveIncome.values()) {
			DocumentInputFieldMapping field = new DocumentInputFieldMapping().withFieldName(CaseUtils.toCamelCase(stmt.name(), true, '_'));
			field.withColumnNameExpression(field.getFieldName());
			mappings.add(field);
		}

		input.setFields(mappings);
		input.setFieldsIdsMatchingTemplate(docTemplate);

		input = new DocumentInput("DOC Income Statement");
		input.setFormat(DocumentFormat.DOC);
		docTemplate.addInput(input);

		mappings = new LinkedList<>();
		mappings.add(new DocumentInputFieldMapping().withFieldName(GeneralLedgerArchetype.FIELDS_NAMES.TaxPayerId.name())
						.withColumnNameExpression("TaxPayer"));
		mappings.add(new DocumentInputFieldMapping().withFieldName(GeneralLedgerArchetype.FIELDS_NAMES.TaxYear.name())
						.withColumnNameExpression("Year"));
		for (StatementComprehensiveIncome stmt: StatementComprehensiveIncome.values()) {
			DocumentInputFieldMapping field = new DocumentInputFieldMapping().withFieldName(CaseUtils.toCamelCase(stmt.name(), true, '_'));
			field.withColumnNameExpression(field.getFieldName());
			mappings.add(field);
		}

		input.setFields(mappings);
		input.setFieldsIdsMatchingTemplate(docTemplate);
	}

	/**
	 * Add sample {@link DocumentInput} for a given {@link DocumentTemplate}
	 *
	 * @param docTemplate
	 */
	private static void addShareholding(DocumentTemplate docTemplate) {

		if (docTemplate == null)
			return;

		DocumentInput input = new DocumentInput("CSV Shareholding");
		input.setFormat(DocumentFormat.CSV);
		docTemplate.addInput(input);
		
		input.setFields(getMappingsWithFixedColumnPositions(docTemplate));
		input.setFieldsIdsMatchingTemplate(docTemplate);

		input = new DocumentInput("XLS Shareholding");
		input.setFormat(DocumentFormat.XLS);
		docTemplate.addInput(input);

		input.setFields(getMappingsWithFixedColumnPositionsAndSheetIndex(docTemplate, 0));
		input.setFieldsIdsMatchingTemplate(docTemplate);

		input = new DocumentInput("PDF Shareholding");
		input.setFormat(DocumentFormat.PDF);
		docTemplate.addInput(input);

		input.setFields(getMappingsWithFixedColumnPositions(docTemplate));
		input.setFieldsIdsMatchingTemplate(docTemplate);

		input = new DocumentInput("DOC Shareholding");
		input.setFormat(DocumentFormat.DOC);
		docTemplate.addInput(input);

		input.setFields(getMappingsWithFixedColumnPositions(docTemplate));
		input.setFieldsIdsMatchingTemplate(docTemplate);
	}
}
