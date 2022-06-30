/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.ui;

import java.util.Properties;

import javax.validation.Valid;

import org.idb.cacao.web.GenericResponse;
import org.idb.cacao.web.controllers.services.ConfigEMailService;
import org.idb.cacao.web.controllers.services.IConfigEMailService;
import org.idb.cacao.web.dto.ConfigEMailDto;
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
	@GetMapping("/config-email")
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
    @PutMapping("/config-email")
    @ResponseBody
    public GenericResponse updateEMail(@Valid @RequestBody ConfigEMailDto config, BindingResult result) {
    	
        if (result.hasErrors()) {
        	return ControllerUtils.returnErrorsAsGenericResponse(result);
        }
        
        if (UNCHANGED_PASSWORD.equals(config.getPassword())) {
        	ConfigEMail prevConfig = configEMailService.getActiveConfig();
        	if (prevConfig!=null)
        		config.setPassword(prevConfig.getPassword());
        }
        else if (config.getPassword()!=null && config.getPassword().length()>0) {
        	config.setPassword(configEMailService.encryptPassword(config.getPassword()));
        }
        
        ConfigEMail entity = new ConfigEMail();
        config.updateEntity(entity);
        configEMailService.setActiveConfig(entity);
        
        configureMailSender((JavaMailSenderImpl)mailSender, configEMailService, entity);
        
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
