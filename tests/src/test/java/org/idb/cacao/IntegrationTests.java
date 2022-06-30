/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao;

import static org.junit.Assert.assertTrue;

import java.time.Duration;

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

    // ES Container
    @Container
    private static ElasticsearchContainer esContainer = 
        new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:" + ELASTICSEARCH_VERSION)
            .withEnv("discovery.type", "single-node")
            .withExposedPorts(ELASTICSEARCH_PORT);


    @BeforeAll
    public static void beforeAll() {
        esContainer.setWaitStrategy(Wait.forHttp("/")
            .forPort(9200)
            .forStatusCode(200)
            .withStartupTimeout(Duration.ofSeconds(120)));
        esContainer.start();

        int containerPort = esContainer.getMappedPort(ELASTICSEARCH_PORT);
        System.setProperty("es.port", "" + containerPort);
        System.setProperty("es.host", "127.0.0.1");
        System.setProperty("ssl.trust.server", esContainer.getHost());
        System.setProperty("i18n.locale", "en_US");
        System.setProperty("user.country", "en_US");
        System.setProperty("user.language", "en-US");
        System.setProperty("spring.mvc.locale", "en_US");
        
        SpringApplicationBuilder cacaoWeb = 
            new SpringApplicationBuilder(org.idb.cacao.web.WebApplication.class).
            profiles("dev").
            properties("es.port=" + containerPort).
            properties("es.host=127.0.0.1")
            .properties("ssl.trust.server=" + esContainer.getHost())
            .properties(
                "es.ssl=false",
                "es.ssl.verifyhost=false",
                "spring.elasticsearch.rest.connection-timeout=5m",
                "cacao.user.language=en_US",
                "cacao.user.country=US",
                "i18n.locale=en_US",
                "spring.mvc.locale=en_US",
                "privilege.ADMIN_OPS=SYSADMIN",
                "privilege.CONFIG_API_TOKEN=SYSADMIN,SUPPORT,DECLARANT",
                "privilege.CONFIG_SYSTEM_MAIL=SYSADMIN",
                "privilege.INTERPERSONAL_READ_ALL=SYSADMIN,SUPPORT,MASTER,AUTHORITY",
                "privilege.INTERPERSONAL_WRITE=SYSADMIN,SUPPORT,AUTHORITY",
                "privilege.SYNC_OPS=SYSADMIN",
                "privilege.TAX_DECLARATION_READ=SYSADMIN,SUPPORT,AUTHORITY,MASTER,DECLARANT",
                "privilege.TAX_DECLARATION_READ_ALL=SYSADMIN,SUPPORT,MASTER,AUTHORITY",
                "privilege.TAX_DECLARATION_WRITE=SYSADMIN,DECLARANT",
                "privilege.TAX_DECLARATION_WRITE_EMPTY=SYSADMIN,SUPPORT,MASTER",
                "privilege.TAX_TEMPLATE_WRITE=SYSADMIN,SUPPORT",
                "privilege.TAX_DOMAIN_TABLE_WRITE=SYSADMIN,SUPPORT",
                "privilege.TAXPAYER_READ=SYSADMIN,SUPPORT,AUTHORITY,MASTER,DECLARANT",
                "privilege.TAXPAYER_READ_ALL=SYSADMIN,SUPPORT,AUTHORITY,MASTER",
                "privilege.TAXPAYER_WRITE=SYSADMIN,SUPPORT",
                "privilege.USER_RECENT_READ=SYSADMIN",
                "privilege.USER_HISTORY_READ=SYSADMIN,SUPPORT",
                "privilege.USER_READ=SYSADMIN,SUPPORT",
                "privilege.USER_WRITE=SYSADMIN,SUPPORT",
                "privilege.TAX_REPORT_READ=SYSADMIN,SUPPORT,AUTHORITY,MASTER"
                /*,
                "server.ssl.key-store-type=PKCS12",
                "server.ssl.key-store=file:/f/cacao/cert.p12",
                "server.ssl.key-store-password=123456",
                "server.ssl.key-alias=cacao",
                "server.ssl.enabled=true",
                "spring.thymeleaf.cache=false",
                "auto.reload.properties=10",
                "use.kafka.embedded=true",
                "storage.incoming.files.original.dir=f:/cacao"*/
                
            );

        cacaoWeb.run("server.port=8888");



        /*
        SpringApplicationBuilder cacaoValidator = 
            new SpringApplicationBuilder(org.idb.cacao.validator.Application.class).properties("es.port=" + containerPort);
        
        cacaoValidator.run("server.port=8081");
        */
        

    }

	@AfterAll
	public static void afterClass() {
		if (esContainer !=null)
            esContainer.stop();

	}

    @Test
    public void esShouldBeUpAndRunning() {
        assertTrue(esContainer.isRunning());
    }

}
