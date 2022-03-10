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

import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
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
import org.elasticsearch.search.aggregations.PipelineAggregatorBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.aggregations.metrics.MedianAbsoluteDeviation;
import org.elasticsearch.search.aggregations.metrics.Percentiles;
import org.elasticsearch.search.aggregations.metrics.Sum;
import org.elasticsearch.search.aggregations.pipeline.BucketSortPipelineAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.idb.cacao.api.utils.IndexNamesUtils;
import org.idb.cacao.api.utils.ParserUtils;
import org.idb.cacao.api.utils.Utils;
import org.idb.cacao.web.dto.Account;
import org.idb.cacao.web.dto.AggregatedAccountingFlow;
import org.idb.cacao.web.dto.AnalysisData;
import org.idb.cacao.web.dto.AnalysisItem;
import org.idb.cacao.web.dto.BalanceSheet;
import org.idb.cacao.web.dto.CustomerVsSupplier;
import org.idb.cacao.web.dto.Outlier;
import org.idb.cacao.web.dto.Shareholding;
import org.idb.cacao.web.dto.StatementIncomeItem;
import org.idb.cacao.web.utils.ErrorUtils;
import org.idb.cacao.web.utils.FormatUtils;
import org.idb.cacao.web.utils.Script;
import org.idb.cacao.web.utils.SearchUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Service methods to get values for analysis over taxpayers informations 
 * 
 * @author Rivelino Patrício
 *
 */
@Service
public class AnalysisService {

	private static final Logger log = Logger.getLogger(AnalysisService.class.getName());

	private static final String ACCOUNT_SUBCATEGORY_TAX_PROVISION = "LIABILITY_PROVISION_TAX";
	private static final String ACCOUNT_CATEGORY_EXPENSE = "EXPENSE";

	@Autowired
	private RestHighLevelClient elasticsearchClient;
	
	@Autowired
	private Environment env;
	
	@Autowired
	private MessageSource messageSource;

	private final String BALANCE_SHEET_INDEX = IndexNamesUtils.formatIndexNameForPublishedData("Balance Sheet Monthly");
	private final String COMPUTED_STATEMENT_INCOME_INDEX = IndexNamesUtils
			.formatIndexNameForPublishedData("Accounting Computed Statement Income");
	private final String DECLARED_STATEMENT_INCOME_INDEX = IndexNamesUtils
			.formatIndexNameForPublishedData("Accounting Declared Statement Income");
	private final String SHAREHOLDING_INDEX = IndexNamesUtils
			.formatIndexNameForPublishedData("Accounting Shareholding");
	public static final String CUSTOMERS_INDEX = IndexNamesUtils
			.formatIndexNameForPublishedData("Accounting Customers");
	public static final String SUPPLIERS_INDEX = IndexNamesUtils
			.formatIndexNameForPublishedData("Accounting Suppliers");
	private static final String TAXPAYER_INDEX = "cacao_taxpayers";

	private static final int SOURCE_JOURNAL = 1;
	private static final int SOURCE_DECLARED_INCOME_STATEMENT = 2;
	private static final int SOURCE_BOOTH_INCOME_STATEMENT = 3;
	private static final int SOURCE_SHAREHOLDERS = 4;
	private static final int SOURCE_BOOTH_INCOME_STATEMENT_AND_SHAREHOLDERS = 5;

	private final String INDEX_PUBLISHED_ACCOUNTING_FLOW = IndexNamesUtils
			.formatIndexNameForPublishedData("Accounting Flow Daily");

	public static final int SEARCH_SHAREHOLDINGS = 1;
	public static final int SEARCH_SHAREHOLDERS = 2;
	public static final int REVENUE_NET_AND_GROSS_PROFIT_DECLARED = 3;
	public static final int REVENUE_NET_AND_GROSS_PROFIT_COMPUTED = 4;
	public static final int TAX_PROVISION = 5;
	public static final int ANALYTICS_ACCOUNTS = 6;
	public static final int CUSTOMERS = 7;
	public static final int SUPPLIERS = 8;

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
	 * Build the query object used by
	 * {@link #getAccounts(String, YearMonth, boolean)}
	 * 
	 * @param taxpayerId A taxpayer to filter for
	 * @param year       A year to filter for
	 * @param period     A period to filter for
	 * @param groupBy	 Fields do group by
	 * @param metric	 Field to sum value
	 * @return A {@link SearchRequest} with all parameters and filters
	 */
	private SearchRequest searchBalance(final String taxpayerId, YearMonth period, String[] groupBy, AggregationBuilder metric) {

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
		AbstractAggregationBuilder<?> aggregationBuilder = SearchUtils.aggregationBuilder(null, groupBy, metric);
		buildSearchSourceBuilder(query, aggregationBuilder, searchRequest);
		return searchRequest;
	}

	/**
	 * Retrive and return a list of accounts with it's attributes for conditions on
	 * parameters
	 * 
	 * @param taxpayerId       A taxpayer to filter for
	 * @param year             A year to filter for
	 * @param period           A period to filter for
	 * @param fetchZeroBalance If true, fetches and return accounts with ZERO
	 *                         balance
	 * 
	 * @return A {@link List} of {@link Account}
	 */
	@Cacheable("accounts")
	public List<Account> getAccounts(final String taxpayerId, YearMonth period, boolean fetchZeroBalance) {

		String[] groupBy = {"account_category.keyword", translate("account_category_name") + ".keyword",
				"account_subcategory.keyword", translate("account_subcategory_name") + ".keyword", 
				"account_code.keyword", "account_name.keyword"};
		
		AggregationBuilder metric = AggregationBuilders.sum("finalBalance").field("final_balance_with_sign");
		
		// Create a search request
		SearchRequest searchRequest = searchBalance(taxpayerId, period, groupBy, metric);

		// Execute a search
		SearchResponse sresp = doSearch(searchRequest);
		if (sresp == null) {
			log.log(Level.INFO, () -> "No accounts found for taxPayer " + taxpayerId + " for period " + period.toString());
			return Collections.emptyList(); // No balance sheet found
		}
		
		BiFunction<Aggregations, String[], Account> function = (agg, values) -> {
			Sum sum = agg.get("finalBalance");
			return new Account(values, sum.getValue());
		};		
			
		List<Account> accounts = SearchUtils.collectAggregations(sresp.getAggregations(), groupBy, function);
		addCategorySubcategoryData(accounts);
		accounts.sort(null);
		return accounts;
	}

	/**
	 * Add informations about category, subcategory and percentage of each account
	 * 
	 * @param accounts A {@link List} of {@link Account} to add a subcategory
	 */
	private void addCategorySubcategoryData(List<Account> accounts) {

		// Group all categories
		Map<String, Map<String, Double>> result = accounts.stream()
				.collect(Collectors.groupingBy(Account::getCategoryCode, Collectors.groupingBy(
						Account::getCategory, Collectors.summingDouble(Account::getBalance))));

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
				.collect(Collectors.groupingBy(Account::getCategoryCode,
						Collectors.groupingBy(Account::getCategory,
								Collectors.groupingBy(Account::getSubcategoryCode,
										Collectors.groupingBy(Account::getSubcategory,
												Collectors.summingDouble(Account::getBalance))))));

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
	 * Search and return a {@link Map} of accounts for a specific taxpayerid and
	 * period.
	 * 
	 * @param taxpayerId        Taxpayer id to search accounts for
	 * @param period            Period to be searched
	 * @param fetchZeroBalance  Indicates if accounts with ZERO balance will be
	 *                          included
	 * @param additionalPeriods A list os additional periods to search balance
	 *                          values
	 * @return	A {@link List} of {@link Map} of accounts with it's attributes 
	 */
	public List<Map<String, Object>> getMapOfAccounts(String taxpayerId, YearMonth period, boolean fetchZeroBalance,
			List<YearMonth> additionalPeriods) {

		// Get balance for initial period
		BalanceSheet balanceP0 = getBalance(taxpayerId, period, fetchZeroBalance);

		// If there is no accounts for the initial period, there is nothing to return
		if (balanceP0 == null || balanceP0.getAccounts() == null || balanceP0.getAccounts().isEmpty())
			return Collections.emptyList();

		Map<String, Map<String, Object>> accountsToRet = new HashMap<>();

		// For each account add balance and vertical analysis value
		for (Account account : balanceP0.getAccounts()) {
			String key = account.getKey();
			Map<String, Object> accountData = account.getAccountData();
			accountData.put("B0", account.getBalance());
			accountData.put("V0", account.getPercentage()); // Vertical analysis for base period
			accountsToRet.put(key, accountData);
		}

		int i = 1;
		// For each additional period
		for (YearMonth p : additionalPeriods) {

			// Get balance for period
			BalanceSheet balance = getBalance(taxpayerId, p, fetchZeroBalance);

			// If there is no data for this period, go next
			if (balance == null || balance.getAccounts() == null || balance.getAccounts().isEmpty())
				continue;

			String prefix = "B" + i;
			String prefixVerticalAnalysis = "V" + i;
			String prefixHorizontalAnalysis = "H" + i;

			// For each account add balance, vertical and horizontal analysis values
			for (Account account : balance.getAccounts()) {
				String key = account.getKey();
				Map<String, Object> accountData = accountsToRet.get(key);

				if (accountData == null) {
					accountData = account.getAccountData();
					accountsToRet.put(key, accountData);
				}

				accountData.put(prefix, account.getBalance()); // Balance for period
				accountData.put(prefixVerticalAnalysis, account.getPercentage()); // Vertical analysis for period
				double balanceBasePeriod = accountData.get("B0") == null ? 0 : Precision.round((double) accountData.get("B0"), 2, RoundingMode.HALF_DOWN.ordinal());
				double percentage = ( balanceBasePeriod == 0d ) ? 0 : (account.getBalance() * 100 / balanceBasePeriod);
				accountData.put(prefixHorizontalAnalysis, percentage); // Horizontal analysis for period

			}

			i++;

		}

		// Sort accounts
		List<String> accountKeys = new ArrayList<>(accountsToRet.keySet());
		accountKeys.sort(null);
		List<Map<String, Object>> accounts = new ArrayList<>(accountKeys.size());
		accountKeys.forEach(key -> accounts.add(accountsToRet.get(key)));
		return accounts;
	}

	/**
	 * Build the query object used by {@link #getAccounts(String, int, String)}
	 * 
	 * @param taxpayerIds A {@link List} of taxpayers to tilter for
	 * @param sourceData  An indication of source data (index) to use
	 * 		Valid values for sourceData are:
	 * 		1 - SOURCE_JOURNAL
	 * 		2 - SOURCE_DECLARED_INCOME_STATEMENT
	 * @param year        A year to filter for 
	 * 
	 * @return A {@link SearchRequest} with all parameters and filters
	 */
	private SearchRequest searchComputedStatementIncome(final List<String> taxpayerIds, int sourceData, int year, 
			String[] groupBy, AggregationBuilder[] metrics) {

		// Index over 'Accounting Computed Statement Income' objects
		SearchRequest searchRequest = new SearchRequest(
				SOURCE_JOURNAL == sourceData ? COMPUTED_STATEMENT_INCOME_INDEX : DECLARED_STATEMENT_INCOME_INDEX);

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
		
		AbstractAggregationBuilder<?> aggregationBuilder = SearchUtils.aggregationBuilder(null, groupBy, metrics);
		
		// Configure the aggregations
//		AbstractAggregationBuilder<?> aggregationBuilder = // Aggregates by first field
//				AggregationBuilders.terms("byNumber").size(10_000).field("statement_number.keyword")
//						.subAggregation(AggregationBuilders.terms("byName").size(10_000)
//								.field(translate("statement_name") + ".keyword")
//								.subAggregation(AggregationBuilders.sum("sum").field("amount"))
//								.subAggregation(AggregationBuilders.percentiles("amount").field("amount_relative"))
//								.subAggregation(AggregationBuilders.avg("average").field("amount_relative"))
//								.subAggregation(AggregationBuilders.medianAbsoluteDeviation("deviation")
//										.field("amount_relative")));		
		
		buildSearchSourceBuilder(query, aggregationBuilder, searchRequest);
		return searchRequest;

	}

	/**
	 * Search and return a {@link Map} of values for a specific qualifier, qualifier
	 * value and year.
	 * 
	 * @param qualifier      A name of qualifier to search values
	 * @param qualifierValue A value for qualifier
	 * @param sourceData     An indication of source values (index) to use
	 * 		Valid values for sourceData are:
	 * 		1 - SOURCE_JOURNAL
	 * 		2 - SOURCE_DECLARED_INCOME_STATEMENT
	 * 		3 - SOURCE_BOOTH_INCOME_STATEMENT
	 * 		4 - SOURCE_SHAREHOLDERS 
	 * @param year           A year of values to search
	 * 
	 * @return A {@link AnalysisData} with all data about all taxpayers for
	 *         specified parameters
	 */
	public AnalysisData getGeneralAnalysisValues(String qualifier, String qualifierValue, int sourceData, int year) {

		if (qualifier == null || qualifier.isEmpty() || qualifierValue == null || qualifierValue.isEmpty()) {
			log.log(Level.SEVERE, "No parameters provided");
			return null;
		}

		List<String> taxpayerIds = getTaxPayersId(qualifier, qualifierValue);

		if (taxpayerIds == null || taxpayerIds.isEmpty()) {
			log.log(Level.INFO, () -> "No taxpayers found for qualifier " + qualifier + " for year " + year);
			return null; // No data found
		}
		
		//String[] groupBy = {"byNumber", "byName"};
		String[] groupBy = {"statement_number.keyword",translate("statement_name") + ".keyword"};
		AggregationBuilder[] metrics = {
			AggregationBuilders.sum("sum").field("amount"),
			AggregationBuilders.percentiles("amount").field("amount_relative"),
			AggregationBuilders.avg("average").field("amount_relative"),
			AggregationBuilders.medianAbsoluteDeviation("deviation").field("amount_relative")
		};		

		// Create a search request
		SearchRequest searchRequest = searchComputedStatementIncome(taxpayerIds, sourceData, year, groupBy, metrics);

		// Execute a search
		SearchResponse sresp = doSearch(searchRequest);
		if (sresp == null) {
			log.log(Level.INFO, () -> "No data found for qualifier " + qualifier + " for year " + year);
			return null; // No data found
		}

		// Let's fill the resulting list with values
		List<AnalysisItem> items = fillGeneralAnalysisItems(sresp, groupBy);

		// Add outliers
		for (AnalysisItem item : items) {
			addOutliers(item, sourceData, year, taxpayerIds);
		}

		AnalysisData data = new AnalysisData();
		data.setItems(items);
		updateScale(data);
		data.setTotalTaxpayers(FormatUtils.getQuantityFormat().format(taxpayerIds.size()));

		return data;

	}
	
	/**
	 * For each item from {@link SearchResponse}, build a new {@link AnalysisItem} object 
	 * @param sresp	A search response with data from ES database
	 * @param groupBy	Fields returned by ES
	 * @return	A {@link List} of {@link AnalysisItem}
	 */
	private List<AnalysisItem> fillGeneralAnalysisItems(SearchResponse sresp, Object[] groupBy) {

		// Let's fill the resulting list with values
		// Retrieve information from result
		BiFunction<Aggregations, String[], AnalysisItem> function = (agg, values) -> {
			Sum sum = agg.get("sum");			
			Avg average = agg.get("average");
			MedianAbsoluteDeviation deviation = agg.get("deviation");
			Percentiles percentile = agg.get("amount");
			
			double sumValue = sum == null ? 0d
					: Precision.round(sum.getValue(), 2, RoundingMode.HALF_DOWN.ordinal());
			double averegaValue = average == null ? 0d
					: Precision.round(average.getValue(), 2, RoundingMode.HALF_DOWN.ordinal());
			double deviationValue = deviation == null ? 0d
					: Precision.round(deviation.value(), 2, RoundingMode.HALF_DOWN.ordinal());			
			
			AnalysisItem analysisItem = new AnalysisItem(values, sumValue, averegaValue, deviationValue, percentile);
			
			if (!Double.isNaN(analysisItem.getQ1()) && !Double.isNaN(analysisItem.getQ3())
					&& analysisItem.getQ1() != 0 && analysisItem.getQ3() != 0) {
				analysisItem.setSum(sumValue);
				analysisItem.setAverage(averegaValue);
				analysisItem.setDeviation(deviationValue);							
				return analysisItem;
			}			
			return null;
		};

		// Update outlier information for this taxpayer
		List<AnalysisItem> allItems = SearchUtils.collectAggregations(sresp.getAggregations(), groupBy, function);
		return allItems.stream().filter(Objects::nonNull).collect(Collectors.toList());
		
	}

	/**
	 * Calculate and store a scale in with graph will be displayed
	 * 
	 * @param data All data to be used in graph
	 */
	private void updateScale(AnalysisData data) {

		double min = -100d;
		double max = 200d;

		// Get max an min values in graph data
		for (AnalysisItem item : data.getItems()) {
			min = Math.min(min, Double.isNaN(item.getMin()) ? 0 : item.getMin());
			max = Math.max(max, Double.isNaN(item.getMax()) ? 0 : item.getMax());
		}

		// Add and subtract a value of 100 from max and min value
		data.setScaleMin(min - 100);
		data.setScaleMax(max + 100);

		// Normalize data
		for (AnalysisItem item : data.getItems()) {

			double bigger = 0;
			double smaller = 0;
			for (Outlier outlier : item.getOutliers()) {
				bigger = Math.max(bigger, outlier.getValue());
				smaller = Math.min(smaller, outlier.getValue());
			}

			for (Outlier outlier : item.getOutliers()) {

				outlier.setStatementName(item.getStatementName());
				data.addOutlier(outlier);

				double value = outlier.getValue();
				if (value < min)
					value = normalize(value, item.getMin(), item.getMax(), smaller, false /* superior */);
				else if (value > max)
					value = normalize(value, item.getMin(), item.getMax(), bigger, true /* superior */);

				value = Precision.round(value, 2, RoundingMode.HALF_DOWN.ordinal());

				Outlier copy = new Outlier(outlier.getTaxpayerId(),
						outlier.getTaxpayerName() + " : " + FormatUtils.getPercentageFormat().format(outlier.getValue()/100), 
						item.getStatementName(), item.getStatementOrder(), value);
				item.addNormalizedOutlier(copy);
			}

		}
	}

	/**
	 * Normalize values to be used in graphics
	 * 
	 * @param value    A value to compare
	 * @param min      Minimal value
	 * @param max      Maximal value
	 * @param limit    Limit for view (graph scale)
	 * @param superior Indicates if it's about superior limit
	 * @return A value between max/min and limit
	 */
	private double normalize(double value, double min, double max, double limit, boolean superior) {

		if (superior) {

			double viewLimit = (max + 100) * 0.9d;
			return Precision.round((((value - max) * viewLimit) / (limit - max)), 2, RoundingMode.HALF_DOWN.ordinal());

		}

		double viewLimit = (min - 100) * 0.9d;
		return Precision.round((((value - min) * viewLimit) / (limit - min)), 2, RoundingMode.HALF_DOWN.ordinal());

	}

	/**
	 * Add outliers to analysis data
	 * 
	 * @param item        Analysis item
	 * @param sourceData  An indication of source data (index) to use
	 * @param year        Year of analysis
	 * @param taxpayerIds A {@link List} of taxpayers to filter for
	 */
	private void addOutliers(AnalysisItem item, int sourceData, int year, List<String> taxpayerIds) {

		if (item.getStatementName() == null)
			return;
		
		// Define group by fields
		String[] groupBy = { "taxpayer_id.keyword", "taxpayer_name.keyword" };		

		// Add outliers for minimal value
		SearchRequest searchRequest = getRequestForOutliers(true /* min */, item.getStatementOrder(), item.getMin(),
				sourceData, year, taxpayerIds, groupBy);
		
		// Execute a search
		SearchResponse sresp = doSearch(searchRequest);
		if (sresp != null) {
			fillOutliers(sresp,item,groupBy);
		}

		// Add outliers for maximal value
		searchRequest = getRequestForOutliers(false /* min */, item.getStatementOrder(), item.getMax(), sourceData,
				year, taxpayerIds, groupBy);

		// Execute a search
		sresp = doSearch(searchRequest);

		if (sresp != null) {			
			fillOutliers(sresp,item,groupBy);
		}

	}

	/**
	 * Fill {@link Outlier} objects with 
	 * @param sresp
	 * @param item
	 * @param groupBy
	 */
	private void fillOutliers(SearchResponse sresp, AnalysisItem item, Object[] groupBy) {

		// Let's fill the resulting list with values
		// Retrieve information from result
		BiFunction<Aggregations, String[], Outlier> function = (agg, values) -> {
			Sum amount = agg.get("amount");
			return new Outlier(values, item, amount.getValue());
		};

		// Update outlier information for this taxpayer
		SearchUtils.collectAggregations(sresp.getAggregations(), groupBy, function);
		
	}

	/**
	 * Create and return a search request for getting outliers
	 * 
	 * @param min         A minimal value to consider
	 * @param statement   A statement to search
	 * @param value       A value to compare
	 * @param sourceData  An indication of source data (index) to use
	 * @param year        A year to filter for
	 * @param taxpayerIds A list of taxpayers to filter for
	 * @return A {@link SearchRequest} with all configurations
	 */
	private SearchRequest getRequestForOutliers(boolean min, String statement, double value, int sourceData, int year,
			List<String> taxpayerIds, String[] groupBy ) {
		
		if ( Double.isNaN(value) )
			value = 0d;

		// Index over 'Accounting Computed Statement Income' objects
		SearchRequest searchRequest = new SearchRequest(
				SOURCE_JOURNAL == sourceData ? COMPUTED_STATEMENT_INCOME_INDEX : DECLARED_STATEMENT_INCOME_INDEX);

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
		AggregationBuilder metric = AggregationBuilders.sum("amount").field("amount_relative");
		AbstractAggregationBuilder<?> aggregationBuilder = SearchUtils.aggregationBuilder(null, groupBy, metric);
		buildSearchSourceBuilder(query, aggregationBuilder, searchRequest);
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

		String[] groupBy = {"taxPayerId.keyword"};
		
		// Configure the aggregations
		AbstractAggregationBuilder<?> aggregationBuilder = SearchUtils.aggregationBuilder(null, groupBy);

		// Execute a search
		SearchResponse sresp = doSearch(query, aggregationBuilder, searchRequest);
		if (sresp == null) {
			log.log(Level.INFO, () -> "No data found for qualifier " + qualifier + " with value " + qualifierValue);
			return Collections.emptyList(); // No data found
		}
		
		BiFunction<Aggregations, String[], String> function = (agg, values) -> {
			return values[0];
		};

		// Update outlier information for this taxpayer
		return SearchUtils.collectAggregations(sresp.getAggregations(), groupBy, function);
	}

	/**
	 * Search and return values for a specified qualifier
	 * 
	 * @param qualifier Qualifier to search
	 * 
	 * @return A {@link List} of values for specified qualifier
	 */
	@Cacheable("qualifierValues")
	public List<String> getQualifierValues(String qualifier) {

		// Index over 'taxpayer' objects
		SearchRequest searchRequest = new SearchRequest(TAXPAYER_INDEX);
		
		String[] groupBy = {qualifier + ".keyword"};

		// Configure the aggregations
		AbstractAggregationBuilder<?> aggregationBuilder = SearchUtils.aggregationBuilder(null, groupBy);

		// Execute a search
		SearchResponse sresp = doSearch(null, aggregationBuilder, searchRequest);
		if (sresp == null) {
			log.log(Level.INFO, () -> "No data found for qualifier " + qualifier);
			return Collections.emptyList(); // No data found
		} 
		
		BiFunction<Aggregations, String[], String> function = (agg, values) -> {
			return values[0];
		};

		// Update outlier information for this taxpayer
		List<String> values = SearchUtils.collectAggregations(sresp.getAggregations(), groupBy, function);
		return values.stream().filter(Objects::nonNull).sorted().collect(Collectors.toList());

	}

	/**
	 * @param sourceData An indication of source data (index) to use If sourceData
	 *                   isn't #SOURCE_JOURNAL or #SOURCE_DECLARED_INCOME_STATEMENT,
	 *                   search for booth to return.
	 *                   
	 *		Valid values for sourceData param
	 *		1 - SOURCE_JOURNAL
	 *		2 - SOURCE_DECLARED_INCOME_STATEMENT
	 *		3 - SOURCE_BOOTH_INCOME_STATEMENT (1 and 2 options)
	 *		4 - SOURCE_SHAREHOLDERS
	 *		5 - SOURCE_BOOTH_INCOME_STATEMENT (1 and 2 options) AND SOURCE_SHAREHOLDERS
	 * 
	 * @return A list of years present in Accounting Statement Income indexes
	 */
	@Cacheable("years")
	public List<Integer> getYears(int sourceData) {

		if (sourceData == SOURCE_BOOTH_INCOME_STATEMENT) {
			List<Integer> toRet = getYearsByIndex(SOURCE_JOURNAL);
			toRet.addAll(getYearsByIndex(SOURCE_DECLARED_INCOME_STATEMENT));
			return toRet.stream().distinct().map(Integer::intValue).sorted().collect(Collectors.toList());
		}
		else if (sourceData == SOURCE_BOOTH_INCOME_STATEMENT_AND_SHAREHOLDERS) {
			List<Integer> toRet = getYearsByIndex(SOURCE_JOURNAL);
			toRet.addAll(getYearsByIndex(SOURCE_DECLARED_INCOME_STATEMENT));
			toRet.addAll(getYearsByIndex(SOURCE_SHAREHOLDERS));
			return toRet.stream().distinct().map(Integer::intValue).sorted().collect(Collectors.toList());
		}

		return getYearsByIndex(sourceData);

	}

	/**
	 * 
	 * @param sourceData An indication of source data (index) to use
	 * @return A list of years present in Accounting Statement Income indexes
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
		
		String[] groupBy = {"year"};

		// Configure the aggregations
		AbstractAggregationBuilder<?> aggregationBuilder = SearchUtils.aggregationBuilder(null, groupBy);
		// Execute a search
		SearchResponse sresp = doSearch(null,aggregationBuilder,searchRequest);
		if (sresp == null) {
			log.log(Level.INFO, "No data found for years");
			return new LinkedList<>(); // No data found
		}
			
		BiFunction<Aggregations, String[], Integer> function = (agg, values) -> {			
			return new Integer(values[0]);
		};

		// Update outlier information for this taxpayer
		List<Integer> years = SearchUtils.collectAggregations(sresp.getAggregations(), groupBy, function);
		return years.stream().filter(Objects::nonNull).sorted().collect(Collectors.toList());
	}

	/**
	 * 
	 * @param taxpayerId
	 * @param startDate
	 * @param finalDate
	 * @return
	 */
	public List<AggregatedAccountingFlow> getAccountingFlow(String taxpayerId, Date startDate, Date finalDate) {
		SearchRequest searchRequest = new SearchRequest(INDEX_PUBLISHED_ACCOUNTING_FLOW);

		// Filter by taxpayerId
		BoolQueryBuilder query = QueryBuilders.boolQuery();
		query = query.must(new TermQueryBuilder("taxpayer_id.keyword", taxpayerId));

		// Filter for period
		String from = ParserUtils.formatTimestampES(startDate);
		String to = ParserUtils.formatTimestampES(finalDate);
		RangeQueryBuilder dateRange = new RangeQueryBuilder("date").from(from).to(to);
		query = query.must(dateRange);

		String[] groupBy = { "credited_account_category.keyword", "credited_account_category_name.keyword",
				"credited_account_subcategory.keyword", "credited_account_subcategory_name.keyword",
				"credited_account_code.keyword", "credited_account_name.keyword", "debited_account_category.keyword",
				"debited_account_category_name.keyword", "debited_account_subcategory.keyword",
				"debited_account_subcategory_name.keyword", "debited_account_code.keyword",
				"debited_account_name.keyword" };

		AggregationBuilder metric = AggregationBuilders.sum("totalFlow").field("amount");

		AbstractAggregationBuilder<?> aggregationBuilder = SearchUtils.aggregationBuilder(null, groupBy, metric);

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(query)
				.aggregation(aggregationBuilder);

		// We are not interested on individual documents
		searchSourceBuilder.size(0);

		searchRequest.source(searchSourceBuilder);

		SearchResponse sresp = doSearch(searchRequest);
		if (sresp == null) {
			log.log(Level.INFO, () -> "No flows found for taxPayer " + taxpayerId + " for period from " + from + " to " + to);
			return Collections.emptyList(); // No flows found
		}

		BiFunction<Aggregations, String[], AggregatedAccountingFlow> function = (agg, values) -> {
			Sum sum = agg.get("totalFlow");
			return new AggregatedAccountingFlow(values, sum.getValue());
		};

		return SearchUtils.collectAggregations(sresp.getAggregations(), groupBy, function);
	}

	/**
	 * Build the query object used by {@link #getAccounts(String, int, String)}
	 * 
	 * @param taxpayerId A taxpayer to filter for
	 * @param sourceData An indication of source data (index) to use
	 * @param year       A year to filter for
	 * @param groupBy	 Fields to group by for
	 * @param metric	 Metric to sum
	 * @return A {@link SearchRequest} with all parameters and filters
	 */
	private SearchRequest searchComputedStatementIncomeValues(String taxpayerId, int sourceData, int year, 
			String[] groupBy, AggregationBuilder metric) {

		// Index over 'Accounting Computed Statement Income' objects
		SearchRequest searchRequest = new SearchRequest(
				SOURCE_JOURNAL == sourceData ? COMPUTED_STATEMENT_INCOME_INDEX : DECLARED_STATEMENT_INCOME_INDEX);

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
		AbstractAggregationBuilder<?> aggregationBuilder = SearchUtils.aggregationBuilder(null, groupBy, metric); 
		buildSearchSourceBuilder(query, aggregationBuilder, searchRequest);
		return searchRequest;

	}

	/**
	 * Given a field name, translate it (add suffix) for current language
	 * 
	 * @param fieldName A field name to translate
	 * @return Translated field name
	 */
	private String translate(String fieldName) {

		String language = LocaleContextHolder.getLocale().getLanguage();

		if ("en".equalsIgnoreCase(language))
			return fieldName;

		return fieldName + "_" + language;
	}

	/**
	 * Search and return Statement Income for a specified taxpayer and year
	 * 
	 * @param taxpayerId Taxpayer to search statement income
	 * @param year       Year to search statement income
	 * @return A {@link List} of {@link StatementIncomeItem}
	 * 
	 */
	public List<StatementIncomeItem> getStatementIncomeDeclaredAndCalculated(String taxpayerId, int year) {

		if (taxpayerId == null || taxpayerId.isEmpty()) {
			log.log(Level.INFO, "No valid taxpayer received");
			return Collections.emptyList(); // No data found
		}

		if (year == 0) {
			log.log(Level.INFO, "No valid year received");
			return Collections.emptyList(); // No data found
		}

		// Search statement income values
		Map<String, StatementIncomeItem> items = new HashMap<>();
		getStatementIncomeValues(taxpayerId, year, SOURCE_JOURNAL, items);
		getStatementIncomeValues(taxpayerId, year, SOURCE_DECLARED_INCOME_STATEMENT, items);
		List<StatementIncomeItem> values = new ArrayList<>(items.values());
		values.sort(null);
		return values;
	}

	/**
	 * Search and store values for Stated Income calculated or declared, according
	 * with received parameters
	 * 
	 * @param taxpayerId A taxpayer to filter for
	 * @param year       A year to filter for
	 * @param sourceData Indicates a source index (calculated or declared)
	 * @param items      A {@link Map} to store values
	 */
	private void getStatementIncomeValues(String taxpayerId, int year, int sourceData,
			Map<String, StatementIncomeItem> items) {
		
		String[] groupBy = {"statement_number.keyword", translate("statement_name") + ".keyword"};		
		AggregationBuilder metric = AggregationBuilders.sum("amount").field("amount");

		// Define a search request according with parameters
		SearchRequest searchRequest = searchComputedStatementIncomeValues(taxpayerId, sourceData, year, groupBy, metric);

		// Execute a search
		SearchResponse sresp = doSearch(searchRequest);
		if (sresp == null) {
			log.log(Level.INFO, "No data found");
			return;
		} 

		// Retrieve information from result
		BiFunction<Aggregations, String[], StatementIncomeItem> function = (agg, values) -> {
			Sum amount = agg.get("amount");
			StatementIncomeItem item = items.getOrDefault(values[0]/*statementNumber*/, 
					new StatementIncomeItem(values, amount.getValue(), (SOURCE_JOURNAL == sourceData) /*calculated*/));
			items.put(values[0]/*statementNumber*/, item);
			return item;
		};

		// Update shareholding information for this taxpayer
		SearchUtils.collectAggregations(sresp.getAggregations(), groupBy, function);

	}

	/**
	 * Search and return all data about a taxpayer specified in parameter
	 * 
	 * @param taxpayerId A taxpayer to search data for
	 * @param year       A specific year to search
	 * @param searchType A specific search to do
	 * 		Valid values for searchType:
	 * 		1 - SEARCH_SHAREHOLDINGS
	 * 		2 - SEARCH_SHAREHOLDERS
	 * 		3 - REVENUE_NET_AND_GROSS_PROFIT_DECLARED
	 * 		4 - REVENUE_NET_AND_GROSS_PROFIT_COMPUTED
	 * 		5 - TAX_PROVISION
	 * 		6 - ANALYTICS_ACCOUNTS
	 * 		7 - CUSTOMERS
	 * 		8 - SUPPLIERS
	 * 
	 * @return Data about a specified taxpayer for a specific year according with
	 *         specified search type.
	 */
	public List<?> getTaxpayerData(String taxpayerId, int year, int searchType) {

		switch (searchType) {
		case SEARCH_SHAREHOLDINGS:
			return getShareholdings(taxpayerId, year);
		case SEARCH_SHAREHOLDERS:
			return getShareholders(taxpayerId, year);
		case REVENUE_NET_AND_GROSS_PROFIT_DECLARED:
			return getRevenueNetAndGrossProfit(taxpayerId, year, DECLARED_STATEMENT_INCOME_INDEX);
		case REVENUE_NET_AND_GROSS_PROFIT_COMPUTED:
			return getRevenueNetAndGrossProfit(taxpayerId, year, COMPUTED_STATEMENT_INCOME_INDEX);
		case TAX_PROVISION:
			return getTaxProvision(taxpayerId, year);
		case ANALYTICS_ACCOUNTS:
			return getAnalyticsAccounts(taxpayerId, year);
		case CUSTOMERS:
			return getCustomers(taxpayerId, year);
		case SUPPLIERS:
			return getSuppliers(taxpayerId, year);
		default:
			break;
		}

		return Collections.emptyList();

	}

	/**
	 * Search and add shareholding information for a specified taxpayer and year
	 * 
	 * @param taxpayerId Taxpayer to search for
	 * @param year       A year to search for
	 * 
	 * @return A {@link List} of {@link Shareholding} itens with shareholding
	 *         information
	 */
	private List<Shareholding> getShareholdings(String taxpayerId, int year) {
		
		// Define group by fields
		Object[] groupBy = { "shareholding_id.keyword", "shareholding_name.keyword",
				translate("share_type_name") + ".keyword", new Script(getShreholdScript(), "byShareClass") };
		
		// Index to search in
		SearchRequest searchRequest = getShareholdRequest("taxpayer_id.keyword",taxpayerId,year,groupBy, getShareholdAggregationBuilder(true));

		// Search results
		SearchResponse sresp = doSearch(searchRequest);
		if (sresp == null) {
			log.log(Level.INFO, () -> "No shareholding information found for taxPayer " + taxpayerId + " for period " + year);
			return Collections.emptyList(); // No shareholding found
		}

		// Retrieve information from result
		BiFunction<Aggregations, String[], Shareholding> function = (agg, values) -> {
			Sum amount = agg.get("shareAmount");
			Sum percentage = agg.get("sharePercentage");
			Sum quantity = agg.get("shareQuantity");
			Sum equityResult = agg.get("equityMethodResult");
			return new Shareholding(values, amount.getValue(), percentage.getValue(), quantity.getValue(), equityResult.getValue());
		};

		// Update shareholding information for this taxpayer
		return SearchUtils.collectAggregations(sresp.getAggregations(), groupBy, function);

	}
	
	/**
	 * Search and add shareholders information for a specified taxpayer and year
	 * 
	 * @param taxpayerId Taxpayer to search for
	 * @param year       A year to search for
	 * 
	 * @return A {@link List} of {@link Shareholding} itens with shareholders
	 *         information
	 */
	private List<Shareholding> getShareholders(String taxpayerId, int year) {
		
		// Define group by fields
		Object[] groupBy = { "taxpayer_id.keyword", "taxpayer_name.keyword", translate("share_type_name") + ".keyword",
				new Script(getShreholdScript(), "byShareClass") };		

		// Index to search in
		SearchRequest searchRequest = getShareholdRequest("shareholding_id.keyword",taxpayerId,year,groupBy, getShareholdAggregationBuilder(false));

		// Search results
		SearchResponse sresp = doSearch(searchRequest);
		if (sresp == null) {
			log.log(Level.INFO, () -> "No shareholders information found for taxPayer " + taxpayerId + " for period " + year);
			return Collections.emptyList(); // No shareholders found
		}

		// Retrieve information from result
		BiFunction<Aggregations, String[], Shareholding> function = (agg, values) -> {
			Sum amount = agg.get("shareAmount");
			Sum percentage = agg.get("sharePercentage");
			Sum quantity = agg.get("shareQuantity");			
			return new Shareholding(values, amount.getValue(), percentage.getValue(), quantity.getValue(), 0d);
		};

		// Update shareholding information for this taxpayer
		return SearchUtils.collectAggregations(sresp.getAggregations(), groupBy, function);

	}
	
	/**
	 * 
	 * @return	A simple script to get share_class.keywod field value
	 */
	private org.elasticsearch.script.Script getShreholdScript() {
		// Script in 'painless' language for identifying if there is a field called
		//share_class.keyword
		return new org.elasticsearch.script.Script(
				"if (doc['share_class.keyword'].size()==0) return '';"
						+ "else return doc['share_class.keyword'].value; ");
	}
	
	/**
	 * Create a {@link List} of {@link AggregationBuilder} with metrics
	 * @param shareholding	Indicates if it's a shareholding search
	 * @return	A {@link List} of {@link AggregationBuilder} with metrics
	 */
	private AggregationBuilder[] getShareholdAggregationBuilder(boolean shareholding) {
		// Define aggregations
		List<AggregationBuilder> metrics = new LinkedList<>();
		metrics.add(AggregationBuilders.sum("shareAmount").field("share_amount"));
		metrics.add(AggregationBuilders.sum("sharePercentage").field("share_percentage"));
		metrics.add(AggregationBuilders.sum("shareQuantity").field("share_quantity"));
		
		if ( shareholding )
			metrics.add(AggregationBuilders.sum("equityMethodResult").field("equity_method_result"));
		
		return metrics.toArray(new AggregationBuilder[0]);
	}	

	/**
	 * Create and return a {@link SearchRequest} with provided parameters 
	 * @param fieldName	Field name to search
	 * @param taxpayerId	Taxpayer id
	 * @param year			Year to search
	 * @param groupBy		Fields to group result
	 * @param metrics		Metrics to group
	 * @return	A {@link SearchRequest} object
	 */
	private SearchRequest getShareholdRequest(String fieldName, String taxpayerId, int year, Object[] groupBy, AggregationBuilder[] metrics) {
		
		SearchRequest searchRequest = new SearchRequest(SHAREHOLDING_INDEX);
		
		BoolQueryBuilder query = QueryBuilders.boolQuery();
		// Filter by taxpayer
		query = query.must(new TermQueryBuilder(fieldName, taxpayerId));

		// Filter for year
		query = query.must(new TermQueryBuilder("year", year));

		AbstractAggregationBuilder<?> aggregationBuilder = SearchUtils.aggregationBuilder(null, groupBy, metrics);

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(query)
				.aggregation(aggregationBuilder);

		// We are not interested on individual documents
		searchSourceBuilder.size(0);
		searchRequest.source(searchSourceBuilder);		
		
		return searchRequest;
	}

	/**
	 * Search and return Revenue Net and Gross Profit information for a specified
	 * taxpayer and year
	 * 
	 * @param taxpayerId Taxpayer to search for
	 * @param year       A year to search for
	 * @param index      An index to search in
	 * 
	 * @return A {@link List} of {@link Map} with revenue net and gross profit
	 *         information for a taxpayer and specified year and previous periods
	 */
	private List<Map<String, Object>> getRevenueNetAndGrossProfit(String taxpayerId, int year, String index) {

		// Index to search in
		SearchRequest searchRequest = new SearchRequest(index);
		
		BoolQueryBuilder query = QueryBuilders.boolQuery();
		// Filter by taxpayer
		query = query.must(new TermQueryBuilder("taxpayer_id.keyword", taxpayerId));

		// Filter by year
		query.must(new RangeQueryBuilder("year").from(year - getPastPeriods()).to(year));

		// Filter by statement
		BoolQueryBuilder subquery = QueryBuilders.boolQuery();
		subquery = subquery.should(new TermQueryBuilder("statement_number.keyword", "01"));
		subquery = subquery.should(new TermQueryBuilder("statement_number.keyword", "03"));
		subquery = subquery.minimumShouldMatch(1);
		query = query.must(subquery);

		// Define group by fields
		Object[] groupBy = { "year", "statement_number.keyword", translate("statement_name") + ".keyword" };

		// Define aggregations
		AggregationBuilder shareAmount = AggregationBuilders.sum("amount").field("amount");

		AbstractAggregationBuilder<?> aggregationBuilder = SearchUtils.aggregationBuilder(null, groupBy, shareAmount);

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(query)
				.aggregation(aggregationBuilder);

		// We are not interested on individual documents
		searchSourceBuilder.size(0);
		searchRequest.source(searchSourceBuilder);

		// Search results
		SearchResponse sresp = doSearch(searchRequest);
		if (sresp == null) {
			log.log(Level.INFO, () -> "No revenue net and gross profit information found for taxPayer " + taxpayerId
					+ " for period " + year);
			return Collections.emptyList(); // No data found
		}
		
		final String type = DECLARED_STATEMENT_INCOME_INDEX.equals(index) ? 
				messageSource.getMessage("taxpayers.analysis.statement.income.declared.values", null, LocaleContextHolder.getLocale()) : 
					messageSource.getMessage("taxpayers.analysis.statement.income.calculated.values", null, LocaleContextHolder.getLocale());

		// Retrieve information from result
		BiFunction<Aggregations, String[], Map<String, Object>> function = (agg, values) -> {
			Sum amount = agg.get("amount");
			Map<String, Object> instance = new HashMap<>();
			instance.put("type", type);
			instance.put("year", values[0]);
			instance.put("statementOrder", values[1]);
			instance.put("statementName", values[2]);
			instance.put("value", amount.getValue());						
			return instance;
		};

		// Get information
		return SearchUtils.collectAggregations(sresp.getAggregations(), groupBy, function);

	}

	/**
	 * Search and return tax provision information for a specified taxpayer and year
	 * 
	 * @param taxpayerId Taxpayer to search for
	 * @param year       A year to search for
	 * 
	 * @return A {@link List} of {@link Map} with tax provision information for a
	 *         taxpayer and specified year and previous periods
	 */
	private List<Map<String, Object>> getTaxProvision(String taxpayerId, int year) {

		// Index to search in
		SearchRequest searchRequest = new SearchRequest(BALANCE_SHEET_INDEX);

		BoolQueryBuilder query = QueryBuilders.boolQuery();
		// Filter by taxpayer
		query = query.must(new TermQueryBuilder("taxpayer_id.keyword", taxpayerId));

		// Filter by subcategory
		query = query.must(new TermQueryBuilder("account_subcategory_tag.keyword", ACCOUNT_SUBCATEGORY_TAX_PROVISION));

		// Filter by month
		query = query.must(new TermQueryBuilder("month_number", 12));

		// Filter by year
		query.must(new RangeQueryBuilder("year").from(year - getPastPeriods()).to(year));

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(query);

		searchRequest.source(searchSourceBuilder);

		// Search results
		SearchResponse sresp = doSearch(searchRequest);
		if (sresp == null) {
			log.log(Level.INFO,
					() -> "No tax provision information found for taxPayer " + taxpayerId + " for period " + year);
			return Collections.emptyList(); // No data found
		}

		List<Map<String, Object>> instances = new LinkedList<>();

		sresp.getHits().forEach(hit -> {
			Map<String,Object> map = hit.getSourceAsMap();						
			removeUnecessaryFields(map);
			instances.add(map);
		});

		// Get information
		return instances;

	}
	
	/**
	 * Fields to remove from document before returning it
	 */
	static String[] fieldsToRemove = {
	        "taxperiod_number",
	        "template_version",
	        "line",
	        "account_subcategory_tag",
	        "month_number",
	        "account_category",
	        "amount_debits",
	        "taxpayer_name",
	        "account_category_name_es",
	        "taxpayer_qualifier_5",
	        "final_balance_debit_credit",
	        "taxpayer_qualifier_4",
	        "taxpayer_qualifier_3",
	        "taxpayer_qualifier_2",
	        "account_description",
	        "account_name",
	        "taxpayer_qualifier_1",
	        "final_balance_with_sign",
	        "timestamp",
	        "account_subcategory",
	        "tax_year",
	        "tax_payer_id",
	        "account_category_tag",
	        "template_name",
	        "account_category_name",
	        "month",
	        "book_entries",
	        "taxpayer_id",
	        "account_subcategory_name_es",
	        "initial_balance_debit_credit",
	        "account_code",
	        "account_subcategory_name",
	        "amount_credits",
	        "initial_balance",
	        "initial_balance_with_sign",
	        "doc_timestamp" };
	
	/**
	 * Remove unecessary fields from document before return it
	 * @param map	Map with pairs field names and values
	 */
	private void removeUnecessaryFields(Map<String, Object> map) {	
		for ( String field : fieldsToRemove )
			map.remove(field);	
	}

	/**
	 * Search and return major expense analytics accounts for a specified taxpayer
	 * and year
	 * 
	 * @param taxpayerId Taxpayer to search for
	 * @param year       A year to search for
	 * 
	 * @return A {@link List} of {@link Map} with major expense analytics accounts
	 *         for a taxpayer and specified year and previous periods
	 */
	private List<Map<String, Object>> getAnalyticsAccounts(String taxpayerId, int year) {

		// Index to search in
		SearchRequest searchRequest = new SearchRequest(BALANCE_SHEET_INDEX);

		BoolQueryBuilder query = QueryBuilders.boolQuery();
		// Filter by taxpayer
		query = query.must(new TermQueryBuilder("taxpayer_id.keyword", taxpayerId));

		// Filter by expense category
		query = query.must(new TermQueryBuilder("account_category_tag.keyword", ACCOUNT_CATEGORY_EXPENSE));

		// Filter by month
		query = query.must(new TermQueryBuilder("month_number", 12));

		// Filter by year
		query.must(new RangeQueryBuilder("year").from(year - getPastPeriods()).to(year));
		
		AggregationBuilder sum = AggregationBuilders.sum("final_balance").field("final_balance");
		
		BucketSortPipelineAggregationBuilder paging = PipelineAggregatorBuilders
				.bucketSort("paging", Arrays.asList(new FieldSortBuilder("final_balance").order(SortOrder.DESC))).from(0)
				.size(5);		

		// Configure the aggregations
		AbstractAggregationBuilder<?> aggregationBuilder = AggregationBuilders.terms("year").size(10_000).field("year")
				.subAggregation(AggregationBuilders.terms("account_code").size(10_000).field("account_code.keyword")
				.subAggregation(AggregationBuilders.terms("account_name").size(10_000).field("account_name.keyword"))
						.subAggregation(sum).subAggregation(paging));

		SearchResponse sresp = doSearch(query,aggregationBuilder,searchRequest);
		
		if ( sresp == null ) {
			log.log(Level.INFO,() -> "No tax provision information found for taxPayer " + taxpayerId + " for period " + year);
			return Collections.emptyList(); // No data found
		}

		String[] groupBy = { "year", "account_code" };
		Map<String,Double> yearValues = new HashMap<>();

		// Retrieve information from result
		BiFunction<Aggregations, String[], Map<String, Object>> function = (agg, values) -> {
			String accountName = getStringValueForAgg(agg,"account_name");
			if ( accountName == null )
				return null;
			Sum amount = agg.get("final_balance");
			
			if ( amount.getValue() <= 0 )
				return null;
			
			Map<String, Object> instance = new HashMap<>();
			instance.put("year", values[0]);
			Map<String,Object> instanceValues = new HashMap<>();
			instanceValues.put("account_code", values[1]);
			instanceValues.put("account_name", accountName);
			instanceValues.put("value", amount.getValue());
			instanceValues.put("title", values[0] + ": " + accountName);
			instance.put("values", instanceValues);
			
			yearValues.compute(values[0], (k,v) -> v == null ? amount.getValue() : (v+=amount.getValue()));
			
			return instance;
		};

		// Get information
		return getInstances(sresp,groupBy,function,yearValues);
	}

	/**
	 * Build search with given parameters
	 * @param query
	 * @param aggregationBuilder
	 * @param searchRequest
	 */
	private void buildSearchSourceBuilder(BoolQueryBuilder query, AbstractAggregationBuilder<?> aggregationBuilder,SearchRequest searchRequest) {
		SearchSourceBuilder searchSourceBuilder = null;
		if ( query == null )
			searchSourceBuilder = new SearchSourceBuilder().aggregation(aggregationBuilder);			
		else
			searchSourceBuilder = new SearchSourceBuilder().query(query).aggregation(aggregationBuilder);
		// We are not interested on individual documents
		searchSourceBuilder.size(0);
		searchRequest.source(searchSourceBuilder);		
	}

	/**
	 * Do a search on ES database and return {@link SearchResponse} object
	 * @param query	A query and it's parameters
	 * @param buildSearchSourceBuilder	
	 * @param searchRequest
	 * @return	A {@link SearchResponse} object
	 */
	private SearchResponse doSearch(BoolQueryBuilder query, AbstractAggregationBuilder<?> buildSearchSourceBuilder,SearchRequest searchRequest) {
		buildSearchSourceBuilder(query,buildSearchSourceBuilder,searchRequest);
		return doSearch(searchRequest);
	}
	
	/**
	 * Do a search on ES database and return {@link SearchResponse} object
	 * @param searchRequest	Search parameters
	 * @return	A {@link SearchResponse} object
	 */
	private SearchResponse doSearch(SearchRequest searchRequest) {
		// Search results
		SearchResponse sresp = null;
		try {
			sresp = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
		} catch (Exception ex) {
			if (!ErrorUtils.isErrorNoIndexFound(ex) && !ErrorUtils.isErrorNoMappingFoundForColumn(ex)
					&& !ErrorUtils.isErrorNotFound(ex))
				log.log(Level.SEVERE, ex.getMessage(), ex);
			return null;
		}		
		if (sresp == null || Utils.getTotalHits(sresp) == 0) {
			return null;			
		}
		return sresp;
	}	

	/**
	 * Search and return major customers for a specified taxpayer and year
	 * 
	 * @param taxpayerId Taxpayer to search for
	 * @param year       A year to search for
	 * 
	 * @return A {@link List} of {@link Map} with major customers for a taxpayer and
	 *         specified year and previous periods
	 */
	private List<Map<String, Object>> getCustomers(String taxpayerId, int year) {

		return filterByTaxpayerIdAndYearDoingAggregation(taxpayerId, year, CUSTOMERS_INDEX,
				new String[] {"customer_id.keyword", "customer_name.keyword"});

	}

	/**
	 * Search and return major suppliers for a specified taxpayer and year
	 * 
	 * @param taxpayerId Taxpayer to search for
	 * @param year       A year to search for
	 * 
	 * @return A {@link List} of {@link Map} with major suppliers for a taxpayer and
	 *         specified year and previous periods
	 */
	private List<Map<String, Object>> getSuppliers(String taxpayerId, int year) {
		
		return filterByTaxpayerIdAndYearDoingAggregation(taxpayerId, year, SUPPLIERS_INDEX,
				new String[] {"supplier_id.keyword", "supplier_name.keyword"});
		
	}
	
	/**
	 * Search with some filters (over taxpayer id and year), doing some aggregations, and returns the data.
	 * @param taxpayerId	Taxpayer id
	 * @param year			Period (year)
	 * @param index			Index to search
	 * @param fieldsGroupBy	Fields to aggregate
	 * @return
	 */
	private List<Map<String, Object>> filterByTaxpayerIdAndYearDoingAggregation(String taxpayerId, int year,
			String index, String[] fieldsGroupBy) {

		// Index to search in
		SearchRequest searchRequest = new SearchRequest(index);

		BoolQueryBuilder query = QueryBuilders.boolQuery();
		// Filter by taxpayer
		query = query.must(new TermQueryBuilder("taxpayer_id.keyword", taxpayerId));

		// Filter by year
		query.must(new RangeQueryBuilder("year").from(year - getPastPeriods()).to(year));

		AggregationBuilder sum = AggregationBuilders.sum("amount").field("amount");
		
		BucketSortPipelineAggregationBuilder paging = PipelineAggregatorBuilders
				.bucketSort("paging", Arrays.asList(new FieldSortBuilder("amount").order(SortOrder.DESC))).from(0)
				.size(5);		

		// Configure the aggregations
		AbstractAggregationBuilder<?> aggregationBuilder = AggregationBuilders.terms("year").size(10_000).field("year")
				.subAggregation(AggregationBuilders.terms(fieldsGroupBy[0]).size(10_000).field(fieldsGroupBy[0])
				.subAggregation(AggregationBuilders.terms(fieldsGroupBy[1]).size(10_000).field(fieldsGroupBy[1]))
						.subAggregation(sum).subAggregation(paging));

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(query)
				.aggregation(aggregationBuilder);

		// We are not interested on individual documents
		searchSourceBuilder.size(0);
		searchRequest.source(searchSourceBuilder);

		// Search results
		SearchResponse sresp = doSearch(searchRequest);
		if (sresp == null) {
			log.log(Level.INFO,
					() -> "No major information found for taxPayer " + taxpayerId + " for period " + year);
			return Collections.emptyList(); // No data found
		}

		String[] groupBy = { "year", fieldsGroupBy[0] };
		Map<String,Double> yearValues = new HashMap<>();

		// Retrieve information from result
		BiFunction<Aggregations, String[], Map<String, Object>> function = (agg, values) -> {
			String secondAggregation = getStringValueForAgg(agg,fieldsGroupBy[1]);
			if ( secondAggregation == null )
				return null;			
			Sum amount = agg.get("amount");

			if ( amount.getValue() <= 0 )
				return null;
			
			Map<String, Object> instance = new HashMap<>();
			instance.put("year", values[0]);
			Map<String, Object> instanceValues = new HashMap<>();
			instanceValues.put(fieldsGroupBy[0].replace(".keyword", ""), values[1]);
			instanceValues.put(fieldsGroupBy[1].replace(".keyword", ""), secondAggregation);			
			instanceValues.put("value", amount.getValue());
			instanceValues.put("title", values[0] + ": " + secondAggregation);
			instance.put("values", instanceValues);
			
			yearValues.compute(values[0], (k,v) -> v == null ? amount.getValue() : (v+=amount.getValue()));			
			
			return instance;
		};

		// Get information
		return getInstances(sresp,groupBy,function,yearValues);
	}

	/**
	 * Given an {@link Aggregations} and a aggregation name, search value and return it
	 * @param agg		An {@link Aggregations} to search in
	 * @param aggName	A parameter to search in {@link Aggregations}
	 * @return			A value for a specified parameter, or null if it's not found
	 */
	private String getStringValueForAgg(Aggregations agg, String aggName) {
		ParsedStringTerms terms = agg.get(aggName);
		if ( terms != null ) {
			List<? extends Bucket> buckets = terms.getBuckets();
			if ( buckets != null && !buckets.isEmpty() )
				return buckets.get(0).getKeyAsString();				
		}
		return null;
	}

	/**
	 * Locate on application.properties a number of past periods to consider on searches.
	 * If value is not present, 5 periodes will be considered.
	 * @return	A number of past periods to consider on searches
	 */
	private int getPastPeriods() {
		String s = env.getProperty("search.past.periods");
		return Integer.parseInt(s == null ? "5" : s);
	}

	/**
	 * Search and return differences values for customers and suppliers for a specified taxpayer and year
	 * 
	 * @param taxpayerId Taxpayer to search for
	 * @param year       A year to search for
	 * 
	 * @return A {@link List} of {@link Map} with differences values for customers and suppliers for a taxpayer and
	 *         specified year
	 */
	public List<CustomerVsSupplier> getCustomersVsSuppliers(String taxpayerId, int year) {
		
		String[] groupByCustomers = { "year", "month_number", "customer_id.keyword", "customer_name.keyword" };
		
		List<Map<String, Object>> customers = getCustomersVsSuppliersValues(taxpayerId, year, CUSTOMERS_INDEX, groupByCustomers);
		
		String[] groupBySuppliers = { "year", "month_number", "taxpayer_id.keyword","taxpayer_name.keyword" };
		
		List<Map<String, Object>> suppliers = getCustomersVsSuppliersValues(taxpayerId, year, SUPPLIERS_INDEX, groupBySuppliers);
		
		Map<Pair<YearMonth,String>,CustomerVsSupplier> instances = new HashMap<>(customers.size());
		
		customers.forEach( item -> {
			CustomerVsSupplier instance = new CustomerVsSupplier(item, "customer");
			instances.put(Pair.of(instance.getMonth(),instance.getCustomerId()), instance);
		});
		
		suppliers.forEach( item -> {
			YearMonth month = YearMonth.of(Integer.valueOf(item.get("year").toString()), 
					Integer.valueOf(item.get("month_number").toString()));
			Pair<YearMonth,String> key = Pair.of(month,item.get("taxpayer_id.keyword").toString());
			CustomerVsSupplier instance = instances.get(key);
			if ( instance == null ) {
				instance = new CustomerVsSupplier(item, "supplier");
				instances.put(key, instance);
			}
			else {
				instance.setSupplierValue(Double.parseDouble(item.get("amount").toString()));
			}
			instance.setDifference(instance.getCustomerValue()-instance.getSupplierValue());

		}); 

		return instances.values().stream().sorted().collect(Collectors.toList());
	}

	/**
	 * Get values for customers os suppliers for a specified taxpayer and period
	 * @param taxpayerId	A taxpayer to filter for
	 * @param year			A year to filter for
	 * @param index			An index to search in
	 * @param groupBy		An array of fields do search
	 * @return	A @{@link List} of {@link Map} where key is field name and value is field value. 
	 */
	private List<Map<String, Object>> getCustomersVsSuppliersValues(String taxpayerId, int year, String index,
			String[] groupBy) {

		// Index over 'Customers' objects
		SearchRequest searchRequest = new SearchRequest(index);

		BoolQueryBuilder query = QueryBuilders.boolQuery();		
		
		// Filter by taxpayer		
		if ( CUSTOMERS_INDEX.equals(index) )
			query = query.must(new TermQueryBuilder("taxpayer_id.keyword", taxpayerId));
		else
			query = query.must(new TermQueryBuilder("supplier_id.keyword", taxpayerId));

		// Filter by year
		query = query.must(new TermQueryBuilder("year", year));

		// Configure the aggregations
		AggregationBuilder sum = AggregationBuilders.sum("amount").field("amount");
		AbstractAggregationBuilder<?> aggregationBuilder = SearchUtils.aggregationBuilder(null, groupBy, sum);		

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(query)
				.aggregation(aggregationBuilder);

		// We are not interested on individual documents
		searchSourceBuilder.size(0);
		searchRequest.source(searchSourceBuilder);

		// Search results
		SearchResponse sresp = doSearch(searchRequest);
		if (sresp == null) {
			log.log(Level.INFO,
					() -> "No customers information found for taxPayer " + taxpayerId + " for period " + year);
			return Collections.emptyList(); // No data found
		}

		// Retrieve information from result
		BiFunction<Aggregations, String[], Map<String, Object>> function = (agg, values) -> {
			Sum amount = agg.get("amount");
			Map<String, Object> instance = new HashMap<>();
			instance.put(groupBy[0], values[0]);
			instance.put(groupBy[1], values[1]);
			instance.put(groupBy[2], values[2]);
			instance.put(groupBy[3], values[3]);
			instance.put("amount", amount.getValue());
			return instance;
		};

		// Get customers/suppliers information
		return SearchUtils.collectAggregations(sresp.getAggregations(), groupBy, function);
		
	}
	
	/**
	 * Transform the response from a ElasticSearch's query into a list of records, each record represented by a map of key/value
	 * pairs
	 * @param sresp Response from ElasticSearch's query
	 * @param groupBy Group by criteria (just the names of the fields)
	 * @param function Function for converting each aggregated response into a record
	 * @param yearValues Map of aggregated value per year to be included in each record as a new value 'yearValue'. This will try to match by the implicit 'year' field of each record.
	 */
	public static List<Map<String, Object>> getInstances(SearchResponse sresp, String[] groupBy,
			BiFunction<Aggregations, String[], Map<String, Object>> function,
			Map<String,Double> yearValues) {
		
		List<Map<String, Object>> instances = SearchUtils.collectAggregations(sresp.getAggregations(), groupBy,
				function);
		
		//Remove null itens
		instances = instances.stream().filter(Objects::nonNull).collect(Collectors.toList());		

		for ( Map.Entry<String,Double> entry : yearValues.entrySet() ) {
			for ( Map<String, Object> map : instances ) {
				if ( entry.getKey().equals(map.get("year").toString()) ) {
					map.putIfAbsent("yearValue", entry.getValue());					
				}
			}
		}		
		
		return instances;

	}
}
