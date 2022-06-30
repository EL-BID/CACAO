/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.ui;

import java.util.logging.Logger;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller class for all endpoints related to UI regarding legal terms.
 * 
 * @author Gustavo Figueiredo
 *
 */
@Controller
public class LegalUIController {

	static final Logger log = Logger.getLogger(LegalUIController.class.getName());

	/**
	 * Show HTML contents with Terms of Use
	 */
	@GetMapping("/terms")
	public String showTerms(Model model) {

		return "legal/terms";
	}

	/**
	 * Show HTML contents with Privacy Policy
	 */
	@GetMapping("/privacy")
	public String showPrivacy(Model model) {

		return "legal/privacy";
	}

	/**
	 * Show HTML contents with Software Licenses
	 */
	@GetMapping("/license")
	public String showLicense(Model model) {

		return "legal/license";
	}

	/**
	 * Show HTML contents with Architecture
	 */
	@GetMapping("/architecture")
	public String showArchitecture(Model model) {

		return "legal/architecture";
	}

}
