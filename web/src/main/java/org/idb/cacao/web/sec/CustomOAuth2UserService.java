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

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.idb.cacao.web.controllers.services.PrivilegeService;
import org.idb.cacao.web.controllers.services.UserService;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Callback for retrieving user information once the user has logged in using standard OAUTH2 protocol.
 * 
 * This applies to GOOGLE, MICROSOFT AZURE and others that requires an OidcUserRequest.
 * 
 * @author Gustavo Figueiredo
 *
 */
@Service
@Transactional
public class CustomOAuth2UserService implements OAuth2UserService<OidcUserRequest, OidcUser> {
	
	private static final Logger log = Logger.getLogger(CustomOAuth2UserService.class.getName());

	@Autowired
	private UserService userService;

    @Autowired
    private UserRepository userRepository;
    
	@Autowired
	private Environment env;

    @Autowired
    private PrivilegeService privilegeService;

    private final OidcUserService delegate;
    
    public CustomOAuth2UserService() {
    	delegate = new OidcUserService();
    }

	@Override
	public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
		
		// Delegate to the default implementation for loading a user
		OidcUser oidcUser = delegate.loadUser(userRequest);
		
		String full_name = oidcUser.getFullName();
		String email = oidcUser.getEmail();
		
		if (email==null) {
			throw new BadCredentialsException("Bad credentials");
		}
		
		User user = userRepository.findByLoginIgnoreCaseAndActiveIsTrue(email);
		
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, () -> "OAUTH-LOGIN: fullname:"+oidcUser.getFullName()
			+" email:"+oidcUser.getEmail()
			+" name:"+oidcUser.getName()
			+" given name:"+oidcUser.getGivenName()
			+" subject:"+oidcUser.getSubject()
			);
		}
		
		if (user == null && full_name!=null && full_name.trim().length()>0 && email!=null && email.trim().length()>0) {
			if ("true".equalsIgnoreCase(env.getProperty("auto.create.users"))) {
				user = userService.createUser(email, full_name, userRequest);
			}
		}
		
        if (user == null || user.getLogin()==null) {
        	throw new BadCredentialsException("Bad credentials");
        }
        
         return new CustomOidcUser(user, oidcUser);
	}

	public Collection<GrantedAuthority> getUserAuthorities(User user) {
		if (user==null || user.getProfile()==null)
			return Collections.emptyList();
		return privilegeService.getGrantedAuthorities(user.getProfile());
	}

	public class CustomOidcUser extends DefaultOidcUser {
		private static final long serialVersionUID = 1L;
		private final User user;
		public CustomOidcUser(User user, OidcUser oidcUser) {
			super(getUserAuthorities(user), oidcUser.getIdToken(), oidcUser.getUserInfo());
			this.user = user;
		}
		public User getUser() {
			return user;
		}
		@Override
		public String getName() {
			return user.getName();
		}
	}
}
