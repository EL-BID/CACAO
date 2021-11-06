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
package org.idb.cacao.web.templates;

import static org.junit.jupiter.api.Assertions.*;

import org.idb.cacao.api.templates.TemplateArchetype;
import org.idb.cacao.api.templates.TemplateArchetypes;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

/**
 * Tests related to template archetypes
 * 
 * @author Gustavo Figueiredo
 *
 */
@RunWith(JUnitPlatform.class)
public class TemplateArchetypesTest {

	/**
	 * List installed archetypes (Consider all those modules present at current classpath)
	 * WARNING: at least the 'CACAO-ACCOUNT' plugin should be present at current classpath for this test case!
	 */
	@Test
	public void listArchetypes() throws Exception {
		
		Iterable<TemplateArchetype> known_archetypes = TemplateArchetypes.getTemplateArchetypes();
		
		TemplateArchetype found_general_arch = null;
		TemplateArchetype found_accounting_gl_arch = null;
		TemplateArchetype found_accounting_ca_arch = null;
		
		for (TemplateArchetype arch: known_archetypes) {
			
			if (arch.getName().equalsIgnoreCase("general.archetype")) {
				found_general_arch = arch;
			}

			if (arch.getName().equalsIgnoreCase("accounting.general.ledger")) {
				found_accounting_gl_arch = arch;
			}

			if (arch.getName().equalsIgnoreCase("accounting.chart.accounts")) {
				found_accounting_ca_arch = arch;
			}
		}
		
		assertNotNull(found_general_arch, "Did not find General-Purpose archetype defined in API module!");
		assertNotNull(found_accounting_gl_arch, "Did not find General Ledger archetype defined in Account (plugin) module!");
		assertNotNull(found_accounting_ca_arch, "Did not find Chart of Accounts archetype defined in Account (plugin) module!");
		
	}
	
	/**
	 * Test utility methods regarding searching for installed archetypes (Consider all those modules present at current classpath)
	 * WARNING: at least the 'CACAO-ACCOUNT' plugin should be present at current classpath for this test case!
	 */
	@Test
	public void findArchetypes() throws Exception {
		
		assertTrue(TemplateArchetypes.hasArchetype("general.archetype"), "Should find the General-Purpose archetype defined in API module!");
		assertTrue(TemplateArchetypes.hasArchetype("accounting.general.ledger"), "Should find the General Ledger archetype defined in Account (plugin) module!");
		assertTrue(TemplateArchetypes.hasArchetype("accounting.chart.accounts"), "Should find the Chart of Accounts archetype defined in Account (plugin) module!");
		assertFalse(TemplateArchetypes.hasArchetype("!!!inexistent!!!"), "Should not find an inexistent archetype!");
		
		assertEquals("general.archetype",TemplateArchetypes.getArchetype("general.archetype").get().getName(), "The archetype returned does not match the requested name!");
		assertEquals("accounting.general.ledger",TemplateArchetypes.getArchetype("accounting.general.ledger").get().getName(), "The archetype returned does not match the requested name!");
		assertEquals("accounting.chart.accounts",TemplateArchetypes.getArchetype("accounting.chart.accounts").get().getName(), "The archetype returned does not match the requested name!");
		
	}
}
