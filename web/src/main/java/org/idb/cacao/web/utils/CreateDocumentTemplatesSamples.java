/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.text.CaseUtils;
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
		docTemplate.setName("Journal");
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
		int fieldIndex = 1;
		List<DocumentField> fields = new ArrayList<>();
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
			} catch (Exception ex) { }
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
	 * Given a document template and its DocumentField's definitions, returns a list of DocumentInputFieldMapping
	 * considering as input columns the column names
	 */
	public static List<DocumentInputFieldMapping> getMappingsWithColumnNameExpression(DocumentTemplate docTemplate) {
		List<DocumentInputFieldMapping> mappings = new ArrayList<>(docTemplate.getFields().size());
		for (DocumentField field: docTemplate.getFields()) {
			mappings.add(new DocumentInputFieldMapping().withFieldName(field.getFieldName()).withColumnNameExpression(field.getFieldName()));
		}
		return mappings;
	}
	
	/**
	 * Configure the DocumentTemplate with multiple DocumentInput related to common formats
	 */
	public static void addInputsCommonFormats(DocumentTemplate docTemplate, String inputSufix) {
		
		if (docTemplate == null)
			return;

		List<DocumentInput> inputs = docTemplate.getInputs();
		Set<DocumentFormat> formats = new HashSet<DocumentFormat>();

		if (inputs != null) {
			for (DocumentInput input: inputs) {
				formats.add(input.getFormat());
			}
		}
		

		if (!formats.contains(DocumentFormat.CSV)) {
			DocumentInput input = new DocumentInput("CSV "+inputSufix);
			input.setFormat(DocumentFormat.CSV);
			docTemplate.addInput(input);
	
			input.setFields(getMappingsWithFixedColumnPositions(docTemplate));
			input.setFieldsIdsMatchingTemplate(docTemplate);
		}

		if (!formats.contains(DocumentFormat.XLS)) {
			DocumentInput input = new DocumentInput("XLS "+inputSufix);
			input.setFormat(DocumentFormat.XLS);
			docTemplate.addInput(input);
	
			input.setFields(getMappingsWithFixedColumnPositionsAndSheetIndex(docTemplate, 0));
			input.setFieldsIdsMatchingTemplate(docTemplate);
		}

		if (!formats.contains(DocumentFormat.JSON)) {
			DocumentInput input = new DocumentInput("JSON "+inputSufix);
			input.setFormat(DocumentFormat.JSON);
			docTemplate.addInput(input);
	
			input.setFields(getMappingsWithColumnNameExpression(docTemplate));
			input.setFieldsIdsMatchingTemplate(docTemplate);
		}

		if (!formats.contains(DocumentFormat.XML)) {
			DocumentInput input = new DocumentInput("XML "+inputSufix);
			input.setFormat(DocumentFormat.XML);
			docTemplate.addInput(input);
	
			input.setFields(getMappingsWithColumnNameExpression(docTemplate));
			input.setFieldsIdsMatchingTemplate(docTemplate);
		}

		if (!formats.contains(DocumentFormat.PDF)) {
			DocumentInput input = new DocumentInput("PDF "+inputSufix);
			input.setFormat(DocumentFormat.PDF);
			docTemplate.addInput(input);
	
			input.setFields(getMappingsWithFixedColumnPositions(docTemplate));
			input.setFieldsIdsMatchingTemplate(docTemplate);
		}

		if (!formats.contains(DocumentFormat.DOC)) {
			DocumentInput input = new DocumentInput("DOC "+inputSufix);
			input.setFormat(DocumentFormat.DOC);
			docTemplate.addInput(input);

			input.setFields(getMappingsWithFixedColumnPositions(docTemplate));
			input.setFieldsIdsMatchingTemplate(docTemplate);
		}

	}

	/**
	 * Add sample {@link DocumentInput} for a given {@link DocumentTemplate}
	 *
	 * @param docTemplate
	 */
	private static void addInputsOpeningBalance(DocumentTemplate docTemplate) {
		
		addInputsCommonFormats(docTemplate, "Opening Balance");

	}

	/**
	 * Add sample {@link DocumentInput} for a given {@link DocumentTemplate}
	 *
	 * @param docTemplate
	 */
	private static void addInputsChartOfAccounts(DocumentTemplate docTemplate) {

		if (docTemplate == null)
			return;

		DocumentInput input = new DocumentInput("XML Chart Of Accounts");
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

		addInputsCommonFormats(docTemplate, "Chart Of Accounts");

	}

	/**
	 * Add sample {@link DocumentInput} for a given {@link DocumentTemplate}
	 *
	 * @param docTemplate
	 */
	private static void addInputsGeneralLedger(DocumentTemplate docTemplate) {

		addInputsCommonFormats(docTemplate, "Journal");

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

		addInputsCommonFormats(docTemplate, "Shareholding");

	}
}
