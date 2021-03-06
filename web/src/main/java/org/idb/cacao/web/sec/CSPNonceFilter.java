/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.sec;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

/**
 * This WebFilter will change the static '{nonce}' text configured at 'SecurityConfig' class with a dynamic
 * per-request random generated alphanumeric sequence. All inline javascript and style sheets should include
 * the same code using thymeleaf reference to 'cspNonce' variable. This variable is set here by this WebFilter.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class CSPNonceFilter extends GenericFilterBean {

    private static final int NONCE_SIZE = 32; //recommended is at least 128 bits/16 bytes
    private static final String CSP_NONCE_ATTRIBUTE = "cspNonce";
 
    private SecureRandom secureRandom = new SecureRandom();
 
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
 
        byte[] nonceArray = new byte[NONCE_SIZE];
 
        secureRandom.nextBytes(nonceArray);
 
        String nonce = Base64.getEncoder().encodeToString(nonceArray);
        request.setAttribute(CSP_NONCE_ATTRIBUTE, nonce);
 
        chain.doFilter(request, new CSPNonceResponseWrapper(response, nonce));
    }
 
    /**
     * Wrapper to fill the nonce value
     */
    public static class CSPNonceResponseWrapper extends HttpServletResponseWrapper {
        private String nonce;
 
        public CSPNonceResponseWrapper(HttpServletResponse response, String nonce) {
            super(response);
            this.nonce = nonce;
        }
 
        @Override
        public void setHeader(String name, String value) {
            if (name.equals("Content-Security-Policy") && StringUtils.isNotBlank(value)) {
                super.setHeader(name, value.replace("{nonce}", nonce));
            } else {
                super.setHeader(name, value);
            }
        }
 
        @Override
        public void addHeader(String name, String value) {
            if (name.equals("Content-Security-Policy") && StringUtils.isNotBlank(value)) {
                super.addHeader(name, value.replace("{nonce}", nonce));
            } else {
                super.addHeader(name, value);
            }
        }
    }
}
