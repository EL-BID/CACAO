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
package org.idb.cacao.web.utils;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Some static formatters for numbers
 * 
 * @author Rivelino Patrício
 * 
 * @since 11/02/2022
 * 
 */
public class FormatUtils {

	private static Locale locale = LocaleContextHolder.getLocale();
	
	/**
	 * General purpose formatter for numbers with 2 fraction digits
	 */
	private static NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
	
	/**
	 * General purpose formatter for numbers with no fraction digits
	 */
	private static NumberFormat quantityFormat = NumberFormat.getNumberInstance(locale);
	
	/**
	 * General purpose formatter for numbers that represents a percentage value
	 */
	private static NumberFormat percentageFormat = NumberFormat.getPercentInstance(locale);
	
	static { 
		numberFormat.setMaximumFractionDigits(2);
		numberFormat.setMinimumFractionDigits(2);
		numberFormat.setRoundingMode(RoundingMode.CEILING);
		numberFormat.setGroupingUsed(true);
		quantityFormat.setMaximumFractionDigits(0);
		quantityFormat.setMinimumFractionDigits(0);
		quantityFormat.setRoundingMode(RoundingMode.CEILING);
		quantityFormat.setGroupingUsed(true);
		percentageFormat.setMaximumFractionDigits(2);
		percentageFormat.setMinimumFractionDigits(2);
		percentageFormat.setRoundingMode(RoundingMode.CEILING);
		percentageFormat.setGroupingUsed(true);
	}
	
	public static NumberFormat getNumberFormat() {
		return FormatUtils.numberFormat;
	}
	
	public static NumberFormat getQuantityFormat() {
		return FormatUtils.quantityFormat;
	}
	
	public static NumberFormat getPercentageFormat() {
		return FormatUtils.percentageFormat;
	}
	
}
