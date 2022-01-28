package org.idb.cacao.web.controllers.services;

import java.time.YearMonth;
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
import org.idb.cacao.api.utils.IndexNamesUtils;
import org.idb.cacao.web.dto.Account;
import org.idb.cacao.web.dto.BalanceSheet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AnalysisService {

	private static final Logger log = Logger.getLogger(AnalysisService.class.getName());

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
	 * @param period		Year and month of balance sheet
	 * @param fetchZeroBalance	If true, fetches and return accounts with ZERO balance
	 * @return		A balance sheet with it's accounts
	 */
	public BalanceSheet getBalance(String taxpayerId, YearMonth period, boolean fetchZeroBalance) {

		BalanceSheet balance = new BalanceSheet();
		balance.setTaxPayerId(taxpayerId);

		balance.setAccounts(getAccounts(taxpayerId,period,fetchZeroBalance));

		return balance;

	}

	/**
	 * Build the query object used by {@link #getAccounts(String, int, String)}
	 */
	private SearchRequest searchBalance(final String taxpayerId, YearMonth period) {
		
		// Index over 'balance sheet monthly' objects
		SearchRequest searchRequest = new SearchRequest(BALANCE_SHEET_INDEX);
		
		// Filter by taxpayerId
		BoolQueryBuilder query = QueryBuilders.boolQuery();
		query = query.must(new TermQueryBuilder("taxpayer_id.keyword", taxpayerId));

		// Filter for year
		query = query.must(new TermQueryBuilder("year", period.getYear()));

		// Filter for Month
		query = query.must(new TermQueryBuilder("month_number", period.getMonthValue()));

		// Configure the aggregations
		AbstractAggregationBuilder<?> aggregationBuilder = //Aggregates by first field
			AggregationBuilders.terms("byCategory").size(10_000).field("account_category.keyword")
				.subAggregation(AggregationBuilders.terms("byCategoryName").size(10_000).field("account_category_name.keyword")
					.subAggregation(AggregationBuilders.terms("bySubCategory").size(10_000).field("account_subcategory.keyword")
						.subAggregation(AggregationBuilders.terms("bySubCategoryName").size(10_000).field("account_subcategory_name.keyword")
							.subAggregation(AggregationBuilders.terms("byAccount").size(10_000).field("account_code.keyword")
								.subAggregation(AggregationBuilders.terms("byAccountName").size(10_000).field("account_name.keyword")
									.subAggregation(AggregationBuilders.sum("finalBalance").field("final_balance_with_sign"))
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
	 * @param fetchZeroBalance	If true, fetches and return accounts with ZERO balance
	 * @return	A {@link List} of {@link Account}
	 */
	public List<Account> getAccounts(final String taxpayerId, YearMonth period, boolean fetchZeroBalance) {

		//Create a search request
		SearchRequest searchRequest = searchBalance(taxpayerId, period);

		//Execute a search
		SearchResponse sresp = null;
		try {
			sresp = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
		} catch (Throwable ex) {
			log.log(Level.SEVERE, "Error getting accounts", ex);
		}

		List<Account> accounts = new LinkedList<>();

		if (sresp == null || sresp.getHits().getTotalHits().value == 0) {
			log.log(Level.INFO, "No accounts found for taxPayer " + taxpayerId + " for period " + period.toString());			
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
									
									Sum balance = accountNameBucket.getAggregations().get("finalBalance");
							
									if ( balance != null ) {											
										double balanceValue = balance.getValue();
										
										if ( !fetchZeroBalance && balanceValue == 0d )
											continue;
										
										//Add each account
										Account account = new Account();
										account.setCategoryCode(category);
										account.setCategory(categoryName);
										account.setSubcategoryCode(subcategory);
										account.setSubcategory(subcategoryName);
										account.setCode(accountCode);
										account.setName(accountName);
										account.setBalance(balanceValue);
										account.setLevel(3);
										accounts.add(account);
									}										
									
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
			account.getCategoryCode(), Collectors.groupingBy(account -> account.getCategory(), Collectors.summingDouble(account->account.getBalance()))));
		
		Map<String,Double> categoriesSum = new HashMap<>();
		
		//Iterate over all categories
		for ( Map.Entry<String, Map<String,Double>> entry : result.entrySet() ) {
			
			Account account = new Account();
			account.setLevel(1);			
			account.setCategoryCode(entry.getKey());
			account.setCategory(new ArrayList<>(entry.getValue().keySet()).get(0));
			account.setBalance(new ArrayList<>(entry.getValue().values()).get(0));
			account.setPercentage(100);			
			accounts.add(account);			
			categoriesSum.put(account.getCategoryCode(), account.getBalance());
			
		}
		
		//Group all subcategories
		Map<String,Map<String,Map<String,Map<String,Double>>>> resultSubcategories = 
				accounts.stream().filter(account->account.getSubcategory()!=null)
					.collect(
						Collectors.groupingBy(account -> account.getCategoryCode(), 
								Collectors.groupingBy(account -> account.getCategory(), 
										Collectors.groupingBy(account -> account.getSubcategoryCode(), 
												Collectors.groupingBy(account -> account.getSubcategory(),										
														Collectors.summingDouble(account->account.getBalance()))))));
	
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
						account.setBalance(entry4.getValue());
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
			double percent = (account.getBalance() * 100) / total;
			if ( Double.isNaN(percent) )
				account.setPercentage(0);
			else
				account.setPercentage(percent);
			
		});
		
	}

	public List<Map<String, Object>> getMapOfAccounts(String taxpayerId, YearMonth period, boolean fetchZeroBalance,
			List<YearMonth> additionalPeriods) {
		
		BalanceSheet balanceP0 = getBalance(taxpayerId,period,fetchZeroBalance);
		
		if ( balanceP0 == null || balanceP0.getAccounts() == null || balanceP0.getAccounts().isEmpty() )
			return Collections.emptyList();
		
		Map<String,Map<String,Object>> accountsToRet = new HashMap<>();
		
		for ( Account account : balanceP0.getAccounts() ) {		
			String key = account.getKey();
			Map<String,Object> accountData = account.getAccountData();
			accountData.put("B0",account.getBalance());
			accountData.put("V0", account.getPercentage()); //Vertical analysis for base period
			accountsToRet.put(key, accountData);			
		}
		
		
		int i = 1;
		for ( YearMonth p : additionalPeriods ) {			
		
			BalanceSheet balance = getBalance(taxpayerId,p,fetchZeroBalance);
			
			if ( balance == null || balance.getAccounts() == null || balance.getAccounts().isEmpty() )
				continue;
			
			String prefix = "B" + i;
			String prefixVerticalAnalysis =  "V" +  i;
			String prefixHorizontalAnalysis =  "H" +  i;
			
			for ( Account account : balance.getAccounts() ) {		
				String key = account.getKey();
				Map<String,Object> accountData = accountsToRet.get(key);
				
				if ( accountData == null ) {
					accountData = account.getAccountData();					
					accountsToRet.put(key, accountData);
				}					
					
				accountData.put(prefix , account.getBalance()); //Balance for period
				accountData.put(prefixVerticalAnalysis , account.getPercentage()); //Vertical analysis for period
				double balanceBasePeriod = accountData.get("B0") == null ? 0 : (double)accountData.get("B0"); 
				double percentage = balanceBasePeriod == 0d ? 0 : (account.getBalance() * 100 / balanceBasePeriod);
				accountData.put(prefixHorizontalAnalysis , percentage); //Horizontal analysis for period
							
			}
			
			i++;
			
		}
		
		List<String> accountKeys = new ArrayList<>(accountsToRet.keySet());
		accountKeys.sort(null);
		List<Map<String, Object>> accounts = new ArrayList<>(accountKeys.size());
		accountKeys.forEach(key->accounts.add(accountsToRet.get(key)));
		return accounts;
	}

}