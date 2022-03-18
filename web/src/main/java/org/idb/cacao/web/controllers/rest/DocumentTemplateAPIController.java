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

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.validation.Valid;

import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.sort.SortOrder;
import org.idb.cacao.api.templates.DocumentInput;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.utils.ScrollUtils;
import org.idb.cacao.web.controllers.AdvancedSearch;
import org.idb.cacao.web.controllers.services.DocumentTemplateService;
import org.idb.cacao.web.dto.DocumentTemplateDto;
import org.idb.cacao.web.dto.PaginationData;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.errors.UserNotFoundException;
import org.idb.cacao.web.repositories.DocumentTemplateRepository;
import org.idb.cacao.web.utils.ControllerUtils;
import org.idb.cacao.web.utils.SearchUtils;
import org.idb.cacao.web.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
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

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name="document-template-api-controller", description="Controller class for all endpoints related to 'template' object interacting by a REST interface")
public class DocumentTemplateAPIController {
	private static final Logger log = Logger.getLogger(DocumentTemplateAPIController.class.getName());
	
	@Autowired
	private MessageSource messageSource;

	@Autowired
	private DocumentTemplateRepository templateRepository;
	
	@Autowired
	private DocumentTemplateService templateService;
	
	@Autowired
	private RestHighLevelClient elasticsearchClient;
	
    @GetMapping(value="/template", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Returns the list of all document templates",response=DocumentTemplateDto[].class)
	public ResponseEntity<DocumentTemplateDto[]> getTemplates() {
		List<DocumentTemplateDto> allTemplates = new LinkedList<>();
		try (Stream<DocumentTemplate> stream = ScrollUtils.findAll(templateRepository, elasticsearchClient, 1);) {
			stream.forEach(t -> allTemplates.add(new DocumentTemplateDto(t)));
		}
		return ResponseEntity.ok(allTemplates.toArray(new DocumentTemplateDto[0]));
	}

    @GetMapping(value="/template/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Returns the template configuration given its internal identification",response=DocumentTemplateDto.class)
	public ResponseEntity<Object> getTemplate(
			@ApiParam(name = "Document ID", allowEmptyValue = false, allowMultiple = false, example = "1234567890", required = true, type = "String")
			@PathVariable("id") String id) {
		Optional<DocumentTemplate> match = templateRepository.findById(id);
		if (!match.isPresent())
        	return ResponseEntity.notFound().build();

		return ResponseEntity.ok(new DocumentTemplateDto(match.get()));
	}

	@Secured({"ROLE_TAX_TEMPLATE_WRITE"})
    @PostMapping(value="/template", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Add a new document template",response=DocumentTemplateDto.class)
    public ResponseEntity<Object> addTemplate(@Valid @RequestBody DocumentTemplateDto template, BindingResult result) {
		
		templateService.validateTemplate(template, result);
		
        if (result.hasErrors()) {
        	return ControllerUtils.returnErrors(result, messageSource);
        }
        
        Optional<DocumentTemplate> existing = templateRepository.findByNameAndVersion(template.getName(), template.getVersion());
		if (existing.isPresent()) {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			String username = (auth==null) ? null : auth.getName();
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, String.format("User %s attempted to create new template with name %s and version %s, but there is already an existing template with the same name and version",
						username, template.getName(), template.getVersion()));
			}
			return ResponseEntity.ok().body(existing.get());
		}
		
		DocumentTemplate entity = new DocumentTemplate();
		template.updateEntity(entity);
		templateService.compatibilizeTemplateFieldsMappings(entity);

        try {
        	templateRepository.saveWithTimestamp(entity);
        }
        catch (Exception ex) {
        	log.log(Level.SEVERE,"Create template failed", ex);
        	return ResponseEntity.badRequest().body(messageSource.getMessage("op.failed", null, LocaleContextHolder.getLocale()));
        }
        
        return ResponseEntity.ok().body(new DocumentTemplateDto(entity));
    }

	@Secured({"ROLE_TAX_TEMPLATE_WRITE"})
    @PutMapping(value="/template/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Updates an existing document template", response=DocumentTemplateDto.class)
    public ResponseEntity<Object> updateTemplate(@PathVariable("id") String id, @Valid @RequestBody DocumentTemplateDto template, BindingResult result) {
		
		templateService.validateTemplate(template, result);
		
        if (result.hasErrors()) {
        	return ControllerUtils.returnErrors(result, messageSource);
        }
        
		Optional<DocumentTemplate> existing = templateRepository.findById(id);
		if (!existing.isPresent())
        	return ResponseEntity.notFound().build();

		DocumentTemplate entity = existing.get();
		template.setInputs(entity.getInputs());
		template.updateEntity(entity);
		templateService.compatibilizeTemplateFieldsMappings(entity);
        templateRepository.saveWithTimestamp(entity);
        return ResponseEntity.ok().body(new DocumentTemplateDto(entity));
    }

	@Secured({"ROLE_TAX_TEMPLATE_WRITE"})
    @GetMapping(value="/template/{id}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Activate an existing document template", response=DocumentTemplateDto.class)
    public ResponseEntity<Object> activateTemplate(
    		@ApiParam(name = "Document ID", allowEmptyValue = false, allowMultiple = false, example = "1234567890", required = true, type = "String")
    		@PathVariable("id") String id) {
        
		Optional<DocumentTemplate> existing = templateRepository.findById(id);
		if (!existing.isPresent())
        	return ResponseEntity.notFound().build();
		DocumentTemplate template = existing.get();
		template.setActive(true);
        templateRepository.saveWithTimestamp(template);
        return ResponseEntity.ok().body(new DocumentTemplateDto(template));
    }

	@Secured({"ROLE_TAX_TEMPLATE_WRITE"})
    @GetMapping(value="/template/{id}/deactivate", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Deactivate an existing document template", response=DocumentTemplateDto.class)
    public ResponseEntity<Object> deactivateTemplate(
    		@ApiParam(name = "Document ID", allowEmptyValue = false, allowMultiple = false, example = "1234567890", required = true, type = "String")
    		@PathVariable("id") String id) {
        
		Optional<DocumentTemplate> existing = templateRepository.findById(id);
		if (!existing.isPresent())
        	return ResponseEntity.notFound().build();
		DocumentTemplate template = existing.get();
		template.setActive(false);
        templateRepository.saveWithTimestamp(template);
        return ResponseEntity.ok().body(new DocumentTemplateDto(template));
    }

	@Secured({"ROLE_TAX_TEMPLATE_WRITE"})
    @DeleteMapping(value="/template/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Deletes an existing document template", response=DocumentTemplateDto.class)
    public ResponseEntity<Object> deleteTemplate(
    		@ApiParam(name = "Document ID", allowEmptyValue = false, allowMultiple = false, example = "1234567890", required = true, type = "String")
    		@PathVariable("id") String id) {
        try {
        	templateRepository.deleteById(id);
        } catch(Exception e) {
        	return ControllerUtils.returnBadRequest("template.not.exists", messageSource);
        }

        return ResponseEntity.ok().body(id);
    }

	
	@Secured({"ROLE_TAX_TEMPLATE_WRITE"})
    @PostMapping(value="/template/{id}/input", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Creates a new document input", response=DocumentTemplateDto.class)
    public ResponseEntity<Object> addDocumentInput(@PathVariable("id") String id, @Valid @RequestBody DocumentInput docInput, BindingResult result) {
        if (result.hasErrors()) {
        	return ControllerUtils.returnErrors(result, messageSource);
        }
		Optional<DocumentTemplate> match = templateRepository.findById(id);
		
		if (!match.isPresent())
        	return ResponseEntity.notFound().build();
		
		DocumentTemplate template = match.get();
		
		DocumentInput existingDocInput = template.getInputWithName(docInput.getInputName());
		if (existingDocInput!=null) {
			return ControllerUtils.returnBadRequest("template.input.format.exists", messageSource, docInput.getInputName());
		}
		template.addInput(docInput);
		templateService.compatibilizeTemplateFieldsMappings(template);
        templateRepository.saveWithTimestamp(template);
        return ResponseEntity.ok().body(new DocumentTemplateDto(template));
    }

    
	@Secured({"ROLE_TAX_TEMPLATE_WRITE"})
    @PutMapping(value="/template/{id}/input", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Updates an existing document template", response=DocumentTemplateDto.class)
    public ResponseEntity<Object> editDocumentInput(@PathVariable("id") String id, @Valid @RequestBody DocumentInput docInput, BindingResult result) {
        if (result.hasErrors()) {
        	return ControllerUtils.returnErrors(result, messageSource);
        }
		Optional<DocumentTemplate> match = templateRepository.findById(id);
		
		if (!match.isPresent())
        	return ResponseEntity.notFound().build();
		
		DocumentTemplate template = match.get();
		
		DocumentInput existingDocInput = template.getInputWithName(docInput.getInputName());
		if (existingDocInput==null) {
			ControllerUtils.returnBadRequest("template.input.format.not.exists", messageSource, docInput.getFormat().toString());
		}
		else {
			existingDocInput.setInputName(docInput.getInputName());
			existingDocInput.setFields(docInput.getFields());
			existingDocInput.setFieldsIdsMatchingTemplate(template);
			existingDocInput.setAcceptIncompleteFiles(docInput.getAcceptIncompleteFiles());
		}
		templateService.compatibilizeTemplateFieldsMappings(template);
        templateRepository.saveWithTimestamp(template);
        return ResponseEntity.ok().body(new DocumentTemplateDto(template));
    }
    
    
    /**
	 * Search templates with pagination and filters
	 * @return
	 */
	@GetMapping(value="/templates", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Method used for listing existing templates")
	public PaginationData<DocumentTemplateDto> getTemplatesWithPagination(Model model, 
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
		Page<DocumentTemplateDto> docs;
		Optional<String> sortField = Optional.of(sortBy.orElse("name"));
		Optional<SortOrder> direction = Optional.of(sortOrder.orElse("asc").equals("asc") ? SortOrder.ASC : SortOrder.DESC);
		try {
			docs = SearchUtils.doSearch(filters.orElse(new AdvancedSearch()), DocumentTemplate.class, elasticsearchClient, page, size, 
					sortField, direction)
					  .map(DocumentTemplateDto::new);

		}
		catch (Exception ex) {
			log.log(Level.SEVERE, "Error while searching for all documents", ex);
			docs = Page.empty();
		}		
		return new PaginationData<>(docs.getTotalPages(), docs.getContent());
	}
	
}
