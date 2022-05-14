/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.sec;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.idb.cacao.web.conf.KibanaProxyServletConfiguration;
import org.idb.cacao.web.utils.LoginUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * All configurations related to security and login
 * 
 * @author Gustavo Figueiredo
 *
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
	
	/**
	 * This is the default Content Security Policy directive to be returned as HTTP header for almost all responses
	 * produced by this web application. 
	 */
	private static final String DEFAULT_CSP_DIRECTIVE = 
			  "default-src 'none'; "
			+ "script-src 'self' 'nonce-{nonce}'; "	// The {nonce} part is replaced by internals of 'CSPNonceFilter'
			+ "style-src https: 'unsafe-inline' ; "
			+ "img-src https: data:; "
			+ "font-src 'self' https://fonts.gstatic.com data:; "
			+ "frame-ancestors 'self'; "
			+ "form-action 'self'; "
			+ "connect-src 'self'; " // for auto-complete (e.g. while filling the form for creating a simplified payment)
			+ "child-src 'self' https:; "	// for child frames, including external mobile payment platforms redirected from here
			+ "object-src 'none'";

	/**
	 * This is an alternative Content Security Policy directive to be returned as HTTP header exclusively for responses
	 * including reference to Vega.JS and VegaLite.JS, because they require the use of 'unsafe-eval' at 'script-src' due
	 * to the use of 'new Function()' inside this visualization library.
	 * @see https://github.com/vega/vega/issues/1106 (issue still open to this today)
	 */
	private static final String VEGA_COMPATIBLE_CSP_DIRECTIVE = 
			  "default-src 'none'; "
			+ "script-src 'self' 'nonce-{nonce}' 'unsafe-eval'; "	// The {nonce} part is replaced by internals of 'CSPNonceFilter'
			+ "style-src https: 'unsafe-inline' ; "
			+ "img-src https: data:; "
			+ "font-src 'self' https://fonts.gstatic.com data:; "
			+ "frame-ancestors 'self'; "
			+ "form-action 'self'; "
			+ "connect-src 'self'; " // for auto-complete (e.g. while filling the form for creating a simplified payment)
			+ "child-src 'self' https:; "	// for child frames, including external mobile payment platforms redirected from here
			+ "object-src 'none'";
	
	/**
	 * The name of the CSP header according to https://www.w3.org/TR/CSP2/
	 */
	private static final String CONTENT_SECURITY_POLICY_HEADER = "Content-Security-Policy";

	/**
	 * Handles authentication failures
	 */
    @Autowired
    private AuthenticationFailureHandler authenticationFailureHandler;

    /**
     * Access to application properties
     */
	@Autowired
	private Environment env;

	/**
	 * Provides more information about logged users
	 */
    @Autowired
    private UserDetailsService userDetailsService;

    /**
     * Service for OAUTH2 login (e.g. Google and Microsoft)
     */
    @Autowired
    private OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService;
    
    /**
     * Service for OAUTH2 login for social media (e.g. Facebook)
     */
    @Autowired
    DefaultOAuth2UserService oidcUserServiceSocial;
    
    /**
     * Configurable endpoint for Kibana pages proxied by this web application
     */
    @Value("${kibana.endpoint}")
    private String kibanaEndpoint;

    /**
     * Process an authentication request
     */
    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		// authentication manager 
		auth.authenticationProvider(authProvider());
	}

	@Override
	public void configure(WebSecurity web) throws Exception {
		// http builder configurations for authorize requests and form login 
		web.ignoring().antMatchers("/resources/**");
	}

    @Override
	protected void configure(HttpSecurity http) throws Exception {
        http
        .csrf()
        	// ignore Kibana-related endpoints since they are managed by Kibana
        	.ignoringRequestMatchers(kibanaRequestMatcher())
        	.and()
        .addFilterBefore(apiAuthenticationFilter(), CsrfFilter.class)      
        .addFilterBefore(new CSPNonceFilter(), HeaderWriterFilter.class)
        .addFilterAfter(new SwaggerUIAuthenticationFilter(), HeaderWriterFilter.class)
        .headers()
        	.frameOptions().sameOrigin()
        	.referrerPolicy(ReferrerPolicy.SAME_ORIGIN).and() // includes a 'Referrer-Policy: same-origin' header for increased security measure
        	.contentSecurityPolicy(DEFAULT_CSP_DIRECTIVE).and()
        .and()
        
        // WHO CAN DO WHAT ...
        .authorizeRequests()
        
    	// DISABLE HTTP METHODS THAT ARE NOT USED BY OUR APPLICATION
        	.antMatchers(HttpMethod.OPTIONS).denyAll()
        	.antMatchers(HttpMethod.TRACE).denyAll()
        
            .antMatchers(
            		
        		// ALLOW ANY STATIC RESOURCES
        		"/resources/**", "/components/**", "/images/**", "/themes/**", 
        		"/js/**", 
        		"/css/**",
        		"/.well-known/**",
        		"/google*.html",
        		
        		// ALLOW ERROR PAGE
        		"/error*", "/emailError*", 
        		
        		// ALLOW FIRST PAGE
        		"/", "/index*", "/login*", "/privacy*", "/terms*", "/license*",
        		"/institutional*","/usermanual*",  
        		"/forgetPassword*", "/resetPassword*", "/savePassword*", "/updatePassword*")
            
            .permitAll()

            // Page with REST endpoints
            .antMatchers("/swagger-ui/**").hasAnyRole("SYSADMIN","SUPPORT","CONFIG_API_TOKEN")
            
            // ALL OTHERS URL's NEED AUTHENTICATION
            .anyRequest().authenticated()
            
            ;
        
        // Includes configuration related to OAUTH2
        
        if (LoginUtils.hasOIDCProviders(env)) {
        	http.oauth2Login()
	        	.loginPage("/login")
	        	.defaultSuccessUrl("/home")
	            .successHandler(authSucessHandler())
	        	.userInfoEndpoint()
	        	.oidcUserService(oidcUserService)
	        	.and();
        }
            
        // LOGIN PAGES ...
        http.formLogin()
            .loginPage("/login")
            .loginProcessingUrl("/login")
            .defaultSuccessUrl("/home")
            .successHandler(authSucessHandler())
            .failureUrl("/login?error=true")
            .failureHandler(authenticationFailureHandler)
            .permitAll()
            
        .and()
        
        // LOGOUT PAGES ...
        .logout()
            .invalidateHttpSession(false)
            .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
            .logoutSuccessUrl("/login?logout=true")
            .deleteCookies("JSESSIONID")
            .permitAll();
        
	}

	@Bean
    public DaoAuthenticationProvider authProvider() {
        final CustomAuthenticationProvider authProvider = new CustomAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(encoder());
        return authProvider;
    }
	
	@Bean
	public AuthenticationSuccessHandler authSucessHandler() {
		return new AuthenticationSuccessHandler() {
			
			@Override
			public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
					throws IOException, ServletException {
				// Force redirection to 'home' after a successful authentication
				// It's important because we may have been logged out from the application after expired session or because the application
				// has restarted. 
				// The login page is always shown as 'top' frame, never as a child frame. There is a javascript code in 'login.html' ensuring this is the case, otherwise
				// we would incur in a problem with 'X-Frame-Options' denial when user tries to login with OAUTH2. We are not allowing 'X-Frame-Options' at login page
				// for security reason.
				// Due to this, after a sucessfull authentication we must show the full page including menu bar.
				
				response.sendRedirect("/home");
			}
		};
	}

    @Bean
    public PasswordEncoder encoder() {
        return new BCryptPasswordEncoder(11);
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }
    
    @Bean
    public ApiKeyAuthenticationFilter apiAuthenticationFilter() {
    	return new ApiKeyAuthenticationFilter();
    }

	@Bean("CustomUserDetailsService")
	@Override
	public UserDetailsService userDetailsService() {
		return userDetailsService;
	}

	/**
	 * Returns a RequestMatcher designated to allow access to the proxied Kibana endpoints. Kibana itself offers defense
	 * against CSRF attacks - @see https://www.elastic.co/guide/en/kibana/current/security-best-practices.html#_cross_site_request_forgery_csrfxsrf
	 */
	private RequestMatcher kibanaRequestMatcher() {
		
		String kibanaEndpointExpression = Optional.ofNullable(this.kibanaEndpoint).orElse(KibanaProxyServletConfiguration.DEFAULT_KIBANA_ENDPOINT);
		if (kibanaEndpointExpression.trim().length()==0 || "/".equalsIgnoreCase(kibanaEndpointExpression.trim())) {
			// If we have not defined a kibana endpoint, let's return an 'admits nothing' RequestMatcher so that there won't be any exceptions
			// for CSRF
			return (r)->false;
		}
				
		if (!kibanaEndpointExpression.startsWith("/"))
			kibanaEndpointExpression = "/" + kibanaEndpointExpression;
		if (!kibanaEndpointExpression.endsWith("/"))
			kibanaEndpointExpression += "/";
		kibanaEndpointExpression += "**";
		
		return new AntPathRequestMatcher(kibanaEndpointExpression);
	}
	
	/**
	 * Set the response header related to CSP for compatibility with JS code dependent on Vega/VegaLite visualization library.
	 */
	public static void setVegaCompatibleCSPDirective(HttpServletResponse response) {
		response.setHeader(CONTENT_SECURITY_POLICY_HEADER, VEGA_COMPATIBLE_CSP_DIRECTIVE);
	}
}
