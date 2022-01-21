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