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
package org.idb.cacao.web.sec;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.idb.cacao.web.controllers.services.KeyStoreService;
import org.idb.cacao.web.controllers.services.PrivilegeService;
import org.idb.cacao.web.entities.SystemPrivilege;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfFilter;

/**
 * Filter used for pre-authenticating user requests based on API token.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class ApiKeyAuthenticationFilter implements Filter {

	/**
	 * Authentication method we are supporting here
	 */
	private static final String AUTH_METHOD = "api-key";
    
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private KeyStoreService ksRepository;

    @Autowired
    private PrivilegeService privilegeService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException
    {
        if(request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            String apiKey = getApiKey((HttpServletRequest) request);
            if(apiKey != null) {
            	Optional<User> matchingUser = getUserGivenTokenAPI(apiKey);
                if(matchingUser.isPresent()) {
                	if (!privilegeService.hasPrivilege(matchingUser.get().getProfile(), SystemPrivilege.CONFIG_API_TOKEN)) {
                        HttpServletResponse httpResponse = (HttpServletResponse) response;
                        httpResponse.setStatus(HttpStatus.SC_UNAUTHORIZED); // 401
                        httpResponse.getWriter().write("Use of API Token is prohibited for your user profile");                    
                        return;                		
                	}
                	else if (!matchingUser.get().isActive()) {
                        HttpServletResponse httpResponse = (HttpServletResponse) response;
                        httpResponse.setStatus(HttpStatus.SC_UNAUTHORIZED); // 401
                        httpResponse.getWriter().write("User account has been disabled");                    
                        return;                		                		
                	}
                	else {
	                    ApiKeyAuthenticationToken apiToken = new ApiKeyAuthenticationToken(matchingUser.get(), privilegeService.getGrantedAuthorities(matchingUser.get().getProfile()));
	                    SecurityContextHolder.getContext().setAuthentication(apiToken);
	                    // Since this is a stateless request to API using access token, we won't need the CSRF filter for this request
	                    CsrfFilter.skipRequest((HttpServletRequest)request);
                	}
                } else {
                    HttpServletResponse httpResponse = (HttpServletResponse) response;
                    httpResponse.setStatus(HttpStatus.SC_UNAUTHORIZED); // 401
                    httpResponse.getWriter().write("Invalid API Key");                    
                    return;
                }
            }
        }
        
        chain.doFilter(request, response);
        
    }

    /**
     * Extract the API key from the HTTP request as long as it uses the same authentication method described in AUTH_METHOD
     */
    private String getApiKey(HttpServletRequest httpRequest) {
        String apiKey = null;
        
        String authHeader = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);
        if(authHeader != null) {
            authHeader = authHeader.trim();
            if(authHeader.toLowerCase().startsWith(AUTH_METHOD + " ")) {
                apiKey = authHeader.substring(AUTH_METHOD.length()).trim();
            }
        }
        
        return apiKey;
    }
    
    private Optional<User> getUserGivenTokenAPI(String apiKey) {
    	if (apiKey==null || apiKey.trim().length()==0)
    		return Optional.empty();
    	
    	for (User user:userRepository.findAll()) {
    		if (user.getApiToken()==null || user.getApiToken().trim().length()==0)
    			continue;
    		String decrypted = ksRepository.decrypt(KeyStoreService.PREFIX_MAIL, user.getApiToken());
    		if (decrypted.equals(apiKey)) {
    			return Optional.of(user);
    		}
    	}
    	
    	return Optional.empty();
    }
}
