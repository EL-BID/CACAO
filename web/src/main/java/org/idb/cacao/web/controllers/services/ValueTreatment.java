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
package org.idb.cacao.web.controllers.services;

import java.util.function.Function;

/**
 * Enumerates some options for 'decimal value treatment'
 * 
 * @author Gustavo Figueiredo
 *
 */
public enum ValueTreatment implements Function<Double,Double> {

	/**
	 * Does nothing (keeps value as is)
	 */
	NONE,
	
	/**
	 * round the decimal value to the closes integer
	 */
	ROUND,
	
	/**
	 * truncates the decimal value to an equal or lower integer
	 */
	TRUNCATE;
	
	/**
	 * Parse the text content into one of the valid ValueTreatment options
	 */
	public static ValueTreatment parse(String option) {
		if (option==null || option.trim().length()==0)
			return NONE;
		option = option.trim().toLowerCase();
		if (option.startsWith("\"") || option.startsWith("'"))
			option = option.substring(1).trim();
		if (option.startsWith("roun"))
			return ROUND;
		if (option.startsWith("trunc"))
			return TRUNCATE;
		return NONE;
	}
	
	/**
	 * Applies the decimal value treatment
	 */
	@Override
	public Double apply(Double v) {
		return (Double)apply((Number)v);
	}
	
	/**
	 * Applies the decimal value treatment
	 */
	public Number apply(Number v) {
		if (v==null)
			return v;
		if ((v instanceof Double) && (Double.isNaN((Double)v) || Double.isInfinite((Double)v)))
			return v;
		if ((v instanceof Float) && (Double.isNaN((Float)v) || Double.isInfinite((Float)v)))
			return v;
		switch (this) {
		case ROUND:
			return Double.valueOf(Math.round(((Number)v).doubleValue()));
		case TRUNCATE:
			return Math.floor(((Number)v).doubleValue());
		default: // NONE
			return v;
		}
	}
	
	/**
	 * Formats the decimal value according to localized information.
	 */
	public String format(Number v, org.thymeleaf.expression.Numbers numbersFormat) {
		if (v==null)
			return null;
		if ((v instanceof Double) && (Double.isNaN((Double)v) || Double.isInfinite((Double)v)))
			return null;
		if ((v instanceof Float) && (Double.isNaN((Float)v) || Double.isInfinite((Float)v)))
			return null;
		switch (this) {
		case ROUND:
			return numbersFormat.formatInteger(Double.valueOf(Math.round(((Number)v).doubleValue())), /*minIntegerDigits*/1, /*thousandsPointType*/"DEFAULT");
		case TRUNCATE:
			return numbersFormat.formatInteger(Math.floor(((Number)v).doubleValue()), /*minIntegerDigits*/1, /*thousandsPointType*/"DEFAULT");
		default: // NONE
			return numbersFormat.formatDecimal(v, /*minIntegerDigits*/1, /*thousandsPointType*/"DEFAULT", /*decimalDigits*/2, /*decimalPointType*/"DEFAULT");
		}		
	}
}
