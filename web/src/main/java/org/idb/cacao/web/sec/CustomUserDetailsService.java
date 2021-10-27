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

import org.idb.cacao.web.controllers.services.UserService;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

	@Override
	public UserDetails loadUserByUsername(String name) throws UsernameNotFoundException {
		
		userService.assertInitialSetup();
		
		User user = userRepository.findByLoginIgnoreCase(name);
        if (user == null || user.getLogin()==null) {
            throw new UsernameNotFoundException("No user found with username: " + name);
        }
        
        if (user.getPassword()==null || user.getPassword().trim().length()==0) {
        	throw new UsernameNotFoundException("Missing password for user: " + name);
        }

        org.springframework.security.core.userdetails.User.UserBuilder builder =
        		org.springframework.security.core.userdetails.User.builder().username(user.getLogin())
        		.authorities(getAuthorities(user));
        if (user.getPassword()!=null && user.getPassword().trim().length()>0)
        	builder = builder.password(user.getPassword());
        return builder.build();
	}

	public static Collection<GrantedAuthority> getAuthorities(User user) {
		// TODO:
		// Should return a list of 'SimpleGrantedAuthority' objects
		return Collections.emptyList();
	}
}
