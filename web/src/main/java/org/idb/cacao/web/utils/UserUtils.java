/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
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
