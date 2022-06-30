/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
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
	private static int MAX_USERS_PER_REQUEST = 10_000;

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
			Set<User> allTargetedUsers = new TreeSet<>();
			String[] multipleAudience = audience.split(",");
			for (String part_audience: multipleAudience) {
				Set<User> partUsers = getUsersAudience(part_audience.trim());
				if (partUsers!=null && !partUsers.isEmpty())
					allTargetedUsers.addAll(partUsers);
			}
			return allTargetedUsers;
		}
		else {
			audience = audience.trim();
			Set<User> allTargetedUsers = new TreeSet<>();
			Page<User> userAudienceByName = userRepository.findByName(audience, PageRequest.of(0, 10_000));
			if (userAudienceByName!=null && userAudienceByName.hasContent())
				allTargetedUsers.addAll(userAudienceByName.getContent());
			User userAudienceByLogin = userRepository.findByLoginIgnoreCase(audience);
			if (userAudienceByLogin!=null)
				allTargetedUsers.add(userAudienceByLogin);
			Page<User> usersAudience = userRepository.findByTaxpayerId(audience, PageRequest.of(0, MAX_USERS_PER_REQUEST));
			if (!usersAudience.isEmpty())
				allTargetedUsers.addAll(usersAudience.getContent());
			UserProfile audienceAsProfile = UserProfile.parse(audience);
			if (audienceAsProfile!=null) {
				usersAudience = userRepository.findByProfile(audienceAsProfile, PageRequest.of(0, MAX_USERS_PER_REQUEST));
				if (!usersAudience.isEmpty())
					allTargetedUsers.addAll(usersAudience.getContent());
			}
			return allTargetedUsers;
		}
	}

	/**
	 * Returns TRUE if 'user' matches the target audience
	 */
	public static boolean isTargetAudience(String audience, User user) {
		if (audience==null || audience.trim().length()==0 || user==null)
			return false;
		if (audience.contains(",")) {
			String[] multipleAudience = audience.split(",");
			for (String part_audience: multipleAudience) {
				if (isTargetAudience(part_audience.trim(), user))
					return true;
			}
			return false;
		}
		else {
			if (user.getLogin()!=null && ( user.getLogin().equalsIgnoreCase(audience) || 
					user.getName().equalsIgnoreCase(audience)|| user.getTaxpayerId().equalsIgnoreCase(audience)) )
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
		
		Page<User> usersWithProfile = userRepository.findByProfile(targetProfile, PageRequest.of(0, 10_000));
		if (usersWithProfile==null || usersWithProfile.isEmpty())
			return;
		
		if (email.isPresent()) {
            ConfigEMail config = configEmailService.getActiveConfig();
            if (config==null || config.getSupportEmail()==null || config.getSupportEmail().trim().length()==0) {
            	log.log(Level.WARNING, "Supposed to send e-mail to users, but the SMTP configuration is missing!");
            }
            else {
    			for (User user_audience: usersWithProfile) {
    				try {
	        			log.log(Level.FINE, String.format("Sending e-mail message to user %s",user_audience.getName()));
	                    final SimpleMailMessage emailMsg = new SimpleMailMessage();
	                    emailMsg.setSubject(title);
	                    emailMsg.setText(email.get());
	                    emailMsg.setTo(user_audience.getLogin());
	                    emailMsg.setFrom(config.getSupportEmail());
	
	                    mailSender.send(emailMsg);
    				}
    				catch (Exception ex) {
    					log.log(Level.WARNING, String.format("Error while sending e-mail to user %s", user_audience.getLogin()), ex);
    				}
    			}
            }
		}
		
	}

}
