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

}
