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
package org.idb.cacao.account.etl;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

import org.elasticsearch.action.index.IndexRequest;
import org.idb.cacao.account.archetypes.AccountBuiltInDomainTables;
import org.idb.cacao.account.archetypes.ChartOfAccountsArchetype;
import org.idb.cacao.account.elements.AccountStandard;
import org.idb.cacao.account.elements.AccountSubcategory;
import org.idb.cacao.account.elements.DebitCredit;
import org.idb.cacao.account.elements.StatementComprehensiveIncome;
import org.idb.cacao.account.etl.AccountingLoader.AccountingFieldNames;
import org.idb.cacao.api.DocumentUploaded;
import org.idb.cacao.api.DomainLanguage;
import org.idb.cacao.api.ETLContext;
import org.idb.cacao.api.PublishedDataFieldNames;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.ETLContext.LoadDataStrategy;
import org.idb.cacao.api.templates.DomainEntry;
import org.idb.cacao.api.templates.DomainTable;
import org.idb.cacao.api.templates.DomainTable.MultiLingualMap;
import org.idb.cacao.api.utils.IndexNamesUtils;

import com.google.common.cache.LoadingCache;

/**
 * Computes the (yearly) statement of comprehensive income while iterating over the bookeeping entries from a
 * General Ledger.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class ComputedStatementIncomeProcessor implements Function<StatementComprehensiveIncome, Number> {

	/**
	 * The field name for date/time of published data
	 */
	private static final String publishedTimestamp = IndexNamesUtils.formatFieldName(PublishedDataFieldNames.TIMESTAMP.name());

	/**
	 * The field name for taxpayer ID in published data
	 */
	private static final String publishedTaxpayerId = IndexNamesUtils.formatFieldName(PublishedDataFieldNames.TAXPAYER_ID.name());

	/**
	 * The field name for period number in published data
	 */
	private static final String publishedtaxPeriodNumber = IndexNamesUtils.formatFieldName(PublishedDataFieldNames.TAXPERIOD_NUMBER.name());

	/**
	 * The field name for template name in published data
	 */
	private static final String publishedTemplateName = IndexNamesUtils.formatFieldName(PublishedDataFieldNames.TEMPLATE_NAME.name());

	/**
	 * The field name for template version in published data
	 */
	private static final String publishedTemplateVersion = IndexNamesUtils.formatFieldName(PublishedDataFieldNames.TEMPLATE_VERSION.name());

	/**
	 * The field name for indication of Year for each Monthly Balance Sheet
	 */
	private static final String publishedYear = IndexNamesUtils.formatFieldName(AccountingFieldNames.Year.name());

	/**
	 * The field name for published data regarding 'Statement Entry Code'
	 */
	private static final String entryCode = IndexNamesUtils.formatFieldName("StatementEntryCode");

	/**
	 * The field name for published data regarding 'Statement Entry'
	 */
	private static final String statementEntry = IndexNamesUtils.formatFieldName("Statement");

	/**
	 * The field name for published data regarding 'Statement Amount'
	 */
	private static final String amount = IndexNamesUtils.formatFieldName("Amount");

	/**
	 * The field name of the subcategory of an account according to the Taxpayer's Chart of Account
	 */
	private static final String accountSubCategory = IndexNamesUtils.formatFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountSubcategory.name());
	
	/**
	 * Maps accounts codes to entries of Statement of Comprehensive Income
	 */
	private final Map<String, Optional<StatementComprehensiveIncome>> mapAccounts;
	
	/**
	 * Maps entries of Statement of Comprehensive Income to multi-language descriptions
	 */
	private final Map<StatementComprehensiveIncome, Map<DomainLanguage, DomainEntry>> mapDomainTexts;
	
	/**
	 * Maps subcategory codes to the corresponding entry of Statement of Comprehensive Income
	 */
	private final Map<String, StatementComprehensiveIncome> mapSubcategories;

	/**
	 * Computes balance sheets while iterating over General Ledger entries
	 */
	private final Map<StatementComprehensiveIncome, ComputedStatementEntry> mapStatementEntries;
	
	private LoadingCache<String, Optional<Map<String, Object>>> lookupChartOfAccounts;
	
	private Optional<Map<String,Object>> declarantInformation;

	/**
	 * Year of the account (will read from the entries)
	 */
	private int year;

	private final String taxPayerId;
	
	private final Integer taxPeriodNumber;
	
	private final OffsetDateTime timestamp;

	private final AtomicLong countRecordsInStatement;

	private final LoadDataStrategy loader;
	
	private LongAdder countRecordsOverall;

	private DocumentUploaded gl;

	/**
	 * Ignores differences lesser than half of a cent	
	 */
	private static final double EPSILON = 0.005;		

	public ComputedStatementIncomeProcessor(final ETLContext context, 
			final AccountStandard accountStandard,
			final OffsetDateTime timestamp) {
		this.mapStatementEntries = new HashMap<>();
		this.mapAccounts = new HashMap<>();
		this.mapDomainTexts = new HashMap<>();
		this.mapSubcategories = new HashMap<>();
		this.countRecordsInStatement = new AtomicLong();
		this.loader = context.getLoadDataStrategy();
		this.taxPayerId = context.getDocumentUploaded().getTaxPayerId();
		this.taxPeriodNumber = context.getDocumentUploaded().getTaxPeriodNumber();
		this.timestamp = timestamp;
		this.declarantInformation = Optional.empty();

		// Use the domain table (either the built-in or the one configured at application)
		DomainTable sci_dt;
		List<DomainTable> custom_sci_dt = context.getDomainTableRepository().findByName(AccountBuiltInDomainTables.ACCOUNT_SCI.getName());
		if (custom_sci_dt.size()==1)
			sci_dt = custom_sci_dt.get(0);
		else if (custom_sci_dt.size()>1)
			sci_dt = custom_sci_dt.stream().sorted(Comparator.comparing(DomainTable::getDomainTableCreateTime).reversed()).findFirst().get();
		else
			sci_dt = AccountBuiltInDomainTables.ACCOUNT_SCI;
		
		// Extracts the relationships between the sub category names and the corresponding domain table entry
		MultiLingualMap mlmap = sci_dt.toMultiLingualMap();
		
		// Prepare the map of descriptions to be used while performing ETL
		for (StatementComprehensiveIncome entry: StatementComprehensiveIncome.values()) {
			Map<DomainLanguage, DomainEntry> mapped_entry = mlmap.get(entry.name());
			if (mapped_entry==null) 				
				continue; // This domain table lacks information about this entry
			mapDomainTexts.put(entry, mapped_entry);
			
			AccountSubcategory subcategory = entry.getSubcategory();
			if (subcategory!=null) {
				String subcategory_code = subcategory.getNumber(accountStandard);
				if (subcategory_code!=null) {
					mapSubcategories.put(subcategory_code, entry);
				}
			}
		}
		
	}
	
	public void setDocumentUploadedForGeneralLedger(DocumentUploaded gl) {
		this.gl = gl;
	}

	public void setLookupChartOfAccounts(LoadingCache<String, Optional<Map<String, Object>>> lookupChartOfAccounts) {
		this.lookupChartOfAccounts = lookupChartOfAccounts;
	}

	public void setCountRecordsOverall(LongAdder countRecordsOverall) {
		this.countRecordsOverall = countRecordsOverall;
	}

	public void setDeclarantInformation(Optional<Map<String, Object>> declarantInformation) {
		this.declarantInformation = declarantInformation;
	}

	/**
	 * Computes a bookeeping entry.
	 * @param accountCode Account code
	 * @param amount Amount
	 * @param isDebit Indication whether the account was debited (otherwise it was credited)
	 */
	public void computeEntry(OffsetDateTime date, String accountCode, Number amount, boolean isDebit) {
		
		final int year = (date==null) ? 0 : ( date.getYear() );
		if (year!=0) {
			if (this.year==0 || this.year<year)
				this.year = year;
		}

		if (amount==null || Math.abs(amount.doubleValue())<EPSILON)
			return;
		
		// Check which entry of StatementComprehensiveIncome corresponds to this accountCode
		if (accountCode==null || accountCode.length()==0)
			return;
		Optional<StatementComprehensiveIncome> entry = mapAccounts.computeIfAbsent(accountCode, this::getStatementComprehensiveIncome);
		if (!entry.isPresent())
			return; // account is not related to Statement of Income
		
		// Compute the value
		mapStatementEntries.computeIfAbsent(entry.get(), k->new ComputedStatementEntry(k.getNature()))
		.compute(amount, isDebit);
		
	}
	
	/**
	 * Given an account code according to the taxpayer's Chart of Accounts, returns the corresponding entry
	 * to StatementComprehensiveIncome. Returns empty otherwise.
	 */
	public Optional<StatementComprehensiveIncome> getStatementComprehensiveIncome(String accountCode) {
		if (lookupChartOfAccounts==null)
			return Optional.empty();
		
		Optional<Map<String, Object>> accountMapping = lookupChartOfAccounts.getUnchecked(accountCode);
		if (!accountMapping.isPresent())
			return Optional.empty();
		
		String subcategoryCode = ValidationContext.toString(accountMapping.get().get(accountSubCategory));
		if (subcategoryCode==null || subcategoryCode.length()==0)
			return Optional.empty();
		
		StatementComprehensiveIncome entry = mapSubcategories.get(subcategoryCode);
		return Optional.ofNullable(entry);
	}
	
	/**
	 * Should be called after iterating all of the book entries
	 */
	public void finish() {
		
		// Do all the computations according to the formulas
		
		for (StatementComprehensiveIncome entry: StatementComprehensiveIncome.values()) {
			if (entry.getFormula()==null)
				continue;
			Number computed = entry.computeFormula(this);
			mapStatementEntries.computeIfAbsent(entry, k->new ComputedStatementEntry(k.getNature()))
			.set(computed);
		}
		
		// Store information in denormalized view
		
		for (StatementComprehensiveIncome t: StatementComprehensiveIncome.values()) {
			ComputedStatementEntry computed = mapStatementEntries.computeIfAbsent(t, k->new ComputedStatementEntry(k.getNature()));
			Map<DomainLanguage, DomainEntry> multiLanguageDomainEntry = mapDomainTexts.get(t);
			
			String rowId_SCI = String.format("%s.%d.%014d", taxPayerId, taxPeriodNumber, countRecordsInStatement.incrementAndGet());
			Map<String,Object> normalizedRecord_SCI = new HashMap<>();
			normalizedRecord_SCI.put("doc_"+publishedTimestamp, timestamp);
			normalizedRecord_SCI.put(publishedTimestamp, LocalDate.of(year, 12, 1));
			normalizedRecord_SCI.put(publishedTaxpayerId, taxPayerId);
			normalizedRecord_SCI.put(publishedtaxPeriodNumber, taxPeriodNumber);
			if (gl!=null) {
				normalizedRecord_SCI.put(publishedTemplateName, gl.getTemplateName());
				normalizedRecord_SCI.put(publishedTemplateVersion, gl.getTemplateVersion());
			}
			normalizedRecord_SCI.put(publishedYear, year);
			normalizedRecord_SCI.put(entryCode, t.name());
			normalizedRecord_SCI.put(amount, computed.getValue());
			if (declarantInformation.isPresent())
				normalizedRecord_SCI.putAll(declarantInformation.get());
			if (multiLanguageDomainEntry!=null && !multiLanguageDomainEntry.isEmpty())
				ETLContext.denormalizeDomainEntryNames(multiLanguageDomainEntry, statementEntry, normalizedRecord_SCI);
			loader.add(new IndexRequest(AccountingLoader.INDEX_PUBLISHED_COMPUTED_STATEMENT_INCOME)
				.id(rowId_SCI)
				.source(normalizedRecord_SCI));
			if (countRecordsOverall!=null)
				countRecordsOverall.increment();	
		}

	}

	@Override
	public Number apply(StatementComprehensiveIncome t) {
		ComputedStatementEntry e = mapStatementEntries.get(t);
		return (e==null) ? null : e.getValue();
	}

	/**
	 * Wraps the value for use in Statement of Comprehensive Income
	 * @author Gustavo Figueiredo
	 *
	 */
	private static class ComputedStatementEntry {
		
		private final DebitCredit nature;
		
		/**
		 * Positive is debit, negative is credit
		 */
		private double value;
		
		ComputedStatementEntry(DebitCredit nature) {
			this.nature = nature;
		}

		public double getValue() {
			return value;
		}

		public void set(Number amount) {
			value = (amount==null) ? 0.0 : amount.doubleValue();
		}
		
		public void compute(Number amount, boolean isDebit) {
			switch (nature) {
			case D:
				value += (isDebit) ? amount.doubleValue() : -amount.doubleValue();
				break;
			case C:
				value += (isDebit) ? -amount.doubleValue() : amount.doubleValue();
				break;
			default:
				// never go here
			}
		}
	}
}
