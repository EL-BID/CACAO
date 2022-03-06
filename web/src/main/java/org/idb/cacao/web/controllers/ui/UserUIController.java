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

import java.util.Optional;

import org.idb.cacao.api.Taxpayer;
import org.idb.cacao.web.controllers.services.IConfigEMailService;
import org.idb.cacao.web.entities.ConfigEMail;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.repositories.TaxpayerRepository;
import org.idb.cacao.web.repositories.UserRepository;
import org.idb.cacao.web.utils.ControllerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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
	private UserRepository userRepository;
	
	@Autowired TaxpayerRepository taxpayerRepository;

	@Autowired
	private IConfigEMailService configEmailService;

	@GetMapping("/institutional")
	public String showInstitutional(Model model) {

		ControllerUtils.tagLoggedArea(model);

		return "legal/institutional";
	}
	
	@GetMapping("/usermanual")
	public String showUsermanual(Model model) {

		ControllerUtils.tagLoggedArea(model);

		return "usermanual/usermanual";
	}


	@Secured({ "ROLE_USER_READ" })
	@GetMapping("/users")
	@Transactional
	public String getUsers(Model model) {
		
		return "users/users";
	}

	@Secured({ "ROLE_USER_WRITE" })
	@GetMapping("/users/add")
	public String showAddUser(User user, Model model) {
		ConfigEMail configEmail = configEmailService.getActiveConfig();
		if (configEmail != null && configEmail.getSupportEmail() != null
				&& configEmail.getSupportEmail().trim().length() > 0) {
			model.addAttribute("omit_password", true);
		}
		return "users/add-user";
	}

	@Secured({ "ROLE_USER_WRITE" })
	@GetMapping("/users/{id}/edit")
	public String showUpdateForm(@PathVariable("id") String id, Model model) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + id));
		model.addAttribute("user", user);
		if(user.getTaxpayerId()!=null) {
			Optional<Taxpayer> name = taxpayerRepository.findByTaxPayerId(user.getTaxpayerId());
			if (name.isPresent()) {
				model.addAttribute("taxpayerName", name.get());
			}
		}
		
		return "users/update-user";
	}

	@Secured({ "ROLE_USER_WRITE" })
	@GetMapping("/users/{id}")
	public String showUser(@PathVariable("id") String id, Model model) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + id));
		model.addAttribute("user", user);
		if(user.getTaxpayerId()!=null) {
			Optional<Taxpayer> name = taxpayerRepository.findByTaxPayerId(user.getTaxpayerId());
			if (name.isPresent()) {
				model.addAttribute("taxpayerName", name.get());
			}
		}
		
		return "users/show-user";
	}

}
