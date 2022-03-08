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
package org.idb.cacao.web.controllers.services;

import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.idb.cacao.web.utils.HttpUtils;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Service;

/**
 * Service for supplying new instances of 'ClientHttpRequestFactory' designated for internal
 * use only (internal services)
 */
@Service
public class InternalHttpRequestsService implements Supplier<ClientHttpRequestFactory> {

	private static final Logger log = Logger.getLogger(InternalHttpRequestsService.class.getName());

	@Override
	public ClientHttpRequestFactory get() {
		return HttpUtils.getHttpRequestFactory(this::chkClientCerts, this::chkServerCerts);
	}
	
	public boolean chkClientCerts(java.security.cert.X509Certificate[] certs, String peerHostName) {
		if (log.isLoggable(Level.WARNING) ) {
			printCerts("Client", certs, peerHostName);
		}
		return true;
	}
	
	public boolean chkServerCerts(java.security.cert.X509Certificate[] certs, String peerHostName) {
		if (log.isLoggable(Level.WARNING) ) {
			printCerts("Server", certs, peerHostName);
		}		
		return true;
	}

	public static void printCerts(String label, java.security.cert.X509Certificate[] certs, String peerHostName) {
		if (certs==null || certs.length==0) {
			if (log.isLoggable(Level.WARNING))
				log.log(Level.WARNING, String.format("Peer %s requested %s certificates: NONE", peerHostName, label));
		}
		else {
			StringBuilder report = new StringBuilder();
			report.append("Peer ").append(peerHostName).append(" requested ").append(certs.length).append(" ").append(label).append(" certificates:\n");
			for (java.security.cert.X509Certificate cert: certs) {
				report.append("\tSUBJECT:").append(cert.getSubjectX500Principal().getName())
				.append("\tISSUER:").append(cert.getIssuerX500Principal().getName()).append("\n");
			}
			if (log.isLoggable(Level.WARNING))
				log.log(Level.WARNING, report.toString());			
		}
	}
}
