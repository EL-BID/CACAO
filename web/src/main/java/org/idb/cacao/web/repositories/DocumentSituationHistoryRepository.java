/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.repositories;

import java.util.List;

import org.idb.cacao.api.DocumentSituationHistory;
import org.idb.cacao.api.utils.DateTimeUtils;
import org.idb.cacao.web.Synchronizable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * DAO for DocumentSituationHistory objects (history of all situation for a given document uploaded)
 * 
 * @author Rivelino Patrício
 * 
 * @since 06/11/2021
 *
 */
@Repository
@Synchronizable(timestamp="changedTime",id="id")
public interface DocumentSituationHistoryRepository extends ElasticsearchRepository<DocumentSituationHistory, String> {
	
	List<DocumentSituationHistory> findByDocumentIdOrderByChangedTimeDesc(String documentId);
	
	default public <S extends DocumentSituationHistory> S saveWithTimestamp(S entity) {
		entity.setChangedTime(DateTimeUtils.now());
		return save(entity);
	}

}
