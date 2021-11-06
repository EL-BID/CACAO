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
 * This is the archetype for DocumentTemplate's related to CHART OF ACCOUNTS in ACCOUNTING
 * 
 * @author Gustavo Figueiredo
 *
 */
public class ChartOfAccountsArchetype implements TemplateArchetype {

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.TemplateArchetype#getName()
	 */
	@Override
	public String getName() {
		return "accounting.chart.accounts";
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
				.withDescription("Account code")
				.withMaxLength(256)
				.withRequired(true),
			new DocumentField()
				.withFieldName("ParentAccountCode")
				.withFieldType(FieldType.CHARACTER)
				.withFieldMapping(FieldMapping.ACCOUNT_CODE)
				.withDescription("Account code of the parent of this account according to the Chart of Accounts hierarchy")
				.withMaxLength(256)
				.withRequired(false),
			new DocumentField()
				.withFieldName("AccountName")
				.withFieldType(FieldType.CHARACTER)
				.withFieldMapping(FieldMapping.ACCOUNT_NAME)
				.withDescription("Account name for displaying alongside the account code in different financial reports")
				.withMaxLength(256)
				.withRequired(true),
			new DocumentField()
				.withFieldName("AccountDescription")
				.withFieldType(FieldType.CHARACTER)
				.withDescription("Account description")
				.withMaxLength(1024)
				.withRequired(false)
		);
	}

}
