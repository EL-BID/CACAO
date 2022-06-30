/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.utils;

import org.springframework.core.env.Environment;

/**
 * Some utility methods for dealing with user login
 * @author Gustavo Figueiredo
 *
 */
public class LoginUtils {

	/**
	 * Returns TRUE if there is any configuration in application properties related to some
	 * OIDC (OpenId Connect) provider. Will only consider providers explicitly informed in
	 * other methods of this class.
	 */
	public static boolean hasOIDCProviders(Environment env) {
		
		return
				hasGoogleProvider(env)
			||	hasAzureProvider(env)
			||	hasFacebookProvider(env);
	}
	
	/**
	 * Returns TRUE if the Google OIDC provider has been configured in application properties
	 */
	public static boolean hasGoogleProvider(Environment env) {
		return env.containsProperty("spring.security.oauth2.client.registration.google.client-id");
	}
	
	/**
	 * Returns TRUE if the Microsoft Azure OIDC provider has been configured in application properties
	 */
	public static boolean hasAzureProvider(Environment env) {
		return env.containsProperty("spring.security.oauth2.client.registration.azure.client-id");
	}

	/**
	 * Returns TRUE if the Facebook OIDC provider has been configured in application properties
	 */
	public static boolean hasFacebookProvider(Environment env) {
		return env.containsProperty("spring.security.oauth2.client.registration.facebook.client-id");
	}
}
