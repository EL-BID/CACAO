/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.etl.repositories;

import java.util.Map;
import java.util.Optional;

import org.idb.cacao.api.ETLContext;
import org.idb.cacao.api.Taxpayer;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * DAO for Taxpayer objects (taxpayer registration) for usage in ETL denormalization
 * 
 * @author Gustavo Figueiredo
 *
 */
@Repository
public interface TaxpayerRepository extends ElasticsearchRepository<Taxpayer, String>, ETLContext.TaxpayerRepository {

	Optional<Taxpayer> findByTaxPayerId(String taxPayerId);
	
	/**
	 * Given the taxpayer Id, should return additional data to be included in denormalized views
	 */
	default public Optional<Map<String,Object>> getTaxPayerData(String taxPayerId) {
		return findByTaxPayerId(taxPayerId).map(ETLContext::getTaxpayerBasicInformation);
	}

}
