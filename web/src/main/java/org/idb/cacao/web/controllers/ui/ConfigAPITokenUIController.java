/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.ui;

import org.idb.cacao.web.controllers.services.UserService;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.errors.InsufficientPrivilege;
import org.idb.cacao.web.errors.UserNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller class for all endpoints related to API token configuration for users by a user interface
 * 
 * @author Gustavo Figueiredo
 *
 */
@Controller
public class ConfigAPITokenUIController {

	@Autowired
	private UserService userService;

	/**
	 * Return UI for API token configuration
	 */
	@Secured({"ROLE_CONFIG_API_TOKEN"})
	@GetMapping("/config-token-api")
    public String showConfigTokenAPI(Model model) {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new InsufficientPrivilege();
    	User user = userService.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
		model.addAttribute("user", user);
		return "config/token/config_token_api";
	}


}
