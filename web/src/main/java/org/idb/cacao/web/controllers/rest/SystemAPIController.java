/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.rest;

import java.util.logging.Logger;

import org.idb.cacao.api.ComponentSystemInformation;
import org.idb.cacao.web.controllers.services.ResourceMonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller class for all endpoints related to 'System Information' by a REST interface
 * 
 * @author Gustavo Figueiredo
 *
 */
@RestController
@RequestMapping("/api")
@Tag(name="system-api-controller", description="The API for retrieving system information")
public class SystemAPIController {

	static final Logger log = Logger.getLogger(SystemAPIController.class.getName());

    @Autowired
    private ResourceMonitorService sysInfoService;

	/**
	 * Collects information about the 'web' component
	 */
    @GetMapping(value="/sys-info", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Collects information about this component",response=ComponentSystemInformation.class)	
	public ResponseEntity<ComponentSystemInformation> collectInfoForComponent() {
		
    	ComponentSystemInformation info = sysInfoService.collectInfoForComponent();
		
		return ResponseEntity.ok(info);
	}

}
