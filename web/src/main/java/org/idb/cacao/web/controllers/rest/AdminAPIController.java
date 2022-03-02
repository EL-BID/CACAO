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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.idb.cacao.web.controllers.services.AdminService;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.errors.MissingParameter;
import org.idb.cacao.web.errors.UserNotFoundException;
import org.idb.cacao.web.utils.ControllerUtils;
import org.idb.cacao.web.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.tags.Tag;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Controller class for all endpoints related to 'administrative operations' interacting by a REST interface
 * 
 * @author Gustavo Figueiredo
 *
 */
@RestController
@RequestMapping("/api")
@Tag(name="admin-api-controller", description="Controller class for all endpoints related to 'administrative operations' interacting by a REST interface")
@ApiIgnore
public class AdminAPIController {

	public static final String LOG_PREFIX_FOR_SHELL_COMMANDS = "Incoming SHELL COMMAND from IP address";

	private static final Logger log = Logger.getLogger(AdminAPIController.class.getName());

	@Autowired
	private MessageSource messageSource;
	
	@Autowired
	private AdminService adminService;

	/**
	 * Post a command regarding one of the administrative operations
	 */
	@Secured({"ROLE_ADMIN_OPS"})
    @PostMapping(value="/op", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Post a command regarding one of the administrative operations",response=String.class)
    public ResponseEntity<Object> postShellCommand(
    		@ApiParam(name = "command", allowEmptyValue = false, allowMultiple = false, example = "delete -a", required = true, type = "String")
    		@Valid @RequestBody String command, 
    		BindingResult result,
			HttpServletRequest request) {
        if (result.hasErrors()) {
        	return ControllerUtils.returnErrors(result, messageSource);
        }
        
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();

    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException("missing user object");
    	
    	if (command==null || command.trim().length()==0)
    		throw new MissingParameter("command");

		String ipAddr = (request!=null && request.getRemoteAddr()!=null && request.getRemoteAddr().trim().length()>0) ? request.getRemoteAddr() : null;

		if (log.isLoggable(Level.INFO)) {
			log.log(Level.INFO, String.format("%s %s user %s: %s", LOG_PREFIX_FOR_SHELL_COMMANDS, ipAddr, user.getLogin(), command));
		}

		Object response;
		try {
			response = adminService.performOperation(command);
		}
		catch (Exception ex) {
			log.log(Level.SEVERE, String.format("Error while performing SHELL COMMAND from IP address %s user %s", ipAddr, user.getLogin()), ex);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
		}
		
		if (response==null)
			return ResponseEntity.ok().build();
		else {
			
			ObjectMapper mapper = new ObjectMapper();
			mapper.setSerializationInclusion(Include.NON_NULL);
			String json;
			try {
				json = mapper.writeValueAsString(response);
			} catch (JsonProcessingException e) {
				json = "{}";
			}
			
			return ResponseEntity.ok().body(json);
		}
	}

}
