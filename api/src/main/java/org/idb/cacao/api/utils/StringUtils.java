/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.api.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.regex.Pattern;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Utility methods for String manipulation
 * 
 * @author Gustavo Figueiredo
 *
 */
public class StringUtils {

    public static final Pattern pOnlyNumbers = Pattern.compile("^\\d+$");
    public static final Pattern pInteger = Pattern.compile("^[+-]?\\s*\\d+$");
    public static final Pattern pDecimal = Pattern.compile("^[+-]?\\s*\\d+\\.\\d+$");
    public static final Pattern pBoolean = Pattern.compile("^(?>true|false)$",Pattern.CASE_INSENSITIVE);

	public static Date toDate(int day, int month, int year) {
		Calendar cal = Calendar.getInstance();
		cal.set(year, month - 1, day, 0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}

	/**
	 * Presents memory information formatted as String
	 */
	public static String formatMemory(Long amount, NumberFormat format) {
		if (amount==null)
			return null;
		if (amount<1024)
			return format.format(amount)+" bytes";
		double kbytes = ((double)amount)/1024.0;
		if (kbytes<1024)
			return format.format(kbytes)+" Kilobytes";
		double mbytes = ((double)kbytes)/1024.0;
		if (mbytes<1024)
			return format.format(mbytes)+" Megabytes";
		double gbytes = ((double)mbytes)/1024.0;
		return format.format(gbytes)+" Gigabytes";
	}

	/**
	 * Given a field value indexed in Elastic Search, returns the same or another value to be presented to user
	 */
	public static String formatValue(MessageSource messageSource, Object value) {
		if (value==null)
			return "";
		if (((value instanceof String) && "true".equalsIgnoreCase((String)value)) || Boolean.TRUE.equals(value))
			return messageSource.getMessage("yes", null, LocaleContextHolder.getLocale());
		if (((value instanceof String) && "false".equalsIgnoreCase((String)value)) || Boolean.FALSE.equals(value))
			return messageSource.getMessage("no", null, LocaleContextHolder.getLocale());
		if ((value instanceof String) && ParserUtils.isTimestamp((String)value))
			return text(messageSource, ParserUtils.parseTimestamp((String)value)).replace(" 00:00:00", "");
		if ((value instanceof String) && ParserUtils.isDecimal((String)value))
			return text(messageSource, Double.parseDouble((String)value));
		if (value instanceof Date)
			return text(messageSource, (Date)value).replace(" 00:00:00", "");
		if (value instanceof Double)
			return text(messageSource, (Double)value);
		if (value instanceof Float)
			return text(messageSource, (Float)value);
		return String.valueOf(value);
	}

	/**
	 * Translates a message according to the language configured for current user session.
	 */
	public static String text(MessageSource messages, String key, Object... arguments) {
		return messages.getMessage(key, arguments, LocaleContextHolder.getLocale());
	}

	/**
	 * Formats a date/time according to the regional defaults
	 */
	public static String text(MessageSource messages, Date timestamp) {
		return new SimpleDateFormat(messages.getMessage("timestamp.format", null, LocaleContextHolder.getLocale())).format(timestamp);
	}
	
	/**
	 * Formats a decimal number according to the regional defaults
	 */
	public static String text(MessageSource messages, Number number) {
		DecimalFormatSymbols sym = new DecimalFormatSymbols();
		sym.setDecimalSeparator(messages.getMessage("decimal_char", null, LocaleContextHolder.getLocale()).charAt(0));
		sym.setGroupingSeparator(messages.getMessage("decimal_grouping_separator", null, LocaleContextHolder.getLocale()).charAt(0));
		return new DecimalFormat("###,###.#############", sym).format(number.doubleValue());

	}
	
	/**
	 * Compares two objects safeguarding from null values
	 */
    public static <T> int compare(T a, T b, Comparator<? super T> c) {
    	if (a == b)
    		return 0;
    	else if (a == null)
    		return -1;
    	else if (b == null)
    		return 1;
    	else
    		return c.compare(a, b);
    }

	/**
	 * Compares two strings safeguarding from null values
	 */
    public static int compareCaseInsensitive(String a, String b) {
    	return compare(a, b, String.CASE_INSENSITIVE_ORDER);
    }
}
