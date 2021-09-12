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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service methods for user operations and queries
 * 
 * @author Gustavo Figueiredo
 *
 */
@Service
public class UserService {

	private static final Logger log = Logger.getLogger(UserService.class.getName());

    @Autowired
    private UserRepository userRepository;
	
	@Autowired
	private Environment env;
    
	/**
	 * Check if the system has a minimum of one user. If the database is fully empty, creates
	 * a new user in order to allow all of the system configurations.
	 */
	@Transactional
	public void assertInitialSetup() {
		
		try {
			// Populate with first master-user if there is none
			if (userRepository.count()==0) {
				String login = env.getProperty("first.master.user.login");
				String password = env.getProperty("first.master.user.password");
				String name = env.getProperty("first.master.user.name");
				
				if ((login!=null && login.trim().length()>0)
					&& (password!=null && password.trim().length()>0)) {
					log.log(Level.WARNING, "Creating first user in empty database: "+login);
					User user = new User();
					user.setLogin(login);
					user.setPassword(encodePassword(password));
					user.setName(name);
					userRepository.saveWithTimestamp(user);
				}
			}
		}
		catch (Throwable ex) {
			log.log(Level.SEVERE, "Error during initialization", ex);
		}

	}
	
	/**
	 * One-way password encoding
	 */
	public String encodePassword(String password) {
		return new BCryptPasswordEncoder(11).encode(password);
	}
	
	/**
	 * Creates a new user with DECLARANT profile given an OIDC token
	 */
	@Transactional
	public User createUser(String email, String full_name, OidcUserRequest userRequest) {
		log.log(Level.INFO,"Creating new user '"+email+"' with name '"+full_name+"' based on OIDC Provider with access token "
				+userRequest.getAccessToken());
		User user = new User();
		user.setLogin(email);
		user.setName(full_name);
		try {
			userRepository.save(user);
		}
		catch (Throwable ex) {
			// In case of any error, check if the user has already been created
			try {
				user = userRepository.findByLoginIgnoreCase(email);
			}
			catch (Throwable ex2) { 
				user = null;
			}
			if (user==null)
				throw ex;
		}
		return user;
	}

	/**
	 * Creates a new user with DECLARANT profile given an OAUTH2 token
	 */
	@Transactional
	public User createUser(String email, String full_name, OAuth2UserRequest userRequest) {
		log.log(Level.INFO,"Creating new user '"+email+"' with name '"+full_name+"' based on OAUTH2 Provider with access token "
				+userRequest.getAccessToken());
		User user = new User();
		user.setLogin(email);
		user.setName(full_name);
		try {
			userRepository.save(user);
		}
		catch (Throwable ex) {
			// In case of any error, check if the user has already been created
			try {
				user = userRepository.findByLoginIgnoreCase(email);
			}
			catch (Throwable ex2) { 
				user = null;
			}
			if (user==null)
				throw ex;
		}
		return user;
	}
	
}
