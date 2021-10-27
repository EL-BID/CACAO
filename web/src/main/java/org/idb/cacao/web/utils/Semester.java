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
