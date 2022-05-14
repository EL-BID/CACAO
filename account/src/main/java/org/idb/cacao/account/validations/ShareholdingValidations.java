/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.account.validations;

import static org.idb.cacao.account.archetypes.ShareholdingArchetype.FIELDS_NAMES.*;
import static org.idb.cacao.api.ValidationContext.getParsedRequiredContent;

import java.util.List;
import java.util.Map;

import org.idb.cacao.api.ValidationContext;

/**
 * Performs some validation tests over incoming file related to Shareholding
 * 
 * @author Gustavo Figueiredo
 *
 */
public class ShareholdingValidations {

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

			Number sharePercentage = getParsedRequiredContent(context, Number.class, record, SharePercentage.name());
			if (sharePercentage!=null) {
				if (sharePercentage.doubleValue()<-0.0) {
					context.addAlert("{error.value.negative("
						+SharePercentage.name()
						+","
						+sharePercentage
						+")}");
					return false;
				}
				if (sharePercentage.doubleValue()>100.0) {
					context.addAlert("{error.value.high("
						+SharePercentage.name()
						+","
						+sharePercentage
						+",100)}");
					return false;
				}
			}
			
			String shareholdingName = getParsedRequiredContent(context, String.class, record, ShareholdingName.name());
			String shareholdingId = getParsedRequiredContent(context, String.class, record, ShareholdingId.name());
			if ((shareholdingName==null || shareholdingName.trim().length()==0)
				&& (shareholdingId==null || shareholdingId.trim().length()==0)) {
				
				context.addAlert("{account.error.shareholding.unidentified}");
				return false;
			}
		}
		
		return true;
	}
	
}
