/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/

package org.idb.cacao.web.controllers.ui;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.IterableUtils;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.api.templates.DocumentInput;
import org.idb.cacao.api.templates.DocumentInputFieldMapping;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.FieldMapping;
import org.idb.cacao.api.templates.FieldType;
import org.idb.cacao.api.templates.TemplateArchetype;
import org.idb.cacao.api.templates.TemplateArchetypes;
import org.idb.cacao.web.dto.NameId;
import org.idb.cacao.web.repositories.DocumentTemplateRepository;
import org.idb.cacao.web.utils.ErrorUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller class for all endpoints related to UI regarding document templates.
 */
@Controller
public class DocumentTemplateUIController {
	private static final String ERROR_INVALID_TEMPLATE_ID = "Invalid template Id:";

	private static final String ATTRIBUTE_FIELD_MAPPINGS = "fieldMappings";

	private static final String ATTRIBUTE_FIELD_TYPES = "fieldTypes";

	private static final String ATTRIBUTE_TEMPLATE = "template";

	@Autowired
	private DocumentTemplateRepository documentTemplateRepository;

	@Autowired
	private MessageSource messages;
	
	@GetMapping("/templates")
	public String getTemplates(Model model) {
		try {
			model.addAttribute("templates", IterableUtils.toList(documentTemplateRepository.findAll(Sort.by("name.keyword","version.keyword").ascending())));
		}
		catch (Exception ex) {
			if (!ErrorUtils.isErrorNoIndexFound(ex))
				throw ex;
			model.addAttribute("templates", Collections.emptyList());
		}
        return "templates/list_templates";
	}

	@Secured({"ROLE_TAX_TEMPLATE_WRITE"})
	@GetMapping("/templates/add")
    public String showAddTemplate(Model model) {
		model.addAttribute("archetypes", 
				TemplateArchetypes.getNames()
					.stream()
					.map( name -> new NameId(messages.getMessage(name, null, LocaleContextHolder.getLocale()), name))
					.collect(Collectors.toList())
		);
		return "templates/add_template";
	}
    
    @GetMapping("/templates/{id}")
    public String showTemplate(@PathVariable("id") String id,  @RequestParam("showInputs") Optional<Boolean> showInputs, Model model) {
    	DocumentTemplate template = documentTemplateRepository.findById(id).orElseThrow(() -> new IllegalArgumentException(ERROR_INVALID_TEMPLATE_ID + id));
        model.addAttribute(ATTRIBUTE_TEMPLATE, template);
        model.addAttribute(ATTRIBUTE_FIELD_TYPES, FieldType.values());
        model.addAttribute(ATTRIBUTE_FIELD_MAPPINGS, FieldMapping.values());
        model.addAttribute("formats", DocumentFormat.values());
        model.addAttribute("showInputs", showInputs.orElse(false));
        return "templates/show_template";
    }
	
    @Secured({"ROLE_TAX_TEMPLATE_WRITE"})
    @GetMapping("/templates/{id}/edit")
    public String showUpdateForm(@PathVariable("id") String id, Model model) {
    	DocumentTemplate template = documentTemplateRepository.findById(id).orElseThrow(() -> new IllegalArgumentException(ERROR_INVALID_TEMPLATE_ID + id));
        model.addAttribute(ATTRIBUTE_TEMPLATE, template);
        model.addAttribute(ATTRIBUTE_FIELD_TYPES, FieldType.values());
        model.addAttribute(ATTRIBUTE_FIELD_MAPPINGS, FieldMapping.values());
        return "templates/edit_template";
    }

    @Secured({"ROLE_TAX_TEMPLATE_WRITE"})
    @GetMapping("/templates/{id}/editinput")
    public String showEditDocumentInput(@PathVariable("id") String id, @RequestParam("inputName") String inputName, Model model) {
    	DocumentTemplate template = documentTemplateRepository.findById(id).orElseThrow(() -> new IllegalArgumentException(ERROR_INVALID_TEMPLATE_ID + id));
    	DocumentInput docInput = template.getInputWithName(inputName);
    	if(docInput==null) {
    		throw new IllegalArgumentException("Input name " + inputName + " is not defined in template");
    	}
        model.addAttribute(ATTRIBUTE_TEMPLATE, template);
        model.addAttribute("docInput", docInput);
        return "templates/edit_doc_input";
    }

    @Secured({"ROLE_TAX_TEMPLATE_WRITE"})
    @GetMapping("/templates/{id}/addinput")
    public String showAddDocumentInput(@PathVariable("id") String id, @RequestParam("format") String format, Model model) {
    	DocumentTemplate template = documentTemplateRepository.findById(id).orElseThrow(() -> new IllegalArgumentException(ERROR_INVALID_TEMPLATE_ID + id));
    	DocumentFormat docFormat = DocumentFormat.valueOf(format);
    	DocumentInput docInput = new DocumentInput();
    	docInput.setFormat(docFormat);
    	template.getFields().stream().forEach( f -> docInput.addField(
    			new DocumentInputFieldMapping()
    			.withFieldName(f.getFieldName())
    			.withFieldId(f.getId())));
    	docInput.setFormat(docFormat);
        model.addAttribute(ATTRIBUTE_TEMPLATE, template);
        model.addAttribute("docInput", docInput);
        return "templates/edit_doc_input";
    }

    @Secured({"ROLE_TAX_TEMPLATE_WRITE"})
    @GetMapping(value="/templates/new")
    public String showEditForm(Model model, @RequestParam("type") Optional<String> typeParam, @RequestParam("id") Optional<String> idParam) {
    	String type=typeParam.orElse("empty");
    	String id=idParam.orElse("");
    	DocumentTemplate template = new DocumentTemplate();
		if (ATTRIBUTE_TEMPLATE.equals(type)) {
			DocumentTemplate referenceTemplate = documentTemplateRepository.findById(id).orElseThrow(() -> new IllegalArgumentException(ERROR_INVALID_TEMPLATE_ID + id));
			template.setName(referenceTemplate.getName());
			template.setArchetype(referenceTemplate.getArchetype());
			template.setGroup(referenceTemplate.getGroup());
			template.setFields(referenceTemplate.getFields());
			template.setPeriodicity(referenceTemplate.getPeriodicity());
			template.setRequired(referenceTemplate.getRequired());
		} else if ("archetype".equals(type)) {
			TemplateArchetype archetype = TemplateArchetypes.getArchetype(id).orElseThrow(() -> new IllegalArgumentException("Invalid archetype Id:" + id));
			template.setArchetype(id);
			template.setGroup(archetype.getSuggestedGroup());
			template.setFields(resolveDescriptions(archetype.getRequiredFields()));
			template.setName(messages.getMessage(id, null, LocaleContextHolder.getLocale()));
		}
		model.addAttribute(ATTRIBUTE_TEMPLATE, template);
        model.addAttribute(ATTRIBUTE_TEMPLATE, template);
        model.addAttribute(ATTRIBUTE_FIELD_TYPES, FieldType.values());
        model.addAttribute(ATTRIBUTE_FIELD_MAPPINGS, FieldMapping.values());
        return "templates/edit_template";
    
    }

    /**
     * Given a list of DocumentField's, check for the presence of descriptions enclosed in curly braces. For these
     * ones, tries to resolve them using the message properties file with the system default Locale.
     */
    public List<DocumentField> resolveDescriptions(List<DocumentField> fields) {
    	if (fields!=null && !fields.isEmpty()) {
    		for (DocumentField field: fields) {
    			if (field.getDescription()!=null 
    					&& field.getDescription().startsWith("{") 
    					&& field.getDescription().endsWith("}")) {
    				
    				try {
    					String desc = field.getDescription().substring(1, field.getDescription().length()-1);
    					String translated = messages.getMessage(desc, null, Locale.getDefault());
    					if (translated!=null && translated.trim().length()>0)
    						field.setDescription(translated);
    				}
    				catch (Exception ex) {
    					// keep the description as informed
    				}
    			}
    		}
    	}
    	return fields;
    }
}
