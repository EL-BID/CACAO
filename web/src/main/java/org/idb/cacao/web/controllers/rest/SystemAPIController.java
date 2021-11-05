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
