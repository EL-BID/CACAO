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
package org.idb.cacao.account.generator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.idb.cacao.account.archetypes.AccountBuiltInDomainTables;
import org.idb.cacao.account.archetypes.ShareholdersArchetype;
import org.idb.cacao.account.elements.ShareType;
import org.idb.cacao.api.templates.CustomDataGenerator;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.utils.RandomDataGenerator;
import org.idb.cacao.api.utils.RandomDataGenerator.DomainTableRepository;

/**
 * Custom implementation of a 'random data generator' for data related to the shareholders archetype.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class ShareholdersGenerator implements CustomDataGenerator {

	public static final int DEFAULT_NUMBER_OF_SHAREHOLDERS = 5;

	private final RandomDataGenerator randomDataGenerator;

	private final long records;

	private int recordsCreated;
	
	private DocumentField taxPayerIdField;
	
	private int num_digits_for_taxpayer_id;
	
	private Number taxpayerId;

	private int year;
	
	private int providedYear;
	
	/**
	 * This is the random generator for other 'documents' related to the same template. Useful for generating
	 * other 'taxpayers id' that might actually be generated in other instances.
	 */
	private Random genSeed;

	public ShareholdersGenerator(DocumentTemplate template, DocumentFormat format, long seed, long records) 
			throws Exception {
		
		this.records = (records<0) ? DEFAULT_NUMBER_OF_SHAREHOLDERS : records;

		this.taxPayerIdField = template.getField(ShareholdersArchetype.FIELDS_NAMES.TaxPayerId.name());
		this.num_digits_for_taxpayer_id = (taxPayerIdField==null) ? 10 : Math.min(20, Math.max(1, Optional.ofNullable(taxPayerIdField.getMaxLength()).orElse(10)));
		this.randomDataGenerator = new RandomDataGenerator(seed);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.CustomDataGenerator#start()
	 */
	@Override
	public void start() {
		recordsCreated = 0;
		
		taxpayerId = randomDataGenerator.nextRandomNumberFixedLength(num_digits_for_taxpayer_id);

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
		genSeed = new Random(overallSeed);
		
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

		record.put(ShareholdersArchetype.FIELDS_NAMES.TaxPayerId.name(), taxpayerId.toString());
		record.put(ShareholdersArchetype.FIELDS_NAMES.TaxYear.name(), year);
		
		// The 'shareholder ID' is created based on the 'overall seed' because we need to create something
		// related to other instances
		Number shareholderId = null;
		if (genSeed!=null) {
			while (shareholderId==null || shareholderId.equals(taxpayerId)) {				
				long doc_seed = genSeed.nextLong();
				RandomDataGenerator doc_random = new RandomDataGenerator(doc_seed);
				shareholderId = doc_random.nextRandomNumberFixedLength(num_digits_for_taxpayer_id);
			}
		}
		else {
			while (shareholderId==null || shareholderId.equals(taxpayerId)) {	
				shareholderId = randomDataGenerator.nextRandomNumberFixedLength(num_digits_for_taxpayer_id);
			}
		}		
		record.put(ShareholdersArchetype.FIELDS_NAMES.ShareholderId.name(), shareholderId.toString());
		
		String shareType = randomDataGenerator.nextRandomDomain(
				AccountBuiltInDomainTables.SHARE_TYPE.getName(), AccountBuiltInDomainTables.SHARE_TYPE.getVersion());
		record.put(ShareholdersArchetype.FIELDS_NAMES.ShareType.name(), shareType);
		if (ShareType.ORDINARY.getKey().equals(shareType)) {
			String shareClass = String.valueOf((char)('A' + randomDataGenerator.getRandomGenerator().nextInt(3)));
			record.put(ShareholdersArchetype.FIELDS_NAMES.ShareClass.name(), shareClass);			
		}
		
		record.put(ShareholdersArchetype.FIELDS_NAMES.ShareQuantity.name(), randomDataGenerator.getRandomGenerator().nextInt(10)*1000+1000);
		record.put(ShareholdersArchetype.FIELDS_NAMES.ShareAmount.name(), roundDecimals(randomDataGenerator.nextRandomDecimal()));
		record.put(ShareholdersArchetype.FIELDS_NAMES.SharePercentage.name(), Math.min(100.0,Math.max(1.0,roundDecimals(randomDataGenerator.nextRandomGauss()*10.0+20.0))));

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
