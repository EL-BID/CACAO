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
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DocumentInput;
import org.idb.cacao.api.templates.DocumentInputFieldMapping;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.utils.ParserUtils;

public class ExcelParser implements FileParser {
	
	private static final Logger log = Logger.getLogger(ExcelParser.class.getName());
	
	/**
	 * Maximum number of successive empty rows (for columnar data) or successive empty columns (for row data) for determining the
	 * data has finished.
	 */
	private static final int MAXIMUM_NUMBER_OF_EMPTY_ROWS_TO_BREAK = 10;

	private int MINIMUM_NUMBER_OF_SHEETS = 1;

	private int numberOfLines = -1;

	private Path path;

	private DocumentInput documentInputSpec;
	
	private DocumentTemplate documentTemplate;

	private Workbook workbook;
	
	/**
	 * For each 'DocumentInputFieldMapping' keep a 'DataSerie' object for iterating through the corresponding data
	 */
	private Map<DocumentInputFieldMapping, DataSerie> mapDataSeries;
	
	/**
	 * Names of the fields that are required according to the document template (except those that are mapped over filenames)
	 */
	private Set<String> requiredFields;

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

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.parsers.FileParser#setDocumentTemplate(org.idb.cacao.api.templates.DocumentTemplate)
	 */
	@Override
	public void setDocumentTemplate(DocumentTemplate template) {
		this.documentTemplate = template;
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
		
		Set<String> mappingsOverFilenames = documentInputSpec.getFields().stream()
				.filter(m->m.getFileNameExpression()!=null && m.getFileNameExpression().trim().length()>0)
				.map(DocumentInputFieldMapping::getFieldName)
				.collect(Collectors.toCollection(()->new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
		
		requiredFields = (documentTemplate==null) ? null
			: documentTemplate.getRequiredFields().stream().map(DocumentField::getFieldName)
			.filter(n->!mappingsOverFilenames.contains(n))
			.collect(Collectors.toCollection(()->new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
		
		mapDataSeries = new IdentityHashMap<>();

		try {
			FileInputStream inputStream = new FileInputStream(path.toFile());
			workbook = WorkbookFactory.create(inputStream);

			// Check if the workbook has all sheets needed
			if (workbook.getNumberOfSheets() < MINIMUM_NUMBER_OF_SHEETS)
				workbook.createSheet("Sample");

			Map<Integer, Sheet> sheetsByNumber = new HashMap<>();
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
				
				addDataSerieBasedOnNamedCell(fieldMapping, sheetsByName);
				
				if ( sheet == null && (fieldMapping.getSheetIndex()==null || fieldMapping.getSheetIndex().intValue()<=0)
						&& (fieldMapping.getSheetNameExpression()==null || fieldMapping.getSheetNameExpression().trim().length()==0)) {
					// If we don't have a specific sheet for this field, let's use all the available sheets
					for (Sheet s: sheetsByNumber.values()) {
						addDataSerie(fieldMapping, s, sheetsByName, firstRowPerSheet);
					}
				}
				else {
					addDataSerie(fieldMapping, sheet, sheetsByName, firstRowPerSheet);
				}
			}
			
		} catch (IOException | EncryptedDocumentException e) {
			log.log(Level.SEVERE, "Error trying to read Excel file " + path.getFileName(), e);
		}

	}
	
	/**
	 * If the input field mapping includes a 'cell name' criteria, this method will feed 'mapDataSeries' with
	 * additional 'DataSerie' object regarding this expression.<BR>
	 * It may consider the cell name in different ways (e.g.: as it was a 'name' for 'named cells' or as it
	 * was 'cell addresses ranges' - like 'A:A' - or as it was some regular expression to match individual cell contents). 
	 */
	private void addDataSerieBasedOnNamedCell(DocumentInputFieldMapping fieldMapping, 
			Map<String, Sheet> sheetsByName) {
		
		if (fieldMapping.getCellName()==null || fieldMapping.getCellName().trim().length()==0) 
			return;
			
		// If we have an expression for named cell intervals, let's look for this
		
		// This function will make irrelevant any difference regarding symbols and spaces. Will also make irrelevant differences
		// in diacritical marks.
		final Function<String,String> uniformNames = (name)->
			Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
				.replaceAll("[^A-Za-z\\d]", "");
		
		final String nameToLookAfter = uniformNames.apply(fieldMapping.getCellName());
		
		List<? extends Name> namedCells = workbook.getAllNames().stream()
				.filter(n->String.CASE_INSENSITIVE_ORDER.compare(nameToLookAfter, uniformNames.apply(n.getNameName()))==0)
				.collect(Collectors.toList());
		
		if (namedCells!=null && !namedCells.isEmpty()) {
			List<CellReference> allReferencedCells = new LinkedList<>();
			for (Name namedCell: namedCells) {
				String formula = namedCell.getRefersToFormula();
				try {
					AreaReference aref = new AreaReference(formula, workbook.getSpreadsheetVersion());
					for (CellReference ref: aref.getAllReferencedCells()) {
						allReferencedCells.add(ref);
					}
				}
				catch (IllegalArgumentException ex) {
					if (!AreaReference.isContiguous(formula)) {
						// Common error: 'References passed to the AreaReference must be contiguous'
						AreaReference[] arefs = AreaReference.generateContiguous(workbook.getSpreadsheetVersion(), formula);
						for (AreaReference aref: arefs) {
							for (CellReference ref: aref.getAllReferencedCells()) {
								allReferencedCells.add(ref);
							}							
						}
					}
					else {
						throw ex;
					}
				}
			}
			if (!allReferencedCells.isEmpty()) {
				DataSerie dataSerie = new DataSerie();
				dataSerie.cellReferences = allReferencedCells.toArray(new CellReference[0]);
				if (isSameSheet(dataSerie.cellReferences)) {
					String sheetName = dataSerie.cellReferences[0].getSheetName();
					if (sheetName!=null) {
						Sheet sheet = sheetsByName.get(sheetName);
						if (sheet!=null)
							dataSerie.sheet = sheet;
					}
				}
				else {
					dataSerie.sheets = sheetsByName;
				}
				mapDataSeries.put(fieldMapping, dataSerie);				
			}
		}
		else {
			// If the provided name does not correspond to a named cell, let's try to find out if it corresponds to
			// a cell range
			CellRangeAddress cellRange;
			try {
				cellRange = CellRangeAddress.valueOf(fieldMapping.getCellName());
			}
			catch (Throwable ex) {
				cellRange = null;
			}
			if (cellRange!=null) {
				List<CellReference> cellReferences = new LinkedList<>();
				for (Sheet sheet: sheetsByName.values()) {
					CellRangeAddress cellRangeForSheet = cellRange.copy();
					if (cellRangeForSheet.getFirstColumn()<0)
						continue;
					if (cellRangeForSheet.getFirstRow()<0)
						cellRangeForSheet.setFirstRow(0);
					if (cellRangeForSheet.getLastRow()<0)
						cellRangeForSheet.setLastRow(sheet.getLastRowNum());
					if (cellRangeForSheet.getLastRow()<cellRangeForSheet.getFirstRow())
						continue;
					if (cellRangeForSheet.getLastColumn()<cellRangeForSheet.getFirstColumn())
						continue;
					cellRangeForSheet.forEach(cellAddress->{
						cellReferences.add(new CellReference(sheet.getSheetName(),cellAddress.getRow(),cellAddress.getColumn(),false,false));
					});
				}
				if (!cellReferences.isEmpty()) {
					
					DataSerie dataSerie = new DataSerie();
					dataSerie.cellReferences = cellReferences.toArray(new CellReference[0]);
					if (isSameSheet(dataSerie.cellReferences)) {
						dataSerie.sheet = sheetsByName.get(dataSerie.cellReferences[0].getSheetName());
					}
					else {
						dataSerie.sheets = sheetsByName;
					}
					mapDataSeries.put(fieldMapping, dataSerie);
				}
			}
			// If the provided name does not correspond to a named cell nor a cell range, let's try to find out if
			// it corresponds to a regular expression with capture group
			if (cellRange==null && ParserUtils.mayBeRegexWithCaptureGroup(fieldMapping.getCellName())) {
				Pattern pattern = Pattern.compile(fieldMapping.getCellName(), Pattern.CASE_INSENSITIVE);
				Collection<Sheet> sheetsToMatch = sheetsByName.values();
				List<IndividualValue> matchingValues = new LinkedList<>();
				for (Sheet s: sheetsToMatch) {
					Iterator<Row> rows = s.iterator();
					while (rows.hasNext()) {
						Row row = rows.next();
						Iterator<Cell> cells = row.cellIterator();
						while (cells.hasNext()) {
							Cell cell = cells.next();
							if (!CellType.STRING.equals(cell.getCellType()))
								continue;
							String cell_value;
							try {
								cell_value = cell.getStringCellValue();
							} catch (Throwable ex) {
								continue;
							}
							if (cell_value==null || cell_value.length()==0)
								continue;
							Matcher matcher = pattern.matcher(cell_value);
							if (!matcher.find())
								continue;
							String extracted_value = (matcher.groupCount()>0) ? matcher.group(1) : matcher.group();
							if (extracted_value==null || extracted_value.length()==0)
								continue;
							matchingValues.add(new IndividualValue(s,row.getRowNum(),cell.getColumnIndex(),extracted_value));
						} // LOOP over cells for pattern matching
					} // LOOP over rows for pattern matching
				} // LOOP over sheets for pattern matching
				if (!matchingValues.isEmpty()) {
					DataSerie dataSerie = new DataSerie();
					dataSerie.individualValues = matchingValues.toArray(new IndividualValue[0]);
					if (isSameSheet(dataSerie.individualValues)) {
						dataSerie.sheet = dataSerie.individualValues[0].getSheet();
					}
					else {
						dataSerie.sheets = sheetsByName;
					}
					mapDataSeries.put(fieldMapping, dataSerie);
				}
			}
		}
	}
	
	/**
	 * Add a new 'data serie' regarding one field mapping and one worksheet
	 */
	private void addDataSerie(DocumentInputFieldMapping fieldMapping, Sheet sheet,
			Map<String, Sheet> sheetsByName,
			Map<Sheet, Integer> firstRowPerSheet) {
		
		if ( sheet == null) {
			// Can't use this field because the sheet could not be found
			return;
		}

		if (fieldMapping.getCellName()!=null && fieldMapping.getCellName().trim().length()>0) {
			
			// This criteria should be configured by means of addDataSerieBasedOnNamedCell
			return;
			
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

			boolean differentSteps = hasMismatchSteps();
			
			if (differentSteps) {
				List<String> sheetNames = new LinkedList<>();
				workbook.sheetIterator().forEachRemaining(s->sheetNames.add(s.getSheetName()));
				mapDataSeries.values().forEach(d->d.optimizeForScanningAllCells(sheetNames));
				return new MultiStepsDataIterator();
			}
			
			else
				return new SameStepsDataIterator();

		} catch (Exception e) {
			log.log(Level.SEVERE, "Error trying to iterate data from Excel file " + path.getFileName(), e);
		}

		return null;
	}
	
	/**
	 * Verifies if we have different fields with different 'stepping'
	 */
	public boolean hasMismatchSteps() {
		if (mapDataSeries==null || mapDataSeries.isEmpty())
			return false;
		
		// Some simplified and compact way to 'describe' the data spread over different sheets
		final Function<DataSerie,String> spreadDescription = (dataSerie)->{
			return dataSerie.getSheets().stream().map(s->String.format("%s:%d",s.getSheetName(),dataSerie.getSize(s))).collect(Collectors.joining("|"));
		};
		
		// Let's keep the previous 'data spread' in order to check for any different one
		String dataSpreadPreviousField = null;
		
		// Check if we have different data spreads for different non-constant fields
		for (DocumentInputFieldMapping fieldMapping : documentInputSpec.getFields()) {
			DataSerie dataSerie = mapDataSeries.get(fieldMapping);
			if (dataSerie==null)
				continue;
			if (dataSerie.isConstant())
				continue;
			if (dataSerie.isVoid())
				continue;
			String dataSpread = spreadDescription.apply(dataSerie);
			if (dataSpreadPreviousField==null)
				dataSpreadPreviousField = dataSpread;
			if (!dataSpreadPreviousField.equals(dataSpread))
				return true;
		}
		
		return false;
	}
	
	/**
	 * Base class for iterating over records in a Spreadsheet
	 */
	private abstract class ExcelDataIterator implements DataIterator {
		
		protected boolean moved = false;
		protected Map<String, Object> toRet;
		protected int countRecords;

		protected abstract void moveForward();

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
	}
	
	/**
	 * This is a 'simple data iterator' implementation. Assumes that with each 'step' for each field there is a corresponding 'step'
	 * for every other field at the same pace (i.e. assumes the data rows keeps 'alignment' throughout the iterations).
	 */
	private class SameStepsDataIterator extends ExcelDataIterator {
		
		@Override
		protected void moveForward() {
			if (moved)
				return;
			
			for (int r=0; r<MAXIMUM_NUMBER_OF_EMPTY_ROWS_TO_BREAK; r++) {
			
				toRet = new HashMap<>(); 
				
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
			toRet = new HashMap<>();

		}

	}

	/**
	 * This is a 'data iterator' implementation for situations where each field may differ from the other on the 'stepping', so we
	 * must coordinate all of them.
	 */
	private class MultiStepsDataIterator extends ExcelDataIterator {
		
		Iterator<Sheet> sheetIterator;
		
		Sheet currentSheet;
		
		Iterator<Row> rowIterator;
		
		Row currentRow;
		
		MultiStepsDataIterator() {
			sheetIterator = workbook.sheetIterator();
		}
		
		@Override
		protected void moveForward() {
			if (moved)
				return;

			Iterator<Cell> cellIterator = null;

			while (true) {
				if (cellIterator!=null) {
					boolean has_enough_data_for_one_record = feedValuesFromRow(cellIterator);
					if (has_enough_data_for_one_record) {
						// If we got here, we have data to return
						moved = true;
						countRecords++;					
						return;						
					}
				}
				if (rowIterator!=null && rowIterator.hasNext()) {
					currentRow = rowIterator.next();
					cellIterator = currentRow.cellIterator();
					continue; // try next row in the same sheet
				}
				if (sheetIterator.hasNext()) {
					currentSheet = sheetIterator.next();
					rowIterator = currentSheet.rowIterator();
					cellIterator = null;
					continue; // try next sheet in the same workbook 
				} 				
				else {
					break; // no more data
				}
			}

			// If we got here, there is no data left
			moved = true;
			toRet = new HashMap<>();
		}
		
		private boolean feedValuesFromRow(Iterator<Cell> cellIterator) {
			if (!cellIterator.hasNext())
				return false;
			
			toRet = new HashMap<>(); 

			int countVariableFieldsLocal = 0;
			AtomicBoolean localIndication = new AtomicBoolean();

			while (cellIterator.hasNext()) {
				
				Cell currentCell = cellIterator.next();
								
				for (DocumentInputFieldMapping fieldMapping : documentInputSpec.getFields()) {
					
					DataSerie dataSerie = mapDataSeries.get(fieldMapping);
					if (dataSerie==null)
						continue;
					
					Object value = dataSerie.getValue(currentCell, localIndication);
					if (value==null)
						continue;

					if (!dataSerie.isConstant() && localIndication.get()) {
						countVariableFieldsLocal++;
						
					}
					
					toRet.put(fieldMapping.getFieldName(), value);

				}				
			}
			
			if (countVariableFieldsLocal==0) {
				// If we got only constant data, or only data gathered from other places, let's try again the next row
				return false;
			}
			
			if (requiredFields!=null && !requiredFields.isEmpty()) {
				// If it's missing required field, let's keep searching for more
				boolean missingRequiredField = requiredFields.stream().anyMatch(n->!toRet.containsKey(n));
				if (missingRequiredField)
					return false;
			}
			
			// If we got here, we have data to return
			return true;
		}
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
			if (DateUtil.isCellDateFormatted(cell)) {
				return cell.getDateCellValue();
	        }
			return cell.getNumericCellValue();
		}
		
		if ( CellType.FORMULA.equals(type) ) { 
			try {
				String formatted = cell.getStringCellValue();
				if ("#REF!".equals(formatted)) {
					return null;
				}
				return formatted;
			} catch (Exception e) {
				return null;
			}
		}
		
		return cell.getStringCellValue();
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
		Integer currentColumn;
		
		/**
		 * Incremented if this particular field has different values in the same column (different rows)
		 */
		Integer currentRow;
		
		/**
		 * Specific cell references for retrieving data.
		 * IF NULL, ignores this field.
		 */
		CellReference[] cellReferences;
		
		/**
		 * Incremented if this particular field has different cell references
		 */
		Integer currentCellReference;
		
		/**
		 * Specific cell values already extracted from the workbook..
		 * IF NULL, ignores this field.
		 */
		IndividualValue[] individualValues;
		
		/**
		 * Incremented if this particular field has specified individual values
		 */
		Integer currentIndividualValue;
		
		/**
		 * Persist the boolean indications of the field mapping (prevent unnecessary recalculations)
		 */
		Boolean sameRow, sameColumn;
		
		/**
		 * Persist the constant value (prevent unnecessary recalculations)
		 */
		Object constantValue;
		
		/**
		 * Keep for each sheet name the corresponding order as long as we do a full scan over all sheets
		 */
		Map<String,Integer> sheetOrderNumbers;
		
		/**
		 * Use this object for sort and binarySearch over cellReferences as we do a full scan over all sheets
		 */
		Comparator<CellReference> cellReferencesComparator;
	
		/**
		 * Use this object for sort and binarySearch over individualValues as we do a full scan over all sheets
		 */
		Comparator<IndividualValue> individualValuesComparator;

		/**
		 * Informs this field has information gathered from the same sheet and the same row 
		 */
		boolean isSameRow() {
			if (sameRow!=null)
				return sameRow;
			sameRow = (row!=null && row.intValue()>=0) || ExcelParser.isSameRow(cellReferences) || ExcelParser.isSameRow(individualValues);
			return sameRow;
		}
		
		/**
		 * Informs this field has information gathered from the same sheet and the same column 
		 */
		boolean isSameColumn() {
			if (sameColumn!=null)
				return sameColumn;
			sameColumn = (column!=null && column.intValue()>=0) || ExcelParser.isSameColumn(cellReferences) || ExcelParser.isSameColumn(individualValues);
			return sameColumn;
		}
		
		/**
		 * Informs this field has information gathered from one singular cell (same sheet, same row, same column) 
		 */
		boolean isConstant() {
			return isSameColumn() && isSameRow();
		}
		
		/**
		 * Informs this field has no information at all
		 */
		boolean isVoid() {
			return !isConstant() 
					&& (cellReferences==null || cellReferences.length==0)
					&& (individualValues==null || individualValues.length==0)
					&& !isSameColumn() && !isSameRow();
		}
		
		/**
		 * Returns the sheets with information regarding this particular field
		 */
		Collection<Sheet> getSheets() {
			if (sheet!=null)
				return Collections.singleton(sheet);
			else
				return sheets.values();
		}
		
		/**
		 * Returns the expected number of information to return for a particular shield regarding this particular field
		 */
		int getSize(Sheet sheet) {
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
					int last_row = sheet.getLastRowNum();
					if (last_row<=0)
						return 0;
					if (firstRow==null)
						return last_row+1;
					else
						return Math.max(0,last_row-firstRow.intValue()+1);
				}
				catch (Throwable ex) {
					return 0;
				}
			}
			else if (isSameRow()) {
				try {
					Row r = sheet.getRow(row);
					if (r==null)
						return 0;
					short last_col = r.getLastCellNum();
					if (last_col<=0)
						return 0;
					return last_col;
				}
				catch (Throwable ex) {
					return 0;
				}
			}
			return 0;
		}
		
		/**
		 * Returns the next available information regarding this field, if any.
		 * @param incrementAfter If TRUE, increments internal counters in order to return another value next time
		 */
		Object getNextValue(boolean incrementAfter) {
			if (isConstant()) {
				if (constantValue!=null)
					return constantValue;
				if (row!=null && row.intValue()>=0) {
					Row r = sheet.getRow(row);
					if (r==null)
						return null;
					Cell c = r.getCell(column);
					if (c==null)
						return null;
					constantValue = getCellValue(c);
					return constantValue;
				}
				else if (cellReferences!=null && cellReferences.length>0) {
					Row r = sheet.getRow(cellReferences[0].getRow());
					if (r==null)
						return null;
					Cell c = r.getCell(cellReferences[0].getCol());
					if (c==null)
						return null;
					constantValue = getCellValue(c);
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
					Sheet sheet = (sheets!=null) ? sheets.getOrDefault(cref.getSheetName(), this.sheet) : this.sheet;
					if (sheet==null)
						return null;
					Row r = sheet.getRow(cref.getRow());
					if (r==null)
						return null;
					Cell c = r.getCell(cref.getCol());
					if (c==null)
						return null;
					return getCellValue(c);
				}
				finally {
					if (incrementAfter)
						currentCellReference++;
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
					if (incrementAfter)
						currentIndividualValue++;
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
		
		/**
		 * Tells we are going to scan every cell in every row in every sheet in the order they appear.
		 */
		void optimizeForScanningAllCells(List<String> sheetOrder) {
			
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
				individualValuesComparator = new Comparator<IndividualValue>() {
					@Override
					public int compare(IndividualValue c1, IndividualValue c2) {
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
		 */
		Object getValue(Cell cell, AtomicBoolean atLocation) {
			atLocation.set(false);
			if (isConstant()) {
				// If this field represents a constant, we should return always the same value regardless of the provided cell
				atLocation.set(true);
				return getNextValue(/*incrementAfter*/false);
			}
			else if (cellReferences!=null) {
				if (currentCellReference==null)
					currentCellReference = 0;
				if (currentCellReference.intValue()>=cellReferences.length) {
					return null;
				}
				CellReference requested_cell_ref = new CellReference(cell);
				CellReference cref = cellReferences[currentCellReference];
				if (cellReferencesComparator.compare(requested_cell_ref, cref)!=0) {
					int position = Arrays.binarySearch(cellReferences, /*fromIndex*/currentCellReference, /*toIndex*/cellReferences.length, requested_cell_ref, cellReferencesComparator);
					if (position<0) {
						// If binarySearch returned negative number, we need to find an approximation to the requested cell
						int in_range_element_position = -2-position;
						if (in_range_element_position<0)
							return null; // we did not reach the position for this field
						currentCellReference = in_range_element_position;
					}
					else {
						// If binarySearch returned non-negative number, this position holds the exact cell reference it was requested
						currentCellReference = position;
					}
					if (currentCellReference.intValue()>=cellReferences.length)
						return null; // we reached the end of mapped data
					cref = cellReferences[currentCellReference];
					// When scanning over multiple sheets, let's make a boundary across different sheets
					if (!cref.getSheetName().equals(cell.getSheet().getSheetName()))
						return null;
				}
				Sheet sheet = cell.getSheet();
				Row r = sheet.getRow(cref.getRow());
				if (r==null)
					return null;
				Cell c = r.getCell(cref.getCol());
				if (c==null)
					return null;
				atLocation.set(cellReferencesComparator.compare(requested_cell_ref, cref)==0);
				return getCellValue(c);
			}
			else if (individualValues!=null) {
				if (currentIndividualValue==null)
					currentIndividualValue = 0;
				if (currentIndividualValue.intValue()>=individualValues.length) {
					return null;
				}
				IndividualValue requested_cell_ref = new IndividualValue(cell, /*value*/null);
				IndividualValue cref = individualValues[currentIndividualValue];
				if (individualValuesComparator.compare(requested_cell_ref, cref)!=0) {
					int position = Arrays.binarySearch(individualValues, /*fromIndex*/currentIndividualValue, /*toIndex*/individualValues.length, requested_cell_ref, individualValuesComparator);
					if (position<0) {
						// If binarySearch returned negative number, we need to find an approximation to the requested cell
						int in_range_element_position = -2-position;
						if (in_range_element_position<0)
							return null; // we did not reach the position for this field
						currentIndividualValue = in_range_element_position;
					}
					else {
						// If binarySearch returned non-negative number, this position holds the exact cell reference it was requested
						currentIndividualValue = position;
					}
					if (currentIndividualValue.intValue()>=individualValues.length)
						return null; // we reached the end of mapped data
					cref = individualValues[currentIndividualValue];
					// When scanning over multiple sheets, let's make a boundary across different sheets
					if (!cref.getSheet().equals(cell.getSheet()))
						return null;
				}
				atLocation.set(individualValuesComparator.compare(requested_cell_ref, cref)==0);
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
				return getCellValue(cell);
			}
			else if (isSameRow()) {
				if (sheet!=null && !sheet.equals(cell.getSheet()))
					return null;
				if (row!=null && row.intValue()!=cell.getRowIndex())
					return null;
				if (cell.getColumnIndex()<1)
					return null;
				atLocation.set(true);
				return getCellValue(cell);
			}
			return null;
		}

	}
	
	/**
	 * This class holds information regarding one singular value and the corresponding cell reference
	 */
	private static class IndividualValue {
		
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
		
		IndividualValue(Sheet sheet, int rowNumber, int colNumber, String value) {
			this.sheet = sheet;
			this.rowNumber = rowNumber;
			this.colNumber = colNumber;
			this.value = value;
		}
		
		IndividualValue(Cell cell, String value) {
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
	
	/**
	 * Returns TRUE if all the cell references corresponds to the same row at the same sheet
	 */
	private static boolean isSameRow(CellReference[] refs) {
		if (refs==null || refs.length==0)
			return false;
		String sheetName = null;
		int row = -1;
		for (CellReference ref: refs) {
			String s = ref.getSheetName();
			if (s!=null) {
				if (sheetName==null)
					sheetName = s;
				else if (!sheetName.equals(s))
					return false; // different sheets means different rows				
			}
			int r = ref.getRow();
			if (row==-1)
				row = r;
			else if (row!=r)
				return false; // different rows
		}
		return true;
	}

	/**
	 * Returns TRUE if all the cell references corresponds to the same column at the same sheet
	 */
	private static boolean isSameColumn(CellReference[] refs) {
		if (refs==null || refs.length==0)
			return false;
		String sheetName = null;
		int col = -1;
		for (CellReference ref: refs) {
			String s = ref.getSheetName();
			if (s!=null) {
				if (sheetName==null)
					sheetName = s;
				else if (!sheetName.equals(s))
					return false; // different sheets means different columns				
			}
			int c = ref.getCol();
			if (col==-1)
				col = c;
			else if (col!=c)
				return false; // different columns
		}
		return true;
	}

	/**
	 * Returns TRUE if all the cell references corresponds to the same sheet
	 */
	private static boolean isSameSheet(CellReference[] refs) {
		if (refs==null || refs.length==0)
			return false;
		String sheetName = null;
		for (CellReference ref: refs) {
			String s = ref.getSheetName();
			if (s!=null) {
				if (sheetName==null)
					sheetName = s;
				else if (!sheetName.equals(s))
					return false; // different sheets			
			}
		}
		return true;
	}

	/**
	 * Returns TRUE if all the individual values corresponds to the same row at the same sheet
	 */
	private static boolean isSameRow(IndividualValue[] refs) {
		if (refs==null || refs.length==0)
			return false;
		String sheetName = null;
		int row = -1;
		for (IndividualValue ref: refs) {
			String s = ref.getSheetName();
			if (s!=null) {
				if (sheetName==null)
					sheetName = s;
				else if (!sheetName.equals(s))
					return false; // different sheets means different rows				
			}
			int r = ref.getRow();
			if (row==-1)
				row = r;
			else if (row!=r)
				return false; // different rows
		}
		return true;
	}

	/**
	 * Returns TRUE if all the individual values corresponds to the same column at the same sheet
	 */
	private static boolean isSameColumn(IndividualValue[] refs) {
		if (refs==null || refs.length==0)
			return false;
		String sheetName = null;
		int col = -1;
		for (IndividualValue ref: refs) {
			String s = ref.getSheetName();
			if (s!=null) {
				if (sheetName==null)
					sheetName = s;
				else if (!sheetName.equals(s))
					return false; // different sheets means different columns				
			}
			int c = ref.getCol();
			if (col==-1)
				col = c;
			else if (col!=c)
				return false; // different columns
		}
		return true;
	}

	/**
	 * Returns TRUE if all the cell references corresponds to the same sheet
	 */
	private static boolean isSameSheet(IndividualValue[] refs) {
		if (refs==null || refs.length==0)
			return false;
		String sheetName = null;
		for (IndividualValue ref: refs) {
			String s = ref.getSheetName();
			if (s!=null) {
				if (sheetName==null)
					sheetName = s;
				else if (!sheetName.equals(s))
					return false; // different sheets			
			}
		}
		return true;
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
