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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.idb.cacao.account.archetypes.ChartOfAccountsArchetype;
import org.idb.cacao.account.archetypes.GeneralLedgerArchetype;
import org.idb.cacao.account.archetypes.OpeningBalanceArchetype;
import org.idb.cacao.account.elements.DailyAccountingFlow;
import org.idb.cacao.account.elements.StatementComprehensiveIncome;
import org.idb.cacao.account.etl.AccountingFlowProcessor;
import org.idb.cacao.account.etl.AccountingLoader;
import org.idb.cacao.account.etl.AccountingLoader.AccountingFieldNames;
import org.idb.cacao.api.DocumentSituation;
import org.idb.cacao.api.DocumentUploaded;
import org.idb.cacao.api.ETLContext;
import org.idb.cacao.api.PublishedDataFieldNames;
import org.idb.cacao.api.Taxpayer;
import org.idb.cacao.api.ValidatedDataFieldNames;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.DomainTable;
import org.idb.cacao.api.templates.TemplateArchetype;
import org.idb.cacao.api.templates.TemplateArchetypes;
import org.idb.cacao.api.utils.IndexNamesUtils;
import org.junit.jupiter.api.Test;

import static org.idb.cacao.account.elements.AccountCategory.*;
import static org.idb.cacao.account.elements.AccountSubcategory.*;

/**
 * Tests the ETL processing of accounting data
 * 
 * @author Gustavo Figueiredo
 *
 */
@RunWith(JUnitPlatform.class)
public class AccountingLoaderTests {

	/**
	 * Test the transformation of separated accounting data into one denormalized
	 * view of General Ledger
	 */
	@Test
	public void testETLForDenormalizedGeneralLedger() throws Exception {
		
		ETLContext etlContext = new ETLContext();
		
		InMemoryValidatedDataRepository inMemoryValidatedDataRepository = new InMemoryValidatedDataRepository();
		inMemoryValidatedDataRepository.addTemplateFromArchetype(new ChartOfAccountsArchetype());
		inMemoryValidatedDataRepository.addTemplateFromArchetype(new GeneralLedgerArchetype());
		inMemoryValidatedDataRepository.addTemplateFromArchetype(new OpeningBalanceArchetype());
		
		InMemoryDomainTableRepository inMemoryDomainTableRepository = new InMemoryDomainTableRepository();
		inMemoryDomainTableRepository.addBuiltIn();
		etlContext.setDomainTableRepository(inMemoryDomainTableRepository);
		
		DocumentUploaded coa = inMemoryValidatedDataRepository.addUpload(/*templateName*/ChartOfAccountsArchetype.NAME,/*templateVersion*/"1.0", 
				/*taxPayerId*/"1234", /*taxPeriodNumber*/2021, /*fileId*/UUID.randomUUID().toString());
		inMemoryValidatedDataRepository.addData(coa, 
			ChartOfAccountsArchetype.FIELDS_NAMES.AccountCode.name(), "1.00.00",
			ChartOfAccountsArchetype.FIELDS_NAMES.AccountDescription.name(), "Cash and Cash Equivalents",
			ChartOfAccountsArchetype.FIELDS_NAMES.AccountCategory.name(), ASSET.getIfrsNumber(),
			ChartOfAccountsArchetype.FIELDS_NAMES.AccountSubcategory.name(), ASSET_CASH.getIfrsNumber(),
			ChartOfAccountsArchetype.FIELDS_NAMES.TaxPayerId.name(), "1234",
			ChartOfAccountsArchetype.FIELDS_NAMES.TaxYear.name(), 2021,
			ValidatedDataFieldNames.FILE_ID.name(), coa.getFileId());
		inMemoryValidatedDataRepository.addData(coa, 
			ChartOfAccountsArchetype.FIELDS_NAMES.AccountCode.name(), "1.10.00",
			ChartOfAccountsArchetype.FIELDS_NAMES.AccountDescription.name(), "Inventory",
			ChartOfAccountsArchetype.FIELDS_NAMES.AccountCategory.name(), ASSET.getIfrsNumber(),
			ChartOfAccountsArchetype.FIELDS_NAMES.AccountSubcategory.name(), ASSET_INVENTORY.getIfrsNumber(),
			ChartOfAccountsArchetype.FIELDS_NAMES.TaxPayerId.name(), "1234",
			ChartOfAccountsArchetype.FIELDS_NAMES.TaxYear.name(), 2021,
			ValidatedDataFieldNames.FILE_ID.name(), coa.getFileId());
		inMemoryValidatedDataRepository.addData(coa, 
			ChartOfAccountsArchetype.FIELDS_NAMES.AccountCode.name(), "3.00.00",
			ChartOfAccountsArchetype.FIELDS_NAMES.AccountDescription.name(), "Owners Equity",
			ChartOfAccountsArchetype.FIELDS_NAMES.AccountCategory.name(), EQUITY.getIfrsNumber(),
			ChartOfAccountsArchetype.FIELDS_NAMES.AccountSubcategory.name(), EQUITY_OWNERS.getIfrsNumber(),
			ChartOfAccountsArchetype.FIELDS_NAMES.TaxPayerId.name(), "1234",
			ChartOfAccountsArchetype.FIELDS_NAMES.TaxYear.name(), 2021,
			ValidatedDataFieldNames.FILE_ID.name(), coa.getFileId());
		
		DocumentUploaded gl = inMemoryValidatedDataRepository.addUpload(/*templateName*/GeneralLedgerArchetype.NAME,/*templateVersion*/"1.0", 
				/*taxPayerId*/"1234", /*taxPeriodNumber*/2021, /*fileId*/UUID.randomUUID().toString());
		inMemoryValidatedDataRepository.addData(gl, 
			GeneralLedgerArchetype.FIELDS_NAMES.AccountCode.name(), "1.00.00",
			GeneralLedgerArchetype.FIELDS_NAMES.Date.name(), LocalDate.of(2021, 1, 1),
			GeneralLedgerArchetype.FIELDS_NAMES.EntryId.name(), "L0001",
			GeneralLedgerArchetype.FIELDS_NAMES.Amount.name(), 100.0,
			GeneralLedgerArchetype.FIELDS_NAMES.DebitCredit.name(), "C",
			GeneralLedgerArchetype.FIELDS_NAMES.Description.name(), "Acquisition of material",
			GeneralLedgerArchetype.FIELDS_NAMES.TaxPayerId.name(), "1234",
			GeneralLedgerArchetype.FIELDS_NAMES.TaxYear.name(), 2021,
			ValidatedDataFieldNames.FILE_ID.name(), gl.getFileId());
		inMemoryValidatedDataRepository.addData(gl, 
			GeneralLedgerArchetype.FIELDS_NAMES.AccountCode.name(), "1.10.00",
			GeneralLedgerArchetype.FIELDS_NAMES.Date.name(), LocalDate.of(2021, 1, 1),
			GeneralLedgerArchetype.FIELDS_NAMES.EntryId.name(), "L0001",
			GeneralLedgerArchetype.FIELDS_NAMES.Amount.name(), 100.0,
			GeneralLedgerArchetype.FIELDS_NAMES.DebitCredit.name(), "D",
			GeneralLedgerArchetype.FIELDS_NAMES.Description.name(), "Acquisition of material",
			GeneralLedgerArchetype.FIELDS_NAMES.TaxPayerId.name(), "1234",
			GeneralLedgerArchetype.FIELDS_NAMES.TaxYear.name(), 2021,
			ValidatedDataFieldNames.FILE_ID.name(), gl.getFileId());
		
		DocumentUploaded ob = inMemoryValidatedDataRepository.addUpload(/*templateName*/OpeningBalanceArchetype.NAME,/*templateVersion*/"1.0", 
				/*taxPayerId*/"1234", /*taxPeriodNumber*/2021, /*fileId*/UUID.randomUUID().toString());
		inMemoryValidatedDataRepository.addData(ob, 
			OpeningBalanceArchetype.FIELDS_NAMES.AccountCode.name(), "1.00.00",
			OpeningBalanceArchetype.FIELDS_NAMES.InitialBalance.name(), 10_000.0,
			OpeningBalanceArchetype.FIELDS_NAMES.DebitCredit.name(), "D",
			OpeningBalanceArchetype.FIELDS_NAMES.TaxPayerId.name(), "1234",
			OpeningBalanceArchetype.FIELDS_NAMES.TaxYear.name(), 2021,
			ValidatedDataFieldNames.FILE_ID.name(), ob.getFileId());
		inMemoryValidatedDataRepository.addData(ob, 
			OpeningBalanceArchetype.FIELDS_NAMES.AccountCode.name(), "3.00.00",
			OpeningBalanceArchetype.FIELDS_NAMES.InitialBalance.name(), 10_000.0,
			OpeningBalanceArchetype.FIELDS_NAMES.DebitCredit.name(), "C",
			OpeningBalanceArchetype.FIELDS_NAMES.TaxPayerId.name(), "1234",
			OpeningBalanceArchetype.FIELDS_NAMES.TaxYear.name(), 2021,
			ValidatedDataFieldNames.FILE_ID.name(), ob.getFileId());

		etlContext.setDocumentTemplate(inMemoryValidatedDataRepository.getTemplate(GeneralLedgerArchetype.NAME));
		etlContext.setDocumentUploaded(gl);
		
		InMemoryTaxpayerRepository inMemoryTaxpayerRepository = new InMemoryTaxpayerRepository();
		inMemoryTaxpayerRepository.addTaxpayer(new Taxpayer()
				.withTaxPayerId("1234")
				.withName("JOHN SMITH LLC")
				.withAddress("Elm Street 24th")
				.withQualifier1("Small business"));
		etlContext.setTaxpayerRepository(inMemoryTaxpayerRepository);
		
		etlContext.setValidatedDataRepository(inMemoryValidatedDataRepository);
		
		InMemoryLoadDataStrategy inMemoryLoadStrategy = new InMemoryLoadDataStrategy();
		etlContext.setLoadDataStrategy(inMemoryLoadStrategy);
		
		boolean result = AccountingLoader.performETL(etlContext);
		assertTrue(result);
		
		// Verifies the published General Ledger
		
		assertEquals(2, inMemoryLoadStrategy.getRecords(AccountingLoader.INDEX_PUBLISHED_GENERAL_LEDGER).size());
		
		// Check direct fields from original Ledger 'file'
		Map<String,Object> record = inMemoryLoadStrategy.getRecords(AccountingLoader.INDEX_PUBLISHED_GENERAL_LEDGER).get(0);
		assertEquals("1234", record.get(IndexNamesUtils.formatFieldName(PublishedDataFieldNames.TAXPAYER_ID.name())));
		assertEquals(2021, record.get(IndexNamesUtils.formatFieldName(PublishedDataFieldNames.TAXPERIOD_NUMBER.name())));
		assertEquals("1.00.00", record.get(IndexNamesUtils.formatFieldName(GeneralLedgerArchetype.FIELDS_NAMES.AccountCode.name())));
		assertEquals(100.0, record.get(IndexNamesUtils.formatFieldName(GeneralLedgerArchetype.FIELDS_NAMES.Amount.name())));
		assertEquals("C", record.get(IndexNamesUtils.formatFieldName(GeneralLedgerArchetype.FIELDS_NAMES.DebitCredit.name())));
		
		// Check 'derived' fields (due to denormalization)
		assertEquals("JOHN SMITH LLC", record.get(IndexNamesUtils.formatFieldName("TaxPayerName")));
		assertEquals("Elm Street 24th", record.get(IndexNamesUtils.formatFieldName("TaxPayerAddress")));
		assertEquals("Small business", record.get(IndexNamesUtils.formatFieldName("TaxPayerQualifier1")));
		assertEquals("Cash and Cash Equivalents", record.get(IndexNamesUtils.formatFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountDescription.name())));
		assertEquals(ASSET.getIfrsNumber(), record.get(IndexNamesUtils.formatFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountCategory.name())));
		assertEquals(ASSET.toString(), record.get(IndexNamesUtils.formatFieldName(AccountingFieldNames.AccountCategoryName.name())));
		assertEquals(ASSET_CASH.getIfrsNumber(), record.get(IndexNamesUtils.formatFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountSubcategory.name())));
		assertEquals(ASSET_CASH.toString(), record.get(IndexNamesUtils.formatFieldName(AccountingFieldNames.AccountSubcategoryName.name())));
		assertEquals(9900.0, record.get(IndexNamesUtils.formatFieldName(AccountingFieldNames.Balance.name())));
		assertEquals("D", record.get(IndexNamesUtils.formatFieldName(AccountingFieldNames.BalanceDebitCredit.name())));

		
		// Verifies the published Monthly Balance Sheets

		assertEquals(3 * 12, inMemoryLoadStrategy.getRecords(AccountingLoader.INDEX_PUBLISHED_BALANCE_SHEET).size(), 
				"There should be 12 monthly balance sheets for three accounts: 12 for each account referenced in General Ledger (two accounts) and 12 more for an additional account in Opening Balance that remained unchanged");
		
		List<Map<String,Object>> balance_sheets_cash =
		inMemoryLoadStrategy.getRecords(AccountingLoader.INDEX_PUBLISHED_BALANCE_SHEET).stream()
			.filter(sheet->"1.00.00".equals(sheet.get(IndexNamesUtils.formatFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountCode.name()))))
			.collect(Collectors.toList());
		assertEquals(12, balance_sheets_cash.size());
		assertEquals(10_000.0, balance_sheets_cash.get(0).get(IndexNamesUtils.formatFieldName(OpeningBalanceArchetype.FIELDS_NAMES.InitialBalance.name())));
		assertEquals(9_900.0, balance_sheets_cash.get(0).get(IndexNamesUtils.formatFieldName(AccountingLoader.AccountingFieldNames.FinalBalance.name())));
		for (int i=1; i<12; i++) {
			assertEquals(9_900.0, balance_sheets_cash.get(i).get(IndexNamesUtils.formatFieldName(OpeningBalanceArchetype.FIELDS_NAMES.InitialBalance.name())));
			assertEquals(9_900.0, balance_sheets_cash.get(i).get(IndexNamesUtils.formatFieldName(AccountingLoader.AccountingFieldNames.FinalBalance.name())));			
		}

		List<Map<String,Object>> balance_sheets_inventory =
		inMemoryLoadStrategy.getRecords(AccountingLoader.INDEX_PUBLISHED_BALANCE_SHEET).stream()
			.filter(sheet->"1.10.00".equals(sheet.get(IndexNamesUtils.formatFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountCode.name()))))
			.collect(Collectors.toList());
		assertEquals(12, balance_sheets_inventory.size());
		assertEquals(0.0, balance_sheets_inventory.get(0).get(IndexNamesUtils.formatFieldName(OpeningBalanceArchetype.FIELDS_NAMES.InitialBalance.name())));
		assertEquals(100.0, balance_sheets_inventory.get(0).get(IndexNamesUtils.formatFieldName(AccountingLoader.AccountingFieldNames.FinalBalance.name())));
		for (int i=1; i<12; i++) {
			assertEquals(100.0, balance_sheets_inventory.get(i).get(IndexNamesUtils.formatFieldName(OpeningBalanceArchetype.FIELDS_NAMES.InitialBalance.name())));
			assertEquals(100.0, balance_sheets_inventory.get(i).get(IndexNamesUtils.formatFieldName(AccountingLoader.AccountingFieldNames.FinalBalance.name())));			
		}
		
		List<Map<String,Object>> balance_sheets_equity =
		inMemoryLoadStrategy.getRecords(AccountingLoader.INDEX_PUBLISHED_BALANCE_SHEET).stream()
			.filter(sheet->"3.00.00".equals(sheet.get(IndexNamesUtils.formatFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountCode.name()))))
			.collect(Collectors.toList());
		assertEquals(12, balance_sheets_equity.size());
		for (int i=0; i<12; i++) {
			assertEquals(10_000.0, balance_sheets_equity.get(i).get(IndexNamesUtils.formatFieldName(OpeningBalanceArchetype.FIELDS_NAMES.InitialBalance.name())));
			assertEquals(10_000.0, balance_sheets_equity.get(i).get(IndexNamesUtils.formatFieldName(AccountingLoader.AccountingFieldNames.FinalBalance.name())));			
		}

		// Verifies the published Daily Accounting Flows

		assertEquals(1, inMemoryLoadStrategy.getRecords(AccountingLoader.INDEX_PUBLISHED_ACCOUNTING_FLOW).size(), 
				"There should be one accounting flow regarding the pair of bookeeping entries");

		Map<String,Object> flow_record = inMemoryLoadStrategy.getRecords(AccountingLoader.INDEX_PUBLISHED_ACCOUNTING_FLOW).get(0);
		assertEquals("1.10.00", flow_record.get("debited_account"));
		assertEquals(ASSET.getIfrsNumber(), flow_record.get("debited_account_category"));
		assertEquals("account.category.asset", flow_record.get("debited_account_category_name"));
		assertEquals(ASSET_INVENTORY.getIfrsNumber(), flow_record.get("debited_account_subcategory"));
		
		assertEquals("1.00.00", flow_record.get("credited_account"));
		assertEquals(ASSET.getIfrsNumber(), flow_record.get("credited_account_category"));
		assertEquals("account.category.asset", flow_record.get("credited_account_category_name"));
		assertEquals(ASSET_CASH.getIfrsNumber(), flow_record.get("credited_account_subcategory"));
	}
	
	/**
	 * Another test with different journal entries for testing the calculated Statement of Incomes
	 */
	@Test
	public void testETLForDenormalizedGeneralLedger2() throws Exception {
		
		ETLContext etlContext = new ETLContext();
		
		InMemoryValidatedDataRepository inMemoryValidatedDataRepository = new InMemoryValidatedDataRepository();
		inMemoryValidatedDataRepository.addTemplateFromArchetype(new ChartOfAccountsArchetype());
		inMemoryValidatedDataRepository.addTemplateFromArchetype(new GeneralLedgerArchetype());
		inMemoryValidatedDataRepository.addTemplateFromArchetype(new OpeningBalanceArchetype());
		
		InMemoryDomainTableRepository inMemoryDomainTableRepository = new InMemoryDomainTableRepository();
		inMemoryDomainTableRepository.addBuiltIn();
		etlContext.setDomainTableRepository(inMemoryDomainTableRepository);
		
		DocumentUploaded coa = inMemoryValidatedDataRepository.addUpload(/*templateName*/ChartOfAccountsArchetype.NAME,/*templateVersion*/"1.0", 
				/*taxPayerId*/"1234", /*taxPeriodNumber*/2021, /*fileId*/UUID.randomUUID().toString());
		inMemoryValidatedDataRepository.addData(coa, 
			ChartOfAccountsArchetype.FIELDS_NAMES.AccountCode.name(), "1.00.00",
			ChartOfAccountsArchetype.FIELDS_NAMES.AccountDescription.name(), "Cash and Cash Equivalents",
			ChartOfAccountsArchetype.FIELDS_NAMES.AccountCategory.name(), ASSET.getIfrsNumber(),
			ChartOfAccountsArchetype.FIELDS_NAMES.AccountSubcategory.name(), ASSET_CASH.getIfrsNumber(),
			ChartOfAccountsArchetype.FIELDS_NAMES.TaxPayerId.name(), "1234",
			ChartOfAccountsArchetype.FIELDS_NAMES.TaxYear.name(), 2021,
			ValidatedDataFieldNames.FILE_ID.name(), coa.getFileId());
		inMemoryValidatedDataRepository.addData(coa, 
			ChartOfAccountsArchetype.FIELDS_NAMES.AccountCode.name(), "1.10.00",
			ChartOfAccountsArchetype.FIELDS_NAMES.AccountDescription.name(), "Inventory",
			ChartOfAccountsArchetype.FIELDS_NAMES.AccountCategory.name(), ASSET.getIfrsNumber(),
			ChartOfAccountsArchetype.FIELDS_NAMES.AccountSubcategory.name(), ASSET_INVENTORY.getIfrsNumber(),
			ChartOfAccountsArchetype.FIELDS_NAMES.TaxPayerId.name(), "1234",
			ChartOfAccountsArchetype.FIELDS_NAMES.TaxYear.name(), 2021,
			ValidatedDataFieldNames.FILE_ID.name(), coa.getFileId());
		inMemoryValidatedDataRepository.addData(coa, 
			ChartOfAccountsArchetype.FIELDS_NAMES.AccountCode.name(), "3.00.00",
			ChartOfAccountsArchetype.FIELDS_NAMES.AccountDescription.name(), "Owners Equity",
			ChartOfAccountsArchetype.FIELDS_NAMES.AccountCategory.name(), EQUITY.getIfrsNumber(),
			ChartOfAccountsArchetype.FIELDS_NAMES.AccountSubcategory.name(), EQUITY_OWNERS.getIfrsNumber(),
			ChartOfAccountsArchetype.FIELDS_NAMES.TaxPayerId.name(), "1234",
			ChartOfAccountsArchetype.FIELDS_NAMES.TaxYear.name(), 2021,
			ValidatedDataFieldNames.FILE_ID.name(), coa.getFileId());
		inMemoryValidatedDataRepository.addData(coa, 
			ChartOfAccountsArchetype.FIELDS_NAMES.AccountCode.name(), "5.00.00",
			ChartOfAccountsArchetype.FIELDS_NAMES.AccountDescription.name(), "Cost of Sales",
			ChartOfAccountsArchetype.FIELDS_NAMES.AccountCategory.name(), EXPENSE.getIfrsNumber(),
			ChartOfAccountsArchetype.FIELDS_NAMES.AccountSubcategory.name(), EXPENSE_COST.getIfrsNumber(),
			ChartOfAccountsArchetype.FIELDS_NAMES.TaxPayerId.name(), "1234",
			ChartOfAccountsArchetype.FIELDS_NAMES.TaxYear.name(), 2021,
			ValidatedDataFieldNames.FILE_ID.name(), coa.getFileId());
		inMemoryValidatedDataRepository.addData(coa, 
			ChartOfAccountsArchetype.FIELDS_NAMES.AccountCode.name(), "4.00.00",
			ChartOfAccountsArchetype.FIELDS_NAMES.AccountDescription.name(), "Revenue from Services",
			ChartOfAccountsArchetype.FIELDS_NAMES.AccountCategory.name(), REVENUE.getIfrsNumber(),
			ChartOfAccountsArchetype.FIELDS_NAMES.AccountSubcategory.name(), REVENUE_NET.getIfrsNumber(),
			ChartOfAccountsArchetype.FIELDS_NAMES.TaxPayerId.name(), "1234",
			ChartOfAccountsArchetype.FIELDS_NAMES.TaxYear.name(), 2021,
			ValidatedDataFieldNames.FILE_ID.name(), coa.getFileId());
		
		DocumentUploaded gl = inMemoryValidatedDataRepository.addUpload(/*templateName*/GeneralLedgerArchetype.NAME,/*templateVersion*/"1.0", 
				/*taxPayerId*/"1234", /*taxPeriodNumber*/2021, /*fileId*/UUID.randomUUID().toString());
		inMemoryValidatedDataRepository.addData(gl, 
			GeneralLedgerArchetype.FIELDS_NAMES.AccountCode.name(), "1.00.00",
			GeneralLedgerArchetype.FIELDS_NAMES.Date.name(), LocalDate.of(2021, 1, 1),
			GeneralLedgerArchetype.FIELDS_NAMES.EntryId.name(), "L0001",
			GeneralLedgerArchetype.FIELDS_NAMES.Amount.name(), 500.0,
			GeneralLedgerArchetype.FIELDS_NAMES.DebitCredit.name(), "D",
			GeneralLedgerArchetype.FIELDS_NAMES.Description.name(), "Cash",
			GeneralLedgerArchetype.FIELDS_NAMES.TaxPayerId.name(), "1234",
			GeneralLedgerArchetype.FIELDS_NAMES.TaxYear.name(), 2021,
			ValidatedDataFieldNames.FILE_ID.name(), gl.getFileId());
		inMemoryValidatedDataRepository.addData(gl, 
			GeneralLedgerArchetype.FIELDS_NAMES.AccountCode.name(), "5.00.00",
			GeneralLedgerArchetype.FIELDS_NAMES.Date.name(), LocalDate.of(2021, 1, 1),
			GeneralLedgerArchetype.FIELDS_NAMES.EntryId.name(), "L0001",
			GeneralLedgerArchetype.FIELDS_NAMES.Amount.name(), 100.0,
			GeneralLedgerArchetype.FIELDS_NAMES.DebitCredit.name(), "D",
			GeneralLedgerArchetype.FIELDS_NAMES.Description.name(), "Cost of Goods Sold",
			GeneralLedgerArchetype.FIELDS_NAMES.TaxPayerId.name(), "1234",
			GeneralLedgerArchetype.FIELDS_NAMES.TaxYear.name(), 2021,
			ValidatedDataFieldNames.FILE_ID.name(), gl.getFileId());
		inMemoryValidatedDataRepository.addData(gl, 
			GeneralLedgerArchetype.FIELDS_NAMES.AccountCode.name(), "4.00.00",
			GeneralLedgerArchetype.FIELDS_NAMES.Date.name(), LocalDate.of(2021, 1, 1),
			GeneralLedgerArchetype.FIELDS_NAMES.EntryId.name(), "L0001",
			GeneralLedgerArchetype.FIELDS_NAMES.Amount.name(), 500.0,
			GeneralLedgerArchetype.FIELDS_NAMES.DebitCredit.name(), "C",
			GeneralLedgerArchetype.FIELDS_NAMES.Description.name(), "Revenues",
			GeneralLedgerArchetype.FIELDS_NAMES.TaxPayerId.name(), "1234",
			GeneralLedgerArchetype.FIELDS_NAMES.TaxYear.name(), 2021,
			ValidatedDataFieldNames.FILE_ID.name(), gl.getFileId());
		inMemoryValidatedDataRepository.addData(gl, 
			GeneralLedgerArchetype.FIELDS_NAMES.AccountCode.name(), "1.10.00",
			GeneralLedgerArchetype.FIELDS_NAMES.Date.name(), LocalDate.of(2021, 1, 1),
			GeneralLedgerArchetype.FIELDS_NAMES.EntryId.name(), "L0001",
			GeneralLedgerArchetype.FIELDS_NAMES.Amount.name(), 100.0,
			GeneralLedgerArchetype.FIELDS_NAMES.DebitCredit.name(), "C",
			GeneralLedgerArchetype.FIELDS_NAMES.Description.name(), "Inventory",
			GeneralLedgerArchetype.FIELDS_NAMES.TaxPayerId.name(), "1234",
			GeneralLedgerArchetype.FIELDS_NAMES.TaxYear.name(), 2021,
			ValidatedDataFieldNames.FILE_ID.name(), gl.getFileId());
		
		DocumentUploaded ob = inMemoryValidatedDataRepository.addUpload(/*templateName*/OpeningBalanceArchetype.NAME,/*templateVersion*/"1.0", 
				/*taxPayerId*/"1234", /*taxPeriodNumber*/2021, /*fileId*/UUID.randomUUID().toString());
		inMemoryValidatedDataRepository.addData(ob, 
			OpeningBalanceArchetype.FIELDS_NAMES.AccountCode.name(), "1.00.00",
			OpeningBalanceArchetype.FIELDS_NAMES.InitialBalance.name(), 5_000.0,
			OpeningBalanceArchetype.FIELDS_NAMES.DebitCredit.name(), "D",
			OpeningBalanceArchetype.FIELDS_NAMES.TaxPayerId.name(), "1234",
			OpeningBalanceArchetype.FIELDS_NAMES.TaxYear.name(), 2021,
			ValidatedDataFieldNames.FILE_ID.name(), ob.getFileId());
		inMemoryValidatedDataRepository.addData(ob, 
			OpeningBalanceArchetype.FIELDS_NAMES.AccountCode.name(), "1.10.00",
			OpeningBalanceArchetype.FIELDS_NAMES.InitialBalance.name(), 5_000.0,
			OpeningBalanceArchetype.FIELDS_NAMES.DebitCredit.name(), "D",
			OpeningBalanceArchetype.FIELDS_NAMES.TaxPayerId.name(), "1234",
			OpeningBalanceArchetype.FIELDS_NAMES.TaxYear.name(), 2021,
			ValidatedDataFieldNames.FILE_ID.name(), ob.getFileId());
		inMemoryValidatedDataRepository.addData(ob, 
			OpeningBalanceArchetype.FIELDS_NAMES.AccountCode.name(), "3.00.00",
			OpeningBalanceArchetype.FIELDS_NAMES.InitialBalance.name(), 10_000.0,
			OpeningBalanceArchetype.FIELDS_NAMES.DebitCredit.name(), "C",
			OpeningBalanceArchetype.FIELDS_NAMES.TaxPayerId.name(), "1234",
			OpeningBalanceArchetype.FIELDS_NAMES.TaxYear.name(), 2021,
			ValidatedDataFieldNames.FILE_ID.name(), ob.getFileId());

		etlContext.setDocumentTemplate(inMemoryValidatedDataRepository.getTemplate(GeneralLedgerArchetype.NAME));
		etlContext.setDocumentUploaded(gl);
		
		InMemoryTaxpayerRepository inMemoryTaxpayerRepository = new InMemoryTaxpayerRepository();
		inMemoryTaxpayerRepository.addTaxpayer(new Taxpayer()
				.withTaxPayerId("1234")
				.withName("JOHN SMITH LLC")
				.withAddress("Elm Street 24th")
				.withQualifier1("Small business"));
		etlContext.setTaxpayerRepository(inMemoryTaxpayerRepository);
		
		etlContext.setValidatedDataRepository(inMemoryValidatedDataRepository);
		
		InMemoryLoadDataStrategy inMemoryLoadStrategy = new InMemoryLoadDataStrategy();
		etlContext.setLoadDataStrategy(inMemoryLoadStrategy);
		
		boolean result = AccountingLoader.performETL(etlContext);
		assertTrue(result);
		
		// Verifies the published Statement of Income
		
		assertEquals(StatementComprehensiveIncome.values().length, inMemoryLoadStrategy.getRecords(AccountingLoader.INDEX_PUBLISHED_COMPUTED_STATEMENT_INCOME).size(), 
				"Missing computations for the Statement of Income");

		List<Map<String,Object>> statement_records = inMemoryLoadStrategy.getRecords(AccountingLoader.INDEX_PUBLISHED_COMPUTED_STATEMENT_INCOME);
		
		Map<String,Object> record_revenue_net = statement_records.stream().filter(record->StatementComprehensiveIncome.REVENUE_NET.name().equals(record.get("statement_entry_code"))).findAny().orElse(null);
		assertNotNull(record_revenue_net, "Missing revenue net calculation!");
		assertEquals(500.0, record_revenue_net.get("amount"), "The expected value should be the value retrieved from General Ledger");
		
		Map<String,Object> record_expense_cost = statement_records.stream().filter(record->StatementComprehensiveIncome.EXPENSE_COST.name().equals(record.get("statement_entry_code"))).findAny().orElse(null);
		assertNotNull(record_expense_cost, "Missing expense cost calculation!");
		assertEquals(100.0, record_expense_cost.get("amount"), "The expected value should be the value retrieved from General Ledger");
		
		Map<String,Object> record_gross_profit = statement_records.stream().filter(record->StatementComprehensiveIncome.GROSS_PROFIT.name().equals(record.get("statement_entry_code"))).findAny().orElse(null);
		assertNotNull(record_gross_profit, "Missing gross profit calculation!");
		assertEquals(400.0, record_gross_profit.get("amount"), "The expected value should be the computation of REVENUE_NET - EXPENSE_COST");

		
		// Verifies the published Daily Accounting Flows

		assertEquals(2, inMemoryLoadStrategy.getRecords(AccountingLoader.INDEX_PUBLISHED_ACCOUNTING_FLOW).size(), 
				"There should be two accounting flow regarding the two pairs of bookeeping entries");

		List<Map<String,Object>> flow_records = inMemoryLoadStrategy.getRecords(AccountingLoader.INDEX_PUBLISHED_ACCOUNTING_FLOW);
		
		// One flow of value 500
		
		Map<String,Object> flow_record = flow_records.stream().filter(record->"1.00.00".equals(record.get("debited_account"))).findAny().orElse(null);
		assertNotNull(flow_record);
		assertEquals(500.0, flow_record.get("amount"));
		
		assertEquals(ASSET.getIfrsNumber(), flow_record.get("debited_account_category"));
		assertEquals("account.category.asset", flow_record.get("debited_account_category_name"));
		assertEquals(ASSET_CASH.getIfrsNumber(), flow_record.get("debited_account_subcategory"));
		
		assertEquals("4.00.00", flow_record.get("credited_account"));
		assertEquals(REVENUE.getIfrsNumber(), flow_record.get("credited_account_category"));
		assertEquals("account.category.revenue", flow_record.get("credited_account_category_name"));
		assertEquals(REVENUE_NET.getIfrsNumber(), flow_record.get("credited_account_subcategory"));
		
		// Another flow of value 100

		flow_record = flow_records.stream().filter(record->"5.00.00".equals(record.get("debited_account"))).findAny().orElse(null);
		assertNotNull(flow_record);
		assertEquals(100.0, flow_record.get("amount"));
		
		assertEquals(EXPENSE.getIfrsNumber(), flow_record.get("debited_account_category"));
		assertEquals("account.category.expense", flow_record.get("debited_account_category_name"));
		assertEquals(EXPENSE_COST.getIfrsNumber(), flow_record.get("debited_account_subcategory"));
		
		assertEquals("1.10.00", flow_record.get("credited_account"));
		assertEquals(ASSET.getIfrsNumber(), flow_record.get("credited_account_category"));
		assertEquals("account.category.asset", flow_record.get("credited_account_category_name"));
		assertEquals(ASSET_INVENTORY.getIfrsNumber(), flow_record.get("credited_account_subcategory"));

	}

	/**
	 * Test the processing of accounting flows considering different combinations of debits and credits
	 */
	@Test
	public void testCombinationsForAccountFlows() throws Exception {
		
		AccountingFlowProcessor processor = new AccountingFlowProcessor();
		List<DailyAccountingFlow> flows = new LinkedList<>();
		processor.setCollectDailyAccountingFlows(flows::add);
		
		// Scenario 1: trivial 1 debit for 1 credit in order
		
		processor.computeEntry(OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), /*accountCode*/"01", /*amount*/1000.0, /*isDebit*/true);
		processor.computeEntry(OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), /*accountCode*/"02", /*amount*/1000.0, /*isDebit*/false);
		processor.computeEntry(OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), /*accountCode*/"03", /*amount*/200.0, /*isDebit*/true);
		processor.computeEntry(OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), /*accountCode*/"04", /*amount*/200.0, /*isDebit*/false);
		processor.computeEntry(OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), /*accountCode*/"05", /*amount*/500.0, /*isDebit*/true);
		processor.computeEntry(OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), /*accountCode*/"06", /*amount*/500.0, /*isDebit*/false);
		processor.finish();
		
		assertEquals(3, flows.size(), "Expected 3 accounting flows (one for each pair of debit/credit)");
		assertEquals(1000.0, flows.get(0).getAmount());
		assertEquals("01", flows.get(0).getDebitedAccountCode());
		assertEquals("02", flows.get(0).getCreditedAccountCode());
		assertEquals(200.0, flows.get(1).getAmount());
		assertEquals("03", flows.get(1).getDebitedAccountCode());
		assertEquals("04", flows.get(1).getCreditedAccountCode());
		assertEquals(500.0, flows.get(2).getAmount());
		assertEquals("05", flows.get(2).getDebitedAccountCode());
		assertEquals("06", flows.get(2).getCreditedAccountCode());
		
		// Scenario 2: pairs of debit:credit shuffled 
		
		flows.clear();

		processor.computeEntry(OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), /*accountCode*/"01", /*amount*/1000.0, /*isDebit*/true);
		processor.computeEntry(OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), /*accountCode*/"03", /*amount*/200.0, /*isDebit*/true);
		processor.computeEntry(OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), /*accountCode*/"05", /*amount*/500.0, /*isDebit*/true);
		processor.computeEntry(OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), /*accountCode*/"04", /*amount*/200.0, /*isDebit*/false);
		processor.computeEntry(OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), /*accountCode*/"06", /*amount*/500.0, /*isDebit*/false);
		processor.computeEntry(OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), /*accountCode*/"02", /*amount*/1000.0, /*isDebit*/false);
		processor.finish();

		assertEquals(3, flows.size(), "Expected 3 accounting flows (one for each pair of debit/credit)");
		assertEquals(1000.0, flows.get(0).getAmount());
		assertEquals("01", flows.get(0).getDebitedAccountCode());
		assertEquals("02", flows.get(0).getCreditedAccountCode());
		assertEquals(500.0, flows.get(1).getAmount());
		assertEquals("05", flows.get(1).getDebitedAccountCode());
		assertEquals("06", flows.get(1).getCreditedAccountCode());
		assertEquals(200.0, flows.get(2).getAmount());
		assertEquals("03", flows.get(2).getDebitedAccountCode());
		assertEquals("04", flows.get(2).getCreditedAccountCode());
		
		// Scenario 3: trivial 1 debit for 2 credits and 2 debits for 1 credit in order
		
		flows.clear();

		processor.computeEntry(OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), /*accountCode*/"01", /*amount*/1000.0, /*isDebit*/true);
		processor.computeEntry(OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), /*accountCode*/"02", /*amount*/200.0, /*isDebit*/false);
		processor.computeEntry(OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), /*accountCode*/"03", /*amount*/800.0, /*isDebit*/false);
		processor.computeEntry(OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), /*accountCode*/"04", /*amount*/100.0, /*isDebit*/true);
		processor.computeEntry(OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), /*accountCode*/"05", /*amount*/100.0, /*isDebit*/true);
		processor.computeEntry(OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), /*accountCode*/"06", /*amount*/200.0, /*isDebit*/false);
		processor.finish();
		
		assertEquals(4, flows.size(), "Expected 4 accounting flows (two for each triplet of debits/credits)");
		flows.stream()
			.filter(flow->flow.getAmount()==800.0 && "01".equals(flow.getDebitedAccountCode()) && "03".equals(flow.getCreditedAccountCode()))
			.findAny().orElseThrow(()->new Exception("Missing expected flow!"));
		flows.stream()
			.filter(flow->flow.getAmount()==200.0 && "01".equals(flow.getDebitedAccountCode()) && "02".equals(flow.getCreditedAccountCode()))
			.findAny().orElseThrow(()->new Exception("Missing expected flow!"));
		flows.stream()
			.filter(flow->flow.getAmount()==100.0 && "04".equals(flow.getDebitedAccountCode()) && "06".equals(flow.getCreditedAccountCode()))
			.findAny().orElseThrow(()->new Exception("Missing expected flow!"));
		flows.stream()
			.filter(flow->flow.getAmount()==100.0 && "05".equals(flow.getDebitedAccountCode()) && "06".equals(flow.getCreditedAccountCode()))
			.findAny().orElseThrow(()->new Exception("Missing expected flow!"));
	
		
		// Scenario 4: triplets of 1 debit for 2 credits and 2 debits for 1 credit shuffled
		
		flows.clear();

		processor.computeEntry(OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), /*accountCode*/"04", /*amount*/105.0, /*isDebit*/true);
		processor.computeEntry(OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), /*accountCode*/"01", /*amount*/1000.0, /*isDebit*/true);
		processor.computeEntry(OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), /*accountCode*/"05", /*amount*/105.0, /*isDebit*/true);
		processor.computeEntry(OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), /*accountCode*/"02", /*amount*/200.0, /*isDebit*/false);
		processor.computeEntry(OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), /*accountCode*/"03", /*amount*/800.0, /*isDebit*/false);
		processor.computeEntry(OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), /*accountCode*/"06", /*amount*/210.0, /*isDebit*/false);
		processor.finish();
		
		assertEquals(4, flows.size(), "Expected 4 accounting flows (two for each triplet of debits/credits)");
		flows.stream()
			.filter(flow->flow.getAmount()==800.0 && "01".equals(flow.getDebitedAccountCode()) && "03".equals(flow.getCreditedAccountCode()))
			.findAny().orElseThrow(()->new Exception("Missing expected flow!"));
		flows.stream()
			.filter(flow->flow.getAmount()==200.0 && "01".equals(flow.getDebitedAccountCode()) && "02".equals(flow.getCreditedAccountCode()))
			.findAny().orElseThrow(()->new Exception("Missing expected flow!"));
		flows.stream()
			.filter(flow->flow.getAmount()==105.0 && "04".equals(flow.getDebitedAccountCode()) && "06".equals(flow.getCreditedAccountCode()))
			.findAny().orElseThrow(()->new Exception("Missing expected flow!"));
		flows.stream()
			.filter(flow->flow.getAmount()==105.0 && "05".equals(flow.getDebitedAccountCode()) && "06".equals(flow.getCreditedAccountCode()))
			.findAny().orElseThrow(()->new Exception("Missing expected flow!"));

	}
	
	/**
	 * Simplified in-memory implementation of 'ETLContext.ValidatedDataRepository' for test cases
	 * @author Gustavo Figueiredo
	 *
	 */
	public static class InMemoryValidatedDataRepository implements ETLContext.ValidatedDataRepository {
		
		private final List<DocumentTemplate> templates;
		
		private final List<DocumentUploaded> uploads;
		
		private final Map<DocumentUploaded, List<Map<String,Object>>> data;
		
		public InMemoryValidatedDataRepository() {
			templates = new LinkedList<>();
			uploads = new LinkedList<>();
			data = new IdentityHashMap<>();
		}
		
		public void addTemplateFromArchetype(TemplateArchetype arch) {
			DocumentTemplate template = new DocumentTemplate();
			template.setFields(arch.getRequiredFields());
			template.setArchetype(arch.getName());
			template.setName(arch.getName());
			template.setVersion("1.0");
			templates.add(template);
		}
		
		public DocumentTemplate getTemplate(String archetype) {
			return templates.stream().filter(t->archetype.equalsIgnoreCase(t.getArchetype())).findAny().orElse(null);
		}

		@Override
		public Collection<DocumentTemplate> getTemplates(String archetype) throws Exception {
			return templates.stream().filter(t->archetype.equalsIgnoreCase(t.getArchetype())).collect(Collectors.toList());
		}
		
		public DocumentUploaded addUpload(String templateName, 
				String templateVersion, 
				String taxPayerId, 
				Integer taxPeriodNumber, 
				String fileId) {
			
			DocumentUploaded upload = new DocumentUploaded();
			upload.setTemplateName(templateName);
			upload.setTemplateVersion(templateVersion);
			upload.setTaxPayerId(taxPayerId);
			upload.setTaxPeriodNumber(taxPeriodNumber);
			upload.setFileId(fileId);
			upload.setSituation(DocumentSituation.VALID);
			uploads.add(upload);
			
			return upload;
		}
		
		public void addData(DocumentUploaded upload, Map<String,Object> record) {
			data.computeIfAbsent(upload, u->new LinkedList<>()).add(record);
		}

		public void addData(DocumentUploaded upload, Object... fieldsAndNamesPairs) {
			Map<String, Object> record = new HashMap<>();
			for (int i=0; i<fieldsAndNamesPairs.length-1; i+=2) {
				String fieldName = IndexNamesUtils.formatFieldName((String)fieldsAndNamesPairs[i]);
				Object fieldValue = fieldsAndNamesPairs[i+1];
				record.put(fieldName, fieldValue);
			}
			data.computeIfAbsent(upload, u->new LinkedList<>()).add(record);
		}

		@Override
		public Collection<DocumentUploaded> getUploads(String templateName, String templateVersion, String taxPayerId,
				Integer taxPeriodNumber) throws Exception {
			return uploads.stream().filter(u->templateName.equalsIgnoreCase(u.getTemplateName())
					&& templateVersion.equalsIgnoreCase(u.getTemplateVersion())
					&& taxPayerId.equalsIgnoreCase(u.getTaxPayerId())
					&& taxPeriodNumber.equals(u.getTaxPeriodNumber()))
					.collect(Collectors.toList());
		}

		@Override
		public boolean hasValidation(String templateName, String templateVersion, String fileId) throws Exception {
			return uploads.stream().anyMatch(u->templateName.equalsIgnoreCase(u.getTemplateName())
					&& templateVersion.equalsIgnoreCase(u.getTemplateVersion())
					&& fileId.equalsIgnoreCase(u.getFileId()));
		}

		@Override
		public Stream<Map<String, Object>> getValidatedData(String templateName, String templateVersion, String fileId,
				Optional<String[]> sortBy, Optional<SortOrder> sortOrder) throws Exception {
			DocumentUploaded upload = uploads.stream().filter(u->templateName.equalsIgnoreCase(u.getTemplateName())
					&& templateVersion.equalsIgnoreCase(u.getTemplateVersion())
					&& fileId.equalsIgnoreCase(u.getFileId()))
					.findAny().orElse(null);
			if (upload==null)
				return Collections.<Map<String, Object>>emptySet().stream();
			return data.getOrDefault(upload, Collections.emptyList()).stream();
		}

		@Override
		public Optional<Map<String, Object>> getValidatedData(String templateName, String templateVersion,
				String fileId, QueryBuilder query) throws Exception {
			DocumentUploaded upload = uploads.stream().filter(u->templateName.equalsIgnoreCase(u.getTemplateName())
					&& templateVersion.equalsIgnoreCase(u.getTemplateVersion())
					&& fileId.equalsIgnoreCase(u.getFileId()))
					.findAny().orElse(null);
			if (upload==null)
				return Optional.empty();
			if (query instanceof TermQueryBuilder) {
				final String fieldName = ((TermQueryBuilder)query).fieldName().replace(".keyword", "");
				final Object value = ((TermQueryBuilder)query).value();
				List<Map<String,Object>> contents = data.getOrDefault(upload, Collections.emptyList());
				return contents.stream().filter(record->{
					Object recordValue = record.get(fieldName);
					return (value==recordValue || (value!=null && value.equals(recordValue)));
				}).findFirst();
			}
			throw new UnsupportedOperationException("Does not support "+query.getClass().getName());
		}
		
	}
	
	/**
	 * Simplified in-memory implementation of 'ETLContext.DomainTableRepository' for test cases
	 * @author Gustavo Figueiredo
	 *
	 */
	public static class InMemoryDomainTableRepository implements ETLContext.DomainTableRepository {
		
		private final List<DomainTable> tables;
		
		public InMemoryDomainTableRepository() {
			tables = new LinkedList<>();
		}
		
		public void addBuiltIn() {
			tables.addAll(TemplateArchetypes.getBuiltInDomainTables());
		}

		@Override
		public Optional<DomainTable> findByNameAndVersion(String name, String version) {
			return tables.stream().filter(t->name.equalsIgnoreCase(t.getName()) && version.equalsIgnoreCase(t.getVersion())).findAny();
		}

		@Override
		public List<DomainTable> findByName(String name) {
			return tables.stream().filter(t->name.equalsIgnoreCase(t.getName())).collect(Collectors.toList());
		}
		
	}
	
	/**
	 * Simplified in-memory implementation of 'ETLContext.TaxpayerRepository' for test cases
	 * @author Gustavo Figueiredo
	 *
	 */
	public static class InMemoryTaxpayerRepository implements ETLContext.TaxpayerRepository {
		
		private final Map<String, Taxpayer> mapTaxpayers;
		
		public InMemoryTaxpayerRepository() {
			mapTaxpayers = new HashMap<>();
		}
		
		public void addTaxpayer(Taxpayer taxpayer) {
			mapTaxpayers.put(taxpayer.getTaxPayerId(), taxpayer);
		}

		@Override
		public Optional<Map<String, Object>> getTaxPayerData(String taxPayerId) {
			Taxpayer taxpayer = mapTaxpayers.get(taxPayerId);
			if (taxpayer==null)
				return Optional.empty();
			return Optional.of(ETLContext.getTaxpayerBasicInformation(taxpayer));
		}
	}
	
	/**
	 * Simplified in-memory implementation of 'ETLContext.LoadDataStrategy' for test cases
	 * @author Gustavo Figueiredo
	 *
	 */
	public static class InMemoryLoadDataStrategy implements ETLContext.LoadDataStrategy {
		
		private final Map<String,List<Map<String,Object>>> recordsPerIndex;
		
		public InMemoryLoadDataStrategy() {
			recordsPerIndex = new HashMap<>();
		}
		
		@Override
		public void add(IndexRequest request) {
			recordsPerIndex.computeIfAbsent(request.index(), i->new LinkedList<>())
				.add(request.sourceAsMap());
		}
		
		public List<Map<String,Object>> getRecords(String index) {
			return recordsPerIndex.get(index);
		}
	}
}
