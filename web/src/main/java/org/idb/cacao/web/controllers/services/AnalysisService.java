package org.idb.cacao.web.controllers.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
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
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.aggregations.metrics.MedianAbsoluteDeviation;
import org.elasticsearch.search.aggregations.metrics.Percentiles;
import org.elasticsearch.search.aggregations.metrics.Sum;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.idb.cacao.api.utils.IndexNamesUtils;
import org.idb.cacao.api.utils.ParserUtils;
import org.idb.cacao.web.dto.Account;
import org.idb.cacao.web.dto.AggregatedAccountingFlow;
import org.idb.cacao.web.dto.AnalysisData;
import org.idb.cacao.web.dto.AnalysisItem;
import org.idb.cacao.web.dto.BalanceSheet;
import org.idb.cacao.web.dto.Outlier;
import org.idb.cacao.web.dto.Shareholding;
import org.idb.cacao.web.dto.StatementIncomeItem;
import org.idb.cacao.web.dto.TaxpayerData;
import org.idb.cacao.web.utils.Script;
import org.idb.cacao.web.utils.SearchUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.i18n.LocaleContextHolder;
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
	private final String DECLARED_STATEMENT_INCOME_INDEX = IndexNamesUtils
			.formatIndexNameForPublishedData("Accounting Declared Statement Income");
	private final String SHAREHOLDING_INDEX = IndexNamesUtils.formatIndexNameForPublishedData("Accounting Shareholders");
	private final String TAXPAYER_INDEX = "cacao_taxpayers";
	
	private final static int SOURCE_JOURNAL = 1;
	private final static int SOURCE_DECLARED_INCOME_STATEMENT = 2;
	private final static int SOURCE_BOOTH_INCOME_STATEMENT = 3;
	private final static int SOURCE_SHAREHOLDERS = 4;
	
	private final String INDEX_PUBLISHED_ACCOUNTING_FLOW = IndexNamesUtils.formatIndexNameForPublishedData("Accounting Flow Daily");
	
	public final static int SEARCH_SHAREHOLDINGS = 1;
	public final static int SEARCH_SHAREHOLDERS = 2;

	/**
	 * Retrieves and return a balance sheet for a given taxpayer and period (month
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
	 * Build the query object used by {@link #getAccounts(String, YearMonth, boolean)}
	 * @param taxpayerId	A taxpayer to filter for
	 * @param year			A year to filter for
	 * @param period		A period to filter for
	 * 
	 *  @return A {@link SearchRequest} with all parameters and filters
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
								.field(translate("account_category_name") +".keyword")
								.subAggregation(AggregationBuilders.terms("bySubCategory").size(10_000)
										.field("account_subcategory.keyword")
										.subAggregation(AggregationBuilders.terms("bySubCategoryName").size(10_000)
												.field(translate("account_subcategory_name")+".keyword")
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
	 * @param taxpayerId	A taxpayer to filter for
	 * @param year			A year to filter for
	 * @param period		A period to filter for 
	 * @param fetchZeroBalance If true, fetches and return accounts with ZERO balance
	 * 
	 * @return A {@link List} of {@link Account}
	 */
	@Cacheable("accounts")
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
	 * @param accounts	A {@link List} of {@link Account} to add a subcategory
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

	/**
	 * Search and return a {@link Map} of accounts for a specific taxpayerid and period. 
	 * @param taxpayerId	Taxpayer id to search accounts for
	 * @param period		Period to be searched
	 * @param fetchZeroBalance	Indicates if accounts with ZERO balance will be included
	 * @param additionalPeriods	A list os additional periods to search balance values
	 * @return
	 */
	public List<Map<String, Object>> getMapOfAccounts(String taxpayerId, YearMonth period, boolean fetchZeroBalance,
			List<YearMonth> additionalPeriods) {

		//Get balance for initial period
		BalanceSheet balanceP0 = getBalance(taxpayerId, period, fetchZeroBalance);

		//If there is no accounts for the initial period, there is nothing to return
		if (balanceP0 == null || balanceP0.getAccounts() == null || balanceP0.getAccounts().isEmpty())
			return Collections.emptyList();
		
		Map<String, Map<String, Object>> accountsToRet = new HashMap<>();

		//For each account add balance and vertical analysis value
		for (Account account : balanceP0.getAccounts()) {
			String key = account.getKey();
			Map<String, Object> accountData = account.getAccountData();
			accountData.put("B0", account.getBalance());
			accountData.put("V0", account.getPercentage()); // Vertical analysis for base period
			accountsToRet.put(key, accountData);
		}

		int i = 1;
		//For each adittional period
		for (YearMonth p : additionalPeriods) {

			//Get balance for period
			BalanceSheet balance = getBalance(taxpayerId, p, fetchZeroBalance);

			//If there is no data for this period, go next
			if (balance == null || balance.getAccounts() == null || balance.getAccounts().isEmpty())
				continue;

			String prefix = "B" + i;
			String prefixVerticalAnalysis = "V" + i;
			String prefixHorizontalAnalysis = "H" + i;

			//For each account add balance, vertical and horizontal analysis values
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

		//Sort accounts 
		List<String> accountKeys = new ArrayList<>(accountsToRet.keySet());
		accountKeys.sort(null);
		List<Map<String, Object>> accounts = new ArrayList<>(accountKeys.size());
		accountKeys.forEach(key -> accounts.add(accountsToRet.get(key)));
		return accounts;
	}

	/**
	 * Build the query object used by {@link #getAccounts(String, int, String)}
	 * @param taxpayerIds	A {@link List} of taxpayers to tilter for
	 * @param sourceData	An indication of source data (index) to use
	 * @param year	A year to filter for	 * 
	 * @return A {@link SearchRequest} with all parameters and filters
	 */
	private SearchRequest searchComputedStatementIncome(final List<String> taxpayerIds, int sourceData, int year) {

		// Index over 'Accounting Computed Statement Income' objects
		SearchRequest searchRequest = new SearchRequest(SOURCE_JOURNAL == sourceData ? 
				COMPUTED_STATEMENT_INCOME_INDEX : DECLARED_STATEMENT_INCOME_INDEX);

		BoolQueryBuilder query = QueryBuilders.boolQuery();

		// Filter by taxpayer id
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
			AggregationBuilders.terms("byNumber").size(10_000).field("statement_number.keyword")
				.subAggregation(AggregationBuilders.terms("byName").size(10_000).field(translate("statement_name")+".keyword")
					.subAggregation(AggregationBuilders.percentiles("amount").field("amount_relative"))
					.subAggregation(AggregationBuilders.avg("average").field("amount_relative"))
					.subAggregation(AggregationBuilders.medianAbsoluteDeviation("deviation").field("amount_relative")
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
	 * Search and return a {@link Map} of values for a specific qualifier, qualifier
	 * value and year.
	 * 
	 * @param qualifier      A name of qualifier to search values
	 * @param qualifierValue A value for qualifier
	 * @param sourceData	 An indication of source values (index) to use
	 * @param year           A year of values to search
	 * @return A {@link Map} of values separated by taxpayers
	 */
	public AnalysisData getGeneralAnalysisValues(String qualifier, String qualifierValue, int sourceData, int year) {
		
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
		SearchRequest searchRequest = searchComputedStatementIncome(taxpayerIds, sourceData, year);

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

			Terms statementsNumbers = sresp.getAggregations().get("byNumber");

			for (Terms.Bucket statementNumberBucket : statementsNumbers.getBuckets()) {

				String statementNumber = statementNumberBucket.getKeyAsString();
				if (statementNumber == null || statementNumber.isEmpty() )
					continue;				

				Terms statementsNames = statementNumberBucket.getAggregations().get("byName");

				for (Terms.Bucket statementNameBucket : statementsNames.getBuckets()) {

					String statementName = statementNameBucket.getKeyAsString();
					if (statementName == null || statementName.isEmpty())
						continue;

					AnalysisItem analysisItem = new AnalysisItem();
					analysisItem.setStatementOrder(statementNumber);
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
				
			} // Loop over statement number

			// Add outliers
			for (AnalysisItem item : items) {
				addOutliers(item, sourceData, year, taxpayerIds);
			}
			
			AnalysisData data = new AnalysisData();			
			data.setItems(items);
			updateScale(data);

			return data;

		} // condition: got results from query

	}

	/**
	 * Calculate and store a scale in with graph will be displayed
	 * @param data	All data to be used in graph
	 */
	private void updateScale(AnalysisData data) {
		
		double min = -100d;
		double max = 200d;
		
		//Get max an min values in graph data
		for ( AnalysisItem item : data.getItems() ) {
			min = Math.min(min, item.getMin());
			max = Math.max(max, item.getMax());
		}
		
		//Add and subtract a value of 100 from max and min value 
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
			
				outlier.setStatementName(item.getStatementName());
				data.addOutlier(outlier);
				
				double value = outlier.getValue();
				if ( value < min )
					value = normalize(value, item.getMin(), item.getMax(), smaller, false /*superior*/);
				else if ( value > max )
					value = normalize(value, item.getMin(), item.getMax(), bigger, true /*superior*/);					
				
				value = Precision.round(value, 2, BigDecimal.ROUND_HALF_DOWN);
				
				Outlier copy = new Outlier(outlier.getTaxpayerId(),
						outlier.getTaxpayerName() + " : " + outlier.getValue(), item.getStatementName(), item.getStatementOrder(), value);
				item.addNormalizedOutlier(copy);				
			}
			
		}		
		
		//data.getOutliers().sort(null);		
		//data.getItems().sort(null);
	}	
	
	/**
	 * Normalize values to be used in graphics
	 * @param value	A value to compare
	 * @param min	Minimal value
	 * @param max	Maximal value
	 * @param limit	Limit for view (graph scale)
	 * @param superior	Indicates if it's about superior limit
	 * @return	A value between max/min and limit
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
	 * @param item 			Analysis item
	 * @param sourceData	An indication of source data (index) to use
	 * @param year 			Year of analysis
	 * @param taxpayerIds	A {@link List} of taxpayers to filter for
	 */
	private void addOutliers(AnalysisItem item, int sourceData, int year, List<String> taxpayerIds) {

		if (item.getStatementName() == null)
			return;

		//Add outliers for minimal value
		SearchRequest searchRequest = getRequestForOutliers(true /* min */, item.getStatementOrder(), item.getMin(),
				sourceData, year, taxpayerIds);

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

					Outlier outlier = new Outlier(taxpayerId, taxpayerName, item.getStatementName(), item.getStatementOrder(), value);
					item.addOutlier(outlier);

				}

			}

		}

		//Add outliers for maximal value
		searchRequest = getRequestForOutliers(false /* min */, item.getStatementOrder(), item.getMax(), sourceData, year, taxpayerIds);

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

					Outlier outlier = new Outlier(taxpayerId, taxpayerName, item.getStatementName(), item.getStatementOrder(), value);
					item.addOutlier(outlier);

				}

			}

		}

	}

	/**
	 * Create and return a search request for getting outliers
	 * 
	 * @param min	A minimal value to consider
	 * @param statement	A statement to search
	 * @param value	A value to compare
	 * @param sourceData	An indication of source data (index) to use 
	 * @param year	A year to filter for
	 * @param taxpayerIds	A list of taxpayers to filter for
	 * @return A {@link SearchRequest} with all configurations	
	 */
	private SearchRequest getRequestForOutliers(boolean min, String statement, double value, int sourceData, int year, List<String> taxpayerIds) {

		// Index over 'Accounting Computed Statement Income' objects
		SearchRequest searchRequest = new SearchRequest(SOURCE_JOURNAL == sourceData ? 
				COMPUTED_STATEMENT_INCOME_INDEX : DECLARED_STATEMENT_INCOME_INDEX);
		
		// Filter by statementCode
		BoolQueryBuilder query = QueryBuilders.boolQuery();
		query = query.must(new TermQueryBuilder("statement_number.keyword", statement));
		
		// Filter by taxpayer id
		BoolQueryBuilder subquery = QueryBuilders.boolQuery();
		for (String argument : taxpayerIds) {
			subquery = subquery.should(new TermQueryBuilder("taxpayer_id.keyword", argument));
		}
		subquery = subquery.minimumShouldMatch(1);
		query = query.must(subquery);		

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
		
		// Index over 'taxpayer' objects
		SearchRequest searchRequest = new SearchRequest(TAXPAYER_INDEX);
		
		// Filter by qualifier
		BoolQueryBuilder query = QueryBuilders.boolQuery();
		query = query.should(new TermQueryBuilder(qualifier + ".keyword", qualifierValue));

		// Configure the aggregations
		AbstractAggregationBuilder<?> aggregationBuilder = // Aggregates by taxpayerid field
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
			return Collections.emptyList(); // No data found
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
	@Cacheable("qualifierValues")
	public List<String> getQualifierValues(String qualifier) {

		//Index over 'taxpayer' objects
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
	 * @param sourceData	An indication of source data (index) to use
	 * 						If sourceData isn't #SOURCE_JOURNAL or #SOURCE_DECLARED_INCOME_STATEMENT, search for booth
	 * 						to return.
	 * 
	 * @return A list of years present in Accounting Statement Income indexes
	 */
	@Cacheable("years")	
	public List<Integer> getYears(int sourceData) {
		
		if ( sourceData == SOURCE_BOOTH_INCOME_STATEMENT ) {
			List<Integer> toRet = getYearsByIndex(SOURCE_JOURNAL);
			toRet.addAll(getYearsByIndex(SOURCE_DECLARED_INCOME_STATEMENT));
			return toRet.stream().distinct().map(Integer::intValue).sorted().collect(Collectors.toList());
		}
		
		return getYearsByIndex(sourceData);
		
	}

	/**
	 * 
	 * @param sourceData	An indication of source data (index) to use
	 * @return	A list of years present in Accounting Statement Income indexes
	 */
	private List<Integer> getYearsByIndex(int sourceData) {
		// Index over 'Accounting Computed Statement Income' objects
		String index = null;
		switch (sourceData) {
		case SOURCE_JOURNAL:
			index = COMPUTED_STATEMENT_INCOME_INDEX;
			break;
		case SOURCE_DECLARED_INCOME_STATEMENT:
			index = DECLARED_STATEMENT_INCOME_INDEX;
			break;
		case SOURCE_SHAREHOLDERS:
			index = SHAREHOLDING_INDEX;
			break;
		default:
			index = COMPUTED_STATEMENT_INCOME_INDEX;
			break;
		}
		SearchRequest searchRequest = new SearchRequest(index);

		//Configure the aggregations 
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

		List<Integer> values = new LinkedList<>();

		if (sresp == null || sresp.getHits().getTotalHits().value == 0) {
			log.log(Level.INFO, "No data found for years");
			return Collections.emptyList(); // No data found
		} else {

			// Let's fill the resulting list with values

			Terms yearValues = sresp.getAggregations().get("byYear");

			for (Terms.Bucket yearValueBucket : yearValues.getBuckets()) {

				Number value = yearValueBucket.getKeyAsNumber();
				if (value == null || value.intValue() == 0)
					continue;

				values.add(value.intValue());

			}

		}

		values.sort(null);
		return values;
	}

	public List<AggregatedAccountingFlow> getAccountingFlow(String taxpayerId, Date startDate, Date finalDate) {
		SearchRequest searchRequest = new SearchRequest(INDEX_PUBLISHED_ACCOUNTING_FLOW);

		// Filter by taxpayerId
		BoolQueryBuilder query = QueryBuilders.boolQuery();
		query = query.must(new TermQueryBuilder("taxpayer_id.keyword", taxpayerId));

		// Filter for period
		String from = ParserUtils.formatTimestampES(startDate);
		String to = ParserUtils.formatTimestampES(finalDate);
		RangeQueryBuilder dateRange = new RangeQueryBuilder("date")
				.from(from)
				.to(to);
		query = query.must(dateRange);

		String[] groupBy = {"credited_account_category.keyword", "credited_account_category_name.keyword", 
		            "credited_account_subcategory.keyword", "credited_account_subcategory_name.keyword",
		            "credited_account_code.keyword", "credited_account_name.keyword",
		            "debited_account_category.keyword", "debited_account_category_name.keyword",
		            "debited_account_subcategory.keyword", "debited_account_subcategory_name.keyword",
		            "debited_account_code.keyword", "debited_account_name.keyword" };
		
		AggregationBuilder metric = AggregationBuilders.sum("totalFlow").field("amount");
		
		AbstractAggregationBuilder<?> aggregationBuilder = SearchUtils.aggregationBuilder(null, groupBy, metric);

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(query)
				.aggregation(aggregationBuilder);

		// We are not interested on individual documents
		searchSourceBuilder.size(0);

		searchRequest.source(searchSourceBuilder);

		SearchResponse sresp = null;
		try {
			sresp = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
		} catch (Throwable ex) {
			log.log(Level.SEVERE, "Error getting flows", ex);
		}
		
		if (sresp == null || sresp.getHits().getTotalHits().value == 0) {
			log.log(Level.INFO, "No flows found for taxPayer " + taxpayerId + " for period from " + from + " to " + to);
			return Collections.emptyList(); // No flows found
		} 

		BiFunction<Aggregations, String[], AggregatedAccountingFlow> function = (agg, values) -> {
			Sum sum = agg.get("totalFlow");
			return new AggregatedAccountingFlow(values, sum.getValue());
		};
		
		List<AggregatedAccountingFlow> result = SearchUtils.collectAggregations(sresp.getAggregations(), groupBy, function);
		
		return result;

	}
	
	/**
	 * Build the query object used by {@link #getAccounts(String, int, String)}
	 * @param taxpayerId	A taxpayer to filter for
	 * @param sourceData	An indication of source data (index) to use
	 * @param year	A year to filter for	  
	 * @return A {@link SearchRequest} with all parameters and filters
	 */
	private SearchRequest searchComputedStatementIncomeValues(String taxpayerId, int sourceData, int year) {

		// Index over 'Accounting Computed Statement Income' objects
		SearchRequest searchRequest = new SearchRequest(SOURCE_JOURNAL == sourceData ? 
				COMPUTED_STATEMENT_INCOME_INDEX : DECLARED_STATEMENT_INCOME_INDEX);

		BoolQueryBuilder query = QueryBuilders.boolQuery();

		// Filter by taxpayer id
		BoolQueryBuilder subquery = QueryBuilders.boolQuery();
		List<String> taxpayerIds = Arrays.asList(taxpayerId);
		for (String argument : taxpayerIds) {
			subquery = subquery.should(new TermQueryBuilder("taxpayer_id.keyword", argument));
		}
		subquery = subquery.minimumShouldMatch(1);
		query = query.must(subquery);

		// Filter for year
		query = query.must(new TermQueryBuilder("year", year));

		// Configure the aggregations
		AbstractAggregationBuilder<?> aggregationBuilder = // Aggregates by first field
			AggregationBuilders.terms("byNumber").size(10_000).field("statement_number.keyword")
				.subAggregation(AggregationBuilders.terms("byName").size(10_000).field(translate("statement_name") + ".keyword")
					.subAggregation(AggregationBuilders.sum("amount").field("amount")
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
	 * Given a field name, translate it (add suffix) for current language
	 * @param fieldName	A field name to translate
	 * @return	Translated field name
	 */
	private String translate(String fieldName) {

		String language = LocaleContextHolder.getLocale().getLanguage();
		
		if ( "en".equalsIgnoreCase(language) )
			return fieldName;
		
		return fieldName + "_" + language;
	}

	/**
	 * Search and return Statement Income for a specified taxpayer and year
	 * @param taxpayerId	Taxpayer to search statement income
	 * @param year			Year to search statement income
	 * @return	A {@link List} of {@link StatementIncomeItem}
	 * 			
	 */
	public List<StatementIncomeItem> getStatementIncomeDeclaredAndCalculated(String taxpayerId, int year) {
		
		if (taxpayerId == null || taxpayerId.isEmpty()) {
			log.log(Level.INFO, "No valid taxpayer received");
			return null; // No data found
		}
		
		if (year == 0) {
			log.log(Level.INFO, "No valid year received");
			return null; // No data found
		}
		
		//Search statement income values
		Map<String,StatementIncomeItem> items = new HashMap<>();
		getStatementIncomeValues(taxpayerId, year, SOURCE_JOURNAL,  items);
		getStatementIncomeValues(taxpayerId, year, SOURCE_DECLARED_INCOME_STATEMENT, items);
		List<StatementIncomeItem> values = new ArrayList<>(items.values());
		values.sort(null);
		return values;
	}

	/**
	 * Search and store values for Stated Income calculated or declared, according with received parameters
	 * @param taxpayerId	A taxpayer to filter for
	 * @param year			A year to filter for
	 * @param sourceData	Indicates a source index (calculated or declared)
	 * @param items			A {@link Map} to store values
	 */
	private void getStatementIncomeValues(String taxpayerId, int year, int sourceData, Map<String,StatementIncomeItem> items) {
		
		//Define a search request according with parameters
		SearchRequest searchRequest = searchComputedStatementIncomeValues(taxpayerId, sourceData, year);
		
		// Execute a search
		SearchResponse sresp = null;
		try {
			sresp = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
		} catch (Throwable ex) {
			log.log(Level.SEVERE, "Error getting accounts", ex);
		}

		if (sresp == null || sresp.getHits().getTotalHits().value == 0) {
			log.log(Level.INFO, "No data found");
			return; // No data found		
		} else {

			//Add values for statement income items
			Terms statementsNumbers = sresp.getAggregations().get("byNumber");

			for (Terms.Bucket statementNumberBucket : statementsNumbers.getBuckets()) {

				String statementNumber = statementNumberBucket.getKeyAsString();
				if (statementNumber == null || statementNumber.isEmpty() )
					continue;				

				Terms statementsNames = statementNumberBucket.getAggregations().get("byName");

				for (Terms.Bucket statementNameBucket : statementsNames.getBuckets()) {

					String statementName = statementNameBucket.getKeyAsString();
					if (statementName == null || statementName.isEmpty())
						continue;
					
					Sum sum = statementNameBucket.getAggregations().get("amount");

					if (sum != null) {
						StatementIncomeItem item = items.getOrDefault(statementNumber, new StatementIncomeItem());
						item.setStatementOrder(statementNumber);
						item.setStatementName(statementName);
						if ( SOURCE_JOURNAL == sourceData)
							item.setCalculatedValue(sum.getValue());
						else
							item.setDeclaredValue(sum.getValue());
						item.setDifference(item.getDeclaredValue()-item.getCalculatedValue());
						items.put(statementNumber, item);
					}

				} // Loop over statement name
				
			} // Loop over statement number
			
		}
			
	}

	/**
	 * Search and return all data about a taxpayer specified in parameter
	 * @param taxpayerId	A taxpayer to search data for
	 * @param year			A specific year to search
	 * 
	 * @return	All data about a specified taxpayer
	 */
	public List<?> getTaxpayerData(String taxpayerId, int year, int searchType) {
		
		TaxpayerData data = new TaxpayerData();
		data.setTaxpayerId(taxpayerId);
		data.setYear(year);
		
		switch (searchType) {
		case SEARCH_SHAREHOLDINGS:
			addShareholdings(data);
			return data.getShareholdings();
		case SEARCH_SHAREHOLDERS:
			addShareholders(data);
			return data.getShareholders();			
		default:
			break;
		}
		
		return Collections.emptyList();
		
	}

	/**
	 * Search and add shareholding information for a specified taxpayer and year
	 * 
	 * @param data	Information about taxpayer and year
	 */
	private void addShareholdings(TaxpayerData data) {
		
		// Index over 'Accounting Computed Statement Income' objects
		SearchRequest searchRequest = new SearchRequest(SHAREHOLDING_INDEX);

		BoolQueryBuilder query = QueryBuilders.boolQuery();
		//Filter by taxpayer
		query = query.must(new TermQueryBuilder("taxpayer_id.keyword", data.getTaxpayerId()));

		// Filter for year
		query = query.must(new TermQueryBuilder("year", data.getYear()));
		
    	// Script in 'painless' language for identifying confirmations and returning the confirmed payment value
    	// We return '0' in case this property not being defined, so that we can have aggregation of 'zeroes' over
    	// slips with no corresponding confirmations.
    	org.elasticsearch.script.Script scriptletShareClass = new org.elasticsearch.script.Script(
    			  "if (doc['share_class.keyword'].size()==0) return '';"
    			  +"else return doc['share_class.keyword'].value; "); 		

		//Define group by fields
		Object[] groupBy = {
				"shareholder_id.keyword", 
				"shareholder_name.keyword", 
	            translate("share_type_name")+".keyword",
	            new Script(scriptletShareClass, "byShareClass") };
		
		//Define aggregations
		AggregationBuilder shareAmount = AggregationBuilders.sum("shareAmount").field("share_amount");
		AggregationBuilder sharePercentage = AggregationBuilders.sum("sharePercentage").field("share_percentage");
		AggregationBuilder shareQuantity = AggregationBuilders.sum("shareQuantity").field("share_quantity");
	
		AbstractAggregationBuilder<?> aggregationBuilder = SearchUtils.aggregationBuilder(null, groupBy, 
				shareAmount, sharePercentage, shareQuantity);

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(query)
				.aggregation(aggregationBuilder);

		// We are not interested on individual documents
		searchSourceBuilder.size(0);
		searchRequest.source(searchSourceBuilder);
		
		//Search results
		SearchResponse sresp = null;
		try {
			sresp = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
		} catch (Throwable ex) {
			log.log(Level.SEVERE, "Error getting flows", ex);
		}
		
		if (sresp == null || sresp.getHits().getTotalHits().value == 0) {
			log.log(Level.INFO, "No shaholding information found for taxPayer " + data.getTaxpayerId() + " for period " + data.getYear());
			return; // No flows found
		} 

		//Retrieve information from result
		BiFunction<Aggregations, String[], Shareholding> function = (agg, values) -> {
			Sum amount = agg.get("shareAmount");
			Sum percentage = agg.get("sharePercentage");
			Sum quantity = agg.get("shareQuantity");
			return new Shareholding(values, amount.getValue(), percentage.getValue(), quantity.getValue());
		};
		
		//Update shareholding information for this taxpayer
		data.setShareholdings(SearchUtils.collectAggregations(sresp.getAggregations(), groupBy, function));
		
	}
	
	/**
	 * Search and add shareholders information for a specified taxpayer and year
	 * 
	 * @param data	Information about taxpayer and year
	 */
	private void addShareholders(TaxpayerData data) {
		
		// Index over 'Accounting Computed Statement Income' objects
		SearchRequest searchRequest = new SearchRequest(SHAREHOLDING_INDEX);

		BoolQueryBuilder query = QueryBuilders.boolQuery();
		//Filter by taxpayer
		query = query.must(new TermQueryBuilder("shareholder_id.keyword", data.getTaxpayerId()));		

		// Filter for year
		query = query.must(new TermQueryBuilder("year", data.getYear()));
		
    	// Script in 'painless' language for identifying confirmations and returning the confirmed payment value
    	// We return '0' in case this property not being defined, so that we can have aggregation of 'zeroes' over
    	// slips with no corresponding confirmations.
    	org.elasticsearch.script.Script scriptletShareClass = new org.elasticsearch.script.Script(
    			  "if (doc['share_class.keyword'].size()==0) return '';"
    			  +"else return doc['share_class.keyword'].value; ");    	

		//Define group by fields
		Object[] groupBy = {
				"taxpayer_id.keyword", 
				"taxpayer_name.keyword", 
	            translate("share_type_name")+".keyword",
	            new Script(scriptletShareClass, "byShareClass") };
	
		//Define aggregations
		AggregationBuilder shareAmount = AggregationBuilders.sum("shareAmount").field("share_amount");
		AggregationBuilder sharePercentage = AggregationBuilders.sum("sharePercentage").field("share_percentage");
		AggregationBuilder shareQuantity = AggregationBuilders.sum("shareQuantity").field("share_quantity");
	
		AbstractAggregationBuilder<?> aggregationBuilder = SearchUtils.aggregationBuilder(null, groupBy, 
				shareAmount, sharePercentage, shareQuantity);

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(query)
				.aggregation(aggregationBuilder);

		// We are not interested on individual documents
		searchSourceBuilder.size(0);
		searchRequest.source(searchSourceBuilder);
		
		//Search results
		SearchResponse sresp = null;
		try {
			sresp = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
		} catch (Throwable ex) {
			log.log(Level.SEVERE, "Error getting flows", ex);
		}
		
		if (sresp == null || sresp.getHits().getTotalHits().value == 0) {
			log.log(Level.INFO, "No shaholding information found for taxPayer " + data.getTaxpayerId() + " for period " + data.getYear());
			return; // No flows found
		} 

		//Retrieve information from result
		BiFunction<Aggregations, String[], Shareholding> function = (agg, values) -> {
			Sum amount = agg.get("shareAmount");
			Sum percentage = agg.get("sharePercentage");
			Sum quantity = agg.get("shareQuantity");
			return new Shareholding(values, amount.getValue(), percentage.getValue(), quantity.getValue());
		};
		
		//Update shareholding information for this taxpayer
		data.setShareholders(SearchUtils.collectAggregations(sresp.getAggregations(), groupBy, function));
		
	}
		
}
