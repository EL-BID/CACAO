/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.rest;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.idb.cacao.web.GenericResponse;
import org.idb.cacao.web.controllers.services.KeyStoreService;
import org.idb.cacao.web.controllers.services.UserService;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.errors.InsufficientPrivilege;
import org.idb.cacao.web.errors.UserNotFoundException;
import org.idb.cacao.web.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import springfox.documentation.annotations.ApiIgnore;

/**
 * Controller class for all endpoints related to API token configuration for users by a REST interface
 * 
 * @author Gustavo Figueiredo
 *
 */
@RestController
@RequestMapping("/api")
@ApiIgnore
public class ConfigAPITokenAPIController {

	@Autowired
	private KeyStoreService keystoreService;
	
	@Autowired
	private UserService userService;

	@Autowired
	private UserRepository userRepository;

	/**
	 * Return the user's API token
	 */
	@Secured({"ROLE_CONFIG_API_TOKEN"})
	@GetMapping(value = "/token-api", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiIgnore
    public ResponseEntity<Map<String,String>> getTokenAPI(Model model) {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new InsufficientPrivilege();
    	User user = userService.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
    	String encryptedApiToken = user.getApiToken();
    	if (encryptedApiToken==null || encryptedApiToken.trim().length()==0)    		
    		return ResponseEntity.ok().body(Collections.singletonMap("token",""));
    	String apiToken = keystoreService.decrypt(KeyStoreService.PREFIX_MAIL, encryptedApiToken);
    	return ResponseEntity.ok().body(Collections.singletonMap("token",apiToken));
	}

	/**
	 * Deletes the user's API token
	 */
	@Secured({"ROLE_CONFIG_API_TOKEN"})
	@Transactional
	@DeleteMapping(value = "/token-api")
	@ApiIgnore
	@ResponseBody
    public GenericResponse deleteTokenAPI(Model model) {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new InsufficientPrivilege();
    	User user = userService.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
    	
		user.setApiToken(null);
		userRepository.saveWithTimestamp(user);
    	
    	return new GenericResponse("OK");
	}

	/**
	 * Creates a new API token for the user
	 */
	@Secured({"ROLE_CONFIG_API_TOKEN"})
	@Transactional
	@PutMapping(value = "/token-api", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiIgnore
    public ResponseEntity<Map<String,String>> createTokenAPI(Model model) {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new InsufficientPrivilege();
    	User user = userService.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
    	
    	String apiToken;
		apiToken = UUID.randomUUID().toString();
		String encryptedApiToken = keystoreService.encrypt(KeyStoreService.PREFIX_MAIL, apiToken);
		user.setApiToken(encryptedApiToken);
		userRepository.saveWithTimestamp(user);
    	
    	return ResponseEntity.ok().body(Collections.singletonMap("token",apiToken));
	}
}
