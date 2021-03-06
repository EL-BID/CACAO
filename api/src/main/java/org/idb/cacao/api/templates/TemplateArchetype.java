/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.api.templates;

import java.util.Collections;
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
	 * require at least the following fields: date, value, debit/credit indicator and account code.<BR>
	 * The 'description' of each field usually is presented to user as is declared here in each object. If
	 * the description needs to be resolved to a specific language using message properties file, this
	 * method should enclose the description between curly braces. 
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
	
	/**
	 * Returns all the indices related to published (denormalized) data that are derived from this archetype
	 */
	default public List<String> getRelatedPublishedDataIndices() {
		return Collections.emptyList();
	}
	
	/**
	 * Returns indication that this archetype has a custom implementation for the provided document template
	 * and document format.
	 */
	default public boolean hasCustomGenerator(DocumentTemplate template, DocumentFormat format) {
		return false;
	}
	
	/**
	 * Returns a custom implementation for generating random data according to a given template and file format.
	 * @param template Template that serves as a blueprint for data generation
	 * @param format File format chosen to generate data
	 * @param seed Initial seed for random data generation
	 * @param records The expected total number of records to generate with this custom generator. Negative number means the total number is undefined, so the implementation
	 * may decide when to stop generating new records.
	 * @return Returns implementation for generating data. Returns NULL if there is no custom implementation for data generation. Throws exception in
	 * case of incompatible parameters.
	 */
	default public CustomDataGenerator getCustomGenerator(DocumentTemplate template, DocumentFormat format, long seed, long records) throws Exception {
		return null;
	}
}
