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

import org.idb.cacao.account.elements.AccountCategory;
import org.idb.cacao.account.elements.AccountSubcategory;
import org.idb.cacao.api.templates.DomainEntry;
import org.idb.cacao.api.templates.DomainTable;

/**
 * Built-in domain tables related to accounting. These are 'in-memory replicas'. The application
 * must store domain tables at Elastic Search in order to be used (e.g. to be referenced by DocumentField).
 * 
 * @author Gustavo Figueiredo
 *
 */
public class AccountBuiltInDomainTables {
	
	/**
	 * Domain table for nature of account (i.e. DEBIT or CREDIT)
	 */
	public static DomainTable DEBIT_CREDIT = new DomainTable("Debit/Credit", /*version*/"1.0")
			.withEntries(
					new DomainEntry("D", "account.debit"),
					new DomainEntry("C", "account.credit"));
	
	/**
	 * Domain table for the category of accounts according to GAAP
	 */
	public static DomainTable ACCOUNT_CATEGORY_GAAP = DomainTable.fromEnum("Account Category GAAP", /*version*/"1.0", 
			/*enumeration with values*/AccountCategory.class, 
			/*getKey*/AccountCategory::getGaapNumber);

	/**
	 * Domain table for the category of accounts according to IFRS
	 */
	public static DomainTable ACCOUNT_CATEGORY_IFRS = DomainTable.fromEnum("Account Category IFRS", /*version*/"1.0", 
			/*enumeration with values*/AccountCategory.class, 
			/*getKey*/AccountCategory::getIfrsNumber);

	/**
	 * Domain table for the category of accounts according to GAAP
	 */
	public static DomainTable ACCOUNT_SUBCATEGORY_GAAP = DomainTable.fromEnum("Account Sub-Category GAAP", /*version*/"1.0", 
			/*enumeration with values*/AccountSubcategory.class, 
			/*getKey*/AccountSubcategory::getGaapNumber);

	/**
	 * Domain table for the category of accounts according to IFRS
	 */
	public static DomainTable ACCOUNT_SUBCATEGORY_IFRS = DomainTable.fromEnum("Account Sub-Category IFRS", /*version*/"1.0", 
			/*enumeration with values*/AccountSubcategory.class, 
			/*getKey*/AccountSubcategory::getIfrsNumber);
}
