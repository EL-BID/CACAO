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
import org.idb.cacao.web.dto.PaginationData;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.errors.UserNotFoundException;
import org.idb.cacao.web.repositories.DocumentTemplateRepository;
import org.idb.cacao.web.utils.ControllerUtils;
import org.idb.cacao.web.utils.SearchUtils;
import org.idb.cacao.web.utils.UserUtils;
import org.springframework.beans.BeanUtils;
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
	@ApiOperation(value="Returns the list of all document templates",response=DocumentTemplate[].class)
	public ResponseEntity<DocumentTemplate[]> getTemplates() {
		List<DocumentTemplate> allTemplates = new LinkedList<>();
		try (Stream<DocumentTemplate> stream = ScrollUtils.findAll(templateRepository, elasticsearchClient, 1);) {
			stream.forEach(allTemplates::add);
		}
		return ResponseEntity.ok(allTemplates.toArray(new DocumentTemplate[0]));
	}

    @GetMapping(value="/template/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Returns the template configuration given its internal identification",response=DocumentTemplate.class)
	public ResponseEntity<Object> getTemplate(@PathVariable("id") String id) {
		Optional<DocumentTemplate> match = templateRepository.findById(id);
		if (!match.isPresent())
        	return ResponseEntity.notFound().build();

		return ResponseEntity.ok(match.get());
	}

	@Secured({"ROLE_TAX_TEMPLATE_WRITE"})
    @PostMapping(value="/template", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Add a new document template",response=DocumentTemplate.class)
    public ResponseEntity<Object> addTemplate(@Valid @RequestBody DocumentTemplate template, BindingResult result) {
		
		templateService.validateTemplate(template, result);
		
        if (result.hasErrors()) {
        	return ControllerUtils.returnErrors(result, messageSource);
        }
        
        Optional<DocumentTemplate> existing_template = templateRepository.findByNameAndVersion(template.getName(), template.getVersion());
		if (existing_template!=null && existing_template.isPresent()) {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			String username = (auth==null) ? null : auth.getName();
			log.log(Level.FINE, "User "+username+" attempted to create new template with name "+template.getName()+" and version "+template.getVersion()+", but there is already an existing template with the same name and version");
			return ResponseEntity.ok().body(existing_template.get());
		}
   
		templateService.compatibilizeTemplateFieldsMappings(template);

//        template.setTemplateCreateTime(new Date());
        
        try {
        	templateRepository.saveWithTimestamp(template);
        }
        catch (Exception ex) {
        	log.log(Level.SEVERE,"Create template failed", ex);
        	return ResponseEntity.badRequest().body(messageSource.getMessage("op.failed", null, LocaleContextHolder.getLocale()));
        }
        
        return ResponseEntity.ok().body(template);
    }

	@Secured({"ROLE_TAX_TEMPLATE_WRITE"})
    @PutMapping(value="/template/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Updates an existing document template", response=DocumentTemplate.class)
    public ResponseEntity<Object> updateTemplate(@PathVariable("id") String id, @Valid @RequestBody DocumentTemplate template, BindingResult result) {
		
		templateService.validateTemplate(template, result);
		
        if (result.hasErrors()) {
        	return ControllerUtils.returnErrors(result, messageSource);
        }
        
		Optional<DocumentTemplate> existing_template = templateRepository.findById(id);
		if (existing_template.isPresent()) {
			// just a few parts of DocumentTemplate object are editable 
			// let's copy all the properties to be preserved, except the properties that might change
			BeanUtils.copyProperties(existing_template.get(), template, 
					/*ignoreProperties = editable properties*/
					"name", "version", "periodicity", "required", "fields", "active");
		}

        template.setId(id);
		templateService.compatibilizeTemplateFieldsMappings(template);
        templateRepository.saveWithTimestamp(template);
        return ResponseEntity.ok().body(template);
    }

	@Secured({"ROLE_TAX_TEMPLATE_WRITE"})
    @GetMapping(value="/template/{id}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Activate an existing document template", response=DocumentTemplate.class)
    public ResponseEntity<Object> activateTemplate(@PathVariable("id") String id) {
        
		Optional<DocumentTemplate> existing_template = templateRepository.findById(id);
		if (!existing_template.isPresent())
        	return ResponseEntity.notFound().build();
		DocumentTemplate template = existing_template.get();
		template.setActive(true);
        templateRepository.saveWithTimestamp(template);
        return ResponseEntity.ok().body(template);
    }

	@Secured({"ROLE_TAX_TEMPLATE_WRITE"})
    @GetMapping(value="/template/{id}/deactivate", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Deactivate an existing document template", response=DocumentTemplate.class)
    public ResponseEntity<Object> deactivateTemplate(@PathVariable("id") String id) {
        
		Optional<DocumentTemplate> existing_template = templateRepository.findById(id);
		if (!existing_template.isPresent())
        	return ResponseEntity.notFound().build();
		DocumentTemplate template = existing_template.get();
		template.setActive(false);
        templateRepository.saveWithTimestamp(template);
        return ResponseEntity.ok().body(template);
    }

	@Secured({"ROLE_TAX_TEMPLATE_WRITE"})
    @DeleteMapping(value="/template/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Deletes an existing document template", response=DocumentTemplate.class)
    public ResponseEntity<Object> deleteTemplate(@PathVariable("id") String id) {
        try {
        	templateRepository.deleteById(id);
        } catch(Exception e) {
        	return ControllerUtils.returnBadRequest("template.not.exists", messageSource);
        }

        return ResponseEntity.ok().body(id);
    }

	
	@Secured({"ROLE_TAX_TEMPLATE_WRITE"})
    @PostMapping(value="/template/{id}/input", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Creates a new document input", response=DocumentTemplate.class)
    public ResponseEntity<Object> addDocumentInput(@PathVariable("id") String id, @Valid @RequestBody DocumentInput docInput, BindingResult result) {
        if (result.hasErrors()) {
        	return ControllerUtils.returnErrors(result, messageSource);
        }
		Optional<DocumentTemplate> match = templateRepository.findById(id);
		
		if (!match.isPresent())
        	return ResponseEntity.notFound().build();
		
		DocumentTemplate template = match.get();
		
		DocumentInput existingDocInput = template.getInputOfFormat(docInput.getFormat());
		if (existingDocInput!=null) {
			return ControllerUtils.returnBadRequest("template.input.format.exists", messageSource, docInput.getFormat().toString());
		}
		template.addInput(docInput);
		templateService.compatibilizeTemplateFieldsMappings(template);
        templateRepository.saveWithTimestamp(template);
        return ResponseEntity.ok().body(template);
    }

    
	@Secured({"ROLE_TAX_TEMPLATE_WRITE"})
    @PutMapping(value="/template/{id}/input", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Updates an existing document template", response=DocumentTemplate.class)
    public ResponseEntity<Object> editDocumentInput(@PathVariable("id") String id, @Valid @RequestBody DocumentInput docInput, BindingResult result) {
        if (result.hasErrors()) {
        	return ControllerUtils.returnErrors(result, messageSource);
        }
		Optional<DocumentTemplate> match = templateRepository.findById(id);
		
		if (!match.isPresent())
        	return ResponseEntity.notFound().build();
		
		DocumentTemplate template = match.get();
		
		DocumentInput existingDocInput = template.getInputOfFormat(docInput.getFormat());
		if (existingDocInput==null) {
			ControllerUtils.returnBadRequest("template.input.format.not.exists", messageSource, docInput.getFormat().toString());
		}
		else {
			existingDocInput.setInputName(docInput.getInputName());
			existingDocInput.setFields(docInput.getFields());
			existingDocInput.setFieldsIdsMatchingTemplate(template);
		}
		templateService.compatibilizeTemplateFieldsMappings(template);
        templateRepository.saveWithTimestamp(template);
        return ResponseEntity.ok().body(template);
    }
    
    
    /**
	 * Search templates with pagination and filters
	 * @return
	 */
	@GetMapping(value="/templates", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Method used for listing existing templates")
	public PaginationData<DocumentTemplate> getTemplatesWithPagination(Model model, @RequestParam("page") Optional<Integer> page,
			@RequestParam("size") Optional<Integer> size, @RequestParam("filter") Optional<String> filter, 
			@RequestParam("sortby") Optional<String> sortBy, @RequestParam("sortorder") Optional<String> sortOrder) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();

		Optional<AdvancedSearch> filters = SearchUtils.fromTabulatorJSON(filter);
		Page<DocumentTemplate> docs;
		Optional<String> sortField = Optional.of(sortBy.orElse("name"));
		Optional<SortOrder> direction = Optional.of(sortOrder.orElse("asc").equals("asc") ? SortOrder.ASC : SortOrder.DESC);
		try {
			docs = SearchUtils.doSearch(filters.orElse(new AdvancedSearch()), DocumentTemplate.class, elasticsearchClient, page, size, 
					sortField, direction);

		}
		catch (Exception ex) {
			log.log(Level.SEVERE, "Error while searching for all documents", ex);
			docs = Page.empty();
		}		
		PaginationData<DocumentTemplate> result = new PaginationData<>(docs.getTotalPages(), docs.getContent());
		return result;
	}
	
}
