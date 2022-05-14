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
 * This is a list of standard categories for accounts.<BR>
 * <BR>
 * Each account should be associated to one of these categories.<BR>
 * <BR>
 * This is the first level category in the hierarchy of a full chart of accounts
 * in accordance to both GAAP and IFRS.<BR>
 * <BR>
 * For GAAP, see: https://www.ifrs-gaap.com/chart-accounts<BR>
 * For IFRS, see: https://www.ifrs-gaap.com/ifrs-chart-accounts<BR>
 * <BR>
 * @author Gustavo Figueiredo
 *
 */
public enum AccountCategory {

	ASSET("account.category.asset", 				/*isDebitNature*/true,	/*GAAP*/"1", /*IFRS*/"1"),
	LIABILITY("account.category.liability", 		/*isDebitNature*/false,	/*GAAP*/"2", /*IFRS*/"3"),
	EQUITY("account.category.equity", 				/*isDebitNature*/false,	/*GAAP*/"3", /*IFRS*/"2"),
	REVENUE("account.category.revenue", 			/*isDebitNature*/false,	/*GAAP*/"4", /*IFRS*/"4"),
	EXPENSE("account.category.expense", 			/*isDebitNature*/true,	/*GAAP*/"5", /*IFRS*/"5"),
	INTERCOMPANY("account.category.intercompany", 	/*isDebitNature*/true,	/*GAAP*/"7", /*IFRS*/"7"),
	OTHER("account.category.other", 				/*isDebitNature*/true,	/*GAAP*/"X", /*IFRS*/"X");
	
	private final String display;
	private final String gaapNumber;
	private final String ifrsNumber;
	private final boolean isDebitNature;
	
	AccountCategory(String display, boolean isDebitNature, String gaapNumber, String ifrsNumber) {
		this.display = display;
		this.isDebitNature = isDebitNature;
		this.gaapNumber = gaapNumber;
		this.ifrsNumber = ifrsNumber;
	}

	@Override
	public String toString() {
		return display;
	}
	
	public boolean isDebitNature() {
		return isDebitNature;
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

	public static AccountCategory parse(String s) {
		if (s==null || s.trim().length()==0)
			return null;
		return Arrays.stream(values()).filter(t->t.name().equalsIgnoreCase(s)).findAny().orElse(null);
	}

	public static AccountCategory parse(String s, MessageSource messageSource) {
		if (s==null || s.trim().length()==0)
			return null;
		return Arrays.stream(values()).filter(t->t.name().equalsIgnoreCase(s)).findAny()
				.orElse(Arrays.stream(values()).filter(t->messageSource.getMessage(t.toString(),null,LocaleContextHolder.getLocale()).equalsIgnoreCase(s)).findAny()
						.orElse(null));
	}
	
}
