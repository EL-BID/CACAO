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

package org.idb.cacao.web.controllers.ui;

import java.util.Arrays;
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
        List<DocumentFormat> usedFormats = template.getInputs()== null ? Collections.emptyList() : 
        	template.getInputs().stream()
        	.map(DocumentInput::getFormat)
        	.collect(Collectors.toList());
        List<DocumentFormat> availableFormats = Arrays.stream(DocumentFormat.values())
            .filter(format -> !usedFormats.contains(format))
            .collect(Collectors.toList());
        model.addAttribute("formats", availableFormats);
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
    @GetMapping("/templates/{id}/input/{format}")
    public String showEditDocumentInput(@PathVariable("id") String id, @PathVariable("format") String format, Model model) {
    	DocumentTemplate template = documentTemplateRepository.findById(id).orElseThrow(() -> new IllegalArgumentException(ERROR_INVALID_TEMPLATE_ID + id));
    	DocumentFormat docFormat = DocumentFormat.valueOf(format);
    	DocumentInput docInput = template.getInputOfFormat(docFormat);
    	if(docInput==null) {
    		throw new IllegalArgumentException("Input format " + docFormat.toString() + " is not defined in template");
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
    	if (template.getInputOfFormat(docFormat)!=null) {
    		throw new IllegalArgumentException("Input format " + docFormat.toString() + " is already defined in template");
    	}
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
