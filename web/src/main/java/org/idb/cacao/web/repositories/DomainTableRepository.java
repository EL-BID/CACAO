/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.repositories;

import java.util.List;
import java.util.Optional;

import org.idb.cacao.api.templates.DomainTable;
import org.idb.cacao.api.utils.DateTimeUtils;
import org.idb.cacao.web.Synchronizable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
@Synchronizable(timestamp="changedTime",id="id",uniqueConstraint={"name","version"})
public interface DomainTableRepository extends ElasticsearchRepository<DomainTable, String> {

	@Query("{\"match\": {\"name.keyword\": {\"query\": \"?0\"}}}")
	public List<DomainTable> findByName(String name);

	public List<DomainTable> findByNameIgnoreCase(String name);
	
	public List<DomainTable> findByNameIgnoreCaseAndActiveTrue(String name);

	@Query("{\"bool\":{\"must\":[{\"match\": {\"name.keyword\": {\"query\": \"?0\"}}},"
			+ "{\"match\": {\"version.keyword\": {\"query\": \"?1\"}}}]}}")
	public Optional<DomainTable> findByNameAndVersion(String name, String version);
	
	public Optional<DomainTable> findByNameIgnoreCaseAndVersion(String name, String version);

	Page<DomainTable> findByNameStartsWith(String name, Pageable pageable);
	
	Page<DomainTable> findByNameContaining(String name, Pageable pageable);
	
	default public <S extends DomainTable> S saveWithTimestamp(S entity) {
		if (entity.getDomainTableCreateTime()==null)
			entity.setDomainTableCreateTime(DateTimeUtils.now());
		entity.setChangedTime(DateTimeUtils.now());
		return save(entity);
	}
	
}
