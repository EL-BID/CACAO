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
package org.idb.cacao.web.controllers.services;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.Max;
import org.elasticsearch.search.aggregations.metrics.Min;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.idb.cacao.web.controllers.rest.DocumentStoreAPIController;
import org.idb.cacao.web.entities.DocumentUploaded;
import org.idb.cacao.web.entities.Periodicity;
import org.idb.cacao.web.utils.ErrorUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for retrieving information about uploaded documents
 * 
 * @author Gustavo Figueiredo
 *
 */
@Service
public class DocumentStoreService {

	private static final Logger log = Logger.getLogger(DocumentStoreService.class.getName());
	
	/**
	 * Minimal value to consider some monetary figure as 'zero' (e.g.: 0.00001 should be considered as zero).
	 */
	public static final double EPSILON = 0.005;

	@Autowired
	private RestHighLevelClient elasticsearchClient;

	@Autowired
	private MessageSource messageSource;
	
	@Autowired
	private FieldsConventionsService fieldsConventionsService;

	/**
	 * Queries ElasticSearch for structured information collected and stored for an uploaded file.
	 */
	public void getDocumentInformation(DocumentUploaded doc, Authentication auth, Consumer<Map<String,Object>> consumer) throws Exception {
    	String template = doc.getTemplateName();
    	String index_name = "doc_"+DocumentStoreAPIController.formatIndexName(/*indexName*/template);
    	SearchRequest searchRequest = new SearchRequest(index_name);
    	SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
    			.query(QueryBuilders.idsQuery().addIds(doc.getId()));   
    	searchRequest.source(searchSourceBuilder);
    	SearchResponse resp = null;
		try {
			resp = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
		} catch (IOException ex) {
			if (ErrorUtils.isErrorNoIndexFound(ex) || ErrorUtils.isErrorNoMappingFoundForColumn(ex))
				return; // no match
			log.log(Level.SEVERE, "Error while fetching document details about document "+doc.getId()+" "+template, ex);
			throw new Exception(messageSource.getMessage("general_error_msg1", null, LocaleContextHolder.getLocale()));
		}
		if (resp!=null) 
		{
	    	log.log(Level.FINE, "User "+auth.getName()+" requested details about document "+doc.getId()+" "+template+" and got "+resp.getHits().getTotalHits().value+" response in "+resp.getTook());
	    	
	    	if (resp.isTimedOut() || Boolean.TRUE.equals(resp.isTerminatedEarly())) {
	    		throw new Exception(messageSource.getMessage("timed_out", null, LocaleContextHolder.getLocale()));
	    	}
	    	else if (resp.getHits().getTotalHits().value==0) {
	    		throw new Exception(messageSource.getMessage("doc_not_found", null, LocaleContextHolder.getLocale()));
	    	}
	    	else {
	    		for (SearchHit hit:resp.getHits()) {
	    			Map<String,Object> source = hit.getSourceAsMap();
	    			consumer.accept(source);
	    		}
	    	}
		}

	}
	
	/**
	 * Search for all uploaded documents for a given taxpayer ID and template name, starting from a given tax period (identified by a number usually
	 * in the form YYYYMM). Returns TRUE if found any document, returns FALSE otherwise.
	 */
	@Transactional(readOnly=true)
	public boolean hasDeclarationsAfterPeriod(
			final String filter_taxpayer_id, 
			final String filter_template,
			final Integer filter_period_min,
			final boolean includeLower) {
		
    	// Index over 'DocumentUploaded' objects
    	SearchRequest searchRequest = new SearchRequest("docs_uploaded");
    	
    	BoolQueryBuilder query = QueryBuilders.boolQuery();
    	
    	// Filter by taxPeriodNumber
    	query.must(new RangeQueryBuilder("taxPeriodNumber").from(filter_period_min, includeLower));
    	
    	// Filter by taxpayer ID
    	query.must(new TermQueryBuilder("taxPayerId.keyword", filter_taxpayer_id));
    	
    	// Filter for template name
		query.must(new TermQueryBuilder("templateName.keyword", filter_template));

    	// Configure the aggregations
    	AbstractAggregationBuilder<?> aggregationBuilder = AggregationBuilders.count("agg").field("_id");    			
    	
    	SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
    			.query(query)
    			.aggregation(aggregationBuilder); 	    	
    	
    	// We are not interested on individual documents
    	searchSourceBuilder.size(0);
    	
    	searchRequest.source(searchSourceBuilder);

    	SearchResponse sresp;
		try {
			sresp = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
		} catch (IOException ex) {
			if (ErrorUtils.isErrorNoIndexFound(ex) || ErrorUtils.isErrorNoMappingFoundForColumn(ex))
				return false; // no match
			throw new RuntimeException(ex);
		}
		
    	if (sresp.getHits().getTotalHits().value==0) {
    		return false;	// No match
    	}
    	else {
    		
    		return true; 	// At least one match
    		
    	} // condition: got results from query
	}

	/**
	 * Search for all uploaded documents for a given taxpayer ID and template name and given tax period (identified by a number usually
	 * in the form YYYYMM). Returns TRUE if found any document, returns FALSE otherwise.
	 */
	@Transactional(readOnly=true)
	public boolean hasDeclarationsAtPeriod(
			final String filter_taxpayer_id, 
			final String filter_template,
			final Integer filter_period) {
		
    	// Index over 'DocumentUploaded' objects
    	SearchRequest searchRequest = new SearchRequest("docs_uploaded");
    	
    	BoolQueryBuilder query = QueryBuilders.boolQuery();
    	
    	// Filter by taxPeriodNumber
    	query.must(new TermQueryBuilder("taxPeriodNumber", filter_period));
    	
    	// Filter by taxpayer ID
    	query.must(new TermQueryBuilder("taxPayerId.keyword", filter_taxpayer_id));
    	
    	// Filter for template name
		query.must(new TermQueryBuilder("templateName.keyword", filter_template));

    	// Configure the aggregations
    	AbstractAggregationBuilder<?> aggregationBuilder = AggregationBuilders.count("agg").field("_id");    			
    	
    	SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
    			.query(query)
    			.aggregation(aggregationBuilder); 	    	
    	
    	// We are not interested on individual documents
    	searchSourceBuilder.size(0);
    	
    	searchRequest.source(searchSourceBuilder);

    	SearchResponse sresp;
		try {
			sresp = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
		} catch (IOException ex) {
			if (ErrorUtils.isErrorNoIndexFound(ex) || ErrorUtils.isErrorNoMappingFoundForColumn(ex))
				return false; // no match
			throw new RuntimeException(ex);
		}
		
    	if (sresp.getHits().getTotalHits().value==0) {
    		return false;	// No match
    	}
    	else {
    		
    		return true; 	// At least one match
    		
    	} // condition: got results from query
	}

	/**
	 * Search for last periods of documents for a given taxpayer ID and template name, up to a given tax period (identified by a number usually
	 * in the form YYYYMM). Returns the number of the period for the first missing document, returns NULL otherwise.
	 */
	@Transactional(readOnly=true)
	public Integer getDeclarationMissingBeforePeriod(
			final String filter_taxpayer_id, 
			final String filter_template,
			final Integer filter_period_max,
			final boolean includeUpper) {
		
    	// Index over 'DocumentUploaded' objects
    	SearchRequest searchRequest = new SearchRequest("docs_uploaded");
    	
    	BoolQueryBuilder query = QueryBuilders.boolQuery();
    	
    	// Filter by taxPeriodNumber
    	query.must(new RangeQueryBuilder("taxPeriodNumber").to(filter_period_max, includeUpper));
    	
    	// Filter by taxpayer ID
    	query.must(new TermQueryBuilder("taxPayerId.keyword", filter_taxpayer_id));
    	
    	// Filter for template name
		query.must(new TermQueryBuilder("templateName.keyword", filter_template));

    	// Configure the aggregations
    	AbstractAggregationBuilder<?> aggregationBuilder = AggregationBuilders.max("agg").field("taxPeriodNumber");    			
    	
    	SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
    			.query(query)
    			.aggregation(aggregationBuilder); 	    	
    	
    	// We are not interested on individual documents
    	searchSourceBuilder.size(0);
    	
    	searchRequest.source(searchSourceBuilder);

    	SearchResponse sresp;
		try {
			sresp = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
		} catch (IOException ex) {
			if (ErrorUtils.isErrorNoIndexFound(ex) || ErrorUtils.isErrorNoMappingFoundForColumn(ex))
				return null; // no match
			throw new RuntimeException(ex);
		}
		
    	if (sresp.getHits().getTotalHits().value==0) {
    		return null;	// No match
    	}
    	else {
    		
    		// At least one match
    		Max max = sresp.getAggregations().get("agg");
    		if (max==null || max.getValue()==0 || Double.isNaN(max.getValue()))
    			return null;
    		
    		int prev_period = (int)max.getValue();
    		Number next_period = Periodicity.getNextPeriod(prev_period);
    		if (next_period==null)
    			return null;  // Could not validate period number
    		
    		if (next_period.intValue()>=filter_period_max.intValue())
    			return null;  // No missing period
    		
    		return (int)next_period.intValue(); // first missing period number
    		
    	} // condition: got results from query
	}

	/**
	 * Given all documents uploaded since 'min_timestamp', returns the first period of declaration aggregated by taxpayer and by declaration template.
	 * @param filter_taxpayers_ids Optional filter for taxpayers. If not null, will be used to restrict the search for these taxpayers. Provide their ID's.
	 * @param filter_template Optional filter for template. If present, will be used to restrict the search for this template name.
	 * @return Returns a map with the following structure:<BR>
	 * First level of aggregation contains taxpayers' Ids as keys.<BR>
	 * Second level of aggregation contains template names as keys.<BR>
	 * The value corresponds to the tax period number (dependind on tax periodicity, may be an year, a month, etc.)<BR>
	 */
	@Transactional(readOnly=true)
	public Map<String,Map<String,Integer>> getMapTaxpayersFirstPeriod(
			final Set<String> filter_taxpayers_ids, 
			final Optional<String> filter_template) {
		
		SearchRequest searchRequest = searchTaxpayersDeclarationsFirstPeriods(
				filter_taxpayers_ids,
				filter_template,
				"templateName.keyword");
		
    	SearchResponse sresp;
		try {
			sresp = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
		} catch (Throwable ex) {
			if (ErrorUtils.isErrorIllegalArgumentFieldsNotOptimized(ex) || ErrorUtils.isErrorNoMappingFoundForColumn(ex)) {
				searchRequest = searchTaxpayersDeclarationsFirstPeriods(
						filter_taxpayers_ids,
						filter_template,
						"templateName");
				try {
					sresp = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
				} catch (Throwable ex2) {
					if (ErrorUtils.isErrorNoIndexFound(ex2) || ErrorUtils.isErrorNoMappingFoundForColumn(ex2))
						return Collections.emptyMap();
					throw new RuntimeException(ex2);					
				}
			}
			else {
				if (ErrorUtils.isErrorNoIndexFound(ex) || ErrorUtils.isErrorNoMappingFoundForColumn(ex))
					return Collections.emptyMap();
				throw new RuntimeException(ex);
			}
		}
		
		Map<String,Map<String,Integer>> statistics = new TreeMap<>();
    	
    	if (sresp.getHits().getTotalHits().value==0) {
    		return Collections.emptyMap();	// No taxpayers have uploaded files for the past months
    	}
    	else {
    		
    		// Let's fill the resulting map with those statistics

    		Terms taxpayersIds = sresp.getAggregations().get("byTaxPayerId");
    		for (Terms.Bucket txid_bucket : taxpayersIds.getBuckets()) {
    			String taxpayerId = txid_bucket.getKeyAsString();
    			if (taxpayerId==null || taxpayerId.trim().length()==0)
    				continue;
    			
    			Map<String,Integer> statistics_for_taxpayer = new TreeMap<>();
    			statistics.put(taxpayerId, statistics_for_taxpayer);
    			
    			Terms templates = txid_bucket.getAggregations().get("byTemplate");
    			
    			for (Terms.Bucket template_bucket : templates.getBuckets()) {
    				
    				String template_name = template_bucket.getKeyAsString();
    				if (template_name==null || template_name.trim().length()==0)
    					continue;
    				
    				Min minPeriod = template_bucket.getAggregations().get("minPeriod");
    				
    				if (minPeriod==null || minPeriod.getValue()==0 || Double.isNaN(minPeriod.getValue()))
    					continue;
    				
    				statistics_for_taxpayer.put(template_name, (int)minPeriod.getValue());
    				
    			} // LOOP over templates names buckets
    		} // LOOP over taxpayers buckets
    	} // condition: got results from query
    	
    	return statistics;
	}

	
	
	/**
	 * Build the query object used by {@link #getMapTaxpayersFirstPeriod(Set, Optional) getMapTaxpayersFirstPeriod}
	 */
	private SearchRequest searchTaxpayersDeclarationsFirstPeriods(final Set<String> filter_taxpayers_ids, 
			final Optional<String> filter_template,
			final String templateFieldName) {
    	// Index over 'DocumentUploaded' objects
    	SearchRequest searchRequest = new SearchRequest("docs_uploaded");
    	
    	// Filter by timestamp (only consider recent months for uploads)
    	BoolQueryBuilder query = QueryBuilders.boolQuery();
    	
    	// Optional filter by taxpayers Id's (depends on user profile and UI advanced filters)
    	if (filter_taxpayers_ids!=null) {
			BoolQueryBuilder subquery = QueryBuilders.boolQuery();
			for (String argument: filter_taxpayers_ids) {
				subquery = subquery.should(new TermQueryBuilder("taxPayerId.keyword", argument));
			}
			subquery = subquery.minimumShouldMatch(1);
			query = query.must(subquery);
    	}
    	
    	// Optional filter for template name
    	if (filter_template!=null && filter_template.isPresent()) {
    		query = query.must(new TermQueryBuilder(templateFieldName, filter_template.get()));
    	}

    	// Configure the aggregations
    	AbstractAggregationBuilder<?> aggregationBuilder = 	    			
    			// Aggregates by TaxPayer ID
    			AggregationBuilders.terms("byTaxPayerId").size(10_000).field("taxPayerId.keyword")
    			
    			// And then aggregates by Declaration Template Name
    			.subAggregation(AggregationBuilders.terms("byTemplate").size(10_000).field(templateFieldName)
    			
	    			// And then get the minimum MONTH
	    			.subAggregation(AggregationBuilders.min("minPeriod").field("taxPeriodNumber")
    			)	    			
    		);
    	
    	SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
    			.query(query)
    			.aggregation(aggregationBuilder); 	    	
    	
    	// We are not interested on individual documents
    	searchSourceBuilder.size(0);
    	
    	searchRequest.source(searchSourceBuilder);
    	
    	return searchRequest;
	}

}