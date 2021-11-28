package org.idb.cacao.web.controllers.rest;

import static org.idb.cacao.web.utils.ControllerUtils.searchPage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.validation.Valid;

import org.elasticsearch.client.RestHighLevelClient;
import org.idb.cacao.api.Views;
import org.idb.cacao.api.templates.DomainTable;
import org.idb.cacao.web.controllers.AdvancedSearch;
import org.idb.cacao.web.controllers.dto.PaginationData;
import org.idb.cacao.web.controllers.services.DomainTableService;
import org.idb.cacao.web.repositories.DomainTableRepository;
import org.idb.cacao.web.utils.ControllerUtils;
import org.idb.cacao.web.utils.SearchUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonView;

import org.springframework.core.env.Environment;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name="domain-table-api-controller", description="Controller class for all endpoints related to 'domain tables' object interacting by a REST interface")
public class DomainTableAPIController {
	private static final Logger log = Logger.getLogger(DomainTableAPIController.class.getName());
	
	/*
	 * Maximum items returned on a search
	 */
	public static int LIMIT_RESULT = 10;
	
	@Autowired
	private MessageSource messageSource;

	@Autowired
	private DomainTableRepository domainTableRepository;
	
	@Autowired
	private DomainTableService domainTableService;
	
	@Autowired
	private RestHighLevelClient elasticsearchClient;
	
	@Autowired
	private Environment env;

	/**
	 * Method used for returning domain tables that match a given term. Useful for 'auto complete' fields (such as the domain table field
	 * in 'template' object). Limited to 10 results per request, unless the 'limit' parameter is provided as well.
	 */
	@JsonView(Views.Selection.class)
	@GetMapping("/domaintable_search")
	@ApiOperation(value="Method used for returning domain table that match a given term. Useful for 'auto complete' fields")
	public ResponseEntity<List<String>> searchDomainTables(
			@ApiParam(required=false) @RequestParam("term") Optional<String> term,
			@ApiParam(required=false,defaultValue="10") @RequestParam("limit") Optional<Integer> limit) {
		List<String> result;
		try {
			result = SearchUtils.doSearchTopDistinctWithFilter(
					elasticsearchClient, 
					DomainTable.class, 
					"name.keyword",
					"name",
					term.orElse(null));
		} catch(IOException e) {
			result = new ArrayList<>();
		}
		return ResponseEntity.ok().body(result);
	}
	

	@GetMapping("/domaintables")
	@ApiOperation(value="Method used for listing users using pagination")
	public PaginationData<DomainTable> getDomainTables(
			Model model, @RequestParam("page") Optional<Integer> page,
			@RequestParam("size") Optional<Integer> size, @RequestParam("q") Optional<String> filters_as_json) {
		int currentPage = page.orElse(1);
		int pageSize = ControllerUtils.getPageSizeForUser(size, env);
		Optional<AdvancedSearch> filters = SearchUtils.fromJSON(filters_as_json);
		Page<DomainTable> tables;
		if (filters.isPresent() && !filters.get().isEmpty()) {
			tables = domainTableService.searchDomainTables(filters, page, size);
		} else {
			tables = searchPage(() -> domainTableRepository
					.findAll(PageRequest.of(currentPage - 1, pageSize, Sort.by("name").ascending())));
		}
		PaginationData<DomainTable> result = new PaginationData<>(tables.getTotalPages(), tables.getContent());
		return result;

	}
	
//	@Secured({"ROLE_SYSADMIN","ROLE_SUPPORT"})
    @DeleteMapping(value="/domaintable/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Deletes an existing user",response=DomainTable.class)
    public ResponseEntity<Object> deleteUser(@PathVariable("id") String id) {
        DomainTable table = domainTableRepository.findById(id).orElse(null);
        if (table==null)
        	return ResponseEntity.notFound().build();
        
        log.log(Level.INFO, "Deleting domain table #"+id+" "+ table.getId() +" " + table.getName());
        
        // Removes User object itself

        domainTableRepository.delete(table);
        return ResponseEntity.ok().body(table);
    }

    @PutMapping(value="/domaintable/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Updates an existing user",response=DomainTable.class)
    public ResponseEntity<Object> updateDomainTable(@PathVariable("id") String id, @Valid @RequestBody DomainTable table, BindingResult result) {
        if (result.hasErrors()) {
        	return ControllerUtils.returnErrors(result, messageSource);
        }
        
        Optional<DomainTable> table_in_database = domainTableRepository.findById(id);
        
        log.log(Level.INFO, "Changing domain table #"+id+" "+table.getId() +" "+ table.getName());

        table.setId(id);
        domainTableRepository.saveWithTimestamp(table);
        
        return ResponseEntity.ok().body(table);
    }
}
