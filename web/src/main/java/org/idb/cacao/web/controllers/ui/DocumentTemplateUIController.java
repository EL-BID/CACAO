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
import java.util.Optional;
import java.util.stream.Collectors;

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
	@Autowired
	private DocumentTemplateRepository documentTemplateRepository;

	@Autowired
	private MessageSource messages;
	
	@GetMapping("/templates")
	public String getTemplates(Model model) {
		try {
			model.addAttribute("templates", documentTemplateRepository.findAll(Sort.by("name.keyword","version.keyword").ascending()));
		}
		catch (Throwable ex) {
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
    public String showTemplate(@PathVariable("id") String id, Model model) {
    	DocumentTemplate template = documentTemplateRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid template Id:" + id));
        model.addAttribute("template", template);
        model.addAttribute("fieldTypes", FieldType.values());
        model.addAttribute("fieldMappings", FieldMapping.values());
        List<DocumentFormat> usedFormats = template.getInputs().stream()
        	.map(input -> input.getFormat())
        	.collect(Collectors.toList());
        List<DocumentFormat> availableFormats = Arrays.stream(DocumentFormat.values())
            .filter(format -> !usedFormats.contains(format))
            .collect(Collectors.toList());
        model.addAttribute("formats", availableFormats);
        return "templates/show_template";
    }
	
    @Secured({"ROLE_TAX_TEMPLATE_WRITE"})
    @GetMapping("/templates/{id}/edit")
    public String showUpdateForm(@PathVariable("id") String id, Model model) {
    	DocumentTemplate template = documentTemplateRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid template Id:" + id));
        model.addAttribute("template", template);
        model.addAttribute("fieldTypes", FieldType.values());
        model.addAttribute("fieldMappings", FieldMapping.values());
        return "templates/edit_template";
    }

    @Secured({"ROLE_TAX_TEMPLATE_WRITE"})
    @GetMapping("/templates/{id}/input/{format}")
    public String showEditDocumentInput(@PathVariable("id") String id, @PathVariable("format") String format, Model model) {
    	DocumentTemplate template = documentTemplateRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid template Id:" + id));
    	DocumentFormat docFormat = DocumentFormat.valueOf(format);
    	DocumentInput docInput = template.getInputOfFormat(docFormat);
    	if(docInput==null) {
    		throw new IllegalArgumentException("Input format " + docFormat.toString() + " is not defined in template");
    	}
        model.addAttribute("template", template);
        model.addAttribute("docInput", docInput);
        return "templates/edit_doc_input";
    }

    @Secured({"ROLE_TAX_TEMPLATE_WRITE"})
    @GetMapping("/templates/{id}/addinput")
    public String showAddDocumentInput(@PathVariable("id") String id, @RequestParam("format") String format, Model model) {
    	DocumentTemplate template = documentTemplateRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid template Id:" + id));
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
        model.addAttribute("template", template);
        model.addAttribute("docInput", docInput);
        return "templates/edit_doc_input";
    }

    @Secured({"ROLE_TAX_TEMPLATE_WRITE"})
    @GetMapping(value="/templates/new")
    public String showEditForm(Model model, @RequestParam("type") Optional<String> type_param, @RequestParam("id") Optional<String> id_param) {
    	String type=type_param.orElse("empty");
    	String id=id_param.orElse("");
    	DocumentTemplate template = new DocumentTemplate();
		switch(type) {
			case "template":
				DocumentTemplate referenceTemplate = documentTemplateRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid template Id:" + id));
				template.setName(referenceTemplate.getName());
				template.setArchetype(referenceTemplate.getArchetype());
				template.setGroup(referenceTemplate.getGroup());
				template.setFields(referenceTemplate.getFields());
				template.setPeriodicity(referenceTemplate.getPeriodicity());
				template.setRequired(referenceTemplate.getRequired());
				break;
			case "archetype":
				TemplateArchetype archetype = TemplateArchetypes.getArchetype(id).orElseThrow(() -> new IllegalArgumentException("Invalid archetype Id:" + id));
				template.setArchetype(id);
				template.setGroup(archetype.getSuggestedGroup());
				template.setFields(archetype.getRequiredFields());
				template.setName(messages.getMessage(id, null, LocaleContextHolder.getLocale()));
				break;
		}
		model.addAttribute("template", template);
        model.addAttribute("template", template);
        model.addAttribute("fieldTypes", FieldType.values());
        model.addAttribute("fieldMappings", FieldMapping.values());
        return "templates/edit_template";
    
    }

}
