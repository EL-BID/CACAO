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

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.templates.DocumentInput;
import org.idb.cacao.api.templates.DocumentInputFieldMapping;

public class ExcelParser implements FileParser {
	
	/**
	 * Maximum number of successive empty rows (for columnar data) or successive empty columns (for row data) for determining the
	 * data has finished.
	 */
	private static final int MAXIMUM_NUMBER_OF_EMPTY_ROWS_TO_BREAK = 10;

	private int MINIMUM_NUMBER_OF_SHEETS = 1;

	private int numberOfLines = -1;

	private Path path;

	private DocumentInput documentInputSpec;

	private Workbook workbook;
	
	/**
	 * For each 'DocumentInputFieldMapping' keep a 'DataSerie' object for iterating through the corresponding data
	 */
	private Map<DocumentInputFieldMapping, DataSerie> mapDataSeries;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.idb.cacao.validator.parsers.FileParser#getPath()
	 */
	@Override
	public Path getPath() {
		return path;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.idb.cacao.validator.parsers.FileParser#setPath(java.nio.file.Path)
	 */
	@Override
	public void setPath(Path path) {
		this.path = path;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.idb.cacao.validator.parsers.FileParser#getDocumentInputSpec()
	 */
	@Override
	public DocumentInput getDocumentInputSpec() {
		return documentInputSpec;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.idb.cacao.validator.parsers.FileParser#setDocumentInputSpec(org.idb.cacao
	 * .api.templates.DocumentInput)
	 */
	@Override
	public void setDocumentInputSpec(DocumentInput inputSpec) {
		this.documentInputSpec = inputSpec;
	}

	@Override
	public void start() {
		if (path == null || !path.toFile().exists()) {
			return;
		}

		if (workbook != null) {
			try {
				workbook.close();
			} catch (Exception e) {
			}
		}
		
		mapDataSeries = new IdentityHashMap<>();

		try {
			FileInputStream inputStream = new FileInputStream(path.toFile());
			Workbook workbook = WorkbookFactory.create(inputStream);

			// Check if the workbook has all sheets needed
			if (workbook.getNumberOfSheets() < MINIMUM_NUMBER_OF_SHEETS)
				workbook.createSheet("Sample");

			Map<Integer, Sheet> sheetsByNumber = new LinkedHashMap<>();
			Map<String, Sheet> sheetsByName = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			Map<Sheet, Integer> firstRowPerSheet = new IdentityHashMap<>();

			for (int i = 0; i < workbook.getNumberOfSheets(); i++) {

				Sheet sheet = workbook.getSheetAt(i);
				sheetsByNumber.put(i, sheet);
				sheetsByName.put(sheet.getSheetName(), sheet);

				numberOfLines = Math.max(numberOfLines, sheet.getLastRowNum());
				
				int firstDataRowInSheet = guessFirstRow(i, workbook);
				firstRowPerSheet.put(sheet, firstDataRowInSheet);

			}

			for (DocumentInputFieldMapping fieldMapping : documentInputSpec.getFields()) {
				
				// Get sheet by index
				Integer sheetIndex = fieldMapping.getSheetIndex();
				Sheet sheet = null;
				if ( sheetIndex != null && sheetIndex >= 0 )
					sheet = sheetsByNumber.get(sheetIndex);
				
				if ( sheet == null ) {							
					// Get sheet by name
					String sheetNameExpr = fieldMapping.getSheetNameExpression();
					if (sheetNameExpr != null && sheetNameExpr.trim().length() > 0) {
						
						sheet = sheetsByName.get(sheetNameExpr);
						
						if (sheet == null) {
							
							// Try to find a sheet matching the expression in different ways
							sheet = ValidationContext.matchExpression(sheetsByName.entrySet(), Map.Entry::getKey, sheetNameExpr).map(Map.Entry::getValue).orElse(null);
							
						}
						
					}
					
				}
				
				if ( sheet == null) {
					// Can't use this field because the sheet could not be found
					continue;
				}

				DataSerie dataSerie = new DataSerie();
				dataSerie.sheet = sheet;
				
				if (fieldMapping.getRowIndex()!=null) {
					
					// If we have a specific row, this field may be one singular value (if we also
					// have one specific column) or it may be an entire row of values (if we don't
					// have one specific column)
					
					dataSerie.row = fieldMapping.getRowIndex();
					
					dataSerie.column = fieldMapping.getColumnIndex();
					
				}
				else {
					
					// If we have no specific row, we have to start with some row 
					Integer rowStart = firstRowPerSheet.get(sheet);
					
					Integer column = fieldMapping.getColumnIndex();
					if (column==null || column.intValue()<0) {
						// If we don't know which column to use, let's try with the 'expression for column names'
						if (fieldMapping.getColumnNameExpression()!=null 
								&& fieldMapping.getColumnNameExpression().trim().length()>0
								&& rowStart.intValue()>0) {
							
							// Try to find a column name matching the expression in different ways
							// Let's assume the row before data contains 'column titles'
							final Row header = sheet.getRow(rowStart-1);
							Iterable<Integer> colsInHeader = getColumnsInRow(sheet, rowStart);
							column = ValidationContext.matchExpression(colsInHeader, 
								/*toText*/colInHeader->{
									Cell cell = header.getCell(colInHeader);
									Object value = getCellValue(cell);
									return (value==null) ? "" : value.toString();
								}, 
								fieldMapping.getColumnNameExpression())
								.orElse(null);
							
						}
					}
					
					if (column!=null) {
						// Let's increment the 'rowStart' if it looks like a column title
						Row r = sheet.getRow(rowStart);
						Cell c = (r==null) ? null : r.getCell(column);
						Object value = getCellValue(c);
						String txt = (value==null) ? "" : value.toString();
						if (ValidationContext.matchExpression(Collections.singletonList(txt), Function.identity(), fieldMapping.getFieldName()).isPresent()
							|| ValidationContext.matchExpression(Collections.singletonList(txt), Function.identity(), fieldMapping.getColumnNameExpression()).isPresent()) {
							rowStart++;
						}
					}
					
					dataSerie.column = column;
					dataSerie.firstRow = rowStart;
					
				}
				
				mapDataSeries.put(fieldMapping, dataSerie);
			}
			
		} catch (IOException | EncryptedDocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	/**
	 * Given a sheet and a row index in the sheet, returns an 'iterable' of 'column positions' of filled cells in this row.
	 */
	public static Iterable<Integer> getColumnsInRow(Sheet sheet, int rowIndex) {
		Row row = sheet.getRow(rowIndex);
		if (row==null)
			return Collections.emptyList();
		return IntStream.rangeClosed(row.getFirstCellNum(),row.getLastCellNum()).boxed().collect(Collectors.toList());
	}
	
	@Override
	public DataIterator iterator() {
		if (path == null || !path.toFile().exists()) {
			return null;
		}

		if (mapDataSeries == null) {
			start();
		}

		try {

			return new DataIterator() {
				
				boolean moved = false;
				Map<String, Object> toRet = new LinkedHashMap<>();
				int countRecords;
				
				private void moveForward() {
					if (moved)
						return;
					
					for (int r=0; r<MAXIMUM_NUMBER_OF_EMPTY_ROWS_TO_BREAK; r++) {
					
						toRet.clear();
						
						int count_fixed_fields = 0;
						int count_variable_fields = 0;
						
						for (DocumentInputFieldMapping fieldMapping : documentInputSpec.getFields()) {
							
							DataSerie dataSerie = mapDataSeries.get(fieldMapping);
							if (dataSerie==null)
								continue;
							
							Object value = dataSerie.getNextValue(/*incrementAfter*/true);
							if (value==null)
								continue;
	
							toRet.put(fieldMapping.getFieldName(), value);									
	
							if (dataSerie.isConstant())
								count_fixed_fields++;
							else
								count_variable_fields++;
							
							toRet.put(fieldMapping.getFieldName(), value);
	
						}
						
						if (count_fixed_fields==0 && count_variable_fields==0) {
							// If we got no data in this iteration, let's try again the next row (or column)
							continue;
						}
						
						if (count_variable_fields==0 && countRecords>0) {
							// If we got only constant data, let's try again the next row (or column), unless
							// this is the first iteration (maybe all the data is fixed)
							continue;
						}
						
						// If we got here, we have data to return
						moved = true;
						countRecords++;
						return;
					}
					
					// If we got here, there is no data left
					moved = true;
					toRet.clear();

				}

				@Override
				public Map<String, Object> next() {
					if (!moved)
						moveForward();
					
					moved = false;
					
					return toRet;
				}

				@Override
				public boolean hasNext() {
					if (!moved)
						moveForward();
					return !toRet.isEmpty();
				}

				@Override
				public void close() {
					if (workbook != null) {
						try {
							workbook.close();
						} catch (Exception e) {
						}
					}
				}
			};

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Extract and return value from a given cell
	 * 
	 * @param cell	A given cell to extract it's value
	 * @return	A cell value
	 */
	protected static Object getCellValue(Cell cell) {
		
		if ( cell == null )
			return null;
		
		CellType type = cell.getCellType();
		
		if ( CellType.BLANK.equals(type) ) 
			return null;
		
		if ( CellType.ERROR.equals(type) ) { 
			return null;
		}
		
		if ( CellType.BOOLEAN.equals(type) ) { 
			return cell.getBooleanCellValue();
		}
		
		if ( CellType.NUMERIC.equals(type) ) { 
			return cell.getNumericCellValue();
		}
		
		if ( CellType.FORMULA.equals(type) ) { 
			try {
				return cell.getStringCellValue();
			} catch (Exception e) {
				return null;
			}
		}
		
		return cell.getStringCellValue();
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}
	
	/**
	 * References for retrieving data for a specific field
	 * @author Gustavo Figueiredo
	 */
	private static class DataSerie {

		/**
		 * Specific Sheet for retrieving data
		 */
		Sheet sheet;
		
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
		 * First for for iterating values in the same column.
		 */
		Integer firstRow;
		
		/**
		 * Incremented if this particular field has different values in the same row (different columns)
		 */
		Integer currentColumn;
		
		/**
		 * Incremented if this particular field has different values in the same column (different rows)
		 */
		Integer currentRow;
		
		boolean isSameRow() {
			return row!=null && row.intValue()>=0;
		}
		
		boolean isSameColumn() {
			return column!=null && column.intValue()>=0;
		}
		
		boolean isConstant() {
			return isSameColumn() && isSameRow();
		}
		
		Object getNextValue(boolean incrementAfter) {
			if (isConstant()) {
				Row r = sheet.getRow(row);
				if (r==null)
					return null;
				Cell c = r.getCell(column);
				if (c==null)
					return null;
				return getCellValue(c);
			}
			else if (isSameColumn()) {
				if (currentRow==null)
					currentRow = firstRow;
				try {
					Row r = sheet.getRow(currentRow);
					if (r==null)
						return null;
					Cell c = r.getCell(column);
					if (c==null)
						return null;
					return getCellValue(c);
				}
				finally {
					if (incrementAfter)
						currentRow++;
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
					return getCellValue(c);
				}
				finally {
					if (incrementAfter)
						currentColumn++;
				}				
			}
			return null;
		}
	}

	/**
	 * Locate the 'best guess' for the 'first line' in a worksheet.<BR>
	 * Use the following heuristic:<BR>
	 * The 'best guess for the first line' is the one with most of the cells
	 * filled (counts non blank cells in a row). Suppose the first filled row
	 * corresponds to a 'header' with 'column titles'.
	 */
	public static int guessFirstRow(int sheet, Workbook workbook) {
		int tolerance = 20;
		Sheet s = workbook.getSheetAt(sheet);
		int line;
		int count = 0; // count empty rows
		int min = s.getFirstRowNum();
		int max = s.getLastRowNum();
		int first_best = 0; // the row with the most filled cells
		int rank_best = 0; // the number of filled cells in the 'first_best' row 
		int after_best = 0; // counts how many rows are following the 'first_best' row
		for (line = min; line <= max; line++) {
			Row r = s.getRow(line);
			if (r == null) {
				if (count++ < tolerance)
					continue;
				break;
			}
			int first_col = r.getFirstCellNum();
			int last_col = r.getLastCellNum();
			int rank = 0; // counts non empty cells
			int null_c = 0; // counts successive null cells
			for (int ci = first_col; ci <= last_col && ci >= 0; ci++) {
				Cell c = r.getCell(ci);
				if (c != null && c.getCellType() != CellType.BLANK) {
					rank++;
					null_c = 0;
				} else {
					if (++null_c > 5)
						break;
				}
			}
			if (rank == 0) {
				if (count++ < tolerance)
					continue;
				break;
			}
			if (rank > rank_best) {
				rank_best = rank;
				first_best = line;
				after_best = 0;
			}
			else {
				if (after_best++ >= tolerance)
					break;
			}
		}
		if (rank_best > 0)
			first_best++; // skip the supposed header line
		return first_best;
	}

}
