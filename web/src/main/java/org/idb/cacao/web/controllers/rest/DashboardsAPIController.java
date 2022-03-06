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

import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.idb.cacao.web.GenericResponse;
import org.idb.cacao.web.controllers.services.UserService;
import org.idb.cacao.web.dto.DashboardCopy;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.errors.UserNotFoundException;
import org.idb.cacao.web.repositories.ESStandardRoles;
import org.idb.cacao.web.utils.ESUtils;
import org.idb.cacao.web.utils.HttpUtils;
import org.idb.cacao.web.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller class for all endpoints related to 'Dashboards' management by a REST interface
 * 
 * @author Gustavo Figueiredo
 *
 */
@RestController
@RequestMapping("/api")
@Tag(name="dashboard-api-controller", description="Controller class for all endpoints related to 'dashboard' object interacting by a REST interface")
public class DashboardsAPIController {

	private static final Logger log = Logger.getLogger(DashboardsAPIController.class.getName());

	@Autowired
	private MessageSource messages;

	@Autowired
	private UserService userService;

	@Autowired
	private Environment env;

    private RestTemplate restTemplate;

	@Autowired
	public DashboardsAPIController(RestTemplateBuilder builder) {
		this.restTemplate = builder
				.setConnectTimeout(Duration.ofMinutes(5))
				.setReadTimeout(Duration.ofMinutes(5))
				.requestFactory(HttpUtils::getTrustAllHttpRequestFactory)
				.build();
	}

	/**
	 * Make a copy of a dashboard from one Kibana Space to other Kibana Spaces
	 */
	@Secured({"ROLE_TAX_DECLARATION_READ_ALL"})
	@PostMapping(value = "/dashboard-copy/{space_id}/{dashboard_id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Make a copy of a dashboard from one Kibana Space to other Kibana Spaces",response=GenericResponse.class)
	public ResponseEntity<GenericResponse> copyKibanaDashboard(
			@PathVariable("space_id") String spaceId,
			@PathVariable("dashboard_id") String dashboardId,
			@RequestBody DashboardCopy dashboardCopy)
	{
		if (spaceId==null || spaceId.trim().length()==0) {
			return ResponseEntity.badRequest().body(new GenericResponse(messages.getMessage("error.missingField", new Object[] {"space_id"}, LocaleContextHolder.getLocale())));
		}

		if (dashboardId==null || dashboardId.trim().length()==0) {
			return ResponseEntity.badRequest().body(new GenericResponse(messages.getMessage("error.missingField", new Object[] {"dashboard_id"}, LocaleContextHolder.getLocale())));
		}

		if (dashboardCopy==null || dashboardCopy.getTarget()==null || dashboardCopy.getTarget().length==0) {
			return ResponseEntity.badRequest().body(new GenericResponse(messages.getMessage("error.missingField", new Object[] {"target"}, LocaleContextHolder.getLocale())));
		}
		
		if (Arrays.stream(dashboardCopy.getTarget()).anyMatch(t->t==null || t.trim().length()==0)) {
			return ResponseEntity.badRequest().body(new GenericResponse(messages.getMessage("error.invalidField", new Object[] {"target"}, LocaleContextHolder.getLocale())));
		}
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
    	
    	boolean isSuperUser = userService.hasKibanaUserUserAccess(user);
    	if (!isSuperUser && !userService.hasDashboardWriteAccess(user))
			return ResponseEntity.badRequest().body(new GenericResponse(messages.getMessage("error.accessDenied", null, LocaleContextHolder.getLocale())));
    	
    	Set<ESStandardRoles> standardRoles = ESStandardRoles.getStandardRoles(user.getProfile());

    	// Check access to both source and target spaces
    	
		if (!isSuperUser) {
			
			String txid = (user.getTaxpayerId()==null || user.getTaxpayerId().trim().length()==0) ? null
					: user.getTaxpayerId().replaceAll("\\D", ""); // removes all non-numeric digits
			String personalSpaceId = (txid==null || txid.length()==0) ? null : "user-"+txid;

			if (!(personalSpaceId!=null && String.CASE_INSENSITIVE_ORDER.compare(personalSpaceId, spaceId)==0)
					&& !standardRoles.stream().noneMatch(r->r.hasDashboardPrivilege() && r.matchResource(spaceId))) {
				return ResponseEntity.badRequest().body(new GenericResponse(messages.getMessage("error.accessDenied", null, LocaleContextHolder.getLocale())));				
			}
			for (String target: dashboardCopy.getTarget()) {
				if (!(personalSpaceId!=null && String.CASE_INSENSITIVE_ORDER.compare(personalSpaceId, target)==0)
						&& !standardRoles.stream().noneMatch(r->r.hasDashboardPrivilege() && r.matchResource(target))) {
					return ResponseEntity.badRequest().body(new GenericResponse(messages.getMessage("error.accessDenied", null, LocaleContextHolder.getLocale())));				
				}				
			}
		}
		
		// Do the copy operation, copying dashboard and all related objects (index patterns and such)
		
		boolean errors = false;
		
		for (String target: dashboardCopy.getTarget()) {
			
			if (String.CASE_INSENSITIVE_ORDER.compare(spaceId, target)==0)
				continue; // do not copy to itself

			boolean success;
			try {
				success = ESUtils.copyKibanaSavedObjects(env, restTemplate, spaceId, target, "dashboard", new String[] { dashboardId });
			} catch (Exception ex) {
				success = false;
				log.log(Level.SEVERE, "Error copying dashboard "+dashboardId+" from "+spaceId+" to "+target, ex);
				errors = true;
			}
			
			if (success) {
				try {
					ESUtils.copyTransitiveDependencies(env, restTemplate, spaceId, target, "dashboard", dashboardId, /*max_iterations*/5);
				}
				catch (Exception ex) {
					log.log(Level.SEVERE, "Error copying transitive dependencies for dashboard "+dashboardId+" from "+spaceId+" to "+target, ex);
					errors = true;
				}
			}
			
		}

		if (errors)
			return ResponseEntity.badRequest().body(new GenericResponse(messages.getMessage("op.failed", null, LocaleContextHolder.getLocale())));		
		else
			return ResponseEntity.ok().body(new GenericResponse(messages.getMessage("op.sucessful", null, LocaleContextHolder.getLocale())));
	}

}
