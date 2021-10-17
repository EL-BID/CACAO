package org.idb.cacao.validator.controllers.rest;

import java.util.logging.Logger;

import org.idb.cacao.validator.controllers.services.ResourceMonitorService;
import org.idb.cacao.validator.dto.ComponentSystemInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller class for all endpoints related to 'System Information' by a REST interface
 * 
 * @author Gustavo Figueiredo
 *
 */
@RestController
@RequestMapping("/api")
public class SystemAPIController {

	static final Logger log = Logger.getLogger(SystemAPIController.class.getName());

    @Autowired
    private ResourceMonitorService sysInfoService;

	/**
	 * Collects information about the 'web' component
	 */
    @GetMapping(value="/sys_info", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ComponentSystemInformation> collectInfoForComponent() {
		
    	ComponentSystemInformation info = sysInfoService.collectInfoForComponent();
		
		return ResponseEntity.ok(info);
	}

}
