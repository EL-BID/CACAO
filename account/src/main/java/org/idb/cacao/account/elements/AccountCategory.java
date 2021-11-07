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

	ASSET("account.category.asset", 				/*GAAP*/"1", /*IFRS*/"1"),
	LIABILITY("account.category.liability", 		/*GAAP*/"2", /*IFRS*/"3"),
	EQUITY("account.category.equity", 				/*GAAP*/"3", /*IFRS*/"2"),
	REVENUE("account.category.revenue", 			/*GAAP*/"4", /*IFRS*/"4"),
	EXPENSE("account.category.expense", 			/*GAAP*/"5", /*IFRS*/"5"),
	INTERCOMPANY("account.category.intercompany", 	/*GAAP*/"7", /*IFRS*/"7"),
	OTHER("account.category.other", 				/*GAAP*/"X", /*IFRS*/"X");
	
	private final String display;
	private final String gaapNumber;
	private final String ifrsNumber;
	
	AccountCategory(String display, String gaapNumber, String ifrsNumber) {
		this.display = display;
		this.gaapNumber = gaapNumber;
		this.ifrsNumber = ifrsNumber;
	}

	@Override
	public String toString() {
		return display;
	}
	
	public String getGaapNumber() {
		return gaapNumber;
	}

	public String getIfrsNumber() {
		return ifrsNumber;
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
