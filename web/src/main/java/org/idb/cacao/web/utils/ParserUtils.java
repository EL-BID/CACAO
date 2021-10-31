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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.idb.cacao.api.Periodicity;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Utility methods for parsing contents of different types (e.g. timestamps, monetary values, etc.)
 * 
 * @author Gustavo Figueiredo
 *
 */
public class ParserUtils {

    public static final Pattern pHeadingSymbols = Pattern.compile("^[^A-zÀ-ú\\d\\(\\[\\+\\-]+");
    public static final Pattern pTrailingSymbols = Pattern.compile("[^A-zÀ-ú\\d\\)\\]]+$");
    public static final Pattern pDecimalZero = Pattern.compile("[\\.,]0$");
    public static final Pattern pOnlyNumbers = Pattern.compile("^\\d+$");
    public static final Pattern pInteger = Pattern.compile("^[+-]?\\s*\\d+$");
    public static final Pattern pDecimal = Pattern.compile("^[+-]?\\s*\\d+\\.\\d+$");
    public static final Pattern pDecimalWithComma = Pattern.compile("^[+-]?\\s*[\\d\\.]+\\,\\d+$");
    public static final Pattern pBoolean = Pattern.compile("^(?>true|false)$",Pattern.CASE_INSENSITIVE);
    public static final Pattern pMultipleSpaces = Pattern.compile("\\s{2,}");
    public static final Pattern pZeroes = Pattern.compile("^[+-]?\\s*0+(?>[\\.,]0*)?$");
    public static final Pattern pCientificNotation = Pattern.compile("^[+-]?\\d\\.\\d+E\\d+$",Pattern.CASE_INSENSITIVE);
    public static final Pattern pDMY = Pattern.compile("^(?>[0-2]\\d|30|31|\\d)[/\\-](?>0\\d|10|11|12|\\d)[/\\-]20\\d{2}$");
    public static final Pattern pMDY = Pattern.compile("^(?>0\\d|10|11|12|\\d)[/\\-](?>[0-2]\\d|30|31|\\d)[/\\-]20\\d{2}$");
    public static final Pattern pYMD = Pattern.compile("^(?>20\\d{2})[/\\-](?>0\\d|10|11|12|\\d)[/\\-](?>[0-2]\\d|30|31|\\d)$");
    
    /**
     * Maximum length for a field name to be considered as 'proper field name'
     */
    public static final int MAX_FIELD_NAME_LENGTH = 200;
    
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

    private static final ThreadLocal<DecimalFormat> tlDecimalFormat = new ThreadLocal<DecimalFormat>() {
  
		@Override
		protected DecimalFormat initialValue() {
			DecimalFormatSymbols sym = new DecimalFormatSymbols();
			sym.setDecimalSeparator('.');
			return new DecimalFormat("######.#############", sym);
		}

    };
    
    private static final ThreadLocal<DecimalFormat> tlDecimalCommaFormat = new ThreadLocal<DecimalFormat>() {
    	  
		@Override
		protected DecimalFormat initialValue() {
			DecimalFormatSymbols sym = new DecimalFormatSymbols();
			sym.setDecimalSeparator(',');
			return new DecimalFormat("######.#############", sym);
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

    public static String preformat(String value) {
    	if (value==null)
    		return null;
		String s = value.trim();
		s = pHeadingSymbols.matcher(s).replaceAll("");
		s = pTrailingSymbols.matcher(s).replaceAll("");
		if (pCientificNotation.matcher(value).find()) {
			s = formatDecimal(Double.valueOf(value));
		}
		s = pDecimalZero.matcher(s).replaceAll("");
		s = pMultipleSpaces.matcher(s).replaceAll(" ");
    	if ("null".equalsIgnoreCase(value))
    		return null;
		return s;
    }
    
    public static boolean isInvalidFieldName(String value) {
    	return value==null
    		|| value.length()==0
    		|| value.length()>MAX_FIELD_NAME_LENGTH
    		|| pOnlyNumbers.matcher(value).find()
    		|| pBoolean.matcher(value).find();
    }
    
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
    
    public static boolean isDMY(String value) {
    	return value!=null && pDMY.matcher(value).find();
    }

    public static boolean isMDY(String value) {
    	return value!=null && pMDY.matcher(value).find();
    }
    
    public static boolean isYMD(String value) {
    	return value!=null && pYMD.matcher(value).find();
    }

    public static boolean isDecimalWithComma(String value) {
    	return value!=null && pDecimalWithComma.matcher(value).find();
    }

	public static boolean isNullOrEmpty(String value) {
		return value==null || value.trim().isEmpty();
	}
	
	public static boolean isZeroes(String value) {
		return value!=null && pZeroes.matcher(value).find();
	}

	public static boolean hasChanged(String s1, String s2) {
		if (s1==s2)
			return false;
		s1 = (s1==null) ? "" : s1;
		s2 = (s2==null) ? "" : s2;
		return !s1.equals(s2);
	}

	public static boolean hasChanged(String s1, String s2, int limitFirstCharsInComparison) {
		if (s1==s2)
			return false;
		s1 = (s1==null) ? "" : s1;
		s2 = (s2==null) ? "" : s2;
		if (limitFirstCharsInComparison>0) {
			if (s1.length()>limitFirstCharsInComparison)
				s1 = s1.substring(0, limitFirstCharsInComparison);
			if (s2.length()>limitFirstCharsInComparison)
				s2 = s2.substring(0, limitFirstCharsInComparison);
		}
		return !s1.equals(s2);
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
	 * Try to parse a string as a 'semester' identification. Best-effort method (will ignore any non numeric expressions, like texts or symbols)
	 */
	public static Semester parseSemester(String value) {
		if (value==null)
			return null;
		Pattern pNumbers = Pattern.compile("\\d+");
		int semesterNumber = 0;
		int year = 0;
		Matcher mNumbers = pNumbers.matcher(value);
		while (mNumbers.find()) {
			int n = Integer.parseInt(mNumbers.group());
			if (n<=0)
				return null;
			if (n==1 || n==2) {	// possibly a semester number
				if (semesterNumber!=0)
					return null;	// another semester number?
				else
					semesterNumber = n;
			}
			else { // possibly an year
				if (n<Periodicity.MIN_EXPECTED_YEAR || n>Periodicity.MAX_EXPECTED_YEAR)
					return null;	// unexpected year
				if (year!=0)
					return null;	// another year?
				else
					year = n;
			}
		}
		if (semesterNumber==0)
			return null; // missing semester number
		// we my afford to absence of year
		return new Semester(semesterNumber, year);
	}

	/**
	 * Parse date in format DD/MM/YYYY or DD-MM-YYYY
	 */
	public static Date parseDMY(String dmy) {
		if (dmy==null || dmy.length()!=10)
			return null;
		try {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.YEAR, Integer.parseInt(dmy.substring(6, 10)));
			cal.set(Calendar.MONTH, Integer.parseInt(dmy.substring(3, 5))-1);
			cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dmy.substring(0, 2)));
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			return cal.getTime();
		}
		catch (Throwable ex) {
			return null;
		}
	}
	
	/**
	 * Parse date in format MM/DD/YYYY or MM-DD-YYYY
	 */
	public static Date parseMDY(String mdy) {
		if (mdy==null || mdy.length()!=10)
			return null;
		try {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.YEAR, Integer.parseInt(mdy.substring(6, 10)));
			cal.set(Calendar.MONTH, Integer.parseInt(mdy.substring(0, 2))-1);
			cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(mdy.substring(3, 5)));
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			return cal.getTime();
		}
		catch (Throwable ex) {
			return null;
		}
	}
	
	/**
	 * Parse date in format YYYY/MM/DD or YYYY-MM-DD
	 */
	public static Date parseYMD(String ymd) {
		if (ymd==null || ymd.length()!=10)
			return null;
		try {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.YEAR, Integer.parseInt(ymd.substring(0, 4)));
			cal.set(Calendar.MONTH, Integer.parseInt(ymd.substring(5, 7))-1);
			cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(ymd.substring(8, 10)));
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			return cal.getTime();
		}
		catch (Throwable ex) {
			return null;
		}
	}

	public static String formatDecimal(double decimal) {
		if (Double.isNaN(decimal) || Double.isInfinite(decimal))
			return null;
		return tlDecimalFormat.get().format(decimal);
	}
	
    public static Number parseDecimal(String value) throws ParseException {
    	if (value==null || value.trim().length()==0)
    		return null;
    	return tlDecimalFormat.get().parse(value);
    }

    public static Number parseDecimalWithComma(String value) throws ParseException {
    	if (value==null || value.trim().length()==0)
    		return null;
    	return tlDecimalCommaFormat.get().parse(value);
    }
    
    public static Number parseDecimalFlexible(String value) throws ParseException {
    	try {
    		return parseDecimalWithComma(value);
    	}
    	catch (Throwable ex) {
    		return parseDecimal(value);
    	}
    }

	/**
	 * Returns a monetary value according to localized information
	 */
	public static String formatMonetaryValue(Object value) {
		if (value==null)
			return null;
		Number numeric;
		if (value instanceof Number)
			numeric = ((Number)value);
		else if (value instanceof String) {
			try {
				numeric = parseDecimalFlexible((String)value);
			} catch (ParseException e) {
				return null;
			}
		}
		else
			return null;
		NumberFormat nf = NumberFormat.getCurrencyInstance(LocaleContextHolder.getLocale());
		return nf.format(numeric).replaceAll("[^\\d\\,\\.]", "");
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
	 * Format a group name for use at Kontaktu.<BR>
	 * Removes spaces at start or end.<BR>
	 * Removes dots at start or end (the dots may prevent Elasticsearch from mapping these fields properly)
	 */
	public static String fmtGroupName(String groupName) {
		if (groupName==null)
			return "";
		String g = groupName.trim();
		while (g.startsWith("."))
			g = g.substring(1).trim();
		while (g.endsWith("."))
			g = g.substring(0, g.length()-1).trim();
		return g;
	}
}
