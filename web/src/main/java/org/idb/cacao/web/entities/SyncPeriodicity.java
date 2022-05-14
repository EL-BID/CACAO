/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.entities;

import java.util.Arrays;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Enumeration of different periodic schemes for SYNC
 * 
 * @author Gustavo Figueiredo
 *
 */
public enum SyncPeriodicity {

	NONE("sync.periodicity.none"),
	HOURLY("sync.periodicity.hourly"),
	DAILY("sync.periodicity.daily"),
	WEEKLY("sync.periodicity.weekly");

	private final String display;
	
	SyncPeriodicity(String display) {
		this.display = display;
	}

	@Override
	public String toString() {
		return display;
	}
	
	public static SyncPeriodicity parse(String s) {
		if (s==null || s.trim().length()==0)
			return null;
		return Arrays.stream(values()).filter(t->t.name().equalsIgnoreCase(s)).findAny().orElse(null);
	}

	public static SyncPeriodicity parse(String s, MessageSource messageSource) {
		if (s==null || s.trim().length()==0)
			return null;
		return Arrays.stream(values()).filter(t->t.name().equalsIgnoreCase(s)).findAny()
				.orElse(Arrays.stream(values()).filter(t->messageSource.getMessage(t.toString(),null,LocaleContextHolder.getLocale()).equalsIgnoreCase(s)).findAny()
						.orElse(null));
	}

}
