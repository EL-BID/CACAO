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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.math3.util.Precision;
import org.elasticsearch.search.aggregations.metrics.Percentiles;

public class AnalysisItem implements Comparable<AnalysisItem> {
	
	private String statementOrder;
	
	private String statementName;
	
	private double q1;
	
	private double q3;
	
	private double median;
	
	private double sum;
	
	private double average;
	
	private double deviation;
	
	private double value;
	
	private List<Outlier> outliers;

	private List<Outlier> normalizedOutliers;
	
	public AnalysisItem() {		
	}
	
	public AnalysisItem(String[] values, double sumValue, double averegaValue, double deviationValue, Percentiles percentile) {
		this.statementOrder = values.length > 0 ? values[0] : "";
		this.statementName = values.length > 1 ? values[1] : "";

		if (percentile != null) {

			percentile.forEach(item -> {
				double percent = item.getPercent();

				if (percent == 25) // First quartile
					setQ1(Precision.round(item.getValue(), 2, RoundingMode.HALF_DOWN.ordinal()));
				else if (percent == 50) // Median
					setMedian(Precision.round(item.getValue(), 2, RoundingMode.HALF_DOWN.ordinal()));
				else if (percent == 75) // Third quartile
					setQ3(Precision.round(item.getValue(), 2, RoundingMode.HALF_DOWN.ordinal()));

			});

			if (!Double.isNaN(getQ1()) && !Double.isNaN(getQ3())
					&& getQ1() != 0 && getQ3() != 0) {
				setSum(sumValue);
				setAverage(averegaValue);
				setDeviation(deviationValue);
			}

		}
	}

	public String getStatementOrder() {
		return statementOrder;
	}

	public void setStatementOrder(String statementOrder) {
		this.statementOrder = statementOrder;
	}

	public String getStatementName() {
		return statementName;
	}

	public void setStatementName(String statementName) {
		this.statementName = statementName;
	}

	public double getQ1() {
		return q1;
	}
	
	public double getQ1Percentage() {
		return q1/100;
	}

	public void setQ1(double q1) {
		this.q1 = q1;
	}
	
	public double getQ3Percentage() {
		return q3/100;
	}

	public double getQ3() {
		return q3;
	}

	public void setQ3(double q3) {
		this.q3 = q3;
	}
	
	public double getIIQ() {
		return Precision.round((q3 - q1), 2, BigDecimal.ROUND_HALF_DOWN);
	}
	
	public double getMax() {
		return Precision.round((q3 + (1.5d * getIIQ()) ), 2, BigDecimal.ROUND_HALF_DOWN);
	}
	
	public double getMaxPercentage() {
		return getMax() / 100;
	}	
	
	public double getMin() {
		return Precision.round((q1 - (1.5d * getIIQ()) ), 2, BigDecimal.ROUND_HALF_DOWN);
	}
	
	public double getMinPercentage() {
		return getMin() / 100;
	}	
	
	public double getMedian() {
		return median;
	}
	
	public double getMedianPercentage() {
		return getMedian() / 100;
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
	
	public void addOutlier(Outlier outlier) {
		getOutliers().add(outlier);	
	}

	public List<Outlier> getNormalizedOutliers() {
		if ( normalizedOutliers == null ) {
			normalizedOutliers = new LinkedList<>();			
		}
		return normalizedOutliers;
	}
	
	public void addNormalizedOutlier(Outlier outlier) {
		getNormalizedOutliers().add(outlier);
	}

	public void setNormalizedOutliers(List<Outlier> normalizedOutliers) {
		this.normalizedOutliers = normalizedOutliers;
	}
	
	public double getSum() {
		return sum;
	}

	public void setSum(double sum) {
		this.sum = sum;
	}

	public double getAverage() {
		return average;
	}

	public void setAverage(double average) {
		this.average = average;
	}

	public double getDeviation() {
		return deviation;
	}

	public void setDeviation(double deviation) {
		this.deviation = deviation;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "AnalysisData [statementName=" + statementName + ", q1=" + q1 + ", q3=" + q3 + "]";
	}

	@Override
	public int compareTo(AnalysisItem other) {
		if (this==other)
			return 0;
		if (statementOrder==null)
			return -1;
		if (other.statementOrder==null)
			return 1;
		
		return statementOrder.compareToIgnoreCase(other.statementOrder);
	}

	@Override
	public int hashCode() {
		return Objects.hash(statementName, statementOrder);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AnalysisItem other = (AnalysisItem) obj;
		return Objects.equals(statementName, other.statementName)
				&& Objects.equals(statementOrder, other.statementOrder);
	}
		
}
