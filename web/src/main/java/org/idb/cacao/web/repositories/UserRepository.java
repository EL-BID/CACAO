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
package org.idb.cacao.web.repositories;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.idb.cacao.web.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for CRUD and query operations over 'users' of this application
 * 
 * @author Gustavo Figueiredo
 *
 */
@Repository
public interface UserRepository extends ElasticsearchRepository<User, String> {

	/**
	 * Find all user records given the name, allowing pagination over the results
	 */
	public Page<User> findByName(String name, Pageable pageable);

	/**
	 * Find a user given the login (email). There should be only one.
	 */
	public User findByLogin(String login);
	
	/**
	 * Find a user given the login (email) ignoring case sensitivity. There should be only one.
	 */
	public User findByLoginIgnoreCase(String login);

	/**
	 * Find all users, allowing pagination over the results
	 */
	public Page<User> findAll(Pageable pageable);
	
	/**
	 * Save the record in database, updating the internal 'timestamp' field in order
	 * to track changes. 
	 */
	default public <S extends User> S saveWithTimestamp(S entity) {
		entity.setTimestamp(LocalDateTime.now().atOffset(ZoneOffset.UTC));
		return save(entity);
	}

}
