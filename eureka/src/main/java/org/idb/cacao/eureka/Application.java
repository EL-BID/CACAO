/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * SpringBoot WebApplication entry point.
 * 
 * @author Gustavo Figueiredo
 *
 */
@SpringBootApplication
@EnableEurekaServer
public class Application {

	/**
	 * This is the entrypoint for the entire web application
	 */
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
