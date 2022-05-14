/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.api.utils;

import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.idb.cacao.api.errors.CommonErrors;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.util.CloseableIterator;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Utility methods for generic search elastic search using 'scroll' operations to overcome limit
 * in the number of records returned.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class ScrollUtils {

	/**
	 * Returns a Stream for all data from a repository. In case of 'window too large' error, will 'paginate'
	 * through all the results.
	 */
	public static <T, ID> Stream<T> findAll(PagingAndSortingRepository<T, ID> repository, RestHighLevelClient clientForScrollSearch, long durationInMinutes) {
		
		try {
			return StreamSupport.stream(repository.findAll().spliterator(),false);
		}
		catch (Exception ex) {
			
			if (CommonErrors.isErrorWindowTooLarge(ex)) {
				
				Class<?> entity = ReflectUtils.getParameterType(repository.getClass());
				if (entity==null) {
					// Could not find entity class related to repository, so we cant' use 'ConsumeWithScrollAPIIterator'
					// Throws the first error
					throw ex;
				}

				Document doc_anon = entity.getAnnotation(Document.class);
				String indexName = doc_anon.indexName();

				return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new ConsumeWithScrollAPIIterator<T>(entity, indexName, clientForScrollSearch, /*customizeSearch*/null, durationInMinutes), Spliterator.ORDERED), false);
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
		
		return findWithScroll(entity, indexName, clientForScrollSearch, customizeSearch, /*durationInMinutes*/1L);
	}

	/**
	 * Returns a Stream for all data from a repository. It uses 'scroll' functionality of ElasticSearch API for ensuring all data is iterated.<BR>
	 * You may inform optional 'customizeSearch' parameter to provide extra customization to your search (e.g.: providing some filters).
	 */
	public static <T> Stream<T> findWithScroll(Class<T> entity, String indexName, RestHighLevelClient clientForScrollSearch,
			Consumer<SearchSourceBuilder> customizeSearch,
			long durationInMinutes) {
		
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new ConsumeWithScrollAPIIterator<T>(entity, indexName, clientForScrollSearch, customizeSearch, durationInMinutes), 
				Spliterator.ORDERED), false);
		
	}

	/**
	 * Returns a Stream for all data from a repository. It uses 'scroll' functionality of ElasticSearch API for ensuring all data is iterated.<BR>
	 * You may inform optional 'customizeSearch' parameter to provide extra customization to your search (e.g.: providing some filters).
	 */
	public static <T> Stream<T> findWithScroll(Class<T> entity, String indexName, RestHighLevelClient clientForScrollSearch,
			Consumer<SearchSourceBuilder> customizeSearch,
			long durationInMinutes,
			boolean includeIndex) {
		
		ConsumeWithScrollAPIIterator<T> consumer = new ConsumeWithScrollAPIIterator<T>(entity, indexName, clientForScrollSearch, customizeSearch, durationInMinutes);
		consumer.setIncludeIndex(includeIndex);
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(consumer, 
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
		private boolean includeIndex;

		ConsumeWithScrollAPIIterator(Class<?> entity, String indexName, RestHighLevelClient client,
				Consumer<SearchSourceBuilder> customizeSearch,
				long durationInMinutes) {
			this.scroll = new Scroll(TimeValue.timeValueMinutes(durationInMinutes));
			this.client = client;
			SearchRequest searchRequest = new SearchRequest(indexName);
			searchRequest.scroll(scroll);
			SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
			if (customizeSearch!=null)
				customizeSearch.accept(searchSourceBuilder);
			searchRequest.source(searchSourceBuilder);
			try {
				this.searchResponse = MappingUtils.searchIgnoringNoMapError(client, searchRequest, indexName);
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
			if(!hasNext()){
				throw new NoSuchElementException();
			}
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
		
		public void setIncludeIndex(boolean includeIndex) {
			this.includeIndex = includeIndex;
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
						if (CommonErrors.isErrorThreadInterrupted(e)) {
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
			if (includeIndex)
				map.put("index", hit.getIndex());
			map.remove("_class");
			if (entity==null)
				return (T)map;
			else
				return (T)mapper.convertValue(map, entity);
		}
	}

}
