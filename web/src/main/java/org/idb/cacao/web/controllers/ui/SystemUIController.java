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

import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.idb.cacao.web.controllers.services.ResourceMonitorService;
import org.idb.cacao.web.dto.ComponentSystemInformation;
import org.idb.cacao.web.dto.MenuItem;
import org.idb.cacao.web.utils.ESUtils;
import org.idb.cacao.web.utils.HttpUtils;
import org.idb.cacao.web.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.idb.cacao.web.utils.StringUtils.*;

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
    private MessageSource messages;

    @Autowired
    private ResourceMonitorService sysInfoService;

	@Autowired
	private RestHighLevelClient elasticsearchClient;

    private RestTemplate restTemplate;

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
	@GetMapping("/sys_info")
	public String getSystemInformation(Model model) {
		
		List<MenuItem> itens = new LinkedList<>();
		MenuItem info = new MenuItem(text("sysinfo"));
		itens.add(info);
		
		collectInfoForWebComponent(info);
		
		collectInfoForETLComponent(info);
		
		collectInfoForValidatorComponent(info);
		
		collectInfoForElasticSearch(info);
		
		model.addAttribute("info", itens);
		
		return "system/sys-info";
		
	}
	
	/**
	 * Collects information about the 'web' component
	 */
	public void collectInfoForWebComponent(MenuItem collect) {
		
		ComponentSystemInformation info = sysInfoService.collectInfoForComponent();
		collectInfoForComponent(info, "sysinfo.web.app", collect);
		
	}
	
	/**
	 * Collects information about the 'ETL' component
	 */
	public void collectInfoForETLComponent(MenuItem collect) {
		
		String url = "http://etl:8080/api/sys_info";
		ResponseEntity<ComponentSystemInformation> responseEntity;
		try {
			responseEntity = restTemplate.getForEntity(new URI(url), ComponentSystemInformation.class);
		} catch (RestClientException | URISyntaxException e) {
			MenuItem info_app = new MenuItem(text("sysinfo.etl.app")).withActive(false);
			collect.addChild(info_app);
			info_app.withChild(text("error.internal.server", e.getMessage()));
			return;
		}
		
		ComponentSystemInformation info = responseEntity.getBody();
		collectInfoForComponent(info, "sysinfo.etl.app", collect);
		
	}

	/**
	 * Collects information about the 'Validator' component
	 */
	public void collectInfoForValidatorComponent(MenuItem collect) {
		
		String url = "http://validator:8080/api/sys_info";
		ResponseEntity<ComponentSystemInformation> responseEntity;
		try {
			responseEntity = restTemplate.getForEntity(new URI(url), ComponentSystemInformation.class);
		} catch (RestClientException | URISyntaxException e) {
			MenuItem info_app = new MenuItem(text("sysinfo.validator.app")).withActive(false);
			collect.addChild(info_app);
			info_app.withChild(text("error.internal.server", e.getMessage()));
			return;
		}
		
		ComponentSystemInformation info = responseEntity.getBody();
		collectInfoForComponent(info, "sysinfo.etl.app", collect);
		
	}


	/**
	 * Transforms information collected about some component into 'MenuItem' structure for UI
	 */
	public void collectInfoForComponent(ComponentSystemInformation info, String messageKey, MenuItem collect) {
		
		NumberFormat numbers = NumberFormat.getInstance(LocaleContextHolder.getLocale());
		NumberFormat decimals = new DecimalFormat("#,##0.00", new DecimalFormatSymbols (LocaleContextHolder.getLocale()));

		MenuItem info_web_app = new MenuItem(text(messageKey)).withActive(false);
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
				es_health.addChild(new MenuItem(text("sysinfo.es.cluster.timed_out")).withActive(false));
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

	private String text(String key, Object... arguments) {
		return StringUtils.text(messages, key, arguments);
	}

	/**
	 * Format some primitive value as text
	 */
	private String formatValue(Object value) {
		return StringUtils.formatValue(messages, value);
	}
}
