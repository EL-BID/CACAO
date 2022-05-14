/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.ui;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.idb.cacao.web.controllers.services.InternalHttpRequestsService;
import org.idb.cacao.web.controllers.services.UserService;
import org.idb.cacao.web.dto.Dashboard;
import org.idb.cacao.web.dto.Space;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.errors.InsufficientPrivilege;
import org.idb.cacao.web.errors.UserNotFoundException;
import org.idb.cacao.web.repositories.ESStandardRoles;
import org.idb.cacao.web.utils.ESUtils;
import org.idb.cacao.web.utils.ESUtils.KibanaSavedObject;
import org.idb.cacao.web.utils.ESUtils.KibanaSpace;
import org.idb.cacao.web.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.env.Environment;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

/**
 * Controller class for all endpoints related to UI regarding dashboard management (except for those entrypoints implemented in KIBANA).
 * 
 * @author Gustavo Figueiredo
 *
 */
@Controller
public class DashboardsUIController {

	private static final Logger log = Logger.getLogger(DashboardsUIController.class.getName());

	@Autowired
	private UserService userService;

	@Autowired
	private Environment env;

    private RestTemplate restTemplate;

	@Autowired
	public DashboardsUIController(RestTemplateBuilder builder, InternalHttpRequestsService requestFactory) {
		this.restTemplate = builder
				.setConnectTimeout(Duration.ofMinutes(5))
				.setReadTimeout(Duration.ofMinutes(5))
				.requestFactory(requestFactory)
				.build();
	}

	/**
	 * Returns a list of all dashboards accessible by a user
	 */
	@Secured("ROLE_TAX_DECLARATION_READ_ALL")
	@GetMapping("/dashboards-list")
    public String showDashboardsList(Model model) {
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
    	
    	boolean isSuperUser = userService.hasKibanaUserUserAccess(user);
    	if (!isSuperUser && !userService.hasDashboardWriteAccess(user))
    		throw new InsufficientPrivilege("error.accessDenied");
    	
    	Set<ESStandardRoles> standard_roles = ESStandardRoles.getStandardRoles(user.getProfile());

		List<KibanaSpace> spaces;
		try {
			spaces = ESUtils.getKibanaSpaces(env, restTemplate);
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Error while retrieving list of Kibana spaces", ex);
			spaces = Collections.emptyList();
		}
		
		if (!isSuperUser) {
			String txid = (user.getTaxpayerId()==null || user.getTaxpayerId().trim().length()==0) ? null
					: user.getTaxpayerId().replaceAll("\\D", ""); // removes all non-numeric digits
			String personalSpaceId = (txid==null || txid.length()==0) ? null : "user-"+txid;
			spaces = spaces.stream().filter(space->{
				return (personalSpaceId!=null && String.CASE_INSENSITIVE_ORDER.compare(personalSpaceId, space.getId())==0)
						|| standard_roles.stream().anyMatch(r->r.hasDashboardPrivilege() && r.matchResource(space.getId()));
			}).collect(Collectors.toList());
		}
		
		List<KibanaSavedObject> dashboards =
			spaces.stream().flatMap(space->{
				try {
					return ESUtils.getKibanaDashboards(env, restTemplate, space.getId()).stream();
				} catch (Exception e) {
					return Collections.<KibanaSavedObject>emptyList().stream();
				}
			})
			.collect(Collectors.toList());
		
		Map<String,String> mapSpacesNames = spaces.stream().collect(Collectors.toMap(
				/*keyMapper*/KibanaSpace::getId, 
				/*valueMapper*/KibanaSpace::getName, 
				/*mergeFunction*/(a,b)->a));
		
		List<Dashboard> dashboardsUI = dashboards.stream()
				.map(d->{
					Dashboard dashUI = new Dashboard();
					dashUI.setId(d.getId());
					dashUI.setSpaceId(d.getNamespace());
					dashUI.setSpaceName(mapSpacesNames.get(d.getNamespace()));
					dashUI.setTitle(d.getTitle());
					dashUI.setUrl(getDashboardURL(d));
					return dashUI;
				}).sorted().collect(Collectors.toList());
		
		model.addAttribute("dashboards", dashboardsUI);
		
		model.addAttribute("spaces", spaces.stream()
				.map(s->{
					Space space = new Space();
					space.setId(s.getId());
					space.setName(s.getName());
					return space;
				})
				.sorted()
				.collect(Collectors.toList()));
		
		return "dashboards/dashboards_list";
	}

	/**
	 * Return a valid URL for a dashboard at KIBANA.
	 */
	public String getDashboardURL(KibanaSavedObject dashboard) {
		if (dashboard.getNamespace()!=null && dashboard.getNamespace().trim().length()>0
				&& !dashboard.getNamespace().equalsIgnoreCase("default")) {
			return env.getProperty("kibana.menu.link")+"/s/"+dashboard.getNamespace()+"/app/dashboards#/view/"+dashboard.getId();
		}
		else {
			return env.getProperty("kibana.menu.link")+"/app/dashboards#/view/"+dashboard.getId();
		}
	}
}
