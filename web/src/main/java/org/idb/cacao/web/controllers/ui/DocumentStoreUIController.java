/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
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
import org.idb.cacao.web.utils.ControllerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
	
	@Value("${presentation.mode}")
	private Boolean presentationMode;

	@Secured({"ROLE_TAX_DECLARATION_WRITE"})
	@GetMapping("/docs")
	public String getDocs(Model model) {
		
		if (Boolean.TRUE.equals(presentationMode))
			return ControllerUtils.redirectToPresentationWarning(model, messageSource);
		
		model.addAttribute("templates", templateService.getNamesTemplatesWithVersions());
		return "docs/docs_main";
	}

	@Secured({"ROLE_TAX_DECLARATION_READ"})
	@GetMapping("/docs-search")
	public String searchDocs(Model model) {
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
			log.log(Level.SEVERE, String.format("Error while redirecting for download document %s", documentId), e);
		}
    }
}