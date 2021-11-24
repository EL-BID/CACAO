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
package org.idb.cacao.web;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.idb.cacao.web.controllers.services.DomainTableService;
import org.idb.cacao.web.controllers.services.KeyStoreService;
import org.idb.cacao.web.controllers.services.KibanaSpacesService;
import org.idb.cacao.web.controllers.services.ResourceMonitorService;
import org.idb.cacao.web.controllers.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

/**
 * SpringBoot WebApplication entry point.
 * 
 * @author Gustavo Figueiredo
 *
 */
@SpringBootApplication
@ServletComponentScan
@ComponentScan(basePackages = {"org.idb.cacao.web","org.idb.cacao.api.storage"})
public class WebApplication {

	static final Logger log = Logger.getLogger(WebApplication.class.getName());

	@Autowired
	private UserService userService;

	@Autowired
	private KeyStoreService keyStoreService;

	@Autowired
	private ResourceMonitorService resourceMonitorService;
	
	@Autowired
	private DomainTableService domainTableService;	
	
	@Autowired
	private KibanaSpacesService kibanaSpacesService;

	@Autowired
	private Environment env;
	
	/**
	 * This is the entrypoint for the entire web application
	 */
	public static void main(String[] args) {
		
		SpringApplication.run(WebApplication.class, args);
	}

	/**
	 * Initialization code for the web application during SpringBoot initialization
	 */
	@PostConstruct
	public void doSomethingBeforeStartup() {

		keyStoreService.assertKeyStoreForSSL();
		// fileProducerService.sendBookkeepingFile();
		
		if ("true".equalsIgnoreCase(env.getProperty("use.kafka.embedded", "false"))) {
			startKafkaEmbedded();
		}

	}

	/**
	 * Initialization code for the web application after SpringBoot initialization
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void doSomethingAfterStartup() {

		new Thread("StartupThread") {
			{
				setDaemon(true);
			}

			public void run() {
				startupCode();
			}
		}.start();
	}

	/**
	 * Do some initialization here
	 */
	public void startupCode() {
		userService.assertInitialSetup();		

		try {
			if ("true".equalsIgnoreCase(env.getProperty("resource.monitor"))) {
				resourceMonitorService.start();
			}
		}
		catch (Throwable ex) {
			log.log(Level.SEVERE, "Error during initialization", ex);
		}

		try {
			boolean overwrite = "true".equalsIgnoreCase(env.getProperty("built-in.domain.tables.overwrite"));
			domainTableService.assertDomainTablesForAllArchetypes(overwrite);
		}
		catch (Throwable ex) {
			log.log(Level.SEVERE, "Error during initialization", ex);
		}
		
		try {
			if (kibanaSpacesService.getMinimumDocumentsForAutoCreateIndexPattern()>0)
				kibanaSpacesService.syncKibanaIndexPatterns();
		}
		catch (Throwable ex) {
			log.log(Level.SEVERE, "Error during synchronization of Kibana spaces with ElasticSearch indices", ex);
		}

	}
	
	/**
	 * Starts an 'embedded Kafka broker' (including Zookeeper). Should be used only at development environment.
	 */
	public static void startKafkaEmbedded() {

		try {
			EmbeddedKafkaBroker broker = new EmbeddedKafkaBroker(/*count brokers*/1, /*controlledShutdown*/true, /*partitions*/1)
			      .kafkaPorts(9092);
			broker.afterPropertiesSet();
		}catch (Throwable ex) { 
			log.log(Level.WARNING, "Error starting Embedded Kafka");
		}

	}
}
