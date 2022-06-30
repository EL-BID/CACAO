/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.dto;

/**
 * Data Transfer Object for dashboard copy<BR>
 * View: dashboards_list.html<BR>
 * Controller: DashboardsUIController<BR>
 * 
 * @author Gustavo Figueiredo
 *
 */
public class DashboardCopy {

	private String[] target;

	public String[] getTarget() {
		return target;
	}

	public void setTarget(String[] target) {
		this.target = target;
	}
	
}
