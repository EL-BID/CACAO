/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.dto;

import java.io.Serializable;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.idb.cacao.api.AFieldDescriptor;
import org.idb.cacao.api.Taxpayer;
import org.idb.cacao.api.Views;

import com.fasterxml.jackson.annotation.JsonView;

/**
 * Taxpayers Data Transfer Object
 * 
 * @author Luis Kauer
 *
 */
public class TaxpayerDto implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;

	/**
	 * ID's are being generated automatically.
	 * PS: Elasticsearch generates by default 20 character long ID's, that are both URL-safe, base 64 encoded GUID
	 */
	@JsonView(Views.Public.class)
	@AFieldDescriptor(externalName = "doc.id")
	private String id;

	@NotBlank
	@NotNull
	@NotEmpty
	@Size(min=6, max=20)
	@JsonView(Views.Public.class)
	@AFieldDescriptor(externalName = "taxpayer.id", audit=true)
	private String taxPayerId;

	@NotBlank
	@NotNull
	@NotEmpty
	@Size(min=2, max=300)
	@JsonView(Views.Public.class)
	@AFieldDescriptor(externalName = "taxpayer.name", audit=true)
	private String name;

	@Size(max=500)
	@JsonView(Views.Authority.class)
	@AFieldDescriptor(externalName = "taxpayer.address")
	private String address;
	
	@Size(max=100)
	@JsonView(Views.Authority.class)
	@AFieldDescriptor(externalName = "taxpayer.zip.code")
	private String zipCode;

	@JsonView(Views.Authority.class)
	private String qualifier1;

	@JsonView(Views.Authority.class)
	private String qualifier2;

	@JsonView(Views.Authority.class)
	private String qualifier3;
	
	@JsonView(Views.Authority.class)
	private String qualifier4;
	
	@JsonView(Views.Authority.class)
	private String qualifier5;
	
	@JsonView(Views.Public.class)
	private boolean active=true;
	
	public TaxpayerDto() {
	}
	
	public TaxpayerDto(Taxpayer taxpayer) {
		this.id = taxpayer.getId();
		this.taxPayerId = taxpayer.getTaxPayerId();
		this.name = taxpayer.getName();
		this.zipCode = taxpayer.getZipCode();
		this.address = taxpayer.getAddress();
		this.qualifier1 = taxpayer.getQualifier1();
		this.qualifier2 = taxpayer.getQualifier2();
		this.qualifier3 = taxpayer.getQualifier3();
		this.qualifier4 = taxpayer.getQualifier4();
		this.qualifier5 = taxpayer.getQualifier5();
		this.active = taxpayer.isActive();
	}
	
	public TaxpayerDto(String id, String taxPayerId, String name, String zipCode, String address,
			String qualifier1, String qualifier2, String qualifier3, String qualifier4,
			String qualifier5, boolean active) {
		this.id = id;
		this.taxPayerId = taxPayerId;
		this.name = name;
		this.zipCode = zipCode;
		this.address = address;
		this.qualifier1 = qualifier1;
		this.qualifier2 = qualifier2;
		this.qualifier3 = qualifier3;
		this.qualifier4 = qualifier4;
		this.qualifier5 = qualifier5;
		this.active = active;
	}
	
	/**
	 * Updates a Taxpayer object from this DTO
	 * @param taxpayer Taxpayer entity object
	 * @param updateIfPresent
	 */
	public Taxpayer updateEntity(Taxpayer taxpayer, boolean updateIfPresent) {
		taxpayer.setTaxPayerId(taxPayerId);
		if (!updateIfPresent || (name!=null && !name.isEmpty()))
			taxpayer.setName(name);
		if (!updateIfPresent || (zipCode!=null && !zipCode.isEmpty()))
		    taxpayer.setZipCode(zipCode);
		if (!updateIfPresent || (address!=null && !address.isEmpty()))
			taxpayer.setAddress(address);
		if (!updateIfPresent || (qualifier1!=null && !qualifier1.isEmpty()))
			taxpayer.setQualifier1(qualifier1);
		if (!updateIfPresent || (qualifier2!=null && !qualifier2.isEmpty()))
		    taxpayer.setQualifier2(qualifier2);
		if (!updateIfPresent || (qualifier3!=null && !qualifier3.isEmpty()))
		    taxpayer.setQualifier3(qualifier3);
		if (!updateIfPresent || (qualifier4!=null && !qualifier4.isEmpty()))
		    taxpayer.setQualifier4(qualifier4);
		if (!updateIfPresent || (qualifier5!=null && !qualifier5.isEmpty()))
		    taxpayer.setQualifier5(qualifier5);
		taxpayer.setActive(active);
		return taxpayer;
	}
	
	public Taxpayer createEntity() {
		Taxpayer taxpayer = new Taxpayer();
		taxpayer.setId(id);
		updateEntity(taxpayer, false);
		return taxpayer;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTaxPayerId() {
		return taxPayerId;
	}

	public void setTaxPayerId(String taxPayerId) {
		this.taxPayerId = taxPayerId;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}
	
	public String getZipCode() {
		return zipCode;
	}

	public void setZipCode(String zipCode) {
		this.zipCode = zipCode;
	}
	
	public String getQualifier1() {
		return qualifier1;
	}

	public void setQualifier1(String qualifier1) {
		this.qualifier1 = qualifier1;
	}
	
	public String getQualifier2() {
		return qualifier2;
	}

	public void setQualifier2(String qualifier2) {
		this.qualifier2 = qualifier2;
	}

	public String getQualifier3() {
		return qualifier3;
	}

	public void setQualifier3(String qualifier3) {
		this.qualifier3 = qualifier3;
	}

	public String getQualifier4() {
		return qualifier4;
	}

	public void setQualifier4(String qualifier4) {
		this.qualifier4 = qualifier4;
	}

	public String getQualifier5() {
		return qualifier5;
	}

	public void setQualifier5(String qualifier5) {
		this.qualifier5 = qualifier5;
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
