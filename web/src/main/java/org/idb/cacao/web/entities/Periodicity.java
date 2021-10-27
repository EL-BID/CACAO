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
package org.idb.cacao.web.entities;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Pattern;

import org.idb.cacao.web.controllers.services.FieldsConventionsService;
import org.idb.cacao.web.utils.DateTimeUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Enumeration of different periodic schemes.<BR>
 * <BR>
 * Also includes some functions for dealing with a 'period number', with implicit periodicity.<BR>
 * <BR>
 * Numbers varying between MIN_EXPECTED_YEAR and MAX_EXPECTED_YEAR are considered as 'years' and have periodicity YEARLY.<BR>
 * Numbers varying between MIN_EXPECTED_YYYYMM_NUMBER and MAX_EXPECTED_YYYYMM_NUMBER are considered as months encoded in format (YEAR * 100 + MONTH) and have periodicity MONTHLY.<BR>
 * 
 * 
 * @author Gustavo Figueiredo
 *
 */
public enum Periodicity {

	UNKNOWN("periodicity.unknown"),
	MONTHLY("periodicity.monthly"),
	SEMIANNUALLY("periodicity.semianually"),
	YEARLY("periodicity.yearly");

	private final String display;
	
	public static final int MIN_EXPECTED_YEAR = 2000;
	public static final int MAX_EXPECTED_YEAR = 3001; // when we will be using 'BrainCap' while watching the view from a 'space elevator'

	public static final int MIN_EXPECTED_YYYYS_NUMBER = MIN_EXPECTED_YEAR*10 + 1; 
	public static final int MAX_EXPECTED_YYYYS_NUMBER = MAX_EXPECTED_YEAR*10 + 2; 

	public static final int MIN_EXPECTED_YYYYMM_NUMBER = MIN_EXPECTED_YEAR*100 + 1; 
	public static final int MAX_EXPECTED_YYYYMM_NUMBER = MAX_EXPECTED_YEAR*100 + 12; 
	
	public static final Pattern otherPatternsForMonths = Pattern.compile("^(?>months|month)$", Pattern.CASE_INSENSITIVE);
	public static final Pattern otherPatternsForSemesters = Pattern.compile("^(?>semesters|semester)$", Pattern.CASE_INSENSITIVE);
	public static final Pattern otherPatternsForYears = Pattern.compile("^(?>years|year)$", Pattern.CASE_INSENSITIVE);
	
	Periodicity(String display) {
		this.display = display;
	}

	@Override
	public String toString() {
		return display;
	}
	
	public static Periodicity parse(String s) {
		if (s==null || s.trim().length()==0)
			return null;
		Periodicity p = Arrays.stream(values()).filter(t->t.name().equalsIgnoreCase(s)).findAny().orElse(null);
		if (p!=null)
			return p;
		if (otherPatternsForMonths.matcher(s).find())
			return MONTHLY;
		if (otherPatternsForSemesters.matcher(s).find())
			return SEMIANNUALLY;
		if (otherPatternsForYears.matcher(s).find())
			return YEARLY;
		return null;
	}

	public static Periodicity parse(String s, MessageSource messageSource) {
		if (s==null || s.trim().length()==0)
			return null;
		return Arrays.stream(values()).filter(t->t.name().equalsIgnoreCase(s)).findAny()
				.orElse(Arrays.stream(values()).filter(t->messageSource.getMessage(t.toString(),null,LocaleContextHolder.getLocale()).equalsIgnoreCase(s)).findAny()
						.orElse(null));
	}
	
	/**
	 * Given a periodicity and a period number, returns the first date in this period
	 */
	public static Date getMinDate(Number periodNumber, Periodicity periodicity) {
		if (periodNumber==null)
			return null;
		if (periodicity==null || Periodicity.UNKNOWN.equals(periodicity))
			periodicity = getPeriodicity(periodNumber);
		switch (periodicity) {
		case MONTHLY: {
			final int year = periodNumber.intValue()/100;
			final int month = periodNumber.intValue()%100;
			return new GregorianCalendar(year, /*month 0-based*/ month - 1, /*dayOfMonth*/1, /*hourOfDay*/0, /*minute*/0, /*second*/0).getTime();
		}
		case SEMIANNUALLY: {
			final int year = periodNumber.intValue()/10;
			final int semester = periodNumber.intValue()%10;
			return new GregorianCalendar(year, /*month 0-based*/ (semester - 1) * 6, /*dayOfMonth*/1, /*hourOfDay*/0, /*minute*/0, /*second*/0).getTime();
		}
		case YEARLY: {
			final int year = periodNumber.intValue();
			return new GregorianCalendar(year, /*month 0-based*/0, /*dayOfMonth*/1, /*hourOfDay*/0, /*minute*/0, /*second*/0).getTime();
		}
		default:
			return null;
		}
	}

	/**
	 * Given a periodicity and a date in period, return the period number
	 */
	public static Integer getPeriodNumber(Date dateInPeriod, Periodicity periodicity) {
		if (dateInPeriod==null)
			return null;
		if (periodicity==null || Periodicity.UNKNOWN.equals(periodicity))
			return null;
		switch (periodicity) {
		case MONTHLY: {
			Calendar cal = Calendar.getInstance();
			cal.setTime(dateInPeriod);
			return cal.get(Calendar.YEAR) * 100 + (cal.get(Calendar.MONTH)+1);
		}
		case SEMIANNUALLY: {
			Calendar cal = Calendar.getInstance();
			cal.setTime(dateInPeriod);
			int y = cal.get(Calendar.YEAR);
			int m = cal.get(Calendar.MONTH);
			if (m<Calendar.JULY)
				return y * 10 + 1;
			else
				return y * 10 + 2;
		}
		case YEARLY: {
			return DateTimeUtils.getYear(dateInPeriod);
		}
		default:
			return null;
		}
	}

	/**
	 * Given a period number (usually in the form 'YYYYMM') return the periodicity enumeration constant (e.g. will return MONTHLY for
	 * something like 'YYYYMM')
	 */
	public static Periodicity getPeriodicity(Number periodNumber) {
		if (periodNumber==null)
			return Periodicity.UNKNOWN;
		long n = periodNumber.longValue();
		if (n<=0)
			return Periodicity.UNKNOWN;
		if (n>=MIN_EXPECTED_YYYYMM_NUMBER && n<=MAX_EXPECTED_YYYYMM_NUMBER
				&& (n%100)>=1 && (n%100)<=12) {
			return Periodicity.MONTHLY;
		}
		if (n>=MIN_EXPECTED_YYYYS_NUMBER && n<=MAX_EXPECTED_YYYYS_NUMBER
				&& (n%10)>=1 && (n%10)<=2) {
			return Periodicity.SEMIANNUALLY;
		}
		if (n>=MIN_EXPECTED_YEAR && n<=MAX_EXPECTED_YEAR) {
			return Periodicity.YEARLY;
		}
		return Periodicity.UNKNOWN;
	}
	
	public static boolean isValidPeriod(Number periodNumber, Periodicity periodicity) {
		if (periodicity==null || Periodicity.UNKNOWN.equals(periodicity))
			periodicity = getPeriodicity(periodNumber);
		if (periodicity==null || Periodicity.UNKNOWN.equals(periodicity))
			return false;
		long n = periodNumber.longValue();
		if (n<=0)
			return false;
		switch (periodicity) {
		case MONTHLY: {
			return (n>=MIN_EXPECTED_YYYYMM_NUMBER && n<=MAX_EXPECTED_YYYYMM_NUMBER
					&& (n%100)>=1 && (n%100)<=12);
		}
		case SEMIANNUALLY: {
			return (n>=MIN_EXPECTED_YYYYS_NUMBER && n<=MAX_EXPECTED_YYYYS_NUMBER
					&& (n%10)>=1 && (n%10)<=2);
		}
		case YEARLY: {
			return (n>=MIN_EXPECTED_YEAR && n<=MAX_EXPECTED_YEAR);
		}
		default:
			return false;
		}
	}

	/**
	 * Returns the period number formatted as text.<BR>
	 * Returns NULL if the provided number was not recognized as a valid period number.
	 */
	public static String getFormattedPeriod(Number periodNumber) {
		if (periodNumber==null)
			return null;
		Periodicity periodicity = getPeriodicity(periodNumber);
		if (periodicity==null || Periodicity.UNKNOWN.equals(periodicity))
			return null;
		switch (periodicity) {
		case MONTHLY: {
			final int year = periodNumber.intValue()/100;
			final int month = periodNumber.intValue()%100;
			return String.format("%02d-%04d", month, year);
		}
		case SEMIANNUALLY: {
			final int year = periodNumber.intValue()/10;
			final int semester = periodNumber.intValue()%10;
			return String.format("%01dS-%04d", semester, year);
		}
		case YEARLY: {
			final int year = periodNumber.intValue();
			return String.valueOf(year);
		}
		default:
			return null;
		}
	}
	
	/**
	 * Returns the period number formatted as long text (may include words).<BR>
	 * Returns NULL if the provided number was not recognized as a valid period number.
	 */
	public static String getLongFormattedPeriod(Number periodNumber, FieldsConventionsService fieldsConventionsService) {
		if (periodNumber==null)
			return null;
		Periodicity periodicity = getPeriodicity(periodNumber);
		if (periodicity==null || Periodicity.UNKNOWN.equals(periodicity))
			return null;
		switch (periodicity) {
		case MONTHLY: {
			String[] all_months = fieldsConventionsService.getDefaultCalendarMonths();
			int year_month = periodNumber.intValue();
			int year = year_month/100;
			int month = year_month%100;
			return (all_months!=null && all_months.length==12) ? 
					all_months[month-1] + "-" + String.format("%04d", year) 
					: null;
		}
		case SEMIANNUALLY: {
			String[] all_semesters = fieldsConventionsService.getDefaultCalendarSemesters();
			int year_semester = periodNumber.intValue();
			int year = year_semester/10;
			int semester = year_semester%10;
			return (all_semesters!=null && all_semesters.length==2) ? 
					all_semesters[semester-1] + "-" + String.format("%04d", year) 
					: null;
		}
		case YEARLY: {
			final int year = periodNumber.intValue();
			return String.valueOf(year);			
		}
		default:
			return null;
		}
	}
	
	/**
	 * Returns the year, given some period number.
	 */
	public static Integer getYear(Number periodNumber) {
		if (periodNumber==null)
			return null;
		Periodicity periodicity = getPeriodicity(periodNumber);
		if (periodicity==null || Periodicity.UNKNOWN.equals(periodicity))
			return null;
		switch (periodicity) {
		case MONTHLY: {
			int year_month = periodNumber.intValue();
			return year_month/100;
		}
		case SEMIANNUALLY: {
			int year_semester = periodNumber.intValue();
			return year_semester/10;
		}
		case YEARLY: {
			return periodNumber.intValue();
		}
		default:
			return null;
		}		
	}
	
	/**
	 * Returns the semester, given some period number.
	 */
	public static Integer getSemester(Number periodNumber) {
		if (periodNumber==null)
			return null;
		Periodicity periodicity = getPeriodicity(periodNumber);
		if (periodicity==null || Periodicity.UNKNOWN.equals(periodicity))
			return null;
		switch (periodicity) {
		case MONTHLY: {
			final int month = periodNumber.intValue()%100;
			return (month<=6) ? 1 : 2;
		}
		case SEMIANNUALLY: {
			final int semester = periodNumber.intValue()%10;
			return semester;
		}
		default:
			return null;
		}		
	}

	/**
	 * Returns the next consecutive period number.<BR>
	 * Returns NULL if the provided number was not recognized as a valid period number.
	 */
	public static Number getNextPeriod(Number periodNumber) {
		Periodicity periodicity = getPeriodicity(periodNumber);
		if (periodicity==null || Periodicity.UNKNOWN.equals(periodicity))
			return null;
		switch (periodicity) {
		case MONTHLY: {
			final int year = periodNumber.intValue()/100;
			final int month = periodNumber.intValue()%100;
			if (month==12) {
				return (year+1)*100 + 1;
			}
			else {
				return year*100 + (month+1);
			}
		}
		case SEMIANNUALLY: {
			final int year = periodNumber.intValue()/10;
			final int semester = periodNumber.intValue()%10;
			if (semester==2) {
				return (year+1)*10 + 1;
			}
			else {
				return year*10 + (semester+1);
			}
		}
		case YEARLY: {
			final int year = periodNumber.intValue();
			return year+1;
		}
		default:
			return null;
		}		
	}
}
