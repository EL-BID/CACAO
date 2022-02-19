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
import java.util.Collection;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.idb.cacao.web.entities.UserProfile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

/**
 * This WebFilter will inspect all requests regarding the API Documentation provided by Swagger. If the authenticated
 * user is a 'Declarant', restrict access to the corresponding documentation.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class SwaggerUIAuthenticationFilter extends GenericFilterBean {
	
	private static final Pattern pSwaggerURL = Pattern.compile("^/?v2/api-docs", Pattern.CASE_INSENSITIVE);

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        
        String uri = request.getRequestURI();
        if (pSwaggerURL.matcher(uri).find()) {
        	if (!isAuthenticated() || isDeclarant()) {
        		String query = request.getQueryString();
        		if (!query.equals("group=Declarant")) {
        			response.setStatus(HttpStatus.SC_UNAUTHORIZED); // 401
        			response.getWriter().write("Use of API Token is prohibited for your user profile");                    
                    return;                		
        		}
        	}
        }
        
        chain.doFilter(request, response);
    }
    
    private boolean isAuthenticated() {
    	SecurityContext sec_context = SecurityContextHolder.getContext();
    	if (sec_context==null)
    		return false;
    	Authentication authentication = sec_context.getAuthentication();
    	return authentication!=null;
    }
    
    private boolean isDeclarant() {
    	SecurityContext sec_context = SecurityContextHolder.getContext();
    	if (sec_context==null)
    		return false;
    	Authentication authentication = sec_context.getAuthentication();
    	if (authentication==null)
    		return false;
    	Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
    	if (authorities==null || authorities.isEmpty())
    		return false;
    	return authorities.stream().anyMatch(auth->auth.toString().equals(UserProfile.DECLARANT.getRole()));
    }
    
}
