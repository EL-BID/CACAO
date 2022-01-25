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
package org.idb.cacao.web.entities;

/**
 * Enumerates all the privileges associated to different parts of the system. These
 * privileges are associated with user profiles (UserProfile) by means of application
 * properties. This means that each user profile may have many privileges, and each
 * privilege may be associated to one or or user profiles.
 * 
 * @author Gustavo Figueiredo
 *
 */
public enum SystemPrivilege {
	
	/**
	 * Privilege for running system administrative operations
	 */
	ADMIN_OPS,
	
	/**
	 * Privilege for reading or listing any type of communications
	 */
	COMMUNICATION_READ,
	
	/**
	 * Privilege for uploading or deleting generic files used for communications
	 */
	COMMUNICATION_UPLOAD,

	/**
	 * Privilege for changing any type of communications (except private communications)
	 */
	COMMUNICATION_WRITE,

	/**
	 * Privilege for changing private communications
	 */
	COMMUNICATION_WRITE_PRIVATE,

	/**
	 * Privilege for creating and making use of an individual API TOKEN
	 */
	CONFIG_API_TOKEN,
	
	/**
	 * Privilege for changing configuration of system mail delivery
	 */
	CONFIG_SYSTEM_MAIL,
	
	/**
	 * Privilege for reading or listing interpersonal configurations for all taxpayers
	 */
	INTERPERSONAL_READ_ALL,
	
	/**
	 * Privilege for changing interpersonal configurations
	 */
	INTERPERSONAL_WRITE,
	
	/**
	 * Privilege for running SYNC (synchronization of databases) operations
	 */
	SYNC_OPS,

	/**
	 * Privilege for reading or listing tax declarations
	 */
	TAX_DECLARATION_READ,

	/**
	 * Privilege for reading or listing tax declarations of any taxpayer
	 */
	TAX_DECLARATION_READ_ALL,
	
	/**
	 * Privilege for uploading tax declaration (either for himself or someone he represents)
	 */
	TAX_DECLARATION_WRITE,
	
	/**
	 * Privilege for uploading a empty tax declaration on behalf of any taxpayer
	 */
	TAX_DECLARATION_WRITE_EMPTY,
	
	/**
	 * Privilege for changing tax templates
	 */
	TAX_TEMPLATE_WRITE,
	
	/** 
	 * Privilege for changing tax domain tables
	 */
	TAX_DOMAIN_TABLE_WRITE,
	
	/**
	 * Privilege for reading or listing taxpayers registry
	 */
	TAXPAYER_READ,

	/**
	 * Privilege for changing taxpayers registry
	 */
	TAXPAYER_WRITE,
	
	/**
	 * Privilege for reading or listing user recent access list
	 */
	USER_RECENT_READ,
	
	/**
	 * Privilege for reading or listing user access history
	 */
	USER_HISTORY_READ,
	
	/**
	 * Privilege for reading or listing user accounts
	 */
	USER_READ,

	/**
	 * Privilege for changing user accounts
	 */
	USER_WRITE,
	
	/**
	 * Privilege for generating tax reports
	 */
	TAX_REPORT_READ;
	
	/**
	 * Return the role name associated to this system privilege
	 */
	public String getRole() {
		// IMPORTANT:
		// All role names must be prefixed with 'ROLE_' since it's the Spring standard (otherwise
		// we would need to add more customizations to Spring configuration)
		return "ROLE_"+name();
	}

}
