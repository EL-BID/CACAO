/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.repositories;

import org.idb.cacao.web.Synchronizable;
import org.idb.cacao.web.entities.ConfigSync;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * DAO for ConfigSync objects (actually there will be only one for an application)
 * 
 * @author Gustavo Figueiredo
 *
 */
@Repository
@Synchronizable(timestamp="changedTime",id="id")
public interface ConfigSyncRepository extends ElasticsearchRepository<ConfigSync, Long> {

}
