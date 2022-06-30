/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
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