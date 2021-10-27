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
package org.idb.cacao.web.utils;

import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.sec.CustomOAuth2UserService.CustomOidcUser;
import org.idb.cacao.web.sec.CustomOAuth2UserServiceSocial.CustomOAuth2User;
import org.springframework.security.core.Authentication;

/**
 * Utility methods for User objects manipulation
 * 
 * @author Gustavo Figueiredo
 *
 */
public class UserUtils {
	
	/**
	 * Given the Authentication object, extracts the embedded User object.<BR>
	 * Warning: this information may be out of date (e.g. the user information may have
	 * been changed by a system administrator since the user logged in).
	 */
	public static User getUser(Authentication auth) {
		if (auth==null) {
			return null;
		}
		if (auth.getPrincipal() instanceof User) {
			return (User)auth.getPrincipal();
		}
		if (auth.getPrincipal() instanceof CustomOidcUser) {
			return ((CustomOidcUser)auth.getPrincipal()).getUser();
		}
		if (auth.getPrincipal() instanceof CustomOAuth2User) {
			return ((CustomOAuth2User)auth.getPrincipal()).getUser();
		}
		return null;
	}
	
}
