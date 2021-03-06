/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.ui;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.idb.cacao.web.controllers.services.UserService;
import org.idb.cacao.web.dto.MenuItem;
import org.idb.cacao.web.entities.SystemPrivilege;
import org.idb.cacao.web.entities.UserProfile;
import org.idb.cacao.web.repositories.ESStandardRoles;
import org.idb.cacao.web.utils.LoginUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller class for all endpoints related to user interface regarding 'login', 'home' 
 * and other initial screens.
 * 
 * @author Gustavo Figueiredo
 *
 */
@Controller
public class LoginUIController {

	static final Logger log = Logger.getLogger(LoginUIController.class.getName());

	@Autowired
	private MessageSource messages;

	@Autowired
	private Environment env;
	
	@Autowired
	private UserService userService;

	@Value("${presentation.mode}")
	private Boolean presentationMode;

	@GetMapping("/")
	public String frontend(Model model){
		
		return "login/index";
	}
	
	@GetMapping("/login")
	public String showLogin(Model model) {

		// Enable/disable some login options based on available configuration
		model.addAttribute("has_login_google", LoginUtils.hasGoogleProvider(env));
		model.addAttribute("has_login_azure", LoginUtils.hasAzureProvider(env));
		model.addAttribute("has_login_facebook", LoginUtils.hasFacebookProvider(env));

		return "login/login";
	}

	@GetMapping("/forgetPassword")
	public String showForgetPassword(Model model) {

		return "login/forgetPassword";
	}

	@GetMapping("/home")
	public String showHome(Model model, HttpServletRequest request) {
		
		model.addAttribute("menu", getHomeMenuItens());
		
		// Present the first page for taxpayers and tax administrators
		String first_page_child_frame = "/cards";

		model.addAttribute("first_page_child_frame", first_page_child_frame);

		return "login/home";
	}
	
	@GetMapping("/cards")
	public String showCards(Model model) {
		List<MenuItem> cards = getHomeMenuItens().stream()
		  .flatMap(m -> Stream.concat(Stream.of(m), m.getChildren().stream()))
		  .filter(m -> m.isActive() && m.getIcon()!=null)
		  .collect(Collectors.toList());
		model.addAttribute("cards", cards);
		return "login/cards";
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

	public List<MenuItem> getHomeMenuItens(Collection<? extends GrantedAuthority> roles) {

		List<MenuItem> menu = new LinkedList<>();
		
		final boolean isPresentationMode = Boolean.TRUE.equals(presentationMode);

		menu.add(new MenuItem(messages.getMessage("menu.homepage", null, LocaleContextHolder.getLocale()), "/cards"));
		
		if (!isPresentationMode && hasPrivilege(roles, SystemPrivilege.TAX_DECLARATION_WRITE)) {
			menu.add(new MenuItem(messages.getMessage("docs.main.upload", null, LocaleContextHolder.getLocale()),
				"/docs"));
		}
		if (hasPrivilege(roles, SystemPrivilege.TAX_DECLARATION_READ)) {
			menu.add(new MenuItem(messages.getMessage("docs.history", null, LocaleContextHolder.getLocale()), 
					"/docs-search", "upload"));
		}
		if (hasPrivilege(roles, SystemPrivilege.USER_READ)) {
			menu.add(new MenuItem(messages.getMessage("users.title", null, LocaleContextHolder.getLocale()),
				"/users", "users"));
		}
		if (hasPrivilege(roles, SystemPrivilege.TAXPAYER_READ)) {
			menu.add(new MenuItem(messages.getMessage("taxpayers.title", null, LocaleContextHolder.getLocale()),
					"/taxpayers", "address book outline"));
		}
		
		MenuItem submenu = new MenuItem(messages.getMessage("taxpayers.analysis", null, LocaleContextHolder.getLocale()));
		menu.add(submenu);
		if (hasPrivilege(roles, SystemPrivilege.TAX_REPORT_READ)) {
			submenu.withChild(new MenuItem(messages.getMessage("taxpayers.analysis.vertical.and.horizontal", null, LocaleContextHolder.getLocale()),
					"/vertical-horizontal-analysis", "stream"));
			submenu.withChild(new MenuItem(messages.getMessage("taxpayers.analysis.general", null, LocaleContextHolder.getLocale()),
					"/general-analysis", "sliders horizontal"));
			submenu.withChild(new MenuItem(messages.getMessage("taxpayers.analysis.statement.income", null, LocaleContextHolder.getLocale()),
					"/statement-income-analysis", "search dollar"));
			submenu.withChild(new MenuItem(messages.getMessage("taxpayers.analysis.general.view", null, LocaleContextHolder.getLocale()),
					"/taxpayer-general-view", "street view"));
			submenu.withChild(new MenuItem(messages.getMessage("taxpayers.analysis.customers.versus.suppliers", null, LocaleContextHolder.getLocale()),
					"/customers-vs-suppliers-analysis", "balance scale right"));			
			submenu.withChild(new MenuItem(messages.getMessage("taxpayers.analysis.flows", null, LocaleContextHolder.getLocale()),
					"/accounting-flows", "retweet"));			
		}		

		if (hasPrivilege(roles, SystemPrivilege.INTERPERSONAL_READ_ALL)) {
			menu.add(new MenuItem(messages.getMessage("interpersonals.title", null, LocaleContextHolder.getLocale()),
					"/interpersonals", "people arrows"));
		}
		
		if (hasPrivilege(roles, SystemPrivilege.TAX_TEMPLATE_WRITE)) {
			menu.add(new MenuItem(messages.getMessage("templates", null, LocaleContextHolder.getLocale()),
					"/templates", "file alternate outline"));
		}

		if (hasPrivilege(roles, SystemPrivilege.TAX_DOMAIN_TABLE_WRITE)) {
			menu.add(new MenuItem(messages.getMessage("domain.tables.title", null, LocaleContextHolder.getLocale()),
					"/domaintables", "table"));
		}

		submenu = new MenuItem(messages.getMessage("config.menu", null, LocaleContextHolder.getLocale()));
		menu.add(submenu);
		if (hasPrivilege(roles, SystemPrivilege.CONFIG_SYSTEM_MAIL)) {
			submenu.withChild(new MenuItem(messages.getMessage("config.email", null, LocaleContextHolder.getLocale()),
					"/config-email"));
		}

		if (hasPrivilege(roles, SystemPrivilege.SYNC_OPS)) {
			submenu.withChild(new MenuItem(messages.getMessage("config.sync", null, LocaleContextHolder.getLocale()),
					"/config-sync"));
			submenu.withChild(
					new MenuItem(messages.getMessage("sync", null, LocaleContextHolder.getLocale()), "/sync/current"));
		}

		if (hasPrivilege(roles, SystemPrivilege.ADMIN_OPS)) {
			submenu.withChild(new MenuItem(messages.getMessage("sysinfo", null, LocaleContextHolder.getLocale()),
					"/sys-info",  "info circle"));
		}
		
		if (!isPresentationMode && hasPrivilege(roles, SystemPrivilege.TAX_DECLARATION_READ_ALL)) {
			
			String uri = userService.getDashboardsURI();
			if (uri!=null && uri.trim().length()>0) {
				menu.add(
					new MenuItem(messages.getMessage("dashboards.admin", null, LocaleContextHolder.getLocale()), uri));
			}
			
			if (hasStandardRole(roles, ESStandardRoles.DASHBOARDS_WRITE)) {
				menu.add(
					new MenuItem(messages.getMessage("dashboards.copy", null, LocaleContextHolder.getLocale()), 
						"/dashboards-list"));
			}
			
		}
		
		if (hasPrivilege(roles, SystemPrivilege.CONFIG_API_TOKEN)) {
			submenu.withChild(
					new MenuItem(messages.getMessage("config.token.api", null, LocaleContextHolder.getLocale()),
							"/config-token-api"));
		}

		if (hasPrivilege(roles, SystemPrivilege.CONFIG_API_TOKEN)) {
			submenu.withChild(new MenuItem(messages.getMessage("api.title", null, LocaleContextHolder.getLocale()),
					"/swagger-ui/index.html"));
		}

		if (!isPresentationMode && hasPrivilege(roles, SystemPrivilege.ADMIN_OPS)) {
			menu.add(
				new MenuItem(messages.getMessage("admin.shell", null, LocaleContextHolder.getLocale()), "/admin-shell",  "cogs"));
		}

		return menu;
	}

	/**
	 * Given a collection of user roles, check if any of these roles is mapped to the given privilege
	 */
	public static boolean hasPrivilege(Collection<? extends GrantedAuthority> roles, SystemPrivilege privilege) {
		return roles.stream()
				.anyMatch(a -> privilege.getRole().equalsIgnoreCase(a.getAuthority()));
	}
	
	/**
	 * Check if any of the provided roles (granted to authenticated user) corresponds to the provided ESStandardRoles
	 * (some standard role regarding the use of Kibana interface).
	 */
	public static boolean hasStandardRole(Collection<? extends GrantedAuthority> roles, ESStandardRoles role) {
		if (role==null || roles==null || roles.isEmpty())
			return false;
		Set<UserProfile> profiles_for_role = role.getUserProfiles();
		if (profiles_for_role==null || profiles_for_role.isEmpty())
			return false;
		Set<String> roles_names = roles.stream()
				.map(a->a.getAuthority())
				.collect(Collectors.toCollection(()->new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
		return profiles_for_role.stream().anyMatch(p->roles_names.contains(p.getRole()));
	}

}
