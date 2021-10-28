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

/**
 * Enumeration of different types of communications.
 * 
 * @author Gustavo Figueiredo
 *
 */
public enum CommunicationType {

	LEGISLATION("comm_type.legislation", /*needsAudience*/false, /*acceptsAudience*/true),
	OVERALL("comm_type.overall"),
	INSTRUCTIONS ("comm_type.instructions", /*needsAudience*/false, /*acceptsAudience*/true),
	PRIVATE("comm_type.private", /*needsAudience*/true, /*acceptsAudience*/false),
	OCCURRENCES("comm_type.occurrences");

	private final String display;
	
	/**
	 * Indicates if 'audience' is mandatory
	 */
	private final boolean needsAudience;

	/**
	 * Indicates if 'audience' is optional
	 */
	private final boolean acceptsAudience;

	CommunicationType(String display) {
		this.display = display;
		this.needsAudience = false;
		this.acceptsAudience = false;
	}

	CommunicationType(String display, boolean needsAudience, boolean acceptsAudience) {
		this.display = display;
		this.needsAudience = needsAudience;
		this.acceptsAudience = acceptsAudience;
	}
	
	@Override
	public String toString() {
		return display;
	}
	
	/**
	 * Boolean indicator telling if this communication type needs or accepts indication of target 'audience'
	 */
	public boolean getAudience() {
		return needsAudience || acceptsAudience;
	}
	
	/**
	 * Boolean indicator telling if this communication type needs indication of target 'audience'
	 */
	public boolean isAudienceNeeded() {
		return needsAudience;
	}
	
	/**
	 * Boolean indicator telling if this communication type accepts indication of target 'audience'
	 */
	public boolean isAudienceAccepted() {
		return acceptsAudience;
	}
	
	/**
	 * Boolean indicator telling if this communication type can 'group' messages together
	 */
	public boolean getGroup() {
		return LEGISLATION.equals(this);
	}

	/**
	 * Boolean indicator telling if this communication type is related to a specific taxpayer
	 * @return
	 */
	public boolean getTaxpayer() {
		return OCCURRENCES.equals(this);
	}

	public static CommunicationType parse(String s) {
		if (s==null || s.trim().length()==0)
			return null;
		return Arrays.stream(values()).filter(t->t.name().equalsIgnoreCase(s)).findAny().orElse(null);
	}
}
