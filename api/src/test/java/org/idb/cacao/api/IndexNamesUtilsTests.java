/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.api;

import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import static org.junit.jupiter.api.Assertions.*;

import org.idb.cacao.api.utils.IndexNamesUtils;
import org.junit.jupiter.api.Test;

/**
 * Performs some tests with IndexNamesUtils functions
 * 
 * @author Gustavo Figueiredo
 *
 */
@RunWith(JUnitPlatform.class)
public class IndexNamesUtilsTests {

	/**
	 * Test the outcome of 'formatFieldName' function
	 */
	@Test
	public void formatFieldName() throws Exception {
		
		assertEquals("some_field", IndexNamesUtils.formatFieldName("some_field"));
		assertEquals("some_field", IndexNamesUtils.formatFieldName("some field"));
		assertEquals("some_field", IndexNamesUtils.formatFieldName("Some Field"));
		assertEquals("some_field", IndexNamesUtils.formatFieldName("Some Field !!@@???"));
		assertEquals("some_field", IndexNamesUtils.formatFieldName("\"Some Field\""));
		assertEquals("some_field", IndexNamesUtils.formatFieldName("  Some       Field  "));
		assertEquals("some_field", IndexNamesUtils.formatFieldName("  Some   \t\t    Field \r\n\n\n\r\r "));
		assertEquals("some_field", IndexNamesUtils.formatFieldName("SOME FIELD"));
		assertEquals("some_field", IndexNamesUtils.formatFieldName("SomeField"));
		assertEquals("some_field", IndexNamesUtils.formatFieldName(".SomeField"));
		assertEquals("some_field", IndexNamesUtils.formatFieldName("_SomeField_"));
		
		assertEquals("some_field_123", IndexNamesUtils.formatFieldName("_SomeField_123"));
		assertEquals("some_field_1_123", IndexNamesUtils.formatFieldName("_SomeField_1.123"));
		assertEquals("some_field_123", IndexNamesUtils.formatFieldName("Some Field (123)"));
		
	}
	
}
