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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.idb.cacao.web.controllers.AdvancedSearch;
import org.idb.cacao.web.controllers.services.DocumentTemplateService;
import org.idb.cacao.web.controllers.services.IConfigEMailService;
import org.idb.cacao.web.controllers.services.UserService;
import org.idb.cacao.web.dto.MenuItem;
import org.idb.cacao.web.entities.CommunicationType;
import org.idb.cacao.web.entities.ConfigEMail;
import org.idb.cacao.web.entities.RelationshipType;
import org.idb.cacao.web.entities.SystemPrivilege;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.repositories.UserRepository;
import org.idb.cacao.web.utils.ControllerUtils;
import org.idb.cacao.web.utils.LoginUtils;
import org.idb.cacao.web.utils.SearchUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller class for all endpoints related to 'user' object interacting by a
 * user interface
 * 
 * @author Gustavo Figueiredo
 *
 */
@Controller
public class UserUIController {

	@Autowired
	private MessageSource messages;

	@Autowired
	private Environment env;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private IConfigEMailService configEmailService;

	@Autowired
	private UserService userService;

	@Autowired
	private DocumentTemplateService templateService;

	private static final Pattern pShowSlip = Pattern.compile("/(?>slips|simplepay)/([\\d]+)$");
	private static final Pattern pShowSlipWithTwoParts = Pattern.compile("/(?>slips|simplepay)/([\\d]+)/([\\d]+)$");

	@GetMapping("/login")
	public String showLogin(Model model) {

		// Enable/disable some login options based on available configuration
		model.addAttribute("has_login_google", LoginUtils.hasGoogleProvider(env));
		model.addAttribute("has_login_azure", LoginUtils.hasAzureProvider(env));
		model.addAttribute("has_login_facebook", LoginUtils.hasFacebookProvider(env));

		try {
			Set<String> templates = templateService.getSimplePayTemplates();
			model.addAttribute("has_simplepay_templates", templates != null && !templates.isEmpty());
		} catch (Throwable ex) {
		}

		return "login";
	}

	@GetMapping("/forgetPassword")
	public String showForgetPassword(Model model) {

		try {
			Set<String> templates = templateService.getSimplePayTemplates();
			model.addAttribute("has_simplepay_templates", templates != null && !templates.isEmpty());
		} catch (Throwable ex) {
		}

		return "forgetPassword";
	}

	@GetMapping("/privacy")
	public String showPrivacy(Model model) {

		try {
			Set<String> templates = templateService.getSimplePayTemplates();
			model.addAttribute("has_simplepay_templates", templates != null && !templates.isEmpty());
		} catch (Throwable ex) {
		}

		return "privacy";
	}

	@GetMapping("/license")
	public String showLicense(Model model) {

		return "license";
	}

	@GetMapping("/institutional")
	public String showInstitutional(Model model) {

		try {
			Set<String> templates = templateService.getSimplePayTemplates();
			model.addAttribute("has_simplepay_templates", templates != null && !templates.isEmpty());
		} catch (Throwable ex) {
		}

		ControllerUtils.tagLoggedArea(model);

		return "institutional";
	}

	@GetMapping("/terms")
	public String showTerms(Model model) {

		try {
			Set<String> templates = templateService.getSimplePayTemplates();
			model.addAttribute("has_simplepay_templates", templates != null && !templates.isEmpty());
		} catch (Throwable ex) {
		}

		return "terms";
	}

	@GetMapping("/home")
	public String showHome(Model model, HttpServletRequest request) {
		model.addAttribute("menu", getHomeMenuItens());
		
		String first_page_child_frame = "/bulletin_board";

		// If we were requested to display information about a payment slip (only for showing information), we may pass along additional parameter
		// for this purpose.
		HttpSession session = (request==null) ? null : request.getSession();
		Object saved_request = (session==null) ? null : session.getAttribute("SPRING_SECURITY_SAVED_REQUEST");
		if (saved_request instanceof SavedRequest) {
			String uri = ((SavedRequest)saved_request).getRedirectUrl();
			if (uri!=null) {
				Matcher m = pShowSlip.matcher(uri);
				if (m.find()) {
					String slip_number = m.group(1);
					first_page_child_frame = "/slips/" + slip_number;
				}
				else {
					m = pShowSlipWithTwoParts.matcher(uri);
					if (m.find()) {
						String slip_number = m.group(1) + m.group(2);
						first_page_child_frame = "/slips/" + slip_number;
					}
				}
			}
		}

		try {
			Set<String> templates = templateService.getSimplePayTemplates();
			model.addAttribute("has_simplepay_templates", templates != null && !templates.isEmpty());
		} catch (Throwable ex) {
		}
		
		model.addAttribute("first_page_child_frame", first_page_child_frame);

		return "home";
	}

	@Secured({ "ROLE_USER_READ" })
	@GetMapping("/users")
	@Transactional
	public String getUsers(Model model, @RequestParam("page") Optional<Integer> page,
			@RequestParam("size") Optional<Integer> size, @RequestParam("q") Optional<String> filters_as_json) {
		int currentPage = page.orElse(1);
		int pageSize = ControllerUtils.getPageSizeForUser(size, env);
		Optional<AdvancedSearch> filters = SearchUtils.fromJSON(filters_as_json);
		Page<User> users;
		if (filters.isPresent() && !filters.get().isEmpty()) {
			users = userService.searchUsers(filters, page, size);
		} else {
			users = searchPage(() -> userRepository
					.findAll(PageRequest.of(currentPage - 1, pageSize, Sort.by("name").ascending())));
		}
		model.addAttribute("users", users);
		int totalPages = users.getTotalPages();
		if (totalPages > 0) {
			List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages).boxed().collect(Collectors.toList());
			model.addAttribute("pageNumbers", pageNumbers);
		}

		model.addAttribute("filter_options", new AdvancedSearch()
				.withFilter(new AdvancedSearch.QueryFilterTerm("taxpayerId")
						.withDisplayName(messages.getMessage("taxpayer_id", null, LocaleContextHolder.getLocale())))
				.withFilter(new AdvancedSearch.QueryFilterTerm("name")
						.withDisplayName(messages.getMessage("user_name", null, LocaleContextHolder.getLocale())))
				.withFilter(new AdvancedSearch.QueryFilterTerm("login")
						.withDisplayName(messages.getMessage("user_login", null, LocaleContextHolder.getLocale())))
				.withFilter(new AdvancedSearch.QueryFilterTerm("profile")
						.withDisplayName(messages.getMessage("user_profile", null, LocaleContextHolder.getLocale()))));
		model.addAttribute("applied_filters", filters
				.map(f -> f.withDisplayNames((AdvancedSearch) model.getAttribute("filter_options")).wiredTo(messages)));

		return "users";
	}

	@Secured({ "ROLE_USER_WRITE" })
	@GetMapping("/adduser")
	public String showAddUser(User user, Model model) {
		ConfigEMail config_email = configEmailService.getActiveConfig();
		if (config_email != null && config_email.getSupportEmail() != null
				&& config_email.getSupportEmail().trim().length() > 0) {
			model.addAttribute("omit_password", true);
		}
		return "add-user";
	}

	@Secured({ "ROLE_USER_WRITE" })
	@GetMapping("/edituser/{id}")
	public String showUpdateForm(@PathVariable("id") String id, Model model) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + id));
		model.addAttribute("user", user);
		return "update-user";
	}

	public List<MenuItem> getHomeMenuItens() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null)
			return Collections.emptyList();
		if (auth instanceof AnonymousAuthenticationToken)
			return Collections.emptyList();
		Collection<? extends GrantedAuthority> roles = auth.getAuthorities();
		return getHomeMenuItens(roles);
	}
	
	public static boolean hasPrivilege(Collection<? extends GrantedAuthority> roles, SystemPrivilege privilege) {
		return roles.stream()
				.anyMatch(a -> privilege.getRole().equalsIgnoreCase(a.getAuthority()));
	}

	public List<MenuItem> getHomeMenuItens(Collection<? extends GrantedAuthority> roles) {
		if (roles == null || roles.isEmpty())
			return Collections.emptyList();

		List<MenuItem> menu = new LinkedList<>();

		menu.add(new MenuItem(messages.getMessage("comm_main_page", null, LocaleContextHolder.getLocale()),
				"/bulletin_board"));

		if (hasPrivilege(roles, SystemPrivilege.COMMUNICATION_WRITE)
				|| hasPrivilege(roles, SystemPrivilege.COMMUNICATION_WRITE_PRIVATE)) {
			
			MenuItem submenu = new MenuItem(messages.getMessage("comm_title", null, LocaleContextHolder.getLocale()));
			
			if (hasPrivilege(roles, SystemPrivilege.COMMUNICATION_UPLOAD)) {
				submenu.withChild(new MenuItem(messages.getMessage("generic_files", null, LocaleContextHolder.getLocale()),
					"/generic_files"));
			}

			for (CommunicationType type : CommunicationType.values()) {
				submenu.withChild(
						new MenuItem(messages.getMessage(type.toString(), null, LocaleContextHolder.getLocale()),
								"/communications/" + type.name().toLowerCase()));
			}
			menu.add(submenu);
		}

		if (hasPrivilege(roles, SystemPrivilege.TAX_DECLARATION_WRITE)) {
			menu.add(new MenuItem(messages.getMessage("docs_main_upload", null, LocaleContextHolder.getLocale()), "/docs"));
		}

		if (hasPrivilege(roles, SystemPrivilege.TAX_DECLARATION_READ_ALL)) {
			menu.add(new MenuItem(messages.getMessage("docs_all_history", null, LocaleContextHolder.getLocale()),
					"/docs_search"));
		} else if (hasPrivilege(roles, SystemPrivilege.TAX_DECLARATION_READ)) {
			
			// Deprecated: does not show 'Self uploaded documents' because:
			// a) This kind of search is already satisfied by the other one, more complete
			// b) This kind of search may incur in some problems if the same person has different e-mail accounts registered
			// in the system
			//menu.add(new MenuItem(messages.getMessage("docs_self_history", null, LocaleContextHolder.getLocale()),
			//		"/docs_self_history"));
			
			// Will only show 'Taxpayers uploaded documents'
			// if the user is not seeing 'All uploaded documents'.
			menu.add(new MenuItem(messages.getMessage("docs_taxpayer_history", null, LocaleContextHolder.getLocale()),
					"/docs_taxpayer"));

		}

		if (hasPrivilege(roles, SystemPrivilege.PAYMENT_SLIP_READ)) {
			menu.add(new MenuItem(messages.getMessage("slips_all_history", null, LocaleContextHolder.getLocale()),
					"/slips_search"));
		}
		
		if (hasPrivilege(roles, SystemPrivilege.REPORT_ALL_PAYMENTS_READ)
			|| hasPrivilege(roles, SystemPrivilege.REPORT_MISSING_DECLARATIONS_READ)
			|| hasPrivilege(roles, SystemPrivilege.REPORT_MISSING_PAYMENTS_READ)) {

			MenuItem submenu_management_reports = new MenuItem(
					messages.getMessage("management_reports", null, LocaleContextHolder.getLocale()));
			
			if (hasPrivilege(roles, SystemPrivilege.REPORT_MISSING_DECLARATIONS_READ))
				submenu_management_reports.withChild(
						new MenuItem(messages.getMessage("missing_declarations", null, LocaleContextHolder.getLocale()),
								"/taxpayers/missing_uploads"));
			
			if (hasPrivilege(roles, SystemPrivilege.REPORT_MISSING_PAYMENTS_READ))
				submenu_management_reports.withChild(new MenuItem(
						messages.getMessage("missing_payment_confirmations", null, LocaleContextHolder.getLocale()),
						"/taxpayers/missing_payment_confirmations"));
			
			if (hasPrivilege(roles, SystemPrivilege.REPORT_ALL_PAYMENTS_READ)) {
				submenu_management_reports.withChild(
						new MenuItem(messages.getMessage("summary_tax_paid_monthly", null, LocaleContextHolder.getLocale()),
								"/taxes/months"));
				submenu_management_reports.withChild(
						new MenuItem(messages.getMessage("summary_tax_paid_semiannually", null, LocaleContextHolder.getLocale()),
								"/taxes/semesters"));
				submenu_management_reports.withChild(
						new MenuItem(messages.getMessage("summary_tax_paid_yearly", null, LocaleContextHolder.getLocale()),
								"/taxes/years"));
				submenu_management_reports.withChild(
						new MenuItem(messages.getMessage("summary_tax_paid", null, LocaleContextHolder.getLocale()),
								"/taxes/months_paid"));
			}
			
			menu.add(submenu_management_reports);
		}

		if (hasPrivilege(roles, SystemPrivilege.USER_HISTORY_READ)
				|| hasPrivilege(roles, SystemPrivilege.USER_READ)
				|| hasPrivilege(roles, SystemPrivilege.USER_RECENT_READ)) {
			MenuItem submenu_usersAndTaxpayers = new MenuItem(
					messages.getMessage("users_title", null, LocaleContextHolder.getLocale()));
			
			if (hasPrivilege(roles, SystemPrivilege.USER_RECENT_READ))
				submenu_usersAndTaxpayers.withChild(new MenuItem(
						messages.getMessage("recent_users", null, LocaleContextHolder.getLocale()), "/recent_users"));
			
			if (hasPrivilege(roles, SystemPrivilege.USER_READ))
				submenu_usersAndTaxpayers.withChild(
					new MenuItem(messages.getMessage("users_title", null, LocaleContextHolder.getLocale()), "/users"));
			
			if (hasPrivilege(roles, SystemPrivilege.USER_HISTORY_READ))
				submenu_usersAndTaxpayers.withChild(new MenuItem(
					messages.getMessage("access_history", null, LocaleContextHolder.getLocale()), "/access_history"));
			
			menu.add(submenu_usersAndTaxpayers);
		}

		if (hasPrivilege(roles, SystemPrivilege.TAXPAYER_READ))
			menu.add(new MenuItem(messages.getMessage("taxpayers_title", null, LocaleContextHolder.getLocale()),
					"/taxpayers"));

		if (hasPrivilege(roles, SystemPrivilege.TAX_CREDITS_READ)
				|| hasPrivilege(roles, SystemPrivilege.TAX_DECLARATION_WRITE_EMPTY)) {
			
			MenuItem taxAdministration_submenu = new MenuItem(messages.getMessage("tax_admin_menu", null, LocaleContextHolder.getLocale()));

			if (hasPrivilege(roles, SystemPrivilege.TAX_CREDITS_READ))
				taxAdministration_submenu.withChild(new MenuItem(messages.getMessage("taxcredits_title", null, LocaleContextHolder.getLocale()),
					"/taxcredit/current"));

			if (hasPrivilege(roles, SystemPrivilege.TAX_DECLARATION_WRITE_EMPTY))
				taxAdministration_submenu.withChild(new MenuItem(messages.getMessage("add_emptydoc", null, LocaleContextHolder.getLocale()),
					"/taxadmin/emptydoc"));
			
			menu.add(taxAdministration_submenu);
		}

		MenuItem submenu_interpersonal = new MenuItem(
				messages.getMessage("interpersonal_relationships", null, LocaleContextHolder.getLocale()));
		for (RelationshipType type : RelationshipType.relationshipsForDeclarants) {
			submenu_interpersonal
					.withChild(new MenuItem(messages.getMessage(type.toString(), null, LocaleContextHolder.getLocale()),
							"/interpersonal/" + type.name().toLowerCase()));
		}
		if (hasPrivilege(roles, SystemPrivilege.INTERPERSONAL_READ_ALL)) {
			for (RelationshipType type : RelationshipType.relationshipsForAuthorities) {
				submenu_interpersonal.withChild(
						new MenuItem(messages.getMessage(type.toString(), null, LocaleContextHolder.getLocale()),
								"/interpersonal/" + type.name().toLowerCase()));
			}
		}
		menu.add(submenu_interpersonal);

		if(hasPrivilege(roles, SystemPrivilege.PAYEE_READ)
			|| hasPrivilege(roles, SystemPrivilege.TAX_TEMPLATE_WRITE)
			|| hasPrivilege(roles, SystemPrivilege.PAYMENT_ADDITION_READ)) {
			MenuItem taxConfig_submenu = new MenuItem(messages.getMessage("tax_menu", null, LocaleContextHolder.getLocale()));
			
			if (hasPrivilege(roles, SystemPrivilege.PAYEE_READ))
				taxConfig_submenu.withChild(new MenuItem(messages.getMessage("payees_title", null, LocaleContextHolder.getLocale()),"/payees"));
			
			if (hasPrivilege(roles, SystemPrivilege.TAX_TEMPLATE_WRITE))
				taxConfig_submenu.withChild(new MenuItem(messages.getMessage("templates_title", null, LocaleContextHolder.getLocale()),"/templates"));
			
			if (hasPrivilege(roles, SystemPrivilege.PAYMENT_ADDITION_READ))
				taxConfig_submenu.withChild(new MenuItem(messages.getMessage("config_payment_additions", null, LocaleContextHolder.getLocale()),"/additions"));
			
			menu.add(taxConfig_submenu);
			
		}
		


		MenuItem submenu = new MenuItem(messages.getMessage("config_menu", null, LocaleContextHolder.getLocale()));
		submenu.withChild(
				new MenuItem(messages.getMessage("config_user_telegram", null, LocaleContextHolder.getLocale()),
						"/config_user_telegram"));
		if (hasPrivilege(roles, SystemPrivilege.CONFIG_SYSTEM_MAIL)) {
			submenu.withChild(new MenuItem(messages.getMessage("config_email", null, LocaleContextHolder.getLocale()),
					"/config_email"));
		}
		if (hasPrivilege(roles, SystemPrivilege.CONFIG_SYSTEM_TELEGRAM)) {
			submenu.withChild(
					new MenuItem(messages.getMessage("config_bot_telegram", null, LocaleContextHolder.getLocale()),
							"/config_bot_telegram"));
		}
		if (hasPrivilege(roles, SystemPrivilege.SYNC_OPS)) {
			submenu.withChild(new MenuItem(messages.getMessage("config_sync", null, LocaleContextHolder.getLocale()),
					"/config_sync"));
			submenu.withChild(
					new MenuItem(messages.getMessage("sync", null, LocaleContextHolder.getLocale()), "/sync/current"));
		}
		if (hasPrivilege(roles, SystemPrivilege.ADMIN_OPS)) {
			if ("true".equalsIgnoreCase(env.getProperty("spring.h2.console.enabled")))
				submenu.withChild(new MenuItem(
						messages.getMessage("console_database", null, LocaleContextHolder.getLocale()), "/h2-console"));
		}
		if (hasPrivilege(roles, SystemPrivilege.TAX_DECLARATION_READ_ALL)) {
			
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			User user = (auth==null) ? null : userService.getUser(auth);

			String uri;
			
			if (user!=null && userService.mayHaveSpaceForPrivateDashboards(user)) {
				String txid = user.getTaxpayerId().replaceAll("\\D", "");
				String personal_space_id = "user-"+txid;
				uri = env.getProperty("kibana.menu.link")+"/s/"+personal_space_id+"/app/dashboards#";
			}
			else {
				uri = env.getProperty("kibana.menu.link")+"/app/dashboards#";
			}
			
			submenu.withChild(
					new MenuItem(messages.getMessage("dashboards_admin", null, LocaleContextHolder.getLocale()), uri));
		}
		if (hasPrivilege(roles, SystemPrivilege.CONFIG_API_TOKEN)) {
			submenu.withChild(new MenuItem(messages.getMessage("doc_api", null, LocaleContextHolder.getLocale()),
					"/swagger-ui/index.html"));
		}
		if (hasPrivilege(roles, SystemPrivilege.CONFIG_API_TOKEN))
			submenu.withChild(
					new MenuItem(messages.getMessage("config_token_api", null, LocaleContextHolder.getLocale()),
							"/config_token_api"));
		if (hasPrivilege(roles, SystemPrivilege.ADMIN_OPS)) {
			submenu.withChild(
					new MenuItem(messages.getMessage("sysinfo", null, LocaleContextHolder.getLocale()), "/sys_info"));
			submenu.withChild(
					new MenuItem(messages.getMessage("admin_shell", null, LocaleContextHolder.getLocale()), "/admin_shell"));
		}

		menu.add(submenu);

		return menu;
	}

}
