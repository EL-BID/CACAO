/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
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
