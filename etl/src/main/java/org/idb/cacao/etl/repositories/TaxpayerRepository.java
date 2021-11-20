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
