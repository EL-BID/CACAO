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
 * Enumeration of interpersonal relationship types
 * 
 * @author Gustavo Figueiredo
 *
 */
public enum RelationshipType {

	// Relationship between taxpayers
	LEGAL_REPRESENTATIVE("rel.legal.representative"),
	DIRECTOR("rel.director"),
	ACCOUNTANT("rel.accountant"),
	
	// Relationship between government authorities and taxpayers
	MANAGER("rel.tax.manager");

	private final String display;

	RelationshipType(String display) {
		this.display = display;
	}

	@Override
	public String toString() {
		return display;
	}
	
	/**
	 * All relationship types referring to a declarant (i.e.: the declarant may be 'it' for somebody else)
	 */
	public static final RelationshipType[] relationshipsForDeclarants = {
		RelationshipType.LEGAL_REPRESENTATIVE,
		RelationshipType.DIRECTOR,
		RelationshipType.ACCOUNTANT
	};
	
	/**
	 * All relationship types referring to a government authority (i.e.: the authority may be 'it' for somebody else)
	 */
	public static final RelationshipType[] relationshipsForAuthorities = {
		RelationshipType.MANAGER	
	};
	
	public static RelationshipType parse(String s) {
		if (s==null || s.trim().length()==0)
			return null;
		return Arrays.stream(values()).filter(t->t.name().equalsIgnoreCase(s)).findAny().orElse(null);
	}

	public static RelationshipType parse(String s, MessageSource messageSource) {
		if (s==null || s.trim().length()==0)
			return null;
		return Arrays.stream(values()).filter(t->t.name().equalsIgnoreCase(s)).findAny()
				.orElse(Arrays.stream(values()).filter(t->messageSource.getMessage(t.toString(),null,LocaleContextHolder.getLocale()).equalsIgnoreCase(s)).findAny()
						.orElse(null));
	}
}
