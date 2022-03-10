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

public class StatementIncomeItem implements Comparable<StatementIncomeItem> {
	
	private String statementOrder;
	
	private String statementName;
	
	private double calculatedValue;
	
	private double declaredValue;
	
	private double difference;	
	
	public StatementIncomeItem(String[] values) {
		this.statementOrder = values.length > 0 ? values[0] : "";
		this.statementName = values.length > 1 ? values[1] : "";
	}

	public StatementIncomeItem() {		
	}

	public String getStatementOrder() {
		return statementOrder;
	}

	public void setStatementOrder(String statementOrder) {
		this.statementOrder = statementOrder;
	}

	public String getStatementName() {
		return statementName;
	}

	public void setStatementName(String statementName) {
		this.statementName = statementName;
	}	

	public double getCalculatedValue() {
		return calculatedValue;
	}

	public void setCalculatedValue(double calculatedValue) {
		this.calculatedValue = calculatedValue;
	}

	public double getDeclaredValue() {
		return declaredValue;
	}

	public void setDeclaredValue(double declaredValue) {
		this.declaredValue = declaredValue;
	}

	public double getDifference() {
		return difference;
	}

	public void setDifference(double difference) {
		this.difference = difference;
	}

	@Override
	public String toString() {
		return "StatementIncomeItem [statementName=" + statementName + ", declaredValue=" + declaredValue + ", calculatedValue=" + calculatedValue + "]";
	}

	@Override
	public int compareTo(StatementIncomeItem other) {
		if (this==other)
			return 0;
		if (statementOrder==null)
			return -1;
		if (other.statementOrder==null)
			return 1;
		
		return statementOrder.compareToIgnoreCase(other.statementOrder);
	}
	
}
