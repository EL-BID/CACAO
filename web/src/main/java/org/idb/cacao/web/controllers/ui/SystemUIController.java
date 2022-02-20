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
package org.idb.cacao.web.controllers.ui;

import static org.idb.cacao.api.utils.StringUtils.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.idb.cacao.api.ComponentSystemInformation;
import org.idb.cacao.api.utils.StringUtils;
import org.idb.cacao.web.controllers.services.ResourceMonitorService;
import org.idb.cacao.web.dto.MenuItem;
import org.idb.cacao.web.utils.ESUtils;
import org.idb.cacao.web.utils.HttpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Controller class for all endpoints related to user interface regarding 'system information'.
 * 
 * @author Gustavo Figueiredo
 *
 */
@Controller
public class SystemUIController {

	static final Logger log = Logger.getLogger(SystemUIController.class.getName());

	@Autowired
	private Environment env;

    @Autowired
    private MessageSource messages;

    @Autowired
    private ResourceMonitorService sysInfoService;

	@Autowired
	private RestHighLevelClient elasticsearchClient;
	
    private RestTemplate restTemplate;
    
    @Autowired
    private DiscoveryClient discoveryClient;

	@Autowired
	public SystemUIController(RestTemplateBuilder builder) {
		this.restTemplate = builder
				.setConnectTimeout(Duration.ofMinutes(5))
				.setReadTimeout(Duration.ofMinutes(5))
				.requestFactory(HttpUtils::getTrustAllHttpRequestFactory)
				.build();
	}

	/**
	 * Returns information about the system and its components.
	 */
	@GetMapping("/sys-info")
	public String getSystemInformation(Model model) {
		
		List<MenuItem> itens = new LinkedList<>();
		MenuItem info = new MenuItem(text("sysinfo"));
		itens.add(info);
		
		collectInfoForWebComponent(info);
		
		collectInfoForValidatorComponent(info);

		collectInfoForETLComponent(info);		
		
		collectInfoForElasticSearch(info);
		
		collectInfoForKafka(info);
		
		model.addAttribute("info", itens);
		
		return "system/sys-info";
		
	}
	
	/**
	 * Collects information about the 'web' component
	 */
	public void collectInfoForWebComponent(MenuItem collect) {
		
		ComponentSystemInformation info = sysInfoService.collectInfoForComponent();
		collectInfoForComponent(info, text("sysinfo.web.app"), collect);
		
	}
	
	/**
	 * Collects information about the 'ETL' component
	 */
	public void collectInfoForETLComponent(MenuItem collect) {
		
		List<ServiceInstance> registered_services = discoveryClient.getInstances("CACAO_ETL");
		
		if (registered_services.isEmpty()) {
			MenuItem info_app = new MenuItem(text("sysinfo.etl.app")).withActive(false);
			collect.addChild(info_app);
			info_app.withChild(text("sysinfo.services.none", "ETL"));
			return;
		}

		MenuItem info_app = new MenuItem(text("sysinfo.etl.app")).withActive(false);
		collect.addChild(info_app);

		for (ServiceInstance serv: registered_services) {
		
			String url = String.format("http://%s:%d/api/sys_info", 
					serv.getHost(), 
					serv.getPort());
			
			String instanceName = String.format("%s:%d", serv.getHost(), serv.getPort());
	
			ResponseEntity<ComponentSystemInformation> responseEntity;
			try {
				responseEntity = restTemplate.getForEntity(new URI(url), ComponentSystemInformation.class);
			} catch (RestClientException | URISyntaxException e) {
				MenuItem info_host = new MenuItem(instanceName).withActive(false);
				info_app.addChild(info_host);
				info_host.withChild(text("error.internal.server", e.getMessage()));
				continue;
			}
			
			ComponentSystemInformation info = responseEntity.getBody();
			collectInfoForComponent(info, instanceName, info_app);
			
		}
		
	}

	/**
	 * Collects information about the 'Validator' component
	 */
	public void collectInfoForValidatorComponent(MenuItem collect) {
		
		List<ServiceInstance> registered_services = discoveryClient.getInstances("CACAO_VALIDATOR");
		
		if (registered_services.isEmpty()) {
			MenuItem info_app = new MenuItem(text("sysinfo.validator.app")).withActive(false);
			collect.addChild(info_app);
			info_app.withChild(text("sysinfo.services.none", "Validator"));
			return;
		}

		MenuItem info_app = new MenuItem(text("sysinfo.validator.app")).withActive(false);
		collect.addChild(info_app);

		for (ServiceInstance serv: registered_services) {

			String url = String.format("http://%s:%d/api/sys_info", 
					serv.getHost(), 
					serv.getPort());
			
			String instanceName = String.format("%s:%d", serv.getHost(), serv.getPort());

			ResponseEntity<ComponentSystemInformation> responseEntity;
			try {
				responseEntity = restTemplate.getForEntity(new URI(url), ComponentSystemInformation.class);
			} catch (RestClientException | URISyntaxException e) {
				MenuItem info_host = new MenuItem(instanceName).withActive(false);
				info_app.addChild(info_host);
				info_host.withChild(text("error.internal.server", e.getMessage()));
				continue;
			}
			
			ComponentSystemInformation info = responseEntity.getBody();
			collectInfoForComponent(info, instanceName, info_app);
			
		}
		
	}


	/**
	 * Transforms information collected about some component into 'MenuItem' structure for UI
	 */
	public void collectInfoForComponent(ComponentSystemInformation info, String menuItem, MenuItem collect) {
		
		NumberFormat numbers = NumberFormat.getInstance(LocaleContextHolder.getLocale());
		NumberFormat decimals = new DecimalFormat("#,##0.00", new DecimalFormatSymbols (LocaleContextHolder.getLocale()));

		MenuItem info_web_app = new MenuItem(menuItem).withActive(false);
		collect.addChild(info_web_app);
		
		MenuItem java_version = new MenuItem(text("sysinfo.java.os.version")).withActive(false)
				.withChild(text("sysinfo.java.version",info.getJavaVersion()))
				.withChild(text("sysinfo.java.home",info.getJavaHome()))
				.withChild(text("sysinfo.os.version",info.getOsVersion()));
		info_web_app.addChild(java_version);
		
		MenuItem info_cpu = new MenuItem(text("sysinfo.processors")).withActive(false);
		info_web_app.addChild(info_cpu);
		info_cpu.addChild(new MenuItem(text("sysinfo.processors.count"))
				.withChild(numbers.format(  info.getProcessorsCount()  )));
		info_cpu.addChild(new MenuItem(text("sysinfo.processors.arch"))
				.withChild(info.getProcessorsArch() ));

		MenuItem info_mem = new MenuItem(text("sysinfo.memory")).withActive(false);
		info_web_app.addChild(info_mem);
		info_mem.addChild(new MenuItem(text("sysinfo.heap.used"))
				.withChild(formatMemory(info.getHeapUsed(), decimals)));
		info_mem.addChild(new MenuItem(text("sysinfo.heap.free"))
				.withChild(formatMemory(info.getHeapFree(), decimals)));
		info_mem.addChild(new MenuItem(text("sysinfo.mem.used"))
				.withChild(formatMemory(info.getMemUsed(), decimals)));
		info_mem.addChild(new MenuItem(text("sysinfo.mem.free"))
				.withChild(formatMemory(info.getMemFree(), decimals)));
		
		MenuItem plugins = new MenuItem(text("sysinfo.plugins")).withActive(false);
		info_web_app.addChild(plugins);
		if (info.getInstalledPlugins()!=null && !info.getInstalledPlugins().isEmpty()) {
			for (String pluginName: info.getInstalledPlugins()) {
				plugins.addChild(new MenuItem(pluginName));
			}
		}
		else {
			plugins.addChild(new MenuItem(text("sysinfo.plugins.none")));
		}
	}
	
	/**
	 * Collect information about ElasticSearch
	 * @param info
	 */
	public void collectInfoForElasticSearch(MenuItem info) {
		MenuItem es = new MenuItem(text("sysinfo.es")).withActive(false);
		info.addChild(es);
		MenuItem es_health = new MenuItem(text("sysinfo.es.cluster.health")).withActive(false);
		es.addChild(es_health);
		ClusterHealthResponse es_cluster_health;
		try {
			es_cluster_health = ESUtils.getClusterStatus(elasticsearchClient);
		}
		catch (Throwable ex) {
			log.log(Level.WARNING, "Error while retrieving ElasticSearch cluster health information", ex);
			es_cluster_health = null;
		}
		if (es_cluster_health!=null) {
			if (es_cluster_health.isTimedOut()) {
				es_health.addChild(new MenuItem(text("sysinfo.es.cluster.timed.out")).withActive(false));
			}
			else {
				es_health.addChild(new MenuItem(text("sysinfo.es.cluster.name")).withChild(es_cluster_health.getClusterName()));
				es_health.addChild(new MenuItem(text("sysinfo.es.cluster.status")).withChild(es_cluster_health.getStatus().toString()));
				es_health.addChild(new MenuItem(text("sysinfo.es.cluster.number.nodes")).withChild(formatValue(es_cluster_health.getNumberOfNodes())));
				es_health.addChild(new MenuItem(text("sysinfo.es.cluster.number.data.nodes")).withChild(formatValue(es_cluster_health.getNumberOfDataNodes())));
				es_health.addChild(new MenuItem(text("sysinfo.es.cluster.active.primary.shards")).withChild(formatValue(es_cluster_health.getActivePrimaryShards())));
				es_health.addChild(new MenuItem(text("sysinfo.es.cluster.active.shards")).withChild(formatValue(es_cluster_health.getActiveShards())));
				es_health.addChild(new MenuItem(text("sysinfo.es.cluster.relocating.shards")).withChild(formatValue(es_cluster_health.getRelocatingShards())));
				es_health.addChild(new MenuItem(text("sysinfo.es.cluster.initializing.shards")).withChild(formatValue(es_cluster_health.getInitializingShards())));
				es_health.addChild(new MenuItem(text("sysinfo.es.cluster.unassigned.shards")).withChild(formatValue(es_cluster_health.getUnassignedShards())));
				es_health.addChild(new MenuItem(text("sysinfo.es.cluster.delayed.unassinged.shards")).withChild(formatValue(es_cluster_health.getDelayedUnassignedShards())));
				es_health.addChild(new MenuItem(text("sysinfo.es.cluster.pending.tasks")).withChild(formatValue(es_cluster_health.getNumberOfPendingTasks())));
				es_health.addChild(new MenuItem(text("sysinfo.es.cluster.inflight.fetch")).withChild(formatValue(es_cluster_health.getNumberOfInFlightFetch())));
				es_health.addChild(new MenuItem(text("sysinfo.es.cluster.task.max.waiting")).withChild(formatValue(es_cluster_health.getTaskMaxWaitingTime())));
				es_health.addChild(new MenuItem(text("sysinfo.es.cluster.active.shards.percent")).withChild(formatValue(Math.round(es_cluster_health.getActiveShardsPercent()))+" %"));
			}
		}
		else {
			es_health.addChild(new MenuItem(text("sysinfo.no.info")).withActive(false));
		}
		
		MenuItem es_indices = new MenuItem(text("sysinfo.es.indices")).withActive(false);
		es.addChild(es_indices);
		try {
			List<ESUtils.IndexSummary> summary = ESUtils.catIndices(elasticsearchClient);
			if (summary==null || summary.isEmpty()) {
				es_indices.addChild(new MenuItem(text("sysinfo.no.info")).withActive(false));
			}
			else {
				StringBuilder tabular_html = new StringBuilder();
				tabular_html.append("<table class=\"ui orange small table\">");
				tabular_html.append("<thead class=\"full-width\">");
				tabular_html.append("<tr>");
				tabular_html.append("<td>").append(text("sysinfo.es.indices.index")).append("</td>");
				tabular_html.append("<td>").append(text("sysinfo.es.indices.health")).append("</td>");
				tabular_html.append("<td>").append(text("sysinfo.es.indices.status")).append("</td>");
				tabular_html.append("<td>").append(text("sysinfo.es.indices.pri")).append("</td>");
				tabular_html.append("<td>").append(text("sysinfo.es.indices.rep")).append("</td>");
				tabular_html.append("<td>").append(text("sysinfo.es.indices.docs.count")).append("</td>");
				tabular_html.append("<td>").append(text("sysinfo.es.indices.docs.deleted")).append("</td>");
				tabular_html.append("<td>").append(text("sysinfo.es.indices.store.size")).append("</td>");
				tabular_html.append("<td>").append(text("sysinfo.es.indices.pri.store.size")).append("</td>");				
				tabular_html.append("</tr>");
				tabular_html.append("</thead>");
				tabular_html.append("<tbody>");
				for (ESUtils.IndexSummary s: summary) {
					tabular_html.append("<tr>");
					tabular_html.append("<td>").append(s.getIndex()).append("</td>");
					tabular_html.append("<td>").append(s.getHealth()).append("</td>");
					tabular_html.append("<td>").append(s.getStatus()).append("</td>");
					tabular_html.append("<td class=\"right aligned\">").append(formatValue(s.getPri())).append("</td>");
					tabular_html.append("<td class=\"right aligned\">").append(formatValue(s.getRep())).append("</td>");
					tabular_html.append("<td class=\"right aligned\">").append(formatValue(s.getDocsCount())).append("</td>");
					tabular_html.append("<td class=\"right aligned\">").append(formatValue(s.getDocsDeleted())).append("</td>");
					tabular_html.append("<td class=\"right aligned\">").append(s.getStoreSize()).append("</td>");
					tabular_html.append("<td class=\"right aligned\">").append(s.getPriStoreSize()).append("</td>");
					tabular_html.append("</tr>");
				}
				tabular_html.append("</tbody>");
				tabular_html.append("</table>");
				es_indices.addChild(new MenuItem(tabular_html.toString()).chkForHTMLContents(true));
			}
		}
		catch (Throwable ex) {
			log.log(Level.WARNING, "Error while retrieving ElasticSearch summary about all the indices", ex);
			es_indices.addChild(new MenuItem(text("sysinfo.no.info")).withActive(false));
		}
	}

	/**
	 * Collect information about Kafka
	 * @param info
	 */
	public void collectInfoForKafka(MenuItem info) {
		
		MenuItem kafka_info = new MenuItem(text("sysinfo.kafka")).withActive(false);
		info.addChild(kafka_info);

		try {
		
			try (AdminClient kafkaAdminClient = getKafkaAdminClient();) {
			
				Map<String,String> metrics =
				kafkaAdminClient.metrics().entrySet().stream().collect(Collectors.toMap(e->e.getKey().group()+"."+e.getKey().name(), e->ResourceMonitorService.getKafkaMetricDesc(e.getValue())));
				kafka_info.addChild(new MenuItem(text("sysinfo.kafka.version")).withActive(false).withChild(metrics.get("app-info.version")));
				
				int number_nodes = kafkaAdminClient.describeCluster().nodes().get().size();
				kafka_info.addChild(new MenuItem(text("sysinfo.kafka.nodes")).withActive(false).withChild(formatValue(number_nodes)));
		
				MenuItem kafka_topics = new MenuItem(text("sysinfo.kafka.topics")).withActive(false);
				kafka_info.addChild(kafka_topics);
	
				StringBuilder tabular_html = new StringBuilder();
				tabular_html.append("<table class=\"ui orange small table\">");
				tabular_html.append("<thead class=\"full-width\">");
				tabular_html.append("<tr>");
				tabular_html.append("<td>").append(text("sysinfo.kafka.topics.name")).append("</td>");
				tabular_html.append("<td>").append(text("sysinfo.kafka.topics.parts")).append("</td>");
				tabular_html.append("<td>").append(text("sysinfo.kafka.topics.replicas")).append("</td>");
				tabular_html.append("</tr>");
				tabular_html.append("</thead>");
				tabular_html.append("<tbody>");
				ListTopicsResult topics_info = kafkaAdminClient.listTopics();
				DescribeTopicsResult topics_details = kafkaAdminClient.describeTopics(topics_info.names().get());
				for (Map.Entry<String,KafkaFuture<TopicDescription> > topic_details: topics_details.values().entrySet()) {
					String topic_name = topic_details.getKey();
					TopicDescription topic_desc = topic_details.getValue().get();
					int num_parts = topic_desc.partitions().size();
					int num_replicas = topic_desc.partitions().stream().mapToInt(p->p.replicas().size()).max().orElse(0);
					tabular_html.append("<tr>");
					tabular_html.append("<td>").append(topic_name).append("</td>");
					tabular_html.append("<td class=\"right aligned\">").append(num_parts).append("</td>");
					tabular_html.append("<td class=\"right aligned\">").append(num_replicas).append("</td>");
					tabular_html.append("</tr>");
				}
				tabular_html.append("</tbody>");
				tabular_html.append("</table>");
				kafka_topics.addChild(new MenuItem(tabular_html.toString()).chkForHTMLContents(true));

			}
		}
		catch (Throwable ex) {
			log.log(Level.WARNING, "Error while retrieving Kafka summary", ex);
			kafka_info.addChild(new MenuItem(text("sysinfo.no.info")).withActive(false));			
		}

	}
	
	private String text(String key, Object... arguments) {
		return StringUtils.text(messages, key, arguments);
	}

	/**
	 * Format some primitive value as text
	 */
	private String formatValue(Object value) {
		return StringUtils.formatValue(messages, value);
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

}
