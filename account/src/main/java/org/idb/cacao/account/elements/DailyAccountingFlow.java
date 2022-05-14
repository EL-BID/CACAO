/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
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
	
	public String toString() {
		return creditedAccountCode+"=["+amount+"]=>"+debitedAccountCode;
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
