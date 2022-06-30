/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.account.archetypes;

import java.util.Arrays;
import java.util.List;

import org.idb.cacao.account.etl.AccountingLoader;
import org.idb.cacao.account.validations.ChartOfAccountsValidations;
import org.idb.cacao.api.ETLContext;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DomainTable;
import org.idb.cacao.api.templates.FieldMapping;
import org.idb.cacao.api.templates.FieldType;

import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.*;

/**
 * This is the archetype for DocumentTemplate's related to CHART OF ACCOUNTS in ACCOUNTING
 * 
 * @author Gustavo Figueiredo
 *
 */
public class ChartOfAccountsArchetype extends AccountingGroupArchetype {
	
	public static final String NAME = "accounting.chart.accounts";

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
	 * @see org.idb.cacao.api.templates.TemplateArchetype#getBuiltInDomainTables()
	 */
	@Override
	public List<DomainTable> getBuiltInDomainTables() {
		return Arrays.asList( AccountBuiltInDomainTables.ACCOUNT_CATEGORY_GAAP,
				AccountBuiltInDomainTables.ACCOUNT_CATEGORY_IFRS,
				AccountBuiltInDomainTables.ACCOUNT_SUBCATEGORY_GAAP,
				AccountBuiltInDomainTables.ACCOUNT_SUBCATEGORY_IFRS );
	}

	public static enum FIELDS_NAMES {
		
		TaxPayerId,
		
		TaxYear,
		
		AccountCode,
		
		AccountCategory,
		
		AccountSubcategory,
		
		AccountName,
		
		AccountDescription
	};
	
	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.TemplateArchetype#getRequiredFields()
	 */
	@Override
	public List<DocumentField> getRequiredFields() {
		return Arrays.asList(
			new DocumentField()
				.withFieldName(TaxPayerId.name())
				.withFieldType(FieldType.CHARACTER)
				.withFieldMapping(FieldMapping.TAXPAYER_ID)
				.withDescription("Taxpayer Identification Number")
				.withMaxLength(128)
				.withRequired(true)
				.withFileUniqueness(true)
				.withPersonalData(true),
			new DocumentField()
				.withFieldName(TaxYear.name())
				.withFieldType(FieldType.INTEGER)
				.withFieldMapping(FieldMapping.TAX_YEAR)
				.withDescription("Fiscal year of this financial reporting")
				.withRequired(true)
				.withFileUniqueness(true),
			new DocumentField()
				.withFieldName(AccountCode.name())
				.withFieldType(FieldType.CHARACTER)
				.withDescription("Account code")
				.withMaxLength(256)
				.withRequired(true),
			new DocumentField()
				.withFieldName(AccountCategory.name())
				.withFieldType(FieldType.DOMAIN)
				.withDomainTableName(AccountBuiltInDomainTables.ACCOUNT_CATEGORY_IFRS.getName())
				.withDomainTableVersion(AccountBuiltInDomainTables.ACCOUNT_CATEGORY_IFRS.getVersion())
				.withDescription("Category of this account")
				.withMaxLength(256)
				.withRequired(true),
			new DocumentField()
				.withFieldName(AccountSubcategory.name())
				.withFieldType(FieldType.DOMAIN)
				.withDomainTableName(AccountBuiltInDomainTables.ACCOUNT_SUBCATEGORY_IFRS.getName())
				.withDomainTableVersion(AccountBuiltInDomainTables.ACCOUNT_SUBCATEGORY_IFRS.getVersion())
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
		);
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.TemplateArchetype#validateDocumentUploaded(org.idb.cacao.api.ValidationContext)
	 */
	@Override
	public boolean validateDocumentUploaded(ValidationContext context) {
		return ChartOfAccountsValidations.validateDocumentUploaded(context, context.getParsedContents());
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.TemplateArchetype#performETL(org.idb.cacao.api.ETLContext)
	 */
	@Override
	public boolean performETL(ETLContext context) {
		return AccountingLoader.performETL(context);
	}
	
}
