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
package org.idb.cacao.account.validations;

import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.idb.cacao.account.archetypes.GeneralLedgerArchetype;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import static org.junit.jupiter.api.Assertions.*;

import static org.idb.cacao.api.ValidationContext.*;
import static org.idb.cacao.account.archetypes.GeneralLedgerArchetype.FIELDS_NAMES.*;

@RunWith(JUnitPlatform.class)
public class GeneralLedgerValidationsTests {
	
	private static final boolean DEBIT = true;
	private static final boolean CREDIT = false;

	/**
	 * Validates an arbitrary General Ledger that should be OK
	 */
	@Test
	public void validateOKLedger() throws Exception {
		
		// Prepare the test case scenario
		
		ValidationContext context = new ValidationContext();
		
		DocumentTemplate template = new DocumentTemplate();
		template.setFields(new GeneralLedgerArchetype().getRequiredFields());
		context.setDocumentTemplate(template);
		
		List<Map<String, Object>> records = new LinkedList<>();
		records.add(newLedgerRecord("2021-01-01", 100.0, DEBIT));
		records.add(newLedgerRecord("2021-01-01", 20.0, DEBIT));
		records.add(newLedgerRecord("2021-01-01", 80.0, CREDIT));
		records.add(newLedgerRecord("2021-01-01", 40.0, CREDIT));
		records.add(newLedgerRecord("2021-02-01", 40.0, DEBIT));		
		records.add(newLedgerRecord("2021-02-01", 40.0, CREDIT));
		records.add(newLedgerRecord("2021-02-01", 200.0, DEBIT));		
		records.add(newLedgerRecord("2021-02-01", 100.0, CREDIT));
		records.add(newLedgerRecord("2021-02-01", 100.0, CREDIT));
		records.add(newLedgerRecord("2021-03-01", 240.0, DEBIT));		
		records.add(newLedgerRecord("2021-03-01", 100.0, DEBIT));
		records.add(newLedgerRecord("2021-03-01", 340.0, CREDIT));		
		
		boolean result = GeneralLedgerValidations.validateDocumentUploaded(context, records);
		assertTrue(result, "The result of validation of General Ledger should be OK! Alerts: "+context.getAlerts());
	}
	
	/**
	 * Validates an arbitrary General Ledger that should be considered unbalanced
	 */
	@Test
	public void validateUnbalancedLedger() throws Exception {
		
		// Prepare the test case scenario
		
		ValidationContext context = new ValidationContext();
		
		DocumentTemplate template = new DocumentTemplate();
		template.setFields(new GeneralLedgerArchetype().getRequiredFields());
		context.setDocumentTemplate(template);
		
		List<Map<String, Object>> records = new LinkedList<>();
		records.add(newLedgerRecord("2021-01-01", 100.0, DEBIT));
		records.add(newLedgerRecord("2021-01-01", 20.0, DEBIT));
		records.add(newLedgerRecord("2021-01-01", 80.0, CREDIT));	
		// <=== unbalanced here
		records.add(newLedgerRecord("2021-02-01", 40.0, DEBIT));		
		records.add(newLedgerRecord("2021-02-01", 40.0, CREDIT));
		records.add(newLedgerRecord("2021-02-01", 200.0, DEBIT));		
		records.add(newLedgerRecord("2021-02-01", 100.0, CREDIT));
		records.add(newLedgerRecord("2021-02-01", 100.0, CREDIT));
		records.add(newLedgerRecord("2021-03-01", 240.0, DEBIT));		
		records.add(newLedgerRecord("2021-03-01", 100.0, DEBIT));
		records.add(newLedgerRecord("2021-03-01", 340.0, CREDIT));		
		
		boolean result = GeneralLedgerValidations.validateDocumentUploaded(context, records);
		assertFalse(result, "The result of validation of General Ledger should be NOT OK!");
		assertNotNull(context.getAlerts());
		assertFalse(context.getAlerts().isEmpty());
		assertEquals("{account.error.debits.credits.unbalanced(120.0,80.0,2021-01-01)}",context.getAlerts().get(0));
	}

	/**
	 * Validates an arbitrary General Ledger that should be considered unbalanced
	 */
	@Test
	public void validateUnbalancedLedger2() throws Exception {
		
		// Prepare the test case scenario
		
		ValidationContext context = new ValidationContext();
		
		DocumentTemplate template = new DocumentTemplate();
		template.setFields(new GeneralLedgerArchetype().getRequiredFields());
		context.setDocumentTemplate(template);
		
		List<Map<String, Object>> records = new LinkedList<>();
		records.add(newLedgerRecord("2021-01-01", 100.0, DEBIT));
		records.add(newLedgerRecord("2021-01-01", 20.0, DEBIT));
		records.add(newLedgerRecord("2021-01-01", 80.0, CREDIT));	
		records.add(newLedgerRecord("2021-01-01", 40.0, CREDIT));
		records.add(newLedgerRecord("2021-02-01", 40.0, DEBIT));		
		records.add(newLedgerRecord("2021-02-01", 40.0, CREDIT));
		records.add(newLedgerRecord("2021-02-01", 200.0, DEBIT));		
		records.add(newLedgerRecord("2021-02-01", 100.0, CREDIT));
		records.add(newLedgerRecord("2021-02-01", 100.0, CREDIT));
		records.add(newLedgerRecord("2021-03-01", 240.0, DEBIT));		
		records.add(newLedgerRecord("2021-03-01", 100.0, DEBIT));
		// <=== Unbalanced here		
		
		boolean result = GeneralLedgerValidations.validateDocumentUploaded(context, records);
		assertFalse(result, "The result of validation of General Ledger should be NOT OK!");
		assertNotNull(context.getAlerts());
		assertFalse(context.getAlerts().isEmpty());
		assertEquals("{account.error.debits.credits.unbalanced(340.0,0.0,2021-03-01)}",context.getAlerts().get(0));
	}

	/**
	 * Utility method for this test case
	 */
	private static Map<String, Object> newLedgerRecord(String date, double amount, boolean debit) throws ParseException {
		Map<String, Object> record = new HashMap<>();
		record.put(Date.name(), ISO_8601_DATE.get().parse(date));
		record.put(Amount.name(), amount);
		record.put(DebitCredit.name(), debit?"D":"C");
		return record;
	}
}
