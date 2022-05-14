/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.utils;

import java.util.Calendar;
import java.util.Date;

import org.idb.cacao.api.Periodicity;
import org.idb.cacao.api.utils.DateTimeUtils;
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
