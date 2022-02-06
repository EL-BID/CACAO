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
package org.idb.cacao.account.archetypes;

import java.util.Arrays;
import java.util.List;

import org.idb.cacao.account.etl.AccountingLoader;
import org.idb.cacao.account.generator.AccountDataGenerator;
import org.idb.cacao.account.validations.GeneralLedgerValidations;
import org.idb.cacao.api.ETLContext;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.templates.CustomDataGenerator;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.DomainTable;
import org.idb.cacao.api.templates.FieldMapping;
import org.idb.cacao.api.templates.FieldType;
import org.idb.cacao.api.templates.TemplateArchetype;

import static org.idb.cacao.account.archetypes.GeneralLedgerArchetype.FIELDS_NAMES.*;

/**
 * This is the archetype for DocumentTemplate's related to GENERAL LEDGER in ACCOUNTING
 * 
 * @author Gustavo Figueiredo
 *
 */
public class GeneralLedgerArchetype implements TemplateArchetype {
	
	public static final String NAME = "accounting.general.ledger";

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
		return "Accounting";
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

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.TemplateArchetype#getRelatedPublishedDataIndices()
	 */
	@Override
	public List<String> getRelatedPublishedDataIndices() {
		return Arrays.asList(AccountingLoader.INDEX_PUBLISHED_ACCOUNTING_FLOW,
				AccountingLoader.INDEX_PUBLISHED_BALANCE_SHEET,
				AccountingLoader.INDEX_PUBLISHED_GENERAL_LEDGER,
				AccountingLoader.INDEX_PUBLISHED_COMPUTED_STATEMENT_INCOME,
				AccountingLoader.INDEX_PUBLISHED_CUSTOMERS,
				AccountingLoader.INDEX_PUBLISHED_SUPPLIERS);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.TemplateArchetype#hasCustomGenerator(org.idb.cacao.api.templates.DocumentTemplate, org.idb.cacao.api.templates.DocumentFormat)
	 */
	@Override
	public boolean hasCustomGenerator(DocumentTemplate template, DocumentFormat format) {
		return ChartOfAccountsArchetype.NAME.equalsIgnoreCase(template.getArchetype())
			|| OpeningBalanceArchetype.NAME.equalsIgnoreCase(template.getArchetype())
			|| GeneralLedgerArchetype.NAME.equalsIgnoreCase(template.getArchetype());
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.TemplateArchetype#getCustomGenerator(org.idb.cacao.api.templates.DocumentTemplate, org.idb.cacao.api.templates.DocumentFormat, long, long)
	 */
	@Override
	public CustomDataGenerator getCustomGenerator(DocumentTemplate template, DocumentFormat format, long seed,
			long records) throws Exception {
		if (hasCustomGenerator(template, format))
			return new AccountDataGenerator(template, format, seed, records);
		else
			return null;
	}

}
