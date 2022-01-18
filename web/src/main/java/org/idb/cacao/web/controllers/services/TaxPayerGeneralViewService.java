package org.idb.cacao.web.controllers.services;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.Min;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.idb.cacao.api.utils.DateTimeUtils;
import org.idb.cacao.web.dto.Account;
import org.idb.cacao.web.dto.BalanceSheet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class TaxPayerGeneralViewService {
	
	private static final Logger log = Logger.getLogger(TaxPayerGeneralViewService.class.getName());

	/**
	 * Maximum number of records that we will return for a given parameter.
	 * Should not be too high because it would compromise queries whenever used as query criteria
	 */
	public static final int MAX_TAXPAYERS_PER_TAXMANAGER = 10_000;	
	
	@Autowired
	private Environment env;
	
	@Autowired
	private RestHighLevelClient elasticsearchClient;	
	
	public BalanceSheet getBalance(String taxpayerId ) {
		
    	BalanceSheet balance = new BalanceSheet();
    	balance.setTaxPayerId("1234567");
    	balance.setTaxPayerName("Taxpayer name");
    	balance.setInitialDate(DateTimeUtils.now());
    	balance.setFinalDate(DateTimeUtils.now());
    	
    	Account account = new Account();
    	account.setLevel(1);
    	account.setCategory("Asset");    	
    	account.setDescription("Asset");
    	account.setCode("1");
    	account.setInitialBalance(100);
    	account.setFinalBalance(120);
    	account.setPercentage(100);
    	balance.addAccount(account);
    	
    	account = new Account();
    	account.setLevel(2);
    	account.setCategory("Asset");
    	account.setSubcategory("Cash and Cash Equivalents");
    	account.setDescription("Cash");
    	account.setCode("1.1");
    	account.setInitialBalance(100);
    	account.setFinalBalance(120);
    	account.setPercentage(100);    	
    	balance.addAccount(account);
    	
    	account = new Account();
    	account.setLevel(3);
    	account.setCategory("Asset");
    	account.setSubcategory("Cash and Cash Equivalents");
    	account.setCode("1.1.1");
    	account.setDescription("Money");
    	account.setInitialBalance(20);
    	account.setFinalBalance(20);
    	account.setPercentage(20);    	
    	balance.addAccount(account);
    	
    	account = new Account();
    	account.setLevel(3);
    	account.setCategory("Asset");
    	account.setSubcategory("Cash and Cash Equivalents");
    	account.setCode("1.1.2");
    	account.setDescription("Bank");
    	account.setInitialBalance(80);
    	account.setFinalBalance(100);
    	account.setPercentage(80);    	
    	balance.addAccount(account);
    	
    	return balance;
		
	}

	/**
	 * Build the query object used by {@link #getMapTaxpayersFirstPeriod(Set, Optional) getMapTaxpayersFirstPeriod}
	 */
	private SearchRequest searchBalance(final String taxpayersId, final OffsetDateTime period) {
		

		final String templateFieldName = "!";
		
    	// Index over 'DocumentUploaded' objects
    	SearchRequest searchRequest = new SearchRequest("docs_uploaded");
    	
    	// Filter by timestamp (only consider recent months for uploads)
    	BoolQueryBuilder query = QueryBuilders.boolQuery();
    	
		/*
		 * // Optional filter by taxpayers Id's (depends on user profile and UI advanced
		 * filters) if (filter_taxpayers_ids!=null) { BoolQueryBuilder subquery =
		 * QueryBuilders.boolQuery(); for (String argument: filter_taxpayers_ids) {
		 * subquery = subquery.should(new TermQueryBuilder("taxPayerId.keyword",
		 * argument)); } subquery = subquery.minimumShouldMatch(1); query =
		 * query.must(subquery); }
		 * 
		 * // Optional filter for template name if (filter_template!=null &&
		 * filter_template.isPresent()) { query = query.must(new
		 * TermQueryBuilder(templateFieldName, filter_template.get())); }
		 */

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
	
	public Map<String,Map<String,Integer>> getBalance(final String taxpayersId, final OffsetDateTime period) {
		
		SearchRequest searchRequest = searchBalance(
				taxpayersId,
				period);
		
    	SearchResponse sresp = null;
		try {
			sresp = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
		} catch (Throwable ex) {
			//TODO
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
	
}
