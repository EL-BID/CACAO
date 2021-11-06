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

import java.util.List;

import org.idb.cacao.api.DocumentSituationHistory;
import org.idb.cacao.web.Synchronizable;
import org.idb.cacao.web.utils.DateTimeUtils;
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
	
	List<DocumentSituationHistory> findByDocumentId(String documentId);
	
	default public <S extends DocumentSituationHistory> S saveWithTimestamp(S entity) {
		entity.setChangedTime(DateTimeUtils.now());
		return save(entity);
	}

}
