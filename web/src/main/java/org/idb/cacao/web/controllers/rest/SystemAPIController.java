package org.idb.cacao.web.controllers.rest;

import java.util.logging.Logger;

import org.idb.cacao.web.controllers.services.ResourceMonitorService;
import org.idb.cacao.web.dto.ComponentSystemInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller class for all endpoints related to 'Communication' object interacting by a REST interface
 * 
 * @author Gustavo Figueiredo
 *
 */
@RestController
@RequestMapping("/api")
@Tag(name="system information service", description="The API for retrieving system information")
public class SystemAPIController {

	static final Logger log = Logger.getLogger(SystemAPIController.class.getName());

    @Autowired
    private ResourceMonitorService sysInfoService;

	/**
	 * Collects information about the 'web' component
	 */
    @GetMapping(value="/sys_info", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Collects information about this component",response=ComponentSystemInformation.class)	
	public ResponseEntity<ComponentSystemInformation> collectInfoForComponent() {
		
    	ComponentSystemInformation info = sysInfoService.collectInfoForComponent();
		
		return ResponseEntity.ok(info);
	}

}
