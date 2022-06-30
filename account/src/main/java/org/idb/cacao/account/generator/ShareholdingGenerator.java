/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.account.generator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.idb.cacao.account.archetypes.AccountBuiltInDomainTables;
import org.idb.cacao.account.archetypes.ShareholdingArchetype;
import org.idb.cacao.account.elements.ShareType;
import org.idb.cacao.api.errors.GeneralException;
import org.idb.cacao.api.templates.CustomDataGenerator;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.utils.RandomDataGenerator;
import org.idb.cacao.api.utils.RandomDataGenerator.DomainTableRepository;

/**
 * Custom implementation of a 'random data generator' for data related to the shareholding archetype.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class ShareholdingGenerator implements CustomDataGenerator {

	public static final int DEFAULT_NUMBER_OF_SHAREHOLDING = 5;

	private final RandomDataGenerator randomDataGenerator;

	private final long records;

	private int recordsCreated;
	
	private DocumentField taxPayerIdField;
	
	private int numDigitsForTaxpayerId;
	
	private Number taxpayerId;

	private int year;
	
	private int providedYear;
	
	/**
	 * This is the random generator for other 'documents' related to the same template. Useful for generating
	 * other 'taxpayers id' that might actually be generated in other instances.
	 */
	private Random genSeed;

	public ShareholdingGenerator(DocumentTemplate template, DocumentFormat format, long seed, long records) 
			throws GeneralException {
		
		this.records = (records<0) ? DEFAULT_NUMBER_OF_SHAREHOLDING : records;

		this.taxPayerIdField = template.getField(ShareholdingArchetype.FIELDS_NAMES.TaxPayerId.name());
		this.numDigitsForTaxpayerId = (taxPayerIdField==null) ? 10 : Math.min(20, Math.max(1, Optional.ofNullable(taxPayerIdField.getMaxLength()).orElse(10)));
		this.randomDataGenerator = new RandomDataGenerator(seed);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.CustomDataGenerator#start()
	 */
	@Override
	public void start() {
		recordsCreated = 0;
		
		taxpayerId = randomDataGenerator.nextRandomNumberFixedLength(numDigitsForTaxpayerId);

		year = (providedYear==0) ? randomDataGenerator.nextRandomYear() : providedYear;
		randomDataGenerator.reseedBasedOnYear(year);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.CustomDataGenerator#getTaxpayerId()
	 */
	@Override
	public String getTaxpayerId() {
		return (taxpayerId==null) ? null : taxpayerId.toString();
	}

	@Override
	public void setTaxYear(Number year) {
		if (year==null || year.intValue()==0) {
			providedYear = 0;
		}
		else {
			providedYear = year.intValue();
			if (this.year!=0)
				this.year = providedYear;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.CustomDataGenerator#getTaxYear()
	 */
	@Override
	public Number getTaxYear() {
		return (year==0) ? ( (providedYear==0) ? null : providedYear ) : year;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.CustomDataGenerator#setOverallSeed(long, int, int)
	 */
	@Override
	public void setOverallSeed(long overallSeed, int docsTotal, int docIndex) {
		genSeed = newRandom(overallSeed);
		
		// advance forward in 'genSeed' according to 'docIndex' 
		for (int i=0; i<docIndex && i<docsTotal-records; i++) {
			genSeed.nextLong();
		}
	}

	@Override
	public void setDomainTableRepository(DomainTableRepository domainTableRepository) {
		randomDataGenerator.setDomainTableRepository(domainTableRepository);
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.CustomDataGenerator#nextRecord()
	 */
	@Override
	public Map<String, Object> nextRecord() {
		if (recordsCreated>=records) {
			return null;
		}
		
		Map<String, Object> record = new HashMap<>();

		record.put(ShareholdingArchetype.FIELDS_NAMES.TaxPayerId.name(), taxpayerId.toString());
		record.put(ShareholdingArchetype.FIELDS_NAMES.TaxYear.name(), year);
		
		// The 'shareholding ID' is created based on the 'overall seed' because we need to create something
		// related to other instances
		Number shareholdingId = null;
		if (genSeed!=null) {
			while (shareholdingId==null || shareholdingId.equals(taxpayerId)) {				
				long doc_seed = genSeed.nextLong();
				RandomDataGenerator doc_random = new RandomDataGenerator(doc_seed);
				shareholdingId = doc_random.nextRandomNumberFixedLength(numDigitsForTaxpayerId);
			}
		}
		else {
			while (shareholdingId==null || shareholdingId.equals(taxpayerId)) {	
				shareholdingId = randomDataGenerator.nextRandomNumberFixedLength(numDigitsForTaxpayerId);
			}
		}		
		record.put(ShareholdingArchetype.FIELDS_NAMES.ShareholdingId.name(), shareholdingId.toString());
		
		String shareType = randomDataGenerator.nextRandomDomain(
				AccountBuiltInDomainTables.SHARE_TYPE.getName(), AccountBuiltInDomainTables.SHARE_TYPE.getVersion());
		record.put(ShareholdingArchetype.FIELDS_NAMES.ShareType.name(), shareType);
		if (ShareType.ORDINARY.getKey().equals(shareType)) {
			String shareClass = String.valueOf((char)('A' + randomDataGenerator.getRandomGenerator().nextInt(3)));
			record.put(ShareholdingArchetype.FIELDS_NAMES.ShareClass.name(), shareClass);			
		}
		
		record.put(ShareholdingArchetype.FIELDS_NAMES.ShareQuantity.name(), randomDataGenerator.getRandomGenerator().nextInt(10)*1000+1000);
		record.put(ShareholdingArchetype.FIELDS_NAMES.ShareAmount.name(), roundDecimals(randomDataGenerator.nextRandomDecimal()));
		record.put(ShareholdingArchetype.FIELDS_NAMES.SharePercentage.name(), Math.min(100.0,Math.max(1.0,roundDecimals(randomDataGenerator.nextRandomGauss()*10.0+20.0))));
		
		if (randomDataGenerator.getRandomGenerator().nextInt(4)==0) {
			record.put(ShareholdingArchetype.FIELDS_NAMES.EquityMethodResult.name(), roundDecimals(randomDataGenerator.nextRandomDecimal()));
		}

		recordsCreated++;
		
		return record;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		
	}

	/**
	 * Round to two decimals
	 */
	private static double roundDecimals(Number amount) {
		return Math.round(amount.doubleValue() * 100.0) / 100.0; // round to 2 decimals
	}
}
