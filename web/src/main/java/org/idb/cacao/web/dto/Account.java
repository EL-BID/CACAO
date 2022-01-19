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
import java.util.Map;

import org.apache.commons.collections4.comparators.ComparableComparator;

/**
 * A representation of a specific account with balance
 * 
 * @author Rivelino Patrício
 *
 */
public class Account implements Serializable, Comparable<Account> {

	private static final long serialVersionUID = 1L;

	private int level;

	private String categoryCode;
	
	private String category;

	private String subcategoryCode;
	
	private String subcategory;

	private String code;

	private String name;

	private double initialBalance;

	private double finalBalance;
	
	private String initialBalanceType;

	private String finalBalanceType;

	private double percentage;

	public Account() {
	}

	public Account(Map<String, Object> values) {
		System.out.println(values.toString());
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public String getCategoryCode() {
		return categoryCode;
	}

	public void setCategoryCode(String categoryCode) {
		this.categoryCode = categoryCode;
	}

	public String getSubcategoryCode() {
		return subcategoryCode;
	}

	public void setSubcategoryCode(String subcategoryCode) {
		this.subcategoryCode = subcategoryCode;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getSubcategory() {
		return subcategory;
	}

	public void setSubcategory(String subcategory) {
		this.subcategory = subcategory;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getInitialBalance() {
		return initialBalance;
	}

	public void setInitialBalance(double initialBalance) {
		this.initialBalance = initialBalance;
	}

	public double getFinalBalance() {
		return finalBalance;
	}

	public void setFinalBalance(double finalBalance) {
		this.finalBalance = finalBalance;
	}

	public double getPercentage() {
		return percentage;
	}

	public void setPercentage(double percentage) {
		this.percentage = percentage;
	}

	public String getInitialBalanceType() {
		return initialBalanceType;
	}

	public void setInitialBalanceType(String initialBalanceType) {
		this.initialBalanceType = initialBalanceType;
	}

	public String getFinalBalanceType() {
		return finalBalanceType;
	}

	public void setFinalBalanceType(String finalBalanceType) {
		this.finalBalanceType = finalBalanceType;
	}

	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(Account ref) {
		int comp = 0;
		
		if (this==ref) {
			return comp;
		}

		if (ref == null) {
			return +1;
		}
		
		if ( categoryCode == null )
			return -1;
		
		if ( ref.categoryCode == null )
			return +1;		
		
		comp = ComparableComparator.INSTANCE.compare(categoryCode, ref.categoryCode);		
		if (comp != 0)
			return comp;

		if ( subcategoryCode == null )
			return -1;
		
		if ( ref.subcategoryCode == null )
			return +1;
		
		comp = ComparableComparator.INSTANCE.compare(subcategoryCode, ref.subcategoryCode);
		if (comp != 0)
			return comp;
		
		if ( code == null )
			return -1;
		
		if ( ref.code == null )
			return +1;		

		comp = ComparableComparator.INSTANCE.compare(code, ref.code);
		if (comp != 0)
			return comp;
		
		return 0;
		
	}

}
