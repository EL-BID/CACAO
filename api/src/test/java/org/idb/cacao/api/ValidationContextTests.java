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

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.idb.cacao.api.errors.GeneralException;
import org.idb.cacao.api.utils.ParserUtils;
import org.junit.jupiter.api.Test;

/**
 * Performs some tests with ValidationContext functions
 * 
 * @author Gustavo Figueiredo
 *
 */
@RunWith(JUnitPlatform.class)
public class ValidationContextTests {

	/**
	 * Test the outcome of 'toString' function
	 */
	@Test
	public void testToString() throws GeneralException {
		
		assertNull(ValidationContext.toString(null));
		assertEquals("ABC", ValidationContext.toString("ABC"));
		assertEquals("123", ValidationContext.toString("123"));
		assertEquals("123", ValidationContext.toString(123));
		assertEquals("123", ValidationContext.toString(123.0));
		assertEquals("123.1", ValidationContext.toString(123.1));
		assertEquals("true", ValidationContext.toString(true));
	}
	
	/**
	 * Test the outcome of 'toNumber' function
	 */
	@Test
	public void testToNumber() throws GeneralException {
		
		assertNull(ValidationContext.toNumber(null));
		assertNull(ValidationContext.toNumber("ABC"));
		assertEquals(123L, ValidationContext.toNumber("123"));
		assertEquals(123L, ValidationContext.toNumber(123L));
		assertEquals(123, ValidationContext.toNumber(123));
		assertEquals(123.0, ValidationContext.toNumber(123.0));
		assertEquals(123.0f, ValidationContext.toNumber(123.0f));
	}

	/**
	 * Test the outcome of 'toDate' function
	 */
	@Test
	public void testToDate() throws GeneralException {
		
		assertNull(ValidationContext.toDate(null));
		
		java.util.Date d = ParserUtils.parseDMY("21-11-2021");
		assertEquals(d, ValidationContext.toDate(d));

		java.sql.Date d2 = new java.sql.Date(d.getTime());
		assertEquals(d, ValidationContext.toDate(d2));

		LocalDate ld = LocalDate.of(2021, 11, 21);
		assertEquals(d, ValidationContext.toDate(ld));

		ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(Instant.now());
		OffsetDateTime odt = OffsetDateTime.of(2021, 11, 21, 0, 0, 0, 0, offset);
		assertEquals(d, ValidationContext.toDate(odt));

		assertEquals(d, ValidationContext.toDate("21-11-2021"));
		assertEquals(d, ValidationContext.toDate("11-21-2021"));
		assertEquals(d, ValidationContext.toDate("2021-11-21"));

		assertEquals(d, ValidationContext.toDate("21/11/2021"));
		assertEquals(d, ValidationContext.toDate("11/21/2021"));
		assertEquals(d, ValidationContext.toDate("2021/11/21"));

		assertEquals(d, ValidationContext.toDate("2021-11-21T00:00:00"));
		assertEquals(d, ValidationContext.toDate("2021-11-21T00:00:00.000"+offset));
	}

	/**
	 * Test the outcome of 'toOffsetDateTime' function
	 */
	@Test
	public void testToOffsetDateTime() throws GeneralException {
		
		assertNull(ValidationContext.toOffsetDateTime(null));

		ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(Instant.now());
		OffsetDateTime odt = OffsetDateTime.of(2021, 11, 21, 0, 0, 0, 0, offset);
		assertEquals(odt, ValidationContext.toOffsetDateTime(odt));

		java.util.Date d = ParserUtils.parseDMY("21-11-2021");
		assertEquals(odt, ValidationContext.toOffsetDateTime(d));

		java.sql.Date d2 = new java.sql.Date(d.getTime());
		assertEquals(odt, ValidationContext.toOffsetDateTime(d2));

		LocalDate ld = LocalDate.of(2021, 11, 21);
		assertEquals(odt, ValidationContext.toOffsetDateTime(ld));

		assertEquals(odt, ValidationContext.toOffsetDateTime("21-11-2021"));
		assertEquals(odt, ValidationContext.toOffsetDateTime("11-21-2021"));
		assertEquals(odt, ValidationContext.toOffsetDateTime("2021-11-21"));

		assertEquals(odt, ValidationContext.toOffsetDateTime("21/11/2021"));
		assertEquals(odt, ValidationContext.toOffsetDateTime("11/21/2021"));
		assertEquals(odt, ValidationContext.toOffsetDateTime("2021/11/21"));

		assertEquals(odt, ValidationContext.toOffsetDateTime("2021-11-21T00:00:00"));
		assertEquals(odt, ValidationContext.toOffsetDateTime("2021-11-21T00:00:00.000"+offset));
	}

}
