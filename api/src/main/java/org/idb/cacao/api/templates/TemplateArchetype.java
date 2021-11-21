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

import org.idb.cacao.api.DocumentSituation;
import org.idb.cacao.api.ETLContext;
import org.idb.cacao.api.ValidationContext;

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
	 * Name of the plugin that declared this archetype. Should be the same name for all archetypes in the
	 * same plugin. It's usefull for listing dependencies and diagnosing problems.<BR>
	 * If it's NULL, than it won't be listed as 'installed plugins'.
	 */
	default public String getPluginName() {
		return null;
	}

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
	
	/**
	 * Performs domain-specific validations over the uploaded file contents.
	 * The method should return TRUE if the document is OK to proceed in the workflow and should return FALSE if the document
	 * should be rejected.<BR>
	 * This method may insert into {@link org.idb.cacao.api.ValidationContext#addAlert(String) addAlert} any alerts regarding this validation.
	 * It doesn't mean that the file should be rejected. If this method returns TRUE and also outputs some alerts, those alerts
	 * are considered simple warnings (i.e. the file may be accepted despite of these warnings).
	 * @param context Object created by the validator with information regarding the incoming document
	 * @return Returns TRUE if the document is OK and may be considered. Returns FALSE if the document should be rejected
	 */
	default public boolean validateDocumentUploaded(ValidationContext context) {
		return true;
	}
	
	/**
	 * Executes Extract/Transform/Load operations producing denormalized data at database.<BR>
	 * Performs cross-validations with different files related to different archetypes that are part
	 * of the same 'group' as this one.<BR>
	 * This method should also write into {@link ETLContext#setOutcomeSituation(org.idb.cacao.api.DocumentUploaded, DocumentSituation) outcomeSituations} the resulting
	 * situation for each processed {@link DocumentUploaded DocumentUploaded}. Some may be considered {@link DocumentSituation#PROCESSED PROCESSED}.<BR>
	 * If no situation is written, then nothing will change regarding the documents situations.<BR>
	 * This method may also write some alerts into {@link ETLContext#addAlert(String) alerts} for signaling some warnings about the processed files.
	 * @param context Object created by the ETL consumer with information regarding the incoming document and any other objects
	 * necessary for retrieving related information
	 * @return Returns TRUE if the operation completed successfully. Returns FALSE in case of error.
	 */
	default public boolean performETL(ETLContext context) {
		return true;
	}
}
