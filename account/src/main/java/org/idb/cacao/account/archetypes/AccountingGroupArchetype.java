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
import org.idb.cacao.api.errors.GeneralException;
import org.idb.cacao.api.templates.CustomDataGenerator;
import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.TemplateArchetype;

/**
 * Base class for other accounting archetypes that are part of the same 'Accounting' group.
 * 
 * @author Gustavo Figueiredo
 *
 */
public abstract class AccountingGroupArchetype implements TemplateArchetype {

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
	 * @see org.idb.cacao.api.templates.TemplateArchetype#getSuggestedGroup()
	 */
	@Override
	public String getSuggestedGroup() {
		return "Accounting";
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
			long records) throws GeneralException {
		if (hasCustomGenerator(template, format))
			return new AccountDataGenerator(template, format, seed, records);
		else
			return null;
	}

}
