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

public class Outlier implements Cloneable, Comparable<Outlier> {
	
	private String statementName;

	private String order;
	
	private String taxpayerId;
	
	private String taxpayerName;
	
	private double value;

	public Outlier() {
		super();
	}
	
	public Outlier(String taxpayerId, String taxpayerName, String statementName, String order, double value) {
		super();
		this.taxpayerId = taxpayerId;
		this.taxpayerName = taxpayerName;
		this.statementName = statementName;
		this.order = order;
		this.value = value;
	}
	
	public Outlier(Object[] values, AnalysisItem item, double value) {
		super();
		this.taxpayerId = values.length > 0 ? (String)values[0] : "";
		this.taxpayerName = values.length > 1 ? (String)values[1] : "";
		this.statementName = item.getStatementName();
		this.order = item.getStatementOrder();
		this.value = value;
		item.addOutlier(this);
	}	

	public Outlier(Outlier other) {
		this.taxpayerId = other.taxpayerId;
		this.taxpayerName = other.taxpayerName;
		this.statementName = other.statementName;
		this.order = other.order;
		this.value = other.value;
	}
	
	public String getStatementName() {
		return statementName;
	}

	public void setStatementName(String statementName) {
		this.statementName = statementName;
	}

	public String getOrder() {
		return order;
	}

	public void setOrder(String order) {
		this.order = order;
	}

	public String getTaxpayerId() {
		return taxpayerId;
	}

	public void setTaxpayerId(String taxpayerId) {
		this.taxpayerId = taxpayerId;
	}

	public String getTaxpayerName() {
		return taxpayerName;
	}

	public void setTaxpayerName(String taxpayerName) {
		this.taxpayerName = taxpayerName;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	@Override
	public int compareTo(Outlier other) {
		if (this==other)
			return 0;
		if (order==null)
			return -1;
		if (other.order==null)
			return 1;
		
		int comp = order.compareToIgnoreCase(other.order);
		if (comp==0) //Same order
			return ( value < other.value ) ? 1 : -1;
		else
			return comp;
	}

}
