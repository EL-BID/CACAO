/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
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
