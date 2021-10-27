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
package org.idb.cacao.web.controllers;

import java.util.Calendar;
import java.util.Date;

/**
 * Class used to form an unique identification number for a given document. Some document with the
 * same ID of a previous document should be considered as a replacement.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class DocumentId {
	
	/**
	 * Use this method in order to form a document ID. Call it's method 'build' after giving all available information
	 * about a document.
	 */
	public static Builder Builder() {
		return new Builder();
	}

	/**
	 * Object used for making an ID for a document with available information.
	 * @author Gustavo Figueiredo
	 */
	public static class Builder {
		
		private String subject;
		
		private String year;
		
		private Integer semester;
		
		private String month;
		
		private Date day;
		
		private Builder() {			
		}
		
		public Builder withSubject(String subject) {
			this.subject = subject;
			return this;
		}
		
		public Builder withYear(String year) {
			this.year = year;
			return this;
		}
		
		public Builder withSemester(Integer semester) {
			this.semester = semester;
			return this;
		}

		public Builder withMonth(String month) {
			this.month = month;
			return this;
		}
		
		public Builder withDay(Date day) {
			this.day = day;
			return this;
		}
		
		public String build() {
			StringBuilder sb = new StringBuilder();
			if (subject!=null && subject.trim().length()>0)
				sb.append(subject);
			if (day!=null) {
				Calendar cal = Calendar.getInstance();
				cal.setTime(day);
				if (sb.length()>0)
					sb.append("_");
				sb.append(cal.get(Calendar.YEAR));	
				sb.append("_");
				sb.append(String.format("%02d",cal.get(Calendar.MONTH)+1));
				sb.append("_");
				sb.append(String.format("%02d",cal.get(Calendar.DAY_OF_MONTH)));
			}
			else {
				if (year!=null && year.trim().length()>0) {
					if (sb.length()>0)
						sb.append("_");
					sb.append(year);
				}
				if (semester!=null && semester.intValue()>0) {
					if (sb.length()>0)
						sb.append("_");
					sb.append("S");
					sb.append(semester);
				}
				if (month!=null && month.trim().length()>0) {
					if (sb.length()>0)
						sb.append("_");
					sb.append(month);
				}
			}
			return sb.toString();
		}
	}
}
