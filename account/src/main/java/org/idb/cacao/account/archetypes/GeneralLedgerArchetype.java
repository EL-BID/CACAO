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
import org.idb.cacao.account.validations.GeneralLedgerValidations;
import org.idb.cacao.api.ETLContext;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DomainTable;
import org.idb.cacao.api.templates.FieldMapping;
import org.idb.cacao.api.templates.FieldType;

import static org.idb.cacao.account.archetypes.GeneralLedgerArchetype.FIELDS_NAMES.*;

/**
 * This is the archetype for DocumentTemplate's related to GENERAL LEDGER in ACCOUNTING
 * 
 * @author Gustavo Figueiredo
 *
 */
public class GeneralLedgerArchetype extends AccountingGroupArchetype {
	
	public static final String NAME = "accounting.general.ledger";

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
		return Arrays.asList( AccountBuiltInDomainTables.DEBIT_CREDIT,
				AccountBuiltInDomainTables.ACCOUNT_SCI );
	}
	
	public static enum FIELDS_NAMES {
		
		TaxPayerId,
		
		TaxYear,
		
		Date,
		
		AccountCode,
		
		EntryId,
		
		Description,
		
		Amount,
		
		DebitCredit,
		
		CustomerSupplierId,
		
		CustomerSupplierName,
		
		InvoiceNumber;
		
	}

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
				.withFieldName(Date.name())
				.withFieldType(FieldType.DATE)
				.withDescription("Date of the bookentry")
				.withRequired(true),
			new DocumentField()
				.withFieldName(AccountCode.name())
				.withFieldType(FieldType.CHARACTER)
				.withDescription("Account code (reference to Chart of Account)")
				.withMaxLength(256)
				.withRequired(true),
			new DocumentField()
				.withFieldName(EntryId.name())
				.withFieldType(FieldType.CHARACTER)
				.withDescription("Unique identification of bookeeping entry (shared among counterparts of the same double-entry bookeeping)")
				.withMaxLength(256)
				.withRequired(true),
			new DocumentField()
				.withFieldName(Description.name())
				.withFieldType(FieldType.CHARACTER)
				.withDescription("Description of this bookeeping entry")
				.withMaxLength(1024)
				.withRequired(false),
			new DocumentField()
				.withFieldName(Amount.name())
				.withFieldType(FieldType.DECIMAL)
				.withDescription("The monetary amount of this bookeeping entry")
				.withRequired(true),
			new DocumentField()
				.withFieldName(DebitCredit.name())
				.withFieldType(FieldType.DOMAIN)
				.withDomainTableName(AccountBuiltInDomainTables.DEBIT_CREDIT.getName())
				.withDomainTableVersion(AccountBuiltInDomainTables.DEBIT_CREDIT.getVersion())
				.withDescription("This is an indication of whether this entry is a debit or a credit to the account")
				.withMaxLength(32)
				.withRequired(true),
			new DocumentField()
				.withFieldName(CustomerSupplierId.name())
				.withFieldType(FieldType.CHARACTER)
				.withFieldMapping(FieldMapping.TAXPAYER_ID)
				.withDescription("Customer/supplier identification number (whenever applicable)")
				.withMaxLength(128)
				.withRequired(false)
				.withPersonalData(true),
			new DocumentField()
				.withFieldName(CustomerSupplierName.name())
				.withFieldType(FieldType.CHARACTER)
				.withDescription("Customer/supplier name (whenever applicable)")
				.withMaxLength(1024)
				.withRequired(false)
				.withPersonalData(true),
			new DocumentField()
				.withFieldName(InvoiceNumber.name())
				.withFieldType(FieldType.CHARACTER)
				.withDescription("Invoice number for sales or purchases (whenever applicable)")
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
		return GeneralLedgerValidations.validateDocumentUploaded(context, context.getParsedContents());
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
