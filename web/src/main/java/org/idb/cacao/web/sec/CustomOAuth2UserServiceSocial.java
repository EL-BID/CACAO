/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.sec;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
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
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Callback for retrieving user information once the user has logged in using standard OAUTH2 protocol.
 * 
 * This applies to FACEBOOK and others that requires an OAuth2UserRequest.
 * 
 * @author Gustavo Figueiredo
 *
 */
@Service
@Transactional
public class CustomOAuth2UserServiceSocial extends DefaultOAuth2UserService // OAuth2UserService<OAuth2UserRequest,OAuth2User> 
{

	private static final Logger log = Logger.getLogger(CustomOAuth2UserServiceSocial.class.getName());
	
	public static final String ATTRIBUTE_NAME = "name";
	public static final String ATTRIBUTE_EMAIL = "email";

	@Autowired
	private UserService userService;

    @Autowired
    private UserRepository userRepository;
    
	@Autowired
	private Environment env;

    @Autowired
    private PrivilegeService privilegeService;

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		// Delegate to the default implementation for loading a user
		OAuth2User oauthUser = super.loadUser(userRequest);
		if (oauthUser==null)
			return null;
		
		String fullName = oauthUser.getAttribute(ATTRIBUTE_NAME);
		String email = oauthUser.getAttribute(ATTRIBUTE_EMAIL);
		
		if (email==null) {
			throw new BadCredentialsException("Bad credentials");
		}
		
		User user = userRepository.findByLoginIgnoreCaseAndActiveIsTrue(email);
		
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, () -> "OAUTH-LOGIN: fullname:"+fullName
			+" email:"+email
			+" attributes:"+oauthUser.getAttributes()
			);
		}
		
		if (user == null && fullName!=null && fullName.trim().length()>0 && email.trim().length()>0) {
			if ("true".equalsIgnoreCase(env.getProperty("auto.create.users"))) {
				user = userService.createUser(email, fullName, userRequest);
			}
		}
		
        if (user == null || user.getLogin()==null) {
        	throw new BadCredentialsException("Bad credentials");
        }
        
        return new CustomOAuth2User(user, oauthUser);
	}

	public Collection<GrantedAuthority> getUserAuthorities(User user) {
		if (user==null || user.getProfile()==null)
			return Collections.emptyList();
		return privilegeService.getGrantedAuthorities(user.getProfile());
	}

	public class CustomOAuth2User extends DefaultOAuth2User {
		private static final long serialVersionUID = 1L;
		private final User user;
		public CustomOAuth2User(User user, OAuth2User oauthUser) {
			super(getUserAuthorities(user), oauthUser.getAttributes(), ATTRIBUTE_NAME);
			this.user = user;
		}
		public User getUser() {
			return user;
		}
		@Override
		public String getName() {
			return user.getName();
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getEnclosingInstance().hashCode();
			result = prime * result + Objects.hash(user);
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			CustomOAuth2User other = (CustomOAuth2User) obj;
			if (!getEnclosingInstance().equals(other.getEnclosingInstance()))
				return false;
			return Objects.equals(user, other.user);
		}
		private CustomOAuth2UserServiceSocial getEnclosingInstance() {
			return CustomOAuth2UserServiceSocial.this;
		}		
	}

}
