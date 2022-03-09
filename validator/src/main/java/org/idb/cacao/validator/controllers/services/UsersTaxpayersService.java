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
package org.idb.cacao.validator.controllers.services;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.errors.CommonErrors;
import org.idb.cacao.api.utils.MappingUtils;
import org.idb.cacao.api.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Functionality for determining which taxpayers one user is allowed to represent.
 * 
 * @author Gustavo Figueiredo
 *
 */
@Service
public class UsersTaxpayersService {
	
	/**
	 * User profile internal name for allowing access to upload file on behalf of any taxpayer
	 */
	public static final String SYSADMIN_USER_PROFILE = "SYSADMIN";

	@Autowired
	private RestHighLevelClient elasticsearchClient;

	/**
	 * Check if a given user (identified by his user name) may represent a given taxpayer (identified by his taxpayer Id)
	 * @param userTaxpayerId Optional parameter. If different than NULL, receive the taxpayer Id related to the user regardless of the outcome of this authorization
	 */
	public boolean isUserRepresentativeOf(String userName, String taxpayerId, AtomicReference<String> userTaxpayerId) throws Exception {
		
		if (userName==null || userName.trim().length()==0)
			return false;
		
		if (taxpayerId==null || taxpayerId.trim().length()==0)
			return false;
		
		// First check the user attributes
		
		SearchRequest searchRequest = new SearchRequest("cacao_user");
		BoolQueryBuilder query = QueryBuilders.boolQuery();
		query = query.must(new TermQueryBuilder("name.keyword", userName).caseInsensitive(true))
				.must(new TermQueryBuilder("active", true));
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(query);
		searchSourceBuilder.size(1);
		searchRequest.source(searchSourceBuilder);

		SearchResponse sresp = doSearch(searchRequest);
		if (sresp == null || Utils.getTotalHits(sresp) == 0) 
			return false; // unknown user
		
		Map<String,Object> user_attributes = sresp.getHits().getHits()[0].getSourceAsMap();

		String user_taxpayerId = ValidationContext.toString(user_attributes.get("taxpayerId"));
		if (userTaxpayerId!=null) {
			userTaxpayerId.set(user_taxpayerId);
		}

		String user_profile = ValidationContext.toString(user_attributes.get("profile"));
		if (SYSADMIN_USER_PROFILE.equalsIgnoreCase(user_profile)) {
			return true; 	// System admin may represent any user
		}
		
		if (user_taxpayerId==null) {
			return false;	// unidentified taxpayer
		}
				
		if (user_taxpayerId.equalsIgnoreCase(taxpayerId)) {
			return true;	// The user may represent himself
		}

		
		// Next, check the interpersonal relationships
		
		searchRequest = new SearchRequest("cacao_interpersonal");
		query = QueryBuilders.boolQuery();
		query = query.must(new TermQueryBuilder("personId1", user_taxpayerId));
		query = query.must(new TermQueryBuilder("personId2", taxpayerId));
		query = query.must(new TermQueryBuilder("active", true));
		searchSourceBuilder = new SearchSourceBuilder().query(query);
		searchSourceBuilder.size(0); // we don't need to receive the hits
		searchRequest.source(searchSourceBuilder);

		sresp = doSearch(searchRequest);
		if (sresp != null && Utils.getTotalHits(sresp) > 0) 
			return true; // there is at least one relationship between these two taxpayers
		
		 // no relationship between these two taxpayers
		
		return false;
	}
	
	private SearchResponse doSearch(SearchRequest searchRequest) throws Exception {
		final AtomicReference<SearchResponse> response = new AtomicReference<>();
		final AtomicInteger retries = new AtomicInteger();
		CommonErrors.doWithRetries(()->{
			response.set(MappingUtils.searchIgnoringNoMapError(elasticsearchClient, searchRequest, /*indexName*/searchRequest.indices()[0]));
		}, 
		CommonErrors.DEFAULT_DELAY_MS_BETWEEN_RETRIES, 
		/*tryAgain*/(ex)->retries.incrementAndGet()<=CommonErrors.DEFAULT_MAX_RETRIES 
			&& (CommonErrors.isErrorConnectionRefused(ex) || CommonErrors.isErrorRejectedExecution(ex)));
		
		return response.get();
	}
}
