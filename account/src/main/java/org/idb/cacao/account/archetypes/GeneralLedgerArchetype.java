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

import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.FieldMapping;
import org.idb.cacao.api.templates.FieldType;
import org.idb.cacao.api.templates.TemplateArchetype;

/**
 * This is the archetype for DocumentTemplate's related to GENERAL LEDGER in ACCOUNTING
 * 
 * @author Gustavo Figueiredo
 *
 */
public class GeneralLedgerArchetype implements TemplateArchetype {

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.TemplateArchetype#getName()
	 */
	@Override
	public String getName() {
		return "accounting.general.ledger";
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.TemplateArchetype#getRequiredFields()
	 */
	@Override
	public List<DocumentField> getRequiredFields() {
		return Arrays.asList(
			new DocumentField()
				.withFieldName("TaxPayerId")
				.withFieldType(FieldType.CHARACTER)
				.withFieldMapping(FieldMapping.TAXPAYER_ID)
				.withDescription("Taxpayer Identification Number")
				.withMaxLength(128)
				.withRequired(true),
			new DocumentField()
				.withFieldName("TaxYear")
				.withFieldType(FieldType.INTEGER)
				.withFieldMapping(FieldMapping.TAX_YEAR)
				.withDescription("Fiscal year of this financial reporting")
				.withRequired(true),
			new DocumentField()
				.withFieldName("Date")
				.withFieldType(FieldType.DATE)
				.withDescription("Date of the bookentry")
				.withRequired(true),
			new DocumentField()
				.withFieldName("AccountCode")
				.withFieldType(FieldType.CHARACTER)
				.withFieldMapping(FieldMapping.ACCOUNT_CODE)
				.withDescription("Account code (reference to Chart of Account)")
				.withMaxLength(256)
				.withRequired(true),
			new DocumentField()
				.withFieldName("EntryId")
				.withFieldType(FieldType.CHARACTER)
				.withDescription("Unique identification of bookeeping entry (shared among counterparts of the same double-entry bookeeping)")
				.withMaxLength(256)
				.withRequired(true),
			new DocumentField()
				.withFieldName("Description")
				.withFieldType(FieldType.CHARACTER)
				.withDescription("Description of this bookeeping entry")
				.withMaxLength(1024)
				.withRequired(false),
			new DocumentField()
				.withFieldName("Amount")
				.withFieldType(FieldType.DECIMAL)
				.withFieldMapping(FieldMapping.ACCOUNT_VALUE)
				.withDescription("The monetary amount of this bookeeping entry")
				.withRequired(true),
			new DocumentField()
				.withFieldName("DebitCredit")
				.withFieldType(FieldType.CHARACTER)
				.withFieldMapping(FieldMapping.ACCOUNT_DC)
				.withDescription("This is an indication of whether this entry is a debit or a credit to the account")
				.withMaxLength(32)
				.withRequired(true)
		);
	}

}
