/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.validator.parsers;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
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
import org.idb.cacao.api.templates.DocumentInputFieldMapping;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.FieldType;
import org.idb.cacao.api.utils.ParserUtils;

public class ExcelParser extends FileParserAdapter {
	
	private static final Logger log = Logger.getLogger(ExcelParser.class.getName());
	
	/**
	 * Maximum number of successive empty rows (for columnar data) or successive empty columns (for row data) for determining the
	 * data has finished.
	 */
	private static final int MAXIMUM_NUMBER_OF_EMPTY_ROWS_TO_BREAK = 10;

	private int MINIMUM_NUMBER_OF_SHEETS = 1;

	private int numberOfLines = -1;
	
	private DocumentTemplate documentTemplate;

	private Workbook workbook;
	
	private FormulaEvaluator evaluator;
	
	/**
	 * For each 'DocumentInputFieldMapping' keep a 'DataSerie' object for iterating through the corresponding data
	 */
	private Map<DocumentInputFieldMapping, ExcelDataSerie> mapDataSeries;
	
	/**
	 * Names of the fields that are required according to the document template (except those that are mapped over filenames)
	 */
	private Set<String> requiredFields;

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
				log.log(Level.FINEST, e.getMessage(), e);
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
			evaluator = workbook.getCreationHelper().createFormulaEvaluator();

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
			log.log(Level.SEVERE, String.format("Error trying to read Excel file %s", path.getFileName()), e);
		}
		
		if (documentTemplate!=null && !mapDataSeries.isEmpty()) {
			// Include indications of 'metrics' in order to avoid repeating them if we have multiple cardinality for other fields
			Set<String> metricsNames = documentTemplate.getFields().stream().filter(f->FieldType.DECIMAL.equals(f.getFieldType()))
				.map(DocumentField::getFieldName).collect(Collectors.toCollection(()->new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
			if (!metricsNames.isEmpty()) {
				for (Map.Entry<DocumentInputFieldMapping, ExcelDataSerie> entry:mapDataSeries.entrySet()) {
					boolean isMetric = metricsNames.contains(entry.getKey().getFieldName());
					if (isMetric)
						entry.getValue().metric = isMetric;
				}
			}
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
		final UnaryOperator<String> uniformNames = name->
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
					Collections.addAll(allReferencedCells, aref.getAllReferencedCells());
				}
				catch (IllegalArgumentException ex) {
					if (!AreaReference.isContiguous(formula)) {
						// Common error: 'References passed to the AreaReference must be contiguous'
						AreaReference[] arefs = AreaReference.generateContiguous(workbook.getSpreadsheetVersion(), formula);
						for (AreaReference aref: arefs) {
							Collections.addAll(allReferencedCells, aref.getAllReferencedCells());
						}
					}
					else {
						throw ex;
					}
				}
			}
			if (!allReferencedCells.isEmpty()) {
				ExcelDataSerie dataSerie = new ExcelDataSerie();
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
			catch (Exception ex) {
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
					cellRangeForSheet.forEach(cellAddress->
						cellReferences.add(new CellReference(sheet.getSheetName(),cellAddress.getRow(),cellAddress.getColumn(),false,false))
					);
				}
				if (!cellReferences.isEmpty()) {
					
					ExcelDataSerie dataSerie = new ExcelDataSerie();
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
				List<ExcelValue> matchingValues = new LinkedList<>();
				for (Sheet s: sheetsToMatch) {
					Iterator<Row> rows = s.iterator();
					while (rows.hasNext()) {
						Row row = rows.next();
						Iterator<Cell> cells = row.cellIterator();
						while (cells.hasNext()) {
							Cell cell = cells.next();
							if (!CellType.STRING.equals(cell.getCellType()))
								continue;
							String cellValue;
							try {
								cellValue = cell.getStringCellValue();
							} catch (Exception ex) {
								continue;
							}
							if (cellValue==null || cellValue.length()==0)
								continue;
							Matcher matcher = pattern.matcher(cellValue);
							if (!matcher.find())
								continue;
							String extractedValue = (matcher.groupCount()>0) ? matcher.group(1) : matcher.group();
							if (extractedValue==null || extractedValue.length()==0)
								continue;
							matchingValues.add(new ExcelValue(s,row.getRowNum(),cell.getColumnIndex(),extractedValue));
						} // LOOP over cells for pattern matching
					} // LOOP over rows for pattern matching
				} // LOOP over sheets for pattern matching
				if (!matchingValues.isEmpty()) {
					ExcelDataSerie dataSerie = new ExcelDataSerie();
					dataSerie.individualValues = matchingValues.toArray(new ExcelValue[0]);
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

		ExcelDataSerie dataSerie = new ExcelDataSerie();
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
							Object value = getCellValue(cell, evaluator);
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
				Object value = getCellValue(c, evaluator);
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
			log.log(Level.SEVERE, String.format("Error trying to iterate data from Excel file %s", path.getFileName()), e);
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
		final Function<ExcelDataSerie,String> spreadDescription = dataSerie-> 
			dataSerie.getSheets().stream().map(s->String.format("%s:%d",s.getSheetName(),dataSerie.getSize(s))).collect(Collectors.joining("|"));
		
		// Let's keep the previous 'data spread' in order to check for any different one
		String dataSpreadPreviousField = null;
		
		// Check if we have different data spreads for different non-constant fields
		for (DocumentInputFieldMapping fieldMapping : documentInputSpec.getFields()) {
			ExcelDataSerie dataSerie = mapDataSeries.get(fieldMapping);
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
			if(!hasNext()){
				throw new NoSuchElementException();
			}			
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
					log.log(Level.FINEST, e.getMessage(), e);
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
				
				int countFixedFields = 0;
				int countVariableFields = 0;
				
				for (DocumentInputFieldMapping fieldMapping : documentInputSpec.getFields()) {
					
					ExcelDataSerie dataSerie = mapDataSeries.get(fieldMapping);
					if (dataSerie==null)
						continue;
					
					Object value = dataSerie.getNextValue(/*incrementAfter*/true, evaluator);
					if (value==null)
						continue;

					toRet.put(fieldMapping.getFieldName(), value);									

					if (dataSerie.isConstant())
						countFixedFields++;
					else
						countVariableFields++;
					
					toRet.put(fieldMapping.getFieldName(), value);

				}
				
				if (countFixedFields==0 && countVariableFields==0) {
					// If we got no data in this iteration, let's try again the next row (or column)
					continue;
				}
				
				if (countVariableFields==0 && countRecords>0) {
					// If we got only constant data, let's try again the next row (or column), unless
					// this is the first iteration (maybe all the data is fixed)
					continue;
				}
				
				// If we got here, we have data to return
				moved = true;
				toRet.put(CURRENT_LINE, countRecords);
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
					boolean hasEnoughDataForOneRecord = feedValuesFromRow(cellIterator);
					if (hasEnoughDataForOneRecord) {
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
					//continue; // try next sheet in the same workbook 
				} 				
				else {
					break; // no more data
				}
			}

			// If we got here, there is no data left
			moved = true;
			toRet = new HashMap<>();
		}
		
		/**
		 * Collects data and store inside 'toRet'
		 * @return Returns TRUE if we have enough data to proceed, returns FALSE otherwise.
		 */
		private boolean feedValuesFromRow(Iterator<Cell> cellIterator) {
			if (!cellIterator.hasNext())
				return false;
			
			toRet = new HashMap<>(); 
			
			int countVariableFieldsLocal = 0;
			AtomicBoolean localIndication = new AtomicBoolean();
			IdentityHashMap<DocumentInputFieldMapping, ExcelDataSerie> usedFields = new IdentityHashMap<>();

			while (cellIterator.hasNext()) {
				
				Cell currentCell = cellIterator.next();
				
				toRet.put(CURRENT_LINE, currentCell.getRowIndex());
								
				for (DocumentInputFieldMapping fieldMapping : documentInputSpec.getFields()) {
					
					ExcelDataSerie dataSerie = mapDataSeries.get(fieldMapping);
					if (dataSerie==null)
						continue;
					
					Object value = dataSerie.getValue(currentCell, localIndication, evaluator);
					if (value==null)
						continue;

					if (!dataSerie.isConstant() && localIndication.get()) {
						countVariableFieldsLocal++;
						
					}
					
					toRet.put(fieldMapping.getFieldName(), value);
					
					usedFields.put(fieldMapping, dataSerie);

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
			
			// If we got here, we have data to return as a 'projected record' (all collected data stored inside 'toRet')
			
			usedFields.values().forEach(ExcelDataSerie::setUsedPreviousValue);
			
			return true;
		}
	}
	
	/**
	 * Extract and return value from a given cell
	 * 
	 * @param cell	A given cell to extract it's value
	 * @param evaluator Object used for evaluating formulas.
	 * @return	A cell value
	 */
	protected static Object getCellValue(Cell cell, FormulaEvaluator evaluator) {
		
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
			if (evaluator==null)
				return null;
			try {
				CellValue cellValue = evaluator.evaluate(cell);
				String formatted = cellValue.formatAsString();
				if ("#REF!".equals(formatted)) {
					return null;
				}
				if ("#VALUE!".equals(formatted)) {
					return null;
				}
				return formatted;
			}
			catch (Exception ex) {
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
				log.log(Level.FINEST, e.getMessage(), e);
			}
		}
	}
	
	/**
	 * Returns TRUE if all the cell references corresponds to the same row at the same sheet
	 */
	static boolean isSameRow(CellReference[] refs) {
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
	static boolean isSameColumn(CellReference[] refs) {
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
	static boolean isSameRow(ExcelValue[] refs) {
		if (refs==null || refs.length==0)
			return false;
		String sheetName = null;
		int row = -1;
		for (ExcelValue ref: refs) {
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
	static boolean isSameColumn(ExcelValue[] refs) {
		if (refs==null || refs.length==0)
			return false;
		String sheetName = null;
		int col = -1;
		for (ExcelValue ref: refs) {
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
	private static boolean isSameSheet(ExcelValue[] refs) {
		if (refs==null || refs.length==0)
			return false;
		String sheetName = null;
		for (ExcelValue ref: refs) {
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
		int firstBest = 0; // the row with the most filled cells
		int rankBest = 0; // the number of filled cells in the 'first_best' row 
		int afterBest = 0; // counts how many rows are following the 'first_best' row
		for (line = min; line <= max; line++) {
			Row r = s.getRow(line);
			if (r == null) {
				if (count++ < tolerance)
					continue;
				break;
			}
			int firstCol = r.getFirstCellNum();
			int lastCol = r.getLastCellNum();
			int rank = 0; // counts non empty cells
			int nullCells = 0; // counts successive null cells
			for (int ci = firstCol; ci <= lastCol && ci >= 0; ci++) {
				Cell c = r.getCell(ci);
				if (c != null && c.getCellType() != CellType.BLANK) {
					rank++;
					nullCells = 0;
				} else {
					if (++nullCells > 5)
						break;
				}
			}
			if (rank == 0) {
				if (count++ < tolerance)
					continue;
				break;
			}
			if (rank > rankBest) {
				rankBest = rank;
				firstBest = line;
				afterBest = 0;
			}
			else {
				if (afterBest++ >= tolerance)
					break;
			}
		}
		if (rankBest > 0)
			firstBest++; // skip the supposed header line
		return firstBest;
	}

}
