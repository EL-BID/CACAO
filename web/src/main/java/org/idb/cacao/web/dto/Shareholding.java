/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.dto;

import org.idb.cacao.web.utils.FormatUtils;

public class Shareholding implements Comparable<Shareholding> {
	
	private String shareholderId;
	
	private String shareholderName;
	
	private String shareType;
	
	private String shareClass;
	
	private double shareAmount;
	
	private double sharePercentage;
	
	private double shareQuantity;
	
	private double equityMethodResult;
	
	public Shareholding(String[] values, double shareAmount, double sharePercentage, double shareQuantity, double equityMethodResult) {
		super();		
		this.shareholderId = values.length > 0 ? values[0] : null;
		this.shareholderName = values.length > 1 ? values[1] : null;		
		this.shareType = values.length > 2 ? values[2] : null;
		this.shareClass = values.length > 3 ? values[3] : null;
		this.shareAmount = shareAmount;		
		this.sharePercentage = sharePercentage;
		this.shareQuantity = shareQuantity;
		this.equityMethodResult = equityMethodResult;
	}

	public Shareholding() {
		super();
	}

	public String getShareholderId() {
		return shareholderId;
	}

	public void setShareholderId(String shareholderId) {
		this.shareholderId = shareholderId;
	}

	public String getShareholderName() {
		return shareholderName;
	}

	public void setShareholderName(String shareholderName) {
		this.shareholderName = shareholderName;
	}

	public double getShareAmount() {
		return shareAmount;
	}
	
	public String getShareAmountAsString() {
		return FormatUtils.getNumberFormat().format(shareAmount);
	}

	public void setShareAmount(double shareAmount) {
		this.shareAmount = shareAmount;
	}

	public String getShareType() {
		return shareType;
	}

	public void setShareType(String shareType) {
		this.shareType = shareType;
	}

	public String getShareClass() {
		return shareClass;
	}

	public void setShareClass(String shareClass) {
		this.shareClass = shareClass;
	}

	public double getSharePercentage() {
		return sharePercentage;
	}
	
	public String getSharePercentageAsString() {
		return FormatUtils.getPercentageFormat().format(sharePercentage/100);
	}

	public void setSharePercentage(double sharePercentage) {
		this.sharePercentage = sharePercentage;
	}	

	public double getShareQuantity() {
		return shareQuantity;
	}
	
	public String getShareQuantityAsString() {
		return FormatUtils.getNumberFormat().format(shareQuantity);
	}

	public void setShareQuantity(double shareQuantity) {
		this.shareQuantity = shareQuantity;
	}
	
	public double getEquityMethodResult() {
		return equityMethodResult;
	}

	public void setEquityMethodResult(double equityMethodResult) {
		this.equityMethodResult = equityMethodResult;
	}

	@Override
	public int compareTo(Shareholding other) {
		
		if (this==other)
			return 0;
		return ( shareAmount < other.shareAmount ) ? 1 : -1;
		
	}
	
}