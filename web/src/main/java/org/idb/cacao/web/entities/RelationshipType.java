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
