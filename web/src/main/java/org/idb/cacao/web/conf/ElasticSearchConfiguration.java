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
package org.idb.cacao.web.conf;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Configurations for using ElasticSearch as repository for all persistent entities
 * 
 * @author Gustavo Figueiredo
 *
 */
@Configuration
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class})
@EnableElasticsearchRepositories(basePackages = "org.idb.cacao.web.repositories")
@ComponentScan(basePackages = { "org.idb.cacao.web.controllers" })
public class ElasticSearchConfiguration {

	@Autowired
	private Environment env;

	/**
	 * Bean for usage of ElasticSearch REST API directly
	 */
    @Bean
    public RestHighLevelClient client() {
    	
    	// Set ElasticSearch hostname and port number accordingly to application properties configuration (may be overriden
    	// by command line options as well, check 'setup.sh' if running under Docker image)
		String esUrl = String.format("%s:%s", 
				env.getProperty("es.host"), 
				env.getProperty("es.port"));

		ClientConfiguration clientConfiguration =
				(isSSL()) ? ClientConfiguration.builder().connectedTo(esUrl).usingSsl().build()
						:	ClientConfiguration.builder().connectedTo(esUrl).build();

        return RestClients.create(clientConfiguration)
            .rest();
    }

    @Bean
    public ElasticsearchRestTemplate elasticsearchTemplate() {
        return new ElasticsearchRestTemplate(client());
    }

    /**
     * Indicates if ElasticSearch requires SSL according to the application properties configuration (may be overriden
     * by command line options as well, check 'setup.sh' if running under Docker image)
     */
    public boolean isSSL() {
    	String es_port = env.getProperty("es.port");
    	return "443".equals(es_port);
    }
	
}
