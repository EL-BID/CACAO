/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.api.templates;

import java.io.Closeable;
import java.util.Map;
import java.util.Random;

import org.idb.cacao.api.utils.RandomDataGenerator;
import org.idb.cacao.api.utils.RandomDataGenerator.DomainTableRepository;

/**
 * This is a generic interface for providing a custom implementation for generating random data to be used 
 * as 'samples' of a given template and file format.
 * 
 * @author Gustavo Figueiredo
 *
 */
public interface CustomDataGenerator extends Closeable {
	
	/**
	 * Returns some internally generated 'filename' to be used as 'original filename'. Returns NULL if the filename
	 * may be any arbitrary name.
	 */
	default public String getFileName() {
		return null;
	}
	
	/**
	 * Returns some internally generated 'taxpayer ID' to be used for identification of the generated data. Returns NULL if
	 * the taxpayer ID may be any arbitrary number
	 */
	default public String getTaxpayerId() {
		return null;
	}
	
	/**
	 * Define the fixed 'tax year' to be used with data generated. If NULL, the year may be any arbitrary number. Should be
	 * defined before calling the 'start' method. If the year is not defined, the year may be any arbitrary number.
	 */
	default public void setTaxYear(Number year) { }

	/**
	 * Returns some internally generated 'tax year' to be used for identification of the generated data. Returns NULL if
	 * the year may be any arbitrary number
	 */
	default public Number getTaxYear() {
		return null;
	}

	/**
	 * Object used for retrieving registered domain tables given a domain table name and version
	 */
	default public void setDomainTableRepository(DomainTableRepository domainTableRepository) {	}

	/**
	 * Method called at the start of the procedure. Should initialize internal state.
	 */
	default public void start() { }
	
	/**
	 * Method called for each generated record. NULL means there is no more record to generate.
	 */
	public Map<String,Object> nextRecord();
	
	/**
	 * The 'overall seed' is the 'initial seed' used for generating several documents of the same template. It's different from the 'seed'
	 * informed during construction of this instance (the 'per-document' seed). The implementation may find it useful for generating
	 * additional data compliant to other generated documents.
	 * @param overallSeed The 'overall seed' for the entire process (not just this particular document)
	 * @param docsTotal The total number of documents to generate (including this particular document)
	 * @param docIndex The number of documents already generated in this process
	 */
	default public void setOverallSeed(long overallSeed, int docsTotal, int docIndex) { }

	/**
	 * Random generator for the purpose of data generation
	 */
	default public Random newRandom(long seed) {
		return RandomDataGenerator.newRandom(seed);
	}
}
