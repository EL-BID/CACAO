/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.services;

import java.util.Properties;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.utils.ResourceMonitor;
import org.idb.cacao.web.entities.WebSystemMetrics;
import org.idb.cacao.web.repositories.SystemMetricsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Service for collecting system resources metrics and reporting externally. Useful for diagnostics about system health status.
 * 
 * @author Gustavo Figueiredo
 *
 */
@Service
public class ResourceMonitorService extends ResourceMonitor<WebSystemMetrics> {

	@Autowired
	private Environment env;

	@Autowired
	public ResourceMonitorService(SystemMetricsRepository systemMetricsRepository) {
		super(systemMetricsRepository, WebSystemMetrics::new);
	}

	/**
	 * Returns object for admin operations regarding Kafka Cluster
	 */
	public AdminClient getKafkaAdminClient() {
		Properties properties = new Properties();
		properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, env.getProperty("spring.cloud.stream.kafka.binder.brokers"));
		AdminClient adminClient = AdminClient.create(properties);
		return adminClient;
	}

	/**
	 * Given a Kafka object wrapping a Kafka metric, returns the value stored in this metric formatted as String
	 */
	public static String getKafkaMetricDesc(Metric metric) {
		if (metric==null)
			return null;
		if (metric instanceof KafkaMetric) {
			KafkaMetric km = (KafkaMetric)metric;
			if (km.metricValue()==null)
				return null;
			return ValidationContext.toString(km.metricValue());
		}
		return metric.toString();
	}

}
