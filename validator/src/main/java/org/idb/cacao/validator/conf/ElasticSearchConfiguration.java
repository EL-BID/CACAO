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
package org.idb.cacao.validator.conf;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.client.ClientConfiguration.MaybeSecureClientConfigurationBuilder;
import org.springframework.data.elasticsearch.client.ClientConfiguration.TerminalClientConfigurationBuilder;
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
@EnableElasticsearchRepositories(basePackages = "org.idb.cacao.validator.repositories")
@ComponentScan(basePackages = { "org.idb.cacao.validator.controllers" })
public class ElasticSearchConfiguration {

	@Autowired
	private Environment env;

	@Value("${spring.elasticsearch.rest.connection-timeout}")
	private String elasticSearchConnectionTimeout;

	/**
	 * Bean for usage of ElasticSearch REST API directly
	 */
    @Bean
    public RestHighLevelClient client() {
    	
		String esUrl = String.format("%s:%s", 
				env.getProperty("es.host"), 
				env.getProperty("es.port"));

		TerminalClientConfigurationBuilder clientConfigurationBuilder;
				
		if (isSSL()) {
			MaybeSecureClientConfigurationBuilder builder = ClientConfiguration.builder().connectedTo(esUrl);
			if (isSSLVerifyHost()) {
				clientConfigurationBuilder = builder.usingSsl();
			}
			else {
				try {
					TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
				    SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
					clientConfigurationBuilder = builder.usingSsl(sslContext, NoopHostnameVerifier.INSTANCE);
				} catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
					throw new RuntimeException(e);
				}
			}
		}
		else {
			clientConfigurationBuilder = ClientConfiguration.builder().connectedTo(esUrl);
		}
				
		String username = env.getProperty("es.user");
		String password = env.getProperty("es.password");
		if (username!=null && username.trim().length()>0) {
			clientConfigurationBuilder.withBasicAuth(username, password);
		}
		
		clientConfigurationBuilder.withConnectTimeout(Duration.parse("PT"+elasticSearchConnectionTimeout.toUpperCase()));
		clientConfigurationBuilder.withSocketTimeout(Duration.parse("PT"+elasticSearchConnectionTimeout.toUpperCase()));

		ClientConfiguration clientConfiguration = clientConfigurationBuilder.build();
		
        return RestClients.create(clientConfiguration)
            .rest();
    }

    @Bean
    public ElasticsearchRestTemplate elasticsearchTemplate() {
        return new ElasticsearchRestTemplate(client());
    }

    public boolean isSSL() {
    	if ("true".equalsIgnoreCase(env.getProperty("es.ssl")))
    		return true;
    	String es_port = env.getProperty("es.port");
    	return "443".equals(es_port);
    }
    
    public boolean isSSLVerifyHost() {
    	return !"false".equalsIgnoreCase(env.getProperty("es.ssl.verifyhost"));
    }
	
}
