/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.validator.controllers.services;

import org.idb.cacao.api.utils.ResourceMonitor;
import org.idb.cacao.validator.entities.ValidatorSystemMetrics;
import org.idb.cacao.validator.repositories.SystemMetricsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for collecting system resources metrics and reporting externally. Useful for diagnostics about system health status.
 * 
 * @author Gustavo Figueiredo
 *
 */
@Service
public class ResourceMonitorService extends ResourceMonitor<ValidatorSystemMetrics> {
	
	@Autowired
	public ResourceMonitorService(SystemMetricsRepository systemMetricsRepository) {
		super(systemMetricsRepository, ValidatorSystemMetrics::new);
	}

}
