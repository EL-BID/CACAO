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

import java.util.Calendar;
import java.util.Date;

import org.idb.cacao.api.Periodicity;
import org.idb.cacao.web.controllers.services.FieldsConventionsService;

/**
 * Utility methods about Periodicity
 * 
 * @author Gustavo Figueiredo
 *
 */
public class PeriodicityUtils {

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
	 * Returns the period number formatted as long text (may include words).<BR>
	 * Returns NULL if the provided number was not recognized as a valid period number.
	 */
	public static String getLongFormattedPeriod(Number periodNumber, FieldsConventionsService fieldsConventionsService) {
		if (periodNumber==null)
			return null;
		Periodicity periodicity = Periodicity.getPeriodicity(periodNumber);
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

}
