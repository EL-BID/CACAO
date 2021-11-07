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
package org.idb.cacao.validator.repositories;

import java.util.Optional;

import org.idb.cacao.api.DocumentUploaded;
import org.idb.cacao.api.utils.DateTimeUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * DAO for DocumentUploaded objects (history of all uploads from each user)
 * 
 * @author Gustavo Figueiredo
 *
 */
@Repository
public interface DocumentUploadedRepository extends ElasticsearchRepository<DocumentUploaded, String> {
	
	Page<DocumentUploaded> findByTemplateName(String templateName, Pageable pageable);
	
	Page<DocumentUploaded> findByUser(String user, Pageable pageable);

	Page<DocumentUploaded> findByUserOrderByTimestampDesc(String user, Pageable pageable);

	Page<DocumentUploaded> findByFileId(String fileId, Pageable pageable);
	
	Page<DocumentUploaded> findByFilename(String filename, Pageable pageable);
	
	Optional<DocumentUploaded> findById(String documentId);	
	
	default public <S extends DocumentUploaded> S saveWithTimestamp(S entity) {
		entity.setChangedTime(DateTimeUtils.now());
		return save(entity);
	}

}
