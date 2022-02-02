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
import java.util.Optional;
import java.util.Stack;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import static org.idb.cacao.account.elements.DebitCredit.*;

/**
 * This is a very simplified version of a Statement of Comprehensive Income<BR>
 * <BR>
 * Each entry may be either an information calculated from the General Ledger (in this
 * case there will be a corresponding 'AccountSubcategory') or calculated from the
 * previous entries (in this case there will be a corresponding 'formula').<BR>
 * <BR>
 * Each 'formula' may refer to previous entries (in the same order the enum constants are defined).
 * For example, the formula at ordinal #3 may refer to the constants at ordinals #0, #1 and #2.<BR>
 * <BR>
 * The 'formula' should be a very simple combination of the operators '+' and '-' with the corresponding
 * constant names.<BR>
 * <BR>
 * 
 * @author Gustavo Figueiredo
 *
 */
public enum StatementComprehensiveIncome {
	
	REVENUE_NET(AccountSubcategory.REVENUE_NET, C, /*absolute*/true),
	EXPENSE_COST(AccountSubcategory.EXPENSE_COST, D, /*absolute*/true),
	GROSS_PROFIT("account.gross.profit", "REVENUE_NET - EXPENSE_COST", /*absolute*/false),
	EXPENSE_ADMIN(AccountSubcategory.EXPENSE_ADMIN, D, /*absolute*/true),
	EXPENSE_OPERATING(AccountSubcategory.EXPENSE_OPERATING, D, /*absolute*/true),
	EXPENSE_OPERATING_OTHER(AccountSubcategory.EXPENSE_OPERATING_OTHER, D, /*absolute*/true),
	TOTAL_OPERATING_EXPENSES("account.total.operating.expenses", "EXPENSE_ADMIN + EXPENSE_OPERATING + EXPENSE_OPERATING_OTHER", /*absolute*/true),
	OPERATING_INCOME("account.operating.income", "GROSS_PROFIT - TOTAL_OPERATING_EXPENSES", /*absolute*/false),
	REVENUE_NOP(AccountSubcategory.REVENUE_NOP, C, /*absolute*/true),
	EXPENSE_NOP(AccountSubcategory.EXPENSE_NOP, D, /*absolute*/true),
	GAINS_LOSSES(AccountSubcategory.GAINS_LOSSES, C, /*absolute*/false),
	INCOME_BEFORE_TAXES("account.income.before.taxes", "OPERATING_INCOME + REVENUE_NOP - EXPENSE_NOP + GAINS_LOSSES", /*absolute*/false),
	TAXES_OTHERS(AccountSubcategory.TAXES_OTHERS, D, /*absolute*/true),
	TAXES_INCOME(AccountSubcategory.TAXES_INCOME, D, /*absolute*/true),
	NET_INCOME("account.net.income", "INCOME_BEFORE_TAXES - TAXES_OTHERS - TAXES_INCOME", /*absolute*/false);

	private final String display;
	
	private final DebitCredit nature;
	
	private final AccountSubcategory subcategory;
	
	private final String formula;
	
	/**
	 * Tells if this value should be taken as an absolute value regardless of the sign declared by the taxpayer
	 */
	private final boolean absoluteValue;
	
	StatementComprehensiveIncome(String display, String formula, boolean absoluteValue) {
		this.display = display;
		this.nature = null;
		this.subcategory = null;
		this.formula = formula;
		this.absoluteValue = absoluteValue;
	}

	StatementComprehensiveIncome(AccountSubcategory subcategory, DebitCredit nature, boolean absoluteValue) {
		this.display = subcategory.toString();
		this.nature = nature;
		this.subcategory = subcategory;
		this.formula = null;
		this.absoluteValue = absoluteValue;
	}

	@Override
	public String toString() {
		return display;
	}

	public DebitCredit getNature() {
		return nature;
	}

	public AccountSubcategory getSubcategory() {
		return subcategory;
	}

	public String getFormula() {
		return formula;
	}

	/**
	 * Tells if this value should be taken as an absolute value regardless of the sign declared by the taxpayer
	 */
	public boolean isAbsoluteValue() {
		return absoluteValue;
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

	public Number computeFormula(Function<StatementComprehensiveIncome,Number> fetchData) {
		return computeFormula(formula, fetchData);
	}
	
	public static Number computeFormula(String formula, Function<StatementComprehensiveIncome,Number> fetchData) {
		if (formula==null || formula.trim().length()==0 || fetchData==null)
			return null;
		
		Pattern pOperator = Pattern.compile("[\\+\\-]");
		Matcher mOperator = pOperator.matcher(formula);
		int last_pos = 0;
		Stack<Number> values = new Stack<>();
		BinaryOperator<Number> op = null;
		while (mOperator.find()) {
			String term = formula.substring(last_pos, mOperator.start()).trim();
			StatementComprehensiveIncome term_parsed = StatementComprehensiveIncome.parse(term);
			Number term_value = (term_parsed==null) ? null : fetchData.apply(term_parsed);
			values.push(term_value);
			if (op!=null) {
				Number v2 = Optional.ofNullable((values.isEmpty()) ? null : values.pop()).orElse(0.0);
				Number v1 = Optional.ofNullable((values.isEmpty()) ? null : values.pop()).orElse(0.0);
				Number r = op.apply(v1, v2);
				values.push(r);
			}
			
			String opt = mOperator.group();			
			switch (opt) {
			case "+":
				op = (a,b)->a.doubleValue()+b.doubleValue();
				break;
			case "-":
				op = (a,b)->a.doubleValue()-b.doubleValue();
				break;
			default:
				throw new UnsupportedOperationException("Unsupported operator '"+opt+"' used in formula '"+formula+"'!");
			}
			
			last_pos = mOperator.end();
		}
		
		String term = formula.substring(last_pos).trim();
		if (term.length()>0) {
			StatementComprehensiveIncome term_parsed = StatementComprehensiveIncome.parse(term);
			Number term_value = (term_parsed==null) ? null : fetchData.apply(term_parsed);
			values.push(term_value);
			if (op!=null) {
				Number v2 = Optional.ofNullable((values.isEmpty()) ? null : values.pop()).orElse(0.0);
				Number v1 = Optional.ofNullable((values.isEmpty()) ? null : values.pop()).orElse(0.0);
				Number r = op.apply(v1, v2);
				values.push(r);
			}
		}

		return (values.isEmpty()) ? null : values.pop();
	}
}
