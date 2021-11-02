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

import java.util.List;

import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.web.controllers.services.DocumentTemplateService;
import org.idb.cacao.web.repositories.DocumentTemplateRepository;
import org.idb.cacao.web.utils.CreateDocumentTemplatesSamples;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller class for all endpoints related to 'document' object interacting by a user interface
 * 
 * @author Luis Kauer
 *
 */
@Controller
public class DocumentStoreUIController {
	
	//private static final Logger log = Logger.getLogger(DocumentStoreUIController.class.getName());
	@Autowired
	private DocumentTemplateRepository templateRepository;
	
	@Autowired
	private DocumentTemplateService templateService;
	
	@GetMapping("/docs")
    public String getDocs(Model model) {
		model.addAttribute("templates", templateService.getNamesTemplatesWithFiles());
        return "docs/docs_main";
    }
	
	@GetMapping("/addSamples")
    public String addSampleDocs(Model model) {
		
		List<DocumentTemplate> samples = CreateDocumentTemplatesSamples.getSampleTemplates();
		
		samples.forEach(s->templateRepository.saveWithTimestamp(s));
		
		model.addAttribute("templates", templateService.getNamesTemplatesWithFiles());
        return "docs/docs_main";
    }
	
}