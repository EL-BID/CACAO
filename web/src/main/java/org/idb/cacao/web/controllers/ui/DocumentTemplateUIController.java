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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.FieldMapping;
import org.idb.cacao.api.templates.FieldType;
import org.idb.cacao.web.repositories.DocumentTemplateRepository;
import org.idb.cacao.web.utils.ErrorUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class DocumentTemplateUIController {
	@Autowired
	private DocumentTemplateRepository documentTemplateRepository;

	@Autowired
	private MessageSource messages;
	
	//@Secured({"ROLE_SYSADMIN","ROLE_SUPPORT","ROLE_MASTER","ROLE_AUTHORITY"})
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

//	@Secured({"ROLE_SYSADMIN","ROLE_SUPPORT","ROLE_MASTER","ROLE_AUTHORITY"})
	@GetMapping("/addtemplate")
    public String showAddTemplate(@ModelAttribute("template") DocumentTemplate template) {
		return "templates/add_template";
	}
    
//	@Secured({"ROLE_SYSADMIN","ROLE_SUPPORT","ROLE_MASTER","ROLE_AUTHORITY"})
    @GetMapping("/edittemplate/{id}")
    public String showUpdateForm(@PathVariable("id") String id, Model model) {
    	DocumentTemplate template = documentTemplateRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid template Id:" + id));
        model.addAttribute("template", template);
        Map<Object, Object> fieldTypes = Arrays.stream(FieldType.values())
        	.collect(Collectors.toMap(t -> t.name(), t -> messages.getMessage(t.toString(), null, LocaleContextHolder.getLocale())));
        model.addAttribute("fieldTypes", fieldTypes);
        Map<String, String> fieldMappings = Arrays.stream(FieldMapping.values())
			.collect(Collectors.toMap(t -> t.name(), t -> messages.getMessage(t.toString(), null, LocaleContextHolder.getLocale())));
        model.addAttribute("fieldMappings", fieldMappings);
        return "templates/edit_template";
    }
}
