/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.ui;

import javax.validation.Valid;

import org.idb.cacao.web.GenericResponse;
import org.idb.cacao.web.controllers.services.ConfigSyncService;
import org.idb.cacao.web.controllers.services.SyncAPIService;
import org.idb.cacao.web.dto.ConfigSyncDto;
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
	@GetMapping("/config-sync")
    public String showConfigSync(Model model) {
		ConfigSync config = configSyncService.getActiveConfig();
		if (config==null)
			config = new ConfigSync();
		if (config.getApiToken()!=null && config.getApiToken().trim().length()>0)
			config.setApiToken(UNCHANGED_PASSWORD);	// never reveal password (not even the encrypted one)
		model.addAttribute("config", config);
		return "sync/config_sync";
	}

	@Secured("ROLE_SYNC_OPS")
    @PutMapping("/config-sync")
    @ResponseBody
    public GenericResponse updateSync(@Valid @RequestBody ConfigSyncDto config, BindingResult result) {
    	
        if (result.hasErrors()) {
        	return ControllerUtils.returnErrorsAsGenericResponse(result);
        }

    	final ConfigSync prevConfig = configSyncService.getActiveConfig();

        if (UNCHANGED_PASSWORD.equals(config.getApiToken()) || config.getApiToken()==null || config.getApiToken().trim().length()==0) {
        	if (prevConfig!=null)
        		config.setApiToken(prevConfig.getApiToken());
        }
        else if (config.getApiToken()!=null && config.getApiToken().length()>0) {
        	config.setApiToken(configSyncService.encryptToken(config.getApiToken()));
        }

        ConfigSync entity = new ConfigSync();
        config.updateEntity(entity);
        
        configSyncService.setActiveConfig(entity);
        
        if (ConfigSync.hasChangedScheduleInfo(prevConfig, entity)) {
        	syncAPIService.scheduleSyncThread();
        }
        
        return new GenericResponse("OK");
    }

}
