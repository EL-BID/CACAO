/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.sort.SortOrder;
import org.idb.cacao.web.dto.PaginationData;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.errors.MissingParameter;
import org.idb.cacao.web.errors.UserNotFoundException;
import org.idb.cacao.web.utils.SearchUtils;
import org.idb.cacao.web.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller class for all RESTful endpoints related to 'data exchange' with external applications.
 * 
 * @author Rivelino Patrício
 * 
 * @since 01/06/2026
 *
 */
@RestController
@RequestMapping("/api")
@Tag(name="sync-api-controller", description="Controller class for all RESTful endpoints related to 'data exchange' with external applications.")
public class DataExchangeAPIController {

	private static final Logger log = Logger.getLogger(DataExchangeAPIController.class.getName());

	public static final int MAX_RESULTS_PER_REQUEST = 10_000;
	
	/**
	 * Ignore Kibana indices with these names
	 */
	public static final Pattern KIBANA_IGNORE_PATTERNS = Pattern.compile("^\\.kibana(?>-event-log|_task_manager)");

	@Autowired
	private RestHighLevelClient elasticsearchClient;
	
	@Value("${storage.parquet.files.temporary.dir}")
	private String storageParquetFilesTemporaryDirName;
	
	/**
	 * Downloads a list of all indices on server.<BR>
	 */
	@Secured({ "ROLE_ADMIN_OPS" }) // Ajuste as roles conforme necessário
	@GetMapping(value = "/data_exchange/indices", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "List all existing indices in the Elasticsearch database.")
	public PaginationData<String> getIndices(
			@RequestParam() Optional<Integer> page, 
			@ApiParam(name = "Page size", allowEmptyValue = true, allowMultiple = false, required = false, type = "Integer")
			@RequestParam() Optional<Integer> size,
			@ApiParam(name = "Order to sort. Can be asc or desc", allowEmptyValue = true, allowMultiple = false, required = false, type = "String")
			@RequestParam("sortorder") Optional<String> sortOrder) {

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null)
			throw new UserNotFoundException();
		
        // User validation
		User user = UserUtils.getUser(auth);
		if (user == null)
			throw new UserNotFoundException();		
		
		List<String> allIndices = new ArrayList<>();

		try {
            // Using the HighLevel client to retrieve metadata (generic example)
            // The filter here is applied as a wildcard (*) if there is no specific filter.
            String filterPattern = "*";
            org.elasticsearch.client.indices.GetIndexRequest request = new org.elasticsearch.client.indices.GetIndexRequest(filterPattern);
            org.elasticsearch.client.indices.GetIndexResponse response = elasticsearchClient.indices().get(request, org.elasticsearch.client.RequestOptions.DEFAULT);
            
            allIndices.addAll(Arrays.asList(response.getIndices()));

		} catch (Exception e) {
			log.log(Level.SEVERE, "Error while retrieving indices list", e);
            // Returns empty
			return new PaginationData<>(0, Collections.emptyList());
		}
		
		allIndices = allIndices.stream().filter(name->name!=null&&name.startsWith("cacao")&&!name.contains("pwd")&&!name.contains("sync")&&!name.contains("user")).collect(Collectors.toList());

		// In-Memory Sorting Logic (since it's metadata)
        String sortDir = sortOrder.orElse("asc");
        Collections.sort(allIndices, (a, b) -> sortDir.equalsIgnoreCase("asc") ? a.compareTo(b) : b.compareTo(a));

		// In-Memory Paging Logic
		int currentPage = page.orElse(0);
		int currentSize = size.orElse(10);
		int start = Math.min(currentPage * currentSize, allIndices.size());
		int end = Math.min((currentPage + 1) * currentSize, allIndices.size());

        List<String> pagedIndices = allIndices.subList(start, end);
        int totalPages = (int) Math.ceil((double) allIndices.size() / currentSize);

		return new PaginationData<>(totalPages, pagedIndices);
	}	


	@Secured({ "ROLE_ADMIN_OPS" })
	@GetMapping(value = "/data_exchange/indices/{indexName}/data", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Return data via API for a given specific index name.")
	public PaginationData<Map<String, Object>> getIndexData(
			@ApiParam(name = "Name of the elasticsearch index", allowEmptyValue = false, allowMultiple = false, required = true, type = "String")
			@PathVariable() String indexName, 
			@RequestParam() Optional<Integer> page, 
			@ApiParam(name = "Page size", allowEmptyValue = true, allowMultiple = false, required = false, type = "Integer")
			@RequestParam() Optional<Integer> size, 
			@ApiParam(name = "Field name to sort data", allowEmptyValue = true, allowMultiple = false, required = false, type = "String")
			@RequestParam("sortby") Optional<String> sortBy,
			@ApiParam(name = "Order to sort. Can be asc or desc", allowEmptyValue = true, allowMultiple = false, required = false, type = "String")
			@RequestParam("sortorder") Optional<String> sortOrder) {

		if (indexName == null || indexName.trim().length() == 0) {
			throw new MissingParameter("indexName");
		}

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null)
			throw new UserNotFoundException();
		User user = UserUtils.getUser(auth);
		if (user == null)
			throw new UserNotFoundException();

		Optional<String> sortField = Optional.of(sortBy.orElse("timestamp")); 
		Optional<SortOrder> direction = Optional
				.of(sortOrder.orElse("asc").equals("asc") ? SortOrder.ASC : SortOrder.DESC);

		Page<Map<String, Object>> data;

		try {
			data = SearchUtils.doSearch(					
                    indexName, // Index name
                    elasticsearchClient,					
                    page, 
                    size, 
                    sortField,
					direction);		

		} catch (Exception e) {
			log.log(Level.SEVERE, String.format("Error while searching data for index %s", indexName), e);
			data = Page.empty();
		}

		return new PaginationData<>(data.getTotalPages(), data.getContent());
	}
	
	
}
