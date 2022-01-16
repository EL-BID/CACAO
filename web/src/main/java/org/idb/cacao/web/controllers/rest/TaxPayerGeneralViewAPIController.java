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

import org.idb.cacao.api.utils.DateTimeUtils;
import org.idb.cacao.web.controllers.services.TaxPayerGeneralViewService;
import org.idb.cacao.web.dto.Account;
import org.idb.cacao.web.dto.BalanceSheet;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.errors.UserNotFoundException;
import org.idb.cacao.web.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller class for all endpoints related to 'gerenal view' from tax payers
 * 
 * @author Rivelino Patrício
 *
 */
@RestController
@RequestMapping("/api")
@Tag(name="taxpayer-general-view-api-controller", description="Controller class for all endpoints related to 'TaxPayer' general view interacting by a REST interface.")
public class TaxPayerGeneralViewAPIController {

	private static final Logger log = Logger.getLogger(TaxPayerGeneralViewAPIController.class.getName());

    @Autowired
    private MessageSource messages;

	@Autowired
	private Environment env;

	@Autowired
	private TaxPayerGeneralViewService taxPayerGeneralViewService;
	
	//@Secured({"ROLE_TAXPAYER_GENERAL_VIEW"})
	@GetMapping(value= {"/generalview/vertical-analysis"})
	public ResponseEntity<Object> getVerticalAnalysis(@RequestParam("personId") String personId) {
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
    	
    	BalanceSheet balance = new BalanceSheet();
    	balance.setTaxPayerId("1234567");
    	balance.setInitialDate(DateTimeUtils.now());
    	balance.setFinalDate(DateTimeUtils.now());
    	
    	Account account = new Account();
    	account.setLevel(1);
    	account.setCategory("Asset");    	
    	account.setDescription("Asset");
    	account.setCode("1");
    	account.setInitialBalance(100);
    	account.setFinalBalance(120);
    	account.setPercentage(100);
    	balance.addAccount(account);
    	
    	account = new Account();
    	account.setLevel(2);
    	account.setCategory("Asset");
    	account.setSubcategory("Cash and Cash Equivalents");
    	account.setDescription("Cash");
    	account.setCode("1.1");
    	account.setInitialBalance(100);
    	account.setFinalBalance(120);
    	account.setPercentage(100);    	
    	balance.addAccount(account);
    	
    	account = new Account();
    	account.setLevel(3);
    	account.setCategory("Asset");
    	account.setSubcategory("Cash and Cash Equivalents");
    	account.setCode("1.1.1");
    	account.setDescription("Money");
    	account.setInitialBalance(20);
    	account.setFinalBalance(20);
    	account.setPercentage(20);    	
    	balance.addAccount(account);
    	
    	account = new Account();
    	account.setLevel(3);
    	account.setCategory("Asset");
    	account.setSubcategory("Cash and Cash Equivalents");
    	account.setCode("1.1.2");
    	account.setDescription("Bank");
    	account.setInitialBalance(80);
    	account.setFinalBalance(100);
    	account.setPercentage(80);    	
    	balance.addAccount(account);
	
    	return ResponseEntity.ok().body(balance);    	
	}

}
