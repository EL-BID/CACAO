/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.validator.conf;

import org.elasticsearch.client.RestHighLevelClient;
import org.idb.cacao.api.utils.ElasticClientFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
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

	/**
	 * Bean for usage of ElasticSearch REST API directly
	 */
    @Bean
    public RestHighLevelClient client(Environment env) {
    	return new ElasticClientFactory.Builder(env).build();
    }

    @Bean
    public ElasticsearchRestTemplate elasticsearchTemplate(RestHighLevelClient client) {
        return new ElasticsearchRestTemplate(client);
    }
	
}
