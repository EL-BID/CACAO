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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.sort.SortOrder;
import org.idb.cacao.web.controllers.AdvancedSearch;
import org.idb.cacao.web.controllers.services.CommunicationService;
import org.idb.cacao.web.controllers.services.DocumentTemplateService;
import org.idb.cacao.web.controllers.services.FieldsConventionsService;
import org.idb.cacao.web.controllers.services.PrivilegeService;
import org.idb.cacao.web.controllers.services.UserService;
import org.idb.cacao.web.dto.MenuItem;
import org.idb.cacao.web.entities.Communication;
import org.idb.cacao.web.entities.CommunicationType;
import org.idb.cacao.web.entities.SystemPrivilege;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.entities.UserProfile;
import org.idb.cacao.web.errors.InsufficientPrivilege;
import org.idb.cacao.web.repositories.CommunicationRepository;
import org.idb.cacao.web.utils.ControllerUtils;
import org.idb.cacao.web.utils.DateTimeUtils;
import org.idb.cacao.web.utils.ErrorUtils;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller class for all endpoints related to 'Communication' object interacting by a user interface
 * 
 * @author Gustavo Figueiredo
 *
 */
@Controller
public class CommunicationUIController {
	
	private static final Logger log = Logger.getLogger(CommunicationUIController.class.getName());

    @Autowired
    private MessageSource messages;

	@Autowired
	private Environment env;

	@Autowired
	private CommunicationRepository commRepository;

	@Autowired
	private FieldsConventionsService fieldsConventionsService;
	
	@Autowired
	private DocumentTemplateService templateService;
	
	@Autowired
	private RestHighLevelClient elasticsearchClient;

    @Autowired
    private UserService userService;

    @Autowired
    private PrivilegeService privilegeService;

	@GetMapping("/legislation")
	public String getLegislation(Model model) {

    	Page<Communication> all_present_communications = searchPage(()->commRepository.findByEndViewTimestampIsNull(PageRequest.of(0, 10000)));
		
    	final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	final User user = (auth==null) ? null : UserUtils.getUser(auth);

		List<Communication> filtered_communications =
				all_present_communications.getContent()
				.stream()
				.filter(comm->{
					switch (comm.getType()) {
					case LEGISLATION:
						if (comm.getAudience()==null || comm.getAudience().trim().length()==0)
							return true;
						else
							return CommunicationService.isTargetAudience(comm.getAudience(), user);
					default:
						return false;
					}
				})
				.collect(Collectors.toList());
		
		List<MenuItem> ungroupedMenuItens = new LinkedList<>();
		Map<String,MenuItem> groupedMenuItens = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		
		for (Communication comm: filtered_communications) {
			if (comm.getGroup()!=null && comm.getGroup().trim().length()>0) {
				MenuItem submenu = groupedMenuItens.computeIfAbsent(comm.getGroup(), k->new MenuItem(comm.getGroup()).withActive(false));
				submenu.addChild(new MenuItem(comm.getTitle()).withActive(false).withMarkdownChild(comm.getMessage()));
			}
			else {
				ungroupedMenuItens.add(new MenuItem(comm.getTitle()).withActive(false).withMarkdownChild(comm.getMessage()));
			}
		}
		
		List<MenuItem> allMenuItens = new ArrayList<>(ungroupedMenuItens.size()+groupedMenuItens.size());
		allMenuItens.addAll(groupedMenuItens.values());
		allMenuItens.addAll(ungroupedMenuItens);
	
		model.addAttribute("legislations", allMenuItens);
		
    	try {
	    	Set<String> templates = templateService.getSimplePayTemplates();
	        model.addAttribute("has_simplepay_templates", templates!=null && !templates.isEmpty());
    	}
    	catch (Throwable ex) { }

    	ControllerUtils.tagLoggedArea(model);

		return "legislation";
	}
	
	@GetMapping("/instructions")
	public String getInstructions(Model model) {

    	Page<Communication> all_present_communications = searchPage(()->commRepository.findByEndViewTimestampIsNull(PageRequest.of(0, 10000)));
		
    	final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	final User user = (auth==null) ? null : UserUtils.getUser(auth);

		List<Communication> filtered_communications =
				all_present_communications.getContent()
				.stream()
				.filter(comm->{
					switch (comm.getType()) {
					case INSTRUCTIONS:
						if (comm.getAudience()==null || comm.getAudience().trim().length()==0)
							return true;
						else
							return CommunicationService.isTargetAudience(comm.getAudience(), user);
					default:
						return false;
					}
				})
				.collect(Collectors.toList());
	
		model.addAttribute("instructions", filtered_communications);
		
    	try {
	    	Set<String> templates = templateService.getSimplePayTemplates();
	        model.addAttribute("has_simplepay_templates", templates!=null && !templates.isEmpty());
    	}
    	catch (Throwable ex) { }

    	ControllerUtils.tagLoggedArea(model);

		return "instructions";
	}
		
	@GetMapping("/bulletin_board")
	public String getBulletinBoard(Model model) {

    	final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	final User user = (auth==null) ? null : UserUtils.getUser(auth);

		Page<Communication> all_present_communications;
		
		try {
			all_present_communications = commRepository.findByEndViewTimestampIsNull(PageRequest.of(0, 10000, Sort.by("createTimestamp").descending()));
		} catch (Throwable ex) {
			if (!ErrorUtils.isErrorNoIndexFound(ex)) {
				log.log(Level.SEVERE, "Error while fetching bulletin board", ex);
			}
			all_present_communications = Page.empty();
		}
		
		List<Communication> filtered_communications =
				all_present_communications.getContent()
				.stream()
				.filter(comm->{
					switch (comm.getType()) {
					case OVERALL:
						return true;
					case PRIVATE:
						return CommunicationService.isTargetAudience(comm.getAudience(), user);
					default:
						return false;
					}
				})
				.sorted(new Comparator<Communication>() {
					@Override
					public int compare(Communication o1, Communication o2) {
						// Give priority to PRIVATE messages, than OVERALL messages
						CommunicationType ct1 = o1.getType();
						CommunicationType ct2 = o2.getType();
						if (ct1!=ct2 && ct1!=null && ct2!=null) {
							boolean ct1_is_priv = (CommunicationType.PRIVATE.equals(ct1));
							boolean ct2_is_priv = (CommunicationType.PRIVATE.equals(ct2));
							if (ct1_is_priv && !ct2_is_priv)
								return -1;
							if (!ct1_is_priv && ct2_is_priv)
								return 1;
						}
						// Order by create timestamp in descending order
						OffsetDateTime dt1 = o1.getCreateTimestamp();
						OffsetDateTime dt2 = o2.getCreateTimestamp();
						if (dt1!=dt2) {
							if (dt1==null)	return -1;
							if (dt2==null)  return 1;
							int comp = dt1.compareTo(dt2);
							if (comp!=0)
								return -comp; // descending order = inverse of natural order
						}
						return 0;
					}					
				})
				.collect(Collectors.toList());
		
		// Mark any private unseen messages so far
		for (Communication comm:filtered_communications) {
			if (CommunicationType.PRIVATE.equals(comm.getType())
				&& comm.getViewTimestamp()==null) {
				comm.setViewTimestamp(DateTimeUtils.now());
				comm.setViewUser(user.getName());
				commRepository.saveWithTimestamp(comm);
			}
		}
		
		model.addAttribute("communications", filtered_communications);

		return "bulletin_board";
	}

	@Secured({"ROLE_COMMUNICATION_READ"})
	@GetMapping(value= {"/communications","/communications/{type}"})
	public String getCommunications(
			@PathVariable Optional<String> type,
			Model model, 
			@RequestParam("page") Optional<Integer> page, 
			@RequestParam("size") Optional<Integer> size,
			@RequestParam("q") Optional<String> filters_as_json) {
		int currentPage = page.orElse(1);
		int pageSize = ControllerUtils.getPageSizeForUser(size, env);
		Optional<AdvancedSearch> filters = SearchUtils.fromJSON(filters_as_json);
		Page<Communication> communications;
		CommunicationType comm_type = null;
		try {
			if (type!=null && type.isPresent()) {
				comm_type = CommunicationType.parse(type.get());
				if (comm_type==null) {
					communications = Page.empty();
				}
				else {
					final String COMM_TYPE = comm_type.name();
					if (filters.isPresent() && !filters.get().isEmpty()) {
						AdvancedSearch searchable = filters.get().clone();
						searchable.addFilter("type", COMM_TYPE);
						communications = SearchUtils.doSearch(searchable, Communication.class, elasticsearchClient, page, size, Optional.of("createTimestamp"), Optional.of(SortOrder.ASC));
					}
					else {
						communications = searchPage(()->commRepository.findByType(COMM_TYPE, PageRequest.of(currentPage-1, pageSize, Sort.by("createTimestamp").descending())));
					}
				}
			}
			else {
				if (filters.isPresent() && !filters.get().isEmpty()) {
					communications = SearchUtils.doSearch(filters.get(), Communication.class, elasticsearchClient, page, size, Optional.of("createTimestamp"), Optional.of(SortOrder.ASC));
				}
				else {
					communications = searchPage(()->commRepository.findAll(PageRequest.of(currentPage-1, pageSize, Sort.by("createTimestamp").descending())));
				}
			}
		}
		catch (Exception ex) {
			log.log(Level.SEVERE, "Error while searching for communications", ex);
			communications = Page.empty();
		}
		model.addAttribute("communications", communications);
		model.addAttribute("type", Optional.ofNullable(comm_type));
		int totalPages = communications.getTotalPages();
		if (totalPages > 0) {
			List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages)
					.boxed()
					.collect(Collectors.toList());
			model.addAttribute("pageNumbers", pageNumbers);
		}
		
		AdvancedSearch filter_options = new AdvancedSearch()
			.withFilter(new AdvancedSearch.QueryFilterTerm("title").withDisplayName(messages.getMessage("comm_msg_title", null, LocaleContextHolder.getLocale())))
			.withFilter(new AdvancedSearch.QueryFilterTerm("message").withDisplayName(messages.getMessage("comm_message", null, LocaleContextHolder.getLocale())))
			.withFilter(new AdvancedSearch.QueryFilterTerm("createUser").withDisplayName(messages.getMessage("comm_create_user", null, LocaleContextHolder.getLocale())))
			.withFilter(new AdvancedSearch.QueryFilterDate("createTimestamp").withDisplayName(messages.getMessage("comm_create_time", null, LocaleContextHolder.getLocale())));
		if (comm_type!=null && comm_type.getAudience()) {
			filter_options
			.withFilter(new AdvancedSearch.QueryFilterTerm("audience").withDisplayName(messages.getMessage("comm_audience", null, LocaleContextHolder.getLocale())));
		}
		if (comm_type!=null && comm_type.getGroup()) {
			filter_options
			.withFilter(new AdvancedSearch.QueryFilterTerm("group").withDisplayName(messages.getMessage("comm_group", null, LocaleContextHolder.getLocale())));
		}
		if (comm_type!=null && comm_type.getTaxpayer()) {
			filter_options
			.withFilter(new AdvancedSearch.QueryFilterTerm("taxPayerId").withDisplayName(messages.getMessage("taxpayer_id", null, LocaleContextHolder.getLocale())))
			.withFilter(new AdvancedSearch.QueryFilterTerm("taxPayerName").withDisplayName(messages.getMessage("taxpayer_name", null, LocaleContextHolder.getLocale())));
		}
		model.addAttribute("filter_options", filter_options);
		model.addAttribute("applied_filters", filters.map(f->f.withDisplayNames((AdvancedSearch)model.getAttribute("filter_options")).wiredTo(messages)));
		
        return "communications";
	}

	@Secured({"ROLE_COMMUNICATION_WRITE","ROLE_COMMUNICATION_WRITE_PRIVATE"})
	@GetMapping(value={"/addcommunication","/addcommunication/{type}"})
    public String showAddCommunication(
			@PathVariable Optional<String> type,
    		Model model) {
		
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new InsufficientPrivilege(messages.getMessage("error.notAuthenticated", null, LocaleContextHolder.getLocale()));
    	User user = userService.getUser(auth);
    	if (user==null)
    		throw new InsufficientPrivilege(messages.getMessage("error.notAuthenticated", null, LocaleContextHolder.getLocale()));

		CommunicationType comm_type = null;
		if (type!=null && type.isPresent()) {
			comm_type = CommunicationType.parse(type.get());
		}
		
    	if (CommunicationType.PRIVATE.equals(comm_type)) {
    		// private communication...
    		if (!privilegeService.hasPrivilege(user.getProfile(), SystemPrivilege.COMMUNICATION_WRITE_PRIVATE)) {
    			throw new InsufficientPrivilege(messages.getMessage("error.accessDenied", null, LocaleContextHolder.getLocale()));
    		}
    	}
    	else {
    		// non private communication...
    		if (!privilegeService.hasPrivilege(user.getProfile(), SystemPrivilege.COMMUNICATION_WRITE)) {
    			throw new InsufficientPrivilege(messages.getMessage("error.accessDenied", null, LocaleContextHolder.getLocale()));    			
    		}
    	}

		model.addAttribute("type", Optional.ofNullable(comm_type));
		Communication new_comm = new Communication();
		new_comm.setType(comm_type);
		model.addAttribute("communication",new_comm);
		return "add-communication";
	}

	@Secured({"ROLE_COMMUNICATION_WRITE","ROLE_COMMUNICATION_WRITE_PRIVATE"})
    @GetMapping("/editcommunication/{id}")
    public String showUpdateForm(@PathVariable("id") String id, Model model) {
		
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new InsufficientPrivilege(messages.getMessage("error.notAuthenticated", null, LocaleContextHolder.getLocale()));
    	User user = userService.getUser(auth);
    	if (user==null)
    		throw new InsufficientPrivilege(messages.getMessage("error.notAuthenticated", null, LocaleContextHolder.getLocale()));

		Communication comm = commRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid communication Id:" + id));
		
    	if (CommunicationType.PRIVATE.equals(comm.getType())) {
    		// private communication...
    		if (!privilegeService.hasPrivilege(user.getProfile(), SystemPrivilege.COMMUNICATION_WRITE_PRIVATE)) {
    			throw new InsufficientPrivilege(messages.getMessage("error.accessDenied", null, LocaleContextHolder.getLocale()));
    		}
    	}
    	else {
    		// non private communication...
    		if (!privilegeService.hasPrivilege(user.getProfile(), SystemPrivilege.COMMUNICATION_WRITE)) {
    			throw new InsufficientPrivilege(messages.getMessage("error.accessDenied", null, LocaleContextHolder.getLocale()));    			
    		}
    	}

		formatAudience(comm);
        model.addAttribute("communication", comm);
        return "update-communication";
    }
	
	@Secured({"ROLE_COMMUNICATION_READ"})
	@GetMapping("/viewcommunication/{id}")
    public String showCommunicationDetails(@PathVariable("id") String id, Model model) {
		Communication comm = commRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid communication Id:" + id));
		formatAudience(comm);
        model.addAttribute("communication", comm);
        MenuItem taxpayer_details = new MenuItem();
        taxpayer_details.addChild(new MenuItem(messages.getMessage("comm_msg_title", null, LocaleContextHolder.getLocale())).withChild(fieldsConventionsService.formatValue(comm.getTitle())));
        taxpayer_details.addChild(new MenuItem(messages.getMessage("comm_type", null, LocaleContextHolder.getLocale())).withChild(messages.getMessage(comm.getType().toString(), null, LocaleContextHolder.getLocale())));
        if (comm.getAudience()!=null && comm.getAudience().trim().length()>0)
        	taxpayer_details.addChild(new MenuItem(messages.getMessage("comm_audience", null, LocaleContextHolder.getLocale())).withChild(comm.getAudience()));
        if (comm.getGroup()!=null && comm.getGroup().trim().length()>0)
        	taxpayer_details.addChild(new MenuItem(messages.getMessage("comm_group", null, LocaleContextHolder.getLocale())).withChild(comm.getGroup()));
        if (comm.getTaxPayerId()!=null && comm.getTaxPayerId().trim().length()>0)
        	taxpayer_details.addChild(new MenuItem(messages.getMessage("taxpayer_id", null, LocaleContextHolder.getLocale())).withChild(comm.getTaxPayerId()));
        if (comm.getTaxPayerName()!=null && comm.getTaxPayerName().trim().length()>0)
        	taxpayer_details.addChild(new MenuItem(messages.getMessage("taxpayer_name", null, LocaleContextHolder.getLocale())).withChild(comm.getTaxPayerName()));
        taxpayer_details.addChild(new MenuItem(messages.getMessage("comm_create_time", null, LocaleContextHolder.getLocale())).withChild(fieldsConventionsService.formatValue(comm.getCreateTimestamp())));
        taxpayer_details.addChild(new MenuItem(messages.getMessage("comm_create_user", null, LocaleContextHolder.getLocale())).withChild(fieldsConventionsService.formatValue(comm.getCreateUser())));
        taxpayer_details.addChild(new MenuItem(messages.getMessage("comm_update_time", null, LocaleContextHolder.getLocale())).withChild(fieldsConventionsService.formatValue(comm.getUpdateTimestamp())));
        taxpayer_details.addChild(new MenuItem(messages.getMessage("comm_update_user", null, LocaleContextHolder.getLocale())).withChild(fieldsConventionsService.formatValue(comm.getUpdateUser())));
        taxpayer_details.addChild(new MenuItem(messages.getMessage("comm_endview_time", null, LocaleContextHolder.getLocale())).withChild(fieldsConventionsService.formatValue(comm.getEndViewTimestamp())));
        taxpayer_details.addChild(new MenuItem(messages.getMessage("comm_view_time", null, LocaleContextHolder.getLocale())).withChild(fieldsConventionsService.formatValue(comm.getViewTimestamp())));
        taxpayer_details.addChild(new MenuItem(messages.getMessage("comm_view_user", null, LocaleContextHolder.getLocale())).withChild(fieldsConventionsService.formatValue(comm.getViewUser())));
        taxpayer_details.addChild(new MenuItem(messages.getMessage("comm_view_ipaddr", null, LocaleContextHolder.getLocale())).withChild(fieldsConventionsService.formatValue(comm.getViewIpAddress())));
        taxpayer_details.addChild(new MenuItem(messages.getMessage("comm_message", null, LocaleContextHolder.getLocale())).withMarkdownChild(fieldsConventionsService.formatValue(comm.getMessage())));
        taxpayer_details.addChild(new MenuItem(messages.getMessage("comm_message_telegram", null, LocaleContextHolder.getLocale())).withChild(fieldsConventionsService.formatValue(comm.getTelegramMessage()))
        		.treatNewLinesAndSpaces(true)
        		.chkForHTMLContents(true));
        taxpayer_details.addChild(new MenuItem(messages.getMessage("comm_message_email", null, LocaleContextHolder.getLocale())).withChild(fieldsConventionsService.formatValue(comm.getEmailMessage()))
        		.treatNewLinesAndSpaces(true)
        		.chkForHTMLContents(true));
        model.addAttribute("communication_details", taxpayer_details);
        return "view-communication";
    }

	/**
	 * If the communication object contains an 'audience', verifies if this audience contains a 'constant name' for a UserProfile. If positive,
	 * replaces this with the corresponding 'localized text' in order to present to the user interface.<BR>
	 * If the communication object does not contain an 'audience', or if the 'audience' does not correspond to any 'constant name' for a UserProfile,
	 * this method does nothing.<BR>
	 * This method is dual to the 'CommunicationAPIController'.treatAudience' method.
	 */
	public void formatAudience(Communication comm) {
		if (comm==null || comm.getAudience()==null || comm.getAudience().trim().length()==0)
			return;
		String audience = comm.getAudience();
		if (audience.contains(",")) {
			// Build a map of UserProfile's constant names and corresponding localized messages
			Map<String,String> map_profiles_names = 
					Arrays.stream(UserProfile.values())
					.map(pr->new String[] {pr.name(), messages.getMessage(pr.toString(), null, LocaleContextHolder.getLocale())})
					.filter(pr->pr[1]!=null)
					.collect(Collectors.toMap(
						/*keyMapper*/pr->pr[0], 
						/*valueMapper*/pr->pr[1], 
						/*mergeFunction*/(a,b)->a, 
						/*mapSupplier*/()->new TreeMap<>(String.CASE_INSENSITIVE_ORDER)));
			// Check for the presence of any of these UserProfile's localized messages
			String[] multiple_audience = audience.split(",");
			boolean changed = false;
			for (int i=0; i<multiple_audience.length; i++) {
				String part_audience = multiple_audience[i].trim();
				String mapped_profile_name = map_profiles_names.get(part_audience);
				if (mapped_profile_name!=null) {
					multiple_audience[i] = mapped_profile_name;
					changed = true;
				}
			}
			if (changed) {
				String merged_audience = String.join(",", multiple_audience);
				comm.setAudience(merged_audience);
			}
		}
		else {
			for (UserProfile profile: UserProfile.values()) {
				if (profile.name().equalsIgnoreCase(audience)) {
					String profile_name = messages.getMessage(profile.toString(), null, LocaleContextHolder.getLocale());
					if (profile_name==null || profile_name.length()==0)
						continue;
					comm.setAudience(profile_name);
					break;
				}
			}			
		}
	}

}
