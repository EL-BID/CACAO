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
	@GetMapping(value = "/token_api", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiIgnore
    public ResponseEntity<Map<String,String>> getTokenAPI(Model model) {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new InsufficientPrivilege();
    	User user = userService.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
    	String encrypted_api_token = user.getApiToken();
    	if (encrypted_api_token==null || encrypted_api_token.trim().length()==0)    		
    		return ResponseEntity.ok().body(Collections.singletonMap("token",""));
    	String api_token = keystoreService.decrypt(encrypted_api_token);
    	return ResponseEntity.ok().body(Collections.singletonMap("token",api_token));
	}

	/**
	 * Deletes the user's API token
	 */
	@Secured({"ROLE_CONFIG_API_TOKEN"})
	@Transactional
	@DeleteMapping(value = "/token_api")
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
	@PutMapping(value = "/token_api", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiIgnore
    public ResponseEntity<Map<String,String>> createTokenAPI(Model model) {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new InsufficientPrivilege();
    	User user = userService.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
    	
    	String api_token;
		api_token = UUID.randomUUID().toString();
		String encrypted_api_token = keystoreService.encrypt(api_token);
		user.setApiToken(encrypted_api_token);
		userRepository.saveWithTimestamp(user);
    	
    	return ResponseEntity.ok().body(Collections.singletonMap("token",api_token));
	}
}
