package org.idb.cacao.web.controllers.services;

import java.math.BigDecimal;
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

import org.apache.commons.math3.util.Precision;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.aggregations.metrics.MedianAbsoluteDeviation;
import org.elasticsearch.search.aggregations.metrics.Percentiles;
import org.elasticsearch.search.aggregations.metrics.Sum;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.idb.cacao.api.utils.IndexNamesUtils;
import org.idb.cacao.web.dto.Account;
import org.idb.cacao.web.dto.AnalysisData;
import org.idb.cacao.web.dto.AnalysisItem;
import org.idb.cacao.web.dto.BalanceSheet;
import org.idb.cacao.web.dto.Outlier;
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
	private final String COMPUTED_STATEMENT_INCOME_INDEX = IndexNamesUtils
			.formatIndexNameForPublishedData("Accounting Computed Statement Income");
	private final String TAXPAYER_INDEX = "cacao_taxpayers";

	/**
	 * Retrieves and return a balanece sheet for a given taxpayer and period (month
	 * and year)
	 * 
	 * @param taxpayerId       Taxpayer for select a balance sheet
	 * @param period           Year and month of balance sheet
	 * @param fetchZeroBalance If true, fetches and return accounts with ZERO
	 *                         balance
	 * @return A balance sheet with it's accounts
	 */
	public BalanceSheet getBalance(String taxpayerId, YearMonth period, boolean fetchZeroBalance) {

		BalanceSheet balance = new BalanceSheet();
		balance.setTaxPayerId(taxpayerId);

		balance.setAccounts(getAccounts(taxpayerId, period, fetchZeroBalance));

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
		AbstractAggregationBuilder<?> aggregationBuilder = // Aggregates by first field
				AggregationBuilders.terms("byCategory").size(10_000).field("account_category.keyword")
						.subAggregation(AggregationBuilders.terms("byCategoryName").size(10_000)
								.field("account_category_name.keyword")
								.subAggregation(AggregationBuilders.terms("bySubCategory").size(10_000)
										.field("account_subcategory.keyword")
										.subAggregation(AggregationBuilders.terms("bySubCategoryName").size(10_000)
												.field("account_subcategory_name.keyword")
												.subAggregation(AggregationBuilders.terms("byAccount").size(10_000)
														.field("account_code.keyword")
														.subAggregation(AggregationBuilders.terms("byAccountName")
																.size(10_000).field("account_name.keyword")
																.subAggregation(AggregationBuilders.sum("finalBalance")
																		.field("final_balance_with_sign")))))));

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(query)
				.aggregation(aggregationBuilder);

		// We are not interested on individual documents
		searchSourceBuilder.size(0);

		searchRequest.source(searchSourceBuilder);

		return searchRequest;
	}

	/**
	 * Retrive and return a list of accounts with it's attributes for conditions on
	 * parameters
	 * 
	 * @param taxpayerId
	 * @param year
	 * @param month
	 * @param fetchZeroBalance If true, fetches and return accounts with ZERO
	 *                         balance
	 * @return A {@link List} of {@link Account}
	 */
	public List<Account> getAccounts(final String taxpayerId, YearMonth period, boolean fetchZeroBalance) {

		// Create a search request
		SearchRequest searchRequest = searchBalance(taxpayerId, period);

		// Execute a search
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

			// Let's fill the resulting list with values

			Terms categories = sresp.getAggregations().get("byCategory");
			for (Terms.Bucket categoryBucket : categories.getBuckets()) {

				String category = categoryBucket.getKeyAsString();
				if (category == null || category.isEmpty())
					continue;

				Map<String, Object> values = new HashMap<>();
				values.put("byCategory", category);

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

							Terms accountCodes = subcategoryNameBucket.getAggregations().get("byAccount");

							for (Terms.Bucket accountCodeBucket : accountCodes.getBuckets()) {

								String accountCode = accountCodeBucket.getKeyAsString();
								if (accountCode == null || accountCode.isEmpty())
									continue;

								Terms accountNames = accountCodeBucket.getAggregations().get("byAccountName");

								for (Terms.Bucket accountNameBucket : accountNames.getBuckets()) {

									String accountName = accountNameBucket.getKeyAsString();
									if (accountName == null || accountName.isEmpty())
										continue;

									Sum balance = accountNameBucket.getAggregations().get("finalBalance");

									if (balance != null) {
										double balanceValue = balance.getValue();

										if (!fetchZeroBalance && balanceValue == 0d)
											continue;

										// Add each account
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

								} // Loop over account_name

							} // Loop over account_code

						} // Loop over account_subcategory_name

					} // Loop over account_subcategory

				} // Loop over account_category_name

			} // Loop over account_category

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

		// Group all categories
		Map<String, Map<String, Double>> result = accounts.stream()
				.collect(Collectors.groupingBy(account -> account.getCategoryCode(), Collectors.groupingBy(
						account -> account.getCategory(), Collectors.summingDouble(account -> account.getBalance()))));

		Map<String, Double> categoriesSum = new HashMap<>();

		// Iterate over all categories
		for (Map.Entry<String, Map<String, Double>> entry : result.entrySet()) {

			Account account = new Account();
			account.setLevel(1);
			account.setCategoryCode(entry.getKey());
			account.setCategory(new ArrayList<>(entry.getValue().keySet()).get(0));
			account.setBalance(new ArrayList<>(entry.getValue().values()).get(0));
			account.setPercentage(100);
			accounts.add(account);
			categoriesSum.put(account.getCategoryCode(), account.getBalance());

		}

		// Group all subcategories
		Map<String, Map<String, Map<String, Map<String, Double>>>> resultSubcategories = accounts.stream()
				.filter(account -> account.getSubcategory() != null)
				.collect(Collectors.groupingBy(account -> account.getCategoryCode(),
						Collectors.groupingBy(account -> account.getCategory(),
								Collectors.groupingBy(account -> account.getSubcategoryCode(),
										Collectors.groupingBy(account -> account.getSubcategory(),
												Collectors.summingDouble(account -> account.getBalance()))))));

		Map<String, Double> subcategoriesSum = new HashMap<>();

		// Iterate over all subcategories
		for (Map.Entry<String, Map<String, Map<String, Map<String, Double>>>> entry : resultSubcategories.entrySet()) {

			String categoryCode = entry.getKey();

			for (Map.Entry<String, Map<String, Map<String, Double>>> entry2 : entry.getValue().entrySet()) {

				String categoryName = entry2.getKey();

				for (Map.Entry<String, Map<String, Double>> entry3 : entry2.getValue().entrySet()) {

					String subcategoryCode = entry3.getKey();

					for (Map.Entry<String, Double> entry4 : entry3.getValue().entrySet()) {

						Account account = new Account();
						account.setLevel(2);
						account.setCategoryCode(categoryCode);
						account.setCategory(categoryName);
						account.setSubcategoryCode(subcategoryCode);
						account.setSubcategory(entry4.getKey());
						account.setBalance(entry4.getValue());
						double total = categoriesSum.get(categoryCode);
						double percent = (entry4.getValue() * 100) / total;
						if (Double.isNaN(percent))
							account.setPercentage(0);
						else
							account.setPercentage(percent);
						accounts.add(account);
						subcategoriesSum.put(subcategoryCode, entry4.getValue());
					}
				}

			}

		}

		// Add percentage of each account
		accounts.stream().filter(a -> a.getLevel() > 2).forEach(account -> {

			double total = categoriesSum.get(account.getCategoryCode());
			double percent = (account.getBalance() * 100) / total;
			if (Double.isNaN(percent))
				account.setPercentage(0);
			else
				account.setPercentage(percent);

		});

	}

	public List<Map<String, Object>> getMapOfAccounts(String taxpayerId, YearMonth period, boolean fetchZeroBalance,
			List<YearMonth> additionalPeriods) {

		BalanceSheet balanceP0 = getBalance(taxpayerId, period, fetchZeroBalance);

		if (balanceP0 == null || balanceP0.getAccounts() == null || balanceP0.getAccounts().isEmpty())
			return Collections.emptyList();

		Map<String, Map<String, Object>> accountsToRet = new HashMap<>();

		for (Account account : balanceP0.getAccounts()) {
			String key = account.getKey();
			Map<String, Object> accountData = account.getAccountData();
			accountData.put("B0", account.getBalance());
			accountData.put("V0", account.getPercentage()); // Vertical analysis for base period
			accountsToRet.put(key, accountData);
		}

		int i = 1;
		for (YearMonth p : additionalPeriods) {

			BalanceSheet balance = getBalance(taxpayerId, p, fetchZeroBalance);

			if (balance == null || balance.getAccounts() == null || balance.getAccounts().isEmpty())
				continue;

			String prefix = "B" + i;
			String prefixVerticalAnalysis = "V" + i;
			String prefixHorizontalAnalysis = "H" + i;

			for (Account account : balance.getAccounts()) {
				String key = account.getKey();
				Map<String, Object> accountData = accountsToRet.get(key);

				if (accountData == null) {
					accountData = account.getAccountData();
					accountsToRet.put(key, accountData);
				}

				accountData.put(prefix, account.getBalance()); // Balance for period
				accountData.put(prefixVerticalAnalysis, account.getPercentage()); // Vertical analysis for period
				double balanceBasePeriod = accountData.get("B0") == null ? 0 : (double) accountData.get("B0");
				double percentage = balanceBasePeriod == 0d ? 0 : (account.getBalance() * 100 / balanceBasePeriod);
				accountData.put(prefixHorizontalAnalysis, percentage); // Horizontal analysis for period

			}

			i++;

		}

		List<String> accountKeys = new ArrayList<>(accountsToRet.keySet());
		accountKeys.sort(null);
		List<Map<String, Object>> accounts = new ArrayList<>(accountKeys.size());
		accountKeys.forEach(key -> accounts.add(accountsToRet.get(key)));
		return accounts;
	}

	/**
	 * Build the query object used by {@link #getAccounts(String, int, String)}
	 */
	private SearchRequest searchComputedStatementIncome(final List<String> taxpayerIds, int year) {

		// Index over 'Accounting Computed Statement Income' objects
		SearchRequest searchRequest = new SearchRequest(COMPUTED_STATEMENT_INCOME_INDEX);

		// Filter by taxpayerId
		BoolQueryBuilder query = QueryBuilders.boolQuery();

		BoolQueryBuilder subquery = QueryBuilders.boolQuery();
		for (String argument : taxpayerIds) {
			subquery = subquery.should(new TermQueryBuilder("taxpayer_id.keyword", argument));
		}
		subquery = subquery.minimumShouldMatch(1);
		query = query.must(subquery);

		// Filter for year
		query = query.must(new TermQueryBuilder("year", year));

		// Configure the aggregations
		AbstractAggregationBuilder<?> aggregationBuilder = // Aggregates by first field
				AggregationBuilders.terms("byCode").size(10_000).field("statement_entry_code.keyword")
						.subAggregation(AggregationBuilders.terms("byName").size(10_000).field("statement_name.keyword")
								.subAggregation(AggregationBuilders.percentiles("amount").field("amount_relative"))
								.subAggregation(AggregationBuilders.avg("average").field("amount_relative"))
								.subAggregation(AggregationBuilders.medianAbsoluteDeviation("deviation").field("amount_relative"))
								);

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(query)
				.aggregation(aggregationBuilder);

		// We are not interested on individual documents
		searchSourceBuilder.size(0);

		searchRequest.source(searchSourceBuilder);

		return searchRequest;

	}

	/**
	 * Search and return a {@link Map} of values for a specific qualifier, qualifier
	 * value and year.
	 * 
	 * @param qualifier      A name of qualifier to search values
	 * @param qualifierValue A value for qualifier
	 * @param year           A year of values to search
	 * @return A {@link Map} of values separated by taxpayers
	 */
	public AnalysisData getGeneralAnalysisValues(String qualifier, String qualifierValue, int year) {
		
		if ( qualifier == null || qualifier.isEmpty() || qualifierValue == null || qualifierValue.isEmpty() ) {
			log.log(Level.SEVERE, "No parameters provided");
			return null;
		}

		List<String> taxpayerIds = getTaxPayersId(qualifier, qualifierValue);

		if (taxpayerIds == null || taxpayerIds.isEmpty()) {
			log.log(Level.INFO, "No taxpayers found for qualifier " + qualifier + " for year " + year);
			return null; // No data found
		}

		// Create a search request
		SearchRequest searchRequest = searchComputedStatementIncome(taxpayerIds, year);

		// Execute a search
		SearchResponse sresp = null;
		try {
			sresp = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
		} catch (Throwable ex) {
			log.log(Level.SEVERE, "Error getting accounts", ex);
		}

		if (sresp == null || sresp.getHits().getTotalHits().value == 0) {
			log.log(Level.INFO, "No data found for qualifier " + qualifier + " for year " + year);
			return null; // No data found		
		} else {

			// Let's fill the resulting list with values

			List<AnalysisItem> items = new LinkedList<>();

			Terms statementsCodes = sresp.getAggregations().get("byCode");

			for (Terms.Bucket statementCodeBucket : statementsCodes.getBuckets()) {

				String statementCode = statementCodeBucket.getKeyAsString();
				if (statementCode == null || statementCode.isEmpty())
					continue;

				Terms statementsNames = statementCodeBucket.getAggregations().get("byName");

				for (Terms.Bucket statementNameBucket : statementsNames.getBuckets()) {

					String statementName = statementNameBucket.getKeyAsString();
					if (statementName == null || statementName.isEmpty())
						continue;

					AnalysisItem analysisItem = new AnalysisItem();
					analysisItem.setStatementCode(statementCode);
					analysisItem.setStatementName(statementName);
					
					Avg average = statementNameBucket.getAggregations().get("average");
					MedianAbsoluteDeviation deviation = statementNameBucket.getAggregations().get("deviation");
					
					double averegaValue = average == null ? 0d: Precision.round(average.getValue(), 2, BigDecimal.ROUND_HALF_DOWN);
					double deviationValue = deviation == null ? 0d : Precision.round(deviation.value(), 2, BigDecimal.ROUND_HALF_DOWN);

					Percentiles percentile = statementNameBucket.getAggregations().get("amount");
					if (percentile != null) {

						percentile.forEach(item -> {
							double percent = item.getPercent();

							if (percent == 25) // First quartile
								analysisItem.setQ1(Precision.round(item.getValue(), 2, BigDecimal.ROUND_HALF_DOWN));
							else if (percent == 50) // Median
								analysisItem.setMedian(Precision.round(item.getValue(), 2, BigDecimal.ROUND_HALF_DOWN));
							else if (percent == 75) // Third quartile
								analysisItem.setQ3(Precision.round(item.getValue(), 2, BigDecimal.ROUND_HALF_DOWN));

						});

						if (!Double.isNaN(analysisItem.getQ1()) && !Double.isNaN(analysisItem.getQ3())
								&& analysisItem.getQ1() != 0 && analysisItem.getQ3() != 0) {
							analysisItem.setAverage(averegaValue);
							analysisItem.setDeviation(deviationValue);
							items.add(analysisItem);
						}

					}

				} // Loop over statement name

			} // Loop over statement code

			// Add outliers
			for (AnalysisItem item : items) {
				addOutliers(item, year);
			}
			
			AnalysisData data = new AnalysisData();
			data.setItems(items);
			updateScale(data);

			return data;

		} // condition: got results from query

	}

	private void updateScale(AnalysisData data) {
		
		double min = -100d;
		double max = 200d;
		
		for ( AnalysisItem item : data.getItems() ) {
			min = Math.min(min, item.getMin());
			max = Math.max(max, item.getMax());
		}
		
		data.setScaleMin(min-100);
		data.setScaleMax(max+100);		
		
		//Normalize data
		
		for ( AnalysisItem item : data.getItems() ) {
			
			double bigger = 0;
			double smaller = 0;
			for ( Outlier outlier : item.getOutliers() ) {
				bigger = Math.max(bigger, outlier.getValue());
				smaller = Math.min(smaller, outlier.getValue());
			}
			
			for ( Outlier outlier : item.getOutliers() ) {
				
				double value = outlier.getValue();
				if ( value < min )
					value = normalize(value, item.getMin(), item.getMax(), smaller, false /*superior*/);
				else if ( value > max )
					value = normalize(value, item.getMin(), item.getMax(), bigger, true /*superior*/);					
				
				value = Precision.round(value, 2, BigDecimal.ROUND_HALF_DOWN);
				
				Outlier copy = new Outlier(outlier.getTaxpayerId(),
						outlier.getTaxpayerName() + " : " + value, value);
				item.addNormalizedOutlier(copy);				
			}
			
		}
	}	
	
	/**
	 * Normalize values to be user in graphics
	 * @param value
	 * @param min
	 * @param max
	 * @param limit
	 * @param superior
	 * @return
	 */
	private double normalize(double value, double min, double max, double limit, boolean superior) {
		
		if ( superior ) {
		
			double viewLimit = (max + 100) * 0.9d;
			return Precision.round(( ( (value - max) * viewLimit) / (limit-max)), 2, BigDecimal.ROUND_HALF_DOWN);
					
		}
		
		double viewLimit = (min - 100) * 0.9d;
		return Precision.round(( ( (value - min) * viewLimit ) / (limit-min)), 2, BigDecimal.ROUND_HALF_DOWN);
 		
	}	

	/**
	 * Add outliers to analysis data
	 * 
	 * @param item Analysis item
	 * @param year Year of analysis
	 */
	private void addOutliers(AnalysisItem item, int year) {

		if (item.getStatementName() == null)
			return;

		// Add outliers for minimal value
		SearchRequest searchRequest = getRequestForOutliers(true /* min */, item.getStatementCode(), item.getMin(),
				year);

		// Execute a search
		SearchResponse sresp = null;
		try {
			sresp = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
		} catch (Throwable ex) {
			log.log(Level.SEVERE, "Error getting outliers", ex);
		}

		if (sresp != null && sresp.getHits().getTotalHits().value > 0) {

			// Let's fill the resulting list with values

			Terms taxpayersIds = sresp.getAggregations().get("byTaxpayerId");

			for (Terms.Bucket taxpayerIdBucket : taxpayersIds.getBuckets()) {

				String taxpayerId = taxpayerIdBucket.getKeyAsString();
				if (taxpayerId == null || taxpayerId.isEmpty())
					continue;

				Terms taxpayersNames = taxpayerIdBucket.getAggregations().get("byTaxpayerName");

				for (Terms.Bucket taxpayerNameBucket : taxpayersNames.getBuckets()) {

					String taxpayerName = taxpayerNameBucket.getKeyAsString();

					Sum amount = taxpayerNameBucket.getAggregations().get("amount");

					double value = 0;
					if (amount != null) {
						value = Precision.round(amount.getValue(), 2, BigDecimal.ROUND_HALF_DOWN);
					}

					Outlier outlier = new Outlier(taxpayerId, taxpayerName, value);
					item.addOutilier(outlier);

				}

			}

		}

		// Add outliers for maximal value
		searchRequest = getRequestForOutliers(false /* min */, item.getStatementCode(), item.getMax(), year);

		// Execute a search
		sresp = null;
		try {
			sresp = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
		} catch (Throwable ex) {
			log.log(Level.SEVERE, "Error getting outliers", ex);
		}

		if (sresp != null && sresp.getHits().getTotalHits().value > 0) {

			// Let's fill the resulting list with values

			Terms taxpayersIds = sresp.getAggregations().get("byTaxpayerId");

			for (Terms.Bucket taxpayerIdBucket : taxpayersIds.getBuckets()) {

				String taxpayerId = taxpayerIdBucket.getKeyAsString();
				if (taxpayerId == null || taxpayerId.isEmpty())
					continue;

				Terms taxpayersNames = taxpayerIdBucket.getAggregations().get("byTaxpayerName");

				for (Terms.Bucket taxpayerNameBucket : taxpayersNames.getBuckets()) {

					String taxpayerName = taxpayerNameBucket.getKeyAsString();

					Sum amount = taxpayerNameBucket.getAggregations().get("amount");

					double value = 0;
					if (amount != null) {
						value = Precision.round(amount.getValue(), 2, BigDecimal.ROUND_HALF_DOWN);
					}

					Outlier outlier = new Outlier(taxpayerId, taxpayerName, value);
					item.addOutilier(outlier);

				}

			}

		}

	}

	/**
	 * Create and return a search request for getting outliers
	 * 
	 * @param min
	 * @param statementCode
	 * @param value
	 * @param year
	 * @return A {@link SearchRequest}
	 */
	private SearchRequest getRequestForOutliers(boolean min, String statementCode, double value, int year) {

		// Index over 'Accounting Computed Statement Income' objects
		SearchRequest searchRequest = new SearchRequest(COMPUTED_STATEMENT_INCOME_INDEX);

		// Filter by statementCode
		BoolQueryBuilder query = QueryBuilders.boolQuery();
		query = query.must(new TermQueryBuilder("statement_entry_code.keyword", statementCode));

		// Filter by year
		query = query.must(new TermQueryBuilder("year", year));

		// Filter by value
		if (min)
			query = query.must(new RangeQueryBuilder("amount_relative").from(0.01).to(value));
		else
			query = query.must(new RangeQueryBuilder("amount_relative").from(value));

		// Configure the aggregations
		AbstractAggregationBuilder<?> aggregationBuilder = // Aggregates by first field
				AggregationBuilders.terms("byTaxpayerId").size(10_000).field("taxpayer_id.keyword").subAggregation(
						AggregationBuilders.terms("byTaxpayerName").size(10_000).field("taxpayer_name.keyword")
								.subAggregation(AggregationBuilders.sum("amount").field("amount_relative")));

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(query)
				.aggregation(aggregationBuilder);

		// We are not interested on individual documents
		searchSourceBuilder.size(0);

		searchRequest.source(searchSourceBuilder);

		return searchRequest;
	}

	/**
	 * Search and return a list of taxpayers for a a qualifier and qualifier value
	 * 
	 * @param qualifier
	 * @param qualifierValue
	 * @return A {@link List} of taxpayers
	 */
	private List<String> getTaxPayersId(String qualifier, String qualifierValue) {
		
		// Index over 'balance sheet monthly' objects
		SearchRequest searchRequest = new SearchRequest(TAXPAYER_INDEX);
		
		// Filter by qualifier
		BoolQueryBuilder query = QueryBuilders.boolQuery();
		query = query.should(new TermQueryBuilder(qualifier + ".keyword", qualifierValue));

		// Configure the aggregations
		AbstractAggregationBuilder<?> aggregationBuilder = // Aggregates by first field
				AggregationBuilders.terms("byTaxpayer").size(10_000).field("taxPayerId");

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(query)
				.aggregation(aggregationBuilder);

		// We are not interested on individual documents
		searchSourceBuilder.size(0);

		searchRequest.source(searchSourceBuilder);

		// Execute a search
		SearchResponse sresp = null;
		try {
			sresp = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
		} catch (Throwable ex) {
			log.log(Level.SEVERE, "Error getting accounts", ex);
		}

		List<String> taxpayers = new LinkedList<>();

		if (sresp == null || sresp.getHits().getTotalHits().value == 0) {
			log.log(Level.INFO, "No data found for qualifier " + qualifier + " with value " + qualifierValue);
			return Collections.emptyList(); // No balance sheet found
		} else {

			// Let's fill the resulting list with values

			Terms taxpayersIds = sresp.getAggregations().get("byTaxpayer");

			for (Terms.Bucket taxpayerBucket : taxpayersIds.getBuckets()) {

				String taxpayerId = taxpayerBucket.getKeyAsString();
				if (taxpayerId == null || taxpayerId.isEmpty())
					continue;

				taxpayers.add(taxpayerId);

			}

		}

		return taxpayers;
	}

	/**
	 * Search and return values for a specified qualifier
	 * 
	 * @param qualifier Qualifier to search
	 * @return A {@link List} of values for specified qualifier
	 */
	public List<String> getQualifierValues(String qualifier) {

		// Index over 'taxpayer' objects
		SearchRequest searchRequest = new SearchRequest(TAXPAYER_INDEX);

		// Configure the aggregations
		AbstractAggregationBuilder<?> aggregationBuilder = // Aggregates by qualifier field
				AggregationBuilders.terms("byQualifierValue").size(10_000).field(qualifier + ".keyword");

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().aggregation(aggregationBuilder);

		// We are not interested on individual documents
		searchSourceBuilder.size(0);

		searchRequest.source(searchSourceBuilder);

		// Execute a search
		SearchResponse sresp = null;
		try {
			sresp = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
		} catch (Throwable ex) {
			log.log(Level.SEVERE, "Error getting qualifier values", ex);
		}

		List<String> values = new LinkedList<>();

		if (sresp == null || sresp.getHits().getTotalHits().value == 0) {
			log.log(Level.INFO, "No data found for qualifier " + qualifier);
			return Collections.emptyList(); // No data found
		} else {

			// Let's fill the resulting list with values

			Terms qualifierValues = sresp.getAggregations().get("byQualifierValue");

			for (Terms.Bucket qualifierValueBucket : qualifierValues.getBuckets()) {

				String value = qualifierValueBucket.getKeyAsString();
				if (value == null || value.isEmpty())
					continue;

				values.add(value);

			}

		}

		values.sort(null);
		return values;

	}

	/**
	 * 
	 * @return A list of years present in Accounting Computed Statement Income index
	 */
	public List<String> getYears() {
		// Index over 'Accounting Computed Statement Income' objects
		SearchRequest searchRequest = new SearchRequest(COMPUTED_STATEMENT_INCOME_INDEX);

		// Configure the aggregations
		AbstractAggregationBuilder<?> aggregationBuilder = // Aggregates by qualifier field
				AggregationBuilders.terms("byYear").size(10_000).field("year");

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().aggregation(aggregationBuilder);

		// We are not interested on individual documents
		searchSourceBuilder.size(0);

		searchRequest.source(searchSourceBuilder);

		// Execute a search
		SearchResponse sresp = null;
		try {
			sresp = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
		} catch (Throwable ex) {
			log.log(Level.SEVERE, "Error getting year values", ex);
		}

		List<String> values = new LinkedList<>();

		if (sresp == null || sresp.getHits().getTotalHits().value == 0) {
			log.log(Level.INFO, "No data found for years");
			return Collections.emptyList(); // No data found
		} else {

			// Let's fill the resulting list with values

			Terms yearValues = sresp.getAggregations().get("byYear");

			for (Terms.Bucket yearValueBucket : yearValues.getBuckets()) {

				String value = yearValueBucket.getKeyAsString();
				if (value == null || value.isEmpty())
					continue;

				values.add(value);

			}

		}

		values.sort(null);
		return values;
	}

}