/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.api.utils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Utility methods for date/time
 * 
 * @author Gustavo Figueiredo
 *
 */
public class DateTimeUtils {

	/**
	 * Returns the last time in the same date
	 */
	public static Date lastTimeOfDay(Date timestamp) {
		if (timestamp==null)
			return null;
		Calendar cal = Calendar.getInstance();
		cal.setTime(timestamp);
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		return cal.getTime();
	}
	
	/**
	 * Returns the first time in the same date
	 */
	public static Date firstTimeOfDay(Date timestamp) {
		if (timestamp==null)
			return null;
		Calendar cal = Calendar.getInstance();
		cal.setTime(timestamp);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}
	
	/**
	 * Returns the last time in the same month
	 */
	public static Date lastTimeOfMonth(Date timestamp) {
		if (timestamp==null)
			return null;
		Calendar cal = Calendar.getInstance();
		cal.setTime(timestamp);
		cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		return cal.getTime();
	}

	/**
	 * Returns the last time in the same year
	 */
	public static Date lastTimeOfYear(Integer year) {
		if (year==null)
			return null;
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONTH, Calendar.DECEMBER);
		cal.set(Calendar.DAY_OF_MONTH, 31);
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		return cal.getTime();
	}

	/**
	 * Returns the first date/time in the first day of the year
	 */
	public static Date firstTimeOfYear(Integer year) {
		if (year==null)
			return null;
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONTH, Calendar.JANUARY);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}

	/**
	 * Change 'cal' to the previous day if it's SATURDAY or SUNDAY.
	 */
	public static void calcPreviousBusinessDay(Calendar cal) {
		while (cal.get(Calendar.DAY_OF_WEEK)==Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK)==Calendar.SUNDAY)
			cal.add(Calendar.DAY_OF_MONTH, -1);
	}
	
	/**
	 * Given a date, returns the year. Returns NULL if parameter is NULL.
	 */
	public static Integer getYear(Date timestamp) {
		if (timestamp==null)
			return null;
		Calendar cal = Calendar.getInstance();
		cal.setTime(timestamp);
		return cal.get(Calendar.YEAR);
	}

	/**
	 * Given a date, returns the month number (starting at 1). Returns NULL if parameter is NULL.
	 */
	public static Integer getMonth(Date timestamp) {
		if (timestamp==null)
			return null;
		Calendar cal = Calendar.getInstance();
		cal.setTime(timestamp);
		return cal.get(Calendar.MONTH) + 1;
	}

	/**
	 * Given a date, returns the semester (either 1 or 2). Returns NULL if parameter is NULL.
	 */
	public static Integer getSemester(Date timestamp) {
		if (timestamp==null)
			return null;
		Calendar cal = Calendar.getInstance();
		cal.setTime(timestamp);
		int m = cal.get(Calendar.MONTH);
		if (m<Calendar.JULY)
			return 1;
		else
			return 2;
	}
	
	/**
	 * Given a Date object representing an instant in local timezone, rewrites this as in GMT
	 */
	public static Date convertLocalTimezoneToGMT(Date d) {
		Calendar cal_gmt = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		Calendar cal_local = Calendar.getInstance(TimeZone.getDefault());
		cal_local.setTime(d);
		cal_gmt.set(Calendar.YEAR, cal_local.get(Calendar.YEAR));
		cal_gmt.set(Calendar.MONTH, cal_local.get(Calendar.MONTH));
		cal_gmt.set(Calendar.DAY_OF_MONTH, cal_local.get(Calendar.DAY_OF_MONTH));
		cal_gmt.set(Calendar.HOUR_OF_DAY, cal_local.get(Calendar.HOUR_OF_DAY));
		cal_gmt.set(Calendar.MINUTE, cal_local.get(Calendar.MINUTE));
		cal_gmt.set(Calendar.SECOND, cal_local.get(Calendar.SECOND));
		cal_gmt.set(Calendar.MILLISECOND, cal_local.get(Calendar.MILLISECOND));
		return new Date(cal_gmt.getTimeInMillis());
	}

	/**
	 * Given a Date object representing an instant in GMT, rewrites this as in local timezone
	 */
	public static Date convertGMTToLocalTimezone(Date d) {
		Calendar cal_gmt = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		Calendar cal_local = Calendar.getInstance(TimeZone.getDefault());
		cal_gmt.setTime(d);
		cal_local.set(Calendar.YEAR, cal_gmt.get(Calendar.YEAR));
		cal_local.set(Calendar.MONTH, cal_gmt.get(Calendar.MONTH));
		cal_local.set(Calendar.DAY_OF_MONTH, cal_gmt.get(Calendar.DAY_OF_MONTH));
		cal_local.set(Calendar.HOUR_OF_DAY, cal_gmt.get(Calendar.HOUR_OF_DAY));
		cal_local.set(Calendar.MINUTE, cal_gmt.get(Calendar.MINUTE));
		cal_local.set(Calendar.SECOND, cal_gmt.get(Calendar.SECOND));
		cal_local.set(Calendar.MILLISECOND, cal_gmt.get(Calendar.MILLISECOND));
		return new Date(cal_local.getTimeInMillis());
	}
	
	/**
	 * 
	 * @return	An {@link OffsetDateTime} in UTC time.
	 */
	public static final OffsetDateTime now() {
		return OffsetDateTime.now(ZoneOffset.UTC);		
	}
}
