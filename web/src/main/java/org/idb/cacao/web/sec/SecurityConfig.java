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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.idb.cacao.web.utils.LoginUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

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
        	.ignoringAntMatchers("/kibana/**")
        	.and()
        .addFilterBefore(apiAuthenticationFilter(), CsrfFilter.class)      
        .addFilterBefore(new CSPNonceFilter(), HeaderWriterFilter.class)
        .addFilterAfter(new SwaggerUIAuthenticationFilter(), HeaderWriterFilter.class)
        .headers()
        	.frameOptions().sameOrigin()
        	.referrerPolicy(ReferrerPolicy.SAME_ORIGIN).and() // includes a 'Referrer-Policy: same-origin' header for increased security measure
        	.contentSecurityPolicy("default-src 'none'; "
        			+ "script-src 'self' 'nonce-{nonce}' 'unsafe-eval'; "	// The {nonce} part is replaced by internals of 'CSPNonceFilter'
        			+ "style-src https: 'unsafe-inline' ; "
        			+ "img-src https: data:; "
        			+ "font-src 'self' https://fonts.gstatic.com data:; "
        			+ "frame-ancestors 'self'; "
        			+ "form-action 'self'; "
        			+ "connect-src 'self'; " // for auto-complete (e.g. while filling the form for creating a simplified payment)
        			+ "child-src 'self' https:; "	// for child frames, including external mobile payment platforms redirected from here
        			+ "object-src 'none'").and()
        .and()
        
        // WHO CAN DO WHAT ...
        .authorizeRequests()
            .antMatchers(
            		
        		// ALLOW ANY STATIC RESOURCES
        		"/resources/**", "/components/**", "/images/**", "/themes/**", 
        		"/js/**", 
        		"/css/**",
        		"/.well-known/**",
        		"/google*.html",
        		
        		// ALLOW ERROR PAGE
        		"/error*", "/emailError*", 
        		
        		// ALLOW PUBLIC API
        		// TODO:
        		
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

}
