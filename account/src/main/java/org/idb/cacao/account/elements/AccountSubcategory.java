/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.account.elements;

import java.util.Arrays;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * This is a list of standard sub-categories for accounts.<BR>
 * <BR>
 * Each account should be associated to one of these sub-categories.<BR>
 * <BR>
 * This is the second level category in the hierarchy of a full chart of accounts
 * in accordance to both GAAP and IFRS.<BR>
 * <BR>
 * Actually just a few sub-categories are represented here. Any other account not related
 * to any of these sub-categories should be associated to general-purpose 'OTHER' sub-category.<BR>
 * <BR>
 * Each sub-category is also related to one category listed in AccountCategory.<BR>
 * <BR>
 * For GAAP, see: https://www.ifrs-gaap.com/chart-accounts<BR>
 * For IFRS, see: https://www.ifrs-gaap.com/ifrs-chart-accounts<BR>
 * <BR>
 * For a common Income Statement, the following subcategories may be considered in calculations:<BR>
 * GROSS PROFIT = REVENUE_NET - EXPENSE_COST<BR>
 * TOTAL OPERATING EXPENSES = EXPENSE_ADMIN + EXPENSE_OPERATING + EXPENSE_OPERATING_OTHER<BR>
 * OPERATING INCOME = GROSS PROFIT - TOTAL OPERATING EXPENSES<BR>
 * INCOME BEFORE TAXES = OPERATING INCOME + REVENUE_NOP - EXPENSE_NOP + GAINS_LOSSES<BR>
 * NET INCOME = INCOME BEFORE TAXES - TAXES_OTHERS -TAXES_INCOME
 * 
 * @author Gustavo Figueiredo
 *
 */
public enum AccountSubcategory {

	ASSET_CASH(AccountCategory.ASSET, "account.subcategory.asset.cash", 				/*GAAP*/"1.1", /*IFRS*/"1.10"),
	ASSET_FINANCIAL(AccountCategory.ASSET, "account.subcategory.asset.financial",		/*GAAP*/"1.1.2", /*IFRS*/"1.5"),
	ASSET_RECEIVABLE(AccountCategory.ASSET, "account.subcategory.asset.receivable", 	/*GAAP*/"1.2", /*IFRS*/"1.9"),
	ASSET_INVENTORY(AccountCategory.ASSET, "account.subcategory.asset.inventory", 		/*GAAP*/"1.3", /*IFRS*/"1.7"),
	ASSET_ACCRUAL(AccountCategory.ASSET, "account.subcategory.asset.accrual", 			/*GAAP*/"1.4", /*IFRS*/"1.8"),
	ASSET_PROPERTY(AccountCategory.ASSET, "account.subcategory.asset.property", 		/*GAAP*/"1.5", /*IFRS*/"1.1"),
	ASSET_INTANGIBLE(AccountCategory.ASSET, "account.subcategory.asset.intangible", 	/*GAAP*/"1.6", /*IFRS*/"1.4"),
	ASSET_GOODWILL(AccountCategory.ASSET, "account.subcategory.asset.goodwill", 		/*GAAP*/"1.7", /*IFRS*/"1.3"),
	ASSET_OTHER(AccountCategory.ASSET, "account.subcategory.asset.other", 				/*GAAP*/"1.X", /*IFRS*/"1.X"),
	
	LIABILITY_PAYABLE(AccountCategory.LIABILITY, "account.subcategory.liability.payable",		/*GAAP*/"2.1", /*IFRS*/"3.1"),
	LIABILITY_ACCRUAL(AccountCategory.LIABILITY, "account.subcategory.liability.accrual",		/*GAAP*/"2.2", /*IFRS*/"3.4"),
	LIABILITY_FINANCIAL(AccountCategory.LIABILITY, "account.subcategory.liability.financial",	/*GAAP*/"2.3", /*IFRS*/"3.3"),
	LIABILITY_PROVISION(AccountCategory.LIABILITY, "account.subcategory.liability.provision",	/*GAAP*/"2.4", /*IFRS*/"3.2"),
	LIABILITY_PROVISION_TAX(AccountCategory.LIABILITY, "account.subcategory.liability.provision.tax",	/*GAAP*/"2.4.1", /*IFRS*/"3.2.1"),
	LIABILITY_OTHER(AccountCategory.LIABILITY, "account.subcategory.liability.other", 			/*GAAP*/"2.X", /*IFRS*/"3.X"),
	
	EQUITY_OWNERS(AccountCategory.EQUITY, "account.subcategory.equity.owners",			/*GAAP*/"3.1", /*IFRS*/"2.1"),
	EQUITY_RETAINED(AccountCategory.EQUITY, "account.subcategory.equity.retained",		/*GAAP*/"3.2", /*IFRS*/"2.1.2"),
	EQUITY_ACCUMULATED(AccountCategory.EQUITY, "account.subcategory.equity.accumulated",/*GAAP*/"3.3", /*IFRS*/"2.3"),
	EQUITY_OTHER(AccountCategory.EQUITY, "account.subcategory.equity.other",			/*GAAP*/"3.4", /*IFRS*/"2.4"),
	EQUITY_MINORITY(AccountCategory.EQUITY, "account.subcategory.equity.minority",		/*GAAP*/"3.5", /*IFRS*/"2.6"),
	
	REVENUE_NET(AccountCategory.REVENUE, "account.subcategory.revenue.net",				 /*GAAP*/"4.1", /*IFRS*/"4.1"),
	REVENUE_ADJUSTMENT(AccountCategory.REVENUE, "account.subcategory.revenue.adjustment",/*GAAP*/"4.3", /*IFRS*/"4.3"),
	
	EXPENSE_COST(AccountCategory.EXPENSE, "account.subcategory.expense.cost",			/*GAAP*/"5.2.1", /*IFRS*/"5.2.1"),
	EXPENSE_ADMIN(AccountCategory.EXPENSE, "account.subcategory.expense.admin",			/*GAAP*/"5.2.2", /*IFRS*/"5.2.2"),
	EXPENSE_OPERATING(AccountCategory.EXPENSE, "account.subcategory.expense.operating",	/*GAAP*/"5.1", /*IFRS*/"5.1"),
	EXPENSE_OPERATING_OTHER(AccountCategory.EXPENSE, "account.subcategory.expense.operating.other",	/*GAAP*/"5.2", /*IFRS*/"5.2"),
	
	REVENUE_NOP(AccountCategory.REVENUE, "account.subcategory.revenue.nop",			/*GAAP*/"6.1.1", /*IFRS*/"6.1.1"),
	EXPENSE_NOP(AccountCategory.EXPENSE, "account.subcategory.expense.nop",			/*GAAP*/"6.1.2", /*IFRS*/"6.1.2"),
	
	GAINS_LOSSES(AccountCategory.REVENUE, "account.subcategory.gains.losses",	/*GAAP*/"6.2", /*IFRS*/"6.2"),
	
	TAXES_OTHERS(AccountCategory.EXPENSE, "account.subcategory.taxes.others",	/*GAAP*/"6.3", /*IFRS*/"6.3"),
	TAXES_INCOME(AccountCategory.EXPENSE, "account.subcategory.taxes.income",	/*GAAP*/"6.4", /*IFRS*/"6.4"),
	
	INTERCOMPANY_ASSET(AccountCategory.INTERCOMPANY, "account.subcategory.intercompany.asset",			 /*GAAP*/"7.1", /*IFRS*/"7.1"),
	INTERCOMPANY_LIABILITY(AccountCategory.INTERCOMPANY, "account.subcategory.intercompany.liability",	 /*GAAP*/"7.2", /*IFRS*/"7.2"),
	INTERCOMPANY_INCOME_EXPENSE(AccountCategory.INTERCOMPANY, "account.subcategory.intercompany.expense",/*GAAP*/"7.3", /*IFRS*/"7.3"),
	
	OTHER(AccountCategory.OTHER, "account.subcategory.other", /*GAAP*/"X.X", /*IFRS*/"X.X");

	private final AccountCategory category;
	private final String display;
	private final String gaapNumber;
	private final String ifrsNumber;

	AccountSubcategory(AccountCategory category, String display, String gaapNumber, String ifrsNumber) {
		this.category = category;
		this.display = display;
		this.gaapNumber = gaapNumber;
		this.ifrsNumber = ifrsNumber;
	}

	@Override
	public String toString() {
		return display;
	}
	
	public AccountCategory getCategory() {
		return category;
	}

	public String getGaapNumber() {
		return gaapNumber;
	}

	public String getIfrsNumber() {
		return ifrsNumber;
	}

	public String getNumber(AccountStandard standard) {
		if (standard==null)
			return null;
		switch (standard) {
		case IFRS:
			return ifrsNumber;
		case GAAP:
			return gaapNumber;
		default:
			return null;
		}
	}

	public static AccountSubcategory parse(String s) {
		if (s==null || s.trim().length()==0)
			return null;
		return Arrays.stream(values()).filter(t->t.name().equalsIgnoreCase(s)).findAny().orElse(null);
	}

	public static AccountSubcategory parse(String s, MessageSource messageSource) {
		if (s==null || s.trim().length()==0)
			return null;
		return Arrays.stream(values()).filter(t->t.name().equalsIgnoreCase(s)).findAny()
				.orElse(Arrays.stream(values()).filter(t->messageSource.getMessage(t.toString(),null,LocaleContextHolder.getLocale()).equalsIgnoreCase(s)).findAny()
						.orElse(null));
	}
}
