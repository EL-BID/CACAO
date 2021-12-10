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

import java.util.stream.Collectors;

import org.idb.cacao.api.templates.DomainTable;
import org.idb.cacao.web.controllers.dto.NameId;
import org.idb.cacao.web.controllers.services.DomainTableService;
import org.idb.cacao.web.repositories.DomainTableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Controller class for all endpoints related to 'domain table' object interacting by a
 * user interface
 * 
 * @author Luis Kauer
 *
 */
@Controller
public class DomainTableUIController {

	@Autowired
	private DomainTableRepository domainTableRepository;

	@Autowired
	private DomainTableService domainTableService;
	
	@Autowired
	private MessageSource messageSource;
	
	@GetMapping("/domaintables")
	@Transactional
	public String getDomainTables(Model model) {
		return "domain/domain-tables";
	}

	@Secured({"ROLE_TAX_DOMAIN_TABLE_WRITE"})
	@GetMapping("/adddomaintable")
	public String showAddDomainTable(DomainTable table, Model model) {
		model.addAttribute("table", new DomainTable());
		model.addAttribute("languages", domainTableService.getProvidedLanguages().stream()
				.map(l -> new NameId(l.name(), messageSource.getMessage(l.toString(), null, LocaleContextHolder.getLocale())))
				.collect(Collectors.toList()));

		return "domain/add-domain-tables";
	}

	@Secured({"ROLE_TAX_DOMAIN_TABLE_WRITE"})
	@GetMapping("/domaintables/{id}/edit")
	public String showUpdateForm(@PathVariable("id") String id, Model model) {
		DomainTable table = domainTableRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Invalid domain table Id:" + id));
		model.addAttribute("table", table);
		model.addAttribute("languages", domainTableService.getProvidedLanguages().stream()
				.map(l -> new NameId(l.name(), messageSource.getMessage(l.toString(), null, LocaleContextHolder.getLocale())))
				.collect(Collectors.toList()));
		return "domain/update-domain-table";
	}

	@GetMapping("/domaintables/{id}")
	public String showDomainTable(@PathVariable("id") String id, Model model) {
		DomainTable table = domainTableRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Invalid domain table Id:" + id));
		model.addAttribute("table", table);
		model.addAttribute("languages", domainTableService.getProvidedLanguages().stream()
				.map(l -> new NameId(l.name(), messageSource.getMessage(l.toString(), null, LocaleContextHolder.getLocale())))
				.collect(Collectors.toList()));
		return "domain/update-domain-table";
	}

}
