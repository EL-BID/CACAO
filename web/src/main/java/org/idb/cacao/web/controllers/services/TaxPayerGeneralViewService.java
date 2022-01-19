package org.idb.cacao.web.controllers.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
import org.elasticsearch.search.aggregations.metrics.Sum;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.idb.cacao.api.utils.DateTimeUtils;
import org.idb.cacao.api.utils.IndexNamesUtils;
import org.idb.cacao.web.dto.Account;
import org.idb.cacao.web.dto.BalanceSheet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaxPayerGeneralViewService {

	private static final Logger log = Logger.getLogger(TaxPayerGeneralViewService.class.getName());

	/**
	 * Maximum number of records that we will return for a given parameter. Should
	 * not be too high because it would compromise queries whenever used as query
	 * criteria
	 */
	public static final int MAX_TAXPAYERS_PER_TAXMANAGER = 10_000;

	@Autowired
	private RestHighLevelClient elasticsearchClient;

	private final String BALANCE_SHEET_INDEX = IndexNamesUtils.formatIndexNameForPublishedData("Balance Sheet Monthly");
	
	/**
	 * Retrieves and return a balanece sheet for a given taxpayer and period (month and year) 
	 * @param taxpayerId	Taxpayer for select a balance sheet
	 * @param year			Year of balance sheet
	 * @param month			Month of balance sheet
	 * @return		A balance sheet with it's accounts
	 */
	public BalanceSheet getBalance(String taxpayerId, int year, String month) {

		BalanceSheet balance = new BalanceSheet();
		balance.setTaxPayerId("1234567");
		balance.setTaxPayerName("Taxpayer name");
		balance.setInitialDate(DateTimeUtils.now());
		balance.setFinalDate(DateTimeUtils.now());

		balance.setAccounts(getAccounts(taxpayerId,year,month));

		return balance;

	}

	/**
	 * Build the query object used by {@link #getAccounts(String, int, String)}
	 */
	private SearchRequest searchBalance(final String taxpayerId, final int year, final String month) {
		
		// Index over 'balance sheet monthly' objects
		SearchRequest searchRequest = new SearchRequest(BALANCE_SHEET_INDEX);

		// Filter by timestamp (only consider recent months for uploads)
		BoolQueryBuilder query = QueryBuilders.boolQuery();

		// Filter by taxpayer Id (depends on user profile and UI advanced filters)
		BoolQueryBuilder subquery = QueryBuilders.boolQuery();
		subquery = subquery.should(new TermQueryBuilder("taxPayerId.keyword", taxpayerId));
		subquery = subquery.minimumShouldMatch(1);
		//query = query.must(subquery);

		// Filter for year
		query = query.must(new TermQueryBuilder("year", year));

		// Filter for Month
		query = query.must(new TermQueryBuilder("month.keyword", month.toLowerCase()));

		// Configure the aggregations
		AbstractAggregationBuilder<?> aggregationBuilder = //Aggregates by first field
			AggregationBuilders.terms("byCategory").size(10_000).field("account_category.keyword")
				.subAggregation(AggregationBuilders.terms("byCategoryName").size(10_000).field("account_category_name.keyword")
					.subAggregation(AggregationBuilders.terms("bySubCategory").size(10_000).field("account_subcategory.keyword")
						.subAggregation(AggregationBuilders.terms("bySubCategoryName").size(10_000).field("account_subcategory_name.keyword")
							.subAggregation(AggregationBuilders.terms("byAccount").size(10_000).field("account_code.keyword")
								.subAggregation(AggregationBuilders.terms("byAccountName").size(10_000).field("account_name.keyword")
									.subAggregation(AggregationBuilders.terms("byFinalBalenceDebitCredit").size(10_000).field("final_balance_debit_credit.keyword")
										.subAggregation(AggregationBuilders.sum("finalBalance").field("final_balance"))
									)
								)
							)
						)
					)
				);

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(query)
				.aggregation(aggregationBuilder);

		// We are not interested on individual documents
		searchSourceBuilder.size(0);

		searchRequest.source(searchSourceBuilder);


		return searchRequest;
	}

	/**
	 * Retrive and return a list of accounts with it's attributes for conditions on parameters 
	 * @param taxpayerId
	 * @param year
	 * @param month
	 * @return	A {@link List} of {@link Account}
	 */
	public List<Account> getAccounts(final String taxpayerId, final int year, final String month) {

		//Create a search request
		SearchRequest searchRequest = searchBalance(taxpayerId, year, month);

		//Execute a search
		SearchResponse sresp = null;
		try {
			sresp = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
		} catch (Throwable ex) {
			log.log(Level.SEVERE, "Error getting accounts", ex);
		}

		List<Account> accounts = new LinkedList<>();

		if (sresp.getHits().getTotalHits().value == 0) {
			log.log(Level.INFO, "No accounts found for taxPayer " + taxpayerId + " for period " + month + "/" + year);			
			return Collections.emptyList(); // No balance sheet found
		} else {
			
			//Let's fill the resulting list with values
			
			Terms categories = sresp.getAggregations().get("byCategory");
			for (Terms.Bucket categoryBucket : categories.getBuckets()) {
			
				String category = categoryBucket.getKeyAsString();
				if (category == null || category.isEmpty())
					continue;

				Map<String,Object> values = new HashMap<>();
				values.put("byCategory",category);
				
				Terms categoryNames = categoryBucket.getAggregations().get("byCategoryName");

				for (Terms.Bucket categoryNameBucket : categoryNames.getBuckets()) {
					
					String categoryName = categoryNameBucket.getKeyAsString();
					if (categoryName == null || categoryName.isEmpty())
						continue;
					
					Terms subcategories = categoryNameBucket.getAggregations().get("bySubCategory");

					for (Terms.Bucket subcategoryBucket : subcategories.getBuckets()) {

						String subcategory = subcategoryBucket.getKeyAsString();
						if (subcategory == null || subcategory.isEmpty())
							continue;
						
						Terms subcategoryNames = subcategoryBucket.getAggregations().get("bySubCategoryName");

						for (Terms.Bucket subcategoryNameBucket : subcategoryNames.getBuckets()) {

							String subcategoryName = subcategoryNameBucket.getKeyAsString();
							if (subcategoryName == null || subcategoryName.isEmpty())
								continue;
							
							Terms accountCodes= subcategoryNameBucket.getAggregations().get("byAccount");

							for (Terms.Bucket accountCodeBucket : accountCodes.getBuckets()) {

								String accountCode = accountCodeBucket.getKeyAsString();
								if (accountCode == null || accountCode.isEmpty())
									continue;
								
								Terms accountNames= accountCodeBucket.getAggregations().get("byAccountName");

								for (Terms.Bucket accountNameBucket : accountNames.getBuckets()) {

									String accountName = accountNameBucket.getKeyAsString();
									if (accountName == null || accountName.isEmpty())
										continue;
									
									Terms debitCredit = accountNameBucket.getAggregations().get("byFinalBalenceDebitCredit");

									for (Terms.Bucket balanceTypeBucket : debitCredit.getBuckets()) {

										String balanceType = balanceTypeBucket.getKeyAsString();
										if (balanceType == null || balanceType.isEmpty())
											continue;										
										
										Sum balance = balanceTypeBucket.getAggregations().get("finalBalance");
										double balanceValue = balance.getValue();
										
										//Add each account
										Account account = new Account();
										account.setCategoryCode(category);
										account.setCategory(categoryName);
										account.setSubcategoryCode(subcategory);
										account.setSubcategory(subcategoryName);
										account.setCode(accountCode);
										account.setName(accountName);
										account.setFinalBalance(balanceValue);
										account.setLevel(3);
										accounts.add(account);
										
									} //Loop over balance_final_debit_credit
									
								} //Loop over account_name
								
							} //Loop over account_code
							
						} //Loop over account_subcategory_name
						
					} //Loop over account_subcategory

				} //Loop over account_category_name
				
			} //Loop over account_category
			
		} // condition: got results from query

		addCategorySubcategoryData(accounts);
		
		accounts.sort(null);
		
		return accounts;
	}

	/**
	 * Add informations about category, subcategory and percentage of each account 
	 * 
	 * @param accounts
	 */
	private void addCategorySubcategoryData(List<Account> accounts) {
		
		//Group all categories
		Map<String,Map<String,Double>> result = accounts.stream().collect(Collectors.groupingBy(account -> 
			account.getCategoryCode(), Collectors.groupingBy(account -> account.getCategory(), Collectors.summingDouble(account->account.getFinalBalance()))));
		
		Map<String,Double> categoriesSum = new HashMap<>();
		
		//Iterate over all categories
		for ( Map.Entry<String, Map<String,Double>> entry : result.entrySet() ) {
			
			Account account = new Account();
			account.setLevel(1);			
			account.setCategoryCode(entry.getKey());
			account.setCategory(new ArrayList<>(entry.getValue().keySet()).get(0));
			account.setFinalBalance(new ArrayList<>(entry.getValue().values()).get(0));
			account.setPercentage(100);			
			accounts.add(account);			
			categoriesSum.put(account.getCategoryCode(), account.getFinalBalance());
			
		}
		
		//Group all subcategories
		Map<String,Map<String,Map<String,Map<String,Double>>>> resultSubcategories = 
				accounts.stream().filter(account->account.getSubcategory()!=null)
					.collect(
						Collectors.groupingBy(account -> account.getCategoryCode(), 
								Collectors.groupingBy(account -> account.getCategory(), 
										Collectors.groupingBy(account -> account.getSubcategoryCode(), 
												Collectors.groupingBy(account -> account.getSubcategory(),										
														Collectors.summingDouble(account->account.getFinalBalance()))))));
	
		Map<String,Double> subcategoriesSum = new HashMap<>();
	
		//Iterate over all subcategories
		for ( Map.Entry<String,Map<String,Map<String,Map<String,Double>>>> entry : resultSubcategories.entrySet() ) {
			
			String categoryCode = entry.getKey();
			
			for ( Map.Entry<String,Map<String,Map<String,Double>>> entry2 : entry.getValue().entrySet() ) {
			
				String categoryName = entry2.getKey();
				
				for ( Map.Entry<String,Map<String,Double>> entry3 : entry2.getValue().entrySet() ) {
					
					String subcategoryCode = entry3.getKey();
					
					for ( Map.Entry<String,Double> entry4 : entry3.getValue().entrySet() ) {
						
						Account account = new Account();
						account.setLevel(2);			
						account.setCategoryCode(categoryCode);
						account.setCategory(categoryName);						
						account.setSubcategoryCode(subcategoryCode);
						account.setSubcategory(entry4.getKey());
						account.setFinalBalance(entry4.getValue());
						double total = categoriesSum.get(categoryCode);
						double percent = (entry4.getValue() * 100) / total;
						if ( Double.isNaN(percent) )
							account.setPercentage(0);
						else
							account.setPercentage(percent);
						accounts.add(account);			
						subcategoriesSum.put(subcategoryCode, entry4.getValue());			
					}
				}
				
			}
			
		}
		
		//Add percentage of each account 
		accounts.stream().filter(a->a.getLevel() > 2).forEach(account->{
			
			double total = categoriesSum.get(account.getCategoryCode());			
			double percent = (account.getFinalBalance() * 100) / total;
			if ( Double.isNaN(percent) )
				account.setPercentage(0);
			else
				account.setPercentage(percent);
			
		});
		
	}

}
