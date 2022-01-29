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
	
	private String statementName;
	
	private double q1;
	
	private double q3;
	
	private double median;
	
	private List<Outlier> outliers;

	public String getStatementName() {
		return statementName;
	}

	public void setStatementName(String statementName) {
		this.statementName = statementName;
	}

	public double getQ1() {
		return q1;
	}

	public void setQ1(double q1) {
		this.q1 = q1;
	}

	public double getQ3() {
		return q3;
	}

	public void setQ3(double q3) {
		this.q3 = q3;
	}
	
	public double getIIQ() {
		return (q3 - q1);
	}
	
	public double getMax() {
		return (q3 + (1.5d * getIIQ()) );
	}
	
	public double getMin() {
		return (q1 - (1.5d * getIIQ()) );
	}
	
	public double getMedian() {
		return median;
	}

	public void setMedian(double median) {
		this.median = median;
	}

	public List<Outlier> getOutliers() {
		if ( outliers == null )
			outliers = new LinkedList<>();
		return outliers;
	}

	public void setOutliers(List<Outlier> outliers) {
		this.outliers = outliers;
	}
	
	public void addOutilier(Outlier outlier) {
		getOutliers().add(outlier);	
	}

	@Override
	public String toString() {
		return "AnalysisData [statementName=" + statementName + ", q1=" + q1 + ", q3=" + q3 + "]";
	}

	
}
