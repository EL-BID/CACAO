/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.account.validations;

import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.idb.cacao.account.elements.StatementComprehensiveIncome;
import org.junit.jupiter.api.Test;

/**
 * Tests over the computed StatementComprehensiveIncome
 * 
 * @author Gustavo Figueiredo
 *
 */
@RunWith(JUnitPlatform.class)
public class StatementComprehensiveIncomeTests {

	/**
	 * Check the simplified 'formula parser' related to StatementComprehensiveIncome
	 */
	@Test
	public void validateFormulas() throws Exception {
		
		// Test data
		Map<StatementComprehensiveIncome, Number> data = new HashMap<>();
		
		// No data should result in zeroes for 'formula based' and NULL for the rest
		for (StatementComprehensiveIncome entry: StatementComprehensiveIncome.values()) {
			if (entry.getFormula()==null) {
				assertNull(entry.computeFormula(data::get));
			}
			else {
				assertEquals(0.0, entry.computeFormula(data::get));
			}
		}
		
		// Let's consider one value
		data.put(StatementComprehensiveIncome.REVENUE_NET, 1_000_000.0);
		
		// Test the computations
		checkFormulaAndUpdateData(StatementComprehensiveIncome.GROSS_PROFIT, 1_000_000.0, data);
		checkFormulaAndUpdateData(StatementComprehensiveIncome.TOTAL_OPERATING_EXPENSES, 0.0, data);
		checkFormulaAndUpdateData(StatementComprehensiveIncome.OPERATING_INCOME, 1_000_000.0, data);
		checkFormulaAndUpdateData(StatementComprehensiveIncome.INCOME_BEFORE_TAXES, 1_000_000.0, data);
		checkFormulaAndUpdateData(StatementComprehensiveIncome.NET_INCOME, 1_000_000.0, data);
		
		// Let's consider two values
		data.clear();
		data.put(StatementComprehensiveIncome.REVENUE_NET, 1_000_000.0);
		data.put(StatementComprehensiveIncome.EXPENSE_COST, 500_000.0);

		// Test the computations
		checkFormulaAndUpdateData(StatementComprehensiveIncome.GROSS_PROFIT, 500_000.0, data);
		checkFormulaAndUpdateData(StatementComprehensiveIncome.TOTAL_OPERATING_EXPENSES, 0.0, data);
		checkFormulaAndUpdateData(StatementComprehensiveIncome.OPERATING_INCOME, 500_000.0, data);
		checkFormulaAndUpdateData(StatementComprehensiveIncome.INCOME_BEFORE_TAXES, 500_000.0, data);
		checkFormulaAndUpdateData(StatementComprehensiveIncome.NET_INCOME, 500_000.0, data);
		
		// Let's consider more values
		data.clear();
		data.put(StatementComprehensiveIncome.REVENUE_NET, 1_000_000.0);
		data.put(StatementComprehensiveIncome.EXPENSE_COST, 500_000.0);
		data.put(StatementComprehensiveIncome.EXPENSE_ADMIN, 20_000.0);
		data.put(StatementComprehensiveIncome.EXPENSE_OPERATING, 30_000.0);
		data.put(StatementComprehensiveIncome.EXPENSE_OPERATING_OTHER, 10_000.0);
		data.put(StatementComprehensiveIncome.GAINS_LOSSES, -40_000.0);
		data.put(StatementComprehensiveIncome.TAXES_INCOME, 100_000.0);

		// Test the computations
		checkFormulaAndUpdateData(StatementComprehensiveIncome.GROSS_PROFIT, 500_000.0, data);
		checkFormulaAndUpdateData(StatementComprehensiveIncome.TOTAL_OPERATING_EXPENSES, 60_000.0, data);
		checkFormulaAndUpdateData(StatementComprehensiveIncome.OPERATING_INCOME, 440_000.0, data);
		checkFormulaAndUpdateData(StatementComprehensiveIncome.INCOME_BEFORE_TAXES, 400_000.0, data);
		checkFormulaAndUpdateData(StatementComprehensiveIncome.NET_INCOME, 300_000.0, data);
	}
	
	/**
	 * Compute the value according to a statement entry formula. Compare the result of computation
	 * with the expected value. Updates the 'data' with the computed value in order to compute the
	 * rest.
	 */
	public static void checkFormulaAndUpdateData(
			StatementComprehensiveIncome entryToCompute,
			Number expectedValue,
			Map<StatementComprehensiveIncome, Number> data) throws Exception {

		Number computed = entryToCompute.computeFormula(data::get);
		assertEquals(expectedValue, computed);
		data.put(entryToCompute, computed);
	}
}
