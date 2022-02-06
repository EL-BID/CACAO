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
import org.idb.cacao.account.archetypes.ShareholdersArchetype;
import org.idb.cacao.account.etl.AccountingLoader.AccountingFieldNames;
import org.idb.cacao.api.DocumentSituation;
import org.idb.cacao.api.ETLContext;
import org.idb.cacao.api.Periodicity;
import org.idb.cacao.api.PublishedDataFieldNames;
import org.idb.cacao.api.ValidatedDataFieldNames;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.ETLContext.LoadDataStrategy;
import org.idb.cacao.api.templates.DomainTable;
import org.idb.cacao.api.templates.DomainTable.MultiLingualMap;
import org.idb.cacao.api.utils.IndexNamesUtils;

import com.google.common.cache.LoadingCache;

/**
 * Projects Shareholders information into Database after validation phases. Performs denormalization
 * of data.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class ShareholdersLoader {

	private static final Logger log = Logger.getLogger(ShareholdersLoader.class.getName());

	/**
	 * Index name for published (denormalized) data regarding the Shareholders information.<BR>
	 * There is one record for each year and each Shareholder entry.
	 */
	public static final String INDEX_PUBLISHED_SHAREHOLDERS = IndexNamesUtils.formatIndexNameForPublishedData("Accounting Shareholders");

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
	 * The field name for published data regarding share type
	 */
	private static final String publishedShareType = IndexNamesUtils.formatFieldName(ShareholdersArchetype.FIELDS_NAMES.ShareType.name());

	/**
	 * The field name for published data regarding Shareholder Identification number
	 */
	private static final String publishedShareholderId = IndexNamesUtils.formatFieldName(ShareholdersArchetype.FIELDS_NAMES.ShareholderId.name());

	/**
	 * The field name for published data regarding Shareholder Name
	 */
	private static final String publishedShareholderName = IndexNamesUtils.formatFieldName(ShareholdersArchetype.FIELDS_NAMES.ShareholderName.name());

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

			// Search for the validated shareholders list related to the matching template
			Stream<Map<String, Object>> data = context.getValidatedDataRepository().getValidatedData(context.getDocumentTemplate().getName(), context.getDocumentTemplate().getVersion(), context.getDocumentUploaded().getFileId(),
					/*sortBy*/Optional.empty(),
					/*sortOrder*/Optional.empty());
			
			if (data==null)
				data = Collections.<Map<String, Object>>emptySet().stream();

			// Deletes previous published data
			try {
				context.getLoadDataStrategy().delete(INDEX_PUBLISHED_SHAREHOLDERS, taxPayerId, taxPeriodNumber);
			} catch (Throwable ex) {
				log.log(Level.SEVERE, "Error while deleting previous published data at "+INDEX_PUBLISHED_SHAREHOLDERS+" regarding "+taxPayerId+" and period "+taxPeriodNumber, ex);
			}

			// Use the domain table (either the built-in or the one configured at application)
			DomainTable share_type_dt;
			List<DomainTable> custom_share_type_dt = context.getDomainTableRepository().findByName(AccountBuiltInDomainTables.SHARE_TYPE.getName());
			if (custom_share_type_dt.size()==1)
				share_type_dt = custom_share_type_dt.get(0);
			else if (custom_share_type_dt.size()>1)
				share_type_dt = custom_share_type_dt.stream().sorted(Comparator.comparing(DomainTable::getDomainTableCreateTime).reversed()).findFirst().get();
			else
				share_type_dt = AccountBuiltInDomainTables.SHARE_TYPE;

			MultiLingualMap share_type_mlmap = share_type_dt.toMultiLingualMap();

			
			final int year = Periodicity.getYear(taxPeriodNumber);
			final OffsetDateTime timestampForView = LocalDate.of(year, 12, 1).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();

			// Start the denormalization process
			
			boolean success = true;
			try {
				
				// Pass through data in order to collect all the entries for Shareholders
				
				data.forEach(record->{
					
					// Publish denormalized SHAREHOLDERS record
					
					String rowId_SH = String.format("%s.%d.%014d", taxPayerId, taxPeriodNumber, countRecordsOverall.incrementAndGet());
					Map<String,Object> normalizedRecord_SH = new HashMap<>(record);
					for (ValidatedDataFieldNames vfieldName: ValidatedDataFieldNames.values()) {
						// Published data has all fields in lower case
						Object value = normalizedRecord_SH.remove(vfieldName.name());
						if (value!=null) {
							normalizedRecord_SH.put(IndexNamesUtils.formatFieldName(vfieldName.name()), value);
						}
					}

					normalizedRecord_SH.remove("tax_payer_id"); // ambiguous field with 'taxpayer_id'
					normalizedRecord_SH.put(PublishedDataFieldNames.ETL_TIMESTAMP.getFieldName(), timestamp);
					normalizedRecord_SH.put(publishedTimestamp, timestampForView);
					normalizedRecord_SH.put(lineNumber, countRecordsOverall.longValue());
					normalizedRecord_SH.put(publishedTaxpayerId, taxPayerId);
					normalizedRecord_SH.put(publishedtaxPeriodNumber, taxPeriodNumber);
					normalizedRecord_SH.put(publishedTemplateName, context.getDocumentTemplate().getName());
					normalizedRecord_SH.put(publishedTemplateVersion, context.getDocumentTemplate().getVersion());
					normalizedRecord_SH.put(publishedYear, year);

					if (declarantInformation.isPresent())
						normalizedRecord_SH.putAll(declarantInformation.get());

					// Includes multi language names for the share type
					String shareType = ValidationContext.toString(record.get(publishedShareType));
					if (shareType!=null)
						ETLContext.denormalizeDomainEntryNames(share_type_mlmap.get(shareType), publishedShareType, normalizedRecord_SH);

					// If the shareholder is identified, includes information according to the taxpayers registry
					String shareholderId = ValidationContext.toString(record.get(publishedShareholderId));
					if (shareholderId!=null) {
						final Optional<Map<String,Object>> shareholderInformation = lookupTaxpayers.getUnchecked(shareholderId);
						if (shareholderInformation.isPresent() && !shareholderInformation.get().isEmpty()) {
							for (Map.Entry<String,Object> entry: shareholderInformation.get().entrySet()) {
								String fieldName = entry.getKey();
								if ("taxpayer_id".equals(fieldName))
									continue; // we already got this information in 'shareholder_id' field
								if ("taxpayer_name".equals(fieldName)) {
									String shareholderName = ValidationContext.toString(record.get(publishedShareholderName));
									if (shareholderName!=null && shareholderName.trim().length()>0)
										continue; // we already got this information in 'shareholder_name' field
									normalizedRecord_SH.put(publishedShareholderName, entry.getValue());
									continue;
								}
								// All the other fields will be stored as 'shareholder_XXXXX'
								if (fieldName.startsWith("taxpayer")) {
									normalizedRecord_SH.put(fieldName.replace("taxpayer", "shareholder"), entry.getValue());
								}
							} // LOOP over fields of shareholder according to the taxpayers registry
						}
					}
					
					loader.add(new IndexRequest(INDEX_PUBLISHED_SHAREHOLDERS)
							.id(rowId_SH)
							.source(normalizedRecord_SH));

				});
				
			}
			finally {
				data.close();
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
				context.setOutcomeSituation(context.getDocumentUploaded(), DocumentSituation.PROCESSED);
				ETLContext.markReplacedDocuments(context.getDocumentUploaded(), context);
			}
			
			return success;

		}
		catch (Throwable ex) {
			String fileId = (context==null || context.getDocumentUploaded()==null) ? null : context.getDocumentUploaded().getFileId();
			log.log(Level.SEVERE, "Error while performing ETL regarding file "+fileId, ex);
			return false;
		}

	}

}
