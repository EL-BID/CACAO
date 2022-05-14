/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.validator;

import org.idb.cacao.api.CommonApplication;
import org.idb.cacao.validator.controllers.services.ResourceMonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

/**
 * SpringBoot WebApplication entry point.
 * 
 * @author Gustavo Figueiredo
 *
 */
@SpringBootApplication
@ServletComponentScan
@ComponentScan(basePackages = {"org.idb.cacao.validator","org.idb.cacao.api.storage"})
public class Application extends CommonApplication {

	/**
	 * This is the entrypoint for the entire web application
	 */
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
	
	@Autowired
	public Application(Environment env, ResourceMonitorService validatorMonitorService) {
		super(env, validatorMonitorService);
	}

	/**
	 * Initialization code for the web application
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void doSomethingAfterValidatorStartup() {
		
		runStartupCodeAsync();
		
	}

}
