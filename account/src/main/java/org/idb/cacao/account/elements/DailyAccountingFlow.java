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

import java.time.LocalDate;

import org.springframework.util.ObjectUtils;

/**
 * This element represents a 'Daily Accounting Flow'. It may be computed over
 * bookeeping entries from a General Ledger.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class DailyAccountingFlow {

	/**
	 * Special value we may use to represent 'many accounts' being debited and 'many accounts' being
	 * credit in case it's not possible to break it further into accounting flows with specific accounts.
	 */
	public static final String MANY_ACCOUNTS = "*";

	/**
	 * The day of measurement
	 */
    private LocalDate date;
    
    /**
     * The account code that was credited by this accounting flow
     */
    private String creditedAccountCode;
    
    /**
     * The account code that was debited by this accounting flow
     */
    private String debitedAccountCode;
    
    /**
     * The sum of amounts taken from bookeeping entries that credited the given
     * account and debited the given account in the given date.
     */
    private double amount;

	/**
	 * The day of measurement
	 */
	public LocalDate getDate() {
		return date;
	}

	/**
	 * The day of measurement
	 */
	public void setDate(LocalDate date) {
		this.date = date;
	}

    /**
     * The account code that was credited by this accounting flow
     */
	public String getCreditedAccountCode() {
		return creditedAccountCode;
	}

    /**
     * The account code that was credited by this accounting flow
     */
	public void setCreditedAccountCode(String creditedAccountCode) {
		this.creditedAccountCode = creditedAccountCode;
	}

	/**
	 * Indication that the 'credit account code' is not a specific account, but represents 'many undefined accounts'
	 */
	public boolean hasCreditedManyAccountCodes() {
		return MANY_ACCOUNTS.equals(creditedAccountCode);
	}

    /**
     * The account code that was debited by this accounting flow
     */
	public String getDebitedAccountCode() {
		return debitedAccountCode;
	}

    /**
     * The account code that was debited by this accounting flow
     */
	public void setDebitedAccountCode(String debitedAccountCode) {
		this.debitedAccountCode = debitedAccountCode;
	}
	
	/**
	 * Indication that the 'debit account code' is not a specific account, but represents 'many undefined accounts'
	 */
	public boolean hasDebitedManyAccountCodes() {
		return MANY_ACCOUNTS.equals(debitedAccountCode);
	}

    /**
     * The sum of amounts taken from bookeeping entries that credited the given
     * account and debited the given account in the given date.
     */
	public double getAmount() {
		return amount;
	}

    /**
     * The sum of amounts taken from bookeeping entries that credited the given
     * account and debited the given account in the given date.
     */
	public void setAmount(double amount) {
		this.amount = amount;
	}
	
	public void addAmount(double amount) {
		this.amount += amount;
	}
    
    public int hashCode() {
		int result = ObjectUtils.nullSafeHashCode(date);
		result = 31 * result + ObjectUtils.nullSafeHashCode(creditedAccountCode);
		result = 31 * result + ObjectUtils.nullSafeHashCode(debitedAccountCode);
		return result;
    }
    
    public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (!(o instanceof DailyAccountingFlow)) {
			return false;
		}

		DailyAccountingFlow other = (DailyAccountingFlow) o;

		if (!ObjectUtils.nullSafeEquals(date, other.date)) {
			return false;
		}

		if (!ObjectUtils.nullSafeEquals(creditedAccountCode, other.creditedAccountCode)) {
			return false;
		}

		return ObjectUtils.nullSafeEquals(debitedAccountCode, other.debitedAccountCode);
    }
}
