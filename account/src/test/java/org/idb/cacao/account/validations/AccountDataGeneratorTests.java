package org.idb.cacao.account.validations;

import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Map;

import org.idb.cacao.account.archetypes.ChartOfAccountsArchetype;
import org.idb.cacao.account.archetypes.GeneralLedgerArchetype;
import org.idb.cacao.account.archetypes.OpeningBalanceArchetype;
import org.idb.cacao.account.generator.AccountDataGenerator;
import org.idb.cacao.account.generator.SampleChartOfAccounts;
import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.junit.jupiter.api.Test;

/**
 * Tests the generation of random data for accounting
 * 
 * @author Gustavo Figueiredo
 *
 */
@RunWith(JUnitPlatform.class)
public class AccountDataGeneratorTests {

	/**
	 * Test generation of Chart of Accounts
	 */
	@Test
	public void testGenChartOfAccounts() throws Exception {
		
		DocumentTemplate template = new DocumentTemplate();
		template.setFields(new ChartOfAccountsArchetype().getRequiredFields());
		template.setArchetype(ChartOfAccountsArchetype.NAME);
    	long seed = "TEST".hashCode();

		AccountDataGenerator gen = new AccountDataGenerator(template, DocumentFormat.XLS, seed, /*records*/-1);
		gen.start();
		try {
			long count_records = 0;
			while (true) {
				Map<String,Object> record = gen.nextRecord();
				if (record==null)
					break;
				//System.out.println("ACC: "+record);
				assertTrue(count_records<SampleChartOfAccounts.values().length, "Unexpected number of records created!");
				count_records++;
			}
			assertEquals(SampleChartOfAccounts.values().length, count_records, "Unexpected number of records created!");
		}
		finally {
			gen.close();
		}
	}
	
	/**
	 * Test generation of Opening Balance
	 */
	@Test
	public void testGenOpeningBalance() throws Exception {
		
		DocumentTemplate template = new DocumentTemplate();
		template.setFields(new OpeningBalanceArchetype().getRequiredFields());
		template.setArchetype(OpeningBalanceArchetype.NAME);
    	long seed = "TEST".hashCode();

		AccountDataGenerator gen = new AccountDataGenerator(template, DocumentFormat.XLS, seed, /*records*/-1);
		gen.start();
		try {
			double total_debit = 0;
			double total_credit = 0;
			long count_records = 0;
			while (true) {
				Map<String,Object> record = gen.nextRecord();
				if (record==null)
					break;
				//System.out.println("OB: "+record);
				Number balance = (Number)record.get(OpeningBalanceArchetype.FIELDS_NAMES.InitialBalance.name());
				assertNotNull(balance, "Missing initial balance amount");
				String dc = (String)record.get(OpeningBalanceArchetype.FIELDS_NAMES.DebitCredit.name());
				assertNotNull(dc, "Missing D/C indication");
				assertTrue("D".equals(dc) || "C".equals(dc), "Wrong value for DebitCredit: "+dc);
				if ("D".equals(dc)) {
					total_debit += balance.doubleValue();
				}
				else {
					total_credit += balance.doubleValue();
				}
				assertTrue(count_records<SampleChartOfAccounts.values().length, "Unexpected number of records created!");
				count_records++;
			}
			assertEquals(SampleChartOfAccounts.values().length, count_records, "Unexpected number of records created!");
			assertEquals(total_debit, total_credit, /*tolerance*/0.01, "Total debits should equal total credits!");
		}
		finally {
			gen.close();
		}

	}

	/**
	 * Test generation of General Ledger
	 */
	@Test
	public void testGenGeneralLedger() throws Exception {
		
		DocumentTemplate template = new DocumentTemplate();
		template.setFields(new GeneralLedgerArchetype().getRequiredFields());
		template.setArchetype(GeneralLedgerArchetype.NAME);
    	long seed = "TEST".hashCode();

		AccountDataGenerator gen = new AccountDataGenerator(template, DocumentFormat.XLS, seed, /*records*/10_000);
		gen.start();
		try {
			double total_debit = 0;
			double total_credit = 0;
			long count_records = 0;
			LocalDate prev_date = null;
			while (true) {
				Map<String,Object> record = gen.nextRecord();
				if (record==null)
					break;
				//System.out.println("GL: "+record);
				LocalDate date = (LocalDate)record.get(GeneralLedgerArchetype.FIELDS_NAMES.Date.name());
				assertNotNull(date, "Missing entry date");
				if (prev_date==null) {
					prev_date = date;
				}
				else if (!prev_date.equals(date)) {
					assertEquals(total_debit, total_credit, /*tolerance*/0.01, "Total debits should equal total credits each day!");
					total_debit = total_credit = 0;
					prev_date = date;
				}
				Number amount = (Number)record.get(GeneralLedgerArchetype.FIELDS_NAMES.Amount.name());
				assertNotNull(amount, "Missing entry amount");
				String dc = (String)record.get(GeneralLedgerArchetype.FIELDS_NAMES.DebitCredit.name());
				assertNotNull(dc, "Missing D/C indication");
				assertTrue("D".equals(dc) || "C".equals(dc), "Wrong value for DebitCredit: "+dc);
				if ("D".equals(dc)) {
					total_debit += amount.doubleValue();
				}
				else {
					total_credit += amount.doubleValue();
				}
				assertTrue(count_records<10_000, "Unexpected number of records created!");
				count_records++;
			}
			assertEquals(10_000, count_records, "Unexpected number of records created!");
			assertEquals(total_debit, total_credit, /*tolerance*/0.01, "Total debits should equal total credits each day!");
		}
		finally {
			gen.close();
		}

	}
}
