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
