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
package org.idb.cacao.etl.loader;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.elasticsearch.action.index.IndexRequest;
import org.idb.cacao.api.DocumentSituation;
import org.idb.cacao.api.DocumentUploaded;
import org.idb.cacao.api.ETLContext;
import org.idb.cacao.api.PublishedDataFieldNames;
import org.idb.cacao.api.ValidatedDataFieldNames;
import org.idb.cacao.api.ETLContext.LoadDataStrategy;
import org.idb.cacao.api.ETLContext.TaxpayerRepository;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.DomainTable;
import org.idb.cacao.api.templates.TemplateArchetype;
import org.idb.cacao.api.utils.IndexNamesUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * This is an implementation for publishing denormalized data for general purpose. It's not
 * intended to be used for specific domains (these ones should implement their own publishing
 * strategy through {@link TemplateArchetype#performETL(org.idb.cacao.api.ETLContext) performETL}).
 * 
 * @author Gustavo Figueiredo
 *
 */
public class GenericDataPublisher {

	private static final Logger log = Logger.getLogger(GenericDataPublisher.class.getName());

	/**
	 * The field name to be used by default as date/time filter of published data
	 */
	private static final String dashboardTimestamp = IndexNamesUtils.formatFieldName(PublishedDataFieldNames.TIMESTAMP.name());
	
	/**
	 * The field name for line numbering for the same published data contents
	 */
	private static final String lineNumber = IndexNamesUtils.formatFieldName(PublishedDataFieldNames.LINE.name());

	/**
	 * The field name for line numbering for the same published data contents represented as timestamps (@see javadoc comments for PublishedDataFieldNames.LINE_SORT)
	 */
	private static final String lineNumberSort = IndexNamesUtils.formatFieldName(PublishedDataFieldNames.LINE_SORT.name());

	/**
	 * The field name for date/time of published data
	 */
	private static final String publishedTimestamp = PublishedDataFieldNames.ETL_TIMESTAMP.getFieldName();

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
	 * Performs the Extract/Transform/Load operations with available data
	 */
	public static boolean performETL(ETLContext context) {
		
		try {
			
			String taxPayerId = context.getDocumentUploaded().getTaxPayerId();
			Integer taxPeriodNumber = context.getDocumentUploaded().getTaxPeriodNumber();
			
			DocumentUploaded doc = context.getDocumentUploaded();
			DocumentTemplate template = context.getDocumentTemplate();
			Map<String,DomainTable.MultiLingualMap> domainTables = ETLContext.getDomainTablesInTemplate(template, context.getDomainTableRepository());

			// Structure for loading and caching information from the provided Taxpayers registry
			LoadingCache<String, Optional<Map<String, Object>>> lookupTaxpayers = getLookupTaxpayers(context.getTaxpayerRepository());

			final Optional<Map<String,Object>> declarantInformation = (taxPayerId==null) ? Optional.empty() : lookupTaxpayers.getUnchecked(taxPayerId);

			LoadDataStrategy loader = context.getLoadDataStrategy();
			loader.start();
			
			final AtomicLong countRecordsOverall = new AtomicLong();

	        final OffsetDateTime timestamp = context.getDocumentUploaded().getTimestamp();

			// Reads the validated incoming data
			Stream<Map<String, Object>> data = context.getValidatedDataRepository().getValidatedData(doc.getTemplateName(), doc.getTemplateVersion(), doc.getFileId(),
					/*sortBy*/Optional.empty(),
					/*sortOrder*/Optional.empty());

			if (data==null)
				data = Collections.<Map<String, Object>>emptySet().stream();
			
			// Get a name for the published data
			final String published_data_index = IndexNamesUtils.formatIndexNameForPublishedData(template.getName());

			// Deletes previous published data
			if (taxPayerId!=null || taxPeriodNumber!=null) {
				try {
					context.getLoadDataStrategy().delete(published_data_index, taxPayerId, taxPeriodNumber);
				} catch (Throwable ex) {
					log.log(Level.SEVERE, "Error while deleting previous published data at "+published_data_index+" regarding "+taxPayerId+" and period "+taxPeriodNumber, ex);
				}
			}

			// Start the denormalization process
			
			boolean success = true;
			try {
				
				data.forEach(record->{
					
					String rowId = String.format("%s.%d.%014d", taxPayerId, taxPeriodNumber, countRecordsOverall.incrementAndGet());
					Map<String,Object> normalizedRecord = new HashMap<>(record);
					
					for (ValidatedDataFieldNames vfieldName: ValidatedDataFieldNames.values()) {
						// Published data has all fields in lower case
						Object value = normalizedRecord.remove(vfieldName.name());
						if (value!=null) {
							normalizedRecord.put(IndexNamesUtils.formatFieldName(vfieldName.name()), value);
						}
					}

					normalizedRecord.put(dashboardTimestamp, timestamp);
					normalizedRecord.put(publishedTimestamp, timestamp);
					normalizedRecord.put(publishedTaxpayerId, taxPayerId);
					normalizedRecord.put(publishedtaxPeriodNumber, taxPeriodNumber);
					normalizedRecord.put(publishedTemplateName, doc.getTemplateName());
					normalizedRecord.put(publishedTemplateVersion, doc.getTemplateVersion());
					normalizedRecord.put(lineNumber, countRecordsOverall.longValue());
					normalizedRecord.put(lineNumberSort, new java.util.Date(countRecordsOverall.longValue()));

					// Includes data about declarant
					if (declarantInformation.isPresent())
						normalizedRecord.putAll(declarantInformation.get());
					
					// Includes data about domain tables (possibly in multiple languages)
					ETLContext.denormalizeDomainTables(record, normalizedRecord, domainTables);

					loader.add(new IndexRequest(published_data_index)
						.id(rowId)
						.source(normalizedRecord));


				}); // LOOP over all entries in validated data

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
				context.setOutcomeSituation(doc, DocumentSituation.PROCESSED);
				ETLContext.markReplacedDocuments(doc, context);
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
	
}
