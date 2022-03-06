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


import org.idb.cacao.web.WebApplication;
import org.junit.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

/**
 * Unit test for simple App.
 */
@RunWith(JUnitPlatform.class)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@SpringBootTest( webEnvironment = WebEnvironment.RANDOM_PORT)
public class IntegrationTests {
    
    private static final String ELASTICSEARCH_VERSION = "7.14.1";

    private static ElasticsearchContainer esContainer = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:" + ELASTICSEARCH_VERSION);

    @BeforeAll
    public static void beforeAll() {
        esContainer.setWaitStrategy(Wait.forHttp("/"));
        esContainer.start();
        esContainer.getExposedPorts().forEach(port -> System.setProperty("es.port", String.valueOf(port)));
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
