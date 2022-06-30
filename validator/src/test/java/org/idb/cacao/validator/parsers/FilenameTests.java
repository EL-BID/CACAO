/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.validator.parsers;

import org.idb.cacao.validator.controllers.services.FileUploadedConsumerService;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.idb.cacao.api.DocumentUploaded;
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
		DocumentUploaded doc = new DocumentUploaded();
		doc.setFilename(filename);
		validationContext.setDocumentPath(new File(filename).toPath());
		validationContext.setDocumentUploaded(doc);
		FileUploadedConsumerService.fetchInformationFromFileName(docInputSpec, validationContext);
		
		assertFalse(validationContext.isEmpty(), "Should have created a record regarding the 'TaxPayerId' fetched from the file name!");
		assertEquals(1, validationContext.size());
		assertEquals("123456789", validationContext.getParsedContent(0, "TaxPayerId"));
		
		// Repeat the test considering we got some data supposedly from the file contents itself
		validationContext.clearParsedContents();
		validationContext.addParsedContent(genRecord("Field1", "123",  "Field2", "01/01/2020"));
		validationContext.addParsedContent(genRecord("Field1", "456",  "Field2", "02/01/2020"));
		validationContext.addParsedContent(genRecord("Field1", "555",  "Field2", "03/01/2020"));
		
		FileUploadedConsumerService.fetchInformationFromFileName(docInputSpec, validationContext);
		
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
		doc.setFilename(filename);
		validationContext.setDocumentPath(new File(filename).toPath());
		FileUploadedConsumerService.fetchInformationFromFileName(docInputSpec, validationContext);

		assertTrue(validationContext.isEmpty(), "Shouldn't have get the TaxPayerId because the filename does not match the configured expression");
		
		// Repeat the test considering we got some data supposedly from the file contents itself
		validationContext.addParsedContent(genRecord("Field1", "123",  "Field2", "01/01/2020"));
		validationContext.addParsedContent(genRecord("Field1", "456",  "Field2", "02/01/2020"));
		validationContext.addParsedContent(genRecord("Field1", "555",  "Field2", "03/01/2020"));
		
		FileUploadedConsumerService.fetchInformationFromFileName(docInputSpec, validationContext);
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
