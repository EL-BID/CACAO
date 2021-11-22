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

import java.time.Month;
import java.time.OffsetDateTime;
import java.time.format.TextStyle;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.search.sort.SortOrder;
import org.idb.cacao.account.archetypes.ChartOfAccountsArchetype;
import org.idb.cacao.account.archetypes.GeneralLedgerArchetype;
import org.idb.cacao.account.archetypes.OpeningBalanceArchetype;
import org.idb.cacao.account.elements.DailyAccountingFlow;
import org.idb.cacao.api.DocumentSituation;
import org.idb.cacao.api.DocumentUploaded;
import org.idb.cacao.api.DomainLanguage;
import org.idb.cacao.api.ETLContext;
import org.idb.cacao.api.ETLContext.LoadDataStrategy;
import org.idb.cacao.api.ETLContext.TaxpayerRepository;
import org.idb.cacao.api.Periodicity;
import org.idb.cacao.api.PublishedDataFieldNames;
import org.idb.cacao.api.ValidatedDataFieldNames;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.DomainEntry;
import org.idb.cacao.api.templates.DomainTable;
import org.idb.cacao.api.utils.IndexNamesUtils;
import org.springframework.data.util.Pair;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Projects accounting data into Database after validation phases. Performs denormalization
 * of data.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class AccountingLoader {
	
	private static final Logger log = Logger.getLogger(AccountingLoader.class.getName());

	/**
	 * Index name for published (denormalized) data regarding General Ledger.<BR>
	 * There is one record for each record in General Ledger, including more fields regarding the denormalization process (e.g. taxpayers fields, account category names, etc.)
	 */
	public static final String INDEX_PUBLISHED_GENERAL_LEDGER = IndexNamesUtils.formatIndexNameForPublishedData("General Ledger");

	/**
	 * Index name for published (denormalized) data regarding Balance Sheet.<BR>
	 * There is one record for each month and each account.
	 */
	public static final String INDEX_PUBLISHED_BALANCE_SHEET = IndexNamesUtils.formatIndexNameForPublishedData("Balance Sheet Monthly");
	
	/**
	 * Index name for published (denormalized) data regarding Accounting Flows.<BR>
	 * There is one record for each day and each pair of accounts (credited and debited).
	 */
	public static final String INDEX_PUBLISHED_ACCOUNTING_FLOW = IndexNamesUtils.formatIndexNameForPublishedData("Accounting Flow Daily");

	/**
	 * Ignores differences lesser than half of a cent	
	 */
	private static final double EPSILON = 0.005;		

	/**
	 * Fields names for published (denormalized) views
	 */
	public static enum AccountingFieldNames {
		
		/**
		 * The final balance amount after a book entry in General Ledger
		 */
		Balance,
		
		/**
		 * The debit/credit indication of the final balance amount after a book entry in General Ledger
		 */
		BalanceDebitCredit,
		
		/**
		 * The name of the account category
		 */
		AccountCategoryName,
		
		/**
		 * The name of the account sub-category
		 */
		AccountSubcategoryName,
		
		/**
		 * The total amount of debits (does not sum the credits)
		 */
		AmountDebits,
		
		/**
		 * The total amount of credits (does not sum the debits)
		 */
		AmountCredits,
		
		/**
		 * The number of book entries (for Monthly Balance Sheets)
		 */
		BookEntries,
		
		/**
		 * Debit/Credit indication for initial balance (for Monthly Balance Sheets)
		 */
		InitialBalanceDebitCredit,
		
		/**
		 * The final balance (for Monthly Balance Sheets)
		 */
		FinalBalance,
		
		/**
		 * Debit/Credit indication for final balance (for Monthly Balance Sheets)
		 */
		FinalBalanceDebitCredit,
		
		/**
		 * Year indication (for Monthly Balance Sheets and Daily Accounting Flows)
		 */
		Year,
		
		/**
		 * Month indication (for Monthly Balance Sheets and Daily Accounting Flows)
		 */
		Month,
		
		/**
		 * Credited account code (for Daily Accounting Flows)
		 */
		CreditedAccount,
		
		/**
		 * Debited account code (for Daily Accounting Flows)
		 */
		DebitedAccount,
		
	};
	
	/**
	 * The field name for published data regarding 'Account Code'
	 */
	private static final String ledgerAccountCode = IndexNamesUtils.formatFieldName(GeneralLedgerArchetype.FIELDS_NAMES.AccountCode.name());

	/**
	 * The field name for published data regarding 'Date'
	 */
	private static final String ledgerDate = IndexNamesUtils.formatFieldName(GeneralLedgerArchetype.FIELDS_NAMES.Date.name());

	/**
	 * The field name for validated data regarding 'Book Entry' in General Ledger
	 */
	private static final String ledgerId = IndexNamesUtils.formatFieldName(GeneralLedgerArchetype.FIELDS_NAMES.EntryId.name());

	/**
	 * The field name for published data regarding 'Amount' (some value, may be debit or may be credit)
	 */
	private static final String ledgerAmount = IndexNamesUtils.formatFieldName(GeneralLedgerArchetype.FIELDS_NAMES.Amount.name());

	/**
	 * The field name for published data regarding 'D/C' indication of amount
	 */
	private static final String ledgerDebitCredit = IndexNamesUtils.formatFieldName(GeneralLedgerArchetype.FIELDS_NAMES.DebitCredit.name());

	/**
	 * The field name for published data regarding amount of debit values
	 */
	private static final String ledgerAmountDebits = IndexNamesUtils.formatFieldName(AccountingFieldNames.AmountDebits.name());

	/**
	 * The field name for published data regarding amount of credit values
	 */
	private static final String ledgerAmountCredits = IndexNamesUtils.formatFieldName(AccountingFieldNames.AmountCredits.name());
	
	/**
	 * The field name for published data regarding the initial balance amount for each Monthly Balance Sheet
	 */
	private static final String openingBalanceInitial = IndexNamesUtils.formatFieldName(OpeningBalanceArchetype.FIELDS_NAMES.InitialBalance.name());

	/**
	 * The field name for validated data regarding the 'D/C' indication of initial balance amount
	 */
	private static final String openingBalanceDC = IndexNamesUtils.formatFieldName(OpeningBalanceArchetype.FIELDS_NAMES.DebitCredit.name());
	
	/**
	 * The field name for published data regarding the 'D/C' indication of initial balance amount for each Monthly Balance Sheet
	 */
	private static final String openingBalanceMonthlyDC = IndexNamesUtils.formatFieldName(AccountingFieldNames.InitialBalanceDebitCredit.name());

	/**
	 * The field name for published data regarding the closing balance amount for each Monthly Balance Sheet
	 */
	private static final String closingBalanceMonthly = IndexNamesUtils.formatFieldName(AccountingFieldNames.FinalBalance.name());

	/**
	 * The field name for published data regarding the 'D/C' indication of closing balance amount for each Monthly Balance Sheet
	 */
	private static final String closingBalanceMonthlyDC = IndexNamesUtils.formatFieldName(AccountingFieldNames.FinalBalanceDebitCredit.name());
	
	/**
	 * The number of book entries for each Monthly Balance Sheet
	 */
	private static final String bookEntries = IndexNamesUtils.formatFieldName(AccountingFieldNames.BookEntries.name());
	
	/**
	 * The field name for date/time of published data
	 */
	private static final String publishedTimestamp = IndexNamesUtils.formatFieldName(PublishedDataFieldNames.TIMESTAMP.name());
	
	/**
	 * The field name for indication of Year for each Monthly Balance Sheet
	 */
	private static final String publishedYear = IndexNamesUtils.formatFieldName(AccountingFieldNames.Year.name());

	/**
	 * The field name for indication of Month for each Monthly Balance Sheet
	 */
	private static final String publishedMonth = IndexNamesUtils.formatFieldName(AccountingFieldNames.Month.name());

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
	 * The field name for final balance after each General Ledger entry
	 */
	private static final String ledgerBalance = IndexNamesUtils.formatFieldName(AccountingFieldNames.Balance.name());

	/**
	 * The field name for the Debit/Credit indication of the final balance after each General Ledger entry
	 */
	private static final String ledgerBalanceDebitCredit = IndexNamesUtils.formatFieldName(AccountingFieldNames.BalanceDebitCredit.name());

	/**
	 * The field name for the credited account code for Daily Accounting Flow
	 */
	private static final String creditedAccount = IndexNamesUtils.formatFieldName(AccountingFieldNames.CreditedAccount.name());

	/**
	 * The field name for the debited account code for Daily Accounting Flow
	 */
	private static final String debitedAccount = IndexNamesUtils.formatFieldName(AccountingFieldNames.DebitedAccount.name());

	/**
	 * Comparator of Document Templates that gives precedence to the most recent (according to date of creation)
	 */
	public static final Comparator<DocumentTemplate> DOCUMENT_TEMPLATE_COMPARATOR = new Comparator<DocumentTemplate>() {

		@Override
		public int compare(DocumentTemplate t1, DocumentTemplate t2) {
			OffsetDateTime t1_timestamp = t1.getTemplateCreateTime();
			OffsetDateTime t2_timestamp = t2.getTemplateCreateTime();
			return t2_timestamp.compareTo(t1_timestamp);
		}
		
	};
	
	/**
	 * Returns validated document regarding a specific taxpayerId and tax Period according to a collection of document templates
	 */
	public static DocumentUploaded getValidatedDocument(String taxPayerId, Integer taxPeriodNumber, Collection<DocumentTemplate> templates, ETLContext context) throws Exception {
		DocumentUploaded foundUploadWithEmptyData = null;
		DocumentUploaded foundUploadMatchingCriteria = null;
		for (DocumentTemplate template: templates.stream().sorted(DOCUMENT_TEMPLATE_COMPARATOR).collect(Collectors.toList())) {
			Collection<DocumentUploaded> uploads = context.getValidatedDataRepository().getUploads(template.getName(), template.getVersion(), taxPayerId, taxPeriodNumber);
			if (uploads.isEmpty()) {
				continue; // no uploads found with this particular template, try another template 
			}
			for (DocumentUploaded upload: uploads) {
				if (!DocumentSituation.VALID.equals(upload.getSituation())
					&& !DocumentSituation.PROCESSED.equals(upload.getSituation())
					&& !DocumentSituation.PENDING.equals(upload.getSituation())
					&& !DocumentSituation.REPLACED.equals(upload.getSituation()))
					continue; // ignores invalid or not validated files
				boolean has_data = context.getValidatedDataRepository().hasValidation(template.getName(), template.getVersion(), upload.getFileId());
				if (!has_data) {
					if (foundUploadWithEmptyData==null)
						foundUploadWithEmptyData = upload; // keep this information in case we don't find any other valid data
					continue;
				}
				// Found data for this template, but maybe there is another one more recent
				// So we will keep looking for more occurrences.
				if (foundUploadMatchingCriteria==null 
					|| foundUploadMatchingCriteria.getTimestamp().isBefore(upload.getTimestamp()))
				foundUploadMatchingCriteria = upload;
			}
		}
		if (foundUploadMatchingCriteria!=null)
			return foundUploadMatchingCriteria;
		if (foundUploadWithEmptyData!=null)
			return foundUploadWithEmptyData;
		return null;
	}
	
	/**
	 * Returns object used for retrieving information from Chart of Accounts, keeping a temporary cache in memory.
	 */
	public static LoadingCache<String, Optional<Map<String, Object>>> getLookupChartOfAccounts(final ETLContext.ValidatedDataRepository repository, 
			final Optional<DomainTable> category_domain_table,
			final Optional<DomainTable> subcategory_domain_table,
			final DocumentUploaded coa) {
		return CacheBuilder.newBuilder()
			.maximumSize(1000)
			.expireAfterWrite(10, TimeUnit.MINUTES)
			.build(new CacheLoader<String, Optional<Map<String,Object>>>(){
				final String fieldName = IndexNamesUtils.formatFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountCode.name())+".keyword";
				@Override
				public Optional<Map<String,Object>> load(String accountCode) throws Exception {
					if (accountCode==null)
						return Optional.empty();
					else {
						try {
							Optional<Map<String,Object>> accountInfo = repository.getValidatedData(coa.getTemplateName(), coa.getTemplateVersion(), coa.getFileId(), fieldName, accountCode)
									.map(IndexNamesUtils::normalizeAllKeysForES);
							if (accountInfo.isPresent() && category_domain_table.isPresent()) {
								String category = ValidationContext.toString(accountInfo.get().get(IndexNamesUtils.formatFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountCategory.name())));
								if (category!=null) {
									List<DomainEntry> entry_multi_lingual = category_domain_table.get().getEntryAllLanguages(category);
									if (entry_multi_lingual!=null && !entry_multi_lingual.isEmpty()) {
										for (DomainEntry entry: entry_multi_lingual) {
											String derivedFieldName = IndexNamesUtils.formatFieldName(AccountingFieldNames.AccountCategoryName.name());
											if (entry.getLanguage()!=null && !DomainLanguage.ENGLISH.equals(entry.getLanguage())) {
												derivedFieldName += "_" + entry.getLanguage().getDefaultLocale().getLanguage();
											}
											accountInfo.get().put(derivedFieldName, entry.getDescription());
										}
									}
								}
							}
							if (accountInfo.isPresent() && subcategory_domain_table.isPresent()) {
								String subcategory = ValidationContext.toString(accountInfo.get().get(IndexNamesUtils.formatFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountSubcategory.name())));
								if (subcategory!=null) {
									List<DomainEntry> entry_multi_lingual = subcategory_domain_table.get().getEntryAllLanguages(subcategory);
									if (entry_multi_lingual!=null && !entry_multi_lingual.isEmpty()) {
										for (DomainEntry entry: entry_multi_lingual) {
											String derivedFieldName = IndexNamesUtils.formatFieldName(AccountingFieldNames.AccountSubcategoryName.name());
											if (entry.getLanguage()!=null && !DomainLanguage.ENGLISH.equals(entry.getLanguage())) {
												derivedFieldName += "_" + entry.getLanguage().getDefaultLocale().getLanguage();
											}
											accountInfo.get().put(derivedFieldName, entry.getDescription());
										}
									}
								}
							}
							return accountInfo;
						}
						catch (Throwable ex) {
							return Optional.empty();
						}
					}
				}
			});
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
							return repository.getValidatedData(ob.getTemplateName(), ob.getTemplateVersion(), ob.getFileId(), fieldName, accountCode).map(IndexNamesUtils::normalizeAllKeysForES);
						}
						catch (Throwable ex) {
							return Optional.empty();
						}
					}
				}
			});
	}
	
	/**
	 * Returns object used for retrieving information from Taxpayers Registry, keeping a temporary cache in memory.
	 */
	public static LoadingCache<String, Optional<Map<String, Object>>> getLookupTaxpayers(final TaxpayerRepository repository) {
		return CacheBuilder.newBuilder()
			.maximumSize(1000)
			.expireAfterWrite(10, TimeUnit.MINUTES)
			.build(new CacheLoader<String, Optional<Map<String,Object>>>(){
				@Override
				public Optional<Map<String,Object>> load(String taxPayerId) throws Exception {
					if (taxPayerId==null || repository==null)
						return Optional.empty();
					else {
						try {
							return repository.getTaxPayerData(taxPayerId).map(IndexNamesUtils::normalizeAllKeysForES);
						}
						catch (Throwable ex) {
							return Optional.empty();
						}
					}
				}
			});
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
	 * Performs the Extract/Transform/Load operations with available data
	 */
	public static boolean performETL(ETLContext context) {
		
		try {
				
			// Check for the presence of all required data
			
			Collection<DocumentTemplate> templatesForCoA = context.getValidatedDataRepository().getTemplates(ChartOfAccountsArchetype.NAME);
			if (templatesForCoA.isEmpty()) {
				context.addAlert(context.getDocumentUploaded(), "{error.missingTemplate({accounting.chart.accounts})}");
				return false;
			}
			
			Collection<DocumentTemplate> templatesForLedger = context.getValidatedDataRepository().getTemplates(GeneralLedgerArchetype.NAME);
			if (templatesForLedger.isEmpty()) {
				context.addAlert(context.getDocumentUploaded(), "{error.missingTemplate({accounting.general.ledger})}");
				return false;
			}
			
			Collection<DocumentTemplate> templatesForOpening = context.getValidatedDataRepository().getTemplates(OpeningBalanceArchetype.NAME);
			if (templatesForOpening.isEmpty()) {
				context.addAlert(context.getDocumentUploaded(), "{error.missingTemplate({accounting.opening.balance})}");
				return false;
			}
			
			String taxPayerId = context.getDocumentUploaded().getTaxPayerId();
			Integer taxPeriodNumber = context.getDocumentUploaded().getTaxPeriodNumber();
			
			final DocumentUploaded coa = getValidatedDocument(taxPayerId, taxPeriodNumber, templatesForCoA, context);
			if (coa==null) {
				context.addAlert(context.getDocumentUploaded(), "{error.missingFile({accounting.chart.accounts})}");
				return false;
			}
			final DocumentUploaded gl = getValidatedDocument(taxPayerId, taxPeriodNumber, templatesForLedger, context);
			if (gl==null) {
				context.addAlert(context.getDocumentUploaded(), "{error.missingFile({accounting.general.ledger})}");
				return false;
			}
			final DocumentUploaded ob = getValidatedDocument(taxPayerId, taxPeriodNumber, templatesForOpening, context);
			if (ob==null) {
				context.addAlert(context.getDocumentUploaded(), "{error.missingFile({accounting.opening.balance})}");
				return false;
			}
			
			// If we got here, we have enough information for generating denormalized data
			
			DocumentTemplate coa_template =
					templatesForCoA.stream().filter(t->coa.getTemplateName().equalsIgnoreCase(t.getName()) && coa.getTemplateVersion().equalsIgnoreCase(t.getVersion()))
					.findFirst().orElse(null);
			DocumentField category_field = (coa_template==null) ? null : coa_template.getField(ChartOfAccountsArchetype.FIELDS_NAMES.AccountCategory.name());
			DocumentField subcategory_field = (coa_template==null) ? null : coa_template.getField(ChartOfAccountsArchetype.FIELDS_NAMES.AccountSubcategory.name());
			Optional<DomainTable> category_domain_table = (category_field==null) ? Optional.empty() : context.getDomainTableRepository().findByNameAndVersion(category_field.getDomainTableName(), category_field.getDomainTableVersion());
			Optional<DomainTable> subcategory_domain_table = (subcategory_field==null) ? Optional.empty() : context.getDomainTableRepository().findByNameAndVersion(subcategory_field.getDomainTableName(), subcategory_field.getDomainTableVersion());
			
			DocumentTemplate gl_template =
					templatesForLedger.stream().filter(t->gl.getTemplateName().equalsIgnoreCase(t.getName()) && gl.getTemplateVersion().equalsIgnoreCase(t.getVersion()))
					.findFirst().orElse(null);
			Periodicity periodicity = gl_template.getPeriodicity();

			// Structure for loading and caching information from the provided Chart of Accounts
			LoadingCache<String, Optional<Map<String, Object>>> lookupChartOfAccounts = getLookupChartOfAccounts(context.getValidatedDataRepository(), 
					category_domain_table, subcategory_domain_table, coa);

			// Structure for loading and caching information from the provided Opening Balance
			LoadingCache<String, Optional<Map<String, Object>>> lookupOpeningBalance = getLookupOpeningBalance(context.getValidatedDataRepository(), ob);
			
			// Structure for loading and caching information from the provided Taxpayers registry
			LoadingCache<String, Optional<Map<String, Object>>> lookupTaxpayers = getLookupTaxpayers(context.getTaxpayerRepository());
			
			final Optional<Map<String,Object>> declarantInformation = lookupTaxpayers.getUnchecked(taxPayerId);

			LoadDataStrategy loader = context.getLoadDataStrategy();
			loader.start();
			
			final LongAdder countRecordsOverall = new LongAdder();
			final AtomicLong countRecordsInGeneralLedger = new AtomicLong();
			final AtomicLong countRecordsInBalanceSheet = new AtomicLong();
			final AtomicLong countRecordsInAccountingFlows = new AtomicLong();
			
	        final OffsetDateTime timestamp = context.getDocumentUploaded().getTimestamp();

			// Computes balance sheets while iterating over General Ledger entries
			final Map<String, BalanceSheet> mapBalanceSheets = new HashMap<>();
			
			// Computes accounting flows
			final AccountingFlowProcessor accountingFlowProc = new AccountingFlowProcessor();
			accountingFlowProc.setCollectWarnings(alert->context.addAlert(gl, alert));
			accountingFlowProc.setCollectDailyAccountingFlows(flow->{
				addRecordToDailyAccountingFlow(
						gl,
						flow,
						taxPayerId,
						taxPeriodNumber,
						timestamp,
						countRecordsInAccountingFlows,
						countRecordsOverall,
						declarantInformation,
						lookupChartOfAccounts,
						loader);
			});

			// Search for the validated general ledger related to the matching template
			// Reads the validated general ledger in chronological order. For each day order by ledger entry ID.
			Stream<Map<String, Object>> gl_data = context.getValidatedDataRepository().getValidatedData(gl.getTemplateName(), gl.getTemplateVersion(), gl.getFileId(),
					/*sortBy*/Optional.of(new String[] {ledgerDate, ledgerId+".keyword" }),
					/*sortOrder*/Optional.of(SortOrder.ASC));
			
			if (gl_data==null)
				gl_data = Collections.<Map<String, Object>>emptySet().stream();
			
			// Deletes previous published data
			for (String index: new String[] {
				INDEX_PUBLISHED_GENERAL_LEDGER,
				INDEX_PUBLISHED_BALANCE_SHEET
			}) {

				try {
					context.getLoadDataStrategy().delete(index, taxPayerId, taxPeriodNumber);
				} catch (Throwable ex) {
					log.log(Level.SEVERE, "Error while deleting previous published data at "+index+" regarding "+taxPayerId+" and period "+taxPeriodNumber, ex);
				}

			}
			
			// Start the denormalization process
			
			boolean success = true;
			try {
				
				// Stores here the last month (a number in the format yyyy-mm) of the last parsed entry from the Journal
				final AtomicInteger previousMonth = new AtomicInteger(0);
				
				// Keep track of all known account codes for reporting balance sheets. This list may grow as we proceed.
				final Set<String> accountsForBalanceSheets = getAccountsForOpeningBalances(context.getValidatedDataRepository(), ob);
				
				// Keep track of informed balance sheets for accounts (first in key pair) and months (second in key pair) and their corresponding
				// final balance. This will be used for completing balance sheets for absent months after we have processed all the data.
				final Map<Pair<String, Integer>, Double> informedBalancedSheetsAccountsAndMonths = new HashMap<>();
				
				gl_data.forEach(record->{
					
					OffsetDateTime date = ValidationContext.toOffsetDateTime(record.get(ledgerDate));
					final int year_month = (date==null) ? 0 : ( date.getYear() * 100 + date.getMonthValue() );
					final boolean changed_month = year_month!=0 && previousMonth.get()!=0 && previousMonth.get()!=year_month;
					final int previous_year_month = previousMonth.get();
					if (date!=null && (changed_month || previousMonth.get()==0)) {
						previousMonth.set(year_month);
					}
					
					String entryId = ValidationContext.toString(record.get(ledgerId));

					String accountCode = ValidationContext.toString(record.get(ledgerAccountCode));
					if (accountCode==null)
						accountCode = "";
					
					final Optional<Map<String,Object>> accountInformation = lookupChartOfAccounts.getUnchecked(accountCode);
					if (!accountInformation.isPresent() && accountCode.length()>0) {
						DocumentUploaded reporting_doc = (coa.equals(context.getDocumentUploaded())) ? coa : gl;
						context.addAlert(reporting_doc, "{account.error.ledger.invalid.account("
							+accountCode.replaceAll("[\\{\\}\\,\\(\\)\r\n\t]","")
							+","
							+entryId.replaceAll("[\\{\\}\\,\\(\\)]\r\n\t","")
							+")}");
						// Either CoA or GL should be replaced
						context.setOutcomeSituation(coa, DocumentSituation.PENDING);
						context.setOutcomeSituation(gl, DocumentSituation.PENDING);
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

					Number amount = ValidationContext.toNumber(record.get(ledgerAmount));
					String debitCredit = ValidationContext.toString(record.get(ledgerDebitCredit));
					if (debitCredit==null)
						debitCredit = "";
					boolean is_debit = debitCredit.equalsIgnoreCase("D");
					
					// Computes Monthly Balance Sheet
					
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
							context.addAlert(ob, "{account.error.missing.opening.balance("
									+acc.replaceAll("[\\{\\}\\,\\(\\)\r\n\t]","")
									+")}");
							context.setOutcomeSituation(ob, DocumentSituation.PENDING);
							accountsForBalanceSheets.add(acc);
						}
						return balance;
					});
					
					balanceSheet.computeEntry(amount, is_debit);
					
					// Computes Accounting Flow
					
					if (amount!=null && Math.abs(amount.doubleValue())>EPSILON)
						accountingFlowProc.computeEntry(entryId, date, accountCode, amount, is_debit);

					// Publish denormalized GENERAL LEDGER record
					
					String rowId_GL = String.format("%s.%d.%014d", taxPayerId, taxPeriodNumber, countRecordsInGeneralLedger.incrementAndGet());
					Map<String,Object> normalizedRecord_GL = new HashMap<>(record);
					for (ValidatedDataFieldNames vfieldName: ValidatedDataFieldNames.values()) {
						// Published data has all fields in lower case
						Object value = normalizedRecord_GL.remove(vfieldName.name());
						if (value!=null) {
							normalizedRecord_GL.put(IndexNamesUtils.formatFieldName(vfieldName.name()), value);
						}
					}
					normalizedRecord_GL.put(publishedTimestamp, timestamp);
					normalizedRecord_GL.put(publishedTaxpayerId, taxPayerId);
					normalizedRecord_GL.put(publishedtaxPeriodNumber, taxPeriodNumber);
					normalizedRecord_GL.put(publishedTemplateName, gl.getTemplateName());
					normalizedRecord_GL.put(publishedTemplateVersion, gl.getTemplateVersion());
					normalizedRecord_GL.put(ledgerBalance, balanceSheet.getFinalValue());
					normalizedRecord_GL.put(ledgerBalanceDebitCredit, balanceSheet.isFinalValueDebit() ? "D" : "C");
					if (amount!=null && is_debit)
						normalizedRecord_GL.put(ledgerAmountDebits, amount);
					if (amount!=null && !is_debit)
						normalizedRecord_GL.put(ledgerAmountCredits, amount);
					if (declarantInformation.isPresent())
						normalizedRecord_GL.putAll(declarantInformation.get());
					if (accountInformation.isPresent())
						normalizedRecord_GL.putAll(accountInformation.get());

					loader.add(new IndexRequest(INDEX_PUBLISHED_GENERAL_LEDGER)
						.id(rowId_GL)
						.source(normalizedRecord_GL));
					countRecordsOverall.increment();

				}); // LOOP over all entries in General Ledger
				
				// After processing all the General Ledger, let's fill the Monthly Balance Sheet for all accounts
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
				
				// After processing all the General Ledger, let's finish computing the Daily Accounting Flows
				accountingFlowProc.finish();
				
			}
			finally {
				gl_data.close();
				try {
					loader.commit();
				}
				catch (Throwable ex) {
					log.log(Level.SEVERE, "Error while storing "+countRecordsOverall.longValue()+" rows of denormalized data for taxpayer id "+taxPayerId+" period "+taxPeriodNumber, ex);
					success = false;
				}
				loader.close();
			}
			
			if (success && !context.hasOutcomeSituations()) {
				context.setOutcomeSituation(coa, DocumentSituation.PROCESSED);
				ETLContext.markReplacedDocuments(coa, context);
				
				context.setOutcomeSituation(gl, DocumentSituation.PROCESSED);
				ETLContext.markReplacedDocuments(gl, context);
				
				context.setOutcomeSituation(ob, DocumentSituation.PROCESSED);
				ETLContext.markReplacedDocuments(ob, context);
			}
			
			return success;
			
		}
		catch (Throwable ex) {
			String fileId = (context==null || context.getDocumentUploaded()==null) ? null : context.getDocumentUploaded().getFileId();
			log.log(Level.SEVERE, "Error while performing ETL regarding file "+fileId, ex);
			return false;
		}
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
	private static void addRecordToMonthlyBalanceSheet(
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
				final Optional<Map<String,Object>> accountInformation = lookupChartOfAccounts.getUnchecked(account);
				String rowId_BS = String.format("%s.%d.%014d", taxPayerId, taxPeriodNumber, countRecordsInBalanceSheet.incrementAndGet());
				Map<String,Object> normalizedRecord_BS = new HashMap<>();
				normalizedRecord_BS.put(publishedTimestamp, timestamp);
				normalizedRecord_BS.put(publishedTaxpayerId, taxPayerId);
				normalizedRecord_BS.put(publishedtaxPeriodNumber, taxPeriodNumber);
				normalizedRecord_BS.put(publishedTemplateName, gl.getTemplateName());
				normalizedRecord_BS.put(publishedTemplateVersion, gl.getTemplateVersion());
				normalizedRecord_BS.put(publishedYear, year);
				normalizedRecord_BS.put(publishedMonth, month);
				normalizedRecord_BS.put(ledgerAccountCode, account);
				normalizedRecord_BS.put(openingBalanceInitial, Math.abs(computedBalanceSheet.getInitialValue()));
				normalizedRecord_BS.put(openingBalanceMonthlyDC, (computedBalanceSheet.isInitialValueDebit()) ? "D" : "C");
				normalizedRecord_BS.put(ledgerAmountDebits, computedBalanceSheet.getDebits());
				normalizedRecord_BS.put(ledgerAmountCredits, computedBalanceSheet.getCredits());
				normalizedRecord_BS.put(closingBalanceMonthly, Math.abs(computedBalanceSheet.getFinalValue()));
				normalizedRecord_BS.put(closingBalanceMonthlyDC, (computedBalanceSheet.isFinalValueDebit()) ? "D" : "C");
				normalizedRecord_BS.put(bookEntries, computedBalanceSheet.getCountEntries());
				if (declarantInformation.isPresent())
					normalizedRecord_BS.putAll(declarantInformation.get());
				if (accountInformation.isPresent())
					normalizedRecord_BS.putAll(accountInformation.get());
				loader.add(new IndexRequest(INDEX_PUBLISHED_BALANCE_SHEET)
					.id(rowId_BS)
					.source(normalizedRecord_BS));
				informedBalancedSheetsAccountsAndMonths.put(Pair.of(account, yearMonth), computedBalanceSheet.getFinalValue());
				countRecordsOverall.increment();	
				computedBalanceSheet.flipBalance();
			}
		}
	}
	
	/**
	 * Feed information about missing periods or missing accounts in Monthly Balance Sheets.
	 */
	private static void fillMissingMonthlyBalanceSheet(
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
				
				final Optional<Map<String,Object>> accountInformation = lookupChartOfAccounts.getUnchecked(account);
				String rowId_BS = String.format("%s.%d.%014d", taxPayerId, taxPeriodNumber, countRecordsInBalanceSheet.incrementAndGet());
				Map<String,Object> normalizedRecord_BS = new HashMap<>();
				normalizedRecord_BS.put(publishedTimestamp, timestamp);
				normalizedRecord_BS.put(publishedTaxpayerId, taxPayerId);
				normalizedRecord_BS.put(publishedtaxPeriodNumber, taxPeriodNumber);
				normalizedRecord_BS.put(publishedTemplateName, gl.getTemplateName());
				normalizedRecord_BS.put(publishedTemplateVersion, gl.getTemplateVersion());
				normalizedRecord_BS.put(publishedYear, year);
				normalizedRecord_BS.put(publishedMonth, month);
				normalizedRecord_BS.put(ledgerAccountCode, account);
				normalizedRecord_BS.put(openingBalanceInitial, Math.abs(previous_final_balance.doubleValue()));
				normalizedRecord_BS.put(openingBalanceMonthlyDC, (previous_final_balance.doubleValue()>=0) ? "D" : "C");
				normalizedRecord_BS.put(ledgerAmountDebits, 0.0);
				normalizedRecord_BS.put(ledgerAmountCredits, 0.0);
				normalizedRecord_BS.put(closingBalanceMonthly, Math.abs(previous_final_balance.doubleValue()));
				normalizedRecord_BS.put(closingBalanceMonthlyDC, (previous_final_balance.doubleValue()>=0) ? "D" : "C");
				normalizedRecord_BS.put(bookEntries, 0);
				if (declarantInformation.isPresent())
					normalizedRecord_BS.putAll(declarantInformation.get());
				if (accountInformation.isPresent())
					normalizedRecord_BS.putAll(accountInformation.get());
				loader.add(new IndexRequest(INDEX_PUBLISHED_BALANCE_SHEET)
					.id(rowId_BS)
					.source(normalizedRecord_BS));
				countRecordsOverall.increment();	
				
			} // LOOP over account codes
			
		} // LOOP over expected months
	}
	
	/**
	 * Feeds information about Daily Accounting Flows
	 * @param gl The Upload record regarding the General Ledger
	 * @param flow One computed Daily Accounting Flow
	 * @param taxPayerId The Taxpayer ID
	 * @param taxPeriodNumber The number of the period
	 * @param timestamp Time date/time of ETL procedure
	 * @param countRecordsInAccountingFlows The total number of 'Monthly Balance Sheets' records (incremented here)
	 * @param countRecordsOverall The total number of records (incremented here)
	 * @param declarantInformation Additional information about the declarant
	 * @param lookupChartOfAccounts Object used for searching additional information about accounts
	 * @param loader Object used for writing the results
	 */
	private static void addRecordToDailyAccountingFlow(
			final DocumentUploaded gl,
			final DailyAccountingFlow flow,
			final String taxPayerId,
			final Integer taxPeriodNumber,
			final OffsetDateTime timestamp,
			final AtomicLong countRecordsInAccountingFlows,
			final LongAdder countRecordsOverall,
			final Optional<Map<String,Object>> declarantInformation,
			final LoadingCache<String, Optional<Map<String, Object>>> lookupChartOfAccounts,
			final LoadDataStrategy loader) {
		
		final int year = flow.getDate().getYear();
		final int monthNumber = flow.getDate().getMonthValue();
		final String month = Month.of(monthNumber).getDisplayName(
	        TextStyle.SHORT, 
	        Locale.getDefault()
	    );
		
		final Optional<Map<String,Object>> accountDebitedInfo = (flow.hasDebitedManyAccountCodes()) ? Optional.empty() : lookupChartOfAccounts.getUnchecked(flow.getDebitedAccountCode());
		final Optional<Map<String,Object>> accountCreditedInfo = (flow.hasCreditedManyAccountCodes()) ? Optional.empty() : lookupChartOfAccounts.getUnchecked(flow.getCreditedAccountCode());
		String rowId_DAF = String.format("%s.%d.%014d", taxPayerId, taxPeriodNumber, countRecordsInAccountingFlows.incrementAndGet());
		Map<String,Object> normalizedRecord_DAF = new HashMap<>();
		normalizedRecord_DAF.put(publishedTimestamp, timestamp);
		normalizedRecord_DAF.put(publishedTaxpayerId, taxPayerId);
		normalizedRecord_DAF.put(publishedtaxPeriodNumber, taxPeriodNumber);
		normalizedRecord_DAF.put(publishedTemplateName, gl.getTemplateName());
		normalizedRecord_DAF.put(publishedTemplateVersion, gl.getTemplateVersion());
		normalizedRecord_DAF.put(publishedYear, year);
		normalizedRecord_DAF.put(publishedMonth, month);
		normalizedRecord_DAF.put(ledgerDate, ValidationContext.toDate(flow.getDate()));
		normalizedRecord_DAF.put(creditedAccount, flow.getCreditedAccountCode());
		normalizedRecord_DAF.put(debitedAccount, flow.getDebitedAccountCode());
		normalizedRecord_DAF.put(ledgerAmount, flow.getAmount());
		if (declarantInformation.isPresent())
			normalizedRecord_DAF.putAll(declarantInformation.get());
		if (accountDebitedInfo.isPresent())
			normalizedRecord_DAF.putAll(addPrefixToMapKeys(accountDebitedInfo.get(),"debited_"));
		if (accountCreditedInfo.isPresent())
			normalizedRecord_DAF.putAll(addPrefixToMapKeys(accountCreditedInfo.get(),"credited_"));
		loader.add(new IndexRequest(INDEX_PUBLISHED_ACCOUNTING_FLOW)
			.id(rowId_DAF)
			.source(normalizedRecord_DAF));
		countRecordsOverall.increment();	
	}
	
	/**
	 * Adds a prefix to every key in the map, returning the modified map (the original map keeps unchanged)
	 */
	public static Map<String, Object> addPrefixToMapKeys(Map<String, Object> map, String prefix) {
		if (map==null || map.isEmpty() || prefix==null || prefix.length()==0)
			return map;
		return map.entrySet().stream().collect(Collectors.toMap(
			/*keyMapper*/e->prefix+e.getKey(), 
			/*valueMapper*/Map.Entry::getValue, 
			/*mergeFunction*/(a,b)->a));
	}

	/**
	 * Wraps computed information about balance sheet while iterating over General Ledger entries
	 * @author Gustavo Figueiredo
	 */
	public static class BalanceSheet {
		
		/**
		 * Positive = debit, Negative = credit
		 */
		private double initialValue;
		
		private double debits;
		
		private double credits;
		
		private int countEntries;

		/**
		 * Positive = debit, Negative = credit
		 */
		public double getInitialValue() {
			return initialValue;
		}

		/**
		 * Positive = debit, Negative = credit
		 */
		public void setInitialValue(double initialValue) {
			this.initialValue = initialValue;
		}
		
		public boolean isInitialValueDebit() {
			return initialValue >= 0;
		}

		public double getDebits() {
			return debits;
		}

		public void setDebits(double debits) {
			this.debits = debits;
		}

		public double getCredits() {
			return credits;
		}

		public void setCredits(double credits) {
			this.credits = credits;
		}
		
		public void computeEntry(Number amount, boolean isDebit) {
			if (amount==null)
				return;
			if (isDebit)
				debits += Math.abs(amount.doubleValue());
			else
				credits += Math.abs(amount.doubleValue());
			countEntries++;
		}
		
		/**
		 * Positive = debit, Negative = credit
		 */
		public double getFinalValue() {
			return initialValue + debits - credits;
		}
		
		public boolean isFinalValueDebit() {
			return  ( initialValue + debits - credits ) >= 0;
		}

		public int getCountEntries() {
			return countEntries;
		}

		public void setCountEntries(int countEntries) {
			this.countEntries = countEntries;
		}
				
		/**
		 * Overwrite the initial balance amount with the final balance amount and reset the total debits and credits.
		 * Useful for making use of the same object for the 'next period' for monthly balance sheets.
		 */
		public void flipBalance() {
			initialValue = getFinalValue();
			debits = 0;
			credits = 0;
			countEntries = 0;
		}
	}
}
