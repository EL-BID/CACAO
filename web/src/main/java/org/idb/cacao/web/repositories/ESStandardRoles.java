/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.repositories;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.idb.cacao.web.entities.UserProfile;

/**
 * Enumeration of standard roles that this application will manage over ElasticSearch + Kibana
 * 
 * @author Gustavo Figueiredo
 */
public enum ESStandardRoles {

	DASHBOARDS_READ_TAX("roleDashboardsRead", /*application*/"kibana-.kibana", /*privileges*/Arrays.asList("feature_dashboard.read"), /*resources*/Arrays.asList("space:tax-public"), /*allIndicesPrivileges*/Arrays.asList("read"),
			/*userProfiles*/UserProfile.SYSADMIN,UserProfile.MASTER,UserProfile.SUPPORT,UserProfile.AUTHORITY),
	
	DASHBOARDS_READ_DECLARANT("roleDashboardsReadDeclarant", /*application*/"kibana-.kibana", /*privileges*/Arrays.asList("feature_dashboard.read"), /*resources*/Arrays.asList("space:declarant-public"), /*allIndicesPrivileges*/Arrays.asList("read"),
			/*userProfiles*/UserProfile.DECLARANT, UserProfile.SYSADMIN,UserProfile.MASTER,UserProfile.SUPPORT,UserProfile.AUTHORITY),
	
	DASHBOARDS_WRITE("roleDashboardsWrite", /*application*/"kibana-.kibana", /*privileges*/Arrays.asList("feature_dashboard.all","feature_discover.all","feature_visualize.all"), /*resources*/Arrays.asList("space:default","space:tax-public","space:declarant-public","space:tax-master"), /*allIndicesPrivileges*/Arrays.asList("read"),
			/*userProfiles*/UserProfile.SYSADMIN,UserProfile.MASTER,UserProfile.SUPPORT,UserProfile.AUTHORITY),
	
	INDEX_PATTERN_WRITE("roleIndexPatternWrite", /*application*/"kibana-.kibana", /*privileges*/Arrays.asList("feature_indexPatterns.all"), /*resources*/Arrays.asList("*"), /*allIndicesPrivileges*/Arrays.asList("read"),
			/*userProfiles*/UserProfile.SYSADMIN,UserProfile.SUPPORT);
	
	
	
	private final String name;
	private final String application;
	private final Collection<String> privileges;
	private final Collection<String> resources;
	private final Set<UserProfile> userProfiles;
	private final Collection<String> allIndicesPrivileges;
	
	ESStandardRoles(String name, String application, Collection<String> privileges, Collection<String> resources, Collection<String> allIndicesPrivileges,
			UserProfile... userProfiles) {
		this.name = name;		
		this.application = application;
		this.privileges = privileges;
		this.allIndicesPrivileges = allIndicesPrivileges;
		this.resources = resources;
		this.userProfiles = new HashSet<>(Arrays.asList(userProfiles));
	}

	public String getName() {
		return name;
	}

	public String getApplication() {
		return application;
	}

	public Collection<String> getPrivileges() {
		return privileges;
	}
	
	public boolean hasDashboardPrivilege() {
		return privileges!=null && privileges.stream().anyMatch(p->p.startsWith("feature_dashboard"));
	}

	public Collection<String> getAllIndicesPrivileges() {
		return allIndicesPrivileges;
	}

	public Collection<String> getResources() {
		return resources;
	}

	public Set<UserProfile> getUserProfiles() {
		return userProfiles;
	}
	
	public boolean matchProfile(UserProfile profile) {
		if (profile==null || userProfiles==null)
			return false;
		return userProfiles.contains(profile);
	}
	
	public boolean matchResource(String space) {
		if (resources==null || resources.isEmpty())
			return false;
		String resource = (space==null || space.trim().length()==0) ? "space:default"
				: (space.startsWith("space:")) ? space
						: "space:"+space;
		return resources.stream().anyMatch(r->String.CASE_INSENSITIVE_ORDER.compare(r,resource)==0);
	}

	public static Set<ESStandardRoles> getStandardRoles(UserProfile profile) {
		if (profile==null)
			return Collections.emptySet();
		return Arrays.stream(values()).filter(role->role.matchProfile(profile)).collect(Collectors.toSet());
	}
}
