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
package org.idb.cacao.web.entities;

import static org.springframework.data.elasticsearch.annotations.FieldType.*;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Document(indexName="templates")
public class DocumentTemplate implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;

	/**
	 * ID's are being generated automatically.
	 * PS: Elasticsearch generates by default 20 character long ID's, that are both URL-safe, base 64 encoded GUID
	 */
	@Id   
	private String id;

	@MultiField(
		mainField = @Field(type=Text, fielddata=true),
		otherFields = {
			@InnerField(suffix = "keyword", type=Keyword)
		}
	)
	@NotBlank
	@NotNull
	@NotEmpty
	@Size(min=2, max=120)
	private String name;

	@MultiField(
		mainField = @Field(type=Text, fielddata=true),
		otherFields = {
			@InnerField(suffix = "keyword", type=Keyword)
		}
	)
	@NotBlank
	@NotNull
	@NotEmpty
	@Size(min=1, max=20)
	private String version;
	
	/**
	 * Tax description (only for the case where this declaration derives just one kind of tax)
	 */
	@Field(type=Keyword)
	private String tax;

	/**
	 * Tax code (only for the case where this declaration derives just one kind of tax)
	 */
	@Field(type=Keyword)
	private String taxCode;
	
	/**
	 * Indicates that this tax/declaration admits simplified payment (i.e. the taxpayer can generate a payment slip directly without submitting a document)
	 */
	@Field(type=Boolean)
	private Boolean allowSimplePay;

	/**
	 * Indicates that this tax/declaration admits rectifying any past periods.
	 * Configuration for rejecting or accepting incoming file that rectifies another previous uploaded file when there
	 * are other files related to later periods. E.g.: Suppose taxpayer has already uploaded files for jan/2020 and feb/2020,
	 * and now he wants to rectify the file related to jan/2020. This flag will tell whether it's acceptable or not. 
	 */
	@Field(type=Boolean)
	private Boolean allowRectifierAnyPeriod;

	@Enumerated(EnumType.STRING)
	@Field(type=Text)
	private DocumentFormat format;

	@Enumerated(EnumType.STRING)
	@Field(type=Text)
	private DueDateCalcType dueDateCalc;

	/**
	 * Date/time the template was created
	 */
	@Field(type=Date, store = true, format = DateFormat.custom, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
	@DateTimeFormat(iso = ISO.DATE_TIME)
    private Date templateCreateTime;

	/**
	 * Date/time of template upload
	 */
	@Field(type=Date, store = true, format = DateFormat.custom, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
	@DateTimeFormat(iso = ISO.DATE_TIME)
    private Date timestampTemplate;
	
	/**
	 * Local filename (not including directory) for template file contents
	 */
	@Field(type=Text)
	private String filename;

	/**
	 * Date/time of sample upload
	 */
	@Field(type=Date, store = true, format = DateFormat.custom, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
	@DateTimeFormat(iso = ISO.DATE_TIME)
    private Date timestampSample;
	
	/**
	 * Date/time of last modification or creation of any part of this object
	 */
	@Field(type=Date, store = true, format = DateFormat.custom, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
	private Date changedTime;
	
	/**
	 * Local filename (not including directory) for sample file contents
	 */
	@Field(type=Text)
	private String sample;
	
	/**
	 * Maximum tax period (usually in the format of MONTH/YEAR) for considering this version. For other periods the taxpayer
	 * must upload a different version of the same template.
	 */
	@Field(type=Keyword)
	private String taxPeriodMax;

	@Enumerated(EnumType.STRING)
	@Field(type=Text)
	private Periodicity periodicity;
	
	@Field(type=Keyword)
	private String payeeId;
	
	@MultiField(
		mainField = @Field(type=Text, fielddata=true),
		otherFields = {
			@InnerField(suffix = "keyword", type=Keyword)
		}
	)
	private String payeeName;

	@Field(type=Nested)
	private List<DocumentField> fields;
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getTax() {
		return tax;
	}

	public void setTax(String tax) {
		this.tax = tax;
	}

	public String getTaxCode() {
		return taxCode;
	}

	public void setTaxCode(String taxCode) {
		this.taxCode = taxCode;
	}

	public DocumentFormat getFormat() {
		return format;
	}

	public void setFormat(DocumentFormat format) {
		this.format = format;
	}

	public DueDateCalcType getDueDateCalc() {
		return dueDateCalc;
	}

	public void setDueDateCalc(DueDateCalcType dueDateCalc) {
		this.dueDateCalc = dueDateCalc;
	}

	public Periodicity getPeriodicity() {
		if (periodicity==null)
			return Periodicity.UNKNOWN;
		return periodicity;
	}

	public void setPeriodicity(Periodicity periodicity) {
		this.periodicity = periodicity;
	}

	/**
	 * Local filename (not including directory) for template file contents
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * Local filename (not including directory) for template file contents
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}

	/**
	 * Date/time the template was created
	 */
	public Date getTemplateCreateTime() {
		return templateCreateTime;
	}

	/**
	 * Date/time the template was created
	 */
	public void setTemplateCreateTime(Date templateCreateTime) {
		this.templateCreateTime = templateCreateTime;
	}

	public Date getTimestampTemplate() {
		return timestampTemplate;
	}

	public void setTimestampTemplate(Date timestampTemplate) {
		this.timestampTemplate = timestampTemplate;
	}

	public Date getTimestampSample() {
		return timestampSample;
	}

	public void setTimestampSample(Date timestampSample) {
		this.timestampSample = timestampSample;
	}

	/**
	 * Indicates that this tax/declaration admits simplified payment (i.e. the taxpayer can generate a payment slip directly without submitting a document)
	 */
	public Boolean getAllowSimplePay() {
		return allowSimplePay;
	}

	/**
	 * Indicates that this tax/declaration admits simplified payment (i.e. the taxpayer can generate a payment slip directly without submitting a document)
	 */
	public void setAllowSimplePay(Boolean allowSimplePay) {
		this.allowSimplePay = allowSimplePay;
	}

	/**
	 * Indicates that this tax/declaration admits rectifying any past periods.
	 */
	public Boolean getAllowRectifierAnyPeriod() {
		return allowRectifierAnyPeriod;
	}

	/**
	 * Indicates that this tax/declaration admits rectifying any past periods.
	 */
	public void setAllowRectifierAnyPeriod(Boolean allowRectifierAnyPeriod) {
		this.allowRectifierAnyPeriod = allowRectifierAnyPeriod;
	}

	/**
	 * Date/time of last modification or creation of any part of this object
	 */
	public Date getChangedTime() {
		return changedTime;
	}

	/**
	 * Date/time of last modification or creation of any part of this object
	 */
	public void setChangedTime(Date changedTime) {
		this.changedTime = changedTime;
	}

	public String getSample() {
		return sample;
	}

	public void setSample(String sample) {
		this.sample = sample;
	}
	
	/**
	 * Maximum tax period (usually in the format of MONTH/YEAR) for considering this version. For other periods the taxpayer
	 * must upload a different version of the same template.
	 */
	public String getTaxPeriodMax() {
		return taxPeriodMax;
	}
	
	public boolean hasTaxPeriodMax() {
		return taxPeriodMax!=null && taxPeriodMax.trim().length()>0;
	}

	/**
	 * Maximum tax period (usually in the format of MONTH/YEAR) for considering this version. For other periods the taxpayer
	 * must upload a different version of the same template.
	 */
	public void setTaxPeriodMax(String taxPeriodMax) {
		this.taxPeriodMax = taxPeriodMax;
	}

	public List<DocumentField> getFields() {
		return fields;
	}
	
	/**
	 * Find a field with the given name
	 */
	public DocumentField getField(String name) {
		if (fields==null)
			return null;
		return fields.stream().filter(f->name.equalsIgnoreCase(f.getFieldName())).findAny().orElse(null);
	}
	
	/**
	 * Find fields with the given name, ignoring array indexes, if any.
	 */
	@JsonIgnore
	public List<DocumentField> getFieldIgnoringArrayIndex(String name) {
		if (fields==null)
			return Collections.emptyList();
		return fields.stream().filter(f->name.equalsIgnoreCase(f.getFieldNameIgnoringArrayIndex())).collect(Collectors.toList());
	}

	/**
	 * Returns all the 'assigned' fields in this template (i.e. all fields with 'fieldType' different of 'ANY')
	 */
	@JsonIgnore
	public List<DocumentField> getAssignedFields() {
		if (fields==null)
			return Collections.emptyList();
		return fields.stream().filter(DocumentField::isAssigned).collect(Collectors.toList());
	}
	
	/**
	 * Returns all the 'assigned' fields in this template that are also 'required'
	 */
	@JsonIgnore
	public List<DocumentField> getAssignedRequiredFields() {
		if (fields==null)
			return Collections.emptyList();
		return fields.stream().filter(DocumentField::isAssigned).filter(f->f.getFieldType().isRequired()).collect(Collectors.toList());
	}

	/**
	 * Returns all the 'assigned' fields in this template of a given type
	 */
	@JsonIgnore
	public List<DocumentField> getFieldsOfType(FieldType type) {
		if (fields==null)
			return Collections.emptyList();
		return fields.stream().filter(f->type.equals(f.getFieldType())).collect(Collectors.toList());		
	}

	public void setFields(List<DocumentField> fields) {
		this.fields = fields;
	}
	
	public void clearFields() {
		if (fields!=null)
			fields.clear();
	}
	
	public void addField(DocumentField field) {
		if (fields==null)
			fields = new LinkedList<>();
		fields.add(field);
		field.setId(getNextUnassignedFieldId());
	}
	
	public void addField(String name, String sampleValue) {
		addField(new DocumentField(name, sampleValue));
	}
	
	public void removeField(DocumentField field) {
		if (fields==null)
			return;
		fields.remove(field);
		field.setId(0);
	}
	
	public void sortFields() {
		if (fields==null || fields.size()<2)
			return;
		Collections.sort(fields);
	}
	
	@JsonIgnore
	public int getNumAssignedFields() {
		if (fields==null)
			return 0;
		return (int)fields.stream().filter(DocumentField::isAssigned).count();
	}

	@JsonIgnore
	public int getNumTotalFields() {
		if (fields==null)
			return 0;
		else
			return fields.size();
	}
	
	@JsonIgnore
	public int getNextUnassignedFieldId() {
		if (fields==null || fields.isEmpty())
			return 1;
		return 1 + fields.stream().mapToInt(DocumentField::getId).max().orElse(0);
	}

	public String getPayeeId() {
		return payeeId;
	}

	public void setPayeeId(String payeeId) {
		this.payeeId = payeeId;
	}

	public String getPayeeName() {
		return payeeName;
	}

	public void setPayeeName(String payeeName) {
		this.payeeName = payeeName;
	}

	public DocumentTemplate clone() {
		try {
			return (DocumentTemplate)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

    @Override
    public String toString() {
        return "DocumentTemplate{" + "id=" + id + ", name=" + name +", version=" + version + '}';
    }

    /**
     * Put DocumentTemplate instances in reverse chronological order according to their timestamps
     */
    public static final Comparator<DocumentTemplate> TIMESTAMP_COMPARATOR = new Comparator<DocumentTemplate>() {

		@Override
		public int compare(DocumentTemplate o1, DocumentTemplate o2) {
			Date d1 = o1.getTemplateCreateTime();
			Date d2 = o2.getTemplateCreateTime();
			if (d1!=d2) {
				if (d1==null)
					return 1;
				if (d2==null)
					return -1;
				int comp = d1.compareTo(d2);
				if (comp!=0)
					return - comp;
			}
			d1 = o1.getTimestampTemplate();
			d2 = o2.getTimestampTemplate();
			if (d1!=d2) {
				if (d1==null)
					return 1;
				if (d2==null)
					return -1;
				int comp = d1.compareTo(d2);
				if (comp!=0)
					return - comp;
			}
			return 0;
		}
    	
    };
}
