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
package org.idb.cacao;

import static org.junit.Assert.assertTrue;

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



@AutoConfigureJsonTesters
@RunWith(JUnitPlatform.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@SpringBootTest( webEnvironment = WebEnvironment.DEFINED_PORT, classes = {org.idb.cacao.web.WebApplication.class})
public class IntegrationTests {
    
    private static final String ELASTICSEARCH_VERSION = "7.14.1";
    
    private static final Integer ELASTICSEARCH_PORT = 9200;

    // ES Container
    private static ElasticsearchContainer esContainer = 
        new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:" + ELASTICSEARCH_VERSION)
            .withEnv("discovery.type", "single-node")
            .withExposedPorts(ELASTICSEARCH_PORT);


    @BeforeAll
    public static void beforeAll() {
        esContainer.setWaitStrategy(Wait.forHttp("/"));
        esContainer.start();
        esContainer.getExposedPorts().forEach(port -> System.setProperty("es.port", String.valueOf(port)));

        int containerPort = esContainer.getMappedPort(ELASTICSEARCH_PORT);

        SpringApplicationBuilder cacaoWeb = 
            new SpringApplicationBuilder(org.idb.cacao.web.WebApplication.class).
            properties("es.port=" + containerPort);

        cacaoWeb.run("server.port=8888");

        SpringApplicationBuilder cacaoValidator = 
            new SpringApplicationBuilder(org.idb.cacao.validator.Application.class).properties("es.port=" + containerPort);
        
        cacaoValidator.run("server.port=8081");

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
