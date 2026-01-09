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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
	
	@Value("${data.exchange.filter.indices.starts}")
	private Set<String> filterIndicesStartsWith;

	@Value("${data.exchange.filter.indices.contains}")
	private Set<String> filterIndicesContains;
	
	/**
	 * Ignore Kibana indices with these names
	 */
	public static final Pattern KIBANA_IGNORE_PATTERNS = Pattern.compile("^\\.kibana(?>-event-log|_task_manager)");

	@Autowired
	private RestHighLevelClient elasticsearchClient;
	
	@Value("${storage.parquet.files.temporary.dir}")
	private String storageParquetFilesTemporaryDirName;

	/**
     * Retrieves a list of available Elasticsearch indices, applying in-memory filtering, sorting, and pagination.
     * <p>
     * Unlike document search, this method fetches all indices metadata first and then processes the list locally.
     * It filters out indices based on defined exclusion rules (prefixes or contained strings),
     * sorts the remaining list, and calculates the specific sublist for the requested page.
     * </p>
     *
     * @param page      An {@link Optional} containing the page number (0-based). Defaults to 0.
     * @param size      An {@link Optional} containing the page size. Defaults to 10.
     * @param sortOrder An {@link Optional} containing the sort direction ("asc" or "desc"). Defaults to "asc".
     * @return A {@link PaginationData} containing the filtered and paginated list of index names.
     * @throws UserNotFoundException If the security context does not contain a valid authenticated user.
     */
	@Secured({ "ROLE_DATA_EXCHANGE" }) // Secure to special privilege
	@GetMapping(value = "/data_exchange/indices", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Retrieves a list of available Elasticsearch indices, applying in-memory filtering, sorting, and pagination.")	
    public PaginationData<String> getIndices(
            @RequestParam() Optional<Integer> page,
            @ApiParam(name = "Page size", allowEmptyValue = true, allowMultiple = false, required = false, type = "Integer")
            @RequestParam() Optional<Integer> size,
            @ApiParam(name = "Order to sort. Can be asc or desc", allowEmptyValue = true, allowMultiple = false, required = false, type = "String")
            @RequestParam("sortorder") Optional<String> sortOrder) {

        // 1. Security Validation: Ensure user is authenticated
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null)
            throw new UserNotFoundException();

        User user = UserUtils.getUser(auth);
        if (user == null)
            throw new UserNotFoundException();

        List<String> allIndices = new ArrayList<>();

        try {
            // 2. Fetch Metadata: Retrieve all available indices from the cluster
            // Using a wildcard (*) to get everything, as ES GetIndex API doesn't support server-side pagination efficiently
            String filterPattern = "*";
            org.elasticsearch.client.indices.GetIndexRequest request = new org.elasticsearch.client.indices.GetIndexRequest(filterPattern);
            org.elasticsearch.client.indices.GetIndexResponse response = elasticsearchClient.indices().get(request, org.elasticsearch.client.RequestOptions.DEFAULT);

            allIndices.addAll(Arrays.asList(response.getIndices()));

        } catch (Exception e) {
            // 3. Error Handling: Log the error and return an empty list gracefully
            log.log(Level.SEVERE, "Error while retrieving indices list", e);
            return new PaginationData<>(0, Collections.emptyList());
        }

        // 4. In-Memory Filtering: Apply exclusion rules using Java Streams
        allIndices = allIndices.stream()
                // Remove nulls first to avoid NullPointerException in subsequent checks.
                .filter(Objects::nonNull)

                // Filter by Prefix: Keep only if NO prohibited prefix matches
                // Note: Using toLowerCase() for case-insensitive comparison
                .filter(item -> filterIndicesStartsWith.stream()
                        .noneMatch(prefix -> item.toLowerCase().startsWith(prefix.toLowerCase())))

                // Filter by Content: Keep only if NO prohibited content is found inside the string
                .filter(item -> filterIndicesContains.stream()
                        .noneMatch(contents -> item.toLowerCase().contains(contents.toLowerCase())))

                .collect(Collectors.toList());

        // 5. In-Memory Sorting: Sort the filtered list based on the requested direction
        String sortDir = sortOrder.orElse("asc");
        Collections.sort(allIndices, (a, b) -> sortDir.equalsIgnoreCase("asc") ? a.compareTo(b) : b.compareTo(a));

        // 6. In-Memory Pagination: Calculate indices for sublist
        int currentPage = page.orElse(0);
        int currentSize = size.orElse(10);
        
        // Calculate start and end indexes, ensuring they don't exceed list bounds
        int start = Math.min(currentPage * currentSize, allIndices.size());
        int end = Math.min((currentPage + 1) * currentSize, allIndices.size());

        List<String> pagedIndices = allIndices.subList(start, end);
        
        // Calculate total pages based on the filtered list size
        int totalPages = (int) Math.ceil((double) allIndices.size() / currentSize);

        return new PaginationData<>(totalPages, pagedIndices);
    }

	/**
     * Retrieves a paginated list of documents from a specified Elasticsearch index.
     * <p>
     * This method validates the current user's authentication and ensures the index name is provided.
     * It applies default sorting by "changedTime" in ascending order if no sort parameters are specified.
     * In case of a search execution error, the exception is logged, and an empty result set is returned.
     * </p>
     *
     * @param indexName The name of the Elasticsearch index to query. Must not be null or empty.
     * @param page      An {@link Optional} containing the page number to retrieve.
     * @param size      An {@link Optional} containing the number of items per page.
     * @param sortBy    An {@link Optional} containing the field name to sort by. Defaults to "changedTime".
     * @param sortOrder An {@link Optional} containing the sort direction ("asc" or "desc"). Defaults to "asc".
     * @return A {@link PaginationData} object containing the map of results and total page count.
     * @throws MissingParameter      If the {@code indexName} is null or contains only whitespace.
     * @throws UserNotFoundException If the security context does not contain a valid authenticated user.
     */
	@Secured({ "ROLE_DATA_EXCHANGE" })
	@GetMapping(value = "/data_exchange/indices/{indexName}/data", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Retrieves a paginated list of documents from a specified Elasticsearch index.")	
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

        // 1. Validate mandatory input parameters
        if (indexName == null || indexName.trim().length() == 0) {
            throw new MissingParameter("indexName");
        }

        // 2. Security Check: Retrieve authentication from SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null)
            throw new UserNotFoundException();
        
        // 3. Resolve the custom User object from the authentication token
        User user = UserUtils.getUser(auth);
        if (user == null)
            throw new UserNotFoundException();

        // 4. Setup Sorting Defaults:
        // Use "changedTime" if 'sortBy' is not provided
        Optional<String> sortField = Optional.of(sortBy.orElse("changedTime")); 
        
        // Use ASC order if 'sortOrder' is not provided or explicitly "asc", otherwise DESC
        Optional<SortOrder> direction = Optional
                .of(sortOrder.orElse("asc").equals("asc") ? SortOrder.ASC : SortOrder.DESC);

        Page<Map<String, Object>> data;

        try {
            // 5. Execute the search query against Elasticsearch
            data = SearchUtils.doSearch(                    
                    indexName, 
                    elasticsearchClient,                    
                    page, 
                    size, 
                    sortField,
                    direction);     

        } catch (Exception e) {
            // 6. Error Handling: Log the specific error for debugging
            log.log(Level.SEVERE, String.format("Error while searching data for index %s", indexName), e);
            
            // Return an empty page instead of throwing exception to avoid breaking the frontend
            data = Page.empty();
        }

        // 7. Wrap the result in the standard response DTO
        return new PaginationData<>(data.getTotalPages(), data.getContent());
    }
		
}
