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
package org.idb.cacao.web.utils.generators;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DocumentInput;
import org.idb.cacao.api.templates.DocumentInputFieldMapping;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.FieldMapping;
import org.idb.cacao.api.utils.RandomDataGenerator;
import org.idb.cacao.api.utils.RandomDataGenerator.DomainTableRepository;

import com.mifmif.common.regex.Generex;

/**
 * Implementation of a file generator with random data in Excel file format
 * 
 * @author Gustavo Figueiredo
 *
 */
public class ExcelGenerator implements FileGenerator {
	
	private Path path;
	
	private DocumentTemplate template;
	
	private DocumentInput inputSpec;

	private Workbook workbook;
	
	private Sheet sheet;
	
	private long seed;
	
	private RandomDataGenerator randomGenerator;
	
	private int currentRowNumber;
	
	private Map<String, Integer> mapFieldNamesToColumnPositions;

	private DomainTableRepository domainTableRepository;
	
	private String generatedOriginalFileName;
	
	private CellStyle timestampCellStyle;
	
	private CellStyle dateCellStyle;

	private int firstRowOfData = -1;
	
	private int rowOfColumnHeaders = -1;
	
	private String fixedTaxpayerId;
	
	private Number fixedYear;

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.web.utils.generators.FileGenerator#getPath()
	 */
	@Override
	public Path getPath() {
		return path;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.web.utils.generators.FileGenerator#setPath(java.nio.file.Path)
	 */
	@Override
	public void setPath(Path path) {
		this.path = path;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.web.utils.generators.FileGenerator#getRandomSeed()
	 */
	@Override
	public long getRandomSeed() {
		return seed;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.web.utils.generators.FileGenerator#setRandomSeed(long)
	 */
	@Override
	public void setRandomSeed(long seed) {
		this.seed = seed;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.web.utils.generators.FileGenerator#getDocumentInputSpec()
	 */
	@Override
	public DocumentInput getDocumentInputSpec() {
		return inputSpec;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.web.utils.generators.FileGenerator#setDocumentInputSpec(org.idb.cacao.api.templates.DocumentInput)
	 */
	@Override
	public void setDocumentInputSpec(DocumentInput input) {
		this.inputSpec = input;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.web.utils.generators.FileGenerator#getDocumentTemplate()
	 */
	@Override
	public DocumentTemplate getDocumentTemplate() {
		return template;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.web.utils.generators.FileGenerator#setDocumentTemplate(org.idb.cacao.api.templates.DocumentTemplate)
	 */
	@Override
	public void setDocumentTemplate(DocumentTemplate template) {
		this.template = template;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.web.utils.generators.FileGenerator#getDomainTableRepository()
	 */
	@Override
	public DomainTableRepository getDomainTableRepository() {
		return domainTableRepository;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.web.utils.generators.FileGenerator#setDomainTableRepository(org.idb.cacao.api.utils.RandomDataGenerator.DomainTableRepository)
	 */
	@Override
	public void setDomainTableRepository(DomainTableRepository domainTableRepository) {
		this.domainTableRepository = domainTableRepository;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.web.utils.generators.FileGenerator#getFixedTaxpayerId()
	 */
	@Override
	public String getFixedTaxpayerId() {
		return fixedTaxpayerId;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.web.utils.generators.FileGenerator#getFixedTaxpayerId()
	 */
	@Override
	public void setFixedTaxpayerId(String fixedTaxpayerId) {
		this.fixedTaxpayerId = fixedTaxpayerId;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.web.utils.generators.FileGenerator#getFixedTaxpayerId()
	 */
	@Override
	public Number getFixedYear() {
		return fixedYear;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.web.utils.generators.FileGenerator#getFixedTaxpayerId()
	 */
	@Override
	public void setFixedYear(Number fixedYear) {
		this.fixedYear = fixedYear;
	}

	public int getFirstRowOfData() {
		return firstRowOfData;
	}

	public int getRowOfColumnHeaders() {
		return rowOfColumnHeaders;
	}

	public RandomDataGenerator getRandomGenerator() {
		return randomGenerator;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.web.utils.generators.FileGenerator#start()
	 */
	@Override
	public void start() throws Exception {
		
		if (path == null 
				|| template == null || template.getFields()==null || template.getFields().isEmpty() 
				|| inputSpec == null || inputSpec.getFields()==null || inputSpec.getFields().isEmpty()) {
			return;
		}

		workbook = new XSSFWorkbook();
		
		// Creates a new spreadsheet
		
		String anySheetNameExpression = inputSpec.getFields().stream().filter(f->f.getSheetNameExpression()!=null && f.getSheetNameExpression().trim().length()>0)
				.findFirst().map(DocumentInputFieldMapping::getSheetNameExpression).orElse(null);
		if (anySheetNameExpression!=null) {
			Generex gen = new Generex(anySheetNameExpression);
			gen.setSeed(seed);
			sheet = workbook.createSheet(gen.random());
		}
		else {
			sheet = workbook.createSheet(template.getName());
		}
		
		// Cell styles for specific data formats
		CreationHelper createHelper = workbook.getCreationHelper();
		dateCellStyle = workbook.createCellStyle();
		dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("m/d/yy"));
		timestampCellStyle = workbook.createCellStyle();
		timestampCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("m/d/yy h:mm"));
		
		// Writes a title inside the spreadsheed (first row)
		
		RichTextString title = workbook.getCreationHelper().createRichTextString("Random data generated as "+template.getName());
		Font fontTitle = workbook.createFont();
		fontTitle.setBold(true);
		fontTitle.setColor(IndexedColors.BLUE.getIndex());
		fontTitle.setFontHeightInPoints((short)16);
		title.applyFont(fontTitle);
		sheet.createRow(0).createCell(0, CellType.STRING).setCellValue(title);
		sheet.addMergedRegion(new CellRangeAddress(/*firstRow*/0, /*lastRow*/0, /*firstCol*/0, /*lastCol*/4));

		// Writes the column headers giving names that matches the expressions configured
		
		rowOfColumnHeaders = 1;
		Row rowHeader = sheet.createRow(rowOfColumnHeaders);
		
		Font fontHeader = workbook.createFont();
		fontHeader.setBold(true);
		fontHeader.setColor(IndexedColors.BLACK.getIndex());
		
		CellStyle styleHeader = workbook.createCellStyle();
		styleHeader.setFont(fontHeader);
		styleHeader.setBorderBottom(BorderStyle.THIN);
		styleHeader.setBorderTop(BorderStyle.THIN);
		styleHeader.setBorderLeft(BorderStyle.THIN);
		styleHeader.setBorderRight(BorderStyle.THIN);
		styleHeader.setFillBackgroundColor(IndexedColors.YELLOW.getIndex());

		List<DocumentField> fieldsOrderedByName = template.getFields().stream()
				.sorted(Comparator.comparing(DocumentField::getFieldName)).collect(Collectors.toList());
		
		Map<String, DocumentInputFieldMapping> mapFields = inputSpec.getFields().stream()
				.collect(Collectors.toMap(
					/*keyMapper*/DocumentInputFieldMapping::getFieldName, 
					/*valueMapper*/Function.identity(), 
					/*mergeFunction*/(a,b)->a, 
					/*mapSupplier*/()->new TreeMap<>(String.CASE_INSENSITIVE_ORDER)));
		
		// All the column positions that are 'fixed' according to the template field mapping
		Set<Integer> usedColumnPositions = inputSpec.getFields().stream()
				.filter(f->f.getColumnIndex()!=null)
				.map(DocumentInputFieldMapping::getColumnIndex)
				.collect(Collectors.toSet());
		
		mapFieldNamesToColumnPositions = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		
		randomGenerator = new RandomDataGenerator(new Random(seed));
		randomGenerator.setDomainTableRepository(domainTableRepository);

		FilenameGenerator filenameGenerator = new FilenameGenerator(randomGenerator);
		filenameGenerator.setFixedTaxpayerId(fixedTaxpayerId);
		filenameGenerator.setFixedYear(fixedYear);
		
		for (DocumentField field: fieldsOrderedByName) {
			
			String fieldName = field.getFieldName();
			DocumentInputFieldMapping fieldMapping = mapFields.get(fieldName);
			if (fieldMapping==null)
				continue;
			
			if (fieldMapping.getFileNameExpression()!=null && fieldMapping.getFileNameExpression().trim().length()>0) {
				// If the field is generated from the filename, we need a different process for generating this
				filenameGenerator.addFilenameExpression(fieldMapping.getFileNameExpression(), field.getFieldMapping());
				continue;
			}

			// Find a proper column name for this field
			String columnName;

			String expr = fieldMapping.getColumnNameExpression();
			if (expr!=null && expr.trim().length()>0) {
				if (ValidationContext.matchExpression(Collections.singleton(fieldName), Function.identity(), expr).isPresent()) {
					// If the field name given in the template matches the field expression, let's keep this name for the column
					columnName = fieldName;
				}
				else {
					// If the field name given in the template does not match the field expression, let's try to find some name that does
					Generex generex = new Generex(expr);
					generex.setSeed(seed);
					columnName = generex.random();
					if (columnName==null || columnName.trim().length()==0 || !ValidationContext.matchExpression(Collections.singleton(columnName), Function.identity(), expr).isPresent()) {
						columnName = expr; // keep the wildcards characters
					}
				}
			}
			else {
				columnName = fieldName;
			}
			
			int columnPosition;
			
			if (fieldMapping.getColumnIndex()!=null) {
				// If this field has a fixed column position, let's use the configured column position
				columnPosition = fieldMapping.getColumnIndex();
			}
			else {
				// If this field does not have a fixed column position, let's use the next available column position
				columnPosition = 0;
				while (usedColumnPositions.contains(columnPosition))
					columnPosition++;
				usedColumnPositions.add(columnPosition);
			}
			
			Cell cellForFieldHeader = rowHeader.createCell(columnPosition, CellType.STRING);
			cellForFieldHeader.setCellStyle(styleHeader);
			cellForFieldHeader.setCellValue(columnName);
			
			mapFieldNamesToColumnPositions.put(fieldName, columnPosition);
			
		} // LOOP over fields
		
		// The actual data starts at row 2
		firstRowOfData = currentRowNumber = 2;
		
		if (filenameGenerator.isEmpty()) {
			generatedOriginalFileName = template.getName() + ".XLSX";
		}
		else {
			
			generatedOriginalFileName = filenameGenerator.generateFileName();

			if (!Pattern.compile("\\.XLS[XM]?$", Pattern.CASE_INSENSITIVE).matcher(generatedOriginalFileName).find())
				generatedOriginalFileName += ".XLSX";
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.web.utils.generators.FileGenerator#getOriginalFileName()
	 */
	@Override
	public String getOriginalFileName() {
		return generatedOriginalFileName;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.web.utils.generators.FileGenerator#addRandomRecord()
	 */
	@Override
	public void addRandomRecord() {
		Map<String, Object> record = new HashMap<>();

		for (DocumentField field: template.getFields()) {
			
			if (!mapFieldNamesToColumnPositions.containsKey(field.getFieldName()))
				continue;
			
			Object value;
			if (FieldMapping.TAXPAYER_ID.equals(field.getFieldMapping()) && fixedTaxpayerId!=null)
				value = fixedTaxpayerId;
			else if (FieldMapping.TAX_YEAR.equals(field.getFieldMapping()) && fixedYear!=null)
				value = fixedYear;
			else
				value = randomGenerator.nextRandom(field);
			if (value==null) {
				continue; // unsupported field type won't be generated
			}
			
			record.put(field.getFieldName(), value);
			
		}
		
		addRecord(record);
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.web.utils.generators.FileGenerator#addRecord(java.util.Map)
	 */
	@Override
	public void addRecord(Map<String, Object> record) {
		if (record==null || record.isEmpty())
			return;
		
		Row rowData = sheet.createRow(currentRowNumber++);

		for (Map.Entry<String, Object> field: record.entrySet()) {
			
			Integer columnPosition = mapFieldNamesToColumnPositions.get(field.getKey());
			if (columnPosition==null)
				continue;
			
			Object value = field.getValue();
			if (value==null || "".equals(value)) {
				rowData.createCell(columnPosition, CellType.BLANK);
			}
			else if ((value instanceof Double) || (value instanceof Float)) {
				rowData.createCell(columnPosition, CellType.NUMERIC).setCellValue(((Number)value).doubleValue());				
			}
			else if (value instanceof LocalDate) {
				Cell cell = rowData.createCell(columnPosition);
				cell.setCellValue(ValidationContext.toDate(value));
				cell.setCellStyle(dateCellStyle);
			}
			else if (value instanceof OffsetDateTime) {
				Cell cell = rowData.createCell(columnPosition);
				cell.setCellValue(ValidationContext.toDate(value));
				cell.setCellStyle(timestampCellStyle);				
			}
			else {
				rowData.createCell(columnPosition, CellType.STRING).setCellValue(ValidationContext.toString(value));
			}
			
		} // LOOP over each field
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		
		if (path == null || workbook == null) {
			return;
		}

		try (OutputStream fileOut = new BufferedOutputStream(new FileOutputStream(path.toFile()))) {
			workbook.write(fileOut);
		}
		
		if (randomGenerator!=null) {
			// If we are generating multiple documents with the same generator, update the seed for the next iteration
			seed = randomGenerator.getRandomGenerator().nextLong();
		}
	}
}
