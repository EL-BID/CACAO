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

import java.time.YearMonth;
import java.util.Map;

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

}
