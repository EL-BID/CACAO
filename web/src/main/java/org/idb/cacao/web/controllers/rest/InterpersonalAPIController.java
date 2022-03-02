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

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.validation.Valid;

import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.sort.SortOrder;
import org.idb.cacao.api.Views;
import org.idb.cacao.api.utils.DateTimeUtils;
import org.idb.cacao.web.GenericCounts;
import org.idb.cacao.web.controllers.AdvancedSearch;
import org.idb.cacao.web.dto.PaginationData;
import org.idb.cacao.web.entities.Interpersonal;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.errors.UserNotFoundException;
import org.idb.cacao.web.repositories.InterpersonalRepository;
import org.idb.cacao.web.utils.ControllerUtils;
import org.idb.cacao.web.utils.SearchUtils;
import org.idb.cacao.web.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonView;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller class for all endpoints related to 'interpersonal relationship' object interacting by a REST interface
 * 
 * @author Gustavo Figueiredo
 *
 */
@RestController
@RequestMapping("/api")
@Tag(name="interpersonal-api-controller", description="Controller class for all endpoints related to 'interpersonal relationship' object interacting by a REST interface")
public class InterpersonalAPIController {

	private static final Logger log = Logger.getLogger(InterpersonalAPIController.class.getName());
	
	/**
	 * Default paralelism for bulk load operations
	 */
	public static final int DEFAULT_BULK_LOAD_PARALELISM = 4;

	@Autowired
	private MessageSource messageSource;

	@Autowired
	private InterpersonalRepository interpersonalRepository;
	
	@Autowired
	private RestHighLevelClient elasticsearchClient;
	
	@Secured({"ROLE_INTERPERSONAL_READ_ALL"})
	@JsonView(Views.Authority.class)
	@GetMapping(value="/interpersonals", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Method used for listing interpersonal relationships using pagination")
	public PaginationData<Interpersonal> getRelationshipsWithPagination(Model model, 
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

		AdvancedSearch filters = SearchUtils.fromTabulatorJSON(filter).orElse(new AdvancedSearch());
		filters.addFilter(new AdvancedSearch.QueryFilterBoolean("active", "true"));
		Page<Interpersonal> docs;
		Optional<String> sortField = Optional.of(sortBy.orElse("personId1"));
		Optional<SortOrder> direction = Optional.of(sortOrder.orElse("asc").equals("asc") ? SortOrder.ASC : SortOrder.DESC);
		try {
			docs = SearchUtils.doSearch(filters, Interpersonal.class, elasticsearchClient, page, size, 
					sortField, direction);

		}
		catch (Exception ex) {
			log.log(Level.SEVERE, "Error while searching for all documents", ex);
			docs = Page.empty();
		}		
		return new PaginationData<>(docs.getTotalPages(), docs.getContent());
	}
	
	@Secured({"ROLE_INTERPERSONAL_WRITE"})
    @PostMapping(value="/interpersonal", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Add a new interpersonal relationship configuration",response=Interpersonal.class)
    public ResponseEntity<Object> addInterpersonal(@Valid @RequestBody Interpersonal interpersonal, BindingResult result) {
        if (result!=null && result.hasErrors()) {
        	return ControllerUtils.returnErrors(result, messageSource);
        }
        
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();

        Optional<Interpersonal> existentRelationship =
            interpersonalRepository.findByActiveIsTrueAndPersonId1AndRelationshipTypeAndPersonId2(interpersonal.getPersonId1(), interpersonal.getRelationshipType().name(), interpersonal.getPersonId2());
        
        if (existentRelationship.isPresent()) {
            return ResponseEntity.badRequest().body(messageSource.getMessage("interpersonal.error.already.exists", null, LocaleContextHolder.getLocale()));
        }
        
        if (log.isLoggable(Level.INFO)) {
        	log.log(Level.INFO, String.format("Creating new interpersonal relationship between % and %s with type %s", interpersonal.getPersonId1(), interpersonal.getPersonId2(), interpersonal.getRelationshipType().name()));
        }

        interpersonal.setId(null);
        interpersonal.setTimestamp(DateTimeUtils.now());
        interpersonal.setUser(user.getLogin());

        try {
        	interpersonalRepository.saveWithTimestamp(interpersonal);
        }
        catch (Exception ex) {
        	log.log(Level.SEVERE,"Create interpersonal relationship failed", ex);
        	return ResponseEntity.badRequest().body(messageSource.getMessage("op.failed", null, LocaleContextHolder.getLocale()));
        }
        return ResponseEntity.ok().body(interpersonal);
    }

	@Secured({"ROLE_INTERPERSONAL_WRITE"})
    @PostMapping(value="/interpersonals", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Add multiple interpersonal relationship configurations",response=GenericCounts.class)
    public ResponseEntity<Object> addInterpersonals(@Valid @RequestBody Interpersonal[] interpersonalRelationships, BindingResult result) {
        if (result!=null && result.hasErrors()) {
        	return ControllerUtils.returnErrors(result, messageSource);
        }
        
        if (interpersonalRelationships==null || interpersonalRelationships.length==0) {
        	return ResponseEntity.badRequest().build();
        }
        
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();

    	if (log.isLoggable(Level.INFO)) {
    		log.log(Level.INFO, String.format("Creating/updating list of %d interpersonal relationships", interpersonalRelationships.length));
    	}

        GenericCounts counts = addOrCreateInterpersonals(interpersonalRelationships, user);

        if (!counts.hasChanges()) {
        	return ResponseEntity.badRequest().build();
        }
        else {
        	return ResponseEntity.ok().body(counts);
        }
    }
	
	private GenericCounts addOrCreateInterpersonals(Interpersonal[] interpersonalRelationships, User user) {
		
        LongAdder countCreated = new LongAdder();
        LongAdder countUpdated = new LongAdder();
        LongAdder countErrors = new LongAdder();
        
        ExecutorService executor = Executors.newFixedThreadPool(DEFAULT_BULK_LOAD_PARALELISM);
        
        for (Interpersonal interpersonal:interpersonalRelationships) {
        	if (interpersonal==null) {
        		countErrors.increment();
        		continue;
        	}
        	executor.submit(()->{
        		
                Optional<Interpersonal> existent =
                    interpersonalRepository.findByActiveIsTrueAndPersonId1AndRelationshipTypeAndPersonId2(interpersonal.getPersonId1(), interpersonal.getRelationshipType().name(), interpersonal.getPersonId2()); 

	            if (!existent.isPresent()) {
	            	if (log.isLoggable(Level.INFO)) {
	            		log.log(Level.INFO, String.format("Creating new interpersonal relationship between % and % with type %s", interpersonal.getPersonId1(), interpersonal.getPersonId2(), interpersonal.getRelationshipType().name()));
	            	}
	
	                interpersonal.setId(null);
	                interpersonal.setTimestamp(DateTimeUtils.now());
	                interpersonal.setUser(user.getLogin());

	                try {
	                	interpersonalRepository.saveWithTimestamp(interpersonal);
	                }
	                catch (Exception ex) {
	                	log.log(Level.SEVERE,"Create interpersonal relationship failed", ex);
	                	countErrors.increment();
	                }
	                countCreated.increment();
	            }
        	});
        }
        
        executor.shutdown();
        try {
			boolean ok = executor.awaitTermination(4, TimeUnit.HOURS);
			if (!ok)
				log.log(Level.WARNING,"Too much time waiting for bulk load termination");
		} catch (InterruptedException e) {
        	log.log(Level.WARNING,"Interrupted bulk load", e);
        	Thread.currentThread().interrupt();
		}
		
        return new GenericCounts().withCreated(countCreated.longValue()).withUpdated(countUpdated.longValue()).withErrors(countErrors.longValue());
	}

	@Secured({"ROLE_INTERPERSONAL_WRITE"})
    @DeleteMapping(value="/interpersonal/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Deactivate an existing interpersonal relationship configuration",response=Interpersonal.class)
    public ResponseEntity<Object> deactivateInterpersonal(@PathVariable("id") String id) {
		Interpersonal interpersonal = interpersonalRepository.findById(id).orElse(null);
        if (interpersonal==null)
        	return ResponseEntity.notFound().build();
        
        if (log.isLoggable(Level.INFO)) {
        	log.log(Level.INFO, String.format("Deactivate interpersonal relationship between %s and %s with type %s", interpersonal.getPersonId1(), interpersonal.getPersonId2(), interpersonal.getRelationshipType().name()));
        }

        interpersonal.setActive(false);
        interpersonal.setRemovedTimestamp(DateTimeUtils.now());
        interpersonalRepository.saveWithTimestamp(interpersonal);
        
        return ResponseEntity.ok().body(interpersonal);
    }

}
