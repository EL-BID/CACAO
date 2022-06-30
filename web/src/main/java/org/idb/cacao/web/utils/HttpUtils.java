/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.utils;

import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.function.BiPredicate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

/**
 * Utility methods for HTTP operations
 */
public class HttpUtils {
	
	/**
	 * Returns a 'HTTP Request Factory' for validating SSL communication. Usefull for dealing
	 * with communications with internal services provided inside the application platform
	 * (e.g. ElasticSearch and Kibana).<BR>
	 * Should not be used with external servers.
	 * @param chkClient Function that must be provided in order to check if the client certificates should be accepted in SSL handshaking. The second
	 * argument corresponds to the peer hostname obtained from the inbound socket (may be NULL).
	 * @param chkServer Function that must be provided in order to check if the server certificates should be accepted in SSL handshaking. The second
	 * argument corresponds to the peer hostname obtained from the inbound socket (may be NULL).
	 */
	public static ClientHttpRequestFactory getHttpRequestFactory(
			BiPredicate<java.security.cert.X509Certificate[], String> chkClient,
			BiPredicate<java.security.cert.X509Certificate[], String> chkServer) {
		
		TrustManager[] trustAllCerts = new TrustManager[] {
                new X509ExtendedTrustManager() {
					
					@Override
					public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
							throws CertificateException {
						String peerHostname = getPeerHostname(engine);
                    	if (chkServer==null || !chkServer.test(chain, peerHostname)) {
                    		throw new CertificateException("Server certificates not accepted!");                    		
                    	}
					}
					
					@Override
					public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
							throws CertificateException {
						String peerHostname = getPeerHostname(socket);
                    	if (chkServer==null || !chkServer.test(chain, peerHostname)) {
                    		throw new CertificateException("Server certificates not accepted!");                    		
                    	}
					}
					
					@Override
					public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
							throws CertificateException {
						String peerHostname = getPeerHostname(engine);
                    	if (chkClient==null || !chkClient.test(chain, peerHostname)) {
                    		throw new CertificateException("Client certificates not accepted!");
                    	}
					}
					
					@Override
					public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
							throws CertificateException {
						String peerHostname = getPeerHostname(socket);
                    	if (chkClient==null || !chkClient.test(chain, peerHostname)) {
                    		throw new CertificateException("Client certificates not accepted!");
                    	}
					}
						
					@Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    
					@Override
                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] chain, String authType) 
                            		throws CertificateException {
						checkClientTrusted(chain, authType, (Socket)null);
                    }
                    
					@Override
                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] chain, String authType) 
                            		throws CertificateException {
						checkServerTrusted(chain, authType, (Socket)null);
                    }
                }
        }; 
		SSLContext sslContext;
		try {
			sslContext = SSLContext.getInstance("TLSv1.2");
	        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build(); 
		HttpComponentsClientHttpRequestFactory customRequestFactory = new HttpComponentsClientHttpRequestFactory();
        customRequestFactory.setHttpClient(httpClient);
        return customRequestFactory;
	}

	/**
	 * Extract and returns remote IP address from a given {@link HttpServletRequest}
	 * 
	 * @param request	An object {@link HttpServletRequest} 
	 * @return	Remote IP address
	 */
	public static String getRemoteIPAddress(HttpServletRequest request) {
		return (request!=null && request.getRemoteAddr()!=null && request.getRemoteAddr().trim().length()>0) ? request.getRemoteAddr() : null;
	}
	
	/**
	 * Returns the peer hostname related to a net socket. Returns NULL if could not determine the peer hostname.
	 */
	public static String getPeerHostname(Socket socket) {
		if (socket==null || !socket.isConnected() || !(socket instanceof SSLSocket))
			return null;
		SSLSocket sslSocket = (SSLSocket)socket;
		SSLSession session = sslSocket.getHandshakeSession();
		if (session==null)
			return null;
		return session.getPeerHost();
	}

	/**
	 * Returns the peer hostname related to a SSL engine object. Returns NULL if could not determine the peer hostname.
	 */
	public static String getPeerHostname(SSLEngine engine) {
		if (engine==null)
			return null;
		SSLSession session = engine.getHandshakeSession();
		if (session==null)
			return null;
		return session.getPeerHost();
	}
}
