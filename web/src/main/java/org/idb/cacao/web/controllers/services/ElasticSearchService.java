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

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.security.user.privileges.Role;
import org.idb.cacao.web.repositories.ESStandardRoles;
import org.idb.cacao.web.utils.ESUtils;
import org.idb.cacao.web.utils.ESUtils.KibanaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service methods for ElasticSearch administration
 * 
 * @author Gustavo Figueiredo
 *
 */
@Service
public class ElasticSearchService {

	private static final Logger log = Logger.getLogger(ElasticSearchService.class.getName());
	
	@Autowired
	private RestHighLevelClient esClient;
	
	@Autowired
	private Environment env;

	@Autowired
	private MessageSource messageSource;
	
	@Autowired
	private FieldsConventionsService fieldsConventionsService;

    private RestTemplate restTemplate;
    
    /**
     * These are all the Kibana known built-in features. We must keep a list of them because the 'Kibana way' of defining privileges
     * is 'opt-out' style (i.e.: by default any role has all privileges, we have to enumerate which ones we don't want to pass away)
     */
    public static final String[] ALL_KIBANA_FEATURES = {
   		"enterpriseSearch","canvas","ml","maps","infrastructure","logs","apm","siem","uptime","dev_tools","advancedSettings",
   		"indexPatterns","savedObjectsManagement","ingestManager","monitoring","discover","visualize","dashboard",
   		"savedObjectsTagging","fleet","actions","stackAlerts"
    };

	@Autowired
	public ElasticSearchService(RestTemplateBuilder builder, InternalHttpRequestsService requestFactory) {
		this.restTemplate = builder
				.setConnectTimeout(Duration.ofMinutes(5))
				.setReadTimeout(Duration.ofMinutes(5))
				.requestFactory(requestFactory)
				.build();
	}
	
	/**
	 * Returns an array of all Kibana features to be 'disabled', except those ones informed as parameters
	 */
	public static String[] getDisabledFeatures(String... keepFeatures) {
		Set<String> keepFeaturesSet = Arrays.stream(keepFeatures).collect(Collectors.toCollection(()->new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
		return Arrays.stream(ALL_KIBANA_FEATURES).filter(f->!keepFeaturesSet.contains(f)).toArray(String[]::new);
	}

	/**
	 * Check if the standard spaces already exists at Kibana. Creates missing spaces as needed.
	 */
	public void assertStandardSpaces() throws IOException {
		assertStandardSpaces(/*collectCreationInfo*/null);
	}
	
	/**
	 * Check if the standard spaces already exists at Kibana. Creates missing spaces as needed.
	 * @param collectCreationInfo If not NULL, collects output from this method about index creation
	 */
	public void assertStandardSpaces(Consumer<String> collectCreationInfo) throws IOException {

		log.log(Level.FINE, "Asserting Standard Spaces at Kibana...");

		List<KibanaSpace> existing_spaces = ESUtils.getKibanaSpaces(env, restTemplate);
		Set<String> existing_spaces_ids = (existing_spaces==null || existing_spaces.isEmpty()) ? Collections.emptySet()
				: existing_spaces.stream().map(KibanaSpace::getId).collect(Collectors.toCollection(()->new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
		
		if (!existing_spaces_ids.contains("declarant-public")) {
			log.log(Level.INFO, "Creating Kibana space 'declarant-public'");
			// Public space for declarant's dashboards
			KibanaSpace space = new KibanaSpace();
			space.setId("declarant-public");
			space.setName(messageSource.getMessage("kibana_space_declarant_public_name", null, fieldsConventionsService.getDocsLocale()));
			space.setInitials(messageSource.getMessage("kibana_space_declarant_public_initials", null, fieldsConventionsService.getDocsLocale()));
			space.setDescription("Public area for dashboards and other objects accessible by declarants");
			space.setDisabledFeatures(getDisabledFeatures(/*keep features*/ "discover","visualize","dashboard"));
			ESUtils.createKibanaSpace(env, restTemplate, space);
			copyKibanaConfigFromSpaceToSpace("default", space.getId());
			if (collectCreationInfo!=null)
				collectCreationInfo.accept("Created SPACE "+space.getId()+" ("+space.getDescription()+") at Kibana\n");
		}

		if (!existing_spaces_ids.contains("tax-public")) {
			log.log(Level.INFO, "Creating Kibana space 'tax-public'");
			// Public space for tax administration dashboards
			KibanaSpace space = new KibanaSpace();
			space.setId("tax-public");
			space.setName(messageSource.getMessage("kibana_space_tax_public_name", null, fieldsConventionsService.getDocsLocale()));
			space.setInitials(messageSource.getMessage("kibana_space_tax_public_initials", null, fieldsConventionsService.getDocsLocale()));
			space.setDescription("Public area for dashboards and other objects accessible by tax administration");
			space.setDisabledFeatures(getDisabledFeatures(/*keep features*/ "discover","visualize","dashboard"));
			ESUtils.createKibanaSpace(env, restTemplate, space);
			copyKibanaConfigFromSpaceToSpace("default", space.getId());
			if (collectCreationInfo!=null)
				collectCreationInfo.accept("Created SPACE "+space.getId()+" ("+space.getDescription()+") at Kibana\n");
		}

		if (!existing_spaces_ids.contains("tax-master")) {
			log.log(Level.INFO, "Creating Kibana space 'tax-master'");
			// Public space for tax master manager dashboards
			KibanaSpace space = new KibanaSpace();
			space.setId("tax-master");
			space.setName(messageSource.getMessage("kibana_space_tax_master_name", null, fieldsConventionsService.getDocsLocale()));
			space.setInitials(messageSource.getMessage("kibana_space_tax_master_initials", null, fieldsConventionsService.getDocsLocale()));
			space.setDescription("Public area for dashboards and other objects accessible by tax master authority");
			space.setDisabledFeatures(getDisabledFeatures(/*keep features*/ "discover","visualize","dashboard"));
			ESUtils.createKibanaSpace(env, restTemplate, space);
			copyKibanaConfigFromSpaceToSpace("default", space.getId());
			if (collectCreationInfo!=null)
				collectCreationInfo.accept("Created SPACE "+space.getId()+" ("+space.getDescription()+") at Kibana\n");
		}
	}

	/**
	 * Check if the standard roles already exists at ElasticSearch. Creates missing roles as needed.
	 */
	public Map<ESStandardRoles,Role> assertStandardRoles() throws IOException {
		return assertStandardRoles(/*collectCreationInfo*/null);
	}
	
	/**
	 * Check if the standard roles already exists at ElasticSearch. Creates missing roles as needed.
	 * @param collectCreationInfo If not NULL, collects output from this method about index creation
	 */
	public Map<ESStandardRoles,Role> assertStandardRoles(Consumer<String> collectCreationInfo) throws IOException {
		
		log.log(Level.FINE, "Asserting Standard Roles at ElasticSearch...");

		List<Role> roles = ESUtils.getRoles(esClient);
		
		Map<ESStandardRoles,Role> map_of_roles = new HashMap<>();
		
		for (ESStandardRoles standardRole: ESStandardRoles.values()) {

			Role existent_role =
					roles.stream().filter(role->0==String.CASE_INSENSITIVE_ORDER.compare(standardRole.getName(), role.getName()))
					.findAny().orElse(null);

			if (existent_role!=null) {
				map_of_roles.put(standardRole, existent_role);
				continue;
			}
			
			// If the standard role does not exists yet, create it automatically
			Role created_role = ESUtils.createRoleForSingleApplication(esClient, 
					standardRole.getName(), 
					standardRole.getApplication(), 
					standardRole.getPrivileges(), 
					standardRole.getResources(),
					standardRole.getAllIndicesPrivileges());
			if (created_role!=null) {
				map_of_roles.put(standardRole, created_role);
				if (collectCreationInfo!=null)
					collectCreationInfo.accept("Created ROLE "+created_role.getName()+" at Kibana\n");
			}
		}

		return map_of_roles;
	}
	
	/**
	 * Copy Kibana configurations and index-patterns from one space to another
	 */
	public void copyKibanaConfigFromSpaceToSpace(String sourceSpaceId, String targetSpaceId) {
		
		try {
			List<ESUtils.KibanaSavedObject> configs = ESUtils.getKibanaConfig(env, restTemplate, sourceSpaceId);
			if (configs!=null && !configs.isEmpty()) {
				String[] ids = configs.stream().map(ESUtils.KibanaSavedObject::getId).sorted().toArray(String[]::new);
				ESUtils.copyKibanaSavedObjects(env, restTemplate, sourceSpaceId, targetSpaceId, "config", ids);
			}
		}
		catch (Exception ex) {
			log.log(Level.SEVERE, "Error trying to copy config from space '"+sourceSpaceId+"' to space '"+targetSpaceId+"'", ex);
		}
		
		try {
			List<ESUtils.KibanaSavedObject> patterns = ESUtils.getKibanaIndexPatterns(env, restTemplate, sourceSpaceId);
			if (patterns!=null && !patterns.isEmpty()) {
				String[] ids = patterns.stream().map(ESUtils.KibanaSavedObject::getId).sorted().toArray(String[]::new);
				ESUtils.copyKibanaSavedObjects(env, restTemplate, sourceSpaceId, targetSpaceId, "index-pattern", ids);
			}
		}
		catch (Exception ex) {
			log.log(Level.SEVERE, "Error trying to copy index-pattern from space '"+sourceSpaceId+"' to space '"+targetSpaceId+"'", ex);			
		}

	}
}
