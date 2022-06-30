/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.rest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.validation.Valid;

import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.sort.SortOrder;
import org.idb.cacao.api.Views;
import org.idb.cacao.web.controllers.AdvancedSearch;
import org.idb.cacao.web.controllers.services.UserService;
import org.idb.cacao.web.dto.PaginationData;
import org.idb.cacao.web.dto.UserDto;
import org.idb.cacao.web.entities.PasswordResetToken;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.entities.UserProfile;
import org.idb.cacao.web.errors.UserNotFoundException;
import org.idb.cacao.web.repositories.PasswordResetTokenRepository;
import org.idb.cacao.web.repositories.UserRepository;
import org.idb.cacao.web.utils.ControllerUtils;
import org.idb.cacao.web.utils.SearchUtils;
import org.idb.cacao.web.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller class for all endpoints related to 'user' object interacting by a REST interface
 * 
 * @author Gustavo Figueiredo
 *
 */
@RestController
@RequestMapping("/api")
@Tag(name="user-api-controller", description="Controller class for all endpoints related to 'user' object interacting by a REST interface.")
public class UserAPIController {

	private static final Logger log = Logger.getLogger(UserAPIController.class.getName());

	@Autowired
	private MessageSource messageSource;

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private RestHighLevelClient elasticsearchClient;
	
	@Autowired
	private PasswordResetTokenRepository passwordResetRepository;

	@JsonView(Views.Public.class)
	@Secured({"ROLE_USER_READ"})
	@GetMapping(value="/users", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Method used for listing users using pagination")
	public PaginationData<User> getUsersWithPagination(Model model, 
			@ApiParam(name = "Number of page to retrieve", allowEmptyValue = true, allowMultiple = false, required = false, type = "Integer")
			@RequestParam("page") Optional<Integer> page, 
			@ApiParam(name = "Page size", allowEmptyValue = true, allowMultiple = false, required = false, type = "Integer")
			@RequestParam("size") Optional<Integer> size,
			@ApiParam(name = "Fields and values to filer data", allowEmptyValue = true, allowMultiple = false, required = false, type = "String")
			@RequestParam("filter") Optional<String> filter, 
			@ApiParam(name = "Field name to sort data", allowEmptyValue = true, allowMultiple = false, required = false, type = "String")
			@RequestParam("sortby") Optional<String> sortBy,
			@ApiParam(name = "Order to sort. Can be asc or desc", allowEmptyValue = true, allowMultiple = false, required = false, type = "String")
			@RequestParam("sortorder") Optional<String> sortOrder) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();

		Optional<AdvancedSearch> filters = SearchUtils.fromTabulatorJSON(filter);
		Page<User> docs;
		Optional<String> sortField = Optional.of(sortBy.orElse("name"));
		Optional<SortOrder> direction = Optional.of(sortOrder.orElse("asc").equals("asc") ? SortOrder.ASC : SortOrder.DESC);
		try {
			docs = SearchUtils.doSearch(filters.orElse(new AdvancedSearch()), User.class, elasticsearchClient, page, size, 
					sortField, direction);

		}
		catch (Exception ex) {
			log.log(Level.SEVERE, "Error while searching for all documents", ex);
			docs = Page.empty();
		}		
		return new PaginationData<>(docs.getTotalPages(), docs.getContent());
	}
	
	/**
	 * Method used for returning names of users that match a given term. Useful for 'auto complete' fields
	 */
	@Secured({"ROLE_USER_READ"})
	@GetMapping("/user/names")
	@ApiOperation(value="Method used for returning names of users that match a given term. Useful for 'auto complete' fields")
	public ResponseEntity<List<String>> getUserNames(@ApiParam(required=false) @RequestParam("term") Optional<String> term) {
		List<String> names;
		if (term.isPresent()) {
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

	@Secured({"ROLE_USER_WRITE"})
	@PostMapping(value="/user", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Add a new user",response=UserDto.class)
    public ResponseEntity<Object> addUser(@Valid @RequestBody UserDto user, BindingResult result) {
        if (result.hasErrors()) {
        	return ControllerUtils.returnErrors(result, messageSource);
        }
        
        // Only a SYSADMIN may create another SYSADMIN user account
        if (UserProfile.SYSADMIN.equals(user.getProfile()) && !ControllerUtils.isSystemAdmin()) {
       		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(messageSource.getMessage("error.accessDenied", null, LocaleContextHolder.getLocale()));
        }

        User existent = userRepository.findByLoginIgnoreCase(user.getLogin());
        if (existent!=null) {
        	return ResponseEntity.badRequest().body(messageSource.getMessage("user.error.already.exists", null, LocaleContextHolder.getLocale()));
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
        
        if (log.isLoggable(Level.INFO)) {
        	log.log(Level.INFO, String.format("Creating new user %s|%s|%s", user.getName(), user.getLogin(), user.getProfile()));
        }
        
        User entity = new User();
        try {
        	user.updateEntity(entity);
        	if (user.getPassword()!=null && user.getPassword().trim().length()>0
        			&& user.getConfirmPassword()!=null && user.getConfirmPassword().trim().length()>0
        			&& user.getPassword().equalsIgnoreCase(user.getConfirmPassword()))
        		entity.setPassword(userService.encodePassword(user.getPassword()));
        	userRepository.saveWithTimestamp(entity);
        }
        catch (Exception ex) {
        	log.log(Level.SEVERE,"Create user failed", ex);
        	return ResponseEntity.badRequest().body(messageSource.getMessage("op.failed", null, LocaleContextHolder.getLocale()));
        }
        return ResponseEntity.ok().body(new UserDto(entity));
    }

	@Secured({"ROLE_USER_WRITE"})
    @PutMapping(value="/user/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Updates an existing user",response=UserDto.class)
    public ResponseEntity<Object> updateUser(@PathVariable("id") String id, @Valid @RequestBody UserDto user, BindingResult result) {
        if (result.hasErrors()) {
        	return ControllerUtils.returnErrors(result, messageSource);
        }
        
        Optional<User> existent = userRepository.findById(id);
        if (!existent.isPresent()) {
           	return ResponseEntity.notFound().build();
        }

        
        // Only a SYSADMIN may change the user account of another SYSADMIN
        if ((UserProfile.SYSADMIN.equals(existent.get().getProfile())
        	|| UserProfile.SYSADMIN.equals(user.getProfile())) && (!ControllerUtils.isSystemAdmin())) {
        		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(messageSource.getMessage("error.accessDenied", null, LocaleContextHolder.getLocale()));
        }
        
		User entity = existent.get();
        final boolean changed_user_profile = hasChanged(entity.getProfile(), user.getProfile());

        user.updateEntity(entity);
    	if (user.getPassword()!=null && user.getPassword().trim().length()>0
    			&& user.getConfirmPassword()!=null && user.getConfirmPassword().trim().length()>0
    			&& user.getPassword().equalsIgnoreCase(user.getConfirmPassword()))
    		entity.setPassword(userService.encodePassword(user.getPassword()));

        if (log.isLoggable(Level.INFO)) {
        	log.log(Level.INFO, String.format("Changing user #%s|%s|%s|%s", id, user.getName(), user.getLogin(), user.getProfile()));
        }

        userRepository.saveWithTimestamp(entity);
        
        if (changed_user_profile
        		&& entity.getKibanaToken()!=null && entity.getKibanaToken().trim().length()>0
        		&& userService.hasUserControlForKibanaAccess()) {
        	// If the user profile has changed and if there is a token granting access to this user,
        	// it's necessary to update the user account at ElasticSearch as well.
        	try {
        		userService.updateUserForKibanaAccess(entity);
        	}
        	catch (Exception ex) {
        		log.log(Level.SEVERE, String.format("Error while updating user access at Kibana for user account %s", user.getLogin()), ex);
        	}
        }

        return ResponseEntity.ok().body(new UserDto(entity));
    }
    
	@Secured({"ROLE_USER_WRITE"})
	@DeleteMapping(value="/user/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Deletes an existing user",response=UserDto.class)
    public ResponseEntity<Object> deleteUser(@PathVariable("id") String id) {
        User user = userRepository.findById(id).orElse(null);
        if (user==null)
        	return ResponseEntity.notFound().build();
        
        // Only a SYSADMIN may change the user account of another SYSADMIN
        if (UserProfile.SYSADMIN.equals(user.getProfile()) && !ControllerUtils.isSystemAdmin()) {
       		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(messageSource.getMessage("error.accessDenied", null, LocaleContextHolder.getLocale()));
        }

        if (log.isLoggable(Level.INFO)) {
        	log.log(Level.INFO, String.format("Deleting user #%s|%s|%s|%s", id, user.getName(), user.getLogin(), user.getProfile()));
        }
        
        // Removes references to User object
        try {
	        Page<PasswordResetToken> refs = passwordResetRepository.findByUserId(user.getId(), PageRequest.of(0, 10_000));
	        if (refs!=null && refs.hasContent()) {
	        	for (PasswordResetToken ref: refs) {
	        		passwordResetRepository.delete(ref);
	        	}
	        }
        }
        catch (Throwable ex) {
        	log.log(Level.SEVERE, "Error when trying to delete PasswordResetToken that references user to be deleted: #"+id+" "+user.getName()+" "+user.getLogin()+" "+user.getProfile());
        }

        user.setActive(false);
        try {
        	userRepository.saveWithTimestamp(user);
        }
        catch (Exception ex) {
        	log.log(Level.SEVERE,"Delete user failed", ex);
        	return ResponseEntity.badRequest().body(messageSource.getMessage("op.failed", null, LocaleContextHolder.getLocale()));
        }
        return ResponseEntity.ok().body(new UserDto(user));
    }

	@Secured({"ROLE_USER_WRITE"})
    @GetMapping(value="/user/{id}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Activate an existing user", response=UserDto.class)
    public ResponseEntity<Object> activate(
    		@ApiParam(name = "User ID", allowEmptyValue = false, allowMultiple = false, example = "1234567890", required = true, type = "String")
    		@PathVariable("id") String id) {
        
		Optional<User> existing = userRepository.findById(id);
		if (!existing.isPresent())
        	return ResponseEntity.notFound().build();
		User user = existing.get();
		user.setActive(true);
		if (log.isLoggable(Level.INFO)) {
			log.log(Level.INFO, String.format("Activating user #%s|%s|%s|%s", id, user.getName(), user.getLogin(), user.getProfile()));
		}
		try {
			userRepository.saveWithTimestamp(user);
		}
        catch (Exception ex) {
        	log.log(Level.SEVERE,"Activate user failed", ex);
        	return ResponseEntity.badRequest().body(messageSource.getMessage("op.failed", null, LocaleContextHolder.getLocale()));
        }
        return ResponseEntity.ok().body(new UserDto(user));
    }

	@Secured({"ROLE_USER_WRITE"})
    @GetMapping(value="/user/{id}/deactivate", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Deactivate an existing user", response=UserDto.class)
    public ResponseEntity<Object> deactivate(
    		@ApiParam(name = "User ID", allowEmptyValue = false, allowMultiple = false, example = "1234567890", required = true, type = "String")
    		@PathVariable("id") String id) {
        
		Optional<User> existing= userRepository.findById(id);
		if (!existing.isPresent())
        	return ResponseEntity.notFound().build();
		User user= existing.get();
		user.setActive(false);
		if (log.isLoggable(Level.INFO)) {
			log.log(Level.INFO, String.format("Deactivating user #%s|%s|%s|%s", id, user.getName(), user.getLogin(), user.getProfile()));
		}
		try {
			userRepository.saveWithTimestamp(user);
		}
        catch (Exception ex) {
        	log.log(Level.SEVERE,"Deactivate user failed", ex);
        	return ResponseEntity.badRequest().body(messageSource.getMessage("op.failed", null, LocaleContextHolder.getLocale()));
        }	
        return ResponseEntity.ok().body(new UserDto(user));
    }

	
	public static boolean hasChanged(UserProfile profile1, UserProfile profile2) {
		if (profile1==profile2)
			return false;
		if (profile1==null || profile2==null)
			return true;
		return !profile1.equals(profile2);
	}
}
