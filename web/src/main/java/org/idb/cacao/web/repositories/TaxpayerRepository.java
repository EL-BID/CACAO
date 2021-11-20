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

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;

import org.idb.cacao.api.Taxpayer;
import org.idb.cacao.api.utils.DateTimeUtils;
import org.idb.cacao.web.Synchronizable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * DAO for Taxpayer objects (taxpayer registration)
 * 
 * @author Gustavo Figueiredo
 *
 */
@Repository
@Synchronizable(timestamp="changedTime",id="id")
public interface TaxpayerRepository extends ElasticsearchRepository<Taxpayer, String> {

	Page<Taxpayer> findById(String id, Pageable pageable);

	Optional<Taxpayer> findByTaxPayerId(String taxPayerId);

	Page<Taxpayer> findByTaxPayerId(String taxPayerId, Pageable pageable);

	Page<Taxpayer> findByTaxPayerIdContaining(String taxPayerId, Pageable pageable);

	Page<Taxpayer> findByTaxPayerIdStartsWith(String taxPayerId, Pageable pageable);

	Page<Taxpayer> findByName(String name, Pageable pageable);
	
	Optional<Taxpayer> findByName(String name);

	Page<Taxpayer> findByTaxPayerIdIn(Set<String> taxPayerId, Pageable pageable);
		
	default public <S extends Taxpayer> S saveWithTimestamp(S entity) {
		entity.setChangedTime(DateTimeUtils.now());
		return save(entity);
	}
	
	default public <S extends Taxpayer> Iterable<S> saveAllWithTimestamp(Iterable<S> entities) {
		OffsetDateTime now = DateTimeUtils.now();
		entities.forEach(e->e.setChangedTime(now));
		return saveAll(entities);
	}

}
