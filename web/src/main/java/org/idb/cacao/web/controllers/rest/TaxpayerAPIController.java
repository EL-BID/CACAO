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
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.validation.Valid;

import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.sort.SortOrder;
import org.idb.cacao.api.Taxpayer;
import org.idb.cacao.api.Views;
import org.idb.cacao.web.controllers.AdvancedSearch;
import org.idb.cacao.web.dto.NameId;
import org.idb.cacao.web.dto.PaginationData;
import org.idb.cacao.web.dto.SearchResult;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.errors.UserNotFoundException;
import org.idb.cacao.web.repositories.TaxpayerRepository;
import org.idb.cacao.web.utils.ControllerUtils;
import org.idb.cacao.web.utils.SearchUtils;
import org.idb.cacao.web.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
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

/**
 * Controller class for all endpoints related to 'TaxPayer' object interacting by a REST interface
 * 
 * @author Luis Kauer
 *
 */
@RestController
@RequestMapping("/api")
@Tag(name="taxpayer-api-controller", description="Controller class for all endpoints related to 'TaxPayer' object interacting by a REST interface.")
public class TaxpayerAPIController {

	private static final Logger log = Logger.getLogger(TaxpayerAPIController.class.getName());

	@Autowired
	private MessageSource messageSource;

	@Autowired
	private TaxpayerRepository taxpayerRepository;
	
	@Autowired
	private RestHighLevelClient elasticsearchClient;
	
	@Secured({"ROLE_TAXPAYER_READ"})
	@JsonView(Views.Declarant.class)
	@GetMapping(value="/taxpayers", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Method used for listing taxpayers using pagination")
	public PaginationData<Taxpayer> getUsersWithPagination(Model model, 
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
		Page<Taxpayer> docs;
		Optional<String> sortField = Optional.of(sortBy.orElse("taxPayerId"));
		Optional<SortOrder> direction = Optional.of(sortOrder.orElse("asc").equals("asc") ? SortOrder.ASC : SortOrder.DESC);
		try {
			docs = SearchUtils.doSearch(filters.orElse(new AdvancedSearch()), Taxpayer.class, elasticsearchClient, page, size, 
					sortField, direction);

		}
		catch (Exception ex) {
			log.log(Level.SEVERE, "Error while searching for all documents", ex);
			docs = Page.empty();
		}		
		PaginationData<Taxpayer> result = new PaginationData<>(docs.getTotalPages(), docs.getContent());
		return result;
	}
	
	/**
	 * Method used for returning names of users that match a given term. Useful for 'auto complete' fields
	 */
	@Secured({"ROLE_TAXPAYER_READ"})
	@GetMapping("/taxpayers/autocomplete")
	@ApiOperation(value="Method used for returning taxpayer id and name that match a given term with their id or name. Useful for 'auto complete' fields")
	public SearchResult<NameId> autocompleteTaxpayer(@ApiParam(required=false) @RequestParam("term") Optional<String> term) {
		List<NameId> result;
		try {
			result = SearchUtils.doSearchTopWithFilter(elasticsearchClient, Taxpayer.class, "taxPayerId", "name", term.orElse(""), 10)
			    .stream()
			    .map(t -> new NameId(t.get("name").toString(), t.get("taxPayerId").toString()))
			    .collect(Collectors.toList());
			return new SearchResult<NameId>(result);
		} catch (IOException ex) {
			log.log(Level.SEVERE,"Search taxpayer failed", ex);
		}
		return null;
	}

	
	/**
	 * Method used for returning names of users that match a given term. Useful for 'auto complete' fields
	 */
	@Secured({"ROLE_TAXPAYER_READ"})
	@GetMapping("/taxpayer/names")
	@ApiOperation(value="Method used for returning names of taxpayers that match a given term. Useful for 'auto complete' fields")
	public ResponseEntity<List<String>> getTaxpayerNames(@ApiParam(required=false) @RequestParam("term") Optional<String> term) {
		List<String> names;
		if (term!=null && term.isPresent()) {
			Pattern pattern = Pattern.compile(Pattern.quote(term.get()), Pattern.CASE_INSENSITIVE);
			names =
			StreamSupport.stream(taxpayerRepository.findAll().spliterator(),false)
			.filter(taxpayer->pattern.matcher(taxpayer.getName()).find())
			.map(Taxpayer::getName)
			.collect(Collectors.toList());
		}
		else {
			names =
			StreamSupport.stream(taxpayerRepository.findAll().spliterator(),false)
			.map(Taxpayer::getName)
			.collect(Collectors.toList());
		}
		return ResponseEntity.ok().body(names);
	}

	@Secured({"ROLE_TAXPAYER_WRITE"})
    @PostMapping(value="/taxpayer", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Add a new taxpayer",response=Taxpayer.class)
	@CacheEvict(value={"qualifierValues"})
    public ResponseEntity<Object> addTaxpayer(@Valid @RequestBody Taxpayer taxpayer, BindingResult result) {
        if (result.hasErrors()) {
        	return ControllerUtils.returnErrors(result, messageSource);
        }
        
        log.log(Level.INFO, "Creating new taxpayer "+ taxpayer.getName()+" "+ taxpayer.getTaxPayerId());
        
        try {
        	taxpayerRepository.saveWithTimestamp(taxpayer);
        }
        catch (Exception ex) {
        	log.log(Level.SEVERE,"Create taxpayer failed", ex);
        	return ResponseEntity.badRequest().body(messageSource.getMessage("op.failed", null, LocaleContextHolder.getLocale()));
        }
        return ResponseEntity.ok().body(taxpayer);
    }

	@Secured({"ROLE_TAXPAYER_WRITE"})
	@PutMapping(value="/taxpayer/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Updates an existing taxpayer",response=Taxpayer.class)
	@CacheEvict(value={"qualifierValues"})
    public ResponseEntity<Object> updateTaxpayer(@PathVariable("id") String id, @Valid @RequestBody Taxpayer taxpayer, BindingResult result) {
        if (result.hasErrors()) {
        	return ControllerUtils.returnErrors(result, messageSource);
        }
        
        log.log(Level.INFO, "Changing taxpayer #"+id+" "+taxpayer.getName()+" "+taxpayer.getTaxPayerId());

        taxpayer.setId(id);
        taxpayerRepository.saveWithTimestamp(taxpayer);
        
        return ResponseEntity.ok().body(taxpayer);
    }
    
	@Secured({"ROLE_TAXPAYER_WRITE"})
	@DeleteMapping(value="/taxpayer/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Deletes an existing taxpayer",response=Taxpayer.class)
	@CacheEvict(value={"qualifierValues"})
    public ResponseEntity<Object> deleteTaxpayer(@PathVariable("id") String id) {
    	Taxpayer taxpayer = taxpayerRepository.findById(id).orElse(null);
        if (taxpayer==null)
        	return ResponseEntity.notFound().build();
        
        log.log(Level.INFO, "Deleting taxpayer #"+id+" "+taxpayer.getName()+" "+taxpayer.getTaxPayerId());
        
        //taxpayerRepository.delete(taxpayer);
        try {
        	taxpayer.setActive(false);
        	taxpayerRepository.saveWithTimestamp(taxpayer);
        }
        catch (Exception ex) {
        	log.log(Level.SEVERE,"Delete taxpayer failed", ex);
        	return ResponseEntity.badRequest().body(messageSource.getMessage("op.failed", null, LocaleContextHolder.getLocale()));
        }

        return ResponseEntity.ok().body(taxpayer);
    }
	
	@Secured({"ROLE_TAXPAYER_WRITE"})
    @GetMapping(value="/taxpayer/{id}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Activate an existing taxpayer", response=Taxpayer.class)
    public ResponseEntity<Object> activate(
    		@ApiParam(name = "Taxpayer ID", allowEmptyValue = false, allowMultiple = false, example = "1234567890", required = true, type = "String")
    		@PathVariable("id") String id) {
        
		Optional<Taxpayer> existing = taxpayerRepository.findById(id);
		if (!existing.isPresent())
        	return ResponseEntity.notFound().build();
		Taxpayer taxpayer = existing.get();
		taxpayer.setActive(true);
        taxpayerRepository.saveWithTimestamp(taxpayer);
        return ResponseEntity.ok().body(taxpayer);
    }

	@Secured({"ROLE_TAXPAYER_WRITE"})
    @GetMapping(value="/taxpayer/{id}/deactivate", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Deactivate an existing taxpayer", response=Taxpayer.class)
    public ResponseEntity<Object> deactivate(
    		@ApiParam(name = "Taxpayer ID", allowEmptyValue = false, allowMultiple = false, example = "1234567890", required = true, type = "String")
    		@PathVariable("id") String id) {
        
		Optional<Taxpayer> existing= taxpayerRepository.findById(id);
		if (!existing.isPresent())
        	return ResponseEntity.notFound().build();
		Taxpayer taxpayer= existing.get();
		taxpayer.setActive(false);
        taxpayerRepository.saveWithTimestamp(taxpayer);
        return ResponseEntity.ok().body(taxpayer);
    }

}