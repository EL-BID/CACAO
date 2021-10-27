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

/**
 * Enumeration of simple user profiles.
 * In this application we are allowing only one privilege to each user.
 * 
 * @author Gustavo Figueiredo
 */
public enum UserProfile {
	
	SYSADMIN("user.profile.sysadmin"),
	MASTER("user.profile.master"),
	SUPPORT("user.profile.support"),
	AUTHORITY("user.profile.authority"),
	DECLARANT("user.profile.declarant");

	private final String display;
	
	UserProfile(String display) {
		this.display = display;
	}
	
	/**
	 * Return the role name associated to this user profile
	 */
	public String getRole() {
		// IMPORTANT:
		// All role names must be prefixed with 'ROLE_' since it's the Spring standard (otherwise
		// we would need to add more customizations to Spring configuration)
		return "ROLE_"+name();
	}

	@Override
	public String toString() {
		return display;
	}
	
	public static UserProfile parse(String text) {
		if (text==null || text.length()==0)
			return null;
		for (UserProfile p: values()) {
			if (p.name().equalsIgnoreCase(text)
				|| p.display.equalsIgnoreCase(text)
				|| p.getRole().equalsIgnoreCase(text))
				return p;
		}
		return null;
	}
}
