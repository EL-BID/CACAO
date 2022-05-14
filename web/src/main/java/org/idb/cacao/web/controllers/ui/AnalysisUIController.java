/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.ui;

import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.errors.UserNotFoundException;
import org.idb.cacao.web.sec.SecurityConfig;
import org.idb.cacao.web.utils.UserUtils;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller class for all endpoints related to 'analysis view' from tax payers
 * 
 * @author Rivelino Patrício
 *
 */
@Controller
public class AnalysisUIController {
	
	static final Logger log = Logger.getLogger(AnalysisUIController.class.getName());

	@Secured({"ROLE_TAX_REPORT_READ"})
	@GetMapping(value= {"/vertical-horizontal-analysis"})
	public String getVerticalHorizontalAnalysis(Model model) {
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
		
        return "analysis/vertical_horizontal_analysis";
	}	
	
	@Secured({"ROLE_TAX_REPORT_READ"})
	@GetMapping(value= {"/general-analysis"})
	public String getGeneralAnalysis(Model model, HttpServletResponse response) {
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
    	
    	SecurityConfig.setVegaCompatibleCSPDirective(response);

        return "analysis/general_analysis";
	}		

	@Secured({"ROLE_TAX_REPORT_READ"})
	@GetMapping(value= {"/accounting-flows"})
	public String getAccountingFlows(Model model) {
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
		
        return "analysis/accounting_flows";
	}
	@Secured({"ROLE_TAX_REPORT_READ"})
	@GetMapping(value= {"/statement-income-analysis"})
	public String getStatementIncomeAnalysis(Model model, HttpServletResponse response) {
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
		
    	SecurityConfig.setVegaCompatibleCSPDirective(response);
    	
        return "analysis/statement_income_analysis";
	}
	
	@Secured({"ROLE_TAX_REPORT_READ"})
	@GetMapping(value= {"/taxpayer-general-view"})
	public String getTaxpayerGeneralView(Model model, HttpServletResponse response) {
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();

    	SecurityConfig.setVegaCompatibleCSPDirective(response);

        return "analysis/taxpayer_general_view";
	}
	
	@Secured({"ROLE_TAX_REPORT_READ"})
	@GetMapping(value= {"/customers-vs-suppliers-analysis"})
	public String getClientsVsSuppliersView(Model model, HttpServletResponse response) {
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();

    	SecurityConfig.setVegaCompatibleCSPDirective(response);

        return "analysis/customers_vs_suppliers_analysis";
	}	
}
