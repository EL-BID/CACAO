/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.ui;

import java.util.Optional;

import org.idb.cacao.web.controllers.services.SyncAPIService;
import org.idb.cacao.web.controllers.services.SyncThread;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller class for all endpoints related to 'SYNC' requests interacting by a user interface
 * 
 * @author Gustavo Figueiredo
 *
 */
@Controller
public class SyncUIController {

	@Autowired
	private MessageSource messageSource;

	@Autowired
	private SyncAPIService syncAPIService;

	@Secured({"ROLE_SYNC_OPS"})
	@GetMapping("/sync/current")
	public String getSyncCurrentStatus(Model model, 
			@RequestParam("stopped") Optional<Boolean> stopped) {
		
		SyncThread running_sync = syncAPIService.getRunningSyncThread();
		if (running_sync!=null && running_sync.isRunning())
			model.addAttribute("running", Optional.ofNullable(running_sync));
		else
			model.addAttribute("running", Optional.empty());
		
		model.addAttribute("previous_sync", syncAPIService.hasPreviousSync());
		
		model.addAttribute("dateTimeFormat", messageSource.getMessage("timestamp.format", null, LocaleContextHolder.getLocale()));

        return "sync/sync_commits_milestones";
	}

	@Secured({"ROLE_SYNC_OPS"})
	@GetMapping("/sync/history")
	public String getSyncHistory(Model model, 
			@RequestParam("endpoint") String endpoint,
			@RequestParam("stopped") Optional<Boolean> stopped) {
		
		SyncThread running_sync = syncAPIService.getRunningSyncThread();
		if (running_sync!=null && running_sync.isRunning())
			model.addAttribute("running", Optional.ofNullable(running_sync));
		else
			model.addAttribute("running", Optional.empty());

		model.addAttribute("previous_sync", syncAPIService.hasPreviousSync(endpoint));

		model.addAttribute("dateTimeFormat", messageSource.getMessage("timestamp.format", null, LocaleContextHolder.getLocale()));

        return "sync/sync_commits_history";
	}

}
