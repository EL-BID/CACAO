package org.idb.cacao.web.controllers.rest;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.validation.Valid;

import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.web.repositories.DocumentTemplateRepository;
import org.idb.cacao.web.utils.ControllerUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.i18n.LocaleContextHolder;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/api")
@Api(description="Controller class for all endpoints related to 'template' object interacting by a REST interface")
public class DocumentTemplateAPIController {
	private static final Logger log = Logger.getLogger(DocumentTemplateAPIController.class.getName());
	
	@Autowired
	private MessageSource messageSource;

	@Autowired
	private DocumentTemplateRepository templateRepository;
	
//	@Secured({"ROLE_SYSADMIN","ROLE_SUPPORT","ROLE_MASTER","ROLE_AUTHORITY"})
    @PostMapping(value="/template", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Add a new document template",response=DocumentTemplate.class)
    public ResponseEntity<Object> addTemplate(@Valid @RequestBody DocumentTemplate template, BindingResult result) {
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

//	@Secured({"ROLE_SYSADMIN","ROLE_SUPPORT","ROLE_MASTER","ROLE_AUTHORITY"})
    @PutMapping(value="/template/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Updates an existing document template", response=DocumentTemplate.class)
    public ResponseEntity<Object> updateTemplate(@PathVariable("id") String id, @Valid @RequestBody DocumentTemplate template, BindingResult result) {
        if (result.hasErrors()) {
        	return ControllerUtils.returnErrors(result, messageSource);
        }
        
		Optional<DocumentTemplate> existing_template = templateRepository.findById(id);
		if (existing_template.isPresent()) {
			// just a few parts of DocumentTemplate object are editable 
			// let's copy all the properties to be preserved, except the properties that might change
			BeanUtils.copyProperties(existing_template.get(), template, 
					/*ignoreProperties = editable properties*/
					"name", "version", "periodicity", "required", "fields");
		}

        template.setId(id);
        templateRepository.saveWithTimestamp(template);
        return ResponseEntity.ok().body(template);
    }

}
