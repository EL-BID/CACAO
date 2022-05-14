/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.repositories;

import java.util.List;
import java.util.Optional;

import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.utils.DateTimeUtils;
import org.idb.cacao.web.Synchronizable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
@Synchronizable(timestamp="changedTime",id="id",uniqueConstraint={"name","version"})
public interface DocumentTemplateRepository extends ElasticsearchRepository<DocumentTemplate, String> {
	
	@Query("{\"match\": {\"name.keyword\": {\"query\": \"?0\"}}}")
	public List<DocumentTemplate> findByName(String name);
	
	public List<DocumentTemplate> findByNameIgnoreCase(String name);

	public Page<DocumentTemplate> findByActive(Boolean active, Pageable pageable);
	
	@Query("{\"bool\":{\"must\":[{\"match\": {\"name.keyword\": {\"query\": \"?0\"}}},"
			+ "{\"match\": {\"version.keyword\": {\"query\": \"?1\"}}}]}}")
	public Optional<DocumentTemplate> findByNameAndVersion(String name, String version);
	
	public Optional<DocumentTemplate> findByNameIgnoreCaseAndVersion(String name, String version);
	
	public void deleteById(String id);

	public default <S extends DocumentTemplate> S saveWithTimestamp(S entity) {
		if (entity.getTemplateCreateTime()==null)
			entity.setTemplateCreateTime(DateTimeUtils.now());
		entity.setChangedTime(DateTimeUtils.now());
		return save(entity);
	}

}
