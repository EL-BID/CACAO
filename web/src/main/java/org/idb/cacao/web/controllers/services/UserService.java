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

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.repositories.ESStandardRoles;
import org.idb.cacao.web.repositories.InterpersonalRepository;
import org.idb.cacao.web.repositories.UserRepository;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.security.user.privileges.Role;
import org.elasticsearch.search.sort.SortOrder;
import org.idb.cacao.web.controllers.AdvancedSearch;
import org.idb.cacao.web.entities.Interpersonal;
import org.idb.cacao.web.entities.RelationshipType;
import org.idb.cacao.web.entities.SystemPrivilege;
import org.idb.cacao.web.entities.UserProfile;
import org.idb.cacao.web.errors.MissingParameter;
import org.idb.cacao.web.utils.ESUtils;
import org.idb.cacao.web.utils.ESUtils.KibanaSpace;
import org.idb.cacao.web.utils.HttpUtils;
import org.idb.cacao.web.utils.SearchUtils;
import org.idb.cacao.web.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

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
    private MessageSource messages;
    
    @Autowired
    private InterpersonalRepository interpersonalRepository;
    
	@Autowired
	private RestHighLevelClient elasticsearchClient;
	
	@Autowired
	private ElasticSearchService elasticSearchService;

	@Autowired
	private KeyStoreService keystoreService;

	@Autowired
	private PrivilegeService privilegeService;

	@Autowired
	private RestHighLevelClient esClient;

    private RestTemplate restTemplate;
    
    public UserService(RestTemplateBuilder builder) {
		this.restTemplate = builder
				.setConnectTimeout(Duration.ofMinutes(5))
				.setReadTimeout(Duration.ofMinutes(5))
				.requestFactory(HttpUtils::getTrustAllHttpRequestFactory)
				.build();
	}

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
					user.setProfile(UserProfile.SYSADMIN);
					userRepository.saveWithTimestamp(user);
				}
			}
			
			// If no user has any profile, configure the built-in first user as SYSADMIN
			else if (userRepository.countByProfileIsNotNull()==0) {

				String login = env.getProperty("first.master.user.login");
				if (login!=null && login.trim().length()>0) {

					User first_sysadmin_user = userRepository.findByLogin(login);
					if (first_sysadmin_user!=null) {

						log.log(Level.WARNING, "Configuring SYSADMIN profile for user: "+login);
						first_sysadmin_user.setProfile(UserProfile.SYSADMIN);
						userRepository.saveWithTimestamp(first_sysadmin_user);

					}
					
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
		user.setProfile(UserProfile.DECLARANT);
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
		user.setProfile(UserProfile.DECLARANT);
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
	@Transactional(readOnly=true)
	public Page<User> searchUsers(Optional<AdvancedSearch> filters,
			Optional<Integer> page, 
			Optional<Integer> size) {
		try {
			return SearchUtils.doSearch(filters.get().wiredTo(messages), User.class, elasticsearchClient, page, size, Optional.of("name"), Optional.of(SortOrder.ASC));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}
	
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
    	boolean readAll = roles.stream().anyMatch(a-> a.getAuthority().equals("TAX_DECLARATION_READ_ALL"));
    	final Set<String> filter_taxpayers_ids;
    	if (!readAll) {
        	User user = getUser(auth);
        	if (user==null)
        		return Collections.emptySet();
    		String user_taxpayer_id = userRepository.findById(user.getId()).map(User::getTaxpayerId).orElse(null);
    		if (user_taxpayer_id==null || user_taxpayer_id.trim().length()==0)
    			throw new MissingParameter(messages.getMessage("user.missing.taxpayerid", null, LocaleContextHolder.getLocale()));
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
	
	/**
	 * Returns indication that the user has any Kibana Access (read or write)
	 */
	public boolean hasKibanaAccess(User user) {
		Set<ESStandardRoles> standard_roles = ESStandardRoles.getStandardRoles(user.getProfile());
		return standard_roles!=null && !standard_roles.isEmpty();
	}

	/**
	 * Check if the user is a super user at Kibana
	 */
	public boolean hasKibanaUserUserAccess(User user) {
		String es_username = env.getProperty("es.user");
		String es_password = env.getProperty("es.password");
		String kibanaSuperUser = env.getProperty("kibana.superuser");

		boolean is_kibana_super_user = (es_username!=null && es_username.trim().length()>0 
				&& es_password!=null && es_password.trim().length()>0
				&& kibanaSuperUser!=null && kibanaSuperUser.equalsIgnoreCase(user.getLogin()));
		
		return is_kibana_super_user;
	}

	/**
	 * Check if the user may have a private space at Kibana for his dashboards
	 */
	public boolean mayHaveSpaceForPrivateDashboards(User user) {
		
		String es_username = env.getProperty("es.user");
		String es_password = env.getProperty("es.password");
		String kibanaSuperUser = env.getProperty("kibana.superuser");

		boolean is_kibana_super_user = (es_username!=null && es_username.trim().length()>0 
				&& es_password!=null && es_password.trim().length()>0
				&& kibanaSuperUser!=null && kibanaSuperUser.equalsIgnoreCase(user.getLogin()));
		
		// the super-user does not have a private space for himself
		if (is_kibana_super_user)
			return false; 
		
		// a user without write permission does not have a private space for himself
		if (!hasDashboardWriteAccess(user))
			return false;
		
		// a user without tax payer ID does not have a private space for himself
		if (user.getTaxpayerId()==null || user.getTaxpayerId().trim().length()==0)
			return false;
		
		String txid = user.getTaxpayerId().replaceAll("\\D", ""); // removes all non-numeric digits
		if (txid.length()==0)
			return false;

		// Any other user does have the possibility to have a private space for himself
		return true;
	}

	/**
	 * Returns indication that the user has Kibana Access with 'write' privilege to Dashboards
	 */
	public boolean hasDashboardWriteAccess(User user) {
		
		// Users with some profile mapped to a Kibana ROLE related to 'public area' with 'write access' may write dashboards
		Set<ESStandardRoles> standard_roles = ESStandardRoles.getStandardRoles(user.getProfile());
		if (standard_roles!=null && !standard_roles.isEmpty() && standard_roles.contains(ESStandardRoles.DASHBOARDS_WRITE))
			return true;
		
		// Users with 'TAX_DECLARATION_READ_ALL' privilege may write dashboards at his own private area
		Set<SystemPrivilege> user_privileges = privilegeService.getPrivileges(user.getProfile());
		return user_privileges!=null && user_privileges.contains(SystemPrivilege.TAX_DECLARATION_READ_ALL);
	}
	
	/**
	 * Returns the URI for access to Dashboards. If the user has a specific taxpayer ID, he will be redirected
	 * to a private SPACE at Kibana that corresponds to his ID. Otherwise, returns the link to the 'default space'.
	 */
	public String getDashboardsURI() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = (auth==null) ? null : getUser(auth);

		String uri;
		
		if (user!=null && mayHaveSpaceForPrivateDashboards(user)) {
			String txid = user.getTaxpayerId().replaceAll("\\D", "");
			String personal_space_id = "user-"+txid;
			uri = env.getProperty("kibana.menu.link")+"/s/"+personal_space_id+"/app/dashboards#";
		}
		else {
			uri = env.getProperty("kibana.menu.link")+"/app/dashboards#";
		}
		
		return uri;
	}
	
	/**
	 * Creates a SPACE at KIBANA for private user access. Also creates a ROLE giving access to this space as long as 'createCompanionRole' is TRUE.<BR>
	 * The ID of the database will be formed with the user taxpayer ID, so that different user accounts related to the same taxpayer ID shares
	 * the same Kibana space.
	 */
	public void createSpaceForPrivateDashboards(User user, boolean createCompanionRole) throws IOException {
		
		if (user.getTaxpayerId()==null || user.getTaxpayerId().trim().length()==0)
			return;
		
		String txid = user.getTaxpayerId().replaceAll("\\D", ""); // removes all non-numeric digits
		if (txid.length()==0)
			return;
		
		String personal_space_id = "user-"+txid;
		
		if (!ESUtils.hasKibanaSpace(env, restTemplate, personal_space_id)) {
		
			KibanaSpace space = new KibanaSpace();
			space.setId(personal_space_id);
			space.setName(user.getName());
			space.setDescription("Personal area for dashboards and other objects accessible by the user");
			space.setDisabledFeatures(ElasticSearchService.getDisabledFeatures(/*keep features*/ "discover","visualize","dashboard"));
			ESUtils.createKibanaSpace(env, restTemplate, space);
			elasticSearchService.copyKibanaConfigFromSpaceToSpace("default", space.getId());
		}
		
		if (createCompanionRole) {
			
			// Creates the 'companion role' to this particular 'space' if it does not exist yet
			// The 'companion role' gives access to the space for dashboard and visualization features.
			Role role = getOrCreateCompanionRoleForKibanaAccess(user);
			
			if (role!=null) {
				String role_name = role.getName();
				// If there is a user account, update it with the role we just created (also works for assigning existent role to existent user account)
				org.elasticsearch.client.security.user.User user_at_es =
						ESUtils.getUser(esClient, user.getLogin());
				if (user_at_es!=null && (user_at_es.getRoles()==null || !user_at_es.getRoles().contains(role_name))) {
					List<String> roles = new LinkedList<>();
					roles.add(role_name);
					if (user_at_es.getRoles()!=null)
						roles.addAll(user_at_es.getRoles());
					try {
						ESUtils.updateUser(esClient, user.getLogin(), roles);
					}
					catch (Throwable ex) {
						log.log(Level.SEVERE, "Error assigning role "+role_name+" to user "+user.getLogin()+" for access to Kibana personal space "+personal_space_id, ex);
					}
				}
			}
		}

		// Updates this information at database so that we don't have to go through all this work again for this user
		user.setKibanaSpace(personal_space_id);
		userRepository.saveWithTimestamp(user);
	}

	/**
	 * Returns an existent 'companion role' or creates a new one.<BR>
	 * The 'companion role' is a role created for a particular user granting access to this particular Kibana Space.
	 */
	public Role getOrCreateCompanionRoleForKibanaAccess(User user) {
		
		if (user.getTaxpayerId()==null || user.getTaxpayerId().trim().length()==0)
			return null;
		
		String txid = user.getTaxpayerId().replaceAll("\\D", ""); // removes all non-numeric digits
		if (txid.length()==0)
			return null;

		String role_name = "roleUser"+txid;
		String personal_space_id = "user-"+txid;

		Role role;
		try {
			role =
				ESUtils.getRole(esClient, role_name);
		}
		catch (Throwable ex) {
			role = null;				
		}
		
		if (role==null) {
			try {
				role = ESUtils.createRoleForSingleApplication(esClient, role_name, /*application*/"kibana-.kibana", 
					/*privileges*/Arrays.asList("feature_dashboard.all","feature_discover.all","feature_visualize.all"), 
					/*resources*/Arrays.asList("space:"+personal_space_id), 
					/*allIndicesPrivileges*/Arrays.asList("read"));
			}
			catch (Throwable ex) {
				log.log(Level.SEVERE, "Error creating role "+role_name+" for user "+user.getLogin()+" for access to Kibana personal space "+personal_space_id, ex);
			}
		}
		
		return role;
	}

	/**
	 * Creates a new token for a user access to Kibana user interface
	 * @para user User object (must be the actual persistent object in database, not an object stored in the servlet context)
	 */
	@Transactional
	public void createUserForKibanaAccess(User user) throws IOException {
		
		Set<ESStandardRoles> standard_roles = ESStandardRoles.getStandardRoles(user.getProfile());
		if (standard_roles==null || standard_roles.isEmpty()) {
			
			// no access
			
			return;
		}
		
    	String kibana_token;
    	kibana_token = UUID.randomUUID().toString();
		String encrypted_kibana_token = keystoreService.encrypt(KeyStoreService.PREFIX_MAIL, kibana_token);
		user.setKibanaToken(encrypted_kibana_token);
		userRepository.saveWithTimestamp(user);

		List<String> roles = standard_roles.stream().map(ESStandardRoles::getName).collect(Collectors.toCollection(LinkedList::new));
		
		// If the user has 'write access' and also has a personal Kibana Space, creates or assigns an additional role
		// granting access to this personal Kibana Space
		if (hasDashboardWriteAccess(user) && user.getKibanaSpace()!=null && user.getKibanaSpace().trim().length()>0) {
			// Creates the 'companion role' to this particular 'space' if it does not exist yet
			// The 'companion role' gives access to the space for dashboard and visualization features.
			Role role = getOrCreateCompanionRoleForKibanaAccess(user);
			if (role!=null) {
				String role_name = role.getName();
				roles.add(role_name);
			}
		}

		ESUtils.createUser(esClient, user.getLogin(), roles, kibana_token.toCharArray());		
	}

	/**
	 * Update the user roles for a user access to Kibana user interface
	 * @para user User object (must be the actual persistent object in database, not an object stored in the servlet context)
	 */
	@Transactional
	public void updateUserForKibanaAccess(User user) throws IOException {
		
		Set<ESStandardRoles> standard_roles = ESStandardRoles.getStandardRoles(user.getProfile());
		if (standard_roles==null || standard_roles.isEmpty()) {
			
			// no access, disable user if exists
			if (!ESUtils.getUsers(esClient, user.getLogin()).isEmpty()) {
				try {
					ESUtils.disableUser(esClient, user.getLogin());
				}
				catch (Throwable ex) {
					log.log(Level.SEVERE, "Error disabling user "+user.getLogin(), ex);
				}
			}
			
			return;
		}

		List<String> roles = standard_roles.stream().map(ESStandardRoles::getName).collect(Collectors.toCollection(LinkedList::new));

		ESUtils.updateUser(esClient, user.getLogin(), roles);
		
		try {
			ESUtils.enableUser(esClient, user.getLogin());
		}
		catch (Throwable ex) {
			log.log(Level.SEVERE, "Error enabling user "+user.getLogin(), ex);
		}
	}

	/**
	 * Decrypts in memory the individual token used for Kibana access
	 */
	public String decryptKibanaToken(String encrypted_kibana_token) {
		if (encrypted_kibana_token==null || encrypted_kibana_token.trim().length()==0)
			return null;
    	String kibana_token = keystoreService.decrypt(KeyStoreService.PREFIX_MAIL, encrypted_kibana_token);
    	return kibana_token;
	}
	
	/**
	 * Returns indication that security module is enabled at ElasticSearch and user access is required for Kibana access
	 */
	public boolean hasUserControlForKibanaAccess() {
		String es_username = env.getProperty("es.user");
		return (es_username!=null && es_username.trim().length()>0);
	}

}
