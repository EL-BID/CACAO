/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.account.etl;

/**
 * Auxiliary class for storing temporarily information regarding book entries. Each counterpart will
 * have one 'PartialEntry' object.
 * 
 * @author Gustavo Figueiredo
 */
public class PartialEntry {
	
	/**
	 * The account that was credited or debited
	 */
	private final String account;
	
	/**
	 * The amount credited or debited
	 */
	private final double amount;
	
	public PartialEntry(String account, Number amount) {
		this.account = account;
		this.amount = Math.abs(amount.doubleValue());
	}

	public String getAccount() {
		return account;
	}

	public double getAmount() {
		return amount;
	}
	
	public String toString () {
		return String.format("%s: %d", account, amount);
	}
}