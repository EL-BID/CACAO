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

import org.idb.cacao.web.utils.DateTimeUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Enumeration for different basic forms of 'due date' calculations'
 * @author Gustavo Figueiredo
 *
 */
public enum DueDateCalcType {

	NONE("duedate.calc.none"),
	LAST_DAY_SAME_MONTH("duedate.calc.lastday.samemonth"),
	LAST_BUSINESS_DAY_SAME_MONTH("duedate.calc.lastbusinessday.samemonth"),
	LAST_DAY_NEXT_MONTH("duedate.calc.lastday.nextmonth"),
	LAST_BUSINESS_DAY_NEXT_MONTH("duedate.calc.lastbusinessday.nextmonth"),
	SAME_DAY("duedate.sameday");

	private final String display;
	
	DueDateCalcType(String display) {
		this.display = display;
	}

	@Override
	public String toString() {
		return display;
	}
	
	public static DueDateCalcType parse(String s) {
		if (s==null || s.trim().length()==0)
			return null;
		return Arrays.stream(values()).filter(t->t.name().equalsIgnoreCase(s)).findAny().orElse(null);
	}

	public static DueDateCalcType parse(String s, MessageSource messageSource) {
		if (s==null || s.trim().length()==0)
			return null;
		return Arrays.stream(values()).filter(t->t.name().equalsIgnoreCase(s)).findAny()
				.orElse(Arrays.stream(values()).filter(t->messageSource.getMessage(t.toString(),null,LocaleContextHolder.getLocale()).equalsIgnoreCase(s)).findAny()
						.orElse(null));
	}

	public static Date calcDueDate(DueDateCalcType calc, Date day_in_period) {
		if (day_in_period==null || calc==null || DueDateCalcType.NONE.equals(calc))
			return null;
		switch (calc) {
		case LAST_DAY_SAME_MONTH: {
			// Calculate the last day in the same month
			Calendar cal = Calendar.getInstance();
			cal.setTime(day_in_period);
			cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
			return DateTimeUtils.lastTimeOfDay(cal.getTime());		
			}
		case LAST_BUSINESS_DAY_SAME_MONTH: {
			// Calculate the last business day in the same month
			Calendar cal = Calendar.getInstance();
			cal.setTime(day_in_period);
			cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
			DateTimeUtils.calcPreviousBusinessDay(cal);
			return DateTimeUtils.lastTimeOfDay(cal.getTime());
			}
		case LAST_DAY_NEXT_MONTH: {
			// Calculate the last day in the next month
			Calendar cal = Calendar.getInstance();
			cal.setTime(day_in_period);
			cal.set(Calendar.DAY_OF_MONTH, 1);
			cal.add(Calendar.MONTH, 1);
			cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
			return DateTimeUtils.lastTimeOfDay(cal.getTime());			
			}
		case LAST_BUSINESS_DAY_NEXT_MONTH: {
			// Calculate the last business day in the next month
			Calendar cal = Calendar.getInstance();
			cal.setTime(day_in_period);
			cal.set(Calendar.DAY_OF_MONTH, 1);
			cal.add(Calendar.MONTH, 1);
			cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
			DateTimeUtils.calcPreviousBusinessDay(cal);
			return DateTimeUtils.lastTimeOfDay(cal.getTime());						
			}
		case SAME_DAY: {
			return day_in_period;
		}
		default:
			return null;
		}
	}

}
