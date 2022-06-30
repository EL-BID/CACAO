/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
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
