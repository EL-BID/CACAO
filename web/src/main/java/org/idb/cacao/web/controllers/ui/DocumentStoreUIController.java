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

import org.idb.cacao.api.DocumentSituationHistory;
import org.idb.cacao.api.DocumentValidationErrorMessage;
import org.idb.cacao.web.controllers.services.DocumentTemplateService;
import org.idb.cacao.web.controllers.services.MessagesService;
import org.idb.cacao.web.errors.MissingParameter;
import org.idb.cacao.web.repositories.DocumentSituationHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Controller class for all endpoints related to 'document' object interacting
 * by a user interface
 * 
 * @author Luis Kauer
 *
 */
@Controller
public class DocumentStoreUIController {

	@Autowired
	private DocumentTemplateService templateService;

	@Autowired
	private DocumentSituationHistoryRepository documentsSituationHistoryRepository;

	@Autowired
	private MessagesService messagesService;

	@Autowired
	private MessageSource messageSource;

	@GetMapping("/docs")
	public String getDocs(Model model) {
		model.addAttribute("templates", templateService.getNamesTemplatesWithVersions());
		return "docs/docs_main";
	}

	@GetMapping("/docs_search")
	public String searchDocs(Model model) {
		model.addAttribute("templates", templateService.getNamesTemplatesWithVersions());
		return "docs/docs_search";
	}

	@GetMapping("/docs/situations/{documentId}")
    public String getDocSituations(@PathVariable("documentId") String documentId, Model model) {
		
		// Parse the 'templateName' informed at request path
		if (documentId==null || documentId.trim().length()==0) {
			throw new MissingParameter("documentId");
		}
		List<DocumentSituationHistory> situations = documentsSituationHistoryRepository.findByDocumentId(documentId);
		
		model.addAttribute("situations", situations);		
		model.addAttribute("dateTimeFormat", messageSource.getMessage("timestamp.format", null, LocaleContextHolder.getLocale()));
        return "docs/docs_situation";
    }

	@GetMapping("/docs/errors/{documentId}")
	public String getDocErrors(@PathVariable("documentId") String documentId, Model model) {

		// Parse the 'templateName' informed at request path
		if (documentId == null || documentId.trim().length() == 0) {
			throw new MissingParameter("documentId");
		}
		List<DocumentValidationErrorMessage> messages = messagesService.findByDocumentId(documentId);

		model.addAttribute("messages", messages);
		model.addAttribute("dateTimeFormat",
				messageSource.getMessage("timestamp.format", null, LocaleContextHolder.getLocale()));
		return "docs/docs_errors";
	}

}