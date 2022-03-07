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
