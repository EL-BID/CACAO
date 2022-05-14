/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.repositories;

import java.util.Optional;

import org.idb.cacao.api.utils.DateTimeUtils;
import org.idb.cacao.web.Synchronizable;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.entities.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.CountQuery;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for CRUD and query operations over 'users' of this application
 * 
 * @author Gustavo Figueiredo
 *
 */
@Repository
@Synchronizable(timestamp="timestamp",id="id")
public interface UserRepository extends ElasticsearchRepository<User, String> {
	
	/**
	 * Find a user given the user id. There should be only one.
	 */
	public Optional<User> findById(String id);

	/**
	 * Find all user records given the name, allowing pagination over the results
	 */
	public Page<User> findByName(String name, Pageable pageable);

	/**
	 * Find a user given the login (email). There should be only one.
	 */
	public User findByLoginAndActiveIsTrue(String login);
	
	/**
	 * Find a user given the login (email) ignoring case sensitivity. There should be only one.
	 */
	public User findByLoginIgnoreCase(String login);
	
	/**
	 * Find an active user given the login (email) ignoring case sensitivity. There should be only one.
	 */
	public User findByLoginIgnoreCaseAndActiveIsTrue(String login);

	/**
	 * Find all users, allowing pagination over the results
	 */
	public Page<User> findAll(Pageable pageable);
	
	/**
	 * Count the number of User records that has some UserProfile defined
	 */
	@CountQuery("{ \"bool\": { \"filter\": [{ \"exists\": { \"field\": \"profile\" }}, { \"term\": {\"active\": true }}] }}")
	public long countByProfileIsNotNullAndActiveIsTrue();
	
	/**
	 * Save the record in database, updating the internal 'timestamp' field in order
	 * to track changes. 
	 */
	default public <S extends User> S saveWithTimestamp(S entity) {
		entity.setTimestamp(DateTimeUtils.now());
		return save(entity);
	}

	//TODO Check the method use
	default public Page<User> findByProfile(UserProfile profileName, PageRequest of) {
		return null;
	}

	//TODO Check the method use
	default public Page<User> findByTaxpayerId(String taxpayerId, PageRequest of) {
		return null;
	}

}
