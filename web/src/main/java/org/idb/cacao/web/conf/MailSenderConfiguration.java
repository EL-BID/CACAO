/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.conf;

import org.idb.cacao.web.controllers.services.IConfigEMailService;
import org.idb.cacao.web.controllers.ui.ConfigEMailUIController;
import org.idb.cacao.web.entities.ConfigEMail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Configuration related to use of SMTP service for sending e-mails
 * 
 * @author Gustavo Figueiredo
 *
 */
@Configuration
public class MailSenderConfiguration {

    @Autowired
    private IConfigEMailService configEmailService;

	@Bean
	public JavaMailSender getJavaMailSender() {
		
	    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
	    ConfigEMail config = configEmailService.getActiveConfig();
	    if (config!=null) {
	    	ConfigEMailUIController.configureMailSender(mailSender, configEmailService, config);
	    }
	    //props.put("mail.debug", "true");
	    
	    return mailSender;
	}

}
