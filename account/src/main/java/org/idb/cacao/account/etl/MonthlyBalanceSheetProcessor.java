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
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.elasticsearch.action.index.IndexRequest;
import org.idb.cacao.account.archetypes.ChartOfAccountsArchetype;
import org.idb.cacao.account.archetypes.GeneralLedgerArchetype;
import org.idb.cacao.account.archetypes.OpeningBalanceArchetype;
import org.idb.cacao.account.elements.AccountCategory;
import org.idb.cacao.account.elements.AccountStandard;
import org.idb.cacao.account.elements.BalanceSheet;
import org.idb.cacao.account.etl.AccountingLoader.AccountingFieldNames;
import org.idb.cacao.api.DocumentSituation;
import org.idb.cacao.api.DocumentUploaded;
import org.idb.cacao.api.ETLContext;
import org.idb.cacao.api.Periodicity;
import org.idb.cacao.api.PublishedDataFieldNames;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.ETLContext.LoadDataStrategy;
import org.idb.cacao.api.utils.IndexNamesUtils;
import org.springframework.data.util.Pair;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Computes the monthly balance sheets while iterating over the bookeeping entries from a
 * General Ledger.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class MonthlyBalanceSheetProcessor {

	private static final Logger log = Logger.getLogger(MonthlyBalanceSheetProcessor.class.getName());

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
	 * The field name for indication of Month for each Monthly Balance Sheet
	 */
	private static final String publishedMonth = IndexNamesUtils.formatFieldName(AccountingFieldNames.Month.name());

	/**
	 * The field name for published data regarding the initial balance amount for each Monthly Balance Sheet
	 */
	private static final String openingBalanceInitial = IndexNamesUtils.formatFieldName(OpeningBalanceArchetype.FIELDS_NAMES.InitialBalance.name());

	/**
	 * The field name for published data regarding the initial balance amount for each Monthly Balance Sheet, with sign corresponding to the debit/credit nature of the account
	 */
	private static final String openingBalanceWithSign = IndexNamesUtils.formatFieldName("InitialBalanceWithSign");

	/**
	 * The field name for validated data regarding the 'D/C' indication of initial balance amount
	 */
	private static final String openingBalanceDC = IndexNamesUtils.formatFieldName(OpeningBalanceArchetype.FIELDS_NAMES.DebitCredit.name());

	/**
	 * The field name for published data regarding 'Account Code'
	 */
	private static final String balanceAccountCode = IndexNamesUtils.formatFieldName(GeneralLedgerArchetype.FIELDS_NAMES.AccountCode.name());

	/**
	 * The field name for published data regarding the 'D/C' indication of initial balance amount for each Monthly Balance Sheet
	 */
	private static final String openingBalanceMonthlyDC = IndexNamesUtils.formatFieldName(AccountingFieldNames.InitialBalanceDebitCredit.name());

	/**
	 * The field name for published data regarding amount of debit values
	 */
	private static final String balanceAmountDebits = IndexNamesUtils.formatFieldName(AccountingFieldNames.AmountDebits.name());

	/**
	 * The field name for published data regarding amount of credit values
	 */
	private static final String balanceAmountCredits = IndexNamesUtils.formatFieldName(AccountingFieldNames.AmountCredits.name());

	/**
	 * The field name for published data regarding the closing balance amount for each Monthly Balance Sheet
	 */
	private static final String closingBalanceMonthly = IndexNamesUtils.formatFieldName(AccountingFieldNames.FinalBalance.name());

	/**
	 * The field name for published data regarding the 'D/C' indication of closing balance amount for each Monthly Balance Sheet
	 */
	private static final String closingBalanceMonthlyDC = IndexNamesUtils.formatFieldName(AccountingFieldNames.FinalBalanceDebitCredit.name());

	/**
	 * The field name for published data regarding the closing balance amount for each Monthly Balance Sheet, with sign corresponding to the debit/credit nature of the account
	 */
	private static final String closingBalanceWithSign = IndexNamesUtils.formatFieldName("FinalBalanceWithSign");

	/**
	 * The number of book entries for each Monthly Balance Sheet
	 */
	private static final String bookEntries = IndexNamesUtils.formatFieldName(AccountingFieldNames.BookEntries.name());
	
	/**
	 * The category code related to the account informed in each Monthly Balance Sheet
	 */
	private static final String accountCategory = IndexNamesUtils.formatFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountCategory.name());

	/**
	 * Ignores differences lesser than half of a cent	
	 */
	private static final double EPSILON = 0.005;		

	/**
	 * Stores here the last month (a number in the format yyyy-mm) of the last parsed entry from the Journal
	 */
	private final AtomicInteger previousMonth;

	/**
	 * Keep track of all known account codes for reporting balance sheets. This list may grow as we proceed.
	 */
	private final Set<String> accountsForBalanceSheets;

	/**
	 * Computes balance sheets while iterating over General Ledger entries
	 */
	private final Map<String, BalanceSheet> mapBalanceSheets;

	/**
	 * Keep track of informed balance sheets for accounts (first in key pair) and months (second in key pair) and their corresponding
	 * final balance. This will be used for completing balance sheets for absent months after we have processed all the data.
	 */
	private final Map<Pair<String, Integer>, Double> informedBalancedSheetsAccountsAndMonths;

	/**
	 * Structure for loading and caching information from the provided Opening Balance
	 */
	private final LoadingCache<String, Optional<Map<String, Object>>> lookupOpeningBalance;
	
	private final String taxPayerId;
	
	private final Integer taxPeriodNumber;
	
	private final OffsetDateTime timestamp;
	
	private final AtomicLong countRecordsInBalanceSheet;
	
	private final LoadDataStrategy loader;
	
	/**
	 * Codes at domain table of categories of accounts that has debit nature (i.e.: positive means debit)
	 */
	private final Set<String> categoriesWithDebitNature;
	
	private LongAdder countRecordsOverall;

	private DocumentUploaded gl;
	
	private Periodicity periodicity;
	
	private Optional<Map<String,Object>> declarantInformation;
	
	private LoadingCache<String, Optional<Map<String, Object>>> lookupChartOfAccounts;

	/**
	 * Collects warnings generated during this process
	 */
	private Consumer<String> collectWarnings;

	public MonthlyBalanceSheetProcessor(final ETLContext context,
			final AccountStandard accountStandard,
			final DocumentUploaded ob,
			final OffsetDateTime timestamp) {
		this.previousMonth = new AtomicInteger(0);
		this.accountsForBalanceSheets = getAccountsForOpeningBalances(context.getValidatedDataRepository(), ob);
		this.mapBalanceSheets = new HashMap<>();
		this.informedBalancedSheetsAccountsAndMonths = new HashMap<>();
		this.lookupOpeningBalance = getLookupOpeningBalance(context.getValidatedDataRepository(), ob);
		this.taxPayerId = context.getDocumentUploaded().getTaxPayerId();
		this.taxPeriodNumber = context.getDocumentUploaded().getTaxPeriodNumber();
		this.timestamp = timestamp;
		this.countRecordsInBalanceSheet = new AtomicLong();
		this.declarantInformation = Optional.empty();
		this.loader = context.getLoadDataStrategy();
		this.collectWarnings = alert->{
			context.addAlert(ob, alert);
			context.setOutcomeSituation(ob, DocumentSituation.PENDING);
		};
		this.categoriesWithDebitNature = 
		Arrays.stream(AccountCategory.values())
			.filter(AccountCategory::isDebitNature)
			.map(c->c.getNumber(accountStandard))
			.collect(Collectors.toCollection(()->new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
	}
	
	public void setDocumentUploadedForGeneralLedger(DocumentUploaded gl) {
		this.gl = gl;
	}
	
	public void setCountRecordsOverall(LongAdder countRecordsOverall) {
		this.countRecordsOverall = countRecordsOverall;
	}

	public void setDeclarantInformation(Optional<Map<String, Object>> declarantInformation) {
		this.declarantInformation = declarantInformation;
	}

	public void setLookupChartOfAccounts(LoadingCache<String, Optional<Map<String, Object>>> lookupChartOfAccounts) {
		this.lookupChartOfAccounts = lookupChartOfAccounts;
	}

	public void setPeriodicity(Periodicity periodicity) {
		this.periodicity = periodicity;
	}

	/**
	 * Computes a bookeeping entry. It's expected to receive all the entries in the following order: the Date (1st criteria) and the Entry Id (2nd criteria)
	 * @param date Date of this record
	 * @param accountCode Account code
	 * @param amount Amount
	 * @param isDebit Indication whether the account was debited (otherwise it was credited)
	 * @return Returns the resulting BalanceSheet (still open for further inputs)
	 */
	public BalanceSheet computeEntry(OffsetDateTime date, String accountCode, Number amount, boolean isDebit) {
		
		final int year_month = (date==null) ? 0 : ( date.getYear() * 100 + date.getMonthValue() );
		final boolean changed_month = year_month!=0 && previousMonth.get()!=0 && previousMonth.get()!=year_month;
		final int previous_year_month = previousMonth.get();
		if (date!=null && (changed_month || previousMonth.get()==0)) {
			previousMonth.set(year_month);
		}

		if (changed_month) {
			// If the month has changed, let's fill the Monthly Balance Sheet for all accounts
			addRecordToMonthlyBalanceSheet(gl, accountsForBalanceSheets, previous_year_month,
					mapBalanceSheets, taxPayerId, taxPeriodNumber, timestamp,
					countRecordsInBalanceSheet, countRecordsOverall, declarantInformation,
					lookupChartOfAccounts,
					informedBalancedSheetsAccountsAndMonths,
					loader);
		}

		final Optional<Map<String,Object>> accountInformation = (lookupChartOfAccounts==null) ? Optional.empty() : lookupChartOfAccounts.getUnchecked(accountCode);

		BalanceSheet balanceSheet =
		mapBalanceSheets.computeIfAbsent(accountCode, acc->{
			BalanceSheet balance = new BalanceSheet();
			Optional<Map<String, Object>> opening = lookupOpeningBalance.getUnchecked(acc);
			if (opening.isPresent()) {
				Number initialBalance = ValidationContext.toNumber(opening.get().get(openingBalanceInitial));
				String balanceDebitCredit = ValidationContext.toString(opening.get().get(openingBalanceDC));
				boolean initialIsDebit = balanceDebitCredit.equalsIgnoreCase("D");
				if (initialBalance!=null && Math.abs(initialBalance.doubleValue())>EPSILON) {
					if (initialIsDebit)
						balance.setInitialValue(Math.abs(initialBalance.doubleValue()));
					else
						balance.setInitialValue(-Math.abs(initialBalance.doubleValue()));
				}
			}
			else if (accountInformation.isPresent()) {
				collectWarnings.accept("{account.error.missing.opening.balance("
						+acc.replaceAll("[\\{\\}\\,\\(\\)\r\n\t]","")
						+")}");
				accountsForBalanceSheets.add(acc);
			}
			return balance;
		});
		
		balanceSheet.computeEntry(amount, isDebit);

		return balanceSheet;
	}
	
	/**
	 * Should be called after iterating all of the book entries
	 */
	public void finish() {
		
		final int previous_year_month = previousMonth.get();
		if (previous_year_month!=0) {
			addRecordToMonthlyBalanceSheet(gl, accountsForBalanceSheets, previous_year_month,
				mapBalanceSheets, taxPayerId, taxPeriodNumber, timestamp,
				countRecordsInBalanceSheet, countRecordsOverall, declarantInformation,
				lookupChartOfAccounts,
				informedBalancedSheetsAccountsAndMonths,
				loader);
			fillMissingMonthlyBalanceSheet(gl, accountsForBalanceSheets,
				taxPayerId, taxPeriodNumber, timestamp,
				countRecordsInBalanceSheet, countRecordsOverall, declarantInformation,
				lookupChartOfAccounts,
				lookupOpeningBalance,
				informedBalancedSheetsAccountsAndMonths, periodicity,
				loader);
		}

	}
	
	/**
	 * Returns the accounts codes that are referenced in Opening Balances
	 */
	public static Set<String> getAccountsForOpeningBalances(final ETLContext.ValidatedDataRepository repository, final DocumentUploaded ob) {
		final Set<String> accounts = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		final String obAccountCode = IndexNamesUtils.formatFieldName(OpeningBalanceArchetype.FIELDS_NAMES.AccountCode.name());
		try (Stream<Map<String,Object>> stream = repository.getValidatedData(ob.getTemplateName(), ob.getTemplateVersion(), ob.getFileId(), /*sortBy*/Optional.empty(), /*sortOrder*/Optional.empty());) {
			stream.forEach(entry->{
				String accountCode = ValidationContext.toString(entry.get(obAccountCode));
				if (accountCode!=null && accountCode.trim().length()>0) {
					accounts.add(accountCode);
				}
			});
		}			
		catch (Throwable ex) {
			log.log(Level.SEVERE, "Error reading Opening Balances for template "+ob.getTemplateName()+" "+ob.getTemplateVersion()+" taxpayer "+ob.getTaxPayerId()+" period "+ob.getTaxPeriodNumber(), ex);
		}
		return accounts;
	}

	/**
	 * Feeds information about Monthly Balance Sheets
	 * @param gl The Upload record regarding the General Ledger
	 * @param accountsForBalanceSheets Collection of all account codes (according to the taxpayer's Chart of Accounts)
	 * @param yearMonth Year+Month of balance sheet
	 * @param mapBalanceSheets For each account code keeps track of the computed balance sheet so far (presumably of the same month)
	 * @param taxPayerId The Taxpayer ID
	 * @param taxPeriodNumber The number of the period
	 * @param timestamp Time date/time of ETL procedure
	 * @param countRecordsInBalanceSheet The total number of 'Monthly Balance Sheets' records (incremented here)
	 * @param countRecordsOverall The total number of records (incremented here)
	 * @param declarantInformation Additional information about the declarant
	 * @param lookupChartOfAccounts Object used for searching additional information about accounts
	 * @param informedBalancedSheetsAccountsAndMonths Keeps track of all informed balance sheets
	 * @param loader Object used for writing the results
	 */
	private void addRecordToMonthlyBalanceSheet(
			final DocumentUploaded gl,
			final Set<String> accountsForBalanceSheets,
			final int yearMonth,
			final Map<String, BalanceSheet> mapBalanceSheets,
			final String taxPayerId,
			final Integer taxPeriodNumber,
			final OffsetDateTime timestamp,
			final AtomicLong countRecordsInBalanceSheet,
			final LongAdder countRecordsOverall,
			final Optional<Map<String,Object>> declarantInformation,
			final LoadingCache<String, Optional<Map<String, Object>>> lookupChartOfAccounts,
			final Map<Pair<String, Integer>, Double> informedBalancedSheetsAccountsAndMonths,
			final LoadDataStrategy loader) {
		
		final int year = yearMonth/100;
		final int monthNumber = yearMonth%100;
		final String month = Month.of(monthNumber).getDisplayName(
	        TextStyle.SHORT, 
	        Locale.getDefault()
	    );
		
		for (String account: accountsForBalanceSheets) {
			BalanceSheet computedBalanceSheet = mapBalanceSheets.get(account);
			if (computedBalanceSheet!=null) {
				final Optional<Map<String,Object>> accountInformation = (lookupChartOfAccounts==null) ? Optional.empty() : lookupChartOfAccounts.getUnchecked(account);
				String rowId_BS = String.format("%s.%d.%014d", taxPayerId, taxPeriodNumber, countRecordsInBalanceSheet.incrementAndGet());
				Map<String,Object> normalizedRecord_BS = new HashMap<>();
				normalizedRecord_BS.put(PublishedDataFieldNames.ETL_TIMESTAMP.getFieldName(), timestamp);
				normalizedRecord_BS.put(publishedTimestamp, LocalDate.of(year, monthNumber, 1));
				normalizedRecord_BS.put(publishedTaxpayerId, taxPayerId);
				normalizedRecord_BS.put(publishedtaxPeriodNumber, taxPeriodNumber);
				if (gl!=null) {
					normalizedRecord_BS.put(publishedTemplateName, gl.getTemplateName());
					normalizedRecord_BS.put(publishedTemplateVersion, gl.getTemplateVersion());
				}
				normalizedRecord_BS.put(publishedYear, year);
				normalizedRecord_BS.put(publishedMonth, month);
				normalizedRecord_BS.put(balanceAccountCode, account);
				normalizedRecord_BS.put(openingBalanceInitial, Math.abs(computedBalanceSheet.getInitialValue()));
				normalizedRecord_BS.put(openingBalanceMonthlyDC, (computedBalanceSheet.isInitialValueDebit()) ? "D" : "C");
				normalizedRecord_BS.put(balanceAmountDebits, computedBalanceSheet.getDebits());
				normalizedRecord_BS.put(balanceAmountCredits, computedBalanceSheet.getCredits());
				normalizedRecord_BS.put(closingBalanceMonthly, Math.abs(computedBalanceSheet.getFinalValue()));
				normalizedRecord_BS.put(closingBalanceMonthlyDC, (computedBalanceSheet.isFinalValueDebit()) ? "D" : "C");
				normalizedRecord_BS.put(bookEntries, computedBalanceSheet.getCountEntries());
				if (declarantInformation.isPresent())
					normalizedRecord_BS.putAll(declarantInformation.get());
				if (accountInformation.isPresent()) {
					normalizedRecord_BS.putAll(accountInformation.get());
					addBalanceWithSign(openingBalanceWithSign, accountInformation.get(), Math.abs(computedBalanceSheet.getInitialValue()), computedBalanceSheet.isInitialValueDebit(),
							categoriesWithDebitNature,
							normalizedRecord_BS);
					addBalanceWithSign(closingBalanceWithSign, accountInformation.get(), Math.abs(computedBalanceSheet.getFinalValue()), computedBalanceSheet.isFinalValueDebit(),
							categoriesWithDebitNature,
							normalizedRecord_BS);
				}
				loader.add(new IndexRequest(AccountingLoader.INDEX_PUBLISHED_BALANCE_SHEET)
					.id(rowId_BS)
					.source(normalizedRecord_BS));
				informedBalancedSheetsAccountsAndMonths.put(Pair.of(account, yearMonth), computedBalanceSheet.getFinalValue());
				if (countRecordsOverall!=null)
					countRecordsOverall.increment();	
				computedBalanceSheet.flipBalance();
			}
		}
	}

	/**
	 * Feed information about missing periods or missing accounts in Monthly Balance Sheets.
	 */
	private void fillMissingMonthlyBalanceSheet(
			final DocumentUploaded gl,
			final Set<String> accountsForBalanceSheets,
			final String taxPayerId,
			final Integer taxPeriodNumber,
			final OffsetDateTime timestamp,
			final AtomicLong countRecordsInBalanceSheet,
			final LongAdder countRecordsOverall,
			final Optional<Map<String,Object>> declarantInformation,
			final LoadingCache<String, Optional<Map<String, Object>>> lookupChartOfAccounts,
			final LoadingCache<String, Optional<Map<String, Object>>> lookupOpeningBalance,
			final Map<Pair<String, Integer>, Double> informedBalancedSheetsAccountsAndMonths,
			final Periodicity periodicity,
			final LoadDataStrategy loader) {
		
		if (accountsForBalanceSheets==null || accountsForBalanceSheets.isEmpty())
			return;
		
		// Minimum year+month according to published Monthly Balance Sheets, or 0 if none
		int min_year_month_filled = informedBalancedSheetsAccountsAndMonths.isEmpty() ? 0
				: informedBalancedSheetsAccountsAndMonths.keySet().stream().mapToInt(Pair::getSecond).min().orElse(0);

		// Maximum year+month according to published Monthly Balance Sheets, or 0 if none
		int max_year_month_filled = informedBalancedSheetsAccountsAndMonths.isEmpty() ? 0
				: informedBalancedSheetsAccountsAndMonths.keySet().stream().mapToInt(Pair::getSecond).max().orElse(0);
		
		// The informed year, minimum month and maximum month according to published Monthly Balance Sheet, or 0 if none
		int actual_year = min_year_month_filled/100;
		int actual_min_month = min_year_month_filled%100;
		int actual_max_month = max_year_month_filled%100;
		
		// Get the expected minimum month+year and expected maximum month+year according to General Ledger and the template periodicity
		int year = Periodicity.getYear(taxPeriodNumber);
		int first_month = Periodicity.getMinMonthNumber(taxPeriodNumber, periodicity);
		int last_month = Periodicity.getMaxMonthNumber(taxPeriodNumber, periodicity);
		
		// If the expected year does not correspond to the information that was read from General Ledger, consider the information we got from General Ledger 
		if (actual_year!=0 && year!=actual_year)
			year = actual_year;
		
		// If we don't have a clue of what year it is, let's abort
		if (year==0)
			return;
		
		// If the data started some months later than expected, let's consider this
		if (actual_min_month!=0 && first_month<actual_min_month)
			first_month = actual_min_month;
		
		// If we don't have a clue of what month to start with, let's abort
		if (first_month==0)
			return;
		
		if (last_month==0)
			last_month = actual_max_month;
		
		if (last_month==0)
			last_month = first_month;
		
		// Now that we know what is the 'full period', let's start looking for missing data
		
		Map<String, Double> previousBalanceSheets = new HashMap<>();
		
		for (int month=first_month; month<=last_month; month++) {
			
			Integer expected_year_month = year * 100 + month;
			for (String account: accountsForBalanceSheets) {
				
				Pair<String,Integer> expected_account_and_period = Pair.of(account, expected_year_month);
				Double reported_balance = informedBalancedSheetsAccountsAndMonths.get(expected_account_and_period);
				if (reported_balance!=null) {
					// if we have reported a Balance Sheet for this account and period, we are done with it
					previousBalanceSheets.put(account, reported_balance);
					continue;
				}
				
				// we need to report a missing BalanceSheet
				
				// look for the previous BalanceSheet we have for this account
				Double previous_final_balance = previousBalanceSheets.get(account);
				if (previous_final_balance==null) {
					if (month==first_month) {
						Optional<Map<String, Object>> opening = lookupOpeningBalance.getUnchecked(account);
						if (opening.isPresent()) {
							Number initialBalance = ValidationContext.toNumber(opening.get().get(openingBalanceInitial));
							String balanceDebitCredit = ValidationContext.toString(opening.get().get(openingBalanceDC));
							boolean initialIsDebit = balanceDebitCredit.equalsIgnoreCase("D");
							if (initialBalance!=null && Math.abs(initialBalance.doubleValue())>EPSILON) {
								if (initialIsDebit)
									previous_final_balance = Math.abs(initialBalance.doubleValue());
								else
									previous_final_balance = -Math.abs(initialBalance.doubleValue());
								previousBalanceSheets.put(account, previous_final_balance);
							}
						}
					}
					if (previous_final_balance==null) {
						previous_final_balance = 0.0;
						previousBalanceSheets.put(account, previous_final_balance);
					}
				}
				
				final String monthName = Month.of(month).getDisplayName(
			        TextStyle.SHORT, 
			        Locale.getDefault()
			    );

				final Optional<Map<String,Object>> accountInformation = (lookupChartOfAccounts==null) ? Optional.empty() :lookupChartOfAccounts.getUnchecked(account);
				String rowId_BS = String.format("%s.%d.%014d", taxPayerId, taxPeriodNumber, countRecordsInBalanceSheet.incrementAndGet());
				Map<String,Object> normalizedRecord_BS = new HashMap<>();
				normalizedRecord_BS.put(PublishedDataFieldNames.ETL_TIMESTAMP.getFieldName(), timestamp);
				normalizedRecord_BS.put(publishedTimestamp, LocalDate.of(year, month, 1));
				normalizedRecord_BS.put(publishedTaxpayerId, taxPayerId);
				normalizedRecord_BS.put(publishedtaxPeriodNumber, taxPeriodNumber);
				if (gl!=null) {
					normalizedRecord_BS.put(publishedTemplateName, gl.getTemplateName());
					normalizedRecord_BS.put(publishedTemplateVersion, gl.getTemplateVersion());
				}
				normalizedRecord_BS.put(publishedYear, year);
				normalizedRecord_BS.put(publishedMonth, monthName);
				normalizedRecord_BS.put(balanceAccountCode, account);
				normalizedRecord_BS.put(openingBalanceInitial, Math.abs(previous_final_balance.doubleValue()));
				normalizedRecord_BS.put(openingBalanceMonthlyDC, (previous_final_balance.doubleValue()>=0) ? "D" : "C");
				normalizedRecord_BS.put(balanceAmountDebits, 0.0);
				normalizedRecord_BS.put(balanceAmountCredits, 0.0);
				normalizedRecord_BS.put(closingBalanceMonthly, Math.abs(previous_final_balance.doubleValue()));
				normalizedRecord_BS.put(closingBalanceMonthlyDC, (previous_final_balance.doubleValue()>=0) ? "D" : "C");
				normalizedRecord_BS.put(bookEntries, 0);
				if (declarantInformation.isPresent())
					normalizedRecord_BS.putAll(declarantInformation.get());
				if (accountInformation.isPresent()) {
					normalizedRecord_BS.putAll(accountInformation.get());
					addBalanceWithSign(openingBalanceWithSign, accountInformation.get(), Math.abs(previous_final_balance.doubleValue()), (previous_final_balance.doubleValue()>=0),
							categoriesWithDebitNature,
							normalizedRecord_BS);
					addBalanceWithSign(closingBalanceWithSign, accountInformation.get(), Math.abs(previous_final_balance.doubleValue()), (previous_final_balance.doubleValue()>=0),
							categoriesWithDebitNature,
							normalizedRecord_BS);
				}
				loader.add(new IndexRequest(AccountingLoader.INDEX_PUBLISHED_BALANCE_SHEET)
					.id(rowId_BS)
					.source(normalizedRecord_BS));
				if (countRecordsOverall!=null)
					countRecordsOverall.increment();	
				
			} // LOOP over account codes
			
		} // LOOP over expected months
	}

	/**
	 * Returns object used for retrieving information from Opening Balance, keeping a temporary cache in memory.
	 */
	public static LoadingCache<String, Optional<Map<String, Object>>> getLookupOpeningBalance(final ETLContext.ValidatedDataRepository repository, final DocumentUploaded ob) {
		return CacheBuilder.newBuilder()
			.maximumSize(1000)
			.expireAfterWrite(10, TimeUnit.MINUTES)
			.build(new CacheLoader<String, Optional<Map<String,Object>>>(){
				final String fieldName = IndexNamesUtils.formatFieldName(OpeningBalanceArchetype.FIELDS_NAMES.AccountCode.name())+".keyword";
				@Override
				public Optional<Map<String,Object>> load(String accountCode) throws Exception {
					if (accountCode==null)
						return Optional.empty();
					else {
						try {
							return repository.getValidatedData(ob.getTemplateName(), ob.getTemplateVersion(), ob.getFileId(), fieldName, accountCode)
									.map(IndexNamesUtils::normalizeAllKeysForES)
									.map(AccountingLoader::removeControlFields);
						}
						catch (Throwable ex) {
							return Optional.empty();
						}
					}
				}
			});
	}
	
	/**
	 * Write to generic record 'normalizedRecord' a field with the name provided by 'fieldName' with
	 * the value provided by 'value' and with the signal provided by 'isDebit' and the category informed
	 * in 'accountInformation'
	 */
	private static void addBalanceWithSign(
			final String fieldName,
			final Map<String,Object> accountInformation,
			final double value,
			final boolean isDebit,
			final Set<String> categoriesWithDebitNature,
			final Map<String, Object> normalizedRecord
			) {		
		if (accountInformation==null || accountInformation.isEmpty())
			return;
		String category = ValidationContext.toString(accountInformation.get(accountCategory));
		if (category==null)
			return;
		boolean isCategoryDebitNature = categoriesWithDebitNature.contains(category);
		double valueWithSign = (isCategoryDebitNature==isDebit) ? value : -value;
		normalizedRecord.put(fieldName, valueWithSign);
	}
}
