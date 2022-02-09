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
package org.idb.cacao.account.generator;

import java.util.Arrays;

import org.idb.cacao.account.elements.AccountCategory;
import org.idb.cacao.account.elements.AccountSubcategory;

/**
 * This is a 'sample' Chart of Accounts to be used when generating random data for testing purpose.
 * 
 * @author Gustavo Figueiredo
 *
 */
public enum SampleChartOfAccounts {
	
	CASH("1.1.1", "Cash", "Cash and Cash Equivalents", AccountCategory.ASSET, AccountSubcategory.ASSET_CASH),
	RECEIVABLE("1.2.1", "Accounts Receivable", "Accounts, Notes And Loans Receivable", AccountCategory.ASSET, AccountSubcategory.ASSET_RECEIVABLE),
	INVENTORY("1.3.1", "Inventory", "Merchandise in Inventory", AccountCategory.ASSET, AccountSubcategory.ASSET_INVENTORY),
	PAYABLE("2.1.1", "Accounts Payable", "Accounts and Trade Payables", AccountCategory.LIABILITY, AccountSubcategory.LIABILITY_PAYABLE),
	LOANS("2.3.2", "Loans", "Loans Payable", AccountCategory.LIABILITY, AccountSubcategory.LIABILITY_ACCRUAL),
	TAX_PROVISION("3.2.1.1","Tax Provision", "Provision for tax payment", AccountCategory.LIABILITY, AccountSubcategory.LIABILITY_PROVISION_TAX),
	STOCK("3.5.2", "Stock", "Subscribed Stock Receivables", AccountCategory.EQUITY, AccountSubcategory.EQUITY_OWNERS),
	REVENUE_GOODS("4.1.1", "Revenue Goods", "Revenue from selling Goods", AccountCategory.REVENUE, AccountSubcategory.REVENUE_NET),
	REVENUE_SERVICES("4.1.2", "Revenue Services", "Revenue from Services", AccountCategory.REVENUE, AccountSubcategory.REVENUE_NET),
	EMPLOYEE("5.1.2", "Employee", "Employee Benefits", AccountCategory.EXPENSE, AccountSubcategory.EXPENSE_ADMIN),
	SERVICES("5.1.3", "Services", "Expenses for Services", AccountCategory.EXPENSE, AccountSubcategory.EXPENSE_OPERATING),
	RENT("5.1.4", "Rent", "Rent, Depreciation, Amortization And Depletion", AccountCategory.EXPENSE, AccountSubcategory.EXPENSE_OPERATING),
	SALES_EXPENSES("5.2.1", "Sales Expenses", "Cost Of Sales", AccountCategory.EXPENSE, AccountSubcategory.EXPENSE_COST),
	ADMINISTRATIVE_EXPENSES("5.2.2", "Adminitrative Expenses", "Selling, General And Administrative", AccountCategory.EXPENSE, AccountSubcategory.EXPENSE_ADMIN),	
	OTHER_EXPENSES("6.1.2", "Other Expenses", "Other non-operating expenses", AccountCategory.OTHER, AccountSubcategory.OTHER);

	private final String accountCode;

	private final String accountName;

	private final String accountDescription;

	private final AccountCategory category;
	
	private final AccountSubcategory subcategory;
	
	SampleChartOfAccounts(String accountCode, String accountName, String accountDescription, AccountCategory category, AccountSubcategory subcategory) {
		this.accountCode = accountCode;
		this.accountName = accountName;
		this.accountDescription = accountDescription;
		this.category = category;
		this.subcategory = subcategory;
	}

	public String getAccountCode() {
		return accountCode;
	}

	public String getAccountName() {
		return accountName;
	}

	public String getAccountDescription() {
		return accountDescription;
	}

	public AccountCategory getCategory() {
		return category;
	}

	public AccountSubcategory getSubcategory() {
		return subcategory;
	}
	
	public static SampleChartOfAccounts[] assets() {
		return Arrays.stream(values()).filter(c->AccountCategory.ASSET.equals(c.getCategory())).toArray(SampleChartOfAccounts[]::new);
	}

	public static SampleChartOfAccounts[] liabilities() {
		return Arrays.stream(values()).filter(c->AccountCategory.LIABILITY.equals(c.getCategory())).toArray(SampleChartOfAccounts[]::new);		
	}
}
