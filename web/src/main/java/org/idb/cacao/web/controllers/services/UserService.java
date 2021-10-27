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

import static org.idb.cacao.web.utils.ControllerUtils.searchPage;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.repositories.InterpersonalRepository;
import org.idb.cacao.web.repositories.UserRepository;
import org.idb.cacao.web.entities.Interpersonal;
import org.idb.cacao.web.entities.RelationshipType;
import org.idb.cacao.web.entities.UserProfile;
import org.idb.cacao.web.errors.MissingParameter;
import org.idb.cacao.web.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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

	/**
	 * Maximum number of taxpayers that we will return for a given tax manager.
	 * Should not be too high because it would compromise queries whenever used as query criteria
	 */
	public static final int MAX_TAXPAYERS_PER_TAXMANAGER = 10_000;
	
    @Autowired
    private UserRepository userRepository;
	
	@Autowired
	private Environment env;
	
	private final AtomicBoolean initialSetup = new AtomicBoolean();
    
    @Autowired
    MessageSource messages;
	
    
    @Autowired
    InterpersonalRepository interpersonalRepository;
/*    
    @PersistenceContext
    EntityManager entityManager;
*/
	/**
	 * Check if the system has a minimum of one user. If the database is fully empty, creates
	 * a new user in order to allow all of the system configurations.
	 */
	@Transactional
	public synchronized void assertInitialSetup() {
		
		if (!initialSetup.compareAndSet(false, true))
			return;
		
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
	
	/**
	 * Return the User object associated to the authentication object. Enforce this information
	 * is up to date with the database.
	 */
	@Transactional(readOnly=true)
	public User getUser(Authentication auth) {
		// First get the User object associated to the authentication object
		User user = UserUtils.getUser(auth);
		if (user==null 
				&& (auth.getPrincipal() instanceof org.springframework.security.core.userdetails.User)) {
			String username = ((org.springframework.security.core.userdetails.User)auth.getPrincipal()).getUsername();
			user = userRepository.findByLogin(username);
		}
		if (user==null)
			return null;
		// Next, get the most updated information about this same user
    	Optional<User> userInDatabase = userRepository.findById(user.getId());
    	return userInDatabase.orElse(user);
	}

    /**
     * Search user objects using AdvancedSearch filters
     */
/**	@Transactional(readOnly=true)
	public Page<User> searchUsers(Optional<AdvancedSearch> filters,
			Optional<Integer> page, 
			Optional<Integer> size) {
		try {
			return SearchUtils.doSearch(filters.get().wiredTo(messages), User.class, entityManager, true, page, size, Optional.of("name"), Optional.of(SortOrder.ASC));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}
**/	
	/**
	 * Check if user is authorized to send documents for subject ID
	 * @param user User object
	 * @param subject TaxpayerID for subject
	 */
	@Transactional(readOnly=true)
	public boolean isUserAuthorizedForSubject(User user, String subject) {
		if (subject==null || subject.trim().length()==0)
			return true;
		if (user==null)
			return false;
		if (subject.equalsIgnoreCase(user.getTaxpayerId()))
			return true; // User can send his own documents
		if (user.getTaxpayerId()==null || user.getTaxpayerId().trim().length()==0)
			return false; // Unidentified users can't send documents on behalf of others
		// Locates all relationships between user and subject
		Page<Interpersonal> relationships = searchPage(()->interpersonalRepository
				.findByPersonId1AndPersonId2(user.getTaxpayerId(), 
						subject, PageRequest.of(0, 10, Sort.by("timestamp").descending())));
		if (!relationships.hasContent())
			return false;
		for (Interpersonal rel:relationships) {
			if (rel.isRemoved())
				continue; // this relationship has been revoked
			switch (rel.getRelationshipType()) {
			case LEGAL_REPRESENTATIVE:
			case DIRECTOR:
			case ACCOUNTANT:
				return true;
			default:
				continue;
			}
		}
		return false;
	}
	
	/**
	 * Return the taxpayers' IDs related to the user
	 */
	@Transactional(readOnly=true)
	public Set<String> getTaxpayersForTaxManager(User user) {
		if (user==null)
			return Collections.emptySet();
		String user_txid = user.getTaxpayerId();
		if (user_txid==null || user_txid.trim().length()==0)
			return Collections.emptySet();
		
		Set<String> txids = new TreeSet<>();
		
		txids.add(user_txid); // the user can access his own data
		
		// Locates all relationships between user and subject
		Page<Interpersonal> relationships = searchPage(()->interpersonalRepository
				.findByPersonId1AndRelationshipType(user.getTaxpayerId(), 
						RelationshipType.MANAGER.name(), PageRequest.of(0, MAX_TAXPAYERS_PER_TAXMANAGER, Sort.by("timestamp").descending())));

		if (relationships.hasContent()) {
			for (Interpersonal rel:relationships) {
				if (rel.isRemoved())
					continue; // this relationship has been revoked
				txids.add(rel.getPersonId2());
			}
		}
		return txids;
	}
	
	/**
	 * Depending on the authenticated user profile, may or may not get a collection of taxpayers' IDs for
	 * using in additional filters for other search, in order to keep the scope constrained to what the
	 * user can access. Returns NULL if the scope should not be restricted.<BR>
	 * This method consider 'tax manager' user profile (i.e.: the logged user is responsible for watching these
	 * taxpayers, or is allowed to do so).
	 */
	@Transactional(readOnly=true)
	public Set<String> getFilteredTaxpayersForUserAsManager(Authentication auth) {
    	Collection<? extends GrantedAuthority> roles = auth.getAuthorities();
    	boolean is_sysadmin_or_master = roles.stream().anyMatch(a->UserProfile.SYSADMIN.getRole().equalsIgnoreCase(a.getAuthority()))
    			|| roles.stream().anyMatch(a->UserProfile.MASTER.getRole().equalsIgnoreCase(a.getAuthority()));
    	final Set<String> filter_taxpayers_ids;
    	if (!is_sysadmin_or_master) {
        	User user = getUser(auth);
        	if (user==null)
        		return Collections.emptySet();
    		String user_taxpayer_id = userRepository.findById(user.getId()).map(User::getTaxpayerId).orElse(null);
    		if (user_taxpayer_id==null || user_taxpayer_id.trim().length()==0)
    			throw new MissingParameter(messages.getMessage("taxpayer_id", null, LocaleContextHolder.getLocale()));
    		filter_taxpayers_ids = getTaxpayersForTaxManager(user);
    	}
    	else {
    		filter_taxpayers_ids = null; // we don't need this for SYSADMIN requests
    	}
    	return filter_taxpayers_ids;
	}

	/**
	 * Depending on the authenticated user relationships, may or may not get a collection of taxpayers' IDs for
	 * using in additional filters for other search, in order to keep the scope constrained to what the
	 * user can access. Returns NULL if the scope should not be restricted.<BR>
	 * This method consider 'taxpayer' user profile (i.e.: the logged user is responsible for uploading documents
	 * and seeing information on behalf of these taxpayers).
	 */
	@Transactional(readOnly=true)
	public Set<String> getFilteredTaxpayersForUserAsTaxpayer(Authentication auth) {
		Set<String> taxpayersIds = new TreeSet<>();
		User user = getUser(auth);
		if (user==null || user.getTaxpayerId()==null || user.getTaxpayerId().trim().length()==0)
			return Collections.emptySet();
		
		taxpayersIds.add(user.getTaxpayerId());
		
		// Locates all relationships between user and other taxpayers
		Page<Interpersonal> relationships = searchPage(()->interpersonalRepository
				.findByRemovedIsFalseAndPersonId1AndRelationshipTypeIsIn(user.getTaxpayerId(), 
						Arrays.asList(RelationshipType.relationshipsForDeclarants).stream().map(RelationshipType::name).collect(Collectors.toList()), 
						PageRequest.of(0, 10_000)));
		if (!relationships.hasContent())
			return taxpayersIds;
		
		for (Interpersonal rel:relationships) {
			if (rel.isRemoved())
				continue; // this relationship has been revoked
			if (rel.getPersonId2()!=null && rel.getPersonId2().trim().length()>0)
				taxpayersIds.add(rel.getPersonId2());
		}
		return taxpayersIds;
	}

}
