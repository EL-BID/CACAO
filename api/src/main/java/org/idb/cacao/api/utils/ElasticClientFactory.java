/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.api.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.core.env.Environment;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.util.StreamUtils;
import org.springframework.data.elasticsearch.client.ClientConfiguration.MaybeSecureClientConfigurationBuilder;
import org.springframework.data.elasticsearch.client.ClientConfiguration.TerminalClientConfigurationBuilder;

/**
 * Factory class for creating 'RestHighLevelClient' objects
 * 
 * @author Gustavo Figueiredo
 *
 */
public abstract class ElasticClientFactory {
	
	private static final Logger log = Logger.getLogger(ElasticClientFactory.class.getName());

	public static final String PROPERTY_HOST = "es.host";
	public static final String PROPERTY_PORT = "es.port";
	public static final String PROPERTY_SSL = "es.ssl";
	public static final String PROPERTY_VERIFY_HOST = "es.ssl.verifyhost";
	public static final String PROPERTY_USER = "es.user";
	public static final String PROPERTY_PASSWORD = "es.password";
	public static final String PROPERTY_TIMEOUT = "spring.elasticsearch.rest.connection-timeout";

	
	/**
	 * Default host address for ElasticSearch service
	 */
	public static final String DEFAULT_HOST = "127.0.0.1";

	/**
	 * Default port number for ElasticSearch service
	 */
	public static final int DEFAULT_PORT = 9200;

	/**
	 * Default port number for SSL
	 */
	public static final int DEFAULT_SSL_PORT = 443;
	
	/**
	 * Maximum size for passwords
	 */
	private static final int MAX_SIZE_FOR_PASSWORD = 512;
	
	private ElasticClientFactory() {
		// This is a utility class
	}

	/**
	 * Object used for configurations about the RestClient that should be built in order
	 * to access an ElasticSearch service
	 *
	 */
	public static class Builder {
		
		private String host = DEFAULT_HOST;
		
		private int port = DEFAULT_PORT;
		
		private boolean ssl;
		
		private boolean verifySSLHost;
		
		private String username;
		
		private char[] password;
		
		private String timeout;
		
		private static final Function<ClientConfiguration, RestHighLevelClient> CLIENT_SUPPLIER = 
				(clientConfiguration)->RestClients.create(clientConfiguration).rest();
		
		public Builder() { 
			
		}
		
		public Builder(Environment env) {
    		host(env.getProperty(PROPERTY_HOST));
    		port(env.getProperty(PROPERTY_PORT));
        	ssl("true".equalsIgnoreCase(env.getProperty(PROPERTY_SSL)) || port==DEFAULT_SSL_PORT);
        	verifySSLHost(!"false".equalsIgnoreCase(env.getProperty(PROPERTY_VERIFY_HOST)));
    		username(env.getProperty(PROPERTY_USER));
    		password(readESPassword(env));
    		timeout(env.getProperty(PROPERTY_TIMEOUT));
		}

		public String getHost() {
			return host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public Builder host(String host) {
			setHost(host);
			return this;
		}

		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}
		
		public Builder port(int port) {
			setPort(port);
			return this;
		}

		public Builder port(String port) {
			if (port!=null && port.trim().length()>0)
				setPort(Integer.parseInt(port));
			return this;
		}

		public boolean isSSL() {
			return ssl;
		}

		public void setSSL(boolean ssl) {
			this.ssl = ssl;
		}
		
		public Builder ssl(boolean ssl) {
			setSSL(ssl);
			return this;
		}

		public boolean isVerifySSLHost() {
			return verifySSLHost;
		}

		public void setVerifySSLHost(boolean verifySSLHost) {
			this.verifySSLHost = verifySSLHost;
		}		
		
		public Builder verifySSLHost(boolean verifySSLHost) {
			setVerifySSLHost(verifySSLHost);
			return this;
		}
		
		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}
		
		public Builder username(String username) {
			setUsername(username);
			return this;
		}

		public void setPassword(char[] password) {
			this.password = password;
		}
		
		public Builder password(char[] password) {
			setPassword(password);
			return this;
		}

		public Builder password(String password) {
			if (password!=null)
				setPassword(password.toCharArray());
			return this;
		}

		public String getTimeout() {
			return timeout;
		}

		public void setTimeout(String timeout) {
			this.timeout = timeout;
		}
		
		public Builder timeout(String timeout) {
			setTimeout(timeout);
			return this;
		}

		/**
		 * Build an ElasticSearch client according to the configurations provided.
		 */
		public RestHighLevelClient build() {
			String esUrl = String.format("%s:%d", 
					getHost(), 
					getPort());

			TerminalClientConfigurationBuilder clientConfigurationBuilder;
					
			if (isSSL()) {
				MaybeSecureClientConfigurationBuilder builder = ClientConfiguration.builder().connectedTo(esUrl);
				if (isVerifySSLHost()) {
					clientConfigurationBuilder = builder.usingSsl();
				}
				else {
					try {
						TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
					    SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
						clientConfigurationBuilder = builder.usingSsl(sslContext, NoopHostnameVerifier.INSTANCE);
					} catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
						throw new ElasticClientInitException(e);
					}
				}
			}
			else {
				clientConfigurationBuilder = ClientConfiguration.builder().connectedTo(esUrl);
			}
					
			if (username!=null && username.trim().length()>0) {
				String pass = (this.password==null) ? "" : new String(this.password);
				clientConfigurationBuilder.withBasicAuth(username, pass);
			}

			if (timeout!=null && timeout.trim().length()>0) {
				clientConfigurationBuilder.withConnectTimeout(Duration.parse("PT"+timeout.toUpperCase()));
				clientConfigurationBuilder.withSocketTimeout(Duration.parse("PT"+timeout.toUpperCase()));
			}

			ClientConfiguration clientConfiguration = clientConfigurationBuilder.build();
			
	        return CLIENT_SUPPLIER.apply(clientConfiguration);
		}
		
	}
	
	/**
	 * Exception related to failure while initializing ElasticSearch client
	 */
	public static class ElasticClientInitException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		
		public ElasticClientInitException(Exception ex) {
			super(ex);
		}
		
	}

	/**
	 * Read the content of the property containing the password for connection to ElasticSearch. Admits the indication
	 * of a filename instead of the password itself if the provided content starts with the prefix 'file:'
	 */
	public static String readESPassword(Environment env) {
		String password = env.getProperty(PROPERTY_PASSWORD);
		if (password==null || password.trim().length()==0)
			return null;
		// Check if the property value refers to a file (it could be a 'secret' mounted at runtime). In this case will try to load it contents.
		if (password.startsWith("file:")) {
			String passwordFile = password.substring("file:".length());
			File file = new File(passwordFile);
			if (!file.exists()) {
				if (log.isLoggable(Level.SEVERE))
					log.log(Level.SEVERE, String.format("File informed in '%s' does not exist!", PROPERTY_PASSWORD));
				return null;
			}
			if (file.length()==0) {
				if (log.isLoggable(Level.SEVERE))
					log.log(Level.SEVERE, String.format("File informed in '%s' is empty!", PROPERTY_PASSWORD));
				return null;
			}
			if (file.length()>MAX_SIZE_FOR_PASSWORD) {
				if (log.isLoggable(Level.SEVERE))
					log.log(Level.SEVERE, String.format("File informed in '%s' is too big!", PROPERTY_PASSWORD));
				return null;
			}
			try (InputStream input = new BufferedInputStream(new FileInputStream(file));) {
				password = StreamUtils.copyToString(input, StandardCharsets.UTF_8);
				return password;
			}
			catch (IOException ex) {
				if (log.isLoggable(Level.SEVERE))
					log.log(Level.SEVERE, "Error reading contents from file informed in '"+PROPERTY_PASSWORD+"'", ex);
				return null;
			}
		}
		else {
			return password;
		}
	}
}
