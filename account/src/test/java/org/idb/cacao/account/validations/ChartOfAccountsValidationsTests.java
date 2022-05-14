/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.account.validations;

import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.idb.cacao.account.elements.AccountCategory.*;
import static org.idb.cacao.account.elements.AccountSubcategory.*;

import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.idb.cacao.account.archetypes.ChartOfAccountsArchetype;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

@RunWith(JUnitPlatform.class)
public class ChartOfAccountsValidationsTests {

	/**
	 * Validates an arbitrary Chart of Accounts that should be OK
	 */
	@Test
	public void validateOKChartOfAccounts() throws Exception {
		
		// Prepare the test case scenario
		
		ValidationContext context = new ValidationContext();
		
		DocumentTemplate template = new DocumentTemplate();
		template.setFields(new ChartOfAccountsArchetype().getRequiredFields());
		context.setDocumentTemplate(template);
		
		List<Map<String, Object>> records = new LinkedList<>();
		records.add(newCOARecord("1000", ASSET.getIfrsNumber(), ASSET_CASH.getIfrsNumber()));
		records.add(newCOARecord("1010", ASSET.getIfrsNumber(), ASSET_INVENTORY.getIfrsNumber()));
		records.add(newCOARecord("2000", LIABILITY.getIfrsNumber(), LIABILITY_PAYABLE.getIfrsNumber()));
		records.add(newCOARecord("3000", EQUITY.getIfrsNumber(), EQUITY_OWNERS.getIfrsNumber()));
		records.add(newCOARecord("4000", REVENUE.getIfrsNumber(), REVENUE_NET.getIfrsNumber()));
		records.add(newCOARecord("5000", EXPENSE.getIfrsNumber(), EXPENSE_COST.getIfrsNumber()));
		
		boolean result = ChartOfAccountsValidations.validateDocumentUploaded(context, records);
		assertTrue(result, "The result of validation of Chart of Accounts should be OK! Alerts: "+context.getAlerts());
	}

	/**
	 * Validates an arbitrary Chart of Accounts that should be considered invalid because there are duplicated account code
	 */
	@Test
	public void validateDuplicateChartOfAccounts() throws Exception {
		
		// Prepare the test case scenario
		
		ValidationContext context = new ValidationContext();
		
		DocumentTemplate template = new DocumentTemplate();
		template.setFields(new ChartOfAccountsArchetype().getRequiredFields());
		context.setDocumentTemplate(template);
		
		List<Map<String, Object>> records = new LinkedList<>();
		records.add(newCOARecord("1000", ASSET.getIfrsNumber(), ASSET_CASH.getIfrsNumber()));
		records.add(newCOARecord("1010", ASSET.getIfrsNumber(), ASSET_INVENTORY.getIfrsNumber()));
		records.add(newCOARecord("2000", LIABILITY.getIfrsNumber(), LIABILITY_PAYABLE.getIfrsNumber()));
		records.add(newCOARecord("3000", EQUITY.getIfrsNumber(), EQUITY_OWNERS.getIfrsNumber()));
		records.add(newCOARecord("4000", REVENUE.getIfrsNumber(), REVENUE_NET.getIfrsNumber()));
		records.add(newCOARecord("4000", EXPENSE.getIfrsNumber(), EXPENSE_COST.getIfrsNumber()));	// <==== DUPLICATE!
		
		boolean result = ChartOfAccountsValidations.validateDocumentUploaded(context, records);
		assertFalse(result, "The result of validation of Chart of Accounts should be NOT OK!");
		assertNotNull(context.getAlerts());
		assertFalse(context.getAlerts().isEmpty());
		assertEquals("{account.error.account.duplicate(4000)}",context.getAlerts().get(0));
	}

	/**
	 * Validates an arbitrary Chart of Accounts that should be considered invalid because there are no accounts defined in ASSETS category
	 */
	@Test
	public void validateMissingAssetsInChartOfAccounts() throws Exception {
		
		// Prepare the test case scenario
		
		ValidationContext context = new ValidationContext();
		
		DocumentTemplate template = new DocumentTemplate();
		template.setFields(new ChartOfAccountsArchetype().getRequiredFields());
		context.setDocumentTemplate(template);
		
		List<Map<String, Object>> records = new LinkedList<>();
		records.add(newCOARecord("2000", LIABILITY.getIfrsNumber(), LIABILITY_PAYABLE.getIfrsNumber()));
		records.add(newCOARecord("3000", EQUITY.getIfrsNumber(), EQUITY_OWNERS.getIfrsNumber()));
		records.add(newCOARecord("4000", REVENUE.getIfrsNumber(), REVENUE_NET.getIfrsNumber()));
		records.add(newCOARecord("5000", EXPENSE.getIfrsNumber(), EXPENSE_COST.getIfrsNumber()));
		
		boolean result = ChartOfAccountsValidations.validateDocumentUploaded(context, records);
		assertFalse(result, "The result of validation of Chart of Accounts should be NOT OK!");
		assertNotNull(context.getAlerts());
		assertFalse(context.getAlerts().isEmpty());
		assertEquals("{account.error.missing.assets}",context.getAlerts().get(0));
	}

	/**
	 * Validates an arbitrary Chart of Accounts that should be considered invalid because there are no accounts defined in LIABILITIES category
	 */
	@Test
	public void validateMissingLiabilitiesInChartOfAccounts() throws ParseException {
		
		// Prepare the test case scenario
		
		ValidationContext context = new ValidationContext();
		
		DocumentTemplate template = new DocumentTemplate();
		template.setFields(new ChartOfAccountsArchetype().getRequiredFields());
		context.setDocumentTemplate(template);
		
		List<Map<String, Object>> records = new LinkedList<>();
		records.add(newCOARecord("1000", ASSET.getIfrsNumber(), ASSET_CASH.getIfrsNumber()));
		records.add(newCOARecord("1010", ASSET.getIfrsNumber(), ASSET_INVENTORY.getIfrsNumber()));
		records.add(newCOARecord("3000", EQUITY.getIfrsNumber(), EQUITY_OWNERS.getIfrsNumber()));
		records.add(newCOARecord("4000", REVENUE.getIfrsNumber(), REVENUE_NET.getIfrsNumber()));
		records.add(newCOARecord("5000", EXPENSE.getIfrsNumber(), EXPENSE_COST.getIfrsNumber()));
		
		boolean result = ChartOfAccountsValidations.validateDocumentUploaded(context, records);
		assertFalse(result, "The result of validation of Chart of Accounts should be NOT OK!");
		assertNotNull(context.getAlerts());
		assertFalse(context.getAlerts().isEmpty());
		assertEquals("{account.error.missing.liabilities}",context.getAlerts().get(0));
	}

	/**
	 * Validates an arbitrary Chart of Accounts that should be considered invalid because there are no accounts defined in EQUITY category
	 */
	@Test
	public void validateMissingEquityInChartOfAccounts() throws ParseException {
		
		// Prepare the test case scenario
		
		ValidationContext context = new ValidationContext();
		
		DocumentTemplate template = new DocumentTemplate();
		template.setFields(new ChartOfAccountsArchetype().getRequiredFields());
		context.setDocumentTemplate(template);
		
		List<Map<String, Object>> records = new LinkedList<>();
		records.add(newCOARecord("1000", ASSET.getIfrsNumber(), ASSET_CASH.getIfrsNumber()));
		records.add(newCOARecord("1010", ASSET.getIfrsNumber(), ASSET_INVENTORY.getIfrsNumber()));
		records.add(newCOARecord("2000", LIABILITY.getIfrsNumber(), LIABILITY_PAYABLE.getIfrsNumber()));
		records.add(newCOARecord("4000", REVENUE.getIfrsNumber(), REVENUE_NET.getIfrsNumber()));
		records.add(newCOARecord("5000", EXPENSE.getIfrsNumber(), EXPENSE_COST.getIfrsNumber()));
		
		boolean result = ChartOfAccountsValidations.validateDocumentUploaded(context, records);
		assertFalse(result, "The result of validation of Chart of Accounts should be NOT OK!");
		assertNotNull(context.getAlerts());
		assertFalse(context.getAlerts().isEmpty());
		assertEquals("{account.error.missing.equity}",context.getAlerts().get(0));
	}

	/**
	 * Validates an arbitrary Chart of Accounts that should be considered invalid because some account has a subcategory
	 * not related to the category
	 */
	@Test
	public void validateMisplacedChartOfAccounts() throws ParseException {
		
		// Prepare the test case scenario
		
		ValidationContext context = new ValidationContext();
		
		DocumentTemplate template = new DocumentTemplate();
		template.setFields(new ChartOfAccountsArchetype().getRequiredFields());
		context.setDocumentTemplate(template);
		
		List<Map<String, Object>> records = new LinkedList<>();
		records.add(newCOARecord("1000", ASSET.getIfrsNumber(), ASSET_CASH.getIfrsNumber()));
		records.add(newCOARecord("1010", ASSET.getIfrsNumber(), ASSET_INVENTORY.getIfrsNumber()));
		records.add(newCOARecord("2000", LIABILITY.getIfrsNumber(), LIABILITY_PAYABLE.getIfrsNumber()));	
		records.add(newCOARecord("3000", EQUITY.getIfrsNumber(), EQUITY_OWNERS.getIfrsNumber()));
		records.add(newCOARecord("4000", REVENUE.getIfrsNumber(), REVENUE_NET.getIfrsNumber()));
		records.add(newCOARecord("5000", REVENUE.getIfrsNumber(), EXPENSE_COST.getIfrsNumber()));	// <==== UNRELATED!
		
		boolean result = ChartOfAccountsValidations.validateDocumentUploaded(context, records);
		assertFalse(result, "The result of validation of Chart of Accounts should be NOT OK!");
		assertNotNull(context.getAlerts());
		assertFalse(context.getAlerts().isEmpty());
		assertEquals("{account.error.account.subcategory.misplaced(5000,4,5.2.1)}",context.getAlerts().get(0));
	}

	/**
	 * Utility method for this test case
	 */
	private static Map<String, Object> newCOARecord(String code, String category, String subcategory) throws ParseException {
		Map<String, Object> record = new HashMap<>();
		record.put(AccountCode.name(), code);
		record.put(AccountCategory.name(), category);
		record.put(AccountSubcategory.name(), subcategory);
		return record;
	}
}
