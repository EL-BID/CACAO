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
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.sort.SortOrder;
import org.idb.cacao.web.controllers.AdvancedSearch;
import org.idb.cacao.web.controllers.services.FieldsConventionsService;
import org.idb.cacao.web.dto.MenuItem;
import org.idb.cacao.web.entities.Taxpayer;
import org.idb.cacao.web.repositories.TaxpayerRepository;
import org.idb.cacao.web.utils.ControllerUtils;
import org.idb.cacao.web.utils.SearchUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import static org.idb.cacao.web.utils.ControllerUtils.*;

/**
 * Controller class for all endpoints related to 'taxpayer' object interacting by a user interface
 * 
 * @author Gustavo Figueiredo
 *
 */
@Controller
public class TaxpayerUIController {

	private static final Logger log = Logger.getLogger(TaxpayerUIController.class.getName());

    @Autowired
    private MessageSource messages;

	@Autowired
	private Environment env;

	@Autowired
	private TaxpayerRepository taxPayerRepository;
	
	@Autowired
	private FieldsConventionsService fieldsConventionsService;

	@Autowired
	private RestHighLevelClient elasticsearchClient;

	@Secured({"ROLE_SYSADMIN","ROLE_SUPPORT","ROLE_MASTER","ROLE_AUTHORITY"})
	@GetMapping("/taxpayers")
	public String getTaxpayers(Model model, 
			@RequestParam("page") Optional<Integer> page, 
			@RequestParam("size") Optional<Integer> size,
			@RequestParam("q") Optional<String> filters_as_json) {
		int currentPage = page.orElse(1);
		int pageSize = ControllerUtils.getPageSizeForUser(size, env);
		Optional<AdvancedSearch> filters = SearchUtils.fromJSON(filters_as_json);
		Page<Taxpayer> taxpayers;
		try {
			if (filters.isPresent() && !filters.get().isEmpty()) {
				taxpayers = SearchUtils.doSearch(filters.get(), Taxpayer.class, elasticsearchClient, page, size, Optional.of("taxPayerId.keyword"), Optional.of(SortOrder.ASC));
			}
			else {
				taxpayers = searchPage(()->taxPayerRepository.findAll(PageRequest.of(currentPage-1, pageSize, Sort.by("taxPayerId.keyword").ascending())));
			}
		}
		catch (Exception ex) {
			log.log(Level.SEVERE, "Error while searching for taxpayers", ex);
			taxpayers = Page.empty();
		}
		model.addAttribute("taxpayers", taxpayers);
		int totalPages = taxpayers.getTotalPages();
		if (totalPages > 0) {
			List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages)
					.boxed()
					.collect(Collectors.toList());
			model.addAttribute("pageNumbers", pageNumbers);
		}
		
		model.addAttribute("filter_options", new AdvancedSearch()
			.withFilter(new AdvancedSearch.QueryFilterTerm("taxPayerId").withDisplayName(messages.getMessage("taxpayer_id", null, LocaleContextHolder.getLocale())))
			.withFilter(new AdvancedSearch.QueryFilterTerm("name").withDisplayName(messages.getMessage("taxpayer_name", null, LocaleContextHolder.getLocale())))
			.withFilter(new AdvancedSearch.QueryFilterTerm("surname").withDisplayName(messages.getMessage("taxpayer_surname", null, LocaleContextHolder.getLocale())))
			.withFilter(new AdvancedSearch.QueryFilterTerm("address").withDisplayName(messages.getMessage("taxpayer_address", null, LocaleContextHolder.getLocale())))
			.withFilter(new AdvancedSearch.QueryFilterTerm("zipCode").withDisplayName(messages.getMessage("taxpayer_zip", null, LocaleContextHolder.getLocale())))
			.withFilter(new AdvancedSearch.QueryFilterTerm("qualifier1").withDisplayName(messages.getMessage("taxpayer_qualifier1", null, LocaleContextHolder.getLocale())))
			.withFilter(new AdvancedSearch.QueryFilterTerm("qualifier2").withDisplayName(messages.getMessage("taxpayer_qualifier2", null, LocaleContextHolder.getLocale())))
			.withFilter(new AdvancedSearch.QueryFilterTerm("qualifier3").withDisplayName(messages.getMessage("taxpayer_qualifier3", null, LocaleContextHolder.getLocale())))
			.withFilter(new AdvancedSearch.QueryFilterTerm("qualifier4").withDisplayName(messages.getMessage("taxpayer_qualifier4", null, LocaleContextHolder.getLocale())))
			.withFilter(new AdvancedSearch.QueryFilterTerm("qualifier5").withDisplayName(messages.getMessage("taxpayer_qualifier5", null, LocaleContextHolder.getLocale())))
			);
		model.addAttribute("applied_filters", filters.map(f->f.withDisplayNames((AdvancedSearch)model.getAttribute("filter_options")).wiredTo(messages)));

        return "taxpayers";
	}

	@Secured({"ROLE_SYSADMIN","ROLE_SUPPORT","ROLE_MASTER","ROLE_AUTHORITY"})
	@GetMapping("/addtaxpayer")
    public String showAddTaxpayer(Taxpayer taxpayer, Model model) {
		return "add-taxpayer";
	}

	@Secured({"ROLE_SYSADMIN","ROLE_SUPPORT","ROLE_MASTER","ROLE_AUTHORITY"})
    @GetMapping("/edittaxpayer/{id}")
    public String showUpdateForm(@PathVariable("id") String id, Model model) {
		Taxpayer taxpayer = taxPayerRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid taxpayer Id:" + id));
        model.addAttribute("taxpayer", taxpayer);
        return "update-taxpayer";
    }

	@Secured({"ROLE_SYSADMIN","ROLE_SUPPORT","ROLE_MASTER","ROLE_AUTHORITY"})
	@GetMapping("/viewtaxpayer/{id}")
    public String showTaxpayerDetails(@PathVariable("id") String id, Model model) {
		Taxpayer taxpayer = taxPayerRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid taxpayer Id:" + id));
        model.addAttribute("taxpayer", taxpayer);
        MenuItem taxpayer_details = new MenuItem();
        taxpayer_details.addChild(new MenuItem(messages.getMessage("taxpayer_id", null, LocaleContextHolder.getLocale())).withChild(taxpayer.getTaxPayerId()));
        taxpayer_details.addChild(new MenuItem(messages.getMessage("taxpayer_name", null, LocaleContextHolder.getLocale())).withChild(nvl(taxpayer.getName())));
        taxpayer_details.addChild(new MenuItem(messages.getMessage("taxpayer_qualifier1", null, LocaleContextHolder.getLocale())).withChild(nvl(taxpayer.getQualifier1())));
        taxpayer_details.addChild(new MenuItem(messages.getMessage("taxpayer_qualifier2", null, LocaleContextHolder.getLocale())).withChild(nvl(taxpayer.getQualifier2())));
        taxpayer_details.addChild(new MenuItem(messages.getMessage("taxpayer_qualifier3", null, LocaleContextHolder.getLocale())).withChild(nvl(taxpayer.getQualifier3())));
        taxpayer_details.addChild(new MenuItem(messages.getMessage("taxpayer_qualifier4", null, LocaleContextHolder.getLocale())).withChild(nvl(taxpayer.getQualifier4())));
        taxpayer_details.addChild(new MenuItem(messages.getMessage("taxpayer_qualifier5", null, LocaleContextHolder.getLocale())).withChild(nvl(taxpayer.getQualifier5())));
        taxpayer_details.addChild(new MenuItem(messages.getMessage("taxpayer_address", null, LocaleContextHolder.getLocale())).withChild(nvl(taxpayer.getAddress())));
        taxpayer_details.addChild(new MenuItem(messages.getMessage("taxpayer_zip", null, LocaleContextHolder.getLocale())).withChild(nvl(taxpayer.getZipCode())));
        taxpayer_details.addChild(new MenuItem(messages.getMessage("taxpayer_timestamp", null, LocaleContextHolder.getLocale())).withChild(fieldsConventionsService.formatValue(taxpayer.getTimestamp())));
        model.addAttribute("taxpayer_details", taxpayer_details);
        return "view-taxpayer";
    }
	
	public static String nvl(String v) {
		if (v==null)
			return "";
		return v;
	}
}
