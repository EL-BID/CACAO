/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.account.archetypes;

import static org.idb.cacao.account.archetypes.ShareholdingArchetype.FIELDS_NAMES.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.idb.cacao.account.elements.StatementComprehensiveIncome;
import org.idb.cacao.account.etl.ShareholdingLoader;
import org.idb.cacao.account.generator.ShareholdingGenerator;
import org.idb.cacao.account.validations.ShareholdingValidations;
import org.idb.cacao.api.ETLContext;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.errors.GeneralException;
import org.idb.cacao.api.templates.CustomDataGenerator;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.DomainTable;
import org.idb.cacao.api.templates.FieldMapping;
import org.idb.cacao.api.templates.FieldType;
import org.idb.cacao.api.templates.TemplateArchetype;

/**
 * This is the archetype for DocumentTemplate's related to SHAREHOLDING STATEMENT
 * 
 * @author Gustavo Figueiredo
 *
 */
public class ShareholdingArchetype implements TemplateArchetype {

	public static final String NAME = "accounting.shareholding";

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.TemplateArchetype#getPluginName()
	 */
	@Override
	public String getPluginName() {
		return "CACAO Accounting Plug-in";
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.TemplateArchetype#getName()
	 */
	@Override
	public String getName() {
		return NAME;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.TemplateArchetype#getSuggestedGroup()
	 */
	@Override
	public String getSuggestedGroup() {
		return "Financial Report";
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.TemplateArchetype#getBuiltInDomainTables()
	 */
	@Override
	public List<DomainTable> getBuiltInDomainTables() {
		return Arrays.asList(AccountBuiltInDomainTables.SHARE_TYPE);
	}

	public static enum FIELDS_NAMES {
		
		TaxPayerId,
		
		TaxYear,
		
		ShareholdingName,
		
		ShareholdingId,
		
		ShareType,

		ShareClass,
		
		ShareAmount,
		
		ShareQuantity,
		
		SharePercentage,
		
		EquityMethodResult;
		
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.TemplateArchetype#getRequiredFields()
	 */
	@Override
	public List<DocumentField> getRequiredFields() {
		
		List<DocumentField> fields = new ArrayList<>(StatementComprehensiveIncome.values().length+2);
		
		fields.add(new DocumentField()
				.withFieldName(TaxPayerId.name())
				.withFieldType(FieldType.CHARACTER)
				.withFieldMapping(FieldMapping.TAXPAYER_ID)
				.withDescription("Taxpayer Identification Number")
				.withMaxLength(128)
				.withRequired(true)
				.withFileUniqueness(true)
				.withPersonalData(true));
		
		fields.add(new DocumentField()
				.withFieldName(TaxYear.name())
				.withFieldType(FieldType.INTEGER)
				.withFieldMapping(FieldMapping.TAX_YEAR)
				.withDescription("Fiscal year of this financial reporting")
				.withRequired(true)
				.withFileUniqueness(true));

		fields.add(new DocumentField()
				.withFieldName(ShareholdingName.name())
				.withFieldType(FieldType.CHARACTER)
				.withDescription("Shareholding's name")
				.withMaxLength(1024)
				.withPersonalData(true));

		fields.add(new DocumentField()
				.withFieldName(ShareholdingId.name())
				.withFieldType(FieldType.CHARACTER)
				.withFieldMapping(FieldMapping.TAXPAYER_ID)
				.withDescription("Shareholding's Identification Number")
				.withMaxLength(128)
				.withPersonalData(true));

		fields.add(new DocumentField()
				.withFieldName(ShareType.name())
				.withFieldType(FieldType.DOMAIN)
				.withDomainTableName(AccountBuiltInDomainTables.SHARE_TYPE.getName())
				.withDomainTableVersion(AccountBuiltInDomainTables.SHARE_TYPE.getVersion())
				.withDescription("Share type")
				.withRequired(true));

		fields.add(new DocumentField()
				.withFieldName(ShareClass.name())
				.withFieldType(FieldType.CHARACTER)
				.withMaxLength(128)
				.withDescription("Class of share")
				.withRequired(false));
		
		fields.add(new DocumentField()
				.withFieldName(ShareAmount.name())
				.withFieldType(FieldType.DECIMAL)
				.withDescription("Total amount of shares held")
				.withRequired(false));

		fields.add(new DocumentField()
				.withFieldName(ShareQuantity.name())
				.withFieldType(FieldType.DECIMAL)
				.withDescription("Number of shares held")
				.withRequired(true));

		fields.add(new DocumentField()
				.withFieldName(SharePercentage.name())
				.withFieldType(FieldType.DECIMAL)
				.withDescription("Percentage of shares held (100 = 100%)")
				.withRequired(true));
		
		fields.add(new DocumentField()
				.withFieldName(EquityMethodResult.name())
				.withFieldType(FieldType.DECIMAL)
				.withDescription("Amount of profit (positive) or loss (negative) in the reporting period according to equity method of accounting, whenever appliable")
				.withRequired(false));

		return fields;		
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.TemplateArchetype#validateDocumentUploaded(org.idb.cacao.api.ValidationContext)
	 */
	@Override
	public boolean validateDocumentUploaded(ValidationContext context) {
		return ShareholdingValidations.validateDocumentUploaded(context, context.getParsedContents());
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.TemplateArchetype#performETL(org.idb.cacao.api.ETLContext)
	 */
	@Override
	public boolean performETL(ETLContext context) {
		return ShareholdingLoader.performETL(context);
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.TemplateArchetype#getRelatedPublishedDataIndices()
	 */
	@Override
	public List<String> getRelatedPublishedDataIndices() {
		return Arrays.asList(ShareholdingLoader.INDEX_PUBLISHED_SHAREHOLDING);
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.TemplateArchetype#hasCustomGenerator(org.idb.cacao.api.templates.DocumentTemplate, org.idb.cacao.api.templates.DocumentFormat)
	 */
	@Override
	public boolean hasCustomGenerator(DocumentTemplate template, DocumentFormat format) {
		return ShareholdingArchetype.NAME.equalsIgnoreCase(template.getArchetype());
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.TemplateArchetype#getCustomGenerator(org.idb.cacao.api.templates.DocumentTemplate, org.idb.cacao.api.templates.DocumentFormat, long, long)
	 */
	@Override
	public CustomDataGenerator getCustomGenerator(DocumentTemplate template, DocumentFormat format, long seed,
			long records) throws GeneralException {
		if (hasCustomGenerator(template, format))
			return new ShareholdingGenerator(template, format, seed, records);
		else
			return null;
	}

}
