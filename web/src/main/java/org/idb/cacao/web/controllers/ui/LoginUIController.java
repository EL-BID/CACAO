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

import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

import org.idb.cacao.web.utils.LoginUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller class for all endpoints related to user interface regarding 'login', 'home' 
 * and other initial screens.
 * 
 * @author Gustavo Figueiredo
 *
 */
@Controller
public class LoginUIController {

	@Autowired
	private Environment env;

	@GetMapping("/")
	public String frontend(Model model){
		
		return "login/index";
	}
	
	@GetMapping("/login")
	public String showLogin(Model model) {

		// Enable/disable some login options based on available configuration
		model.addAttribute("has_login_google", LoginUtils.hasGoogleProvider(env));
		model.addAttribute("has_login_azure", LoginUtils.hasAzureProvider(env));
		model.addAttribute("has_login_facebook", LoginUtils.hasFacebookProvider(env));

		return "login/login";
	}

	@GetMapping("/forgetPassword")
	public String showForgetPassword(Model model) {

		return "login/forgetPassword";
	}

	@GetMapping("/privacy")
	public String showPrivacy(Model model) {

		return "legal/privacy";
	}

	@GetMapping("/license")
	public String showLicense(Model model) {

		return "legal/license";
	}

	@GetMapping("/institutional")
	public String showInstitutional(Model model) {

		return "legal/institutional";
	}

	@GetMapping("/terms")
	public String showTerms(Model model) {

		return "legal/terms";
	}

	@GetMapping("/home")
	public String showHome(Model model, HttpServletRequest request) {
		
		// TODO: should return a hierarchy of menu items
		model.addAttribute("menu", Collections.emptyList());
		
		String first_page_child_frame = "/bulletin_board";

		model.addAttribute("first_page_child_frame", first_page_child_frame);

		return "login/home";
	}
	
	@GetMapping("/bulletin_board")
	public String getBulletinBoard(Model model) {
		
		// TODO: collect tax administration messages targetted to the logged user
		
		return "comm/bulletin_board";
	}	
}
