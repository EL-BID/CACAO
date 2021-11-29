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
