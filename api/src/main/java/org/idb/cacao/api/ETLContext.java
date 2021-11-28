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
package org.idb.cacao.api;

import java.io.Closeable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.DomainEntry;
import org.idb.cacao.api.templates.DomainTable;
import org.idb.cacao.api.templates.FieldType;
import org.idb.cacao.api.utils.IndexNamesUtils;
import org.springframework.context.MessageSource;
import org.springframework.data.util.Pair;

/**
 * This objects wraps up information collected during the ETL phase. Includes
 * all objects needed for building one or more denormalized views of the data
 * collected in one or more correlated (and pre-validated) files.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class ETLContext {

	/**
	 * This generic interface provides means for fetching pre-validated data
	 * (the output of the 'validation' phase') in generic format (agnostic to
	 * specific file format).
	 * 
	 * @author Gustavo Figueiredo
	 */
	public static interface ValidatedDataRepository {
		
		/**
		 * Given the {@link DocumentTemplate#getArchetype() archetype}, returns the collection of {@link DocumentTemplate DocumentTemplate} that implements it.
		 * Should return empty collection if there is no templates related to the archetype. Should throw exception in
		 * case of error while searching database.
		 */
		public Collection<DocumentTemplate> getTemplates(String archetype) throws Exception;
		
		/**
		 * Given the {@link DocumentUploaded#getTemplateName() templateName}, the {@link DocumentUploaded#getTemplateVersion() templateVersion},
		 * the {@link DocumentUploaded#getTaxPayerId() taxPayerId} and the {@link DocumentUploaded#getTaxPeriodNumber() taxPeriodNumber}, returns
		 * the collection of {@link DocumentUploaded DocumentUploaded} records of pre validated files regarding these criteria.
		 */
		public Collection<DocumentUploaded> getUploads(String templateName, String templateVersion, String taxPayerId, Integer taxPeriodNumber) throws Exception;
		
		/**
		 * Given the {@link DocumentUploaded#getTemplateName() templateName}, the {@link DocumentUploaded#getTemplateVersion() templateVersion},
		 * and the {@link DocumentUploaded#getFileId() fileId}, returns indication that
		 * the import operation finished. Should throw exception in
		 * case of error while searching database.
		 */
		public boolean hasValidation(String templateName, String templateVersion, String fileId) throws Exception;
		 
		/**
		 * Given the {@link DocumentUploaded#getTemplateName() templateName}, the {@link DocumentUploaded#getTemplateVersion() templateVersion},
		 * and the {@link DocumentUploaded#getFileId() fileId}, returns the corresponding
		 * pre-validated data stored in database. Should return empty stream if there is no data. Should throw exception in
		 * case of error while searching database.
		 * @param sortBy Optional sort by criteria. Multiple field names may be informed.
		 */
		public Stream<Map<String,Object>> getValidatedData(String templateName, String templateVersion, String fileId, Optional<String[]> sortBy,
				final Optional<SortOrder> sortOrder) throws Exception;
		
		/**
		 * Given the {@link DocumentUploaded#getTemplateName() templateName}, the {@link DocumentUploaded#getTemplateVersion() templateVersion},
		 * the {@link DocumentUploaded#getFileId() fileId} and one arbitrary query filter, returns the first occurrence of a record that match
		 * these criteria.
		 */
		public Optional<Map<String,Object>> getValidatedData(String templateName, String templateVersion, String fileId, QueryBuilder query) throws Exception;
		
		/**
		 * Applies a term query filter to the search.
		 */
		default public Optional<Map<String,Object>> getValidatedData(String templateName, String templateVersion, String fileId, String queryTermName, String queryTermValue) throws Exception {
			return getValidatedData(templateName, templateVersion, fileId, new TermQueryBuilder(queryTermName, queryTermValue));
		}
	}
	
	/**
	 * This generic interface provides means for fetching Taxpayers information in order
	 * to project additional data in the denormalized views through the ETL job
	 * 
	 * @author Gustavo Figueiredo
	 *
	 */
	public static interface TaxpayerRepository {
		
		/**
		 * Given the taxpayer Id, should return additional data to be included in denormalized views
		 */
		public Optional<Map<String,Object>> getTaxPayerData(String taxPayerId);
		
	}
	
	/**
	 * This generic interface provides means for returning a DomainTable given its name and version
	 * 
	 * @author Gustavo Figueiredo
	 *
	 */
	public static interface DomainTableRepository {
		
		public Optional<DomainTable> findByNameAndVersion(String name, String version);
		
	}
	
	/**
	 * This generic interface will be used by the ETL implementation for loading data with denormalized views.<BR>
	 * There should be one instance for each processing. There should not be a shared instance among different
	 * processes.
	 * 
	 * @author Gustavo Figueiredo
	 *
	 */
	public static interface LoadDataStrategy extends Closeable {
		
		/**
		 * Start the process of storing data.
		 */
		default public void start() { }
		
		/**
		 * Deletes previous published (denormalized) data stored in the given index related to the given taxPayerId and taxPeriodNumber<BR>
		 * The field name for taxPayerId is '_taxpayer_id' (according to method 'toString' of {@link PublishedDataFieldNames#TAXPAYER_ID TAXPAYER_ID}).<BR>
		 * The field name for taxPeriodNumber is '_tax_period_number' (according to method 'toString' of {@link PublishedDataFieldNames#TAXPERIOD_NUMBER TAXPERIOD_NUMBER}).<BR>
		 */
		default public void delete(String indexName, String taxPayerId, Integer taxPeriodNumber) throws Exception { }
		
		/**
		 * Include data to be stored
		 */
		public void add(IndexRequest request);
		
		/**
		 * Commits the data informed so far (save it into database)
		 */
		default public void commit() throws Exception { }
		
		/**
		 * Finishes the process of storing data and release objects
		 */
		default public void close() { }
		
	}
	
	/**
	 * Reference to the incoming file that triggered this ETL operation
	 */
	private DocumentUploaded documentUploaded;
	
	/**
	 * The document template related to the incoming file that triggered this ETL operation
	 */
	private DocumentTemplate documentTemplate;

	/**
	 * The consumer of ETL service provides an implementation of this interface in order to allow
	 * domain specific ETL operations gathering all the information it needs.
	 */
	private ValidatedDataRepository validatedDataRepository;
	
	/**
	 * The consumer of ETL service provides an implementation of this interface in order to allow
	 * domain specific ETL operations gathering information about taxpayers.
	 */
	private TaxpayerRepository taxpayerRepository;
	
	/**
	 * The consumer of ETL service provides an implementation of this interface in order to
	 * return domain table registered in application
	 */
	private DomainTableRepository domainTableRepository;
	
	/**
	 * The consumer of ETL service provides an implementation of this interface in order to allow
	 * loading data after processing.
	 */
	private LoadDataStrategy loadDataStrategy;

	/**
	 * Object used to resolve errors according to a specific language
	 */
	private MessageSource messageSource;
	
	/**
	 * Warnings produced by the validation phase. Texts informed in braces should be resolved with messages.properties.<BR>
	 * E.g.: If the alert is "{some.message}", it will be resolved to another message according to messages.properties
	 * and the user preference. Parameters for messages.properties entry may be provided in parentheses. For example,
	 * {some.message(param1)} will be resolved with messages.properties using 'some.message' as key and 'param1' as
	 * the first parameter to this message. Additional parameters may be separated by commas.  If the parameter is in braces,
	 * it will also be resolved with messages.properties. For example, "{some.message({some.parameter})}" will first
	 * resolve 'some.parameter' as key to messages.properties, than will use it as a parameter using 'some.message' as key. 
	 */
	private Map<DocumentUploaded, List<String>> alerts;

	/**
	 * The ETL process should write here the situation for each DocumentUploaded that was part of this
	 * process. For example, some previous DocumentUploaded should be turned into PROCESSED.
	 */
	private final Map<DocumentUploaded, DocumentSituation> outcomeSituations;
	
	public ETLContext() {
		outcomeSituations = new HashMap<>();
		alerts = new HashMap<>();
	}

	/**
	 * Reference to the incoming file that triggered this ETL operation
	 */
	public DocumentUploaded getDocumentUploaded() {
		return documentUploaded;
	}

	/**
	 * Reference to the incoming file that triggered this ETL operation
	 */
	public void setDocumentUploaded(DocumentUploaded documentUploaded) {
		this.documentUploaded = documentUploaded;
	}

	/**
	 * The document template related to the incoming file that triggered this ETL operation
	 */
	public DocumentTemplate getDocumentTemplate() {
		return documentTemplate;
	}

	/**
	 * The document template related to the incoming file that triggered this ETL operation
	 */
	public void setDocumentTemplate(DocumentTemplate documentTemplate) {
		this.documentTemplate = documentTemplate;
	}

	/**
	 * The consumer of ETL service provides an implementation of this interface in order to allow
	 * domain specific ETL operations gathering all the information it needs.
	 */
	public ValidatedDataRepository getValidatedDataRepository() {
		return validatedDataRepository;
	}

	/**
	 * The consumer of ETL service provides an implementation of this interface in order to allow
	 * domain specific ETL operations gathering all the information it needs.
	 */
	public void setValidatedDataRepository(ValidatedDataRepository validatedDataRepository) {
		this.validatedDataRepository = validatedDataRepository;
	}

	/**
	 * The consumer of ETL service provides an implementation of this interface in order to allow
	 * domain specific ETL operations gathering information about taxpayers.
	 */
	public TaxpayerRepository getTaxpayerRepository() {
		return taxpayerRepository;
	}

	/**
	 * The consumer of ETL service provides an implementation of this interface in order to allow
	 * domain specific ETL operations gathering information about taxpayers.
	 */
	public void setTaxpayerRepository(TaxpayerRepository taxpayerRepository) {
		this.taxpayerRepository = taxpayerRepository;
	}

	/**
	 * The consumer of ETL service provides an implementation of this interface in order to
	 * return domain table registered in application
	 */
	public DomainTableRepository getDomainTableRepository() {
		return domainTableRepository;
	}

	/**
	 * The consumer of ETL service provides an implementation of this interface in order to
	 * return domain table registered in application
	 */
	public void setDomainTableRepository(DomainTableRepository domainTableRepository) {
		this.domainTableRepository = domainTableRepository;
	}

	/**
	 * Object used to resolve errors according to a specific language
	 */
	public MessageSource getMessageSource() {
		return messageSource;
	}

	/**
	 * Object used to resolve errors according to a specific language
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/**
	 * Warnings produced by the validation phase. Texts informed in braces should be resolved with messages.properties.<BR>
	 * E.g.: If the alert is "{some.message}", it will be resolved to another message according to messages.properties
	 * and the user preference. Parameters for messages.properties entry may be provided in parentheses. For example,
	 * {some.message(param1)} will be resolved with messages.properties using 'some.message' as key and 'param1' as
	 * the first parameter to this message. Additional parameters may be separated by commas.  If the parameter is in braces,
	 * it will also be resolved with messages.properties. For example, "{some.message({some.parameter})}" will first
	 * resolve 'some.parameter' as key to messages.properties, than will use it as a parameter using 'some.message' as key. 
	 */
	public Map<DocumentUploaded, List<String>> getAlerts() {
		return alerts;
	}
	
	public boolean hasAlerts() {
		return alerts!=null && !alerts.isEmpty();
	}

	/**
	 * Warnings produced by the validation phase. Texts informed in braces should be resolved with messages.properties.<BR>
	 * E.g.: If the alert is "{some.message}", it will be resolved to another message according to messages.properties
	 * and the user preference. Parameters for messages.properties entry may be provided in parentheses. For example,
	 * {some.message(param1)} will be resolved with messages.properties using 'some.message' as key and 'param1' as
	 * the first parameter to this message. Additional parameters may be separated by commas.  If the parameter is in braces,
	 * it will also be resolved with messages.properties. For example, "{some.message({some.parameter})}" will first
	 * resolve 'some.parameter' as key to messages.properties, than will use it as a parameter using 'some.message' as key. 
	 */
	public void setAlerts(Map<DocumentUploaded, List<String>> alerts) {
		this.alerts = alerts;
	}
	
	/**
	 * Warnings produced by the validation phase. Texts informed in braces should be resolved with messages.properties.<BR>
	 * E.g.: If the alert is "{some.message}", it will be resolved to another message according to messages.properties
	 * and the user preference. Parameters for messages.properties entry may be provided in parentheses. For example,
	 * {some.message(param1)} will be resolved with messages.properties using 'some.message' as key and 'param1' as
	 * the first parameter to this message. Additional parameters may be separated by commas.  If the parameter is in braces,
	 * it will also be resolved with messages.properties. For example, "{some.message({some.parameter})}" will first
	 * resolve 'some.parameter' as key to messages.properties, than will use it as a parameter using 'some.message' as key. 
	 */
	public void addAlert(DocumentUploaded document, String alert) {
		if (document==null || alert==null || alert.trim().length()==0)
			return;
		if (alerts==null)
			alerts = new HashMap<>();
		synchronized (this.alerts) {
			List<String> alerts_same_document = alerts.computeIfAbsent(document, d->new LinkedList<>());
			alerts_same_document.add(alert);
		}
	}

	/**
	 * The ETL process should write here the situation for each DocumentUploaded that was part of this
	 * process. For example, some previous DocumentUploaded should be turned into PROCESSED.
	 */
	public Map<DocumentUploaded, DocumentSituation> getOutcomeSituations() {
		return outcomeSituations;
	}

	/**
	 * The ETL process should write here the situation for each DocumentUploaded that was part of this
	 * process. For example, some previous DocumentUploaded should be turned into PROCESSED.
	 */
	public void setOutcomeSituation(DocumentUploaded upload, DocumentSituation situation) {
		if (upload==null)
			return;
		if (situation==null)
			outcomeSituations.remove(upload);
		else
			outcomeSituations.put(upload, situation);
	}
	
	/**
	 * Same as {@link #setOutcomeSituation(DocumentUploaded, DocumentSituation) setOutcomeSituation}.
	 */
	public void setDocumentSituation(DocumentUploaded upload, DocumentSituation situation) {
		setOutcomeSituation(upload, situation);
	}
	
	/**
	 * Returns the outcome situation regarding a particular upload
	 */
	public DocumentSituation getOutcomeSituation(DocumentUploaded upload) {
		return outcomeSituations.get(upload);
	}
	
	/**
	 * Returns indication there is at least one outcome situation regarding any document
	 */
	public boolean hasOutcomeSituations() {
		return !outcomeSituations.isEmpty();
	}

	/**
	 * The consumer of ETL service provides an implementation of this interface in order to allow
	 * loading data after processing.
	 */
	public LoadDataStrategy getLoadDataStrategy() {
		return loadDataStrategy;
	}

	/**
	 * The consumer of ETL service provides an implementation of this interface in order to allow
	 * loading data after processing.
	 */
	public void setLoadDataStrategy(LoadDataStrategy loadDataStrategy) {
		this.loadDataStrategy = loadDataStrategy;
	}
		
	/**
	 * Utility method for retrieving standard general purpose information about taxpayers. Useful
	 * for denormalized views regarding taxpayers.
	 */
	public static Map<String,Object> getTaxpayerBasicInformation(Taxpayer taxpayer) {
		if (taxpayer==null)
			return Collections.emptyMap();
		Map<String,Object> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		if (taxpayer.getName()!=null && taxpayer.getName().trim().length()>0)
			map.put("TaxPayerName",taxpayer.getName().trim());
		if (taxpayer.getAddress()!=null && taxpayer.getAddress().trim().length()>0)
			map.put("TaxPayerAddress",taxpayer.getAddress().trim());
		if (taxpayer.getZipCode()!=null && taxpayer.getZipCode().trim().length()>0)
			map.put("TaxPayerZipCode",taxpayer.getZipCode().trim());
		if (taxpayer.getQualifier1()!=null && taxpayer.getQualifier1().trim().length()>0)
			map.put("TaxPayerQualifier1",taxpayer.getQualifier1().trim());
		if (taxpayer.getQualifier2()!=null && taxpayer.getQualifier2().trim().length()>0)
			map.put("TaxPayerQualifier2",taxpayer.getQualifier2().trim());
		if (taxpayer.getQualifier3()!=null && taxpayer.getQualifier3().trim().length()>0)
			map.put("TaxPayerQualifier3",taxpayer.getQualifier3().trim());
		if (taxpayer.getQualifier4()!=null && taxpayer.getQualifier4().trim().length()>0)
			map.put("TaxPayerQualifier4",taxpayer.getQualifier4().trim());
		if (taxpayer.getQualifier5()!=null && taxpayer.getQualifier5().trim().length()>0)
			map.put("TaxPayerQualifier5",taxpayer.getQualifier5().trim());
		return map;
	}

	/**
	 * Given all other previous uploaded document regarding the same template, taxpayerId and period number of another document that was
	 * processed in the ETL, change their situation from PROCESSED to REPLACED. In other words, avoid keeping multiple documents regarding the
	 * same subject with the 'PROCESSED' situation. Only one of them will be considered PROCESSED.
	 */
	public static void markReplacedDocuments(DocumentUploaded prevailingDocument, ETLContext context) throws Exception {
		Collection<DocumentUploaded> uploads = context.getValidatedDataRepository().getUploads(prevailingDocument.getTemplateName(), prevailingDocument.getTemplateVersion(), 
				prevailingDocument.getTaxPayerId(), prevailingDocument.getTaxPeriodNumber());
		if (uploads.isEmpty()) {
			return;  
		}
		for (DocumentUploaded upload: uploads) {
			if (upload.equals(prevailingDocument))
				continue;
			if (DocumentSituation.PROCESSED.equals(upload.getSituation())
					|| DocumentSituation.VALID.equals(upload.getSituation())) {
				context.setDocumentSituation(upload, DocumentSituation.REPLACED);
			}
		}
	}

	/**
	 * Returns the list of domain tables that are referenced by the fields declared in the DocumentTemplate. The map returned by this method
	 * contains fields names as keys and the corresponding DomainTable's as values.
	 */
	public static Map<String,DomainTable.MultiLingualMap> getDomainTablesInTemplate(DocumentTemplate template, ETLContext.DomainTableRepository repository) {
		// Get the list of fields that are making use of any domain table
		List<DocumentField> fields_with_domain_tables_references =
			template.getFields().stream()
			.filter(field->FieldType.DOMAIN.equals(field.getFieldType()) 
				&& field.getDomainTableName()!=null 
				&& field.getDomainTableName().trim().length()>0)
			.collect(Collectors.toList());
		if (fields_with_domain_tables_references.isEmpty())
			return Collections.emptyMap();
		
		// Get the references to domain tables considering the DocumentTemplate fields
		List<Pair<String,String>> domain_tables_references =
			fields_with_domain_tables_references.stream()
			.map(field->Pair.of(field.getDomainTableName(), field.getDomainTableVersion()))
			.distinct()
			.collect(Collectors.toList());
		if (domain_tables_references.isEmpty())
			return Collections.emptyMap();
		
		// Resolve the names and versions into objects
		Map<Pair<String,String>, DomainTable> resolved_references = 
			domain_tables_references.stream()
			.map(ref->repository.findByNameAndVersion(ref.getFirst(), ref.getSecond()))
			.filter(Optional::isPresent)
			.collect(Collectors.toMap(
				/*keyMapper*/t->Pair.of(t.get().getName(),t.get().getVersion()), 
				/*valueMapper*/Optional::get, 
				/*mergeFunction*/(a,b)->a));
		if (resolved_references.isEmpty())
			return Collections.emptyMap();
		
		// Make the final map with fields names and corresponding DomainTable objects
		return fields_with_domain_tables_references.stream()
			.filter(field->resolved_references.containsKey(Pair.of(field.getDomainTableName(), field.getDomainTableVersion())))
			.collect(Collectors.toMap(
				/*keyMapper*/f->IndexNamesUtils.formatFieldName(f.getFieldName()), 
				/*valueMapper*/field->resolved_references.get(Pair.of(field.getDomainTableName(), field.getDomainTableVersion())).toMultiLingualMap(), 
				/*mergeFunction*/(a,b)->a));
	}
	
	/**
	 * Includes data about domain tables (possibly in multiple languages) into the generic denormalized record
	 * @param record Generic record with input data (contains references to domain tables)
	 * @param denormalizedRecord Generic record (where this method will write denormalized data)
	 * @param domainTables Output of {@link ETLContext#getDomainTablesInTemplate(DocumentTemplate, DomainTableRepository) getDomainTablesInTemplate}
	 */
	public static void denormalizeDomainTables(
			final Map<String, Object> record,
			final Map<String,Object> denormalizedRecord,
			final Map<String,DomainTable.MultiLingualMap> domainTables) {
		// Includes data about domain tables (possibly in multiple languages)
		if (!domainTables.isEmpty()) {
			for (Map.Entry<String, DomainTable.MultiLingualMap> domainRef: domainTables.entrySet()) {
				String fieldName = domainRef.getKey();
				Object domainValue = record.get(fieldName);
				if (domainValue==null)
					continue;
				Map<DomainLanguage, DomainEntry> multiLingualDesc = domainRef.getValue().get(ValidationContext.toString(domainValue));
				if (multiLingualDesc==null)
					continue;
				for (Map.Entry<DomainLanguage, DomainEntry> domainEntry: multiLingualDesc.entrySet()) {
					String derivedFieldName = fieldName + "_name";
					if (!DomainLanguage.ENGLISH.equals(domainEntry.getKey())) {
						derivedFieldName += "_" + domainEntry.getKey().getDefaultLocale().getLanguage();
					}
					denormalizedRecord.put(derivedFieldName, domainEntry.getValue());
				}
			}
		}

	}
}
