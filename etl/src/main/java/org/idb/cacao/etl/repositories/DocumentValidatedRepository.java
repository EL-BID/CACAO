package org.idb.cacao.etl.repositories;

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
public interface DocumentValidatedRepository extends ElasticsearchRepository<DocumentUploaded, String> {
	
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
