/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.dto;

import java.util.LinkedList;
import java.util.List;

public class AnalysisData {
	
	private double scaleMin;
	
	private double scaleMax;
	
	private String totalTaxpayers;
	
	private List<AnalysisItem> items;
	
	private List<Outlier> outliers;

	public double getScaleMin() {
		return scaleMin;
	}

	public void setScaleMin(double scaleMin) {
		this.scaleMin = scaleMin;
	}

	public double getScaleMax() {
		return scaleMax;
	}

	public void setScaleMax(double scaleMax) {
		this.scaleMax = scaleMax;
	}

	public String getTotalTaxpayers() {
		return totalTaxpayers;
	}

	public void setTotalTaxpayers(String totalTaxpayers) {
		this.totalTaxpayers = totalTaxpayers;
	}

	public List<AnalysisItem> getItems() {
		if ( items == null )
			items = new LinkedList<>();
		return items;
	}

	public void setItems(List<AnalysisItem> items) {
		this.items = items;
	}
	
	public void addItem(AnalysisItem item) {
		getItems().add(item);
	}
	
	public List<Outlier> getOutliers() {
		if ( outliers == null )
			outliers = new LinkedList<>();
		return outliers;
	}

	public void setOutliers(List<Outlier> outliers) {
		this.outliers = outliers;
	}
	
	public void addOutlier(Outlier outlier) {
		getOutliers().add(outlier);	
	}

	@Override
	public String toString() {
		return "AnalysisData [scaleMin=" + scaleMin + ", scaleMax=" + scaleMax + "]";
	}
	
}
