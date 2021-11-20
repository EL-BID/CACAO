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

import static org.idb.cacao.web.utils.ControllerUtils.searchPage;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.sort.SortOrder;
import org.idb.cacao.api.Taxpayer;
import org.idb.cacao.web.controllers.AdvancedSearch;
import org.idb.cacao.web.controllers.services.FieldsConventionsService;
import org.idb.cacao.web.dto.MenuItem;
import org.idb.cacao.web.entities.Interpersonal;
import org.idb.cacao.web.entities.RelationshipType;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.entities.UserProfile;
import org.idb.cacao.web.errors.UserNotFoundException;
import org.idb.cacao.web.repositories.InterpersonalRepository;
import org.idb.cacao.web.repositories.TaxpayerRepository;
import org.idb.cacao.web.repositories.UserRepository;
import org.idb.cacao.web.utils.ControllerUtils;
import org.idb.cacao.web.utils.SearchUtils;
import org.idb.cacao.web.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller class for all endpoints related to 'interpersonal relationship' object interacting by a user interface
 * 
 * @author Gustavo Figueiredo
 *
 */
@Controller
public class InterpersonalUIController {

	private static final Logger log = Logger.getLogger(InterpersonalUIController.class.getName());

    @Autowired
    private MessageSource messages;

	@Autowired
	private Environment env;

	@Autowired
	private InterpersonalRepository interpersonalRepository;
	
	@Autowired
	private TaxpayerRepository taxpayerRepository;
	
	@Autowired
	private FieldsConventionsService fieldsConventionsService;
	
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RestHighLevelClient elasticsearchClient;

	@GetMapping(value= {"/interpersonal","/interpersonal/{type}"})
	public String getInterpersonalRelationships(
			@PathVariable Optional<String> type,
			Model model, 
			@RequestParam("page") Optional<Integer> page, 
			@RequestParam("size") Optional<Integer> size,
			@RequestParam("q") Optional<String> filters_as_json) {
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();

    	Collection<? extends GrantedAuthority> roles = auth.getAuthorities();
    	boolean is_sysadmin_or_master_or_authority = roles.stream().anyMatch(a->UserProfile.SYSADMIN.getRole().equalsIgnoreCase(a.getAuthority()))
    			|| roles.stream().anyMatch(a->UserProfile.MASTER.getRole().equalsIgnoreCase(a.getAuthority()))
    			|| roles.stream().anyMatch(a->UserProfile.AUTHORITY.getRole().equalsIgnoreCase(a.getAuthority()));
    	final String user_taxpayer_id;
    	if (!is_sysadmin_or_master_or_authority) {
    		user_taxpayer_id = userRepository.findById(user.getId()).map(User::getTaxpayerId).orElse(null);
    		if (user_taxpayer_id==null || user_taxpayer_id.trim().length()==0) {
    			model.addAttribute("message",messages.getMessage("user_missing_taxpayerid", null, LocaleContextHolder.getLocale()));
    			model.addAttribute("type", Optional.empty());
    			model.addAttribute("rels", Page.empty());
    			model.addAttribute("filter_options", new AdvancedSearch());
    			model.addAttribute("applied_filters", Optional.empty());
    			return "interpersonal";
    		}
    	}
    	else {
    		user_taxpayer_id = null; // we don't need this for SYSADMIN requests
    	}

		int currentPage = page.orElse(1);
		int pageSize = ControllerUtils.getPageSizeForUser(size, env);
		Optional<AdvancedSearch> filters = SearchUtils.fromJSON(filters_as_json);
		RelationshipType rel_type = null;
		Page<Interpersonal> interpersonal_rels;
		try {
			if (filters.isPresent() && !filters.get().isEmpty()) {
				AdvancedSearch search = filters.get();
				boolean empty_results = false;
				if (type!=null && type.isPresent()) {
					rel_type = RelationshipType.parse(type.get());
					if (rel_type==null) {
						empty_results = true;
					}
					else {
						search = search.clone().withFilter("relationshipType", rel_type.name());
					}					
				}
				if (!is_sysadmin_or_master_or_authority) {
					// We may consider the logged user in both sides of relationship (so we need to combine boolean query 'OR' with boolean query 'AND')
					search = search.clone().withAlternativeFilters("personId1", user_taxpayer_id, "personId2", user_taxpayer_id);
				}
				if (empty_results) {
					interpersonal_rels = Page.empty();
				}
				else {
					interpersonal_rels = SearchUtils.doSearch(search, Interpersonal.class, elasticsearchClient, page, size, Optional.of("timestamp"), Optional.of(SortOrder.DESC));
				}
			}
			else {
				if (type!=null && type.isPresent()) {
					rel_type = RelationshipType.parse(type.get());
					if (rel_type==null) {
						interpersonal_rels = Page.empty();
					}
					else {
						final String REL_TYPE = rel_type.name();
						if (is_sysadmin_or_master_or_authority)
							interpersonal_rels = searchPage(()->interpersonalRepository.findByRelationshipType(REL_TYPE, PageRequest.of(currentPage-1, pageSize, Sort.by("timestamp").descending())));
						else {
							// We may consider the logged user in both sides of relationship (so we need to combine boolean query 'OR' with boolean query 'AND')
							interpersonal_rels = searchPage(()->interpersonalRepository.findByPersonId1OrPersonId2AndRelationshipType(user_taxpayer_id, user_taxpayer_id, REL_TYPE.toLowerCase(), PageRequest.of(currentPage-1, pageSize, Sort.by("timestamp").descending())));
						}
					}
				}
				else {
					if (is_sysadmin_or_master_or_authority)
						interpersonal_rels = searchPage(()->interpersonalRepository.findAll(PageRequest.of(currentPage-1, pageSize, Sort.by("timestamp").descending())));
					else {
						// We may consider the logged user in both sides of relationship (so we need to combine boolean query 'OR' with boolean query 'AND')
						interpersonal_rels = searchPage(()->interpersonalRepository.findByPersonId1OrPersonId2(user_taxpayer_id, user_taxpayer_id, PageRequest.of(currentPage-1, pageSize, Sort.by("timestamp").descending())));
					}
				}
			}
		}
		catch (Exception ex) {
			log.log(Level.SEVERE, "Error while searching for interpersonal relationships", ex);
			interpersonal_rels = Page.empty();
		}
		model.addAttribute("type", Optional.ofNullable(rel_type));
		model.addAttribute("rels", interpersonal_rels);
		int totalPages = interpersonal_rels.getTotalPages();
		if (totalPages > 0) {
			List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages)
					.boxed()
					.collect(Collectors.toList());
			model.addAttribute("pageNumbers", pageNumbers);
		}
		
		AdvancedSearch filter_options = new AdvancedSearch()
		.withFilter(new AdvancedSearch.QueryFilterTerm("personId1").withDisplayName(messages.getMessage("rel.person1", null, LocaleContextHolder.getLocale())))
		.withFilter(new AdvancedSearch.QueryFilterTerm("personId2").withDisplayName(messages.getMessage("rel.person2", null, LocaleContextHolder.getLocale())));
		if (type==null || !type.isPresent())
			filter_options.withFilter(new AdvancedSearch.QueryFilterTerm("relationshipType").withDisplayName(messages.getMessage("rel.type", null, LocaleContextHolder.getLocale())));
		filter_options.withFilter(new AdvancedSearch.QueryFilterTerm("user").withDisplayName(messages.getMessage("rel.user", null, LocaleContextHolder.getLocale())))
		.withFilter(new AdvancedSearch.QueryFilterBoolean("removed").withDisplayName(messages.getMessage("rel.removed", null, LocaleContextHolder.getLocale())))
		.withFilter(new AdvancedSearch.QueryFilterDate("timestamp").withDisplayName(messages.getMessage("rel.timestamp", null, LocaleContextHolder.getLocale())))
		.withFilter(new AdvancedSearch.QueryFilterDate("removedTimestamp").withDisplayName(messages.getMessage("rel.removedTimestamp", null, LocaleContextHolder.getLocale())));
		model.addAttribute("filter_options", filter_options);
		model.addAttribute("applied_filters", filters.map(f->f.withDisplayNames((AdvancedSearch)model.getAttribute("filter_options")).wiredTo(messages)));

        return "interpersonal";
	}

	@Secured({"ROLE_SYSADMIN","ROLE_SUPPORT","ROLE_MASTER","ROLE_AUTHORITY"})
	@GetMapping(value= {"/addinterpersonal","/addinterpersonal/{type}"})
    public String showAddInterpersonalRelationship(@PathVariable Optional<String> type,Interpersonal interpersonal, Model model) {
		RelationshipType rel_type = null;
		if (type!=null && type.isPresent()) {
			rel_type = RelationshipType.parse(type.get());
		}
		model.addAttribute("type", Optional.ofNullable(rel_type));
		Interpersonal new_rel = new Interpersonal();
		new_rel.setRelationshipType(rel_type);
		model.addAttribute("interpersonal",new_rel);
		return "add-interpersonal";
	}

	@GetMapping("/viewinterpersonal/{id}")
    public String showInterpersonalRelationshipDetails(@PathVariable("id") String id, Model model) {
		Interpersonal interpersonal = interpersonalRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid interpersonal Id:" + id));
        model.addAttribute("interpersonal", interpersonal);
        MenuItem interpersonal_details = new MenuItem();
        interpersonal_details.addChild(new MenuItem(messages.getMessage("rel.person1", null, LocaleContextHolder.getLocale())).withChild(interpersonal.getPersonId1()));
        interpersonal_details.addChild(new MenuItem(messages.getMessage("rel.person1.name", null, LocaleContextHolder.getLocale())).withChild(getTaxPayerName(interpersonal.getPersonId1()).orElse(null)));
        if (interpersonal.getRelationshipType()!=null)
        	interpersonal_details.addChild(new MenuItem(messages.getMessage("rel.type", null, LocaleContextHolder.getLocale())).withChild(messages.getMessage(interpersonal.getRelationshipType().toString(), null, LocaleContextHolder.getLocale())));
        interpersonal_details.addChild(new MenuItem(messages.getMessage("rel.person2", null, LocaleContextHolder.getLocale())).withChild(interpersonal.getPersonId2()));
        interpersonal_details.addChild(new MenuItem(messages.getMessage("rel.person2.name", null, LocaleContextHolder.getLocale())).withChild(getTaxPayerName(interpersonal.getPersonId2()).orElse(null)));
        interpersonal_details.addChild(new MenuItem(messages.getMessage("rel.user", null, LocaleContextHolder.getLocale())).withChild(interpersonal.getUser()));
        interpersonal_details.addChild(new MenuItem(messages.getMessage("rel.timestamp", null, LocaleContextHolder.getLocale())).withChild(fieldsConventionsService.formatValue(interpersonal.getTimestamp())));
        interpersonal_details.addChild(new MenuItem(messages.getMessage("rel.removedTimestamp", null, LocaleContextHolder.getLocale())).withChild(fieldsConventionsService.formatValue(interpersonal.getRemovedTimestamp())));
        model.addAttribute("interpersonal_details", interpersonal_details);
        return "view-interpersonal";
    }

	/**
	 * Given taxpayer ID, returns its NAME
	 */
	public Optional<String> getTaxPayerName(String taxPayerId) {
		if (taxPayerId==null || taxPayerId.length()==0)
			return Optional.empty();
		Optional<Taxpayer> taxPayer = taxpayerRepository.findByTaxPayerId(taxPayerId);
		return taxPayer.map(Taxpayer::toString);
	}

}
