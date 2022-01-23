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
package org.idb.cacao.etl.repositories;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.idb.cacao.api.DocumentUploaded;
import org.idb.cacao.api.ETLContext;
import org.idb.cacao.api.ValidatedDataFieldNames;
import org.idb.cacao.api.errors.CommonErrors;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.utils.IndexNamesUtils;
import org.idb.cacao.api.utils.ScrollUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

/**
 * Implementation of a repository for pre-validated data for usage of ETL
 * 
 * @author Gustavo Figueiredo
 *
 */
@Repository
public class ValidatedDataRepository implements ETLContext.ValidatedDataRepository {

	@Autowired
	private RestHighLevelClient elasticsearchClient;
	
	@Autowired
	private DocumentUploadedRepository documentUploadedRepository;
	
	@Autowired
	private DocumentTemplateRepository documentTemplateRepository;

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.ETLContext.ValidatedDataRepository#getTemplates(java.lang.String)
	 */
	@Override
	public Collection<DocumentTemplate> getTemplates(String archetype) throws Exception {
		return documentTemplateRepository.findByArchetype(archetype);
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.ETLContext.ValidatedDataRepository#getUploads(java.lang.String, java.lang.String, java.lang.String, java.lang.Integer)
	 */
	@Override
	public Collection<DocumentUploaded> getUploads(String templateName, String templateVersion, String taxPayerId,
			Integer taxPeriodNumber) throws Exception {
		
		Page<DocumentUploaded> uploads =
		documentUploadedRepository.findByTemplateNameAndTemplateVersionAndTaxPayerIdAndTaxPeriodNumber(templateName, templateVersion, taxPayerId, taxPeriodNumber, PageRequest.of(0, 10_000));
		
		if (uploads.isEmpty())
			return Collections.emptyList();
		
		return uploads.getContent();
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.ETLContext.ValidatedDataRepository#hasValidation(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public boolean hasValidation(String templateName, String templateVersion, String fileId) throws Exception {
		final String index_name = IndexNamesUtils.formatIndexNameForValidatedData(templateName, templateVersion);
    	SearchRequest searchRequest = new SearchRequest(index_name);
    	SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
    			.query(QueryBuilders.termQuery(ValidatedDataFieldNames.FILE_ID.name()+".keyword", fileId));
        searchSourceBuilder.from(0);
        searchSourceBuilder.size(1);
    	searchRequest.source(searchSourceBuilder);
    	SearchResponse resp = null;
		try {
			resp = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
			return resp.getHits().getTotalHits().value>0;
		} catch (IOException ex) {
			if (CommonErrors.isErrorNoIndexFound(ex) || CommonErrors.isErrorNoMappingFoundForColumn(ex))
				return false; // no match
			throw ex;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.ETLContext.ValidatedDataRepository#getValidatedData(java.lang.String, java.lang.String, java.lang.String, java.util.Optional, java.util.Optional)
	 */
	@Override
	public Stream<Map<String, Object>> getValidatedData(String templateName, String templateVersion, String fileId, Optional<String[]> sortBy,
			final Optional<SortOrder> sortOrder) throws Exception {
		final String indexName = IndexNamesUtils.formatIndexNameForValidatedData(templateName, templateVersion);
		
		return ScrollUtils.findWithScroll(/*entity*/null, indexName, elasticsearchClient, 
			/*customizeSearch*/searchSourceBuilder->{
				searchSourceBuilder.query(QueryBuilders.termQuery(ValidatedDataFieldNames.FILE_ID.name()+".keyword", fileId));
				if (sortBy.isPresent()) {
					searchSourceBuilder.sort(Arrays.stream(sortBy.get())
						.map(field->SortBuilders.fieldSort(field).order(sortOrder.orElse(SortOrder.ASC)))
						.collect(Collectors.toList()));
				}
				else {
					searchSourceBuilder.sort(ValidatedDataFieldNames.TIMESTAMP.name(), SortOrder.DESC);
				}
			});
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.ETLContext.ValidatedDataRepository#getValidatedData(java.lang.String, java.lang.String, java.lang.String, org.elasticsearch.index.query.QueryBuilder)
	 */
	@Override
	public Optional<Map<String, Object>> getValidatedData(String templateName, String templateVersion, String fileId,
			QueryBuilder query) throws Exception {
		final String indexName = IndexNamesUtils.formatIndexNameForValidatedData(templateName, templateVersion);
    	SearchRequest searchRequest = new SearchRequest(indexName);
    	SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.from(0);
        searchSourceBuilder.size(1);
        
        BoolQueryBuilder composite_query = new BoolQueryBuilder();
        composite_query.must(QueryBuilders.termQuery(ValidatedDataFieldNames.FILE_ID.name()+".keyword", fileId));
        composite_query.must(query);        
        searchSourceBuilder.query(composite_query);
        
    	searchRequest.source(searchSourceBuilder);
    	SearchResponse resp = null;
		try {
			resp = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
			if (resp.getHits().getTotalHits().value>0) {
				return Optional.of(resp.getHits().getHits()[0].getSourceAsMap());
			}
			else {
				return Optional.empty();
			}
		} catch (IOException ex) {
			if (CommonErrors.isErrorNoIndexFound(ex) || CommonErrors.isErrorNoMappingFoundForColumn(ex))
				return Optional.empty(); // no match
			throw ex;
		}
	}
	
}
