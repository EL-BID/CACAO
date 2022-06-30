/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.repositories;

import java.util.Date;
import java.util.stream.Stream;

import org.idb.cacao.web.entities.PasswordResetToken;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * DAO for PasswordResetToken objects (temporary tokens for password renewal)
 * 
 * @author Gustavo Figueiredo
 *
 */
@Repository
public interface PasswordResetTokenRepository extends ElasticsearchRepository<PasswordResetToken, String> {

    PasswordResetToken findByToken(String token);

    Page<PasswordResetToken> findByUserId(String userId, Pageable pageable);

    Stream<PasswordResetToken> findAllByExpiryDateLessThan(Date now);

    void deleteByExpiryDateLessThan(Date now);

}
