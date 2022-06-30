/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.repositories;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.idb.cacao.api.utils.DateTimeUtils;
import org.idb.cacao.web.Synchronizable;
import org.idb.cacao.web.entities.Interpersonal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * DAO for Interpersonal relationship objects
 * 
 * @author Gustavo Figueiredo
 *
 */
@Repository
@Synchronizable(timestamp="changedTime",id="id")
public interface InterpersonalRepository extends ElasticsearchRepository<Interpersonal, String> {
	
	Page<Interpersonal> findByRelationshipType(String relationshipType, Pageable pageable);

	Page<Interpersonal> findByPersonId1(String personId1, Pageable pageable);
	
	List<Interpersonal> findByPersonId1In(Set<String> personId1s);

	Page<Interpersonal> findByPersonId2(String personId2, Pageable pageable);

	Page<Interpersonal> findByActiveIsTrueAndPersonId1(String personId1, Pageable pageable);

	Page<Interpersonal> findByActiveIsTrueAndPersonId1AndPersonId2(String personId1, String personId2, Pageable pageable);

	Page<Interpersonal> findByPersonId1OrPersonId2(String personId1, String personId2, Pageable pageable);

	Page<Interpersonal> findByActiveIsTrueAndPersonId1AndRelationshipType(String personId1, String relationshipType, Pageable pageable);

	@Query("{\"bool\":{\"must\":[{\"bool\":{\"should\":[{\"term\":{\"personId1\":\"?0\"}},{\"term\":{\"personId2\":\"?1\"}}]}},{\"term\":{\"relationshipType\":\"?2\"}}]}}")
	Page<Interpersonal> findByPersonId1OrPersonId2AndRelationshipType(String personId1, String personId2, String relationshipType, Pageable pageable);

	Page<Interpersonal> findByActiveIsTrueAndPersonId1AndRelationshipTypeIsIn(String personId1, Collection<String> relationshipType, Pageable pageable);

	Page<Interpersonal> findByPersonId2AndRelationshipType(String personId2, String relationshipType, Pageable pageable);

	Page<Interpersonal> findByActiveIsTrueAndPersonId1AndRelationshipTypeAndPersonId2(String personId1, String relationshipType, String personId2, boolean active, Pageable pageable);
	
	Optional<Interpersonal> findByActiveIsTrueAndPersonId1AndRelationshipTypeAndPersonId2(String personId1, String relationshipType, String personId2);
	
	default public <S extends Interpersonal> S saveWithTimestamp(S entity) {
		entity.setChangedTime(DateTimeUtils.now());
		return save(entity);
	}
	
	default public <S extends Interpersonal> Iterable<S> saveAllWithTimestamp(Iterable<S> entities) {
		OffsetDateTime now = DateTimeUtils.now();
		entities.forEach(e->e.setChangedTime(now));
		return saveAll(entities);
	}


}
