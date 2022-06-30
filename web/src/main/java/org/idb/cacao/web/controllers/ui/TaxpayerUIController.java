/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.ui;

import org.idb.cacao.api.Taxpayer;
import org.idb.cacao.web.repositories.TaxpayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Controller class for all endpoints related to 'taxpayer' object interacting by a user interface
 * 
 * @author Gustavo Figueiredo
 *
 */
@Controller
public class TaxpayerUIController {

	@Autowired
	private TaxpayerRepository taxPayerRepository;
	
	@Secured({"ROLE_TAXPAYER_READ"})
	@GetMapping("/taxpayers")
	public String getTaxpayers(Model model) {
		return "taxpayers/taxpayers";
	}

	@Secured({"ROLE_TAXPAYER_WRITE"})
	@GetMapping("/taxpayers/add")
    public String showAddTaxpayer(Model model) {
		model.addAttribute("taxpayer", new Taxpayer());
		return "taxpayers/add-taxpayer";
	}

	@Secured({"ROLE_TAXPAYER_WRITE"})
    @GetMapping("/taxpayers/{id}/edit")
    public String showUpdateForm(@PathVariable("id") String id, Model model) {
		Taxpayer taxpayer = taxPayerRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid taxpayer Id:" + id));
        model.addAttribute("taxpayer", taxpayer);
        return "taxpayers/update-taxpayer";
    }

	@Secured({"ROLE_TAXPAYER_WRITE"})
	@GetMapping("/taxpayers/{id}")
    public String showTaxpayerDetails(@PathVariable("id") String id, Model model) {
		Taxpayer taxpayer = taxPayerRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid taxpayer Id:" + id));
        model.addAttribute("taxpayer", taxpayer);
        return "taxpayers/view-taxpayer";
    }
	
	public static String nvl(String v) {
		if (v==null)
			return "";
		return v;
	}
}
