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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.idb.cacao.web.controllers.services.TaxPayerGeneralViewService;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.errors.MissingParameter;
import org.idb.cacao.web.errors.UserNotFoundException;
import org.idb.cacao.web.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller class for all endpoints related to 'gerenal view' from tax payers
 * 
 * @author Rivelino Patrício
 *
 */
@Controller
public class TaxPayerGeneralViewUIController {

	private static final Logger log = Logger.getLogger(TaxPayerGeneralViewUIController.class.getName());

    @Autowired
    private MessageSource messages;

	@Autowired
	private Environment env;

	@Autowired
	private TaxPayerGeneralViewService taxPayerGeneralViewService;
	
	//@Secured({"ROLE_TAXPAYER_GENERAL_VIEW"})
	@GetMapping(value= {"/vertical-analysis"})
	public String getVerticalAnalysis(Model model) {
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
		
        return "taxpayersgeneralview/view-vertical-analysis";
	}
	
	//@Secured({"ROLE_TAXPAYER_GENERAL_VIEW"})
	@PostMapping(value= {"/vertical-analysis-data"})
	public void getVerticalAnalysisData(HttpServletResponse response, @PathVariable("personId") String personId) {
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
		
		// Parse the 'templateName' informed at request path
		if (personId==null || personId.trim().length()==0) {
			throw new MissingParameter("personId");
		}
		try {
			response.sendRedirect("/api/generalview/verticalanalysis?personId=" + personId);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Error while redirecting for show vertical analysis for taxpayer with id " + personId, e);
		}

	}	

}
