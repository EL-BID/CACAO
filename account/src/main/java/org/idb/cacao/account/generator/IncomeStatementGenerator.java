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
package org.idb.cacao.account.generator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.idb.cacao.account.archetypes.AccountingGroupArchetype;
import org.idb.cacao.account.archetypes.GeneralLedgerArchetype;
import org.idb.cacao.api.errors.GeneralException;
import org.idb.cacao.api.templates.CustomDataGenerator;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.utils.RandomDataGenerator;

/**
 * Custom implementation of a 'random data generator' for data related to the income statement archetype.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class IncomeStatementGenerator implements CustomDataGenerator {

	private final RandomDataGenerator randomDataGenerator;

	private int recordsCreated;
	
	private DocumentField taxPayerIdField;
	
	private Number taxpayerId;

	private int year;
	
	private int providedYear;

	public IncomeStatementGenerator(DocumentTemplate template, DocumentFormat format, long seed, long records) 
			throws GeneralException {
		
		if (records>=0 && records!=1)
			throw new UnsupportedOperationException("When generating data for template '"+template.getName()
				+"' it's not possible to define a total number of records different than "
				+1);
		
		this.taxPayerIdField = template.getField(GeneralLedgerArchetype.FIELDS_NAMES.TaxPayerId.name());
		this.randomDataGenerator = new RandomDataGenerator(seed);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.CustomDataGenerator#start()
	 */
	@Override
	public void start() {
		recordsCreated = 0;
		
		int num_digits_for_taxpayer_id = (taxPayerIdField==null) ? 10 : Math.min(20, Math.max(1, Optional.ofNullable(taxPayerIdField.getMaxLength()).orElse(10)));
		taxpayerId = randomDataGenerator.nextRandomNumberFixedLength(num_digits_for_taxpayer_id);

		year = (providedYear==0) ? randomDataGenerator.nextRandomYear() : providedYear;
		randomDataGenerator.reseedBasedOnYear(year);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.CustomDataGenerator#getTaxpayerId()
	 */
	@Override
	public String getTaxpayerId() {
		return (taxpayerId==null) ? null : taxpayerId.toString();
	}

	@Override
	public void setTaxYear(Number year) {
		if (year==null || year.intValue()==0) {
			providedYear = 0;
		}
		else {
			providedYear = year.intValue();
			if (this.year!=0)
				this.year = providedYear;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.CustomDataGenerator#getTaxYear()
	 */
	@Override
	public Number getTaxYear() {
		return (year==0) ? ( (providedYear==0) ? null : providedYear ) : year;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.CustomDataGenerator#nextRecord()
	 */
	@Override
	public Map<String, Object> nextRecord() {
		if (recordsCreated>=1) {
			return null;
		}

		Map<String, Object> record = new HashMap<>();

		record.put(AccountingGroupArchetype.FIELDS_NAMES.TaxPayerId.name(), taxpayerId.toString());
		record.put(AccountingGroupArchetype.FIELDS_NAMES.TaxYear.name(), year);

		Number rev_net = roundDecimals(randomDataGenerator.nextRandomLogNormal(12.0, 6.0));		
		record.put("RevenueNet", rev_net);			

		double exp_margin = 0.1 + randomDataGenerator.getRandomGenerator().nextDouble() * 0.5;
		Number exp_cost = roundDecimals(rev_net.doubleValue() * exp_margin);		
		record.put("ExpenseCost", exp_cost);	
		
		double gross_profit = roundDecimals(rev_net.doubleValue() - exp_cost.doubleValue());
		record.put("GrossProfit", gross_profit);
		
		double exp_admin;
		if (randomDataGenerator.getRandomGenerator().nextInt(4)>=1) {
			double exp_admin_margin = randomDataGenerator.getRandomGenerator().nextDouble() * 0.1;
			exp_admin = roundDecimals(rev_net.doubleValue() * exp_admin_margin);	
		}
		else {
			exp_admin = 0.0;
		}
		record.put("ExpenseAdmin", exp_admin);

		double exp_op;
		if (randomDataGenerator.getRandomGenerator().nextInt(4)>=1) {
			double exp_op_margin = randomDataGenerator.getRandomGenerator().nextDouble() * 0.1;
			exp_op = roundDecimals(rev_net.doubleValue() * exp_op_margin);	
		}
		else {
			exp_op = 0.0;
		}
		record.put("ExpenseOperating", exp_op);

		double exp_op_other;
		if (randomDataGenerator.getRandomGenerator().nextInt(4)>=1) {
			double exp_op_other_margin = randomDataGenerator.getRandomGenerator().nextDouble() * 0.1;
			exp_op_other = roundDecimals(rev_net.doubleValue() * exp_op_other_margin);	
		}
		else {
			exp_op_other = 0.0;
		}
		record.put("ExpenseOperatingOther", exp_op_other);
		
		double total_op_exp = roundDecimals(exp_admin + exp_op + exp_op_other);
		record.put("TotalOperatingExpenses", total_op_exp);
		
		double op_income = roundDecimals(gross_profit - total_op_exp);
		record.put("OperatingIncome", op_income);

		double rev_nop;
		if (randomDataGenerator.getRandomGenerator().nextInt(3)>=1) {
			double rev_nop_margin = randomDataGenerator.getRandomGenerator().nextDouble() * 0.1;
			rev_nop = roundDecimals(rev_net.doubleValue() * rev_nop_margin);	
		}
		else {
			rev_nop = 0.0;
		}
		record.put("RevenueNop", rev_nop);

		double exp_nop;
		if (randomDataGenerator.getRandomGenerator().nextInt(3)>=1) {
			double exp_nop_margin = randomDataGenerator.getRandomGenerator().nextDouble() * 0.1;
			exp_nop = roundDecimals(rev_net.doubleValue() * exp_nop_margin);	
		}
		else {
			exp_nop = 0.0;
		}
		record.put("ExpenseNop", exp_nop);

		double gains_losses;
		if (randomDataGenerator.getRandomGenerator().nextInt(3)==0) {
			double gains_losses_margin = randomDataGenerator.getRandomGenerator().nextDouble() * 0.1;
			if (randomDataGenerator.nextRandomBoolean()) {
				gains_losses = roundDecimals(rev_net.doubleValue() * gains_losses_margin);
			}
			else {
				gains_losses =  roundDecimals(- rev_net.doubleValue() * gains_losses_margin);
			}
		}
		else {
			gains_losses = 0.0;
		}
		record.put("GainsLosses", gains_losses);
		
		double inc_before_taxes = roundDecimals(op_income + rev_nop - exp_nop + gains_losses);
		record.put("IncomeBeforeTaxes", inc_before_taxes);
		
		double taxes_income = (inc_before_taxes>0) ? roundDecimals(inc_before_taxes * 0.1) : 0.0;
		record.put("TaxesIncome", taxes_income);
		
		double net_income = roundDecimals(inc_before_taxes - taxes_income);
		record.put("NetIncome", net_income);

		recordsCreated++;
		
		return record;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		
	}

	/**
	 * Round to two decimals
	 */
	private static double roundDecimals(Number amount) {
		return Math.round(amount.doubleValue() * 100.0) / 100.0; // round to 2 decimals
	}

}
