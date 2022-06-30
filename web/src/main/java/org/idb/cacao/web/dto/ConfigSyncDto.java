/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.dto;

import java.io.Serializable;

import javax.validation.constraints.Size;

import org.idb.cacao.web.entities.ConfigSync;
import org.idb.cacao.web.entities.DayOfWeek;
import org.idb.cacao.web.entities.SyncPeriodicity;

/**
 * ConfigSync Configuration (for subscribers/slaves)
 */
public class ConfigSyncDto implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final long ID_ACTIVE_CONFIG = 1;

	/**
	 * ID's are being generated automatically.
	 * PS: Elasticsearch generates by default 20 character long ID's, that are both URL-safe, base 64 encoded GUID
	 */
	private long id;

	private String master;

	@Size(max=1024)
	private String apiToken;

	private SyncPeriodicity periodicity;

	/**
	 * Hour of day is only considered when periodicity = SyncPeriodicity.DAILY or periodicity = SyncPeriodicity.WEEKLY
	 */
	private Integer hourOfDay;
	
	/**
	 * Day of week is only considered when periodicity = SyncPeriodicity.WEEKLY
	 */
	private DayOfWeek dayOfWeek;

	public void updateEntity(ConfigSync config) {
		config.setId(id);
		config.setMaster(master);
		config.setApiToken(apiToken);
		config.setPeriodicity(periodicity);
		config.setHourOfDay(hourOfDay);
		config.setDayOfWeek(dayOfWeek);
	}
	
	public void setId(long id) {
		this.id = id;
	}

	public long getId() {
		return id;
	}

	public void setMaster(String master) {
		this.master = master;
	}

	public String getMaster() {
		return master;
	}

	public void setPeriodicity(SyncPeriodicity periodicity) {
		this.periodicity = periodicity;
	}

	public SyncPeriodicity getPeriodicity() {
		if (periodicity==null)
			return SyncPeriodicity.NONE;
		return periodicity;
	}

	public void setApiToken(String apiToken) {
		this.apiToken = apiToken;
	}

	public String getApiToken() {
		return apiToken;
	}

	/**
	 * Hour of day is only considered when periodicity = SyncPeriodicity.DAILY or periodicity = SyncPeriodicity.WEEKLY
	 */
	public void setHourOfDay(Integer hourOfDay) {
		this.hourOfDay = hourOfDay;
	}

	/**
	 * Hour of day is only considered when periodicity = SyncPeriodicity.DAILY or periodicity = SyncPeriodicity.WEEKLY
	 */
	public Integer getHourOfDay() {
		return hourOfDay;
	}

	/**
	 * Day of week is only considered when periodicity = SyncPeriodicity.WEEKLY
	 */
	public void setDayOfWeek(DayOfWeek dayOfWeek) {
		this.dayOfWeek = dayOfWeek;
	}
	
	/**
	 * Day of week is only considered when periodicity = SyncPeriodicity.WEEKLY
	 */
	public DayOfWeek getDayOfWeek() {
		return dayOfWeek;
	}

}
