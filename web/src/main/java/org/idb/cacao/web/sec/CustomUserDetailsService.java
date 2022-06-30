/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.sec;

import java.util.Collection;
import java.util.Collections;

import org.idb.cacao.web.controllers.services.PrivilegeService;
import org.idb.cacao.web.controllers.services.UserService;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides information about logged user
 * 
 * @author Gustavo Figueiredo
 *
 */
@Service
@Transactional
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserService userService;

    @Autowired
    private PrivilegeService privilegeService;

	@Override
	public UserDetails loadUserByUsername(String name) throws UsernameNotFoundException {
		
		userService.assertInitialSetup();
		
		User user = userRepository.findByLoginIgnoreCaseAndActiveIsTrue(name);
        if (user == null || user.getLogin()==null) {
        	throw new BadCredentialsException("Bad credentials");
        }
        
        if (user.getPassword()==null || user.getPassword().trim().length()==0) {
        	throw new BadCredentialsException("Bad credentials");
        }

        org.springframework.security.core.userdetails.User.UserBuilder builder =
        		org.springframework.security.core.userdetails.User.builder().username(user.getLogin())
        		.authorities(getAuthorities(user));
        if (user.getPassword()!=null && user.getPassword().trim().length()>0)
        	builder = builder.password(user.getPassword());
        return builder.build();
	}

	public Collection<GrantedAuthority> getAuthorities(User user) {
		if (user==null || user.getProfile()==null)
			return Collections.emptyList();
		return privilegeService.getGrantedAuthorities(user.getProfile());
	}
}
