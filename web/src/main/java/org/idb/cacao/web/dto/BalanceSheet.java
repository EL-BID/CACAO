/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
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
