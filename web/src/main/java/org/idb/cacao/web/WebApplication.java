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

import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.idb.cacao.web.controllers.services.KeyStoreService;
import org.idb.cacao.web.controllers.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.event.EventListener;

/**
 * SpringBoot WebApplication entry point.
 * 
 * @author Gustavo Figueiredo
 *
 */
@SpringBootApplication
@ServletComponentScan
public class WebApplication {

	static final Logger log = Logger.getLogger(WebApplication.class.getName());
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private KeyStoreService keyStoreService;

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

	}

	/**
	 * Initialization code for the web application after SpringBoot initialization
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void doSomethingAfterStartup() {
		

	    new Thread("StartupThread") {
	    	{	setDaemon(true); }
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
		
	}
}
