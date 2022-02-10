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
package org.idb.cacao.web.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaBuilder.In;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;

import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchPhrasePrefixQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.WildcardQueryBuilder;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.hibernate.jpa.QueryHints;
import org.idb.cacao.api.utils.DateTimeUtils;
import org.idb.cacao.api.utils.ParserUtils;
import org.idb.cacao.web.controllers.AdvancedSearch;
import org.idb.cacao.web.dto.TabulatorFilter;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.util.CloseableIterator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.Lists;

/**
 * Utility methods for advanced search facility with elastic search base
 * 
 * @author Gustavo Figueiredo
 *
 */
public class SearchUtils {
	
	public static final int DEFAULT_PAGE_SIZE = 5;
	
	public static Optional<AdvancedSearch> fromJSON(Optional<String> as_json) {
		if (as_json==null || !as_json.isPresent())
			return Optional.empty();
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		try {
			return Optional.ofNullable(mapper.readValue(as_json.get(), AdvancedSearch.class));
		} catch (JsonProcessingException e) {
			return Optional.empty();
		}
	}

	public static Optional<AdvancedSearch> fromTabulatorJSON(Optional<String> as_json) {
		if (as_json==null || !as_json.isPresent())
			return Optional.empty();
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		try {
			TabulatorFilter[] filters = mapper.readValue(as_json.get(), TabulatorFilter[].class);
			AdvancedSearch search = new AdvancedSearch();
			Arrays.stream(filters)
			  .forEach(filter -> search.addFilter( filter.isString() ? 
					  new AdvancedSearch.QueryFilterTerm(filter.getField(), filter.getStringValue() + "*") :
					  new AdvancedSearch.QueryFilterDate(filter.getField(), filter.getProperty("start"), filter.getProperty("end"))));
			return Optional.of(search);
		} catch (JsonProcessingException e) {
			return Optional.empty();
		}
	}
	
	public static Optional<String> toJSON(Optional<AdvancedSearch> queryArguments) {
		if (queryArguments==null || !queryArguments.isPresent())
			return Optional.empty();
		ObjectMapper mapper = new ObjectMapper();
		try {
			return Optional.ofNullable(mapper.writeValueAsString(queryArguments.get()));
		} catch (JsonProcessingException e) {
			return Optional.empty();
		}
	}

	/**
	 * Performs advanced search at ElasticSearch over some entity of type 'entity'
	 * with query parameters informed as 'queryArguments' pairs of names and values (wildcards are supported)
	 * and with optional pagination control.
	 * @param page Optional page number to start with (1-based)
	 */
	public static <T> Page<T> doSearch(
			final AdvancedSearch queryArguments,
			final Class<T> entity,
			final RestHighLevelClient elasticsearchClient,
			final Optional<Integer> page,
			final Optional<Integer> size,
			final Optional<String> sortBy,
			final Optional<SortOrder> sortOrder) throws IOException {
		
		Document an_doc = entity.getAnnotation(Document.class);
		if (an_doc==null)
			return Page.empty();
		
		String indexName = an_doc.indexName();
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		Page<Map<String,Object>> result = doSearch(queryArguments, entity, indexName, elasticsearchClient, page, size, sortBy, sortOrder);
		return result.map(m->mapper.convertValue(m, entity));
	}
	
	/**
	 * Performs advanced search at ElasticSearch over some entity of any type
	 * with query parameters informed as 'queryArguments' pairs of names and values (wildcards are supported)
	 * and with optional pagination control.
	 * @param page Optional page number to start with (1-based)
	 */
	public static Page<Map<String,Object>> doSearch(
			final AdvancedSearch queryArguments,
			final Class<?> entity,
			final String indexName,
			final RestHighLevelClient elasticsearchClient,
			final Optional<Integer> page,
			final Optional<Integer> size,
			final Optional<String> sortBy,
			final Optional<SortOrder> sortOrder) throws IOException {	
		
		if (indexName==null || indexName.trim().length()==0)
			return Page.empty();
		
    	SearchRequest searchRequest = new SearchRequest(indexName);
    	
    	BoolQueryBuilder query = QueryBuilders.boolQuery();
    	
    	if (queryArguments!=null && !queryArguments.isEmpty()) {
    		for (AdvancedSearch.QueryFilter field:queryArguments.getFilters()) {
    			addFilter(field, query::must, entity);
    		}
    	}
    	
    	SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
    			.query(query); 
    	
    	if (page.isPresent() && size.isPresent()) {
	        int  offset = (page.get()-1) * size.get();
	        searchSourceBuilder.from(offset);
	        searchSourceBuilder.size(size.get());
    	}
    	
    	if (sortBy.isPresent()) {
    		searchSourceBuilder.sort(sortBy.get(), sortOrder.orElse(SortOrder.ASC));
    	}
        
    	searchRequest.source(searchSourceBuilder);
    	SearchResponse sresp = null;    	
		sresp = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
		long totalCount = sresp.getHits().getTotalHits().value;
		int page_hits = sresp.getHits().getHits().length;
		int pageNumber = (page.isPresent()) ? page.get() : 1;
		int pageSize = Math.max(DEFAULT_PAGE_SIZE, (size.isPresent()) ? size.get() : page_hits);
		
		List<Map<String,Object>> results = new ArrayList<>(sresp.getHits().getHits().length);
		for (SearchHit hit : sresp.getHits()) {
			Map<String, Object> map = hit.getSourceAsMap();
			map.put("id", hit.getId());
			map.remove("_class");
			results.add(map);
		}
		
		return new PageImpl<>(results, PageRequest.of(pageNumber-1, pageSize), totalCount);
	}

	/**
	 * Performs advanced search at ElasticSearch filtering by searchText on a
	 * specific field and returns the top entries
	 * Returns a list of documents. Can be useful in autocomplete
	 */
	public static List<Map<String, Object>> doSearchTopWithFilter(
			final RestHighLevelClient elasticsearchClient, 
			final Class<?> entity,
			final String searchField1,
			final String searchField2,
			String searchText,
			
			int size) throws IOException {
		Document doc = entity.getAnnotation(Document.class);
		final String indexName = doc.indexName();
		final SearchSourceBuilder builder = new SearchSourceBuilder().size(size);
		if(searchText!=null && !searchText.isEmpty()) {
			BoolQueryBuilder query = QueryBuilders.boolQuery();
			query.should(new MatchQueryBuilder(searchField1, searchText));
			query.should(new MatchPhrasePrefixQueryBuilder(searchField1, searchText));
			if(searchField2!=null) {
				query.should(new MatchQueryBuilder(searchField2, searchText));
				query.should(new MatchPhrasePrefixQueryBuilder(searchField2, searchText));
			}
			builder.query(query);
		}
		SearchRequest searchRequest = new SearchRequest(indexName).source(builder);
		final SearchResponse response = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
		List<Map<String, Object>> result = Arrays.stream(response.getHits().getHits())
		    .map(hit -> {
		    	Map<String, Object> map = hit.getSourceAsMap();
		    	map.put("id", hit.getId());
		    	map.remove("_class");
		    	return map;
		    })
		    .collect(Collectors.toList());
        return result;
	}
	
	/**
	 * Performs advanced search at ElasticSearch filtering by searchText on a
	 * specific field and returns the top distinct values from distinctField.
	 * Returns a list of string values, useful in autocomplete.
	 */
	public static List<String> doSearchTopDistinctWithFilter(
			final RestHighLevelClient elasticsearchClient, 
			final Class<?> entity,
			final String distinctField,
			final String searchField,
			String searchText) throws IOException {
		Document doc = entity.getAnnotation(Document.class);
		final String indexName = doc.indexName();
		final TermsAggregationBuilder aggregation = AggregationBuilders.terms("top_items")
	            .field(distinctField);
		final SearchSourceBuilder builder = new SearchSourceBuilder().aggregation(aggregation);
		if(searchText!=null && !searchText.isEmpty()) {
			BoolQueryBuilder query = QueryBuilders.boolQuery();
			query.should(new MatchQueryBuilder(searchField, searchText));
			query.should(new MatchPhrasePrefixQueryBuilder(searchField, searchText));
			
			builder.query(query);
		}
		SearchRequest searchRequest = new SearchRequest(indexName).source(builder);
		final SearchResponse response = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);

        final Map<String, Aggregation> results = response.getAggregations()
            .asMap();
        final ParsedStringTerms topTags = (ParsedStringTerms) results.get("top_items");

        final List<String> keys = topTags.getBuckets()
            .stream()
            .map(MultiBucketsAggregation.Bucket::getKeyAsString)
            .sorted()
            .collect(Collectors.toList());
        return keys;
	}
	
	/**
	 * Add filters programmed in 'field' object into 'query_builder' object
	 */
	public static void addFilter(AdvancedSearch.QueryFilter field, Consumer<QueryBuilder> query_builder, final Class<?> entity) {
		if (field.isEmpty()) {
			return;
		}
		if (field instanceof AdvancedSearch.QueryFilterTerm) {
			String argument = ((AdvancedSearch.QueryFilterTerm)field).getArgument();
			if (argument!=null && argument.indexOf(' ')>0) {
				List<String> multiple_terms = Arrays.stream(argument.split("\\s+")).filter(a->a.length()>0).collect(Collectors.toList());
				if (multiple_terms.isEmpty())
					return;
				if (multiple_terms.size()==1) {
					String v = multiple_terms.get(0);
					if (v!=null)
						v = v.toLowerCase();
					query_builder.accept(new WildcardQueryBuilder(field.getName(), v));
				}
				else {
					BoolQueryBuilder subquery = QueryBuilders.boolQuery();
					for (String sub_term: multiple_terms) {
						if (sub_term!=null)
							sub_term = sub_term.toLowerCase();
						subquery = subquery.must(new WildcardQueryBuilder(field.getName(), sub_term));
					}
					query_builder.accept(subquery);
				}
			}
			else {
				if (argument!=null)
					argument = argument.toLowerCase();
				query_builder.accept(new WildcardQueryBuilder(field.getName(), argument));
			}
		}
		else if (field instanceof AdvancedSearch.QueryFilterEnum) {
			if (((AdvancedSearch.QueryFilterEnum)field).getEnumeration()==null) {
				Class<?> type = ReflectUtils.getMemberType(entity, field.getName());
				if (type!=null && type.isEnum()) {
					String v = ((AdvancedSearch.QueryFilterEnum)field).getSelectedConstant(type);
					if (v!=null)
						v = v.toLowerCase();
					query_builder.accept(new WildcardQueryBuilder(field.getName(), v));
				}
			}
			else {
				String v = ((AdvancedSearch.QueryFilterEnum)field).getSelectedConstant();
				if (v!=null)
					v = v.toLowerCase();
				query_builder.accept(new WildcardQueryBuilder(field.getName(), v));
			}
		}
		else if (field instanceof AdvancedSearch.QueryFilterBoolean) {
			if (((AdvancedSearch.QueryFilterBoolean)field).isArgumentTrue())
				query_builder.accept(new TermQueryBuilder(field.getName(), true));
			else if (((AdvancedSearch.QueryFilterBoolean)field).isArgumentFalse())
				query_builder.accept(new TermQueryBuilder(field.getName(), false));	
		}
		else if (field instanceof AdvancedSearch.QueryFilterRange) {
			RangeQueryBuilder b = new RangeQueryBuilder(field.getName());
			if (((AdvancedSearch.QueryFilterRange)field).hasStart())
				b = b.from(((AdvancedSearch.QueryFilterRange)field).getStart(), /*includeLower*/true);
			if (((AdvancedSearch.QueryFilterRange)field).hasEnd())
				b = b.to(((AdvancedSearch.QueryFilterRange)field).getEnd(), /*includeUpper*/true);
			query_builder.accept(b);
		}    			
		else if (field instanceof AdvancedSearch.QueryFilterValue) {
			RangeQueryBuilder b = new RangeQueryBuilder(field.getName());
			if (((AdvancedSearch.QueryFilterValue)field).hasStart())
				b = b.from(((AdvancedSearch.QueryFilterValue)field).getStart(), /*includeLower*/true);
			if (((AdvancedSearch.QueryFilterValue)field).hasEnd())
				b = b.to(((AdvancedSearch.QueryFilterValue)field).getEnd(), /*includeUpper*/true);
			query_builder.accept(b);
		}    			
		else if (field instanceof AdvancedSearch.QueryFilterDate) {
			RangeQueryBuilder b = new RangeQueryBuilder(field.getName());
			if (((AdvancedSearch.QueryFilterDate)field).hasStart())
				b = b.from(ParserUtils.formatTimestampES(ParserUtils.parseTimestamp(((AdvancedSearch.QueryFilterDate)field).getStart())), /*includeLower*/true);
			if (((AdvancedSearch.QueryFilterDate)field).hasEnd())
				b = b.to(ParserUtils.formatTimestampES(DateTimeUtils.lastTimeOfDay(ParserUtils.parseTimestamp(((AdvancedSearch.QueryFilterDate)field).getEnd()))), /*includeUpper*/true);
			query_builder.accept(b);
		}    		
		else if (field instanceof AdvancedSearch.QueryFilterList) {
			BoolQueryBuilder subquery = QueryBuilders.boolQuery();
			for (String argument: ((AdvancedSearch.QueryFilterList)field).getArgument()) {
				subquery = subquery.should(new TermQueryBuilder(field.getName(), argument));
			}
			subquery = subquery.minimumShouldMatch(1);
			query_builder.accept(subquery);
		}
		else if (field instanceof AdvancedSearch.QueryFilterOr) {
			List<AdvancedSearch.QueryFilter> alternatives = ((AdvancedSearch.QueryFilterOr)field).getAlternatives();
			if (alternatives==null || alternatives.isEmpty())
				return;
			BoolQueryBuilder nested_query = QueryBuilders.boolQuery();
			for (AdvancedSearch.QueryFilter nested: alternatives) {
				addFilter(nested, nested_query::should, entity);
			}
			query_builder.accept(nested_query);
		}
	}

	/**
	 * Performs advanced search at JPA over some entity of any type
	 * with query parameters informed as 'queryArguments' pairs of names and values (wildcards are supported)
	 * and with optional pagination control.
	 * @param page Optional page number to start with (1-based)
	 */
	public static <T> Page<T> doSearch(
			final AdvancedSearch queryArguments,
			final Class<T> entity,
			final EntityManager em,
			final boolean computeTotalCount,
			final Optional<Integer> page,
			final Optional<Integer> size,
			final Optional<String> sortBy,
			final Optional<SortOrder> sortOrder) throws IOException {	
		
		if (entity==null)
			return Page.empty();
		
		CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(entity);
		Root<T> entityRoot = criteriaQuery.from(entity);
		
		addCriteriaQueries(queryArguments, criteriaBuilder, entity, entityRoot, criteriaQuery::where);
    	
    	if (sortBy.isPresent()) {
    		switch (sortOrder.orElse(SortOrder.ASC)) {
    		case ASC:    			
    			criteriaQuery = criteriaQuery.orderBy(criteriaBuilder.asc(entityRoot.get(sortBy.get())));
    			break;
    		case DESC:
    			criteriaQuery = criteriaQuery.orderBy(criteriaBuilder.desc(entityRoot.get(sortBy.get())));
    			break;
    		}
    	}
    	
    	long totalCount = 0;
    	if (computeTotalCount) {
    		CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
    		Root<T> countRoot = countQuery.from(entity);
    		countQuery.select(criteriaBuilder.count(countRoot));
    		addCriteriaQueries(queryArguments, criteriaBuilder, entity, countRoot, countQuery::where);
    		totalCount = Optional.ofNullable(em.createQuery(countQuery).getSingleResult()).orElse(0L);
    	}

    	Query query = em.createQuery(criteriaQuery);
    	query.setHint(QueryHints.HINT_READONLY, true);
    	if (page.isPresent() && size.isPresent() && page.get()>1) {
	        int  offset = (page.get()-1) * size.get();
    		query.setFirstResult(offset);
    		query.setMaxResults(size.get());
    	}
    	else if (size.isPresent()) {    		
	    	query.setMaxResults(size.get());
    	}
    	else {
    		query.setMaxResults(DEFAULT_PAGE_SIZE);
    	}
    	        
    	@SuppressWarnings("unchecked")
		List<T> results = (List<T>)query.getResultList();
    	
    	if (totalCount==0) {
    		totalCount = results.size();
    	}
		
		int pageNumber = (page.isPresent()) ? page.get() : 1;
		int pageSize = Math.max(DEFAULT_PAGE_SIZE, (size.isPresent()) ? size.get() : DEFAULT_PAGE_SIZE);

		return new PageImpl<>(results, PageRequest.of(pageNumber-1, pageSize), totalCount);
	}
	
	/**
	 * Given 'advanced search filters', populates 'consumer' with the corresponding JPA boolean expressions.
	 */
	public static <T> void addCriteriaQueries(final AdvancedSearch queryArguments,
			final CriteriaBuilder criteriaBuilder,
			final Class<T> entity,
			final Root<T> entityRoot,
			final Consumer<Expression<Boolean>> consumer) {
    	if (queryArguments!=null && !queryArguments.isEmpty()) {
    		for (AdvancedSearch.QueryFilter field:queryArguments.getFilters()) {
    			addFilter(field, criteriaBuilder, entity, entityRoot, consumer);
    		}
    	}

	}
	
	/**
	 * Add filters programmed in 'field' object into 'consumer' object
	 */
	private static <T> void addFilter(AdvancedSearch.QueryFilter field, 
			final CriteriaBuilder criteriaBuilder, 
			final Class<?> entity,
			final Root<T> entityRoot,
			final Consumer<Expression<Boolean>> consumer) {
		if (field.isEmpty())
			return;
		if (field instanceof AdvancedSearch.QueryFilterTerm) {
			Class<?> type = ReflectUtils.getMemberType(entity, field.getName());
			if (type!=null && type.isEnum()) {
				// First search enum constants, and then use this as search constraints
				In<Object> in_clause = criteriaBuilder.in(entityRoot.get(field.getName()));    					
				filterEnumConstants(type, adaptToRegex(((AdvancedSearch.QueryFilterTerm)field).getArgument()), field.getMessageSource(), in_clause::value);
				consumer.accept(in_clause);
			}
			else {
				String pattern = adaptToJPALikeExpression(((AdvancedSearch.QueryFilterTerm)field).getArgument());
				consumer.accept(criteriaBuilder.like(criteriaBuilder.upper(entityRoot.get(field.getName())), pattern.toUpperCase()));
			}
		}
		if (field instanceof AdvancedSearch.QueryFilterEnum) {
			String pattern = adaptToJPALikeExpression(((AdvancedSearch.QueryFilterEnum)field).getSelectedConstant());
			consumer.accept(criteriaBuilder.like(entityRoot.get(field.getName()), pattern));
		}
		else if (field instanceof AdvancedSearch.QueryFilterBoolean) {
			if (((AdvancedSearch.QueryFilterBoolean)field).isArgumentTrue())
				consumer.accept(criteriaBuilder.equal(entityRoot.get(field.getName()), Boolean.TRUE));
			else if (((AdvancedSearch.QueryFilterBoolean)field).isArgumentFalse())
				consumer.accept(criteriaBuilder.equal(entityRoot.get(field.getName()), Boolean.FALSE));
		}
		else if (field instanceof AdvancedSearch.QueryFilterRange) {
			if (((AdvancedSearch.QueryFilterRange)field).hasStart())
				consumer.accept(criteriaBuilder.greaterThanOrEqualTo(entityRoot.get(field.getName()), ((AdvancedSearch.QueryFilterRange)field).getStart()));
			if (((AdvancedSearch.QueryFilterRange)field).hasEnd())
				consumer.accept(criteriaBuilder.lessThanOrEqualTo(entityRoot.get(field.getName()), ((AdvancedSearch.QueryFilterRange)field).getEnd()));
		}    			
		else if (field instanceof AdvancedSearch.QueryFilterValue) {
			if (((AdvancedSearch.QueryFilterValue)field).hasStart())
				consumer.accept(criteriaBuilder.ge(entityRoot.get(field.getName()), ((AdvancedSearch.QueryFilterValue)field).getStart()));
			if (((AdvancedSearch.QueryFilterValue)field).hasEnd())
				consumer.accept(criteriaBuilder.le(entityRoot.get(field.getName()), ((AdvancedSearch.QueryFilterValue)field).getEnd()));
		}    			
		else if (field instanceof AdvancedSearch.QueryFilterDate) {    				
			if (((AdvancedSearch.QueryFilterDate)field).hasStart())
				consumer.accept(criteriaBuilder.greaterThanOrEqualTo(entityRoot.get(field.getName()), ParserUtils.parseFlexibleDate(((AdvancedSearch.QueryFilterDate)field).getStart())));
			if (((AdvancedSearch.QueryFilterDate)field).hasEnd())
				consumer.accept(criteriaBuilder.lessThanOrEqualTo(entityRoot.get(field.getName()), ParserUtils.parseFlexibleDate(((AdvancedSearch.QueryFilterDate)field).getEnd())));
		}    			
		else if (field instanceof AdvancedSearch.QueryFilterList) {
			In<Object> in_clause = criteriaBuilder.in(entityRoot.get(field.getName()));
			for (String argument: ((AdvancedSearch.QueryFilterList)field).getArgument()) {
				in_clause.value(argument);
			}
			consumer.accept(in_clause);
		}
		else if (field instanceof AdvancedSearch.QueryFilterOr) {
			List<AdvancedSearch.QueryFilter> alternatives = ((AdvancedSearch.QueryFilterOr)field).getAlternatives();
			if (alternatives==null || alternatives.isEmpty())
				return;
			List<Expression<Boolean>> expressions = new ArrayList<>(alternatives.size());
			for (AdvancedSearch.QueryFilter nested: alternatives) {
				addFilter(nested, criteriaBuilder, entity, entityRoot, expressions::add);
			}
			if (expressions.size()==1) {
				consumer.accept(expressions.get(0));
			}
			else if (expressions.size()==2) {
				consumer.accept(criteriaBuilder.or(expressions.get(0), expressions.get(1)));
			}
			else if (expressions.size()>2) {
				consumer.accept(expressions.stream().reduce(criteriaBuilder::or).get());
			}
		}
	}
	
	/**
	 * Filter enumeration constants based on a regular expression. Make the best effort to matching enumeration constants, using whatever
	 * information it has (i.e.: use either the constant name, or the 'toString' returned string, or the 'messageSource' using the 'toString' returned string
	 * as property name). 
	 */
	public static void filterEnumConstants(Class<?> enum_type, String regex, MessageSource messageSource, Consumer<Object> consumer) {
		Pattern p;
		try {
			p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		}
		catch (Throwable ex) {
			return;
		}
		for (Object enum_value:enum_type.getEnumConstants()) {
			String s = ((Enum<?>)enum_value).name();
			if (p.matcher(s).find()) {
				consumer.accept(enum_value);
			}
			else {
				s = enum_value.toString();
				if (s!=null && p.matcher(s).find()) {
					consumer.accept(enum_value);
				}
				else if (messageSource!=null) {
					try {
						s = messageSource.getMessage(s, null, LocaleContextHolder.getLocale());
					}
					catch (Throwable ex) {
						s = null;
					}
					if (s!=null && p.matcher(s).find()) {
						consumer.accept(enum_value);
					}
				}
			}
		}
	}
	
	/**
	 * Adapts an elastic search wildcard expression to JPA 'LIKE' expression syntax
	 */
	public static String adaptToJPALikeExpression(String expression) {
		if (expression==null)
			return null;
		return expression.replace('*', '%');
	}
	
	/**
	 * Adapts an elastic search wildcard expression to REGEX
	 */
	public static String adaptToRegex(String expression) {
		if (expression==null)
			return null;
		return expression.replace("*", ".*");
	}

	/**
	 * Returns a Stream for all data from a repository. In case of 'window too large' error, will 'paginate'
	 * through all the results.
	 */
	public static <T, ID> Stream<T> findAll(PagingAndSortingRepository<T, ID> repository, RestHighLevelClient clientForScrollSearch) {
		
		try {
			return StreamSupport.stream(repository.findAll().spliterator(),false);
		}
		catch (Throwable ex) {
			
			if (ErrorUtils.isErrorWindowTooLarge(ex)) {
				
				Class<?> entity = ReflectUtils.getParameterType(repository.getClass());
				if (entity==null) {
					// Could not find entity class related to repository, so we cant' use 'ConsumeWithScrollAPIIterator'
					// Throws the first error
					throw ex;
				}

				Document doc_anon = entity.getAnnotation(Document.class);
				String indexName = doc_anon.indexName();

				return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new ConsumeWithScrollAPIIterator<T>(entity, indexName, clientForScrollSearch, /*customizeSearch*/null), Spliterator.ORDERED), false);
			}
			
			throw ex;
			
		}
		
	}
	
	/**
	 * Returns a Stream for all data from a repository. It uses 'scroll' functionality of ElasticSearch API for ensuring all data is iterated.<BR>
	 * You may inform optional 'customizeSearch' parameter to provide extra customization to your search (e.g.: providing some filters).
	 */
	public static <T> Stream<T> findWithScroll(Class<T> entity, String indexName, RestHighLevelClient clientForScrollSearch,
			Consumer<SearchSourceBuilder> customizeSearch) {
		
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new ConsumeWithScrollAPIIterator<T>(entity, indexName, clientForScrollSearch, customizeSearch), 
				Spliterator.ORDERED), false);
		
	}
	
	/**
	 * An 'iterator' that consumes all records using the SCROLL API (more efficient than searching through pages)
	 * @author Gustavo Figueiredo
	 */
	private static class ConsumeWithScrollAPIIterator<T> implements CloseableIterator<T> {
		private final Scroll scroll;
		private final RestHighLevelClient client;
		private final Class<?> entity;
		private SearchResponse searchResponse;
		private ObjectMapper mapper;
		
		private T next;
		private boolean moved;
		private boolean finished;
		private String scrollId;
		private SearchHit[] searchHits;
		private int searchHitsIndex;

		ConsumeWithScrollAPIIterator(Class<?> entity, String indexName, RestHighLevelClient client,
				Consumer<SearchSourceBuilder> customizeSearch) {
			this.scroll = new Scroll(TimeValue.timeValueMinutes(1L));
			this.client = client;
			SearchRequest searchRequest = new SearchRequest(indexName);
			searchRequest.scroll(scroll);
			SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
			if (customizeSearch!=null)
				customizeSearch.accept(searchSourceBuilder);
			searchRequest.source(searchSourceBuilder);
			try {
				this.searchResponse = ESUtils.searchIgnoringNoMapError(client, searchRequest, indexName);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			this.mapper = new ObjectMapper();
			this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			this.mapper.registerModule(new JavaTimeModule());
			this.entity = entity;
			
			if (searchResponse==null) {
				this.moved = true;
				this.finished = true;
				this.next = null;
			}
			else {
				this.scrollId = searchResponse.getScrollId();
				this.searchHits = searchResponse.getHits().getHits();
				this.searchHitsIndex = 0;
			}
		}

		@Override
		public boolean hasNext() {
			if (!moved)
				moveForward();
			return next!=null;
		}

		@Override
		public T next() {
			if (!moved)
				moveForward();
			moved = false;
			return next;
		}
		
		@Override
		public void close() {
			ClearScrollRequest clearScrollRequest = new ClearScrollRequest(); 
			clearScrollRequest.addScrollId(scrollId);
			try {
				client.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
			} catch (IOException e) {
				// !!!
			}
		}

		private void moveForward() {
			next = null;
			if (!finished) {
				if (searchHits==null || searchHits.length==0) {
					finished = true;
				}
				else if (searchHitsIndex < searchHits.length) {
					next = treatResponse(searchHits[searchHitsIndex++]);
				}
				else {
					searchHitsIndex = 0;
				    SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId); 
				    scrollRequest.scroll(scroll);
				    try {
						searchResponse = client.scroll(scrollRequest, RequestOptions.DEFAULT);
					} catch (IOException e) {
						if (ErrorUtils.isErrorThreadInterrupted(e)) {
							finished = true;
							moved = true;
							return;
						}
						throw new RuntimeException(e);
					} catch (Throwable e) {
						if (e.getCause() instanceof InterruptedException) {
							finished = true;
							moved = true;
							return;
						}
						else {
							throw e;
						}
					}
				    scrollId = searchResponse.getScrollId();
				    searchHits = searchResponse.getHits().getHits();
				    moveForward();
				    return;
				}
			}
			moved = true;
		}
		
		@SuppressWarnings("unchecked")
		private T treatResponse(SearchHit hit) {
			Map<String, Object> map = hit.getSourceAsMap();
			map.put("id", hit.getId());
			map.remove("_class");
			if (entity==null || Map.class==entity)
				return (T)map;
			else
				return (T)mapper.convertValue(map, entity);
		}
	}
	
	/**
	 * Given a set of filters and some filter names, remove them from the original filters set and returns a new filter set
	 * containing only those filters. The original filters set may be modified by this method as well.
	 */
	public static Optional<AdvancedSearch> splitFilters(Optional<AdvancedSearch> filters, String... filterNames) {
		if (filters==null || !filters.isPresent())
			return Optional.empty();
		
		if (filterNames==null || filterNames.length==0)
			return Optional.empty();
		
		return filters.get().splitFilters(filterNames);
	}
	
	/**
	 * Create an {@link TermsAggregationBuilder} for ES search based on fields and scripts received as parameters. 
	 * 
	 * @param parentAggregation	Parent aggregation
	 * @param fields			An array with field names or {@link Script} objects for aggregation
	 * @param metrics			Metrics to aggregate for each field combination
	 * @return					{@link TermsAggregationBuilder} with all field and script aggregations
	 */
	public static TermsAggregationBuilder aggregationBuilder(TermsAggregationBuilder parentAggregation, Object[] fields, 
			AggregationBuilder... metrics) {
		TermsAggregationBuilder agg = fields[0] instanceof Script ? AggregationBuilders.terms(((Script)fields[0]).getId()).size(10_000).script(((Script)fields[0]).getScript()) :
			AggregationBuilders.terms((String)fields[0]).size(10_000).field((String)fields[0]);
		
		if (fields.length>1) {
			agg = agg.subAggregation(aggregationBuilder(agg, Arrays.copyOfRange(fields, 1, fields.length), metrics));
		} else {
			final TermsAggregationBuilder groupAgg = agg;
			Arrays.stream(metrics)
			  .forEach(m -> groupAgg.subAggregation(m));
		}
		return agg;
	}
	
	public static TermsAggregationBuilder aggregationBuilder(TermsAggregationBuilder parentAggregation, String[] fields, AggregationBuilder... metrics) {
		TermsAggregationBuilder agg = AggregationBuilders.terms(fields[0]).size(10_000).field(fields[0]);
		if (fields.length>1) {
			agg = agg.subAggregation(aggregationBuilder(agg, Arrays.copyOfRange(fields, 1, fields.length), metrics));
		} else {
			final TermsAggregationBuilder groupAgg = agg;
			Arrays.stream(metrics)
			  .forEach(m -> groupAgg.subAggregation(m));
		}
		return agg;
	}
	
	private static <R> void collectAggregationLevel(Aggregations aggregations, Object[] fields, BiFunction<Aggregations, String[], R> function, 
			int level, String[] values, List<R> results) {
		Terms terms = aggregations.get(fields[level] instanceof Script ? ((Script)fields[level]).getId() : (String)fields[level]);
		boolean lastLevel = level == (fields.length-1);
		
		for(Terms.Bucket bucket: terms.getBuckets()) {
			values[level] = bucket.getKeyAsString();
			if (lastLevel) {
				R obj = function.apply(bucket.getAggregations(), values);
				if ( obj != null )
					results.add(obj);
			}
			else {
				collectAggregationLevel(bucket.getAggregations(), fields, function, level+1, values, results);
			}		
		}
	}
	
	public static <R> List<R> collectAggregations(Aggregations agg, Object[] fields, BiFunction<Aggregations, String[], R> function) {
		List<R> result = Lists.newArrayList();
		String[] values = new String[fields.length];
		collectAggregationLevel(agg, fields, function, 0, values, result);
		return result;
	}
	
}
