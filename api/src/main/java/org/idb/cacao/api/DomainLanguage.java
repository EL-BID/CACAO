/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Enumeration of different languages to be defined by users when specifying
 * descriptions (e.g. for Domain Tables)
 * 
 * @author Gustavo Figueiredo
 *
 */
public enum DomainLanguage {

	ENGLISH("domain.lang.english",	Locale.ENGLISH, /*abbreviations*/"en"),
	SPANISH("domain.lang.spanish",	new Locale("es", "ES"), /*abbreviations*/"sp"),		
	FRENCH("domain.lang.french",	Locale.FRENCH, /*abbreviations*/"fr"),
	PORTUGUESE("domain.lang.portuguese",	new Locale("pt", "BR"), /*abbreviations*/"pt");	

	private final String display;
	
	private final Locale defaultLocale;
	
	private final Set<String> abbreviations;
	
	DomainLanguage(String display, Locale defaultLocale, String... abbreviations) {
		this.display = display;
		this.defaultLocale = defaultLocale;
		this.abbreviations = (abbreviations==null) ? Collections.emptySet()
			: Arrays.stream(abbreviations).collect(Collectors.toCollection(()->new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
	}

	@Override
	public String toString() {
		return display;
	}
	
	public Locale getDefaultLocale() {
		return defaultLocale;
	}

	public Set<String> getAbbreviations() {
		return abbreviations;
	}
	
	public boolean match(String s) {
		if (s==null)
			return false;
		if (name().equalsIgnoreCase(s) || abbreviations.contains(s))
			return true;
		int sep = s.indexOf('-');
		if (sep>0) {
			String part = s.substring(0, sep);
			if (name().equalsIgnoreCase(part) || abbreviations.contains(part))
				return true;			
		}
		return false;
	}

	public static DomainLanguage parse(String s) {
		if (s==null || s.trim().length()==0)
			return null;
		return Arrays.stream(values()).filter(t->t.match(s)).findAny().orElse(null);
	}

	public static DomainLanguage parse(String s, MessageSource messageSource) {
		if (s==null || s.trim().length()==0)
			return null;
		return Arrays.stream(values()).filter(t->t.name().equalsIgnoreCase(s)).findAny()
				.orElse(Arrays.stream(values()).filter(t->messageSource.getMessage(t.toString(),null,LocaleContextHolder.getLocale()).equalsIgnoreCase(s)).findAny()
						.orElse(null));
	}
	
}
