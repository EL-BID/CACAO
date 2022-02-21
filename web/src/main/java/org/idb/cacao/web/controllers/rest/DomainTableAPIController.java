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
package org.idb.cacao.web.controllers.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.validation.Valid;

import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.idb.cacao.api.Views;
import org.idb.cacao.api.templates.DomainTable;
import org.idb.cacao.web.controllers.AdvancedSearch;
import org.idb.cacao.web.controllers.services.DomainTableService;
import org.idb.cacao.web.dto.PaginationData;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.errors.UserNotFoundException;
import org.idb.cacao.web.repositories.DomainTableRepository;
import org.idb.cacao.web.utils.ControllerUtils;
import org.idb.cacao.web.utils.SearchUtils;
import org.idb.cacao.web.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonView;

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

	/**
	 * Method used for returning domain tables that match a given term. Useful for 'auto complete' fields (such as the domain table field
	 * in 'template' object). Limited to 10 results per request, unless the 'limit' parameter is provided as well.
	 */
	@JsonView(Views.Selection.class)
	@GetMapping("/domaintable-search")
	@ApiOperation(value="Method used for returning domain table that match a given term. Useful for 'auto complete' fields")
	public ResponseEntity<List<String>> searchDomainTables(
			@ApiParam(required=false) 
			@RequestParam("term") Optional<String> term,
			@ApiParam(required=false,defaultValue="10") 
			@RequestParam("limit") Optional<Integer> limit) {
		List<String> result;
		try {
			TermQueryBuilder filter = new TermQueryBuilder("active", true);
			result = SearchUtils.doSearchTopDistinctWithFilter(
					elasticsearchClient, 
					DomainTable.class, 
					"name.keyword",
					"name",
					term.orElse(""),
					filter);
		} catch(IOException e) {
			result = new ArrayList<>();
		}
		return ResponseEntity.ok().body(result);
	}
	
	/**
	 * Method used for returning domain tables versions for a domain table name. 
	 */
	@GetMapping("/domaintable/versions")
	@ApiOperation(value="Method used for returning domain table that match a given term. Useful for 'auto complete' fields")
	public ResponseEntity<Set<String>> getDomainTableVersons(
			@ApiParam(required=true) 
			@RequestParam("name") String name) {
		Set<String> result=domainTableService.getDomainTablesVersions(name);
		return ResponseEntity.ok().body(result);
	}

	@GetMapping("/domaintables")
	@ApiOperation(value="Method used for listing domain tables using pagination")
	public PaginationData<DomainTable> getDomainTableWithPagination(Model model, 
			@ApiParam(name = "Number of page to retrieve", allowEmptyValue = true, allowMultiple = false, required = false, type = "Integer")
			@RequestParam("page") Optional<Integer> page, 
			@ApiParam(name = "Page size", allowEmptyValue = true, allowMultiple = false, required = false, type = "Integer")
			@RequestParam("size") Optional<Integer> size,
			@ApiParam(name = "Fields and values to filer data", allowEmptyValue = true, allowMultiple = false, required = false, type = "String")
			@RequestParam("filter") Optional<String> filter, 
			@ApiParam(name = "Field name to sort data", allowEmptyValue = true, allowMultiple = false, required = false, type = "String")
			@RequestParam("sortby") Optional<String> sortBy,
			@ApiParam(name = "Order to sort. Can be asc or desc", allowEmptyValue = true, allowMultiple = false, required = false, type = "String")
			@RequestParam("sortorder") Optional<String> sortOrder) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();

		Optional<AdvancedSearch> filters = SearchUtils.fromTabulatorJSON(filter);
		Page<DomainTable> docs;
		Optional<String> sortField = Optional.of(sortBy.orElse("name"));
		Optional<SortOrder> direction = Optional.of(sortOrder.orElse("asc").equals("asc") ? SortOrder.ASC : SortOrder.DESC);
		try {
			docs = SearchUtils.doSearch(filters.orElse(new AdvancedSearch()), DomainTable.class, elasticsearchClient, page, size, 
					sortField, direction);

		}
		catch (Exception ex) {
			log.log(Level.SEVERE, "Error while searching for all documents", ex);
			docs = Page.empty();
		}		
		PaginationData<DomainTable> result = new PaginationData<>(docs.getTotalPages(), docs.getContent());
		return result;
	}

	@Secured({"ROLE_TAX_DOMAIN_TABLE_WRITE"})
    @DeleteMapping(value="/domaintable/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Deletes an existing domain table",response=DomainTable.class)
    public ResponseEntity<Object> deleteDomainTable(@PathVariable("id") String id) {
        DomainTable table = domainTableRepository.findById(id).orElse(null);
        if (table==null)
        	return ResponseEntity.notFound().build();
        
        log.log(Level.INFO, "Deleting domain table #"+id+" "+ table.getId() +" " + table.getName());
        
        // Removes Domain Table object itself

        domainTableRepository.delete(table);
        return ResponseEntity.ok().body(table);
    }

	@Secured({"ROLE_TAX_DOMAIN_TABLE_WRITE"})
	@PutMapping(value="/domaintable/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Updates an existing domain table",response=DomainTable.class)
    public ResponseEntity<Object> updateDomainTable(@PathVariable("id") String id, @Valid @RequestBody DomainTable table, BindingResult result) {
        if (result.hasErrors()) {
        	return ControllerUtils.returnErrors(result, messageSource);
        }
        
//        Optional<DomainTable> table_in_database = domainTableRepository.findById(id);
        
        log.log(Level.INFO, "Changing domain table #"+id+" "+table.getId() +" "+ table.getName());

        table.setId(id);
        domainTableRepository.saveWithTimestamp(table);
        
        return ResponseEntity.ok().body(table);
    }
	
	@Secured({"ROLE_TAX_DOMAIN_TABLE_WRITE"})
    @GetMapping(value="/domaintable/{id}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Activate an existing domain table", response=DomainTable.class)
    public ResponseEntity<Object> activateDomainTable(@PathVariable("id") String id) {
        
		Optional<DomainTable> existing = domainTableRepository.findById(id);
		if (!existing.isPresent())
        	return ResponseEntity.notFound().build();
		DomainTable table = existing.get();
		table.setActive(true);
        domainTableRepository.saveWithTimestamp(table);
        return ResponseEntity.ok().body(table);
    }

	@Secured({"ROLE_TAX_DOMAIN_TABLE_WRITE"})
    @GetMapping(value="/domaintable/{id}/deactivate", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Deactivate an existing domain table", response=DomainTable.class)
    public ResponseEntity<Object> deactivateDomainTable(@PathVariable("id") String id) {
        
		Optional<DomainTable> existing = domainTableRepository.findById(id);
		if (!existing.isPresent())
        	return ResponseEntity.notFound().build();
		DomainTable table = existing.get();
		table.setActive(false);
		domainTableRepository.saveWithTimestamp(table);
        return ResponseEntity.ok().body(table);
    }

	@Secured({"ROLE_TAX_DOMAIN_TABLE_WRITE"})
    @PostMapping(value="/domaintable", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Adds a new domain table",response=DomainTable.class)
    public ResponseEntity<Object> addDomainTable(@Valid @RequestBody DomainTable table, BindingResult result) {
        if (result.hasErrors()) {
        	return ControllerUtils.returnErrors(result, messageSource);
        }
        
        log.log(Level.INFO, "Adding domain table "+ table.getName());

        domainTableRepository.saveWithTimestamp(table);
        
        return ResponseEntity.ok().body(table);
    }

}
