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
package org.idb.cacao.web.conf;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.idb.cacao.api.utils.ElasticClientFactory;
import org.idb.cacao.web.controllers.services.ElasticSearchService;
import org.idb.cacao.web.controllers.services.UserService;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.utils.ControllerUtils;
import org.springframework.beans.BeansException;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Configuration of a PROXY to the Kibana User Interface through CACAO web application<BR>
 * <BR>
 * This proxy will forward the requests to the Kibana endpoints if and only if the user has
 * been authenticated at CACAO web application and has been granted enough privileges for this
 * operation.
 * 
 * @author Gustavo Figueiredo
 *
 */
@Configuration
public class KibanaProxyServletConfiguration implements EnvironmentAware, ApplicationContextAware {

	private static final Logger log = Logger.getLogger(KibanaProxyServletConfiguration.class.getName());

	/**
	 * Default Kibana host address unless otherwise configured with 'kibana.host' application property
	 */
	public static final String DEFAULT_KIBANA_HOST = "127.0.0.1";
	
	/**
	 * Default Kibana port number unless otherwise configured with 'kibana.port' application property
	 */
	public static final int DEFAULT_KIBANA_PORT = 5601;
	
	/**
	 * Default Kibana endpoint unless otherwise configured with 'kibana.endpoint' application property
	 */
	public static final String DEFAULT_KIBANA_ENDPOINT = "/kibana";
	
	/**
	 * Pattern with the list of URI's to be blocked for security reason
	 */
	public static final Pattern BLOCK_LIST = Pattern.compile("(?>/tutorial|/ingest_manager|/api/sample_data)", Pattern.CASE_INSENSITIVE);
	
	/**
	 * Small time elapsed in milliseconds. Prevent checking again user profiles repeatedly.
	 */
	public static final long SMALL_TIME_ELAPSED_AVOID_REDUNDANT_CHECK_USER_PROFILE = 2000;

	private Environment propertyResolver;
	
	private ApplicationContext applicationContext;
	
	/**
	 * Synchronization object per user login
	 */
	private static final ConcurrentHashMap<String, Object> SYNC_OBJECT_PER_LOGIN = new ConcurrentHashMap<>();

	/**
	 * Maps the endpoint '/kibana' in this web application to the corresponding endpoint at Kibana user interface
	 */
	@SuppressWarnings("unchecked")
	@Bean
	public ServletRegistrationBean<Servlet> servletRegistrationBean(){
		@SuppressWarnings("rawtypes")
		ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean(
				new KibanaProxy(propertyResolver, applicationContext), 
				"/kibana/*");
		servletRegistrationBean.addInitParameter("targetUri", (isSSL()?"https":"http")+"://"+getKibanaHost()+":"+getKibanaPort()+getKibanaEndpoint());
		servletRegistrationBean.addInitParameter("log", "false");
		return servletRegistrationBean;
	}

	public String getKibanaHost() {
		if (propertyResolver==null)
			return DEFAULT_KIBANA_HOST;
		return propertyResolver.getProperty("kibana.host", DEFAULT_KIBANA_HOST);
	}
	
	public int getKibanaPort() {
		if (propertyResolver==null)
			return DEFAULT_KIBANA_PORT;
		return Integer.parseInt(propertyResolver.getProperty("kibana.port", String.valueOf(DEFAULT_KIBANA_PORT)));		
	}
	
	public boolean isSSL() {
    	if ("true".equalsIgnoreCase(propertyResolver.getProperty("es.ssl")))
    		return true;
    	String kibana_port = propertyResolver.getProperty("kibana.port");
    	return "443".equals(kibana_port);
	}
	
	public String getKibanaEndpoint() {
		if (propertyResolver==null)
			return DEFAULT_KIBANA_ENDPOINT;
		return propertyResolver.getProperty("kibana.endpoint", DEFAULT_KIBANA_ENDPOINT);		
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.propertyResolver = environment;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
	
	/**
	 * Implementation of a proxy for forwarding user HTTP requests to Kibana. Handles user authentication automatically.
	 * 
	 * @author Gustavo Figueiredo
	 *
	 */
	public static class KibanaProxy extends org.mitre.dsmiley.httpproxy.ProxyServlet {
		private static final long serialVersionUID = 1L;
		
		private static boolean sslVerifyHost;
		private static boolean authenticationEnabled;
		private static UserService userService;
		private static String kibanaSuperUser;
		private static char[] esname;
		private static char[] espass;
		private final Map<String, CheckedUserStatus> checkedUserStatusMap;

		public KibanaProxy(Environment propertyResolver, ApplicationContext app) {
			this.checkedUserStatusMap = new ConcurrentHashMap<>();
			KibanaProxy.sslVerifyHost = !"false".equalsIgnoreCase(propertyResolver.getProperty("es.ssl.verifyhost"));
			KibanaProxy.userService = app.getBean(UserService.class);
			if (ControllerUtils.isJUnitTest()) {
				log.log(Level.INFO, "Skipping KibanaProxy initialization because it's running in a JUnit test case");
				return;
			}
			String esUsername = propertyResolver.getProperty("es.user");
			ElasticSearchService service = app.getBean(ElasticSearchService.class);
			try {
				service.assertStandardSpaces();
			}
			catch (Exception ex) {
				log.log(Level.SEVERE, "Error configuring Kibana Spaces", ex);						
			}
			if (esUsername!=null && esUsername.trim().length()>0) {
				try {
					service.assertStandardRoles();
					KibanaProxy.kibanaSuperUser = propertyResolver.getProperty("kibana.superuser");
					if (kibanaSuperUser!=null && kibanaSuperUser.trim().length()>0) {
						KibanaProxy.esname = esUsername.toCharArray();
						String p = ElasticClientFactory.readESPassword(propertyResolver);
						KibanaProxy.espass = (p!=null) ? p.toCharArray() : null;
					}
					KibanaProxy.authenticationEnabled = true;
				}
				catch (Exception ex) {
					log.log(Level.SEVERE, "Error initializing KibanaProxy", ex);
					KibanaProxy.authenticationEnabled = false; // leave authentication process for Kibana itself (will prompt user for login and password)
				}
			}
		}

		@Override
		protected HttpClientBuilder getHttpClientBuilder() {
			HttpClientBuilder builder = super.getHttpClientBuilder();
			if (!sslVerifyHost) {
				try {
					TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
				    SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
					SSLConnectionSocketFactory sslConnectionFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
			        builder.setSSLSocketFactory(sslConnectionFactory);
				}
				catch (Exception ex) {
					log.log(Level.SEVERE, "Error configuring SSL for Proxy", ex);
				}
			}
			return builder;
		}

		@Override
		protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ServletException, IOException {
			if (BLOCK_LIST.matcher(servletRequest.getRequestURI()).find()) {
				servletResponse.setStatus(HttpStatus.SC_FORBIDDEN); // 403
				servletResponse.getWriter().write("Forbidden");
				log.log(Level.INFO, "Forbidden: "+servletRequest.getRequestURI());
				return;				
			}
			
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth==null) {
				servletResponse.setStatus(HttpStatus.SC_UNAUTHORIZED); // 401
				servletResponse.getWriter().write("Not authenticated");                    
				return;
			}
			User user = userService.getUser(auth);
			if (user==null) {
				servletResponse.setStatus(HttpStatus.SC_UNAUTHORIZED); // 401
				servletResponse.getWriter().write("Not authenticated");                    
				return;					
			}
			
			CheckedUserStatus checkedUserStatus;
			try {
				checkedUserStatus = checkUserAccountForKibanaAccess(user);
			}
			catch (Exception ex) {
				log.log(Level.SEVERE, "Error while checking user account for Kibana access", ex);
				servletResponse.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR); // 500
				servletResponse.getWriter().write("Internal server error");                    
				return;
			}
			
			if (checkedUserStatus.usertoken!=null) {
				// Pass information to be consumed in 'copyRequestHeaders'
				servletRequest.setAttribute("kibana.proxy.username", checkedUserStatus.username);
				servletRequest.setAttribute("kibana.proxy.usertoken", checkedUserStatus.usertoken);				
			}
			else if (KibanaProxy.authenticationEnabled) {
				// If the authentication at Kibana is enforced and if 'checkUserAccountForKibanaAccess' resulted
				// in no user token, then we assume this user is not authorized for this service.
				servletResponse.setStatus(HttpStatus.SC_UNAUTHORIZED); // 401
				servletResponse.getWriter().write("Not authorized");                    
				return;										
			}
			
			super.service(servletRequest, servletResponse);
		}
		
		/**
		 * Before forwarding the user requests to Kibana, let's see if the did the 'housekeeping' work (i.e. setting user private 'space',
		 * setting user 'roles', setting user 'account', etc.).
		 * @param user User object account. Must be the same object persisted in database
		 */
		protected CheckedUserStatus checkUserAccountForKibanaAccess(User user) throws IOException {
			// Before we verify user account, let's check when we performed these same checks for this user last time
			CheckedUserStatus checkedUserStatus = checkedUserStatusMap.get(user.getId());
			if (checkedUserStatus!=null && (System.currentTimeMillis()-checkedUserStatus.timestamp)<SMALL_TIME_ELAPSED_AVOID_REDUNDANT_CHECK_USER_PROFILE) {
				// Avoid too much redundant validations at a short period of time 
				return checkedUserStatus;
			}
			synchronized (SYNC_OBJECT_PER_LOGIN.computeIfAbsent(user.getLogin(),k->new Object())) {
				// Repeat the same temporal check again (due to possible concurrent requests from the same user)
				checkedUserStatus = checkedUserStatusMap.get(user.getId());
				if (checkedUserStatus!=null && (System.currentTimeMillis()-checkedUserStatus.timestamp)<SMALL_TIME_ELAPSED_AVOID_REDUNDANT_CHECK_USER_PROFILE) {
					// Avoid too much redundant validations at a short period of time 
					return checkedUserStatus;
				}
				
				// Perform the actual verifications...
				
				checkedUserStatus = new CheckedUserStatus();
				
				boolean isSuperUser = (authenticationEnabled 
						&& kibanaSuperUser!=null && kibanaSuperUser.equalsIgnoreCase(user.getLogin())
						&& esname!=null && espass!=null);

				// If the user is a super user, use the service account
				
				if (isSuperUser) {
					
					checkedUserStatus.username = new String(esname);
					checkedUserStatus.usertoken = new String(espass);					
					
				}
				
				else {

					// If the user is capable of writing dashboards, there should exist a personal 'space' dedicated to the user besides the 'public' ones
					if ((user.getKibanaSpace()==null || user.getKibanaSpace().trim().length()==0)
						&& (isSuperUser || userService.hasDashboardWriteAccess(user))) {
						// Create a Kibana Space for private user access if the user has write access
						try {
							userService.createSpaceForPrivateDashboards(user, /*createCompanionRole*/authenticationEnabled);
						}
						catch (Exception ex) {
							log.log(Level.SEVERE, "Error creating Kibana Space for user "+user.getLogin(), ex);						
						}
					}
					
					if (authenticationEnabled) {

						if (user.getKibanaToken()==null || user.getKibanaToken().trim().length()==0) {
							// Create a Kibana User for the user if there is read or write access
							userService.createUserForKibanaAccess(user);
						}
						
						if (user.getKibanaToken()!=null && user.getKibanaToken().trim().length()>0) {
							checkedUserStatus.username = user.getLogin();
							checkedUserStatus.usertoken = userService.decryptKibanaToken(user.getKibanaToken());
						}
						
					}

				}
				
				// Prevent multiple successive checks like the ones above for multiple resources accessed in a short period of time
				// from the same user
				checkedUserStatus.timestamp = System.currentTimeMillis();
				checkedUserStatusMap.put(user.getId(), checkedUserStatus);
				
				return checkedUserStatus;
			}
		}
	
		@Override
		protected void copyRequestHeaders(HttpServletRequest servletRequest, HttpRequest proxyRequest) {
			super.copyRequestHeaders(servletRequest, proxyRequest);
			if (authenticationEnabled) {
				// Pass credentials to Kibana
				String username = (String)servletRequest.getAttribute("kibana.proxy.username");
				String usertoken = (String)servletRequest.getAttribute("kibana.proxy.usertoken");
				if (username!=null && username.length()>0 && usertoken!=null && usertoken.length()>0) {
					String encoding = Base64.getEncoder().encodeToString((String.join(":", username, usertoken)).getBytes());
					proxyRequest.setHeader(HttpHeaders.AUTHORIZATION, "Basic "+encoding);					
				}
			}
		}
		
		/**
		 * This object holds information checked about some user account. It's preserved for a short time in memory.
		 */
		private static class CheckedUserStatus {
			long timestamp;
			String username;
			String usertoken;
		}
	}

}
