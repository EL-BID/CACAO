/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
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
	public String showAddUser(Model model) {
		ConfigEMail configEmail = configEmailService.getActiveConfig();
		if (configEmail != null && configEmail.getSupportEmail() != null
				&& configEmail.getSupportEmail().trim().length() > 0) {
			model.addAttribute("omit_password", true);
		}
		model.addAttribute("user", new User());
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
