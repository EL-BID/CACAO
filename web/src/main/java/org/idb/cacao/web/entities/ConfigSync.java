/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.entities;

import static org.springframework.data.elasticsearch.annotations.FieldType.Keyword;
import static org.springframework.data.elasticsearch.annotations.FieldType.Text;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.validation.constraints.Size;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

/**
 * ConfigSync Configuration (for subscribers/slaves)
 */
@Document(indexName="cacao_config_sync")
public class ConfigSync implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;

	public static final long ID_ACTIVE_CONFIG = 1;

	/**
	 * ID's are being generated automatically.
	 * PS: Elasticsearch generates by default 20 character long ID's, that are both URL-safe, base 64 encoded GUID
	 */
	@Id   
	private long id;

	@Field(type=Text)
	private String master;

	@Field(type=Text)
	@Size(max=1024)
	private String apiToken;

	@Enumerated(EnumType.STRING)
	@MultiField(
			mainField = @Field(type=Text, fielddata=true),
			otherFields = {
				@InnerField(suffix = "keyword", type=Keyword)
			}
		)
	private SyncPeriodicity periodicity;

	/**
	 * Hour of day is only considered when periodicity = SyncPeriodicity.DAILY or periodicity = SyncPeriodicity.WEEKLY
	 */
	@Field(type=Text)
	private Integer hourOfDay;
	
	/**
	 * Day of week is only considered when periodicity = SyncPeriodicity.WEEKLY
	 */
	@Enumerated(EnumType.STRING)
	@MultiField(
			mainField = @Field(type=Text, fielddata=true),
			otherFields = {
				@InnerField(suffix = "keyword", type=Keyword)
			}
		)
	private DayOfWeek dayOfWeek;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getMaster() {
		return master;
	}

	public void setMaster(String master) {
		this.master = master;
	}

	public SyncPeriodicity getPeriodicity() {
		if (periodicity==null)
			return SyncPeriodicity.NONE;
		return periodicity;
	}

	public void setPeriodicity(SyncPeriodicity periodicity) {
		this.periodicity = periodicity;
	}

	public String getApiToken() {
		return apiToken;
	}

	public void setApiToken(String apiToken) {
		this.apiToken = apiToken;
	}

	/**
	 * Hour of day is only considered when periodicity = SyncPeriodicity.DAILY or periodicity = SyncPeriodicity.WEEKLY
	 */
	public Integer getHourOfDay() {
		return hourOfDay;
	}

	/**
	 * Hour of day is only considered when periodicity = SyncPeriodicity.DAILY or periodicity = SyncPeriodicity.WEEKLY
	 */
	public void setHourOfDay(Integer hourOfDay) {
		this.hourOfDay = hourOfDay;
	}

	/**
	 * Day of week is only considered when periodicity = SyncPeriodicity.WEEKLY
	 */
	public DayOfWeek getDayOfWeek() {
		return dayOfWeek;
	}

	/**
	 * Day of week is only considered when periodicity = SyncPeriodicity.WEEKLY
	 */
	public void setDayOfWeek(DayOfWeek dayOfWeek) {
		this.dayOfWeek = dayOfWeek;
	}

	/**
	 * Returns TRUE if we have changed any information related to scheduling
	 */
	public static boolean hasChangedScheduleInfo(ConfigSync c1, ConfigSync c2) {
		if (c1==c2)
			return false;
		if (c1==null && SyncPeriodicity.NONE.equals(c2.getPeriodicity())) 
			return false;
		if (c2==null && SyncPeriodicity.NONE.equals(c1.getPeriodicity())) 
			return false;
		if (c1==null || c2==null)
			return true;
		if (SyncPeriodicity.NONE.equals(c1.getPeriodicity()) 
				&& SyncPeriodicity.NONE.equals(c2.getPeriodicity()))
			return false;
		if (!c1.getPeriodicity().equals(c2.getPeriodicity()))
			return true;
		if (!Objects.equals(c1.getHourOfDay(),c2.getHourOfDay())) {
			return true;
		}
		if (!Objects.equals(c1.getDayOfWeek(),c2.getDayOfWeek())) {
			return true;
		}
		return false;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
}
