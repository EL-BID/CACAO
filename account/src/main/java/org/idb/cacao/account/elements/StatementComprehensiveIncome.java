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
package org.idb.cacao.account.elements;

import java.util.Arrays;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * This is a very simplified version of a Statement of Comprehensive Income
 * 
 * @author Gustavo Figueiredo
 *
 */
public enum StatementComprehensiveIncome {

	REVENUE_NET(AccountSubcategory.REVENUE_NET),
	EXPENSE_COST(AccountSubcategory.EXPENSE_COST),
	GROSS_PROFIT("account.gross.profit"),
	EXPENSE_ADMIN(AccountSubcategory.EXPENSE_ADMIN),
	EXPENSE_OPERATING(AccountSubcategory.EXPENSE_OPERATING),
	EXPENSE_OPERATING_OTHER(AccountSubcategory.EXPENSE_OPERATING_OTHER),
	OPERATING_INCOME("account.operating.income"),
	REVENUE_NOP(AccountSubcategory.REVENUE_NOP),
	EXPENSE_NOP(AccountSubcategory.EXPENSE_NOP),
	GAINS_LOSSES(AccountSubcategory.GAINS_LOSSES),
	INCOME_BEFORE_TAXES("account.income.before.taxes"),
	TAXES_OTHERS(AccountSubcategory.TAXES_OTHERS),
	TAXES_INCOME(AccountSubcategory.TAXES_INCOME),
	NET_INCOME("account.net.income");
	
	private final String display;
	
	StatementComprehensiveIncome(String display) {
		this.display = display;
	}

	StatementComprehensiveIncome(AccountSubcategory subcategory) {
		this.display = subcategory.toString();
	}

	@Override
	public String toString() {
		return display;
	}

	public static StatementComprehensiveIncome parse(String s) {
		if (s==null || s.trim().length()==0)
			return null;
		return Arrays.stream(values()).filter(t->t.name().equalsIgnoreCase(s)).findAny().orElse(null);
	}

	public static StatementComprehensiveIncome parse(String s, MessageSource messageSource) {
		if (s==null || s.trim().length()==0)
			return null;
		return Arrays.stream(values()).filter(t->t.name().equalsIgnoreCase(s)).findAny()
				.orElse(Arrays.stream(values()).filter(t->messageSource.getMessage(t.toString(),null,LocaleContextHolder.getLocale()).equalsIgnoreCase(s)).findAny()
						.orElse(null));
	}

}
