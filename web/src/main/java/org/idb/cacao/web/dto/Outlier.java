/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
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
