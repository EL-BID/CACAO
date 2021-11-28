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

import static org.idb.cacao.web.utils.ControllerUtils.searchPage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.validation.Valid;

import org.idb.cacao.api.utils.DateTimeUtils;
import org.idb.cacao.web.GenericCounts;
import org.idb.cacao.web.entities.Interpersonal;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.errors.UserNotFoundException;
import org.idb.cacao.web.repositories.InterpersonalRepository;
import org.idb.cacao.web.utils.ControllerUtils;
import org.idb.cacao.web.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiOperation;
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
	public static int DEFAULT_BULK_LOAD_PARALELISM = 4;

	@Autowired
	private MessageSource messageSource;

	@Autowired
	private InterpersonalRepository interpersonalRepository;
	
	@Secured({"ROLE_SYSADMIN","ROLE_SUPPORT","ROLE_MASTER","ROLE_AUTHORITY"})
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

        Page<Interpersonal> existentRelationships =
        searchPage(()->interpersonalRepository.findByPersonId1AndRelationshipTypeAndPersonId2(interpersonal.getPersonId1(), interpersonal.getRelationshipType().name(), interpersonal.getPersonId2(), 
        		PageRequest.of(0, 10, Sort.by("timestamp").descending())));
        
        Interpersonal existentActiveRelationship = existentRelationships.stream().filter(rel->!rel.isRemoved()).findAny().orElse(null);
        
        if (existentActiveRelationship!=null) {
            return ResponseEntity.badRequest().body(messageSource.getMessage("rel.error.already.exists", null, LocaleContextHolder.getLocale()));
        }
        
        log.log(Level.INFO, "Creating new interpersonal relationship between "+interpersonal.getPersonId1()+" and "+interpersonal.getPersonId2()+" with type "+interpersonal.getRelationshipType().name());

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

	@Secured({"ROLE_SYSADMIN","ROLE_SUPPORT","ROLE_MASTER","ROLE_AUTHORITY"})
    @PostMapping(value="/interpersonals", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Add multiple interpersonal relationship configurations",response=GenericCounts.class)
    public ResponseEntity<Object> addInterpersonals(@Valid @RequestBody Interpersonal[] interpersonal_relationships, BindingResult result) {
        if (result!=null && result.hasErrors()) {
        	return ControllerUtils.returnErrors(result, messageSource);
        }
        
        if (interpersonal_relationships==null || interpersonal_relationships.length==0) {
        	return ResponseEntity.badRequest().build();
        }
        
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();

        log.log(Level.INFO, "Creating/updating list of "+interpersonal_relationships.length+" interpersonal relationships");


        GenericCounts counts = addOrCreateInterpersonals(interpersonal_relationships, user);

        if (!counts.hasChanges()) {
        	return ResponseEntity.badRequest().build();
        }
        else {
        	return ResponseEntity.ok().body(counts);
        }
    }
	
	private GenericCounts addOrCreateInterpersonals(Interpersonal[] interpersonal_relationships, User user) {
		
        LongAdder count_created = new LongAdder();
        LongAdder count_updated = new LongAdder();
        LongAdder count_errors = new LongAdder();
        
        ExecutorService executor = Executors.newFixedThreadPool(DEFAULT_BULK_LOAD_PARALELISM);
        
        for (Interpersonal interpersonal:interpersonal_relationships) {
        	if (interpersonal==null) {
        		count_errors.increment();
        		continue;
        	}
        	executor.submit(()->{
        		
                Page<Interpersonal> existentRelationships =
                        searchPage(()->interpersonalRepository.findByPersonId1AndRelationshipTypeAndPersonId2(interpersonal.getPersonId1(), interpersonal.getRelationshipType().name(), interpersonal.getPersonId2(), 
                        		PageRequest.of(0, 10, Sort.by("timestamp").descending())));
                        
                Interpersonal existentActiveRelationship = existentRelationships.stream().filter(rel->!rel.isRemoved()).findAny().orElse(null);

	            if (existentActiveRelationship!=null) {
	            	// If this relationship already exists, there is nothing to update
	            }
	            else {
	            	log.log(Level.INFO, "Creating new interpersonal relationship between "+interpersonal.getPersonId1()+" and "+interpersonal.getPersonId2()+" with type "+interpersonal.getRelationshipType().name());
	
	                interpersonal.setId(null);
	                interpersonal.setTimestamp(DateTimeUtils.now());
	                interpersonal.setUser(user.getLogin());

	                try {
	                	interpersonalRepository.saveWithTimestamp(interpersonal);
	                }
	                catch (Exception ex) {
	                	log.log(Level.SEVERE,"Create interpersonal relationship failed", ex);
	                	count_errors.increment();
	                }
	                count_created.increment();
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
		}
		
        return new GenericCounts().withCreated(count_created.longValue()).withUpdated(count_updated.longValue()).withErrors(count_errors.longValue());
	}

	@Secured({"ROLE_SYSADMIN","ROLE_SUPPORT","ROLE_MASTER","ROLE_AUTHORITY"})
    @DeleteMapping(value="/interpersonal/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Deletes an existing interpersonal relationship configuration (actually it will only disable this configuration)",response=Interpersonal.class)
    public ResponseEntity<Object> deleteInterpersonal(@PathVariable("id") String id) {
		Interpersonal interpersonal = interpersonalRepository.findById(id).orElse(null);
        if (interpersonal==null)
        	return ResponseEntity.notFound().build();
        
        log.log(Level.INFO, "Deleting interpersonal relationship between "+interpersonal.getPersonId1()+" and "+interpersonal.getPersonId2()+" with type "+interpersonal.getRelationshipType().name());

        interpersonal.setRemoved(true);
        interpersonal.setRemovedTimestamp(DateTimeUtils.now());
        interpersonalRepository.saveWithTimestamp(interpersonal);
        
        return ResponseEntity.ok().body(interpersonal);
    }

}
