/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.etl.repositories;

import java.util.List;
import java.util.Optional;

import org.idb.cacao.api.ETLContext;
import org.idb.cacao.api.templates.DomainTable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for domain tables
 * 
 * @author Gustavo Figueiredo
 *
 */
@Repository
public interface DomainTableRepository extends ElasticsearchRepository<DomainTable, String>, ETLContext.DomainTableRepository {

	@Query("{\"match\": {\"name.keyword\": {\"query\": \"?0\"}}}")
	public List<DomainTable> findByName(String name);
	
	@Query("{\"bool\":{\"must\":[{\"match\": {\"name.keyword\": {\"query\": \"?0\"}}},"
			+ "{\"match\": {\"version.keyword\": {\"query\": \"?1\"}}}]}}")
	public Optional<DomainTable> findByNameAndVersion(String name, String version);
	
}
