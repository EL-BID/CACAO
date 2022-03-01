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
package org.idb.cacao.validator.parsers;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * This object is internally used by ExcelParser and should not be used externally.<BR>
 * <BR>
 * This class holds information regarding one singular value and the corresponding cell reference
 */
class ExcelValue {
	
	/**
	 * The sheet from where we retrieved the information
	 */
	final Sheet sheet;

	/**
	 * The row number (0 based) from where we retrieved the information
	 */
	final int rowNumber;
	
	/**
	 * The column number (0 based) from where we retrieved the information
	 */
	final int colNumber;
	
	/**
	 * The value represented here (already extracted the value of interest from the referenced cell)
	 */
	final String value;
	
	ExcelValue(Sheet sheet, int rowNumber, int colNumber, String value) {
		this.sheet = sheet;
		this.rowNumber = rowNumber;
		this.colNumber = colNumber;
		this.value = value;
	}
	
	ExcelValue(Cell cell, String value) {
		this(cell.getSheet(), cell.getRowIndex(), cell.getColumnIndex(), value);
	}
	
	/**
	 * The sheet from where we retrieved the information
	 */
	public String getSheetName() {
		return sheet.getSheetName();
	}

	/**
	 * The sheet from where we retrieved the information
	 */
	public Sheet getSheet() {
		return sheet;
	}

	/**
	 * The row number (0 based) from where we retrieved the information
	 */
	public int getRow() {
		return rowNumber;
	}
	
	/**
	 * The column number (0 based) from where we retrieved the information
	 */
	public int getCol() {
		return colNumber;
	}
	
	/**
	 * The value represented here (already extracted the value of interest from the referenced cell)
	 */
	public String getValue() {
		return value;
	}
	
	public String toString() {
		return String.format("%s!%s%d=%s",getSheetName(),Character.toString((char)('A'+colNumber)),rowNumber+1,value);
	}
}