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
package org.idb.cacao.web.rest.services;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.storage.FileSystemStorageService;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.api.templates.DocumentInput;
import org.idb.cacao.api.templates.DocumentInputFieldMapping;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.DomainEntry;
import org.idb.cacao.api.templates.DomainTable;
import org.idb.cacao.api.templates.FieldMapping;
import org.idb.cacao.api.templates.FieldType;
import org.idb.cacao.api.templates.TemplateArchetype;
import org.idb.cacao.api.utils.DateTimeUtils;
import org.idb.cacao.api.utils.ParserUtils;
import org.idb.cacao.api.utils.RandomDataGenerator;
import org.idb.cacao.mock_es.ElasticsearchMockClient;
import org.idb.cacao.web.controllers.services.DomainTableService;
import org.idb.cacao.web.repositories.DocumentTemplateRepository;
import org.idb.cacao.web.repositories.DomainTableRepository;
import org.idb.cacao.web.utils.generators.ExcelGenerator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for generating random data
 * 
 * @author Gustavo Figueiredo
 *
 */
@RunWith(JUnitPlatform.class)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@SpringBootTest( webEnvironment = WebEnvironment.RANDOM_PORT,
properties = {
"storage.incoming.files.original.dir=${java.io.tmpdir}/cacao/storage"
})
public class GenerateDocumentTests {

	private static ElasticsearchMockClient mockElastic;
	
	private static TemplateArchetype arch;

	private FileSystemStorageService fileStorageService;

	@Autowired
	private DocumentTemplateRepository templateRepository;

	@Autowired
	private DomainTableRepository domainTableRepository;

	@Autowired
	private DomainTableService domainService;

	@BeforeAll
	public static void beforeClass() throws Exception {

		int port = ElasticsearchMockClient.findRandomPort();
		mockElastic = new ElasticsearchMockClient(port);
		System.setProperty("es.port", String.valueOf(port));
		
		populateBuiltInDomainTable();
	}
	
	@AfterAll
	public static void afterClass() {
		if (mockElastic!=null)
			mockElastic.stop();
	}

	@BeforeEach
	public void init(@TempDir Path uploadFileDir) {
		fileStorageService = new FileSystemStorageService(uploadFileDir.toString());

		// This should enforce the existence of 'TEST-2 DEBIT/CREDIT' domain table due to the TemplateArchetype's specification
		domainService.assertDomainTablesForAchetype(arch);
	}
	
	private static void populateBuiltInDomainTable() {
		// Let's test with a specific Archetype with reference to a specific built-in domain table
		
		DomainTable builtInTable = new DomainTable("TEST-2 DEBIT/CREDIT","1.0")
				.withEntries(new DomainEntry("D", "account.debit"),
						new DomainEntry("C", "account.credit"));

		arch = new TemplateArchetype() {

			@Override
			public String getName() {
				return "Internal Archetype for Test";
			}

			@Override
			public List<DocumentField> getRequiredFields() {
				return Arrays.asList(
					new DocumentField()
					.withFieldName("DebitCredit")
					.withFieldType(FieldType.DOMAIN)
					.withDomainTableName(builtInTable.getName())
					.withDomainTableName(builtInTable.getVersion())
					.withDescription("This is an indication of whether this entry is a debit or a credit to the account")
					.withMaxLength(32)
					.withRequired(true)
					);
			}

			@Override
			public List<DomainTable> getBuiltInDomainTables() {
				return Arrays.asList(builtInTable);
			}
			
		};

	}

	/**
	 * Test the RandomDataGenerator methods
	 */
	@Test 
	void testRandomDataGenerator() {
		
		// Before running the tests, lets populate with some built-in domain table
		domainService.assertDomainTablesForAllArchetypes(/*overwrite*/false);

		// Create and initialize the object used for creating random data
		int seed = "TEST".hashCode();
		RandomDataGenerator generator = new RandomDataGenerator(seed);
		generator.setDomainTableRepository(domainTableRepository::findByNameAndVersion);
		generator.setYearLowerBound(2020);
		generator.setYearUpperBound(2021);
		
		Set<Integer> generatedYears = new HashSet<>();
		for (int i=0; i<10; i++) {
			int year = generator.nextRandomYear();
			assertTrue(year>=2020 && year<=2021, "The generated year lies outside boundary: "+year);
			generatedYears.add(year);
		}
		assertTrue(generatedYears.contains(2020), "Expected at least one occurrence of year 2020");
		assertTrue(generatedYears.contains(2021), "Expected at least one occurrence of year 2021");

		generator.setYearLowerBound(2021);
		generator.setYearUpperBound(2021);

		for (int i=0; i<10; i++) {
			int year = generator.nextRandomYear();
			assertEquals(2021, year, "The generated year lies outside boundary: "+year);
		}
		
		Set<String> generatedMonths = new HashSet<>();
		for (int i=0; i<10; i++) {
			String m = generator.nextRandomMonth();
			assertTrue(ParserUtils.isMonthYear(m), "Expected month/year, but found "+m);
			assertNotNull(ParserUtils.getYearMonth(m), "Expected month/year, but found "+m);
			generatedMonths.add(m);
		}
		assertTrue(generatedMonths.size()>1, "Expected different months, but created only this: "+generatedMonths);
		
		Set<LocalDate> generatedDays = new HashSet<>();
		for (int i=0; i<10; i++) {
			LocalDate d = generator.nextRandomDate();
			assertEquals(2021,d.getYear(), "The generated date lies outside the YEAR boundary: "+d);
			generatedDays.add(d);
		}
		assertTrue(generatedDays.size()>1, "Expected different days, but created only this: "+generatedDays);
		
		Set<Boolean> generatedBooleans = new HashSet<>();
		for (int i=0; i<10; i++) {
			boolean b = generator.nextRandomBoolean();
			generatedBooleans.add(b);
		}
		assertEquals(2, generatedBooleans.size(), "Expected two boolean values, but created only this: "+generatedBooleans);
		
		Set<Integer> generatedIntegers = new HashSet<>();
		for (int i=0; i<10; i++) {
			Integer n = generator.nextRandomInteger();
			generatedIntegers.add(n);
		}
		assertTrue(generatedIntegers.size()>1, "Expected different integer values, but created only this: "+generatedIntegers);

		Set<String> generatedStrings = new HashSet<>();
		for (int i=0; i<10; i++) {
			String s = generator.nextRandomString();
			generatedStrings.add(s);
		}
		assertTrue(generatedStrings.size()>1, "Expected different string values, but created only this: "+generatedStrings);

		Set<String> generatedDomainTableValues = new HashSet<>();
		for (int i=0; i<10; i++) {
			String s = generator.nextRandomDomain("TEST-2 DEBIT/CREDIT","1.0");
			assertTrue(s.equals("D") || s.equals("C"), "Unexpected domain table value: "+s);
			generatedDomainTableValues.add(s);
		}
		assertTrue(generatedDomainTableValues.size()>1, "Expected different domaintable values, but created only this: "+generatedDomainTableValues);
	}
	
	/**
	 * Test the ExcelGenerator object
	 */
	@Test 
	void testExcelGenerator() throws Exception {
		
		// Creates some template for testing
		DocumentTemplate template = new DocumentTemplate();
		template.setName("TEST");
		template.setVersion("1.0");
		// Template fields
		template.addField(new DocumentField().withFieldName("TaxPayerId").withFieldType(FieldType.CHARACTER).withFieldMapping(FieldMapping.TAXPAYER_ID).withRequired(true));
		template.addField(new DocumentField().withFieldName("TaxYear").withFieldType(FieldType.INTEGER).withFieldMapping(FieldMapping.TAX_YEAR).withRequired(true));
		template.addField(new DocumentField().withFieldName("Date").withFieldType(FieldType.DATE).withRequired(true));
		template.addField(new DocumentField().withFieldName("Id").withFieldType(FieldType.INTEGER).withRequired(true));
		template.addField(new DocumentField().withFieldName("Account").withFieldType(FieldType.CHARACTER).withRequired(true));
		template.addField(new DocumentField().withFieldName("D/C").withFieldType(FieldType.DOMAIN).withDomainTableName("TEST-2 DEBIT/CREDIT").withDomainTableVersion("1.0").withRequired(true));
		template.addField(new DocumentField().withFieldName("Amount").withFieldType(FieldType.DECIMAL).withRequired(true));
		// Input format
		DocumentInput inputFormat = new DocumentInput();
		inputFormat.setFormat(DocumentFormat.XLS);
		inputFormat.addField(new DocumentInputFieldMapping().withFieldName("TaxPayerId").withFileNameExpression("^(\\d{11})"));
		inputFormat.addField(new DocumentInputFieldMapping().withFieldName("TaxYear").withFileNameExpression("\\-(\\d{4})"));
		inputFormat.addField(new DocumentInputFieldMapping().withFieldName("Date").withColumnNameExpression("DATE.*"));
		inputFormat.addField(new DocumentInputFieldMapping().withFieldName("Id").withColumnNameExpression("ID.*"));
		inputFormat.addField(new DocumentInputFieldMapping().withFieldName("Account").withColumnNameExpression("ACCOUNT.*"));
		inputFormat.addField(new DocumentInputFieldMapping().withFieldName("D/C").withColumnNameExpression("D.*C"));
		inputFormat.addField(new DocumentInputFieldMapping().withFieldName("Amount").withColumnNameExpression("VALUE.*"));
		template.addInput(inputFormat);
		templateRepository.save(template);
		
		// Choose a temporary location for storing the generated file
		String subdir = fileStorageService.getSubDir();
		Path location = fileStorageService.getLocation(subdir);
		String filename = UUID.randomUUID().toString();
		Path path = location.resolve(Paths.get(filename)).normalize().toAbsolutePath();


		// Create and initialize the object used for creating Excel file with random data
	
		int seed = "TEST".hashCode();
		ExcelGenerator gen = new ExcelGenerator();
		gen.setDocumentTemplate(template);
		gen.setDocumentInputSpec(inputFormat);
		gen.setDomainTableRepository(domainTableRepository::findByNameAndVersion);
		gen.setPath(path);
		gen.setRandomSeed(seed);
		String originalFilename;
		
		final int numRowsToGenerate = 10;
		final int min_year, max_year;
		
		try {
			gen.start();
			originalFilename = gen.getOriginalFileName();
			for (int i=0; i<numRowsToGenerate; i++) {
				gen.addRandomRecord();
			}
		}
		finally {
			
			// Commit (write) the file to disk
			gen.close();

			min_year = gen.getRandomGenerator().getYearLowerBound();
			max_year = gen.getRandomGenerator().getYearUpperBound();

			// Let's read the generated file
			try (FileInputStream inputStream = new FileInputStream(path.toFile());
				Workbook workbook = WorkbookFactory.create(inputStream);) {
				
				// Let's check if the generated column names matches the configured expressions
				
				Sheet sheet = workbook.getSheetAt(0);
				Row rowHeaders = sheet.getRow(gen.getRowOfColumnHeaders());
				Map<String,Integer> mapFieldNameToColumnPosition = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
				for (DocumentInputFieldMapping field: inputFormat.getFields()) {
					if (field.getColumnNameExpression()==null || field.getColumnNameExpression().trim().length()==0)
						continue;
					Optional<Cell> matchingCell = ValidationContext.matchExpression(()->rowHeaders.cellIterator(), Cell::getStringCellValue, field.getColumnNameExpression());
					assertTrue(matchingCell.isPresent(), "Missing a column name for field "+field.getFieldName()+" that matches the expression "+field.getColumnNameExpression());
					mapFieldNameToColumnPosition.put(field.getFieldName(), matchingCell.get().getColumnIndex());
				}
				
				// Let's parse the generated contents
				int firstRow = gen.getFirstRowOfData();
				for (int rowNumber=firstRow; rowNumber<firstRow+numRowsToGenerate; rowNumber++) {
					Row row = sheet.getRow(rowNumber);
					
					Cell cell = row.getCell(mapFieldNameToColumnPosition.get("Date"));
					assertTrue(DateUtil.isCellDateFormatted(cell),"Expects a date formatted cell at column "+mapFieldNameToColumnPosition.get("Date"));
					Date d = cell.getDateCellValue();
					int year = DateTimeUtils.getYear(d);
					assertTrue(year>=min_year && year<=max_year, "Generated date ("+ParserUtils.formatTimestamp(d)+") lies outside of YEAR boundaries!");
					
					cell = row.getCell(mapFieldNameToColumnPosition.get("Id"));
					assertTrue(ParserUtils.isInteger(getCellValueAsString(cell)), "Expects a numeric value at column "+mapFieldNameToColumnPosition.get("Id")+", but found "+getCellValueAsString(cell));

					cell = row.getCell(mapFieldNameToColumnPosition.get("D/C"));
					String dc = getCellValueAsString(cell);
					assertTrue("D".equalsIgnoreCase(dc) || "C".equalsIgnoreCase(dc), "Expects a D/C value at column "+mapFieldNameToColumnPosition.get("D/C")+", but found "+dc);

					cell = row.getCell(mapFieldNameToColumnPosition.get("Amount"));
					assertTrue(ParserUtils.isDecimal(getCellValueAsString(cell)), "Expects a decimal value at column "+mapFieldNameToColumnPosition.get("Amount")+", but found "+getCellValueAsString(cell));
				}
			}

			// Delete the file generated
			Files.deleteIfExists(path);
		}
		
		// The 'original filename' generated by ExcelGenerator should match the provided expressions
		assertTrue(Pattern.compile("\\.XLSX$", Pattern.CASE_INSENSITIVE).matcher(originalFilename).find(), "Invalid filename: "+originalFilename);
		assertTrue(Pattern.compile("^(\\d{11})").matcher(originalFilename).find(), "Invalid filename: "+originalFilename); //  expression for TaxPayerId
		Matcher mYear = Pattern.compile("\\-(\\d{4})").matcher(originalFilename);
		assertTrue(mYear.find(), "Invalid filename: "+originalFilename);//  expression for TaxYear
		int year = Integer.parseInt(mYear.group(1));
		assertTrue(year>=min_year && year<=max_year, "Generated year ("+year+") lies out of boundaries!");
		
	}
	
	public static String getCellValueAsString(Cell cell) {
		if ( cell == null )
			return null;
		
		CellType type = cell.getCellType();
		
		if ( CellType.BLANK.equals(type) ) 
			return null;
		
		if ( CellType.ERROR.equals(type) ) { 
			return null;
		}
		
		if ( CellType.BOOLEAN.equals(type) ) { 
			return String.valueOf(cell.getBooleanCellValue());
		}
		
		if ( CellType.NUMERIC.equals(type) ) { 
			if (DateUtil.isCellDateFormatted(cell)) {
				return ParserUtils.formatTimestampES(cell.getDateCellValue());
	        }
			return String.valueOf(cell.getNumericCellValue());
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
}
