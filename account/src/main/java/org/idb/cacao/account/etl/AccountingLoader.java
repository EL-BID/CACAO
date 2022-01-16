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

import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.AccountCategory;

import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.Arrays;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.search.sort.SortOrder;
import org.idb.cacao.account.archetypes.AccountBuiltInDomainTables;
import org.idb.cacao.account.archetypes.ChartOfAccountsArchetype;
import org.idb.cacao.account.archetypes.GeneralLedgerArchetype;
import org.idb.cacao.account.archetypes.OpeningBalanceArchetype;
import org.idb.cacao.account.elements.AccountStandard;
import org.idb.cacao.account.elements.BalanceSheet;
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
	 * Index name for published (denormalized) data regarding the computed Statement of Incomes.<BR>
	 * There is one record for each year and each Statement of Income entry (according to domain table).
	 */
	public static final String INDEX_PUBLISHED_COMPUTED_STATEMENT_INCOME = IndexNamesUtils.formatIndexNameForPublishedData("Accounting Computed Statement Income");

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
	 * Fields names that are used for keeping track of source of data
	 */
	public static final Set<String> TRACKING_FIELDS = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
	static {
		Arrays.stream(ValidatedDataFieldNames.values()).map(Object::toString).forEach(TRACKING_FIELDS::add);
		Arrays.stream(ValidatedDataFieldNames.values()).map(Enum::name).forEach(TRACKING_FIELDS::add);
		Arrays.stream(ValidatedDataFieldNames.values()).map(e->IndexNamesUtils.formatFieldName(e.name())).forEach(TRACKING_FIELDS::add);

		Arrays.stream(PublishedDataFieldNames.values()).map(Object::toString).forEach(TRACKING_FIELDS::add);
		Arrays.stream(PublishedDataFieldNames.values()).map(Enum::name).forEach(TRACKING_FIELDS::add);
		Arrays.stream(PublishedDataFieldNames.values()).map(e->IndexNamesUtils.formatFieldName(e.name())).forEach(TRACKING_FIELDS::add);
	}

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
									.map(IndexNamesUtils::normalizeAllKeysForES)
									.map(AccountingLoader::removeControlFields);
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
			final Periodicity periodicity = gl_template.getPeriodicity();

			DocumentField account_category_field_map = coa_template.getField(AccountCategory.name());
			String account_category_domain_table_name = (account_category_field_map==null) ? null : account_category_field_map.getDomainTableName();
			final AccountStandard account_standard = AccountBuiltInDomainTables.getAccountStandardRelatedToDomainTable(account_category_domain_table_name);

			// Structure for loading and caching information from the provided Chart of Accounts
			LoadingCache<String, Optional<Map<String, Object>>> lookupChartOfAccounts = getLookupChartOfAccounts(context.getValidatedDataRepository(), 
					category_domain_table, subcategory_domain_table, coa);
			
			// Structure for loading and caching information from the provided Taxpayers registry
			LoadingCache<String, Optional<Map<String, Object>>> lookupTaxpayers = getLookupTaxpayers(context.getTaxpayerRepository());
			
			final Optional<Map<String,Object>> declarantInformation = lookupTaxpayers.getUnchecked(taxPayerId);
			
			// Additional domain table included in customized General Ledger template
			final Map<String,DomainTable.MultiLingualMap> customDomainTables = ETLContext.getDomainTablesInTemplate(gl_template, context.getDomainTableRepository());
			// Removes from 'customDomainTables' the standard fields (otherwise we will be including them in redundancy)
			customDomainTables.remove(ledgerDebitCredit);
 

			LoadDataStrategy loader = context.getLoadDataStrategy();
			loader.start();
			
			final LongAdder countRecordsOverall = new LongAdder();
			final AtomicLong countRecordsInGeneralLedger = new AtomicLong();
			final AtomicLong countRecordsInAccountingFlows = new AtomicLong();
			
	        final OffsetDateTime timestamp = context.getDocumentUploaded().getTimestamp();

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
			
			// Computes monthly balance sheets
			final MonthlyBalanceSheetProcessor balanceSheetProc = new MonthlyBalanceSheetProcessor(context, ob, timestamp);
			balanceSheetProc.setDocumentUploadedForGeneralLedger(gl);
			balanceSheetProc.setCountRecordsOverall(countRecordsOverall);
			balanceSheetProc.setDeclarantInformation(declarantInformation);
			balanceSheetProc.setLookupChartOfAccounts(lookupChartOfAccounts);
			balanceSheetProc.setPeriodicity(periodicity);
			
			// Computes statement of comprehensive income
			final ComputedStatementIncomeProcessor computedStatementIncome = new ComputedStatementIncomeProcessor(context, account_standard, timestamp);
			computedStatementIncome.setLookupChartOfAccounts(lookupChartOfAccounts);
			computedStatementIncome.setCountRecordsOverall(countRecordsOverall);
			computedStatementIncome.setDocumentUploadedForGeneralLedger(gl);
			computedStatementIncome.setDeclarantInformation(declarantInformation);

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
				
				gl_data.forEach(record->{
					
					final OffsetDateTime date = ValidationContext.toOffsetDateTime(record.get(ledgerDate));
					
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
					
					Number amount = ValidationContext.toNumber(record.get(ledgerAmount));
					String debitCredit = ValidationContext.toString(record.get(ledgerDebitCredit));
					if (debitCredit==null)
						debitCredit = "";
					boolean is_debit = debitCredit.equalsIgnoreCase("D");
					
					// Computes Monthly Balance Sheet
					
					BalanceSheet balanceSheet = balanceSheetProc.computeEntry(date, accountCode, amount, is_debit);
					
					// Computes Accounting Flow
					
					if (amount!=null && Math.abs(amount.doubleValue())>EPSILON)
						accountingFlowProc.computeEntry(date, accountCode, amount, is_debit);
					
					// Computes Statement of Incomes
					
					computedStatementIncome.computeEntry(date, accountCode, amount, is_debit);

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
					normalizedRecord_GL.put(PublishedDataFieldNames.ETL_TIMESTAMP.getFieldName(), timestamp);
					normalizedRecord_GL.put(publishedTimestamp, date);
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

					// Includes data about custom domain tables (possibly in multiple languages)
					ETLContext.denormalizeDomainTables(record, normalizedRecord_GL, customDomainTables);

					loader.add(new IndexRequest(INDEX_PUBLISHED_GENERAL_LEDGER)
						.id(rowId_GL)
						.source(normalizedRecord_GL));
					countRecordsOverall.increment();

				}); // LOOP over all entries in General Ledger
				
				// After processing all the General Ledger, let's fill the Monthly Balance Sheet for all accounts
				balanceSheetProc.finish();
				
				// After processing all the General Ledger, let's finish computing the Daily Accounting Flows
				accountingFlowProc.finish();
				
				// After processing all the General Ledger, let's finish computing the Statement of Income
				computedStatementIncome.finish();
				
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
		normalizedRecord_DAF.put(PublishedDataFieldNames.ETL_TIMESTAMP.getFieldName(), timestamp);
		normalizedRecord_DAF.put(publishedTimestamp, flow.getDate().atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime());
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
	 * Removes from the map any 'control fields' (e.g: 'timestamp', 'FILE_ID', etc.)
	 */
	public static Map<String, Object> removeControlFields(Map<String, Object> map) {
		return map.entrySet().stream().filter(e->!TRACKING_FIELDS.contains(e.getKey()))
			.collect(Collectors.toMap(
				/*keyMapper*/Map.Entry::getKey, 
				/*valueMapper*/Map.Entry::getValue, 
				/*mergeFunction*/(a,b)->a));
	}
}
