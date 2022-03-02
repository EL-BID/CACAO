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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import org.elasticsearch.action.index.IndexRequest;
import org.idb.cacao.account.archetypes.ChartOfAccountsArchetype;
import org.idb.cacao.account.archetypes.GeneralLedgerArchetype;
import org.idb.cacao.account.elements.AccountStandard;
import org.idb.cacao.account.elements.AccountSubcategory;
import org.idb.cacao.account.etl.AccountingLoader.AccountingFieldNames;
import org.idb.cacao.api.DocumentUploaded;
import org.idb.cacao.api.ETLContext;
import org.idb.cacao.api.PublishedDataFieldNames;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.ETLContext.LoadDataStrategy;
import org.idb.cacao.api.utils.IndexNamesUtils;

import com.google.common.cache.LoadingCache;

/**
 * Aggregates information about customers and suppliers while parsing the general ledger entries.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class CustomersSuppliersProcessor {

	/**
	 * The field name for date/time of published data
	 */
	private static final String publishedTimestamp = IndexNamesUtils.formatFieldName(PublishedDataFieldNames.TIMESTAMP.name());

	/**
	 * The field name for line numbering for the same published data contents
	 */
	private static final String lineNumber = IndexNamesUtils.formatFieldName(PublishedDataFieldNames.LINE.name());

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
	 * The field name for indication of Year for each Monthly Aggregation
	 */
	private static final String publishedYear = IndexNamesUtils.formatFieldName(AccountingFieldNames.Year.name());

	/**
	 * The field name for name indication of Month for each Monthly Aggregation
	 */
	private static final String publishedMonth = IndexNamesUtils.formatFieldName(AccountingFieldNames.Month.name());

	/**
	 * The field name for number indication of Month for each Monthly Aggregation
	 */
	private static final String publishedMonthNumber = IndexNamesUtils.formatFieldName(AccountingFieldNames.MonthNumber.name());

	/**
	 * The field name for customer/supplier ID in published data
	 */
	private static final String publishedCustomerSupplierId = IndexNamesUtils.formatFieldName(GeneralLedgerArchetype.FIELDS_NAMES.CustomerSupplierId.name());

	/**
	 * The field name for published data regarding amount of debit values
	 */
	private static final String publishedDebits = IndexNamesUtils.formatFieldName(AccountingFieldNames.AmountDebits.name());

	/**
	 * The field name for published data regarding amount of credit values
	 */
	private static final String publishedCredits = IndexNamesUtils.formatFieldName(AccountingFieldNames.AmountCredits.name());

	/**
	 * The number of book entries for each Monthly Aggregation
	 */
	private static final String bookEntries = IndexNamesUtils.formatFieldName(AccountingFieldNames.BookEntries.name());

	/**
	 * The field name for published data regarding 'Amount' (some value, may be debit or may be credit)
	 */
	private static final String ledgerAmount = IndexNamesUtils.formatFieldName(GeneralLedgerArchetype.FIELDS_NAMES.Amount.name());

	/**
	 * Object used for retrieving additional information about accounts according to the taxpayer's Chart of Accounts
	 */
	private final LoadingCache<String, Optional<Map<String, Object>>> lookupChartOfAccounts;
	
	/**
	 * Object for loading and caching information from the provided Taxpayers registry
	 */
	private final LoadingCache<String, Optional<Map<String, Object>>> lookupTaxpayers;
	
	/**
	 * Customer/supplier indication according to the sub-category code
	 */
	private final Map<String,CustomerSupplierIndication> indicationPerSubCategory;
	
	/**
	 * Customer/supplier indication according to the accound code
	 */
	private final Map<String,CustomerSupplierIndication> indicationPerAccount;
	
	/**
	 * Ignores differences lesser than half of a cent	
	 */
	private static final double EPSILON = 0.005;		

	private final String taxPayerId;
	
	private final Integer taxPeriodNumber;
	
	private final OffsetDateTime timestamp;

	private Optional<Map<String,Object>> declarantInformation;

	/**
	 * Stores here the last month (a number in the format yyyy-mm) of the last parsed entry from the Journal
	 */
	private final AtomicInteger previousMonth;
	
	/**
	 * Aggregates information about customers
	 */
	private final Map<String, CustomerSupplierAggregation> customersAggregations;
	
	private final AtomicLong countRecordsInCustomers;
	
	/**
	 * For each customer Id maps the corresponding customer name according to accounting entries
	 */
	private final Map<String, String> customersNames;

	/**
	 * Aggregates information about suppliers
	 */
	private final Map<String, CustomerSupplierAggregation> suppliersAggregations;

	private final AtomicLong countRecordsInSuppliers;

	/**
	 * For each supplier Id maps the corresponding supplier name according to accounting entries
	 */
	private final Map<String, String> suppliersNames;

	private final LoadDataStrategy loader;

	private DocumentUploaded gl;

	private LongAdder countRecordsOverall;

	public CustomersSuppliersProcessor(final ETLContext context,
			LoadingCache<String, Optional<Map<String, Object>>> lookupChartOfAccounts,
			LoadingCache<String, Optional<Map<String, Object>>> lookupTaxpayers,
			final AccountStandard accountStandard,
			final OffsetDateTime timestamp) {
		
		this.taxPayerId = context.getDocumentUploaded().getTaxPayerId();
		this.taxPeriodNumber = context.getDocumentUploaded().getTaxPeriodNumber();
		this.timestamp = timestamp;
		this.previousMonth = new AtomicInteger(0);
		this.declarantInformation = Optional.empty();
		this.lookupChartOfAccounts = lookupChartOfAccounts;
		this.lookupTaxpayers = lookupTaxpayers;
		this.indicationPerSubCategory = getCustomerSupplierIndication(accountStandard);
		this.indicationPerAccount = new TreeMap<>(String.CASE_INSENSITIVE_ORDER); // will be populated during journal processing
		this.customersAggregations = new TreeMap<>(String.CASE_INSENSITIVE_ORDER); // will be populated during journal processing and reset after each month
		this.customersNames = new TreeMap<>(String.CASE_INSENSITIVE_ORDER); // will be populated during journal processing and reset after each month
		this.suppliersAggregations = new TreeMap<>(String.CASE_INSENSITIVE_ORDER); // will be populated during journal processing and reset after each month
		this.suppliersNames = new TreeMap<>(String.CASE_INSENSITIVE_ORDER); // will be populated during journal processing and reset after each month
		this.countRecordsInCustomers = new AtomicLong();
		this.countRecordsInSuppliers = new AtomicLong();
		this.loader = context.getLoadDataStrategy();
	}

	public void setDocumentUploadedForGeneralLedger(DocumentUploaded gl) {
		this.gl = gl;
	}

	public void setDeclarantInformation(Optional<Map<String, Object>> declarantInformation) {
		this.declarantInformation = declarantInformation;
	}

	public void setCountRecordsOverall(LongAdder countRecordsOverall) {
		this.countRecordsOverall = countRecordsOverall;
	}

	/**
	 * Computes a bookeeping entry. It's expected to receive all the entries ordered by Date
	 * @param date Date of this record
	 * @param accountCode Account code
	 * @param customerSupplierId Customer/supplier identification informed in accounting
	 * @param customerSupplierName Customer/supplier name informed in accounting
	 * @param amount Amount
	 * @param isDebit Indication whether the account was debited (otherwise it was credited)
	 */
	public void computeEntry(OffsetDateTime date, String accountCode, String customerSupplierId, String customerSupplierName, Number amount, boolean isDebit) {
		
		if (accountCode==null || accountCode.trim().length()==0 || amount==null || date==null || Math.abs(amount.doubleValue())<EPSILON)
			return;
		
		CustomerSupplierIndication indication = indicationPerAccount.computeIfAbsent(accountCode, 
			code->{
				Optional<Map<String, Object>> accountInformation = lookupChartOfAccounts.getUnchecked(code);
				if (!accountInformation.isPresent())
					return CustomerSupplierIndication.NONE;
				String subcategory = ValidationContext.toString(accountInformation.get().get(IndexNamesUtils.formatFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountSubcategory.name())));
				if (subcategory==null)
					return CustomerSupplierIndication.NONE;
				return indicationPerSubCategory.getOrDefault(subcategory, CustomerSupplierIndication.NONE);
			});
		
		if (CustomerSupplierIndication.NONE.equals(indication))
			return;
		
		final int year_month = ( date.getYear() * 100 + date.getMonthValue() );
		final boolean changed_month = year_month!=0 && previousMonth.get()!=0 && previousMonth.get()!=year_month;
		final int previous_year_month = previousMonth.get();
		if (date!=null && (changed_month || previousMonth.get()==0)) {
			previousMonth.set(year_month);
		}

		if (changed_month) {
			// If the month has changed, let's fill the Monthly aggregations for all customers/suppliers
			addRecordToMonthlyAggregation(previous_year_month);
		}

		// Performs aggregations according to the customer/supplier indication
		
		switch (indication) {
		case CUSTOMER_ADDITIVE_FOR_DEBITS:
			customersAggregations.computeIfAbsent(customerSupplierId, k->new CustomerSupplierAggregation()).compute(amount, isDebit, /*debitIsAdditive*/true);
			if (customerSupplierName!=null && customerSupplierName.trim().length()>0)
				customersNames.computeIfAbsent(customerSupplierId, k->customerSupplierName);
			break;
		case CUSTOMER_ADDITIVE_FOR_CREDITS:
			customersAggregations.computeIfAbsent(customerSupplierId, k->new CustomerSupplierAggregation()).compute(amount, isDebit, /*debitIsAdditive*/false);
			if (customerSupplierName!=null && customerSupplierName.trim().length()>0)
				customersNames.computeIfAbsent(customerSupplierId, k->customerSupplierName);
			break;
		case SUPPLIER_ADDITIVE_FOR_DEBITS:
			suppliersAggregations.computeIfAbsent(customerSupplierId, k->new CustomerSupplierAggregation()).compute(amount, isDebit, /*debitIsAdditive*/true);
			if (customerSupplierName!=null && customerSupplierName.trim().length()>0)
				suppliersNames.computeIfAbsent(customerSupplierId, k->customerSupplierName);
			break;
		case SUPPLIER_ADDITIVE_FOR_CREDITS:
			suppliersAggregations.computeIfAbsent(customerSupplierId, k->new CustomerSupplierAggregation()).compute(amount, isDebit, /*debitIsAdditive*/false);
			if (customerSupplierName!=null && customerSupplierName.trim().length()>0)
				suppliersNames.computeIfAbsent(customerSupplierId, k->customerSupplierName);
			break;
		default:
			// Nothing to aggregate under this indication
		}
		
	}
	
	/**
	 * Should be called after iterating all of the book entries
	 */
	public void finish() {
		
		final int previous_year_month = previousMonth.get();
		if (previous_year_month!=0) {
			addRecordToMonthlyAggregation(previous_year_month);
		}
	}
	
	/**
	 * Feeds information about Monthly Aggregation
	 * @param yearMonth Year+Month of aggregation
	 */
	private void addRecordToMonthlyAggregation(final int yearMonth) {
		
		final int year = yearMonth/100;
		final int monthNumber = yearMonth%100;
		final String month = Month.of(monthNumber).getDisplayName(
	        TextStyle.SHORT, 
	        Locale.getDefault()
	    );
		
		final int PASS_CUSTOMER = 0; // PASS_SUPPLIER = 1
		for (int pass=0; pass<2; pass++) {
			
			final Map<String,CustomerSupplierAggregation> aggregations = (pass==PASS_CUSTOMER) ? customersAggregations : suppliersAggregations;
			final String index_name = (pass==PASS_CUSTOMER) ? AccountingLoader.INDEX_PUBLISHED_CUSTOMERS : AccountingLoader.INDEX_PUBLISHED_SUPPLIERS;
			final AtomicLong count_records = (pass==PASS_CUSTOMER) ? countRecordsInCustomers : countRecordsInSuppliers;
			final Map<String, String> names = (pass==PASS_CUSTOMER) ? customersNames : suppliersNames;
			final String fieldPrefix = (pass==PASS_CUSTOMER) ? "customer" : "supplier";
		
			for (Map.Entry<String,CustomerSupplierAggregation> entryAg: aggregations.entrySet()) {
				String customerSupplierId = entryAg.getKey();
				CustomerSupplierAggregation aggregation = entryAg.getValue();
				if (aggregation.isEmpty())
					continue;
				String rowId_CS = String.format("%s.%d.%014d", taxPayerId, taxPeriodNumber, count_records.incrementAndGet());
				Map<String,Object> normalizedRecord_CS = new HashMap<>();
				normalizedRecord_CS.put(PublishedDataFieldNames.ETL_TIMESTAMP.getFieldName(), timestamp);
				normalizedRecord_CS.put(publishedTimestamp, LocalDate.of(year, monthNumber, 1));
				normalizedRecord_CS.put(lineNumber, count_records.longValue());
				normalizedRecord_CS.put(publishedTaxpayerId, taxPayerId);
				normalizedRecord_CS.put(publishedtaxPeriodNumber, taxPeriodNumber);
				if (gl!=null) {
					normalizedRecord_CS.put(publishedTemplateName, gl.getTemplateName());
					normalizedRecord_CS.put(publishedTemplateVersion, gl.getTemplateVersion());
				}
				normalizedRecord_CS.put(publishedYear, year);
				normalizedRecord_CS.put(publishedMonth, month);
				normalizedRecord_CS.put(publishedMonthNumber, monthNumber);
				
				normalizedRecord_CS.put(publishedCustomerSupplierId.replace("customer_supplier", fieldPrefix), customerSupplierId);
				Optional<Map<String, Object>> customerSupplierInformation = lookupTaxpayers.getUnchecked(customerSupplierId);
				if (customerSupplierInformation.isPresent() && !customerSupplierInformation.get().isEmpty()) {
					for (Map.Entry<String,Object> entry: customerSupplierInformation.get().entrySet()) {
						String fieldName = entry.getKey();
						if ("taxpayer_id".equals(fieldName))
							continue; // we already got this information in 'customer_id' or 'supplier_id' field
						if ("taxpayer_name".equals(fieldName)) {
							// The informed name takes precedence over taxpayers registry
							String customerSupplierName = names.get(customerSupplierId);
							if (customerSupplierName!=null && customerSupplierName.trim().length()>0) {
								normalizedRecord_CS.put(fieldName.replace("taxpayer", fieldPrefix), customerSupplierName);								
							}
							else {
								normalizedRecord_CS.put(fieldName.replace("taxpayer", fieldPrefix), entry.getValue());
							}
							continue;
						}
						// All the other fields will be stored as 'customer_XXXXX' or 'supplier_XXXXX'
						if (fieldName.startsWith("taxpayer")) {
							normalizedRecord_CS.put(fieldName.replace("taxpayer", fieldPrefix), entry.getValue());
						}
					} // LOOP over fields of customer/supplier according to the taxpayers registry
				}
				
				normalizedRecord_CS.put(publishedDebits, aggregation.debits);
				normalizedRecord_CS.put(publishedCredits, aggregation.credits);
				normalizedRecord_CS.put(bookEntries, aggregation.entries);
				normalizedRecord_CS.put(ledgerAmount, aggregation.amount);
				if (declarantInformation.isPresent())
					normalizedRecord_CS.putAll(declarantInformation.get());
				
				loader.add(new IndexRequest(index_name)
					.id(rowId_CS)
					.source(normalizedRecord_CS));
				if (countRecordsOverall!=null)
					countRecordsOverall.increment();	
				aggregation.reset();
			} // LOOP each aggregation (each customer or each supplier)
			
			names.clear();
			
		} // TWO PASS: one for CUSTOMERS, other for SUPPLIERS
		
	}

	/**
	 * Enumerates the possible indications regarding customer/suppliers for each account according to the account sub-category
	 */
	public static enum CustomerSupplierIndication {
		NONE,
		CUSTOMER_ADDITIVE_FOR_DEBITS,
		CUSTOMER_ADDITIVE_FOR_CREDITS,
		SUPPLIER_ADDITIVE_FOR_DEBITS,
		SUPPLIER_ADDITIVE_FOR_CREDITS;		
	}
	
	/**
	 * Maps sub-category codes (according to some account standard) to the corresponding Customer/Supplier indication. The sub-categories
	 * not included in this map should be considered the same as 'NONE' indication.
	 */
	public static Map<String,CustomerSupplierIndication> getCustomerSupplierIndication(AccountStandard accountStandard) {
		Map<String,CustomerSupplierIndication> indicationsBySubCategory = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		
		// Customers
		indicationsBySubCategory.put(AccountSubcategory.REVENUE_NET.getNumber(accountStandard), CustomerSupplierIndication.CUSTOMER_ADDITIVE_FOR_CREDITS);
		
		// Suppliers
		indicationsBySubCategory.put(AccountSubcategory.ASSET_INVENTORY.getNumber(accountStandard), CustomerSupplierIndication.SUPPLIER_ADDITIVE_FOR_DEBITS);
		
		return indicationsBySubCategory;
	}
	
	/**
	 * Aggregated information about customer/supplier
	 */
	private static class CustomerSupplierAggregation {
		double debits;
		double credits;
		double amount;
		long entries;
		void compute(Number amount, boolean isDebit, boolean debitIsAdditive) {
			if (debitIsAdditive==isDebit) {
				this.amount += amount.doubleValue();
			}
			else {
				this.amount -= amount.doubleValue();
			}
			if (isDebit) {
				this.debits += amount.doubleValue();
			}
			else {
				this.credits += amount.doubleValue();
			}
			entries++;
		}
		void reset() {
			debits = credits = amount = entries = 0;
		}
		boolean isEmpty() {
			return entries==0;
		}
	}
}
