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

import static org.idb.cacao.web.utils.ControllerUtils.searchPage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import javax.validation.Valid;

import org.idb.cacao.api.Views;
import org.idb.cacao.web.controllers.AdvancedSearch;
import org.idb.cacao.web.controllers.dto.PaginationData;
import org.idb.cacao.web.controllers.services.UserService;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.entities.UserProfile;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonView;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * Controller class for all endpoints related to 'user' object interacting by a REST interface
 * 
 * @author Gustavo Figueiredo
 *
 */
@RestController
@RequestMapping("/api")
@Api(description="Controller class for all endpoints related to 'user' object interacting by a REST interface.")
public class UserAPIController {

	private static final Logger log = Logger.getLogger(UserAPIController.class.getName());

	@Autowired
	private MessageSource messageSource;

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private Environment env;

	@JsonView(Views.Public.class)
	@GetMapping(value="/users", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Method used for listing users using pagination")
	public PaginationData<User> getUsers(Model model, @RequestParam("page") Optional<Integer> page,
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
		PaginationData<User> result = new PaginationData<>(users.getTotalPages(), users.getContent());
		return result;
	}
	
	/**
	 * Method used for returning names of users that match a given term. Useful for 'auto complete' fields
	 */
	@GetMapping("/user/names")
	@ApiOperation(value="Method used for returning names of users that match a given term. Useful for 'auto complete' fields")
	public ResponseEntity<List<String>> getUserNames(@ApiParam(required=false) @RequestParam("term") Optional<String> term) {
		List<String> names;
		if (term!=null && term.isPresent()) {
			Pattern pattern = Pattern.compile(Pattern.quote(term.get()), Pattern.CASE_INSENSITIVE);
			names =
			StreamSupport.stream(userRepository.findAll().spliterator(),false)
			.filter(user->pattern.matcher(user.getName()).find())
			.map(User::getName)
			.collect(Collectors.toList());
		}
		else {
			names =
			StreamSupport.stream(userRepository.findAll().spliterator(),false)
			.map(User::getName)
			.collect(Collectors.toList());
		}
		return ResponseEntity.ok().body(names);
	}

//	@Secured({"ROLE_SYSADMIN","ROLE_SUPPORT"})
    @PostMapping(value="/user", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Add a new user",response=User.class)
    public ResponseEntity<Object> addUser(@Valid @RequestBody User user, BindingResult result) {
        if (result.hasErrors()) {
        	return ControllerUtils.returnErrors(result, messageSource);
        }
        
        if (UserProfile.SYSADMIN.equals(user.getProfile())) {
        	// Only a SYSADMIN may create another SYSADMIN user account
        	if (!ControllerUtils.isSystemAdmin()) {
        		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(messageSource.getMessage("error.accessDenied", null, LocaleContextHolder.getLocale()));
        	}
        }

        // If password for new user was not provided, generate a random one that nobody knows
        if (user.getPassword()==null || user.getPassword().trim().length()==0) {
            if (user.getConfirmPassword()!=null && user.getConfirmPassword().trim().length()>0) {
            	log.log(Level.SEVERE,"Create user failed due to password mismatch");
            	return ResponseEntity.badRequest().body(messageSource.getMessage("user_password_mismatch", null, LocaleContextHolder.getLocale()));        	
            }
        	user.setPassword(new BCryptPasswordEncoder(11).encode(UUID.randomUUID().toString()));
        }
        else {
        	if (user.getConfirmPassword()==null || !user.getPassword().equals(user.getConfirmPassword())) {
            	log.log(Level.SEVERE,"Create user failed due to password mismatch");
            	return ResponseEntity.badRequest().body(messageSource.getMessage("user_password_mismatch", null, LocaleContextHolder.getLocale()));        	        		
        	}
        	user.setPassword(new BCryptPasswordEncoder(11).encode(user.getPassword()));
        }
        
        log.log(Level.INFO, "Creating new user "+user.getName()+" "+user.getLogin()+" "+user.getProfile());
        
        try {
        	userRepository.saveWithTimestamp(user);
        }
        catch (Exception ex) {
        	log.log(Level.SEVERE,"Create user failed", ex);
        	return ResponseEntity.badRequest().body(messageSource.getMessage("op.failed", null, LocaleContextHolder.getLocale()));
        }
        return ResponseEntity.ok().body(user);
    }

//	@Secured({"ROLE_SYSADMIN","ROLE_SUPPORT"})
    @PutMapping(value="/user/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Updates an existing user",response=User.class)
    public ResponseEntity<Object> updateUser(@PathVariable("id") String id, @Valid @RequestBody User user, BindingResult result) {
        if (result.hasErrors()) {
        	return ControllerUtils.returnErrors(result, messageSource);
        }
        
        Optional<User> user_in_database = userRepository.findById(id);
        
        if ((user_in_database.isPresent() && UserProfile.SYSADMIN.equals(user_in_database.get().getProfile()))
        	|| UserProfile.SYSADMIN.equals(user.getProfile())) {
        	// Only a SYSADMIN may change the user account of another SYSADMIN
        	if (!ControllerUtils.isSystemAdmin()) {
        		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(messageSource.getMessage("error.accessDenied", null, LocaleContextHolder.getLocale()));
        	}
        }
        
        // Keep previous password, token and telegram information
        if (user_in_database.isPresent()) {      
        	user.setPassword(user_in_database.get().getPassword());
        	user.setApiToken(user_in_database.get().getApiToken());
        }
        
        log.log(Level.INFO, "Changing user #"+id+" "+user.getName()+" "+user.getLogin()+" "+user.getProfile());

        user.setId(id);
        userRepository.saveWithTimestamp(user);
        return ResponseEntity.ok().body(user);
    }
    
//	@Secured({"ROLE_SYSADMIN","ROLE_SUPPORT"})
    @DeleteMapping(value="/user/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Deletes an existing user",response=User.class)
    public ResponseEntity<Object> deleteUser(@PathVariable("id") String id) {
        User user = userRepository.findById(id).orElse(null);
        if (user==null)
        	return ResponseEntity.notFound().build();
        
        if (UserProfile.SYSADMIN.equals(user.getProfile())) {
        	// Only a SYSADMIN may change the user account of another SYSADMIN
        	if (!ControllerUtils.isSystemAdmin()) {
        		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(messageSource.getMessage("error.accessDenied", null, LocaleContextHolder.getLocale()));
        	}
        }

        log.log(Level.INFO, "Deleting user #"+id+" "+user.getName()+" "+user.getLogin()+" "+user.getProfile());
        
        // Removes User object itself

        userRepository.delete(user);
        return ResponseEntity.ok().body(user);
    }

}
