/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.dto;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

	private double balance;
	
	private String balanceType;
	
	private double percentage;

	public Account() {
	}

	public Account(Map<String, Object> values) {
	}
	
	public Account(String[] values, double balance) {
		this.categoryCode = values.length > 0 ? values[0] : "";
		this.category = values.length > 1 ? values[1] : "";
		this.subcategoryCode = values.length > 2 ? values[2] : "";
		this.subcategory = values.length > 3 ? values[3] : "";
		this.code = values.length > 4 ? values[4] : "";
		this.name = values.length > 5 ? values[5] : "";
		this.balance = balance;		
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

	public double getBalance() {
		return balance;
	}

	public void setBalance(double balance) {
		this.balance = balance;
	}

	public String getBalanceType() {
		return balanceType;
	}

	public void setBalanceType(String balanceType) {
		this.balanceType = balanceType;
	}

	public double getPercentage() {
		return percentage;
	}

	public void setPercentage(double percentage) {
		this.percentage = percentage;
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

	public String getKey() {
		return categoryCode + "_" 
				+ ( subcategoryCode == null ? "0" : subcategoryCode ) 
				+ "_" + ( code == null ? "0" : code );
	}

	public Map<String, Object> getAccountData() {
		Map<String,Object> accountData = new HashMap<>();
		
		accountData.put("level",level);
		accountData.put("categoryCode",categoryCode);
		accountData.put("category",category);
		accountData.put("subcategoryCode",subcategoryCode);		
		accountData.put("subcategory",subcategory);
		accountData.put("code",code);
		accountData.put("name",name);		
		accountData.put("balanceType",balanceType);
		
		return accountData;
	}

	@Override
	public int hashCode() {
		return Objects.hash(categoryCode, code, subcategoryCode);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Account other = (Account) obj;
		return Objects.equals(categoryCode, other.categoryCode) && Objects.equals(code, other.code)
				&& Objects.equals(subcategoryCode, other.subcategoryCode);
	}

}
