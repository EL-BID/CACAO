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

import org.idb.cacao.api.Taxpayer;
import org.idb.cacao.web.controllers.services.FieldsConventionsService;
import org.idb.cacao.web.dto.MenuItem;
import org.idb.cacao.web.entities.Interpersonal;
import org.idb.cacao.web.entities.RelationshipType;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.errors.UserNotFoundException;
import org.idb.cacao.web.repositories.InterpersonalRepository;
import org.idb.cacao.web.repositories.TaxpayerRepository;
import org.idb.cacao.web.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Controller class for all endpoints related to 'interpersonal relationship' object interacting by a user interface
 * 
 * @author Gustavo Figueiredo
 *
 */
@Controller
public class InterpersonalUIController {

    @Autowired
    private MessageSource messages;


	@Autowired
	private InterpersonalRepository interpersonalRepository;
	
	@Autowired
	private TaxpayerRepository taxpayerRepository;
	
	@Autowired
	private FieldsConventionsService fieldsConventionsService;
	
	@Secured({"ROLE_INTERPERSONAL_READ_ALL"})
	@GetMapping(value= {"/interpersonals"})
	public String getInterpersonalRelationships(Model model) {
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
		model.addAttribute("types", RelationshipType.values());
        return "interpersonal/interpersonals";
	}

	@Secured({"ROLE_INTERPERSONAL_WRITE"})
	@GetMapping(value= {"/interpersonals/add"})
    public String showAddInterpersonalRelationship(@PathVariable Optional<String> type, Model model) {
		RelationshipType relType = null;
		if (type.isPresent()) {
			relType = RelationshipType.parse(type.get());
		}
		model.addAttribute("type", Optional.ofNullable(relType));
		Interpersonal newRel = new Interpersonal();
		newRel.setRelationshipType(relType);
		model.addAttribute("interpersonal",newRel);
		return "interpersonal/add-interpersonal";
	}

	@Secured({"ROLE_INTERPERSONAL_READ_ALL"})
    public String showInterpersonalRelationshipDetails(@PathVariable("id") String id, Model model) {
		Interpersonal interpersonal = interpersonalRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid interpersonal Id:" + id));
        model.addAttribute("interpersonal", interpersonal);
        MenuItem interpersonal_details = new MenuItem();
        interpersonal_details.addChild(new MenuItem(messages.getMessage("rel.person1", null, LocaleContextHolder.getLocale())).withChild(interpersonal.getPersonId1()));
        interpersonal_details.addChild(new MenuItem(messages.getMessage("rel.person1.name", null, LocaleContextHolder.getLocale())).withChild(getTaxPayerName(interpersonal.getPersonId1()).orElse(null)));
        if (interpersonal.getRelationshipType()!=null)
        	interpersonal_details.addChild(new MenuItem(messages.getMessage("rel.type", null, LocaleContextHolder.getLocale())).withChild(messages.getMessage(interpersonal.getRelationshipType().toString(), null, LocaleContextHolder.getLocale())));
        interpersonal_details.addChild(new MenuItem(messages.getMessage("rel.person2", null, LocaleContextHolder.getLocale())).withChild(interpersonal.getPersonId2()));
        interpersonal_details.addChild(new MenuItem(messages.getMessage("rel.person2.name", null, LocaleContextHolder.getLocale())).withChild(getTaxPayerName(interpersonal.getPersonId2()).orElse(null)));
        interpersonal_details.addChild(new MenuItem(messages.getMessage("rel.user", null, LocaleContextHolder.getLocale())).withChild(interpersonal.getUser()));
        interpersonal_details.addChild(new MenuItem(messages.getMessage("rel.timestamp", null, LocaleContextHolder.getLocale())).withChild(fieldsConventionsService.formatValue(interpersonal.getTimestamp())));
        interpersonal_details.addChild(new MenuItem(messages.getMessage("rel.removedTimestamp", null, LocaleContextHolder.getLocale())).withChild(fieldsConventionsService.formatValue(interpersonal.getRemovedTimestamp())));
        model.addAttribute("interpersonal_details", interpersonal_details);
        return "view-interpersonal";
    }

	/**
	 * Given taxpayer ID, returns its NAME
	 */
	public Optional<String> getTaxPayerName(String taxPayerId) {
		if (taxPayerId==null || taxPayerId.length()==0)
			return Optional.empty();
		Optional<Taxpayer> taxPayer = taxpayerRepository.findByTaxPayerId(taxPayerId);
		return taxPayer.map(Taxpayer::toString);
	}

}
