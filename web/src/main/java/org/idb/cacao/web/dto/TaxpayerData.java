package org.idb.cacao.web.dto;

import java.util.LinkedList;
import java.util.List;

public class TaxpayerData {
	
	private transient String taxpayerId;
	
	private transient int year;
	
	private List<Shareholding> shareholdings;
	
	private List<Shareholding> shareholders;

	public String getTaxpayerId() {
		return taxpayerId;
	}

	public void setTaxpayerId(String taxpayerId) {
		this.taxpayerId = taxpayerId;
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public List<Shareholding> getShareholdings() {
		if ( shareholdings == null )
			shareholdings = new LinkedList<>();
		return shareholdings;
	}

	public void setShareholdings(List<Shareholding> shareholdings) {
		this.shareholdings = shareholdings;
	}
	
	public void addShareholding(Shareholding shareholding) {
		getShareholdings().add(shareholding);
	}

	public List<Shareholding> getShareholders() {
		return shareholders;
	}

	public void setShareholders(List<Shareholding> shareholders) {
		this.shareholders = shareholders;
	}

}
