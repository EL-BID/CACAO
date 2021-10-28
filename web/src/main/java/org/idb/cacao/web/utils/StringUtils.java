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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
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

    /**
     * Timestamp format that conforms to ISO 8601
     */
    private static final ThreadLocal<SimpleDateFormat> tlDateFormat = new ThreadLocal<SimpleDateFormat>() {

		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		}
    	
    };
    
    /**
     * Timestamp format used in ElasticSearch
     */
    private static final ThreadLocal<SimpleDateFormat> tlDateFormatES = new ThreadLocal<SimpleDateFormat>() {

		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
		}
    	
    };

	/**
	 * Format dates
	 * E.g.: 01/01/2010
	 */
    private static final ThreadLocal<Pattern> flexible_date = new ThreadLocal<Pattern>() {
		@Override
		protected Pattern initialValue() {
			return Pattern.compile("^(\\d{1,2})[/\\\\\\-\\.]?(\\d{1,2})[/\\\\\\-\\.]?(\\d{2,4})$");
		}
	};

	/**
	 * E.g.: 01/Jan/2010
	 */
	public static final ThreadLocal<Pattern> flexible_date2 = new ThreadLocal<Pattern>() {
		@Override
		protected Pattern initialValue() {
			return Pattern.compile("^(\\d{1,2})[/\\\\\\-\\s]+([A-Z]{3,})[/\\\\\\-\\s]+(\\d{2,4})$", Pattern.CASE_INSENSITIVE);
		}
	};

	/**
	 * E.g.: 1º de jan de 2010
	 */
	public static final ThreadLocal<Pattern> flexible_date4 = new ThreadLocal<Pattern>() {
		@Override
		protected Pattern initialValue() {
			return Pattern.compile("^(\\d{1,2})º?\\s+de\\s+([A-Z]{3,})\\s+de\\s+(\\d{4})$", Pattern.CASE_INSENSITIVE);
		}
	};

	/**
	 * E.g.: 2010/Jan/01
	 */
	public static final ThreadLocal<Pattern> flexible_date6 = new ThreadLocal<Pattern>() {
		@Override
		protected Pattern initialValue() {
			return Pattern.compile("^(\\d{4})[/\\\\\\-\\s]+([A-Z]{3,})[/\\\\\\-\\s]+(\\d{1,2})$", Pattern.CASE_INSENSITIVE);
		}
	};

	/**
	 * E.g.: 2010-01-01
	 */
	public static final ThreadLocal<Pattern> flexible_date7 = new ThreadLocal<Pattern>() {
		@Override
		protected Pattern initialValue() {
			return Pattern.compile("^(\\d{4})[/\\\\\\-\\.](\\d{1,2})[/\\\\\\-\\.](\\d{1,2})$");
		}
	};

    public static boolean isOnlyNumbers(String value) {
    	return value!=null && pOnlyNumbers.matcher(value).find();
    }
    
    public static boolean isInteger(String value) {
    	return value!=null && pInteger.matcher(value).find();
    }
    
    public static boolean isDecimal(String value) {
    	return value!=null && pDecimal.matcher(value).find();
    }
    
    public static boolean isBoolean(String value) {
    	return value!=null && pBoolean.matcher(value).find();
    }

    /**
     * Return timestamp in a format that conforms to ISO 8601
     */
	public static String formatTimestamp(Date timestamp) {
		if (timestamp==null)
			return null;
		return tlDateFormat.get().format(timestamp);
	}
	
	/**
	 * Return indication that provided value looks like timestamp according to ISO 8601
	 */
	public static boolean isTimestamp(String value) {
		if (value==null)
			return false;
		try {
			tlDateFormat.get().parse(value);
			return true;
		}
		catch (Throwable ex) {
			return false;
		}
	}

	/**
	 * Parse the provided value as a timestamp according to ISO 8601
	 */
	public static Date parseTimestamp(String value) {
		if (value==null)
			return null;
		try {
			return tlDateFormat.get().parse(value);
		}
		catch (Throwable ex) {
			return null;
		}		
	}
	
    /**
     * Return timestamp in a format that conforms to Elasticsearch standard
     */
	public static String formatTimestampES(Date timestamp) {
		if (timestamp==null)
			return null;
		return tlDateFormatES.get().format(timestamp);
	}
	
	/**
	 * Return indication that provided value looks like timestamp according to Elasticsearch standard
	 */
	public static boolean isTimestampES(String value) {
		if (value==null)
			return false;
		try {
			tlDateFormatES.get().parse(value);
			return true;
		}
		catch (Throwable ex) {
			return false;
		}
	}

	/**
	 * Parse the provided value as a timestamp according to Elasticsearch standard
	 */
	public static Date parseTimestampES(String value) {
		if (value==null)
			return null;
		try {
			return tlDateFormatES.get().parse(value);
		}
		catch (Throwable ex) {
			return null;
		}		
	}

    /**
     * Parse a text as a date, considering different possible formats.
     */
	public static Date parseFlexibleDate(String date) {
		if (date == null)
			return null;
		date = date.trim();
		if (date.length() == 4 && date.matches("^\\d+$"))
			return null; // could be just an year
		Matcher m;
		m = flexible_date.get().matcher(date);
		if (!m.find()) {
			date = Normalizer.normalize(date, Normalizer.Form.NFD).replaceAll("[\\p{InCombiningDiacriticalMarks}]", "") // e.g.: Março => Marco
			.trim();
			m = flexible_date2.get().matcher(date);
			boolean found = m.find();
			if (!found) {
				m = flexible_date4.get().matcher(date);
				found = m.find();
			}

			int dia, ano;
			if (found) {
				dia = Integer.valueOf(m.group(1)).intValue();
				ano = Integer.valueOf(m.group(3)).intValue();
				if (ano < 1000)
					ano += 2000;
			} else {
				m = flexible_date6.get().matcher(date);
				found = m.find();
				if (found) {
					dia = Integer.valueOf(m.group(3)).intValue();
					ano = Integer.valueOf(m.group(1)).intValue();
				} else {
					m = flexible_date7.get().matcher(date);
					found = m.find();
					if (found) {
						dia = Integer.valueOf(m.group(3)).intValue();
						ano = Integer.valueOf(m.group(1)).intValue();
					} else {
						dia = 0;
						ano = 0;
					}
				}
			}

			if (found) {
				// Evita considerar algo que tenha apenas um caractere de
				// separação (ex: 01/2010 não pode ser confundido com 01/20/10).
				boolean sep1 = m.end(1) < m.start(2);
				boolean sep2 = m.end(2) < m.start(3);
				if (sep1 != sep2)
					return null;

				String mes_str = m.group(2).trim().toUpperCase();
				int mes = 0;
				if (Character.isDigit(mes_str.charAt(0))) {
					try {
						mes = Integer.parseInt(mes_str);
					}
					catch (Throwable e){ }
				}
				else if (mes_str.startsWith("JAN"))
					mes = 1;
				else if (mes_str.startsWith("FEV"))
					mes = 2;
				else if (mes_str.startsWith("MAR"))
					mes = 3;
				else if (mes_str.startsWith("ABR"))
					mes = 4;
				else if (mes_str.startsWith("MAI"))
					mes = 5;
				else if (mes_str.startsWith("JUN"))
					mes = 6;
				else if (mes_str.startsWith("JUL"))
					mes = 7;
				else if (mes_str.startsWith("AGO"))
					mes = 8;
				else if (mes_str.startsWith("SET"))
					mes = 9;
				else if (mes_str.startsWith("OUT"))
					mes = 10;
				else if (mes_str.startsWith("NOV"))
					mes = 11;
				else if (mes_str.startsWith("DEZ"))
					mes = 12;
				if (mes != 0) {
					return toDate(dia, mes, ano);
				}
			}
			return null;
		}
		// Evita considerar algo que tenha apenas um caractere de separação (ex:
		// 01/2010 não pode ser confundido com 01/20/10).
		boolean sep1 = m.end(1) < m.start(2);
		boolean sep2 = m.end(2) < m.start(3);
		if (sep1 != sep2)
			return null;
		int dia = Integer.valueOf(m.group(1)).intValue();
		int mes = Integer.valueOf(m.group(2)).intValue();
		if (mes == 0)
			return null; 
		int ano = Integer.valueOf(m.group(3)).intValue();
		if (ano < 1000)
			ano += 2000;
		return toDate(dia, mes, ano);
	}

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
		if ((value instanceof String) && isTimestamp((String)value))
			return text(messageSource, parseTimestamp((String)value)).replace(" 00:00:00", "");
		if ((value instanceof String) && isDecimal((String)value))
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
		return new SimpleDateFormat(messages.getMessage("timestamp_format", null, LocaleContextHolder.getLocale())).format(timestamp);
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
}
