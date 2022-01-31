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
package org.idb.cacao.web.dto;

import java.util.LinkedList;
import java.util.List;

public class AnalysisData {
	
	private double scaleMin;
	
	private double scaleMax;
	
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
