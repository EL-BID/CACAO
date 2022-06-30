/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.repositories;

import java.time.OffsetDateTime;
import java.util.List;
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
	
	List<Taxpayer> findByTaxPayerIdIn(Set<String> taxPayerIds);

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
