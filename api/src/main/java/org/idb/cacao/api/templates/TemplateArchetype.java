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
package org.idb.cacao.api.templates;

import java.util.List;

/**
 * Archetype for DocumentTemplate's that may be related to some application specific
 * context (e.g. some artifact specific to Accounting).<BR>
 * <BR>
 * Different 'archetypes' may be 'plugged in' the CACAO application.<BR>
 * <BR>
 * The discovery of implemented 'archetypes' may be performed using Java Service Provider Interface (SPI).
 * 
 * @author Gustavo Figueiredo
 *
 */
public interface TemplateArchetype {

	/**
	 * Name of this archetype to choose from an user interface. It should correspond to
	 * an entry in message.properties file.<BR>
	 * For example, an archetype related to 'General Ledger' used in Accounting could
	 * be named 'accounting.general.ledger' and be referred in language specific message.properties
	 * files to the corresponding text.
	 */
	public String getName();
	
	/**
	 * Returns all the required fields for any DocumentTemplate related to this
	 * TemplateArchetype.<BR>
	 * For example, an archetype related to 'General Ledger' used in Accounting would
	 * require at least the following fields: date, value, debit/credit indicator and account code. 
	 */
	public List<DocumentField> getRequiredFields();
	
	/**
	 * Returns all built-in domain table definitions that are referenced by the archetype's fields. 
	 * Returns NULL if there is none.
	 */
	default public List<DomainTable> getBuiltInDomainTables() {
		return null;
	}
	
	/**
	 * Returns the suggested 'group name' for DocumentTemplates generated from this TemplateArchetype.
	 * Returns NULL if there is no name to suggest.
	 */
	default public String getSuggestedGroup() {
		return null;
	}
	
	// TODO:
	// There should be additional members implemented by plugins for doing validations that are
	// specific to a given file related to this archetype. For example, if this is a
	// General Ledger, the sum of credits in any particular day should match the sum of
	// debits in the same day.
	
	// TODO:
	// There should be additional members implemented by plugins for doing cross-validations
	// with different files related to different archetypes that are part of the same 'group'
	// For example, for a group of template archetypes related to ACCOUNTING, there should
	// be a cross validation checking if all the accounts informed in GENERAL LEDGER are also
	// informed in CHART OF ACCOUNTS.
}
