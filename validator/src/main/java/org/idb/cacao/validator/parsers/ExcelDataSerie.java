/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.validator.parsers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;

/**
 * This object is internally used by ExcelParser and should not be used externally.<BR>
 * <BR>
 * This class wraps references to several objects for retrieving data for a specific field
 * 
 * @author Gustavo Figueiredo
 */
class ExcelDataSerie {

	/**
	 * Specific Sheet for retrieving data
	 */
	Sheet sheet;
	
	/**
	 * For multiple sheets (referenced by multiple cellReferences) keep
	 * a map of sheets given their names
	 */
	Map<String, Sheet> sheets;
	
	/**
	 * Specific column for retrieving data.
	 * If NULL or negative, there is no specific column
	 */
	Integer column;
	
	/**
	 * Specific row for retrieving data.
	 * If NULL or negative, there is no specific row
	 */
	Integer row;
	
	/**
	 * First row for iterating values in the same column.
	 */
	Integer firstRow;

	/**
	 * Incremented if this particular field has different values in the same row (different columns)
	 */
	private Integer currentColumn;
	
	/**
	 * Incremented if this particular field has different values in the same column (different rows)
	 */
	private Integer currentRow;
	
	/**
	 * Specific cell references for retrieving data.
	 * IF NULL, ignores this field.
	 */
	CellReference[] cellReferences;
	
	/**
	 * Incremented if this particular field has different cell references
	 */
	private Integer currentCellReference;
	
	/**
	 * Specific cell values already extracted from the workbook..
	 * IF NULL, ignores this field.
	 */
	ExcelValue[] individualValues;
	
	/**
	 * Incremented if this particular field has specified individual values
	 */
	private Integer currentIndividualValue;
	
	/**
	 * Persist the boolean indications of the field mapping (prevent unnecessary recalculations)
	 */
	private Boolean sameRow;
	
	/**
	 * Persist the boolean indications of the field mapping (prevent unnecessary recalculations)
	 */
	private Boolean sameColumn;
	
	/**
	 * Persist the constant value (prevent unnecessary recalculations)
	 */
	private Object constantValue;
	
	/**
	 * Keep for each sheet name the corresponding order as long as we do a full scan over all sheets
	 */
	private Map<String,Integer> sheetOrderNumbers;
	
	/**
	 * Use this object for sort and binarySearch over cellReferences as we do a full scan over all sheets
	 */
	private Comparator<CellReference> cellReferencesComparator;

	/**
	 * Use this object for sort and binarySearch over individualValues as we do a full scan over all sheets
	 */
	private Comparator<ExcelValue> individualValuesComparator;
	
	/**
	 * Indicates this data collected with this field should be treated as 'metrics' (i.e. something to 'sum' when performing queries, not to 'group by'). 
	 * Internally we will try to avoid to repeat the same value for different 'records' produced by the parser. By default all decimal fields are treated as metrics.
	 */
	boolean metric;
	
	/**
	 * Indicates that the previous returned value was used at least once
	 */
	private boolean usedPreviousValue;

	/**
	 * Informs this field has information gathered from the same sheet and the same row 
	 */
	public boolean isSameRow() {
		if (sameRow!=null)
			return sameRow;
		sameRow = (row!=null && row.intValue()>=0) || ExcelParser.isSameRow(cellReferences) || ExcelParser.isSameRow(individualValues);
		return sameRow;
	}
	
	/**
	 * Informs this field has information gathered from the same sheet and the same column 
	 */
	public boolean isSameColumn() {
		if (sameColumn!=null)
			return sameColumn;
		sameColumn = (column!=null && column.intValue()>=0) || ExcelParser.isSameColumn(cellReferences) || ExcelParser.isSameColumn(individualValues);
		return sameColumn;
	}
	
	/**
	 * Informs this field has information gathered from one singular cell (same sheet, same row, same column) 
	 */
	public boolean isConstant() {
		return isSameColumn() && isSameRow();
	}
	
	/**
	 * Indicates the previous collected value related to this field was used and stored in a produced record.
	 */
	public void setUsedPreviousValue() {
		usedPreviousValue = true;
	}
	
	/**
	 * Informs this field has no information at all
	 */
	public boolean isVoid() {
		return !isConstant() 
				&& (cellReferences==null || cellReferences.length==0)
				&& (individualValues==null || individualValues.length==0)
				&& !isSameColumn() && !isSameRow();
	}
	
	/**
	 * Returns the sheets with information regarding this particular field
	 */
	public Collection<Sheet> getSheets() {
		if (sheet!=null)
			return Collections.singleton(sheet);
		else
			return sheets.values();
	}
	
	/**
	 * Returns the expected number of information to return for a particular shield regarding this particular field
	 */
	public int getSize(Sheet sheet) {
		if (isConstant()) {
			return 1;
		}
		if (this.sheet!=null && !this.sheet.equals(sheet)) {
			return 0;
		}
		if (cellReferences!=null) {
			final String sheet_name = sheet.getSheetName();
			return (int)Arrays.stream(cellReferences).filter(c->sheet_name.equals(c.getSheetName())).count();
		}
		else if (individualValues!=null) {
			return (int)Arrays.stream(individualValues).filter(c->sheet.equals(c.getSheet())).count();
		}
		else if (isSameColumn()) {
			try {
				int lastRow = sheet.getLastRowNum();
				if (lastRow<=0)
					return 0;
				if (firstRow==null)
					return lastRow+1;
				else
					return Math.max(0,lastRow-firstRow.intValue()+1);
			}
			catch (Exception ex) {
				return 0;
			}
		}
		else if (isSameRow()) {
			try {
				Row r = sheet.getRow(row);
				if (r==null)
					return 0;
				short lastCol = r.getLastCellNum();
				if (lastCol<=0)
					return 0;
				return lastCol;
			}
			catch (Exception ex) {
				return 0;
			}
		}
		return 0;
	}
	
	/**
	 * Returns the next available information regarding this field, if any.
	 * @param incrementAfter If TRUE, increments internal counters in order to return another value next time
	 * @param evaluator Object used for evaluating formulas.
	 */
	public Object getNextValue(boolean incrementAfter, FormulaEvaluator evaluator) {
		if (isConstant()) {
			// If it's a metric and we have already used this value before, we will not return it again
			if (metric && usedPreviousValue)
				return null;
			// If this field represents a constant, we should return always the same value regardless of the provided cell
			if (constantValue!=null)
				return constantValue;
			if (row!=null && row.intValue()>=0) {
				Row r = sheet.getRow(row);
				if (r==null)
					return null;
				Cell c = r.getCell(column);
				if (c==null)
					return null;
				constantValue = ExcelParser.getCellValue(c, evaluator);
				return constantValue;
			}
			else if (cellReferences!=null && cellReferences.length>0) {
				Row r = sheet.getRow(cellReferences[0].getRow());
				if (r==null)
					return null;
				Cell c = r.getCell(cellReferences[0].getCol());
				if (c==null)
					return null;
				constantValue = ExcelParser.getCellValue(c, evaluator);
				return constantValue;
			}
			else if (individualValues!=null && individualValues.length>0) {
				constantValue = individualValues[0].getValue();
				return constantValue;
			}
		}
		else if (cellReferences!=null) {
			if (currentCellReference==null)
				currentCellReference = 0;
			if (currentCellReference.intValue()>=cellReferences.length) {
				return null;
			}
			try {
				CellReference cref = cellReferences[currentCellReference];
				Sheet sheetRef = (sheets!=null) ? sheets.getOrDefault(cref.getSheetName(), this.sheet) : this.sheet;
				if (sheetRef==null)
					return null;
				Row r = sheetRef.getRow(cref.getRow());
				if (r==null)
					return null;
				Cell c = r.getCell(cref.getCol());
				if (c==null)
					return null;
				return ExcelParser.getCellValue(c, evaluator);
			}
			finally {
				if (incrementAfter) {
					currentCellReference++;
					usedPreviousValue = false;
				}
			}
		}
		else if (individualValues!=null) {
			if (currentIndividualValue==null)
				currentIndividualValue = 0;
			if (currentIndividualValue.intValue()>=individualValues.length) {
				return null;
			}
			try {
				return individualValues[currentIndividualValue].getValue();
			}
			finally {
				if (incrementAfter) {
					currentIndividualValue++;
					usedPreviousValue = false; 
				}
			}
		}
		else if (isSameColumn()) {
			if (currentRow==null)
				currentRow = (firstRow==null) ? 0 : firstRow.intValue();
			try {
				Row r = sheet.getRow(currentRow);
				if (r==null)
					return null;
				Cell c = r.getCell(column);
				if (c==null)
					return null;
				return ExcelParser.getCellValue(c, evaluator);
			}
			finally {
				if (incrementAfter) {
					currentRow++;
					usedPreviousValue = false; 
				}
			}
		}
		else if (isSameRow()) {
			if (currentColumn==null)
				currentColumn = 1;
			try {
				Row r = sheet.getRow(row);
				if (r==null)
					return null;
				Cell c = r.getCell(currentColumn);
				if (c==null)
					return null;
				return ExcelParser.getCellValue(c, evaluator);
			}
			finally {
				if (incrementAfter) {
					currentColumn++;
					usedPreviousValue = false; 
				}
			}				
		}
		return null;
	}
	
	/**
	 * Tells we are going to scan every cell in every row in every sheet in the order they appear.
	 */
	public void optimizeForScanningAllCells(List<String> sheetOrder) {
		
		sheetOrderNumbers = new HashMap<>();
		int i=0;
		for (String s: sheetOrder) {
			sheetOrderNumbers.put(s, i++);
		}

		if (cellReferences!=null) {
			cellReferencesComparator = new Comparator<CellReference>() {
				@Override
				public int compare(CellReference c1, CellReference c2) {
					int o1 = c1.getSheetName()==null ? Integer.MAX_VALUE : sheetOrderNumbers.get(c1.getSheetName());
					int o2 = c2.getSheetName()==null ? Integer.MAX_VALUE : sheetOrderNumbers.get(c2.getSheetName());
					if (o1<o2)
						return -1;
					if (o1>o2)
						return 1;
					if (c1.getRow()<c2.getRow())
						return -1;
					if (c1.getRow()>c2.getRow())
						return 1;
					if (c1.getCol()<c2.getCol())
						return -1;
					if (c1.getCol()>c2.getCol())
						return 1;
					return 0;
				}					
			};
			Arrays.sort(cellReferences, cellReferencesComparator);
			currentCellReference = null;
		}
		if (individualValues!=null) {
			individualValuesComparator = new Comparator<ExcelValue>() {
				@Override
				public int compare(ExcelValue c1, ExcelValue c2) {
					int o1 = c1.getSheetName()==null ? Integer.MAX_VALUE : sheetOrderNumbers.get(c1.getSheetName());
					int o2 = c2.getSheetName()==null ? Integer.MAX_VALUE : sheetOrderNumbers.get(c2.getSheetName());
					if (o1<o2)
						return -1;
					if (o1>o2)
						return 1;
					if (c1.getRow()<c2.getRow())
						return -1;
					if (c1.getRow()>c2.getRow())
						return 1;
					if (c1.getCol()<c2.getCol())
						return -1;
					if (c1.getCol()>c2.getCol())
						return 1;
					return 0;
				}					
			};
			Arrays.sort(individualValues, individualValuesComparator);
			currentIndividualValue = null;
		}
	}
	
	/**
	 * Returns value regarding this cell, if applicable to this field.
	 * @param cell Cell coordinates and methods for retrieving value
	 * @param lookAtNeighbors Will be set to TRUE if the value was gathered at the cell location. Will be set to FALSE if it was gathered at neighborhood.
	 * @param evaluator Object used for evaluating formulas.
	 */
	public Object getValue(Cell cell, AtomicBoolean atLocation, FormulaEvaluator evaluator) {
		atLocation.set(false);
		if (isConstant()) {
			// If this field represents a constant, we should return always the same value regardless of the provided cell
			Object value = getNextValue(/*incrementAfter*/false, evaluator);
			if (value!=null)
				atLocation.set(true);
			return value;
		}
		else if (cellReferences!=null) {
			if (currentCellReference==null)
				currentCellReference = 0;
			if (currentCellReference.intValue()>=cellReferences.length) {
				return null;
			}
			CellReference requestedCellRef = new CellReference(cell);
			CellReference cref = cellReferences[currentCellReference];
			if (cellReferencesComparator.compare(requestedCellRef, cref)!=0) {
				int position = Arrays.binarySearch(cellReferences, /*fromIndex*/currentCellReference, /*toIndex*/cellReferences.length, requestedCellRef, cellReferencesComparator);
				if (position<0) {
					// If binarySearch returned negative number, we need to find an approximation to the requested cell
					int inRrangeElementPosition = -2-position;
					if (inRrangeElementPosition<0)
						return null; // we did not reach the position for this field
					if (currentCellReference != inRrangeElementPosition) {
						currentCellReference = inRrangeElementPosition;
						usedPreviousValue = false; // reset this indication because we are now at a different value position
					}
					else {
						if (metric && usedPreviousValue)
							return null;
					}
				}
				else {
					// If binarySearch returned non-negative number, this position holds the exact cell reference it was requested
					if (currentCellReference != position) {
						currentCellReference = position;
						usedPreviousValue = false; // reset this indication because we are now at a different value position
					}
					else {
						if (metric && usedPreviousValue)
							return null;							
					}
				}
				if (currentCellReference.intValue()>=cellReferences.length)
					return null; // we reached the end of mapped data
				cref = cellReferences[currentCellReference];
				// When scanning over multiple sheets, let's make a boundary across different sheets
				if (!cref.getSheetName().equals(cell.getSheet().getSheetName()))
					return null;
			}
			else {
				// If we are at the same position as we where before, and if it's treated as 'metrics', and if the previous value has already
				// been used at a produced record, does not return the same value again
				if (metric && usedPreviousValue)
					return null;
			}
			Sheet sheetRef = cell.getSheet();
			Row r = sheetRef.getRow(cref.getRow());
			if (r==null)
				return null;
			Cell c = r.getCell(cref.getCol());
			if (c==null)
				return null;
			atLocation.set(cellReferencesComparator.compare(requestedCellRef, cref)==0);
			return ExcelParser.getCellValue(c, evaluator);
		}
		else if (individualValues!=null) {
			if (currentIndividualValue==null)
				currentIndividualValue = 0;
			if (currentIndividualValue.intValue()>=individualValues.length) {
				return null;
			}
			ExcelValue requestedCellRef = new ExcelValue(cell, /*value*/null);
			ExcelValue cref = individualValues[currentIndividualValue];
			if (individualValuesComparator.compare(requestedCellRef, cref)!=0) {
				int position = Arrays.binarySearch(individualValues, /*fromIndex*/currentIndividualValue, /*toIndex*/individualValues.length, requestedCellRef, individualValuesComparator);
				if (position<0) {
					// If binarySearch returned negative number, we need to find an approximation to the requested cell
					int inRangeElementPosition = -2-position;
					if (inRangeElementPosition<0)
						return null; // we did not reach the position for this field
					if (currentIndividualValue != inRangeElementPosition) {
						currentIndividualValue = inRangeElementPosition;
						usedPreviousValue = false; // reset this indication because we are now at a different value position
					}
					else {
						if (metric && usedPreviousValue)
							return null;					
					}
				}
				else {
					// If binarySearch returned non-negative number, this position holds the exact cell reference it was requested
					if (currentIndividualValue != position) {
						currentIndividualValue = position;
						usedPreviousValue = false; // reset this indication because we are now at a different value position
					}
					else {
						if (metric && usedPreviousValue)
							return null;												
					}
				}
				if (currentIndividualValue.intValue()>=individualValues.length)
					return null; // we reached the end of mapped data
				cref = individualValues[currentIndividualValue];
				// When scanning over multiple sheets, let's make a boundary across different sheets
				if (!cref.getSheet().equals(cell.getSheet()))
					return null;
			}
			else {
				// If we are at the same position as we where before, and if it's treated as 'metrics', and if the previous value has already
				// been used at a produced record, does not return the same value again
				if (metric && usedPreviousValue)
					return null;					
			}
			atLocation.set(individualValuesComparator.compare(requestedCellRef, cref)==0);
			return cref.getValue();
		}
		else if (isSameColumn()) {
			if (sheet!=null && !sheet.equals(cell.getSheet()))
				return null;
			if (column!=null && column.intValue()!=cell.getColumnIndex())
				return null;
			if (firstRow!=null && cell.getRowIndex()<firstRow.intValue())
				return null;
			atLocation.set(true);
			return ExcelParser.getCellValue(cell, evaluator);
		}
		else if (isSameRow()) {
			if (sheet!=null && !sheet.equals(cell.getSheet()))
				return null;
			if (row!=null && row.intValue()!=cell.getRowIndex())
				return null;
			if (cell.getColumnIndex()<1)
				return null;
			atLocation.set(true);
			return ExcelParser.getCellValue(cell, evaluator);
		}
		return null;
	}

}