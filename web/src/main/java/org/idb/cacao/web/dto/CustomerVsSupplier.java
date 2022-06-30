/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.dto;

import java.time.YearMonth;
import java.util.Map;
import java.util.Objects;

public class CustomerVsSupplier implements Comparable<CustomerVsSupplier> {
	
	private YearMonth month;
	
	private String customerId;
	
	private String customerName;
	
	private double customerValue;
	
	private double supplierValue;
	
	private double difference;
	
	/**
	 * Constructos for customers values
	 * @param values
	 */
	public CustomerVsSupplier(Map<String, Object> values, String type) {		
		this.month = YearMonth.of(Integer.valueOf(values.getOrDefault("year","0").toString()), 
				Integer.valueOf(values.getOrDefault("month_number","0").toString()));
		this.customerId = values.getOrDefault(type + "_id.keyword","").toString();
		this.customerName = values.getOrDefault(type + "_name.keyword","").toString();
		if ( "customer".equalsIgnoreCase(type) )
			this.customerValue = Double.parseDouble(values.getOrDefault("amount","0").toString());
		else
			this.supplierValue = Double.parseDouble(values.getOrDefault("amount","0").toString());
		difference = customerValue - supplierValue;
	}

	public CustomerVsSupplier(String year, String monthNumber, String customerId, String customerName, double customerValue,
			double supplierValue, double difference) {
		super();
		this.month = YearMonth.of(Integer.valueOf(year), Integer.valueOf(monthNumber));
		this.customerId = customerId;
		this.customerName = customerName;
		this.customerValue = customerValue;
		this.supplierValue = supplierValue;
		this.difference = difference;
	}

	public YearMonth getMonth() {
		return month;
	}

	public void setMonth(YearMonth month) {
		this.month = month;
	}

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	public String getCustomerName() {
		return customerName;
	}

	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}

	public double getCustomerValue() {
		return customerValue;
	}

	public void setCustomerValue(double customerValue) {
		this.customerValue = customerValue;
	}

	public double getSupplierValue() {
		return supplierValue;
	}

	public void setSupplierValue(double supplierValue) {
		this.supplierValue = supplierValue;
	}

	public double getDifference() {
		return difference;
	}

	public void setDifference(double difference) {
		this.difference = difference;
	}

	@Override
	public int compareTo(CustomerVsSupplier other) {
		if (this==other)
			return 0;
		
		if (month==null)
			return -1;
		
		if (other.month==null)
			return 1;
		
		int comp = month.compareTo(other.month);
		if ( comp != 0 )
			return comp;
		
		if (customerId==null)
			return -1;
		
		if (other.customerId==null)
			return 1;
		
		return difference < other.difference ? -1 : 1;
	}

	@Override
	public int hashCode() {
		return Objects.hash(customerId, customerName, month);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CustomerVsSupplier other = (CustomerVsSupplier) obj;
		return Objects.equals(customerId, other.customerId) && Objects.equals(customerName, other.customerName)
				&& Objects.equals(month, other.month);
	}	
}
