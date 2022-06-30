/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.sec;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * This filter will intercept the requests. For some of them, it will replace the default Spring behaviour
 * of returned 'chunked contents' (without 'content-length') for an alternative approach including the
 * response header 'content-length', as required by some external platform integrations (such as the DNS check
 * of Azure Platform)
 * 
 * @author Gustavo Figueiredo
 *
 */
@Component
@Order(1)
public class FilterForContentLength implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException
    {
    	if(request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
    		try {
	    		final String accessed_uri = ((HttpServletRequest)request).getRequestURI();
	    		if (requiresContentLength(accessed_uri)) {
	    			
	    	        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper((HttpServletResponse) response);

	    	        chain.doFilter(request, responseWrapper);

	    	        responseWrapper.copyBodyToResponse();
	    	        return;
	    		}
    		} catch (Throwable ex) { }
    	}
    	
    	chain.doFilter(request, response);
    }
    
    public static boolean requiresContentLength(String uri) {
    	return uri!=null && uri.contains(".well-known");
    }

}
