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
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.idb.cacao.api.templates.DocumentInput;
import org.idb.cacao.api.templates.DocumentInputFieldMapping;

public class ExcelParser implements FileParser {

	private int MINIMUM_NUMBER_OF_SHEETS = 1;

	private int numberOfLines = -1;

	private Path path;

	private DocumentInput documentInputSpec;

	private Map<Integer, Sheet> sheetsByNumber;

	private Map<String, Sheet> sheetsByName;

	private Workbook workbook;

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

		try {
			FileInputStream inputStream = new FileInputStream(path.toFile());
			Workbook workbook = WorkbookFactory.create(inputStream);

			// Check if the workbook has all sheets needed
			if (workbook.getNumberOfSheets() < MINIMUM_NUMBER_OF_SHEETS)
				workbook.createSheet("Sample");

			sheetsByNumber = new LinkedHashMap<>();
			sheetsByName = new LinkedHashMap<>();

			for (int i = 0; i < workbook.getNumberOfSheets(); i++) {

				Sheet sheet = workbook.getSheetAt(i);
				sheetsByNumber.put(i, sheet);
				sheetsByName.put(sheet.getSheetName(), sheet);

				numberOfLines = Math.max(numberOfLines, sheet.getLastRowNum());

			}

		} catch (IOException | EncryptedDocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public DataIterator iterator() {
		if (path == null || !path.toFile().exists()) {
			return null;
		}

		if (sheetsByNumber == null || sheetsByName == null) {
			start();
		}

		try {

			return new DataIterator() {
				
				//Skips first line
				int actualLine = 1;

				@Override
				public Map<String, Object> next() {
					
					Map<String, Object> toRet = new LinkedHashMap<>();

					for (DocumentInputFieldMapping fieldMapping : documentInputSpec.getFields()) {
						
						Object value = null;
						
						int sheetIndex = fieldMapping.getSheetIndex();
						Sheet sheet = null;
						if ( sheetIndex >= 0 )
							sheet = sheetsByNumber.get(sheetIndex);
						
						if ( sheet == null ) {							
							//TODO Get sheet by name							
						}

						if ( sheet != null ) {
							
							Row row = sheet.getRow(actualLine);
							
							if ( row != null ) {
								int cellNumber = fieldMapping.getColumnIndex();
								if ( cellNumber < row.getLastCellNum() ) {
									Cell cell = row.getCell(cellNumber);
									value = getCellValue(cell);
									toRet.put(fieldMapping.getFieldName(), value);									
								}
								else {
									
								}
								 
							}
							
						}
						
						toRet.put(fieldMapping.getFieldName(), value);

					}
					actualLine++;
					return toRet;
				}

				@Override
				public boolean hasNext() {
					return actualLine <= numberOfLines;
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
	protected Object getCellValue(Cell cell) {
		
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

}
