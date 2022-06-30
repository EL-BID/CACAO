/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.dto;

import java.util.Objects;

import org.idb.cacao.api.utils.StringUtils;

/**
 * Data Transfer Object for dashboard management<BR>
 * View: dashboards_list.html<BR>
 * Controller: DashboardsUIController<BR>
 * 
 * @author Gustavo Figueiredo
 *
 */
public class Dashboard implements Comparable<Dashboard> {

	private String id;
	
	private String title;
	
	private String spaceId;
	
	private String spaceName;
	
	private String url;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSpaceId() {
		return spaceId;
	}

	public void setSpaceId(String spaceId) {
		this.spaceId = spaceId;
	}

	public String getSpaceName() {
		return spaceName;
	}

	public void setSpaceName(String spaceName) {
		this.spaceName = spaceName;
	}
		
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String toString() {
		return getTitle()+":"+getSpaceName();
	}

	@Override
	public int compareTo(Dashboard o) {
		int c = StringUtils.compareCaseInsensitive(title, o.title);
		if (c != 0)
			return c;
		c = StringUtils.compareCaseInsensitive(spaceName, o.spaceName);
		if (c != 0)
			return c;
		return 0;
	}

	@Override
	public int hashCode() {
		return Objects.hash(spaceName, title);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Dashboard other = (Dashboard) obj;
		return Objects.equals(spaceName, other.spaceName) && Objects.equals(title, other.title);
	}
	
}
