/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.api.utils;

/**
 * Representation of a 'semester', with or without a year
 */
public class Semester implements Comparable<Semester> {

	private int semesterNumber;
	
	private int year;
	
	public Semester() { }
	
	public Semester(int semesterNumber) {
		this.semesterNumber = semesterNumber;
	}
	
	public Semester(int semesterNumber, int year) {
		this.semesterNumber = semesterNumber;
		this.year = year;
	}

	public int getSemesterNumber() {
		return semesterNumber;
	}

	public void setSemesterNumber(int semesterNumber) {
		this.semesterNumber = semesterNumber;
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}
	
	public int hashCode() {
		return 17 + 37 *( semesterNumber + 37 * year );
	}
	
	public boolean equals(Object o) {
		if (this==o)
			return true;
		if (!(o instanceof Semester))
			return false;
		Semester ref = (Semester)o;
		if (semesterNumber!=ref.semesterNumber)
			return false;
		if (year!=ref.year)
			return false;
		return true;
	}
	
	public String toString() {
		return String.format("%01dS-%04d", semesterNumber, year);
	}

	@Override
	public int compareTo(Semester o) {
		if (this==o)
			return 0;
		if (year<o.year)
			return -1;
		if (year>o.year)
			return 1;
		if (semesterNumber<o.semesterNumber)
			return -1;
		if (semesterNumber>o.semesterNumber)
			return 1;
		return 0;
	}
}
