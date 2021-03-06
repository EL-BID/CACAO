/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.rest;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
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
import org.idb.cacao.web.dto.TaxpayerDto;
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
import com.google.common.collect.Lists;

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

	private static final String ERROR_OP_FAILED = "op.failed";

	private static final String FIELD_TAX_PAYER_ID = "taxPayerId";

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
	public PaginationData<TaxpayerDto> getUsersWithPagination(Model model, 
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
		Page<TaxpayerDto> docs;
		Optional<String> sortField = Optional.of(sortBy.orElse(FIELD_TAX_PAYER_ID));
		Optional<SortOrder> direction = Optional.of(sortOrder.orElse("asc").equals("asc") ? SortOrder.ASC : SortOrder.DESC);
		try {
			docs = SearchUtils.doSearch(filters.orElse(new AdvancedSearch()), Taxpayer.class, elasticsearchClient, page, size, 
					sortField, direction)
				     .map(TaxpayerDto::new);

		}
		catch (Exception ex) {
			log.log(Level.SEVERE, "Error while searching for all documents", ex);
			docs = Page.empty();
		}		
		return new PaginationData<>(docs.getTotalPages(), docs.getContent());
	}
	
	/**
	 * Method used for returning names of users that match a given term. Useful for 'auto complete' fields
	 */
	@Secured({"ROLE_TAXPAYER_READ"})
	@PostMapping("/taxpayers/autocomplete")
	@ApiOperation(value="Method used for returning taxpayer id and name that match a given term with their id or name. Useful for 'auto complete' fields")
	public SearchResult<NameId> autocompleteTaxpayer(@ApiParam(required=false) @RequestParam("term") Optional<String> term) {
		List<NameId> result;
		try {
			result = SearchUtils.doSearchTopWithFilter(elasticsearchClient, Taxpayer.class, FIELD_TAX_PAYER_ID, "name", term.orElse(""), 10)
			    .stream()
			    .map(t -> new NameId(t.get("name").toString(), t.get(FIELD_TAX_PAYER_ID).toString()))
			    .collect(Collectors.toList());
			return new SearchResult<>(result);
		} catch (IOException ex) {
			log.log(Level.SEVERE,"Search taxpayer failed", ex);
		}
		return null;
	}

	
	/**
	 * Method used for returning names of users that match a given term. Useful for 'auto complete' fields
	 */
	@Secured({"ROLE_TAXPAYER_READ"})
	@PostMapping("/taxpayer/names")
	@ApiOperation(value="Method used for returning names of taxpayers that match a given term. Useful for 'auto complete' fields")
	public ResponseEntity<List<String>> getTaxpayerNames(@ApiParam(required=false) @RequestParam("term") Optional<String> term) {
		List<String> names;
		if (term.isPresent()) {
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
	@ApiOperation(value="Add a new taxpayer",response=TaxpayerDto.class)
	@CacheEvict(value={"qualifierValues"})
    public ResponseEntity<Object> addTaxpayer(@Valid @RequestBody TaxpayerDto taxpayer, BindingResult result) {
        if (result.hasErrors()) {
        	return ControllerUtils.returnErrors(result, messageSource);
        }
        
        Optional<Taxpayer> existent = taxpayerRepository.findByTaxPayerId(taxpayer.getTaxPayerId());
        
        if (existent.isPresent()) {
           	return ControllerUtils.returnBadRequest("taxpayer.error.already.exists", messageSource, taxpayer.getTaxPayerId());
        }

        if (log.isLoggable(Level.INFO)) {
        	log.log(Level.INFO, String.format("Creating new taxpayer %s | %s", taxpayer.getName(), taxpayer.getTaxPayerId()));
        }
        
        try {
        	Taxpayer entity = new Taxpayer();
        	taxpayer.updateEntity(entity, false);
        	taxpayerRepository.saveWithTimestamp(entity);
        	taxpayer.setId(entity.getId());
        }
        catch (Exception ex) {
        	log.log(Level.SEVERE,"Create taxpayer failed", ex);
        	return ResponseEntity.badRequest().body(messageSource.getMessage(ERROR_OP_FAILED, null, LocaleContextHolder.getLocale()));
        }
        return ResponseEntity.ok().body(taxpayer);
    }

	@Secured({"ROLE_TAXPAYER_WRITE"})
	@PutMapping(value="/taxpayer/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Updates an existing taxpayer",response=TaxpayerDto.class)
	@CacheEvict(value={"qualifierValues"})
    public ResponseEntity<Object> updateTaxpayer(@PathVariable("id") String id, @Valid @RequestBody TaxpayerDto taxpayer, BindingResult result) {
        if (result.hasErrors()) {
        	return ControllerUtils.returnErrors(result, messageSource);
        }
        
        Optional<Taxpayer> existent = taxpayerRepository.findById(id);
        
        if (!existent.isPresent()) {
           	return ResponseEntity.notFound().build();
        }
        
        if (log.isLoggable(Level.INFO)) {
        	log.log(Level.INFO, String.format("Changing taxpayer #%s | %s | %s", id, taxpayer.getName(), taxpayer.getTaxPayerId()));
        }

        Taxpayer entity = existent.get();
        taxpayer.updateEntity(entity, false);
        taxpayerRepository.saveWithTimestamp(entity);
        
        return ResponseEntity.ok().body(taxpayer);
    }
    
	@Secured({"ROLE_TAXPAYER_WRITE"})
	@DeleteMapping(value="/taxpayer/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Deletes an existing taxpayer",response=TaxpayerDto.class)
	@CacheEvict(value={"qualifierValues"})
    public ResponseEntity<Object> deleteTaxpayer(@PathVariable("id") String id) {
    	Optional<Taxpayer> existent = taxpayerRepository.findById(id);
        if (!existent.isPresent())
        	return ResponseEntity.notFound().build();
        
        Taxpayer taxpayer = existent.get();
        if (log.isLoggable(Level.INFO)) {
        	log.log(Level.INFO, String.format("Deleting taxpayer #%s | %s | %s", id, taxpayer.getName(), taxpayer.getTaxPayerId()));
        }
        
        try {
        	taxpayer.setActive(false);
        	taxpayerRepository.saveWithTimestamp(taxpayer);
        }
        catch (Exception ex) {
        	log.log(Level.SEVERE,"Delete taxpayer failed", ex);
        	return ResponseEntity.badRequest().body(messageSource.getMessage(ERROR_OP_FAILED, null, LocaleContextHolder.getLocale()));
        }

        return ResponseEntity.ok().body(new TaxpayerDto(taxpayer));
    }
	
	@Secured({"ROLE_TAXPAYER_WRITE"})
    @GetMapping(value="/taxpayer/{id}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Activate an existing taxpayer", response=TaxpayerDto.class)
    public ResponseEntity<Object> activate(
    		@ApiParam(name = "Taxpayer ID", allowEmptyValue = false, allowMultiple = false, example = "1234567890", required = true, type = "String")
    		@PathVariable("id") String id) {
        
		Optional<Taxpayer> existing = taxpayerRepository.findById(id);
		if (!existing.isPresent())
        	return ResponseEntity.notFound().build();
		Taxpayer taxpayer = existing.get();
        if (log.isLoggable(Level.INFO)) {
        	log.log(Level.INFO, String.format("Activating taxpayer #%s | %s | %s", id, taxpayer.getName(), taxpayer.getTaxPayerId()));
        }
        
        try {
			taxpayer.setActive(true);
	        taxpayerRepository.saveWithTimestamp(taxpayer);
        }
        catch (Exception ex) {
        	log.log(Level.SEVERE,"Activate taxpayer failed", ex);
        	return ResponseEntity.badRequest().body(messageSource.getMessage(ERROR_OP_FAILED, null, LocaleContextHolder.getLocale()));
        }
        
        return ResponseEntity.ok().body(new TaxpayerDto(taxpayer));
    }

	@Secured({"ROLE_TAXPAYER_WRITE"})
    @GetMapping(value="/taxpayer/{id}/deactivate", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Deactivate an existing taxpayer", response=TaxpayerDto.class)
    public ResponseEntity<Object> deactivate(
    		@ApiParam(name = "Taxpayer ID", allowEmptyValue = false, allowMultiple = false, example = "1234567890", required = true, type = "String")
    		@PathVariable("id") String id) {
        
		Optional<Taxpayer> existing= taxpayerRepository.findById(id);
		if (!existing.isPresent())
        	return ResponseEntity.notFound().build();
		Taxpayer taxpayer= existing.get();
        if (log.isLoggable(Level.INFO)) {
        	log.log(Level.INFO, String.format("Deactivating taxpayer #%s | %s | %s", id, taxpayer.getName(), taxpayer.getTaxPayerId()));
        }
        
        try {
			taxpayer.setActive(false);
	        taxpayerRepository.saveWithTimestamp(taxpayer);
        }
        catch (Exception ex) {
        	log.log(Level.SEVERE,"Deactivate taxpayer failed", ex);
        	return ResponseEntity.badRequest().body(messageSource.getMessage(ERROR_OP_FAILED, null, LocaleContextHolder.getLocale()));
        }   
        return ResponseEntity.ok().body(new TaxpayerDto(taxpayer));
    }
	
	private Taxpayer addOrUpdateTaxpayer(Taxpayer entity, TaxpayerDto taxpayer, User user) {
		return taxpayer.updateEntity(entity, true);
	}
	
	@Secured({"ROLE_TAXPAYER_WRITE"})
    @PostMapping(value="/taxpayers", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Add multiple taxpayer",response=TaxpayerDto.class)
	@CacheEvict(value={"qualifierValues"})
    public ResponseEntity<Object> addTaxpayers(@Valid @RequestBody TaxpayerDto[] taxpayers, BindingResult result) {
        if (result.hasErrors()) {
        	return ControllerUtils.returnErrors(result, messageSource);
        }
        
        if (taxpayers==null || taxpayers.length==0) {
        	return ResponseEntity.badRequest().build();
        }
        
        Set<String> taxpayerIds = Arrays.stream(taxpayers)
        		.map( TaxpayerDto::getTaxPayerId)
        		.collect( Collectors.toSet());
        
        List<Taxpayer> existents = taxpayerRepository.findByTaxPayerIdIn(taxpayerIds);
        Map<String, Taxpayer> existentsMap = existents.stream()
        	.collect(Collectors.toMap(Taxpayer::getTaxPayerId, Function.identity()));
        List<Taxpayer> updated = Arrays.stream(taxpayers)
        	.map(t -> addOrUpdateTaxpayer( existentsMap.getOrDefault(t.getTaxPayerId(), new Taxpayer()), t, null))
        	.collect(Collectors.toList());
        
        try {
        	updated = Lists.newArrayList(taxpayerRepository.saveAllWithTimestamp(updated));
        }
        catch (Exception ex) {
        	log.log(Level.SEVERE,"Create taxpayer failed", ex);
        	return ResponseEntity.badRequest().body(messageSource.getMessage(ERROR_OP_FAILED, null, LocaleContextHolder.getLocale()));
        }
        return ResponseEntity.ok().body(updated);
    }
}
