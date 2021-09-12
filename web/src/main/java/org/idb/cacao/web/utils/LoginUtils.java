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
