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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.comparators.ComparableComparator;

/**
 * A representation of an aggregate of accounting flow
 * 
 * @author Luis Kauer
 *
 */
public class AggregatedAccountingFlow implements Serializable {

	private static final long serialVersionUID = 1L;

	private String creditCategoryCode;
	
	private String creditCategoryName;

	private String creditSubcategoryCode;
	
	private String creditSubcategoryName;

	private String creditAccountCode;

	private String creditAccountName;
	
    private String debitCategoryCode;
	
	private String debitCategoryName;

	private String debitSubcategoryCode;
	
	private String debitSubcategoryName;

	private String debitAccountCode;

	private String debitAccountName;

	private double amount;
	

	public AggregatedAccountingFlow() {
	}


	public AggregatedAccountingFlow(String creditCategoryCode, String creditCategoryName, String creditSubcategoryCode,
			String creditSubcategoryName, String creditAccountCode, String creditAccountName, String debitCategoryCode,
			String debitCategoryName, String debitSubcategoryCode, String debitSubcategoryName, String debitAccountCode,
			String debitAccountName, double amount) {
		super();
		this.creditCategoryCode = creditCategoryCode;
		this.creditCategoryName = creditCategoryName;
		this.creditSubcategoryCode = creditSubcategoryCode;
		this.creditSubcategoryName = creditSubcategoryName;
		this.creditAccountCode = creditAccountCode;
		this.creditAccountName = creditAccountName;
		this.debitCategoryCode = debitCategoryCode;
		this.debitCategoryName = debitCategoryName;
		this.debitSubcategoryCode = debitSubcategoryCode;
		this.debitSubcategoryName = debitSubcategoryName;
		this.debitAccountCode = debitAccountCode;
		this.debitAccountName = debitAccountName;
		this.amount = amount;
	}

	public AggregatedAccountingFlow(String[] values, double amount) {
		this(values[0], values[1], values[2], values[3], values[4], values[5], values[6], values[7], values[8], values[9],
				values[10], values[11], amount);
	}


	public String getCreditCategoryCode() {
		return creditCategoryCode;
	}


	public void setCreditCategoryCode(String creditCategoryCode) {
		this.creditCategoryCode = creditCategoryCode;
	}


	public String getCreditCategoryName() {
		return creditCategoryName;
	}


	public void setCreditCategoryName(String creditCategoryName) {
		this.creditCategoryName = creditCategoryName;
	}


	public String getCreditSubcategoryCode() {
		return creditSubcategoryCode;
	}


	public void setCreditSubcategoryCode(String creditSubcategoryCode) {
		this.creditSubcategoryCode = creditSubcategoryCode;
	}


	public String getCreditSubcategoryName() {
		return creditSubcategoryName;
	}


	public void setCreditSubcategoryName(String creditSubcategoryName) {
		this.creditSubcategoryName = creditSubcategoryName;
	}


	public String getCreditAccountCode() {
		return creditAccountCode;
	}


	public void setCreditAccountCode(String creditAccountCode) {
		this.creditAccountCode = creditAccountCode;
	}


	public String getCreditAccountName() {
		return creditAccountName;
	}


	public void setCreditAccountName(String creditAccountName) {
		this.creditAccountName = creditAccountName;
	}


	public String getDebitCategoryCode() {
		return debitCategoryCode;
	}


	public void setDebitCategoryCode(String debitCategoryCode) {
		this.debitCategoryCode = debitCategoryCode;
	}


	public String getDebitCategoryName() {
		return debitCategoryName;
	}


	public void setDebitCategoryName(String debitCategoryName) {
		this.debitCategoryName = debitCategoryName;
	}


	public String getDebitSubcategoryCode() {
		return debitSubcategoryCode;
	}


	public void setDebitSubcategoryCode(String debitSubcategoryCode) {
		this.debitSubcategoryCode = debitSubcategoryCode;
	}


	public String getDebitSubcategoryName() {
		return debitSubcategoryName;
	}


	public void setDebitSubcategoryName(String debitSubcategoryName) {
		this.debitSubcategoryName = debitSubcategoryName;
	}


	public String getDebitAccountCode() {
		return debitAccountCode;
	}


	public void setDebitAccountCode(String debitAccountCode) {
		this.debitAccountCode = debitAccountCode;
	}


	public String getDebitAccountName() {
		return debitAccountName;
	}


	public void setDebitAccountName(String debitAccountName) {
		this.debitAccountName = debitAccountName;
	}


	public double getAmount() {
		return amount;
	}


	public void setAmount(double amount) {
		this.amount = amount;
	}


	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	
}
