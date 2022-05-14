/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.etl.repositories;

import java.util.List;

import org.idb.cacao.api.DocumentValidationErrorMessage;
import org.idb.cacao.api.utils.DateTimeUtils;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * DAO for DocumentValidationErrorMessage objects (error messages for a given document uploaded)
 * 
 * @author Rivelino Patrício
 * 
 * @since 15/11/2021
 *
 */
@Repository
public interface DocumentValidationErrorMessageRepository extends ElasticsearchRepository<DocumentValidationErrorMessage, String> {
	
	List<DocumentValidationErrorMessage> findByDocumentId(String documentId);
	
	default public <S extends DocumentValidationErrorMessage> S saveWithTimestamp(S entity) {
		entity.setChangedTime(DateTimeUtils.now());
		return save(entity);
	}

}
