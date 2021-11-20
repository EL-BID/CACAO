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
