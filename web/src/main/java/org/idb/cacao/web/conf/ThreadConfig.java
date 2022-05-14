/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configurations for our application's asynchronous threads execution
 * 
 * @author Gustavo Figueiredo
 *
 */
@Configuration
@EnableAsync
public class ThreadConfig {
	
	@Bean(name = "SyncTaskExecutor")
	public TaskExecutor syncTaskExecutor() {
		
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(1);
		executor.setMaxPoolSize(1);
		executor.setThreadNamePrefix("SyncTaskExecutor");
		executor.setDaemon(true);
		executor.initialize();
		
		return executor;
	}

	@Bean(name = "SyncTaskScheduler")
	public TaskScheduler syncTaskScheduler() {
		
		ThreadPoolTaskScheduler executor = new ThreadPoolTaskScheduler();
		executor.setPoolSize(1);
		executor.setThreadNamePrefix("SyncTaskScheduler");
		executor.setDaemon(true);
		executor.initialize();
		
		return executor;
	}

	@Bean(name = "AuditTrailTaskExecutor")
	public TaskExecutor auditTrailTaskExecutor() {
		
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(5);
		executor.setMaxPoolSize(10);
		executor.setThreadNamePrefix("AuditTrailTaskExecutor");
		executor.setDaemon(true);
		executor.initialize();
		
		return executor;
	}
}
