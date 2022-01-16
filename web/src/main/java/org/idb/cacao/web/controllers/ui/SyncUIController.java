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
