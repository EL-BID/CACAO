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
package org.idb.cacao.web.controllers.ui;

import javax.validation.Valid;

import org.idb.cacao.web.GenericResponse;
import org.idb.cacao.web.controllers.services.ConfigSyncService;
import org.idb.cacao.web.controllers.services.SyncAPIService;
import org.idb.cacao.web.entities.ConfigSync;
import org.idb.cacao.web.utils.ControllerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller class for all endpoints related to 'ConfigSync' object interacting by a user interface
 * 
 * @author Gustavo Figueiredo
 *
 */
@Controller
public class ConfigSyncUIController {

	private static final String UNCHANGED_PASSWORD = "$$$$UNCHANGED_PASSWORD$$$$$";

	@Autowired
	ConfigSyncService configSyncService;
	
	@Autowired
	SyncAPIService syncAPIService;

	@Secured("ROLE_SYNC_OPS")
	@GetMapping("/config_sync")
    public String showConfigSync(Model model) {
		ConfigSync config = configSyncService.getActiveConfig();
		if (config==null)
			config = new ConfigSync();
		if (config.getApiToken()!=null && config.getApiToken().trim().length()>0)
			config.setApiToken(UNCHANGED_PASSWORD);	// never reveal password (not even the encrypted one)
		model.addAttribute("config", config);
		return "config_sync";
	}

	@Secured("ROLE_SYNC_OPS")
    @PutMapping("/config_sync")
    @ResponseBody
    public GenericResponse updateSync(@Valid @RequestBody ConfigSync config, BindingResult result) {
    	
        if (result.hasErrors()) {
        	return ControllerUtils.returnErrorsAsGenericResponse(result);
        }

    	final ConfigSync prev_config = configSyncService.getActiveConfig();

        if (UNCHANGED_PASSWORD.equals(config.getApiToken()) || config.getApiToken()==null || config.getApiToken().trim().length()==0) {
        	if (prev_config!=null)
        		config.setApiToken(prev_config.getApiToken());
        }
        else if (config.getApiToken()!=null && config.getApiToken().length()>0) {
        	config.setApiToken(configSyncService.encryptToken(config.getApiToken()));
        }

        configSyncService.setActiveConfig(config);
        
        if (ConfigSync.hasChangedScheduleInfo(prev_config, config)) {
        	syncAPIService.scheduleSyncThread();
        }
        
        return new GenericResponse("OK");
    }

}
