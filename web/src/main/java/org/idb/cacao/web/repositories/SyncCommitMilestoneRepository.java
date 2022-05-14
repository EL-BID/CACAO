/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.repositories;

import java.util.Optional;

import org.idb.cacao.web.entities.SyncCommitMilestone;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * DAO for SyncCommitMilestone objects (one for each milestone)
 * 
 * @author Gustavo Figueiredo
 *
 */
@Repository
public interface SyncCommitMilestoneRepository extends ElasticsearchRepository<SyncCommitMilestone, Long> {

	public Optional<SyncCommitMilestone> findByEndPoint(String endPoint);

}
