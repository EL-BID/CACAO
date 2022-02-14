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
package org.idb.cacao.api.utils;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.cluster.metadata.MappingMetadata;
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
		catch (Throwable ex) {
			
			if (isErrorWindowTooLarge(ex)) {
				
				Class<?> entity = getParameterType(repository.getClass());
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
	 * Returns current mappings for a given index
	 */
	public static MappingMetadata getMapping(RestHighLevelClient client, String indexName) throws IOException {
		GetMappingsRequest request = new GetMappingsRequest(); 
		request.indices(indexName);
		return client.indices().getMapping(request, RequestOptions.DEFAULT).mappings().get(indexName);
	}

	/**
	 * Returns TRUE if there are any mappings for the index
	 */
	public static boolean hasMappings(RestHighLevelClient client, String indexName) throws IOException {
		MappingMetadata mappings = getMapping(client, indexName);
		return !mappings.sourceAsMap().isEmpty();
	}

	/**
	 * Do a search in ElasticSearch but avoid propagating errors related to lack of indexed objects
	 * @return Returns the response of the search, or returns NULL if encountered an error due to lack of mapping or index.
	 */
	public static SearchResponse searchIgnoringNoMapError(final RestHighLevelClient elasticsearchClient, final SearchRequest searchRequest, final String indexName) throws IOException {
    	SearchResponse sresp = null;
    	try {
    		sresp = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
    	}
    	catch (Throwable ex) {
    		if (CommonErrors.isErrorNoMappingFoundForColumn(ex)) {
    			if (!hasMappings(elasticsearchClient, indexName))
    				return null;
    			else
    				throw ex;
    		}
    		else if (CommonErrors.isErrorNoIndexFound(ex)) {
    			return null;
    		}
    		else {
    			throw ex;
    		}
    	}
    	return sresp;
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
				this.searchResponse = searchIgnoringNoMapError(client, searchRequest, indexName);
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

	/**
	 * Returns TRUE if the error is something like 'Result window is too large ...'
	 */
	private static boolean isErrorWindowTooLarge(Throwable ex) {
		if (ex!=null && ex.getMessage()!=null && ex.getMessage().contains("Result window is too large"))
			return true;
		if (ex!=null && ex.getCause()!=null && ex.getCause()!=ex)
			return isErrorWindowTooLarge(ex.getCause());
		if (ex instanceof ElasticsearchStatusException) {
			Throwable[] suppressed = ((ElasticsearchStatusException)ex).getSuppressed();
			if (suppressed!=null && suppressed.length>0) {
				for (Throwable sup:suppressed) {
					if (isErrorWindowTooLarge(sup))
						return true;
				}
			}
		}
		return false;
	}

	/**
	 * Given some class, returns the parameterized type of its superclass, and repeat
	 * this search in superclasses hierarchy, util it finds one. Returns NULL if found none. 
	 */
	private static Class<?> getParameterType(Class<?> some_class) {
		Class<?> cl = some_class;
		while (cl!=null) {
			Type[] ginterfaces = cl.getGenericInterfaces();
			if (ginterfaces!=null && ginterfaces.length>0) {
				for (Type i:ginterfaces) {
					if (i instanceof ParameterizedType) {
						return (Class<?>)((ParameterizedType)i).getActualTypeArguments()[0];
					}
				}
			}
			if (cl.getGenericSuperclass() instanceof ParameterizedType) {
				return (Class<?>)((ParameterizedType)cl.getGenericSuperclass()).getActualTypeArguments()[0];
			}
			Class<?> [] interfaces = cl.getInterfaces();
			if (interfaces!=null && interfaces.length>0) {
				for (Class<?> i:interfaces) {
					Class<?> ptype = getParameterType(i);
					if (ptype!=null)
						return ptype;
				}
			}
			cl = cl.getSuperclass();
		}
		return null;
	}


}
