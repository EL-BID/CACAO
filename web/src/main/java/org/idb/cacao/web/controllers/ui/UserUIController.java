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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.idb.cacao.web.controllers.AdvancedSearch;
import org.idb.cacao.web.controllers.services.IConfigEMailService;
import org.idb.cacao.web.controllers.services.UserService;
import org.idb.cacao.web.entities.ConfigEMail;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.repositories.UserRepository;
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

	@GetMapping("/institutional")
	public String showInstitutional(Model model) {

		ControllerUtils.tagLoggedArea(model);

		return "legal/institutional";
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
						.withDisplayName(messages.getMessage("taxpayer.id", null, LocaleContextHolder.getLocale())))
				.withFilter(new AdvancedSearch.QueryFilterTerm("name")
						.withDisplayName(messages.getMessage("user.name", null, LocaleContextHolder.getLocale())))
				.withFilter(new AdvancedSearch.QueryFilterTerm("login")
						.withDisplayName(messages.getMessage("user.login", null, LocaleContextHolder.getLocale())))
				.withFilter(new AdvancedSearch.QueryFilterTerm("profile")
						.withDisplayName(messages.getMessage("user.profile", null, LocaleContextHolder.getLocale()))));
		model.addAttribute("applied_filters", filters
				.map(f -> f.withDisplayNames((AdvancedSearch) model.getAttribute("filter_options")).wiredTo(messages)));

		return "/user/users";
	}

	//@Secured({ "ROLE_USER_WRITE" })
	@GetMapping("/adduser")
	public String showAddUser(User user, Model model) {
		ConfigEMail config_email = configEmailService.getActiveConfig();
		if (config_email != null && config_email.getSupportEmail() != null
				&& config_email.getSupportEmail().trim().length() > 0) {
			model.addAttribute("omit_password", true);
		}
		return "/user/add-user";
	}

	@Secured({ "ROLE_USER_WRITE" })
	@GetMapping("/edituser/{id}")
	public String showUpdateForm(@PathVariable("id") String id, Model model) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + id));
		model.addAttribute("user", user);
		return "/user/update-user";
	}

}
