/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.validator.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import org.idb.cacao.api.utils.ParserUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

/**
 * Tests validations against document templates and provided values
 * 
 * @author Rivelino Patrício
 * 
 * @since 24/11/2021
 *
 */
@RunWith(JUnitPlatform.class)
public class FieldValidationTests {
	
	
	@BeforeAll
	public static void beforeClass() throws Exception {

	}
	
	@AfterAll
	public static void afterClass() {
	}
	
	@Test
	void testFieldValidations() throws Exception {	
		
		//yyyy-MM-dd'T'HH:mm:ss
		String sDateTime = "2021-12-31T21:15:30";
		Date date = ParserUtils.parseTimestamp(sDateTime);		
		LocalDateTime dateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneOffset.systemDefault());
		assertEquals(2021,dateTime.getYear());
		assertEquals(12,dateTime.getMonthValue());
		assertEquals(31,dateTime.getDayOfMonth());
		assertEquals(21,dateTime.getHour());
		assertEquals(15,dateTime.getMinute());
		assertEquals(30,dateTime.getSecond());
		
	}

}
