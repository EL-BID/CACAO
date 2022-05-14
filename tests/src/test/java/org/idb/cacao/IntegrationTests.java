/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.time.Duration;
import java.util.Locale;

import org.junit.ClassRule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


@Testcontainers
@AutoConfigureJsonTesters
@RunWith(JUnitPlatform.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@SpringBootTest( webEnvironment = WebEnvironment.DEFINED_PORT, classes = {org.idb.cacao.web.WebApplication.class})
public class IntegrationTests {
    
    private static final String ELASTICSEARCH_VERSION = "7.14.1";
    
    private static final Integer ELASTICSEARCH_PORT = 9200;

    @ClassRule
    public static DockerComposeContainer environment =
        new DockerComposeContainer(new File("compose-test.yml"));
                //.withExposedService("web_1", 8080);


    @BeforeAll
    public static void beforeAll() {
        environment.start();
        System.out.println("Environment started");
    }

	@AfterAll
	public static void afterClass() {
		if (environment !=null)
            environment.stop();

	}

    @Test
    public void esShouldBeUpAndRunning() {
        System.out.println("Environment started");
        System.out.println("Environment started");
        //assertTrue(environment.isRunning());
        assertTrue(environment == null);
    }

}
