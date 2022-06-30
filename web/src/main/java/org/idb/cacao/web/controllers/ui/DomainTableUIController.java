/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.ui;

import java.util.stream.Collectors;

import org.idb.cacao.api.templates.DomainTable;
import org.idb.cacao.web.controllers.services.DomainTableService;
import org.idb.cacao.web.dto.NameId;
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
		return "domain/domain_tables";
	}

	@Secured({"ROLE_TAX_DOMAIN_TABLE_WRITE"})
	@GetMapping("/adddomaintable")
	public String showAddDomainTable(Model model) {
		model.addAttribute("table", new DomainTable());
		model.addAttribute("languages", domainTableService.getProvidedLanguages().stream()
				.map(l -> new NameId(l.name(), messageSource.getMessage(l.toString(), null, LocaleContextHolder.getLocale())))
				.collect(Collectors.toList()));

		return "domain/add_domain_tables";
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
		return "domain/update_domain_table";
	}

	@GetMapping("/domaintables/{id}")
	public String showDomainTable(@PathVariable("id") String id, Model model) {
		DomainTable table = domainTableRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Invalid domain table Id:" + id));
		model.addAttribute("table", table);
		model.addAttribute("languages", domainTableService.getProvidedLanguages().stream()
				.map(l -> new NameId(l.name(), messageSource.getMessage(l.toString(), null, LocaleContextHolder.getLocale())))
				.collect(Collectors.toList()));
		return "domain/show_domain_table";
	}

}
