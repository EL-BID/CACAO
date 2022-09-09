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

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
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
		
		// The journal may be presented in one of two possible forms:
		// 1) One column for 'Amount' containing debits/credits, and one column for indicating 'D' for debits and 'C' for credits.
		// or
		// 2) One column for 'AmountDebitOnly' containing only debits, and one column for 'AmountCreditOnly' containing only credits.
		
		// The ETL is expecting only journal in form #1. So, if this is a 'form #2' journal, we will change it in order to make it
		// resemble the 'form #1'
		
		final boolean inputIsUnknown = context.getDocumentInput()==null || context.getDocumentInput().getFields()==null || context.getDocumentInput().getFields().isEmpty();
		
		// Fields for form #1
		final boolean hasAmountField = (inputIsUnknown) ? null!=context.getDocumentTemplate().getField(Amount.name()) 
				: context.getDocumentInput().hasEnoughForFetchingData(context.getDocumentTemplate().getField(Amount.name()));
		final boolean hasDebitCreditField = (inputIsUnknown) ? null!=context.getDocumentTemplate().getField(DebitCredit.name())
				: context.getDocumentInput().hasEnoughForFetchingData(context.getDocumentTemplate().getField(DebitCredit.name()));
		final boolean hasForm1Fields = hasAmountField && hasDebitCreditField;

		// Fields form form #2
		final boolean hasAmountDebitOnlyField = (inputIsUnknown) ? null!=context.getDocumentTemplate().getField(AmountDebitOnly.name())
				: context.getDocumentInput().hasEnoughForFetchingData(context.getDocumentTemplate().getField(AmountDebitOnly.name()));
		final boolean hasAmountCreditOnlyField = (inputIsUnknown) ? null!=context.getDocumentTemplate().getField(AmountCreditOnly.name())
				: context.getDocumentInput().hasEnoughForFetchingData(context.getDocumentTemplate().getField(AmountCreditOnly.name()));
		final boolean hasForm2Fields = hasAmountDebitOnlyField && hasAmountCreditOnlyField;
		
		if (!hasForm1Fields && !hasForm2Fields) {
			context.addAlert("{account.error.journal.unknown.form}");
			return false;
		}
		
		// We will use this list if we need to convert all the records from 'Form #2' into 'Form #1'
		List<Map<String, Object>> convertedRecords = (hasForm2Fields) ? new LinkedList<>() : null;
		
		boolean hasInvalidRecords = false;
		long countValidRecords = 0;
		
		final String LINE_ORDER = "#LineOrder#"; // we will use this in case we are converting records, just to keep the same order afterwards
		long lineOrderSequence = 1;
		Date previousDate = null;

		for (Iterator<Map<String, Object>> it=records.iterator(); it.hasNext(); ) {
			
			Map<String, Object> record = it.next();

			Date date = getParsedRequiredContent(context, Date.class, record, Date.name());
			if (date==null) {
				if (previousDate==null) {
					it.remove();
					hasInvalidRecords = true;
					continue;
				}
				else {
					date = previousDate;
				}
			}
			else {
				previousDate = date;
			}
			
			Number amount = null;
			String debitCredit = null;
			boolean hasForm1Values = false;
			
			Number amountDebit = null;
			Number amountCredit = null;
			boolean hasForm2Values = false;
			
			// Best-effort: let's consider any possibility (form#1, form#2 or both)
			
			if (hasForm1Fields) {
				amount = getParsedRequiredContent(context, Number.class, record, Amount.name());				
				debitCredit = getParsedRequiredContent(context, String.class, record, DebitCredit.name());
				
				hasForm1Values = (amount!=null && debitCredit!=null);			
			}
			
			if (!hasForm1Values && hasForm2Fields) {
				amountDebit = getParsedRequiredContent(context, Number.class, record, AmountDebitOnly.name());
				amountCredit = getParsedRequiredContent(context, Number.class, record, AmountCreditOnly.name());
				
				hasForm2Values = (amountDebit!=null || amountCredit!=null);
			}
			
			if (!hasForm1Values && !hasForm2Values) {
				it.remove();
				hasInvalidRecords = true;
				continue;
			}
			
			if (hasForm1Values && hasForm2Fields && !hasForm2Values) {
				// if we got 'Form#1' values and we expects 'Form#2' fields, let's keep track of the line order
				record.put(LINE_ORDER, lineOrderSequence++);
			}
			
			if (hasForm2Values) {
				// we have to convert this record into Form #1
				it.remove();
				if (amountDebit!=null) {
					Map<String,Object> convertedRecord = new HashMap<>(record); // make a copy of the original record
					convertedRecord.remove(AmountDebitOnly.name());		// Remove the field of Form #2
					convertedRecord.remove(AmountCreditOnly.name());	// Remove the field of Form #2
					convertedRecord.put(Amount.name(), amountDebit);
					convertedRecord.put(DebitCredit.name(), "D");
					convertedRecord.put(LINE_ORDER, lineOrderSequence++);
					convertedRecords.add(convertedRecord);
				}
				if (amountCredit!=null) {
					Map<String,Object> convertedRecord = new HashMap<>(record); // make a copy of the original record
					convertedRecord.remove(AmountDebitOnly.name());		// Remove the field of Form #2
					convertedRecord.remove(AmountCreditOnly.name());	// Remove the field of Form #2
					convertedRecord.put(Amount.name(), amountCredit);
					convertedRecord.put(DebitCredit.name(), "C");
					convertedRecord.put(LINE_ORDER, lineOrderSequence++);
					convertedRecords.add(convertedRecord);
				}
			}
			
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
			
			if (hasForm1Values) {
				boolean is_debit = debitCredit.equalsIgnoreCase("D");
				if (is_debit) {
					current_balance += Math.abs(amount.doubleValue());
					total_debits += Math.abs(amount.doubleValue());
				}
				else {
					current_balance -= Math.abs(amount.doubleValue());
					total_credits += Math.abs(amount.doubleValue());
				}
			}
			else {
				if (amountDebit!=null) {
					current_balance += Math.abs(amountDebit.doubleValue());
					total_debits += Math.abs(amountDebit.doubleValue());					
				}
				if (amountCredit!=null) {
					current_balance -= Math.abs(amountCredit.doubleValue());
					total_credits += Math.abs(amountCredit.doubleValue());					
				}
			}
			
			countValidRecords++;
			
		} // LOOP over all records in General Ledger
		
		// If we have only invalid records, lets abort
		if (hasInvalidRecords && countValidRecords==0) {
			return false;
		}
		
		if (previous_date!=null && Math.abs(current_balance)>epsilon) {
			context.addAlert("{account.error.debits.credits.unbalanced("+total_debits+","+total_credits+","+ISO_8601_DATE.get().format(previous_date)+")}");
			return false;												
		}
		
		if (convertedRecords!=null && !convertedRecords.isEmpty()) {
			
			// If we have validated the journal according to the Form #2, let's insert into the validated data all the records we have translated
			// and let's sort them according to the same order we have found
			records.addAll(convertedRecords);
			
			Collections.sort(records, new Comparator<Map<String,Object>>(){
				@Override
				public int compare(Map<String, Object> r1, Map<String, Object> r2) {
					long n1 = ((Number)r1.get(LINE_ORDER)).longValue();
					long n2 = ((Number)r2.get(LINE_ORDER)).longValue();
					if (n1<n2)
						return -1;
					if (n1>n2)
						return 1;
					return 0;					
				}				
			});
			
			// Remove the internal control field that we have inserted in all the records
			for (Map<String,Object> record: records) {
				record.remove(LINE_ORDER);
			}		
		}
		
		return true;
	}

}
