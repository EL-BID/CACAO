/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.validator.validation;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;

import javax.validation.ValidationException;

import org.idb.cacao.api.DocumentSituation;
import org.idb.cacao.api.DocumentUploaded;
import org.idb.cacao.api.DocumentValidationErrorMessage;
import org.idb.cacao.api.storage.FileSystemStorageService;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.api.templates.DocumentInput;
import org.idb.cacao.api.templates.DocumentInputFieldMapping;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.FieldMapping;
import org.idb.cacao.api.templates.FieldType;
import org.idb.cacao.mock_es.ElasticsearchMockClient;
import org.idb.cacao.validator.controllers.services.FileUploadedConsumerService;
import org.idb.cacao.validator.repositories.DocumentTemplateRepository;
import org.idb.cacao.validator.repositories.DocumentUploadedRepository;
import org.idb.cacao.validator.repositories.DocumentValidationErrorMessageRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import static org.idb.cacao.mock_es.ElasticsearchMockClient.map;

/**
 * Tests the change of status depending on the validation result
 * 
 * @author Gustavo Figueiredo
 * 
 */
@RunWith(JUnitPlatform.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ValidationStatusTests {

	private static ElasticsearchMockClient mockElastic;
	
	@Autowired
	private FileUploadedConsumerService fileUploadedConsumerService;

	@Autowired
	private DocumentTemplateRepository documentTemplateRepository;

	@Autowired
	private DocumentUploadedRepository documentsUploadedRepository;
	
	@Autowired
	private DocumentValidationErrorMessageRepository documentValidationErrorMessageRepository;

	@Autowired
	private FileSystemStorageService fileSystemStorageService;

	@BeforeAll
	public static void beforeClass() throws Exception {

		int port = ElasticsearchMockClient.findRandomPort();
		mockElastic = new ElasticsearchMockClient(port);
		System.setProperty("es.port", String.valueOf(port));
	}
	
	@AfterAll
	public static void afterClass() {
		if (mockElastic!=null)
			mockElastic.stop();
	}
	
	@Test
	void testValidationWithSuccess() throws Exception {
		
		String fileId = "ValidationStatusTests.CSV";
		String fileContents = 
				  "11111111;Something;2020-09-24;2000.0\n"
				+ "11111111;Something else;2020-10-20;1000.0\n";
		
		String subdir = fileSystemStorageService.store(fileId, new ByteArrayInputStream(fileContents.getBytes()), true);
		
		String userName = "admin";
		String userLogin = "admin";
		String userTaxpayerId = "11111111";
		mockElastic.newDocument("cacao_user", 
			map("name", userName, 
				"login", userLogin, 
				"taxpayerId", userTaxpayerId,
				"active", true));
		
		DocumentTemplate template = new DocumentTemplate();
		template.setName("TEST");
		template.setVersion("1.0");
		template.addField(new DocumentField("TAXPAYER",FieldType.CHARACTER).withFieldMapping(FieldMapping.TAXPAYER_ID).withRequired(true).withFileUniqueness(true));
		template.addField(new DocumentField("FIELD1",FieldType.CHARACTER).withRequired(true));
		template.addField(new DocumentField("FIELD2",FieldType.DATE).withRequired(true));
		template.addField(new DocumentField("FIELD3",FieldType.DECIMAL).withRequired(true));
		DocumentInput input = new DocumentInput();
		input.setInputName("TEST-CSV");
		input.setFormat(DocumentFormat.CSV);
		input.addField(new DocumentInputFieldMapping().withFieldName("TAXPAYER").withColumnIndex(0));
		input.addField(new DocumentInputFieldMapping().withFieldName("FIELD1").withColumnIndex(1));
		input.addField(new DocumentInputFieldMapping().withFieldName("FIELD2").withColumnIndex(2));
		input.addField(new DocumentInputFieldMapping().withFieldName("FIELD3").withColumnIndex(3));
		template.addInput(input);
		documentTemplateRepository.save(template);

		DocumentUploaded doc = new DocumentUploaded();
		doc.setUser(userName);
		doc.setUserLogin(userLogin);
		doc.setTemplateName(template.getName());
		doc.setTemplateVersion(template.getVersion());
		doc.setInputName(input.getInputName());
		doc.setFilename(fileId);
		doc.setSubDir(subdir);
		doc.setFileId(fileId);		
		documentsUploadedRepository.saveWithTimestamp(doc);
		
		try {
			fileUploadedConsumerService.validateDocument(doc.getId());
		}
		catch (Exception ex) {
			StringBuilder detailedReport = new StringBuilder();
			try {
				for (DocumentValidationErrorMessage error: documentValidationErrorMessageRepository.findAll()) {
					detailedReport.append("\n");
					detailedReport.append(error.getErrorMessage());
				}
			}
			catch (Throwable ex2) {
				// ignore this error
			}
			if (detailedReport.length()>0)
				throw new Exception(ex.getMessage()+detailedReport.toString(), ex);
			else
				throw ex;
		}
	
		List<DocumentValidationErrorMessage> errors = documentValidationErrorMessageRepository.findByDocumentId(doc.getId());
		assertTrue(errors.isEmpty());

		Optional<DocumentUploaded> docAfterValidation = documentsUploadedRepository.findById(doc.getId());
		assertTrue(docAfterValidation.isPresent());
		assertEquals(DocumentSituation.VALID,docAfterValidation.get().getSituation());
	}
	
	@Test
	void testValidationWithFailure() throws Exception {
		
		String fileId = "ValidationStatusTests.CSV";
		String fileContents = 
				  "11111111;Something;2020-09-24;2000.0\n"
				+ "11111111;Something else;!!!INVALID DATE FORMAT!!!;1000.0\n";		// <-- This line should be considered INVALID
		
		String subdir = fileSystemStorageService.store(fileId, new ByteArrayInputStream(fileContents.getBytes()), true);
		
		String userName = "admin";
		String userLogin = "admin";
		String userTaxpayerId = "11111111";
		mockElastic.newDocument("cacao_user", 
			map("name", userName, 
				"login", userLogin, 
				"taxpayerId", userTaxpayerId,
				"active", true));
		
		DocumentTemplate template = new DocumentTemplate();
		template.setName("TEST");
		template.setVersion("1.0");
		template.addField(new DocumentField("TAXPAYER",FieldType.CHARACTER).withFieldMapping(FieldMapping.TAXPAYER_ID).withRequired(true).withFileUniqueness(true));
		template.addField(new DocumentField("FIELD1",FieldType.CHARACTER).withRequired(true));
		template.addField(new DocumentField("FIELD2",FieldType.DATE).withRequired(true));
		template.addField(new DocumentField("FIELD3",FieldType.DECIMAL).withRequired(true));
		DocumentInput input = new DocumentInput();
		input.setInputName("TEST-CSV");
		input.setFormat(DocumentFormat.CSV);
		input.addField(new DocumentInputFieldMapping().withFieldName("TAXPAYER").withColumnIndex(0));
		input.addField(new DocumentInputFieldMapping().withFieldName("FIELD1").withColumnIndex(1));
		input.addField(new DocumentInputFieldMapping().withFieldName("FIELD2").withColumnIndex(2));
		input.addField(new DocumentInputFieldMapping().withFieldName("FIELD3").withColumnIndex(3));
		template.addInput(input);
		documentTemplateRepository.save(template);

		DocumentUploaded doc = new DocumentUploaded();
		doc.setUser(userName);
		doc.setUserLogin(userLogin);
		doc.setTemplateName(template.getName());
		doc.setTemplateVersion(template.getVersion());
		doc.setInputName(input.getInputName());
		doc.setFilename(fileId);
		doc.setSubDir(subdir);
		doc.setFileId(fileId);		
		documentsUploadedRepository.saveWithTimestamp(doc);
		
		try {
			fileUploadedConsumerService.validateDocument(doc.getId());
		}
		catch (ValidationException ex) {
			// Ignore validation errors here (they are expected)
		}
		
		List<DocumentValidationErrorMessage> errors = documentValidationErrorMessageRepository.findByDocumentId(doc.getId());
		assertFalse(errors.isEmpty());

		Optional<DocumentUploaded> docAfterValidation = documentsUploadedRepository.findById(doc.getId());
		assertTrue(docAfterValidation.isPresent());
		assertEquals(DocumentSituation.INVALID,docAfterValidation.get().getSituation());
	}
}
