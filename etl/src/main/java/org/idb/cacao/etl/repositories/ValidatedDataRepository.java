/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
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
import org.idb.cacao.api.errors.GeneralException;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.utils.IndexNamesUtils;
import org.idb.cacao.api.utils.ScrollUtils;
import org.idb.cacao.api.utils.Utils;
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
	public Collection<DocumentTemplate> getTemplates(String archetype) throws GeneralException {
		return documentTemplateRepository.findByArchetype(archetype);
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.ETLContext.ValidatedDataRepository#getUploads(java.lang.String, java.lang.String, java.lang.String, java.lang.Integer)
	 */
	@Override
	public Collection<DocumentUploaded> getUploads(String templateName, String templateVersion, String taxPayerId,
			Integer taxPeriodNumber) throws GeneralException {
		
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
	public boolean hasValidation(String templateName, String templateVersion, String fileId) throws IOException {
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
			return Utils.getTotalHits(resp)>0;
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
			final Optional<SortOrder> sortOrder) throws GeneralException {
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
			QueryBuilder query) throws IOException {
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
			if (Utils.getTotalHits(resp)>0) {
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
