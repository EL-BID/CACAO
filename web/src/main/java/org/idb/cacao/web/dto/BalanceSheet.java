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
package org.idb.cacao.web.dto;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.LinkedList;
import java.util.List;

/**
 * A balance representation at a specific date
 * 
 * @author Rivelino Patrício
 * 
 */
public class BalanceSheet implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String taxPayerId;
	
	private String taxPayerName;
	
	private OffsetDateTime initialDate;
	
	private OffsetDateTime finalDate;
	
	private List<Account> accounts;

	public String getTaxPayerId() {
		return taxPayerId;
	}

	public void setTaxPayerId(String taxPayerId) {
		this.taxPayerId = taxPayerId;
	}

	public String getTaxPayerName() {
		return taxPayerName;
	}

	public void setTaxPayerName(String taxPayerName) {
		this.taxPayerName = taxPayerName;
	}

	public OffsetDateTime getInitialDate() {
		return initialDate;
	}

	public void setInitialDate(OffsetDateTime initialDate) {
		this.initialDate = initialDate;
	}

	public OffsetDateTime getFinalDate() {
		return finalDate;
	}

	public void setFinalDate(OffsetDateTime finalDate) {
		this.finalDate = finalDate;
	}

	public List<Account> getAccounts() {
		if ( accounts == null )
			accounts = new LinkedList<>();
		return accounts;
	}
	
	public void addAccount(Account account) {
		getAccounts().add(account);	
	}

	public void setAccounts(List<Account> accounts) {
		this.accounts = accounts;
	}
	
}
