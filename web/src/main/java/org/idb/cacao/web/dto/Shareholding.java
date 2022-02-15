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
		return FormatUtils.numberFormat.format(shareAmount);
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
		return FormatUtils.percentageFormat.format(sharePercentage/100);
	}

	public void setSharePercentage(double sharePercentage) {
		this.sharePercentage = sharePercentage;
	}	

	public double getShareQuantity() {
		return shareQuantity;
	}
	
	public String getShareQuantityAsString() {
		return FormatUtils.numberFormat.format(shareQuantity);
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