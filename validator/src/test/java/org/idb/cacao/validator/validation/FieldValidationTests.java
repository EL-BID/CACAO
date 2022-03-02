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
