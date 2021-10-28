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

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.idb.cacao.web.entities.ConfigEMail;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.entities.UserProfile;
import org.idb.cacao.web.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for functionality related to 'Communication' object
 * 
 * @author Gustavo Figueiredo
 *
 */
@Service
@Transactional
public class CommunicationService {
	
	private static final Logger log = Logger.getLogger(CommunicationService.class.getName());

	/**
	 * Maximum results returned when querying users that matches a given audience criteria
	 */
	public static int MAX_USERS_PER_REQUEST = 10_000;

	@Autowired
	private UserRepository userRepository;
	
    @Autowired
    private IConfigEMailService configEmailService;

    @Autowired
    private JavaMailSender mailSender;

	/**
	 * Returns all users objects that matches a given 'audience' criteria
	 */
	public Set<User> getUsersAudience(String audience) {
		if (audience==null || audience.trim().length()==0)
			return Collections.emptySet();
		if (audience.contains(",")) {
			Set<User> all_targeted_users = new TreeSet<>();
			String[] multiple_audience = audience.split(",");
			for (String part_audience: multiple_audience) {
				Set<User> part_users = getUsersAudience(part_audience.trim());
				if (part_users!=null && !part_users.isEmpty())
					all_targeted_users.addAll(part_users);
			}
			return all_targeted_users;
		}
		else {
			audience = audience.trim();
			Set<User> all_targeted_users = new TreeSet<>();
			Page<User> user_audience_by_name = userRepository.findByName(audience, PageRequest.of(0, 10_000));
			if (user_audience_by_name!=null && user_audience_by_name.hasContent())
				all_targeted_users.addAll(user_audience_by_name.getContent());
			User user_audience_by_login = userRepository.findByLoginIgnoreCase(audience);
			if (user_audience_by_login!=null)
				all_targeted_users.add(user_audience_by_login);
			Page<User> users_audience = userRepository.findByTaxpayerId(audience, PageRequest.of(0, MAX_USERS_PER_REQUEST));
			if (!users_audience.isEmpty())
				all_targeted_users.addAll(users_audience.getContent());
			UserProfile audience_as_profile = UserProfile.parse(audience);
			if (audience_as_profile!=null) {
				users_audience = userRepository.findByProfile(audience_as_profile, PageRequest.of(0, MAX_USERS_PER_REQUEST));
				if (!users_audience.isEmpty())
					all_targeted_users.addAll(users_audience.getContent());
			}
			return all_targeted_users;
		}
	}

	/**
	 * Returns TRUE if 'user' matches the target audience
	 */
	public static boolean isTargetAudience(String audience, User user) {
		if (audience==null || audience.trim().length()==0 || user==null)
			return false;
		if (audience.contains(",")) {
			String[] multiple_audience = audience.split(",");
			for (String part_audience: multiple_audience) {
				if (isTargetAudience(part_audience.trim(), user))
					return true;
			}
			return false;
		}
		else {
			if (user.getLogin()!=null && user.getLogin().equalsIgnoreCase(audience))
				return true;
			if (user.getName()!=null && user.getName().equalsIgnoreCase(audience))
				return true;
			if (user.getTaxpayerId()!=null && user.getTaxpayerId().equalsIgnoreCase(audience))
				return true;
			if (user.getProfile()!=null && (user.getProfile().getRole().equalsIgnoreCase(audience)
					|| user.getProfile().name().equalsIgnoreCase(audience)))
				return true;
			return false;
		}
	}

	/**
	 * Notifies all users with the given profile about something.
	 * @param title Title of the message (e-mail or telegram)
	 * @param email Optional parameter indicating the e-mail to send
	 */
	public void notifyAllUsersWithProfile(UserProfile targetProfile, String title, Optional<String> email) {
		if (targetProfile==null)
			return;
		
		Page<User> users_with_profile = userRepository.findByProfile(targetProfile, PageRequest.of(0, 10_000));
		if (users_with_profile==null || users_with_profile.isEmpty())
			return;
		
		if (email.isPresent()) {
            ConfigEMail config = configEmailService.getActiveConfig();
            if (config==null || config.getSupportEmail()==null || config.getSupportEmail().trim().length()==0) {
            	log.log(Level.WARNING, "Supposed to send e-mail to users, but the SMTP configuration is missing!");
            }
            else {
    			for (User user_audience: users_with_profile) {
    				try {
	        			log.log(Level.FINE, "Sending e-mail message to user "+user_audience.getName());
	                    final SimpleMailMessage email_msg = new SimpleMailMessage();
	                    email_msg.setSubject(title);
	                    email_msg.setText(email.get());
	                    email_msg.setTo(user_audience.getLogin());
	                    email_msg.setFrom(config.getSupportEmail());
	
	                    mailSender.send(email_msg);
    				}
    				catch (Throwable ex) {
    					log.log(Level.WARNING, "Error while sending e-mail to user "+user_audience.getLogin(), ex);
    				}
    			}
            }
		}
		
	}

}
