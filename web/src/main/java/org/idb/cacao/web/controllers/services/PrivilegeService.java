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
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.idb.cacao.web.entities.SystemPrivilege;
import org.idb.cacao.web.entities.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

/**
 * Service for managing system privileges and its association to user profiles
 * 
 * @author Gustavo Figueiredo
 *
 */
@Service
public class PrivilegeService {

	@Autowired
	private Environment env;

	private Map<UserProfile,Set<SystemPrivilege>> map_user_profiles_to_privileges;

	private Map<SystemPrivilege,Set<UserProfile>> map_privileges_to_user_profiles;
	
	/**
	 * Returns system privileges associated to user profile
	 */
	public Set<SystemPrivilege> getPrivileges(UserProfile profile) {
		if (profile==null)
			return Collections.emptySet();
		assertInternalMaps();
		return map_user_profiles_to_privileges.get(profile);
	}
	
	/**
	 * Returns TRUE if the UserProfile contains the given SystemPrivilege
	 */
	public boolean hasPrivilege(UserProfile profile, SystemPrivilege privilege) {
		if (profile==null || privilege==null)
			return false;
		Set<SystemPrivilege> privileges = getPrivileges(profile);
		return privileges.contains(privilege);
	}

	/**
	 * Returns user profiles associated to system privilege
	 */
	public Set<UserProfile> getUserProfiles(SystemPrivilege privilege) {
		if (privilege==null)
			return Collections.emptySet();
		assertInternalMaps();
		return map_privileges_to_user_profiles.get(privilege);
	}
	
	/**
	 * Returns the list of granted authority objects for Spring Security configuration
	 */
	public List<GrantedAuthority> getGrantedAuthorities(UserProfile profile) {
		List<GrantedAuthority> authorities = new LinkedList<>();
		if (profile!=null) {
			authorities.add(new SimpleGrantedAuthority(profile.getRole()));
			for (SystemPrivilege privilege: getPrivileges(profile)) {
				authorities.add(new SimpleGrantedAuthority(privilege.getRole()));
			}
		}
		return authorities;
	}

	/**
	 * Populates internal maps with information provided with application properties
	 */	
	private synchronized void assertInternalMaps() {
		if (map_user_profiles_to_privileges==null) {
			Map<UserProfile,Set<SystemPrivilege>> tempMapUserProfilesToPrivileges = new EnumMap<>(UserProfile.class);
			Map<SystemPrivilege,Set<UserProfile>> tempMapPrivilegesToUserProfiles = new EnumMap<>(SystemPrivilege.class);
			
			for (SystemPrivilege privilege: SystemPrivilege.values()) {
				String property = getEnvProperty(privilege);
				Set<UserProfile> userProfiles = parseUserProfileNames(property);
				tempMapPrivilegesToUserProfiles.put(privilege, userProfiles);
				for (UserProfile profile: userProfiles) {
					tempMapUserProfilesToPrivileges.computeIfAbsent(profile, k->new TreeSet<>()).add(privilege);
				}
			}

			for (UserProfile profile: UserProfile.values()) {
				tempMapUserProfilesToPrivileges.computeIfAbsent(profile, k->new TreeSet<>());
			}

			this.map_user_profiles_to_privileges = tempMapUserProfilesToPrivileges;
			this.map_privileges_to_user_profiles = tempMapPrivilegesToUserProfiles;
		}
	}
	
	/**
	 * Returns property related to some privilege
	 */
	public String getEnvProperty(SystemPrivilege priv) {
		String prop = env.getProperty("privilege."+priv.name());
		if (prop==null) {
			// try again with lower cases
			prop = env.getProperty("privilege."+priv.name().toLowerCase());
		}
		return prop;
	}
	
	/**
	 * Parse a list of UserProfile names and return the corresponding array.
	 */
	public Set<UserProfile> parseUserProfileNames(String names) {
		if (names==null || names.trim().length()==0)
			return Collections.emptySet();
		names = names.trim();
		String[] values = names.split("[\\,\\t\\;\\|]"); // admits different separators
		List<UserProfile> parsed = new LinkedList<>();
		for (String value: values) {
			value = value.trim().toUpperCase();
			if (value.startsWith("ROLE_"))
				value = value.substring("ROLE_".length()).trim();
			UserProfile profile;
			try {
				profile = UserProfile.parse(value);
			}
			catch (Exception ex) {
				profile = null; // invalid profile name
			}
			if (profile==null) {
				// Try other variations
				if (value.startsWith("ADMIN"))
					profile = UserProfile.SYSADMIN;
				else if (value.startsWith("TECHNIC"))
					profile = UserProfile.SUPPORT;
			}
			if (profile!=null) 
				parsed.add(profile);
		}
		if (parsed.isEmpty())
			return Collections.emptySet();
		else
			return parsed.stream().distinct().collect(Collectors.toCollection(TreeSet::new));
	}
}
