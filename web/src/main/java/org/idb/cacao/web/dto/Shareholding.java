package org.idb.cacao.web.dto;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

import org.springframework.context.i18n.LocaleContextHolder;

public class Shareholding implements Comparable<Shareholding> {
	
	private String shareholderId;
	
	private String shareholderName;
	
	private String shareType;
	
	private String shareClass;
	
	private double shareAmount;
	
	private double sharePercentage;
	
	private double shareQuantity;
	
	private double equityMethodResult;
	
	private static Locale locale = LocaleContextHolder.getLocale();
	private static NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
	private static NumberFormat percentageFormat = NumberFormat.getPercentInstance(locale);
	
	static { 
		numberFormat.setMaximumFractionDigits(2);
		numberFormat.setMinimumFractionDigits(2);
		numberFormat.setRoundingMode(RoundingMode.CEILING);
		numberFormat.setGroupingUsed(true);
		percentageFormat.setMaximumFractionDigits(2);
		percentageFormat.setMinimumFractionDigits(2);
		percentageFormat.setRoundingMode(RoundingMode.CEILING);
		percentageFormat.setGroupingUsed(true);
	}
	
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
		return numberFormat.format(shareAmount);
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
		return percentageFormat.format(sharePercentage/100);
	}

	public void setSharePercentage(double sharePercentage) {
		this.sharePercentage = sharePercentage;
	}	

	public double getShareQuantity() {
		return shareQuantity;
	}
	
	public String getShareQuantityAsString() {
		return numberFormat.format(shareQuantity);
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