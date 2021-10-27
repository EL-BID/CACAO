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

import java.util.Collection;
import java.util.Date;

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

	Page<Interpersonal> findByPersonId2(String personId2, Pageable pageable);
	
	Page<Interpersonal> findByPersonId1AndPersonId2(String personId1, String personId2, Pageable pageable);

	Page<Interpersonal> findByPersonId1OrPersonId2(String personId1, String personId2, Pageable pageable);

	Page<Interpersonal> findByPersonId1AndRelationshipType(String personId1, String relationshipType, Pageable pageable);

	@Query("{\"bool\":{\"must\":[{\"bool\":{\"should\":[{\"term\":{\"personId1\":\"?0\"}},{\"term\":{\"personId2\":\"?1\"}}]}},{\"term\":{\"relationshipType\":\"?2\"}}]}}")
	Page<Interpersonal> findByPersonId1OrPersonId2AndRelationshipType(String personId1, String personId2, String relationshipType, Pageable pageable);

	Page<Interpersonal> findByRemovedIsFalseAndPersonId1AndRelationshipTypeIsIn(String personId1, Collection<String> relationshipType, Pageable pageable);

	Page<Interpersonal> findByPersonId2AndRelationshipType(String personId2, String relationshipType, Pageable pageable);

	Page<Interpersonal> findByPersonId1AndRelationshipTypeAndPersonId2(String personId1, String relationshipType, String personId2, Pageable pageable);
	
	default public <S extends Interpersonal> S saveWithTimestamp(S entity) {
		entity.setChangedTime(new Date());
		return save(entity);
	}

}
