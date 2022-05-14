/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.sec;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

/**
 * Implementation for handling authentication failures.<BR> 
 * This component is auto wired to 'authenticationFailureHandler' defined in 'SecurityConfig'
 * @author Gustavo Figueiredo
 *
 */
@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Autowired
    private MessageSource messages;

    @Autowired
    private LocaleResolver localeResolver;

    @Override
    public void onAuthenticationFailure(final HttpServletRequest request, final HttpServletResponse response, final AuthenticationException exception) throws IOException, ServletException {
    	
    	setDefaultFailureUrl("/login?error=true");
    	super.onAuthenticationFailure(request, response, exception);
    	
        final Locale locale = localeResolver.resolveLocale(request);

        String errorMessage = messages.getMessage("login.failed", null, locale);

        if (exception.getMessage()
            .equalsIgnoreCase("User is disabled")) {
            errorMessage = messages.getMessage("login.disabled", null, locale);
        }
        
        request.getSession()
            .setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, errorMessage);
    	
    }
}
