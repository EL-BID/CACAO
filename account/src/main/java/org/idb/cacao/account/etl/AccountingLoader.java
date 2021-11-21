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

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
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
import org.idb.cacao.api.DocumentSituation;
import org.idb.cacao.api.DocumentUploaded;
import org.idb.cacao.api.ETLContext;
import org.idb.cacao.api.ETLContext.LoadDataStrategy;
import org.idb.cacao.api.ETLContext.TaxpayerRepository;
import org.idb.cacao.api.PublishedDataFieldNames;
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
	 * Index name for published (denormalized) data regarding General Ledger
	 */
	public static final String INDEX_PUBLISHED_GENERAL_LEDGER = IndexNamesUtils.formatIndexNameForPublishedData("General Ledger");
	
	/**
	 * Fields names for published (denormalized) views
	 */
	public static enum AccountingFieldNames {
		
		Balance,
		
		BalanceDebitCredit,
		
		AccountCategoryName,
		
		AccountSubcategoryName,
		
		AmountDebits,
		
		AmountCredits
		
	};
	
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
					&& !DocumentSituation.PENDING.equals(upload.getSituation()))
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
									DomainEntry entry = category_domain_table.get().getEntry(category);
									if (entry!=null) {
										accountInfo.get().put(IndexNamesUtils.formatFieldName(AccountingFieldNames.AccountCategoryName.name()), entry.getDescription());
									}
								}
							}
							if (accountInfo.isPresent() && subcategory_domain_table.isPresent()) {
								String subcategory = ValidationContext.toString(accountInfo.get().get(IndexNamesUtils.formatFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountSubcategory.name())));
								if (subcategory!=null) {
									DomainEntry entry = subcategory_domain_table.get().getEntry(subcategory);
									if (entry!=null) {
										accountInfo.get().put(IndexNamesUtils.formatFieldName(AccountingFieldNames.AccountSubcategoryName.name()), entry.getDescription());
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
			
	        final OffsetDateTime timestamp = context.getDocumentUploaded().getTimestamp();

			final String ledgerAccountCode = IndexNamesUtils.formatFieldName(GeneralLedgerArchetype.FIELDS_NAMES.AccountCode.name());
			final String ledgerDate = IndexNamesUtils.formatFieldName(GeneralLedgerArchetype.FIELDS_NAMES.Date.name());
			final String ledgerId = IndexNamesUtils.formatFieldName(GeneralLedgerArchetype.FIELDS_NAMES.EntryId.name());
			final String ledgerAmount = IndexNamesUtils.formatFieldName(GeneralLedgerArchetype.FIELDS_NAMES.Amount.name());
			final String ledgerDebitCredit = IndexNamesUtils.formatFieldName(GeneralLedgerArchetype.FIELDS_NAMES.DebitCredit.name());
			final String ledgerAmountDebits = IndexNamesUtils.formatFieldName(AccountingFieldNames.AmountDebits.name());
			final String ledgerAmountCredits = IndexNamesUtils.formatFieldName(AccountingFieldNames.AmountCredits.name());
			
			final String openingBalanceInitial = IndexNamesUtils.formatFieldName(OpeningBalanceArchetype.FIELDS_NAMES.InitialBalance.name());
			final String openingBalanceDC = IndexNamesUtils.formatFieldName(OpeningBalanceArchetype.FIELDS_NAMES.DebitCredit.name());
			
			final String publishedTimestamp = IndexNamesUtils.formatFieldName(PublishedDataFieldNames.TIMESTAMP.name());
			final String publishedTaxpayerId = IndexNamesUtils.formatFieldName(PublishedDataFieldNames.TAXPAYER_ID.name());
			final String publishedtaxPeriodNumber = IndexNamesUtils.formatFieldName(PublishedDataFieldNames.TAXPERIOD_NUMBER.name());
			final String publishedTemplateName = IndexNamesUtils.formatFieldName(PublishedDataFieldNames.TEMPLATE_NAME.name());
			final String publishedTemplateVersion = IndexNamesUtils.formatFieldName(PublishedDataFieldNames.TEMPLATE_VERSION.name());
			final String ledgerBalance = IndexNamesUtils.formatFieldName(AccountingFieldNames.Balance.name());
			final String ledgerBalanceDebitCredit = IndexNamesUtils.formatFieldName(AccountingFieldNames.BalanceDebitCredit.name());

			// Computes balance sheets while iterating over General Ledger entries
			final Map<String, BalanceSheet> mapBalanceSheets = new HashMap<>();

			final double epsilon = 0.005; // ignores differences lesser than half of a cent		

			// Search for the validated general ledger related to the matching template
			// Reads the validated general ledger in chronological order. For each day order by ledger entry ID.
			Stream<Map<String, Object>> gl_data = context.getValidatedDataRepository().getValidatedData(gl.getTemplateName(), gl.getTemplateVersion(), gl.getFileId(),
					/*sortBy*/Optional.of(new String[] {ledgerDate, ledgerId+".keyword" }),
					/*sortOrder*/Optional.of(SortOrder.ASC));
			
			if (gl_data==null)
				gl_data = Collections.<Map<String, Object>>emptySet().stream();
			
			boolean success = true;
			try {
				
				gl_data.forEach(record->{

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
					
					BalanceSheet balanceSheet =
					mapBalanceSheets.computeIfAbsent(accountCode, acc->{
						BalanceSheet balance = new BalanceSheet();
						Optional<Map<String, Object>> opening = lookupOpeningBalance.getUnchecked(acc);
						if (opening.isPresent()) {
							Number initialBalance = ValidationContext.toNumber(opening.get().get(openingBalanceInitial));
							String balanceDebitCredit = ValidationContext.toString(opening.get().get(openingBalanceDC));
							boolean initialIsDebit = balanceDebitCredit.equalsIgnoreCase("D");
							if (initialBalance!=null && Math.abs(initialBalance.doubleValue())>epsilon) {
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
						}
						return balance;
					});
					
					balanceSheet.computeEntry(amount, is_debit);

					// Publish denormalized GENERAL LEDGER record
					
					String rowId_GL = String.format("%s.%d.%014d", taxPayerId, taxPeriodNumber, countRecordsInGeneralLedger.incrementAndGet());
					Map<String,Object> normalizedRecord_GL = new HashMap<>(record);
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

				});
				
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
				context.setOutcomeSituation(gl, DocumentSituation.PROCESSED);
				context.setOutcomeSituation(ob, DocumentSituation.PROCESSED);
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
			return  ( initialValue + debits - credits ) > 0;
		}

		public int getCountEntries() {
			return countEntries;
		}

		public void setCountEntries(int countEntries) {
			this.countEntries = countEntries;
		}
				
	}
}
