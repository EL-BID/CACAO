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
package org.idb.cacao.api;

import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

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
	public void testToString() throws Exception {
		
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
	public void testToNumber() throws Exception {
		
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
	public void testToDate() throws Exception {
		
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
	public void testToOffsetDateTime() throws Exception {
		
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
