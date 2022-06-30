/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.account.validations;

import static org.idb.cacao.api.ValidationContext.*;

import java.util.List;
import java.util.Map;

import org.idb.cacao.api.ValidationContext;

import static org.idb.cacao.account.archetypes.OpeningBalanceArchetype.FIELDS_NAMES.*;

/**
 * Performs some validation tests over incoming file related to Opening Balance
 * 
 * @author Gustavo Figueiredo
 *
 */
public class OpeningBalanceValidations {

	/**
	 * Performs domain-specific validations over the uploaded file contents.
	 * The method should return TRUE if the document is OK to proceed in the workflow and should return FALSE if the document
	 * should be rejected.<BR>
	 * This method may insert into {@link org.idb.cacao.api.ValidationContext#addAlert(String) addAlert} any alerts regarding this validation.
	 * It doesn't mean that the file should be rejected. If this method returns TRUE and also outputs some alerts, those alerts
	 * are considered simple warnings (i.e. the file may be accepted despite of these warnings).
	 * @param context Object created by the validator with information regarding the incoming document
	 * @param records Records to validate
	 * @return Returns TRUE if the document is OK and may be considered. Returns FALSE if the document should be rejected
	 */
	public static boolean validateDocumentUploaded(ValidationContext context, List<Map<String,Object>> records) {
		if (context==null 
				|| records==null
				|| records.isEmpty())
			return false;
		
		for (Map<String, Object> record: records) {
			
			String debitCredit = getParsedRequiredContent(context, String.class, record, DebitCredit.name());
			if (debitCredit==null)
				return false;

		} // LOOP over all records in Opening Balance
		
		return true;
	}
}
