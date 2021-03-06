/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.account.validations;

import org.idb.cacao.api.ValidationContext;
import static org.idb.cacao.api.utils.ParserUtils.ISO_8601_DATE;

import static org.idb.cacao.api.ValidationContext.*;
import static org.idb.cacao.account.archetypes.GeneralLedgerArchetype.FIELDS_NAMES.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Performs some validation tests over incoming file related to General Ledger
 * 
 * @author Gustavo Figueiredo
 *
 */
public class GeneralLedgerValidations {

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
		
		Date previous_date = null;
		double current_balance = 0; // positive for debit, negative for credit
		double total_debits = 0;
		double total_credits = 0;
		final double epsilon = 0.005; // ignores differences lesser than half of a cent		
		
		for (Map<String, Object> record: records) {

			Date date = getParsedRequiredContent(context, Date.class, record, Date.name());
			if (date==null)
				return false;
			
			Number amount = getParsedRequiredContent(context, Number.class, record, Amount.name());
			if (amount==null)
				return false;
			
			String debitCredit = getParsedRequiredContent(context, String.class, record, DebitCredit.name());
			if (debitCredit==null)
				return false;
			boolean is_debit = debitCredit.equalsIgnoreCase("D");
			
			if (previous_date==null) {
				previous_date = date;
			}
			
			if (!previous_date.equals(date)) {
				
				// changed the day, check the balance
				
				if (Math.abs(current_balance)>epsilon) {
					context.addAlert("{account.error.debits.credits.unbalanced("+total_debits+","+total_credits+","+ISO_8601_DATE.get().format(previous_date)+")}");
					return false;									
				}
				
				// Reset counts
				current_balance = 0;
				total_debits = 0;
				total_credits = 0;
				previous_date = date;
				
			}
			
			if (is_debit) {
				current_balance += Math.abs(amount.doubleValue());
				total_debits += Math.abs(amount.doubleValue());
			}
			else {
				current_balance -= Math.abs(amount.doubleValue());
				total_credits += Math.abs(amount.doubleValue());
			}
			
		} // LOOP over all records in General Ledger
		
		if (previous_date!=null && Math.abs(current_balance)>epsilon) {
			context.addAlert("{account.error.debits.credits.unbalanced("+total_debits+","+total_credits+","+ISO_8601_DATE.get().format(previous_date)+")}");
			return false;												
		}
		
		return true;
	}

}
