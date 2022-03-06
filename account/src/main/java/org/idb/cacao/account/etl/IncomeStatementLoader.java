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
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.elasticsearch.action.index.IndexRequest;
import org.idb.cacao.account.archetypes.AccountBuiltInDomainTables;
import org.idb.cacao.account.elements.StatementComprehensiveIncome;
import org.idb.cacao.account.etl.AccountingLoader.AccountingFieldNames;
import org.idb.cacao.api.DocumentSituation;
import org.idb.cacao.api.DomainLanguage;
import org.idb.cacao.api.ETLContext;
import org.idb.cacao.api.Periodicity;
import org.idb.cacao.api.PublishedDataFieldNames;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.templates.DomainEntry;
import org.idb.cacao.api.templates.DomainTable;
import org.idb.cacao.api.templates.DomainTable.MultiLingualMap;
import org.idb.cacao.api.ETLContext.LoadDataStrategy;
import org.idb.cacao.api.utils.IndexNamesUtils;

import com.google.common.cache.LoadingCache;

/**
 * Projects Income Statement data into Database after validation phases. Performs denormalization
 * of data.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class IncomeStatementLoader {

	private static final Logger log = Logger.getLogger(IncomeStatementLoader.class.getName());

	/**
	 * Index name for published (denormalized) data regarding the declared Statement of Incomes.<BR>
	 * There is one record for each year and each Statement of Income entry (according to domain table).
	 */
	public static final String INDEX_PUBLISHED_DECLARED_STATEMENT_INCOME = IndexNamesUtils.formatIndexNameForPublishedData("Accounting Declared Statement Income");

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
	 * The field name for published data regarding 'Statement Order Number'
	 */
	private static final String statementNumber = IndexNamesUtils.formatFieldName("StatementNumber");

	/**
	 * The field name for published data regarding 'Statement Amount'
	 */
	private static final String amount = IndexNamesUtils.formatFieldName("Amount");

	/**
	 * The field name for published data regarding 'Statement Amount Relative to Revenue Net'
	 */
	private static final String amountRelative = IndexNamesUtils.formatFieldName("AmountRelative");

	/**
	 * Performs the Extract/Transform/Load operations with available data
	 */
	public static boolean performETL(ETLContext context) {
		
		try {
			
			String taxPayerId = context.getDocumentUploaded().getTaxPayerId();
			Integer taxPeriodNumber = context.getDocumentUploaded().getTaxPeriodNumber();

			// Structure for loading and caching information from the provided Taxpayers registry
			LoadingCache<String, Optional<Map<String, Object>>> lookupTaxpayers = AccountingLoader.getLookupTaxpayers(context.getTaxpayerRepository());

			final Optional<Map<String,Object>> declarantInformation = lookupTaxpayers.getUnchecked(taxPayerId);

			LoadDataStrategy loader = context.getLoadDataStrategy();
			loader.start();
			
			final AtomicLong countRecordsOverall = new AtomicLong();

	        final OffsetDateTime timestamp = context.getDocumentUploaded().getTimestamp();

			// Search for the validated income statement related to the matching template
			Stream<Map<String, Object>> data = context.getValidatedDataRepository().getValidatedData(context.getDocumentTemplate().getName(), context.getDocumentTemplate().getVersion(), context.getDocumentUploaded().getFileId(),
					/*sortBy*/Optional.empty(),
					/*sortOrder*/Optional.empty());
			
			if (data==null)
				data = Collections.<Map<String, Object>>emptySet().stream();

			// Deletes previous published data
			try {
				context.getLoadDataStrategy().delete(INDEX_PUBLISHED_DECLARED_STATEMENT_INCOME, taxPayerId, taxPeriodNumber);
			} catch (Exception e) {
				log.log(Level.SEVERE, "Error while deleting previous published data at "+INDEX_PUBLISHED_DECLARED_STATEMENT_INCOME+" regarding "+taxPayerId+" and period "+taxPeriodNumber, e);
			}

			// Maps entries of Statement of Comprehensive Income to multi-language descriptions
			Map<StatementComprehensiveIncome, Map<DomainLanguage, DomainEntry>> mapDomainTexts = new HashMap<>();
			
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
			}

			final int year = Periodicity.getYear(taxPeriodNumber);
			final OffsetDateTime timestampForView = LocalDate.of(year, 12, 1).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();

			// Start the denormalization process
			
			boolean success = true;
			try {
				
				Map<StatementComprehensiveIncome, Number> mapValues = new HashMap<>();
				
				// Pass through data in order to collect all the entries for Income statement
								
				data.forEach(record->{
					
					for (StatementComprehensiveIncome stmt: StatementComprehensiveIncome.values()) {
						String fieldName = stmt.name().toLowerCase();
						Object fieldValue = record.get(fieldName);
						if (fieldValue==null)
							continue;
						Number fieldNumericValue = ValidationContext.toNumber(fieldValue);
						if (fieldNumericValue==null)
							continue;
						if (stmt.isAbsoluteValue()) {
							fieldNumericValue = Math.abs(fieldNumericValue.doubleValue());
						}
						mapValues.merge(stmt, fieldNumericValue, (a,b)->a.doubleValue()+b.doubleValue());
					}
					
				}); // LOOP over all entries in Income Statement

				// Publish denormalized GENERAL LEDGER record
				
				// The revenue net is also used as denominator for calculating relative values
				double revenue_net =
						mapValues.getOrDefault(StatementComprehensiveIncome.REVENUE_NET, 0.0).doubleValue();

				// Start with the built-in statement entries
				
				for (StatementComprehensiveIncome t: StatementComprehensiveIncome.values()) {

					double value = mapValues.getOrDefault(t, 0.0).doubleValue();
					
					Map<DomainLanguage, DomainEntry> multiLanguageDomainEntry = mapDomainTexts.get(t);
					
					String rowId_SCI = String.format("%s.%d.%014d", taxPayerId, taxPeriodNumber, countRecordsOverall.incrementAndGet());
					Map<String,Object> normalizedRecord_SCI = new HashMap<>();
					normalizedRecord_SCI.put(PublishedDataFieldNames.ETL_TIMESTAMP.getFieldName(), timestamp);
					normalizedRecord_SCI.put(publishedTimestamp, timestampForView);
					normalizedRecord_SCI.put(lineNumber, countRecordsOverall.longValue());
					normalizedRecord_SCI.put(publishedTaxpayerId, taxPayerId);
					normalizedRecord_SCI.put(publishedtaxPeriodNumber, taxPeriodNumber);
					normalizedRecord_SCI.put(publishedTemplateName, context.getDocumentTemplate().getName());
					normalizedRecord_SCI.put(publishedTemplateVersion, context.getDocumentTemplate().getVersion());
					normalizedRecord_SCI.put(publishedYear, year);
					normalizedRecord_SCI.put(entryCode, t.name());
					normalizedRecord_SCI.put(amount, value);
					if (revenue_net!=0 && !StatementComprehensiveIncome.REVENUE_NET.equals(t)) {
						normalizedRecord_SCI.put(amountRelative, value * 100.0 / revenue_net);
					}
					if (declarantInformation.isPresent())
						normalizedRecord_SCI.putAll(declarantInformation.get());
					normalizedRecord_SCI.put(statementNumber, String.format("%02d", t.ordinal()+1));
					if (multiLanguageDomainEntry!=null && !multiLanguageDomainEntry.isEmpty())
						ETLContext.denormalizeDomainEntryNames(multiLanguageDomainEntry, statementEntry, normalizedRecord_SCI);
					loader.add(new IndexRequest(INDEX_PUBLISHED_DECLARED_STATEMENT_INCOME)
						.id(rowId_SCI)
						.source(normalizedRecord_SCI));

				}
				
			}
			finally {
				data.close();
				try {
					loader.commit();
				}
				catch (Exception e) {
					log.log(Level.SEVERE, "Error while storing "+countRecordsOverall.longValue()+" rows of denormalized data for taxpayer id "+taxPayerId+" period "+taxPeriodNumber, e);
					success = false;
				}
				loader.close();
			}

			if (success && !context.hasOutcomeSituations()) {
				context.setOutcomeSituation(context.getDocumentUploaded(), DocumentSituation.PROCESSED);
				ETLContext.markReplacedDocuments(context.getDocumentUploaded(), context);
			}
			
			return success;

		}
		catch (Exception e) {
			String fileId = (context==null || context.getDocumentUploaded()==null) ? null : context.getDocumentUploaded().getFileId();
			log.log(Level.SEVERE, "Error while performing ETL regarding file "+fileId, e);
			return false;
		}

	}
	
}
