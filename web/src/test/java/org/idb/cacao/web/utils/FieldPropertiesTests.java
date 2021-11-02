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
package org.idb.cacao.web.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import org.idb.cacao.web.entities.Item;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

@RunWith(JUnitPlatform.class)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
/**
 * A group of tests over a test entity using UI controller 
 * 
 * @author Rivelino Patrício
 * 
 * @since 31/10/2021
 *
 */
public class FieldPropertiesTests {


	@Test
	public void allProperties() throws Exception {
		
		Map<String,FieldProperties> allProperties = FieldProperties.toFieldProperties(Item.class);
		
		assertNotNull(allProperties, "Expected to have an object");
		
		assertEquals(4, allProperties.size(), "Expected to have 4 itens");
		
		FieldProperties p = allProperties.get("name");
		
		assertNotNull(p, "Expected to have an object");
		
		assertEquals("item.name", p.getExternalName(), "Expected to have this external name");
		
		assertEquals(Boolean.TRUE, p.isEditable());
		
		assertEquals(Boolean.TRUE, p.isNotNull());
		
		assertEquals(Boolean.TRUE, p.isNotEmpty());
		
		assertEquals(Boolean.FALSE, p.isEmail());
		
		assertEquals(Boolean.FALSE, p.isId());
		
	}

}