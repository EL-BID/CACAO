/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.services;

import java.security.cert.X509Certificate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.net.ssl.SSLException;

import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.idb.cacao.web.utils.HttpUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Service;

/**
 * Service for supplying new instances of 'ClientHttpRequestFactory' designated for internal
 * use only (internal services)
 */
@Service
public class InternalHttpRequestsService implements Supplier<ClientHttpRequestFactory> {

	private static final Logger log = Logger.getLogger(InternalHttpRequestsService.class.getName());
	
	private Pattern patternForSSLTrustServer;
	
	private DefaultHostnameVerifier verifier;
	
	public InternalHttpRequestsService(@Value("${ssl.trust.server}") String expressionForSSLTrustServer) {
		try {
			patternForSSLTrustServer = Pattern.compile(expressionForSSLTrustServer, Pattern.CASE_INSENSITIVE);
		}
		catch (Exception ex) {
			log.log(Level.SEVERE, "Error while compiling the regular expression configured at property 'ssl.trust.server'", ex);
			patternForSSLTrustServer = null;
		}
		verifier = new DefaultHostnameVerifier();
	}

	@Override
	public ClientHttpRequestFactory get() {
		return HttpUtils.getHttpRequestFactory(this::chkClientCerts, this::chkServerCerts);
	}
	
	public boolean chkClientCerts(java.security.cert.X509Certificate[] certs, String peerHostName) {
		if (log.isLoggable(Level.FINE) ) {
			printCerts("Client", certs, peerHostName);
		}
		if (patternForSSLTrustServer==null)
			return false; // reject all request if we have a problem with 'ssl.trust.server' configuration
		if (peerHostName==null)
			return false; // reject all request that does not identify the peer hostname
		return verifyCertHost(peerHostName, certs[0]) && patternForSSLTrustServer.matcher(peerHostName).find();
	}
	
	public boolean chkServerCerts(java.security.cert.X509Certificate[] certs, String peerHostName) {
		if (log.isLoggable(Level.FINE) ) {
			printCerts("Server", certs, peerHostName);
		}		
		if (patternForSSLTrustServer==null)
			return false; // reject all request if we have a problem with 'ssl.trust.server' configuration 
		if (peerHostName==null)
			return false; // reject all request that does not identify the peer hostname
		return verifyCertHost(peerHostName, certs[0]) && patternForSSLTrustServer.matcher(peerHostName).find();
	}

	public static void printCerts(String label, java.security.cert.X509Certificate[] certs, String peerHostName) {
		if (certs==null || certs.length==0) {
			if (log.isLoggable(Level.FINE))
				log.log(Level.FINE, String.format("Peer %s requested %s certificates: NONE", peerHostName, label));
		}
		else {
			StringBuilder report = new StringBuilder();
			report.append("Peer ").append(peerHostName).append(" requested ").append(certs.length).append(" ").append(label).append(" certificates:\n");
			for (java.security.cert.X509Certificate cert: certs) {
				report.append("\tSUBJECT:").append(cert.getSubjectX500Principal().getName())
				.append("\tISSUER:").append(cert.getIssuerX500Principal().getName()).append("\n");
			}
			if (log.isLoggable(Level.FINE))
				log.log(Level.FINE, report.toString());			
		}
	}
	
	public boolean verifyCertHost(final String host, final X509Certificate cert) {
		try {
			verifier.verify(host, cert);
			return true;
		} catch (SSLException e) {
			return false;
		}		
	}
}
