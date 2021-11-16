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
package org.idb.cacao.validator;

import org.idb.cacao.validator.controllers.services.FileUploadedConsumerService;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.api.templates.DocumentInput;
import org.idb.cacao.api.templates.DocumentInputFieldMapping;
import org.idb.cacao.validator.fileformats.FileFormatFactory;

/**
 * Tests with filenames of incoming files to the validator
 * 
 * @author Gustavo Figueiredo
 *
 */
@RunWith(JUnitPlatform.class)
public class FilenameTests {

	/**
	 * Test method for fetching information from filenames
	 */
	@Test
	void requiredFieldsInFileNames() {
		
		// Some DocumentInput specification for this test case
		DocumentInput docInputSpec = new DocumentInput();
		docInputSpec.addField(new DocumentInputFieldMapping()
				.withFieldName("TaxPayerId")
				.withFileNameExpression("^(\\d{9})"));
		
		// Test a hypothetical filename
		String filename = "123456789-JAN-2021.TXT";
		ValidationContext validationContext = new ValidationContext();
		validationContext.setDocumentPath(new File(filename).toPath());
		FileUploadedConsumerService.fetchInformationFromFileName(docInputSpec, filename, validationContext);
		
		assertFalse(validationContext.isEmpty(), "Should have created a record regarding the 'TaxPayerId' fetched from the file name!");
		assertEquals(1, validationContext.size());
		assertEquals("123456789", validationContext.getParsedContent(0, "TaxPayerId"));
		
		// Repeat the test considering we got some data supposedly from the file contents itself
		validationContext.clearParsedContents();
		validationContext.addParsedContent(genRecord("Field1", "123",  "Field2", "01/01/2020"));
		validationContext.addParsedContent(genRecord("Field1", "456",  "Field2", "02/01/2020"));
		validationContext.addParsedContent(genRecord("Field1", "555",  "Field2", "03/01/2020"));
		
		FileUploadedConsumerService.fetchInformationFromFileName(docInputSpec, filename, validationContext);
		
		assertFalse(validationContext.isEmpty(), "Should have filled the existing three records with the 'TaxPayerId' field fetched from the file name!");
		assertEquals(3, validationContext.size());
		assertEquals("123456789", validationContext.getParsedContent(0, "TaxPayerId"));
		assertEquals("123", validationContext.getParsedContent(0, "Field1"));
		
		assertEquals("123456789", validationContext.getParsedContent(1, "TaxPayerId"));
		assertEquals("456", validationContext.getParsedContent(1, "Field1"));
		
		assertEquals("123456789", validationContext.getParsedContent(2, "TaxPayerId"));
		assertEquals("555", validationContext.getParsedContent(2, "Field1"));
		
		
		// Test another hypothetical filename that SHOULD NOT MATCH
		filename = "AAAAAAAAA-JAN-2021.TXT";
		validationContext.clearParsedContents();
		validationContext.setDocumentPath(new File(filename).toPath());
		FileUploadedConsumerService.fetchInformationFromFileName(docInputSpec, filename, validationContext);

		assertTrue(validationContext.isEmpty(), "Shouldn't have get the TaxPayerId because the filename does not match the configured expression");
		
		// Repeat the test considering we got some data supposedly from the file contents itself
		validationContext.addParsedContent(genRecord("Field1", "123",  "Field2", "01/01/2020"));
		validationContext.addParsedContent(genRecord("Field1", "456",  "Field2", "02/01/2020"));
		validationContext.addParsedContent(genRecord("Field1", "555",  "Field2", "03/01/2020"));
		
		FileUploadedConsumerService.fetchInformationFromFileName(docInputSpec, filename, validationContext);
		assertFalse(validationContext.isEmpty(), "The pre-existent fields should remain the same (even if we could not get the information from filename)!");
		assertEquals(3, validationContext.size());
		assertNull(validationContext.getParsedContent(0, "TaxPayerId"));
		assertEquals("123", validationContext.getParsedContent(0, "Field1"));
		
		assertNull(validationContext.getParsedContent(1, "TaxPayerId"));
		assertEquals("456", validationContext.getParsedContent(1, "Field1"));
		
		assertNull(validationContext.getParsedContent(2, "TaxPayerId"));
		assertEquals("555", validationContext.getParsedContent(2, "Field1"));

	}

	/**
	 * Test method for guessing file format from file name
	 */
	@Test
	void guessFileFormatFromFileName() {
		
		assertTrue(FileFormatFactory.getFileFormat(DocumentFormat.XLS).matchFilename("SOME_FILE.XLS"));
		assertTrue(FileFormatFactory.getFileFormat(DocumentFormat.XLS).matchFilename("SOME_FILE.XLSX"));
		assertTrue(FileFormatFactory.getFileFormat(DocumentFormat.XLS).matchFilename("SOME_FILE.XLSM"));
		assertFalse(FileFormatFactory.getFileFormat(DocumentFormat.XLS).matchFilename("SOME_FILE.ODT"));
		
		assertTrue(FileFormatFactory.getFileFormat(DocumentFormat.DOC).matchFilename("SOME_FILE.DOC"));
		assertTrue(FileFormatFactory.getFileFormat(DocumentFormat.DOC).matchFilename("SOME_FILE.DOCX"));
		assertFalse(FileFormatFactory.getFileFormat(DocumentFormat.DOC).matchFilename("SOME_FILE.ODT"));

		assertTrue(FileFormatFactory.getFileFormat(DocumentFormat.PDF).matchFilename("SOME_FILE.PDF"));
		assertFalse(FileFormatFactory.getFileFormat(DocumentFormat.PDF).matchFilename("SOME_FILE.ODT"));

		assertTrue(FileFormatFactory.getFileFormat(DocumentFormat.JSON).matchFilename("SOME_FILE.JSON"));
		assertFalse(FileFormatFactory.getFileFormat(DocumentFormat.JSON).matchFilename("SOME_FILE.ODT"));
	}
	
	public static Map<String,Object> genRecord(String... fieldsAndValues) {
		Map<String,Object> record = new HashMap<>();
		for (int i=0; i<fieldsAndValues.length; i+=2) {
			String fieldName = fieldsAndValues[i];
			String fieldValue = fieldsAndValues[i+1];
			record.put(fieldName, fieldValue);
		}
		return record;
	}
}
