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
