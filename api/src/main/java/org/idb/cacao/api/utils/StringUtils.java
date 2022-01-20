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
package org.idb.cacao.api.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
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
     * Month format as in yyyy/MM
     */
    private static final ThreadLocal<SimpleDateFormat> tlDateFormatMonth = new ThreadLocal<SimpleDateFormat>() {

		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM");
		}
    	
    };    
    
    /**
     * Month format as in yyyy/MM
     */
    private static final ThreadLocal<DateTimeFormatter> tlDateTimeFormatMonth = new ThreadLocal<DateTimeFormatter>() {

		@Override
		protected DateTimeFormatter initialValue() {		
			return DateTimeFormatter.ofPattern("yyyy-MM");
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

    /**
     * Return timestamp in a format that conforms to ISO 8601
     */
	public static String formatTimestamp(Date timestamp) {
		if (timestamp==null)
			return null;
		return tlDateFormat.get().format(timestamp);
	}
	
    /**
     * Return a month in a format yyyy-MM
     */	
	public static String formatMonth(Date date) {
		if (date==null)
			return null;
		return tlDateFormatMonth.get().format(date);	
	}
	
    /**
     * Return a month in a format yyyy-MM
     */	
	public static String formatMonth(OffsetDateTime date) {
		if (date==null)
			return null;
		return date.format(tlDateTimeFormatMonth.get());	
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
			return text(messageSource, parseTimestamp((String)value)).replace(" 00:00:00", "");
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
}
