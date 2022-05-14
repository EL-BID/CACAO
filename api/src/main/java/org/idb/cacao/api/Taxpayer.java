/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.api;

import static org.springframework.data.elasticsearch.annotations.FieldType.Boolean;
import static org.springframework.data.elasticsearch.annotations.FieldType.Date;
import static org.springframework.data.elasticsearch.annotations.FieldType.Keyword;
import static org.springframework.data.elasticsearch.annotations.FieldType.Text;

import java.io.Serializable;
import java.time.OffsetDateTime;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

import com.fasterxml.jackson.annotation.JsonView;

/**
 * Taxpayers registration
 * 
 * @author Gustavo Figueiredo
 *
 */
@Document(indexName="cacao_taxpayers")
public class Taxpayer implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;

	/**
	 * ID's are being generated automatically.
	 * PS: Elasticsearch generates by default 20 character long ID's, that are both URL-safe, base 64 encoded GUID
	 */
	@Id
	@JsonView(Views.Public.class)
	@AFieldDescriptor(externalName = "doc.id")
	private String id;

	@Field(type=Date, store = true, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
	@AFieldDescriptor(externalName = "saved.time")
    private OffsetDateTime timestamp;

	@MultiField(
		mainField = @Field(type=Text, fielddata=true),
		otherFields = {
			@InnerField(suffix = "keyword", type=Keyword)
		}
	)
	@NotBlank
	@NotNull
	@NotEmpty
	@Size(min=6, max=20)
	@JsonView(Views.Public.class)
	@AFieldDescriptor(externalName = "taxpayer.id", audit=true)
	private String taxPayerId;

	@Field(type=Text)
	@NotBlank
	@NotNull
	@NotEmpty
	@Size(min=2, max=300)
	@JsonView(Views.Public.class)
	@AFieldDescriptor(externalName = "taxpayer.name", audit=true)
	private String name;

	@Field(type=Text)
	@Size(max=500)
	@JsonView(Views.Authority.class)
	@AFieldDescriptor(externalName = "taxpayer.address")
	private String address;
	
	@Field(type=Keyword)
	@Size(max=100)
	@JsonView(Views.Authority.class)
	@AFieldDescriptor(externalName = "taxpayer.zip.code")
	private String zipCode;

	/**
	 * Date/time of last modification or creation of any part of this object
	 */
	@Field(type=Date, store = true, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
	@AFieldDescriptor(externalName = "changed.time")
	private OffsetDateTime changedTime;

	@MultiField(
		mainField = @Field(type=Text, fielddata=true),
		otherFields = {
			@InnerField(suffix = "keyword", type=Keyword)
		}
	)
	@JsonView(Views.Authority.class)
	private String qualifier1;

	@MultiField(
		mainField = @Field(type=Text, fielddata=true),
		otherFields = {
			@InnerField(suffix = "keyword", type=Keyword)
		}
	)
	@JsonView(Views.Authority.class)
	private String qualifier2;

	@MultiField(
		mainField = @Field(type=Text, fielddata=true),
		otherFields = {
			@InnerField(suffix = "keyword", type=Keyword)
		}
	)
	@JsonView(Views.Authority.class)
	private String qualifier3;
	
	@MultiField(
		mainField = @Field(type=Text, fielddata=true),
		otherFields = {
			@InnerField(suffix = "keyword", type=Keyword)
		}
	)
	@JsonView(Views.Authority.class)
	private String qualifier4;
	
	@MultiField(
		mainField = @Field(type=Text, fielddata=true),
		otherFields = {
			@InnerField(suffix = "keyword", type=Keyword)
		}
	)
	@JsonView(Views.Authority.class)
	private String qualifier5;
	
	@Field(type=Boolean)
	@JsonView(Views.Public.class)
	private boolean active=true;
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public OffsetDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(OffsetDateTime timestamp) {
		this.timestamp = timestamp;
	}
	
	public Taxpayer withTimestamp(OffsetDateTime timestamp) {
		setTimestamp(timestamp);
		return this;
	}

	public String getTaxPayerId() {
		return taxPayerId;
	}

	public void setTaxPayerId(String taxPayerId) {
		this.taxPayerId = taxPayerId;
	}
	
	public Taxpayer withTaxPayerId(String taxPayerId) {
		setTaxPayerId(taxPayerId);
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public Taxpayer withName(String name) {
		setName(name);
		return this;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}
	
	public Taxpayer withAddress(String address) {
		setAddress(address);
		return this;
	}

	public String getZipCode() {
		return zipCode;
	}

	public void setZipCode(String zipCode) {
		this.zipCode = zipCode;
	}
	
	public Taxpayer withZipCode(String zipCode) {
		setZipCode(zipCode);
		return this;
	}
	
	public OffsetDateTime getChangedTime() {
		return changedTime;
	}

	public void setChangedTime(OffsetDateTime changedTime) {
		this.changedTime = changedTime;
	}
	
	public Taxpayer withChangedTime(OffsetDateTime changedTime) {
		setChangedTime(changedTime);
		return this;
	}

	public String getQualifier1() {
		return qualifier1;
	}

	public void setQualifier1(String qualifier1) {
		this.qualifier1 = qualifier1;
	}
	
	public Taxpayer withQualifier1(String qualifier1) {
		setQualifier1(qualifier1);
		return this;
	}

	public String getQualifier2() {
		return qualifier2;
	}

	public void setQualifier2(String qualifier2) {
		this.qualifier2 = qualifier2;
	}

	public Taxpayer withQualifier2(String qualifier2) {
		setQualifier2(qualifier2);
		return this;
	}

	public String getQualifier3() {
		return qualifier3;
	}

	public void setQualifier3(String qualifier3) {
		this.qualifier3 = qualifier3;
	}

	public Taxpayer withQualifier3(String qualifier3) {
		setQualifier3(qualifier3);
		return this;
	}

	public String getQualifier4() {
		return qualifier4;
	}

	public void setQualifier4(String qualifier4) {
		this.qualifier4 = qualifier4;
	}

	public Taxpayer withQualifier4(String qualifier4) {
		setQualifier4(qualifier4);
		return this;
	}

	public String getQualifier5() {
		return qualifier5;
	}

	public void setQualifier5(String qualifier5) {
		this.qualifier5 = qualifier5;
	}

	public Taxpayer withQualifier5(String qualifier5) {
		setQualifier5(qualifier5);
		return this;
	}

	@Override
	public String toString() {
		return name;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
	
}
