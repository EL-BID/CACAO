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

import java.util.Properties;

import javax.validation.Valid;

import org.idb.cacao.web.GenericResponse;
import org.idb.cacao.web.controllers.services.ConfigEMailService;
import org.idb.cacao.web.controllers.services.IConfigEMailService;
import org.idb.cacao.web.entities.ConfigEMail;
import org.idb.cacao.web.utils.ControllerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller class for all endpoints related to 'ConfigEMail' object interacting by a user interface
 * 
 * @author Gustavo Figueiredo
 *
 */
@Controller
public class ConfigEMailUIController {
	
	private static final String UNCHANGED_PASSWORD = "$$$$UNCHANGED_PASSWORD$$$$$";

	@Autowired
	private ConfigEMailService configEMailService;
	
	@Autowired
	private JavaMailSender mailSender;

	@Secured("ROLE_CONFIG_SYSTEM_MAIL")
	@GetMapping("/config_email")
    public String showConfigEMail(Model model) {
		ConfigEMail config = configEMailService.getActiveConfig();
		if (config==null)
			config = new ConfigEMail();
		if (config.getPassword()!=null && config.getPassword().trim().length()>0)
			config.setPassword(UNCHANGED_PASSWORD);	// never reveal password (not even the encrypted one)
		model.addAttribute("config", config);
		return "config/smtp/config_email";
	}

	@Secured("ROLE_CONFIG_SYSTEM_MAIL")
    @PutMapping("/config_email")
    @ResponseBody
    public GenericResponse updateEMail(@Valid @RequestBody ConfigEMail config, BindingResult result) {
    	
        if (result.hasErrors()) {
        	return ControllerUtils.returnErrorsAsGenericResponse(result);
        }
        
        if (UNCHANGED_PASSWORD.equals(config.getPassword())) {
        	ConfigEMail prev_config = configEMailService.getActiveConfig();
        	if (prev_config!=null)
        		config.setPassword(prev_config.getPassword());
        }
        else if (config.getPassword()!=null && config.getPassword().length()>0) {
        	config.setPassword(configEMailService.encryptPassword(config.getPassword()));
        }
        
        configEMailService.setActiveConfig(config);
        
        configureMailSender((JavaMailSenderImpl)mailSender, configEMailService, config);
        
        return new GenericResponse("OK");
    }

	public static void configureMailSender(JavaMailSenderImpl mailSender, IConfigEMailService configEmailService, ConfigEMail config) {
	    mailSender.setHost(config.getHost());
	    mailSender.setPort(config.getPort());
	    mailSender.setProtocol(config.getProtocol().toString());
	    
	    mailSender.setUsername(config.getUsername());
	    String pwd = configEmailService.decryptPassword(config.getPassword());
	    mailSender.setPassword(pwd);
	    
	    Properties props = mailSender.getJavaMailProperties();
	    props.put("mail.transport.protocol", config.getProtocol().toString());
	    props.put("mail.smtp.auth", String.valueOf(config.isAuth()));
	    props.put("mail.smtp.starttls.enable", String.valueOf(config.isTls()));
	    if (config.getTimeout()>0)
	    	props.put("mail.smtp.timeout", String.valueOf(config.getTimeout()));		
	}
}
