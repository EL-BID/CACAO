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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.idb.cacao.web.controllers.services.DocumentTemplateService;
import org.idb.cacao.web.errors.MissingParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.annotation.Secured;
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
	
	private static final Logger log = Logger.getLogger(DocumentStoreUIController.class.getName());

	@Autowired
	private DocumentTemplateService templateService;
	
	@Autowired
	private MessageSource messageSource;

	@Secured({"ROLE_TAX_DECLARATION_WRITE"})
	@GetMapping("/docs")
	public String getDocs(Model model) {
		model.addAttribute("templates", templateService.getNamesTemplatesWithVersions());
		return "docs/docs_main";
	}

	@Secured({"ROLE_TAX_DECLARATION_READ"})
	@GetMapping("/docs_search")
	public String searchDocs(Model model) {
		model.addAttribute("templates", templateService.getNamesTemplatesWithVersions());
		return "docs/docs_search";
	}

	@Secured({"ROLE_TAX_DECLARATION_READ"})
	@GetMapping("/doc/situations/{documentId}")
    public String getDocSituations(@PathVariable("documentId") String documentId, Model model) {
		
		// Parse the 'templateName' informed at request path
		if (documentId==null || documentId.trim().length()==0) {
			throw new MissingParameter("documentId");
		}
		model.addAttribute("documentId", documentId);		
		model.addAttribute("dateTimeFormat", messageSource.getMessage("timestamp.format", null, LocaleContextHolder.getLocale()));
        return "docs/doc_situations";
    }

	@Secured({"ROLE_TAX_DECLARATION_READ"})
	@GetMapping("/doc/errors/{documentId}")
	public String getDocErrors(@PathVariable("documentId") String documentId, Model model) {

		// Parse the 'templateName' informed at request path
		if (documentId == null || documentId.trim().length() == 0) {
			throw new MissingParameter("documentId");
		}

		model.addAttribute("documentId", documentId);
		model.addAttribute("dateTimeFormat",
				messageSource.getMessage("timestamp.format", null, LocaleContextHolder.getLocale()));
		return "docs/doc_errors";
	}
	
	@Secured({"ROLE_TAX_DECLARATION_READ"})
	@GetMapping("/doc/download/{documentId}")
    public void downloadDocument(HttpServletResponse response, @PathVariable("documentId") String documentId) {
		
		// Parse the 'templateName' informed at request path
		if (documentId==null || documentId.trim().length()==0) {
			throw new MissingParameter("documentId");
		}
		try {
			response.sendRedirect("/api/doc/download?documentId=" + documentId);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Error while redirecting for download document " + documentId, e);
		}
    }
}