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
import org.idb.cacao.account.validations.OpeningBalanceValidations;
import org.idb.cacao.api.ETLContext;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DomainTable;
import org.idb.cacao.api.templates.FieldMapping;
import org.idb.cacao.api.templates.FieldType;
import org.idb.cacao.api.templates.TemplateArchetype;

import static org.idb.cacao.account.archetypes.OpeningBalanceArchetype.FIELDS_NAMES.*;

/**
 * This is the archetype for DocumentTemplate's related to OPENING BALANCE in ACCOUNTING
 * 
 * @author Gustavo Figueiredo
 *
 */
public class OpeningBalanceArchetype implements TemplateArchetype {
	
	public static final String NAME = "accounting.opening.balance";

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
		return Arrays.asList( AccountBuiltInDomainTables.DEBIT_CREDIT );
	}

	public static enum FIELDS_NAMES {
		
		TaxPayerId,
		
		TaxYear,
		
		InitialDate,
		
		AccountCode,
		
		InitialBalance,
		
		DebitCredit;
		
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
				.withRequired(true),
			new DocumentField()
				.withFieldName(TaxYear.name())
				.withFieldType(FieldType.INTEGER)
				.withFieldMapping(FieldMapping.TAX_YEAR)
				.withDescription("Fiscal year of this financial reporting")
				.withRequired(true),
			new DocumentField()
				.withFieldName(InitialDate.name())
				.withFieldType(FieldType.DATE)
				.withDescription("Date for this initial balance and this particular account")
				.withRequired(true),
			new DocumentField()
				.withFieldName(AccountCode.name())
				.withFieldType(FieldType.CHARACTER)
				.withDescription("Account code (reference to Chart of Account)")
				.withMaxLength(256)
				.withRequired(true),
			new DocumentField()
				.withFieldName(InitialBalance.name())
				.withFieldType(FieldType.DECIMAL)
				.withDescription("The monetary amount of this initial balance")
				.withRequired(true),
			new DocumentField()
				.withFieldName(DebitCredit.name())
				.withFieldType(FieldType.DOMAIN)
				.withDomainTableName(AccountBuiltInDomainTables.DEBIT_CREDIT.getName())
				.withDomainTableVersion(AccountBuiltInDomainTables.DEBIT_CREDIT.getVersion())
				.withDescription("This is an indication of whether this balance is debit or credit")
				.withMaxLength(32)
				.withRequired(true)
		);
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.TemplateArchetype#validateDocumentUploaded(org.idb.cacao.api.ValidationContext)
	 */
	@Override
	public boolean validateDocumentUploaded(ValidationContext context) {
		return OpeningBalanceValidations.validateDocumentUploaded(context, context.getParsedContents());
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
