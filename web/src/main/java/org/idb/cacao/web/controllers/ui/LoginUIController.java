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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.idb.cacao.web.dto.MenuItem;
import org.idb.cacao.web.entities.SystemPrivilege;
import org.idb.cacao.web.utils.LoginUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
		
		// TODO: should present the first page for taxpayers and tax administrators
		//String first_page_child_frame = "/bulletin_board";
		String first_page_child_frame = "/sys_info";

		model.addAttribute("first_page_child_frame", first_page_child_frame);

		return "login/home";
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
		// FIXME:
		//if (roles == null || roles.isEmpty())
		//	return Collections.emptyList();

		List<MenuItem> menu = new LinkedList<>();

		if (hasPrivilege(roles, SystemPrivilege.TAX_DECLARATION_WRITE)) {
			menu.add(new MenuItem(messages.getMessage("docs.main.upload", null, LocaleContextHolder.getLocale()),
				"/docs"));
		}
		if (hasPrivilege(roles, SystemPrivilege.TAX_DECLARATION_READ)) {
			menu.add(new MenuItem(messages.getMessage("docs.history", null, LocaleContextHolder.getLocale()), "/docs_search"));
		}
		if (hasPrivilege(roles, SystemPrivilege.USER_READ)) {
			menu.add(new MenuItem(messages.getMessage("users.title", null, LocaleContextHolder.getLocale()),
				"/users"));
		}
		if (hasPrivilege(roles, SystemPrivilege.TAX_TEMPLATE_WRITE)) {
			menu.add(new MenuItem(messages.getMessage("templates", null, LocaleContextHolder.getLocale()),
					"/templates"));
		}

		MenuItem submenu = new MenuItem(messages.getMessage("config.menu", null, LocaleContextHolder.getLocale()));
		menu.add(submenu);
		if (hasPrivilege(roles, SystemPrivilege.CONFIG_SYSTEM_MAIL)) {
			submenu.withChild(new MenuItem(messages.getMessage("config.email", null, LocaleContextHolder.getLocale()),
					"/config_email"));
		}

		if (hasPrivilege(roles, SystemPrivilege.ADMIN_OPS)) {
			submenu.withChild(new MenuItem(messages.getMessage("sysinfo", null, LocaleContextHolder.getLocale()),
					"/sys_info"));
		}
		
		if (hasPrivilege(roles, SystemPrivilege.ADMIN_OPS)) {
			menu.add(
				new MenuItem(messages.getMessage("admin.shell", null, LocaleContextHolder.getLocale()), "/admin_shell"));
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
}
