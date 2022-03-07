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
package org.idb.cacao.api.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DomainEntry;
import org.idb.cacao.api.templates.DomainTable;
import org.idb.cacao.api.templates.FieldType;
import org.springframework.data.util.Pair;

/**
 * Object used for generating random data to be used according to template field types
 * 
 * @author Gustavo Figueiredo
 *
 */
public class RandomDataGenerator {
	
	public static final int DEFAULT_INT_UPPER_BOUND = 100;

	public static final double DEFAULT_NUMERIC_LOGNORMAL_MED = 6.0;

	public static final double DEFAULT_NUMERIC_LOGNORMAL_VAR = 20.0;

	private final Random random;
	
	private int intUpperBound = DEFAULT_INT_UPPER_BOUND;
	
	private double numericLognormalMed = DEFAULT_NUMERIC_LOGNORMAL_MED;
	
	private double numericLognormalVar = DEFAULT_NUMERIC_LOGNORMAL_VAR;
	
	private int yearLowerBound;
	
	private int yearUpperBound;
	
	private ZoneOffset zoneOffset;
	
	private DomainTableRepository domainTableRepository;
	
	private Map<Pair<String,String>,String[]> domainTableValues;
	
	private static final String FEMALE_NAMES_FILE = "/names/dist.female.first.txt";
	private static final String MALE_NAMES_FILE = "/names/dist.male.first.txt";
	private static final String SURNAMES_FILE = "/names/dist.all.last.txt";
	
	private static TreeMap<Float, String> surnames;

	private static TreeMap<Float, String> females;

	private static TreeMap<Float, String> males;
	
	/**
	 * If the chosen year is different from this, will reseed the random number generator according to
	 * the chosen year. Doing this we may generate different data for different years given the same seed.
	 */
	public static final int BASE_YEAR_FOR_RESEED = 2021; // any arbitrary year would do

	
	/**
	 * This generic interface provides means for returning a DomainTable given its name and version
	 * 
	 * @author Gustavo Figueiredo
	 *
	 */
	@FunctionalInterface
	public static interface DomainTableRepository {
		
		public Optional<DomainTable> findByNameAndVersion(String name, String version);
		
	}

	public RandomDataGenerator() {
		this(newRandom());
	}

	public RandomDataGenerator(long seed) {
		this(newRandom(seed));
	}
	
	public RandomDataGenerator(Random random) {
		this.random = random;
		int currentYear = Year.now().getValue();
		this.yearLowerBound = currentYear - 1;
		this.yearUpperBound = currentYear - 1;
		
		this.zoneOffset = ZoneId.systemDefault().getRules().getOffset(Instant.now());
		this.domainTableValues = new ConcurrentHashMap<>();
	}
	
	public Random getRandomGenerator() {
		return random;
	}
	
	public int getIntUpperBound() {
		return intUpperBound;
	}

	public void setIntUpperBound(int intUpperBound) {
		this.intUpperBound = intUpperBound;
	}

	public double getNumericLognormalMed() {
		return numericLognormalMed;
	}

	public void setNumericLognormalMed(double numericLognormalMed) {
		this.numericLognormalMed = numericLognormalMed;
	}

	public double getNumericLognormalVar() {
		return numericLognormalVar;
	}

	public void setNumericLognormalVar(double numericLognormalVar) {
		this.numericLognormalVar = numericLognormalVar;
	}

	public int getYearLowerBound() {
		return yearLowerBound;
	}

	public void setYearLowerBound(int yearLowerBound) {
		this.yearLowerBound = yearLowerBound;
	}

	public int getYearUpperBound() {
		return yearUpperBound;
	}

	public void setYearUpperBound(int yearUpperBound) {
		this.yearUpperBound = yearUpperBound;
	}
	
	public DomainTableRepository getDomainTableRepository() {
		return domainTableRepository;
	}

	public void setDomainTableRepository(DomainTableRepository domainTableRepository) {
		this.domainTableRepository = domainTableRepository;
	}

	public Object nextRandom(DocumentField field) {
		return nextRandom(field.getFieldType(), field.getDomainTableName(), field.getDomainTableVersion());
	}

	public Object nextRandom(FieldType type, String domainTableName, String domainTableVersion) {
		if (type==null)
			return null;
		switch (type) {
		case CHARACTER:
			return nextRandomString();
		case INTEGER:
			return nextRandomInteger();
		case DECIMAL:
			return nextRandomDecimal();
		case BOOLEAN:
			return nextRandomBoolean();
		case TIMESTAMP:
			return nextRandomTimestamp();
		case DATE:
			return nextRandomDate();
		case MONTH:
			return nextRandomMonth();
		case DOMAIN:
			return nextRandomDomain(domainTableName, domainTableVersion);
		default:
			return null; // unsupported field type won't be generated
		}

	}
	
	public String nextRandomString() {
		byte[] bytes = new byte[5];
		random.nextBytes(bytes);
		return UUID.nameUUIDFromBytes(bytes).toString();
	}
	
	public Integer nextRandomInteger() {
		return random.nextInt(intUpperBound);
	}

	public Number nextRandomNumberFixedLength(int length) {
		char[] digits = new char[length];
		for (int i=0; i<digits.length; i++) {
			if (i==0) {
				digits[i] = (char) ( '1' + random.nextInt(9) );				
			}
			else {
				digits[i] = (char) ( '0' + random.nextInt(10) );
			}
		}
		if (length<5)
			return new Short(new String(digits));
		if (length<10)
			return new Integer(new String(digits));
		if (length<19)
			return new Long(new String(digits));
		return new BigInteger(new String(digits));
	}

	public Number nextRandomDecimal() {
		return nextRandomLogNormal(numericLognormalMed, numericLognormalVar);
	}
	
	public Number nextRandomLogNormal(double logNormalMed, double logNormalVar) {
		// Approaches a normal distribution (roughly)
		double r = nextRandomGauss();
		// Turn into a lognormal distribution (more closely related to monetary values)
		double v = Math.exp(logNormalMed + r*logNormalVar);
		v = Math.floor(v*100.0)/100.0; // keeps only 2 decimal places
		return v;		
	}

	public double nextRandomGauss() {
		// Approaches a normal distribution (roughly)
		double r = random.nextDouble()*2.0 - 1.0;	// [-1.0 - 1.0]
		r *= random.nextDouble()*2.0 - 1.0;	// [-1.0 - 1.0]
		r *= random.nextDouble()*2.0 - 1.0;	// [-1.0 - 1.0]
		r *= random.nextDouble()*2.0 - 1.0;	// [-1.0 - 1.0]
		r *= random.nextDouble()*2.0 - 1.0;	// [-1.0 - 1.0]
		return r;
	}
	
	public Boolean nextRandomBoolean() {
		return random.nextBoolean();
	}
	
	public int nextRandomYear() {
		if (yearLowerBound<yearUpperBound) 
			return random.nextInt(yearUpperBound-yearLowerBound+1) + yearLowerBound;
		else
			return yearLowerBound;
	}
	
	public OffsetDateTime nextRandomTimestamp() {
		int year = nextRandomYear();
		int month = random.nextInt(12) + 1;
		int dayOfMonth = random.nextInt(new GregorianCalendar(year, month-Calendar.JANUARY, 1).getActualMaximum(Calendar.DAY_OF_MONTH))+1;
		int hour = random.nextInt(24);
		int minute = random.nextInt(60);
		int second = random.nextInt(60);
		int nanoOfSecond = 0;
		return OffsetDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond, zoneOffset);
	}
	
	public LocalDate nextRandomDate() {
		int year = nextRandomYear();
		int month = random.nextInt(12) + 1;
		int dayOfMonth = random.nextInt(new GregorianCalendar(year, month-Calendar.JANUARY, 1).getActualMaximum(Calendar.DAY_OF_MONTH))+1;
		return LocalDate.of(year, month, dayOfMonth);
	}
	
	public String nextRandomMonth() {
		int year = nextRandomYear();
		int month = random.nextInt(12) + 1;
		String monthName = Month.of(month).getDisplayName(TextStyle.SHORT, Locale.getDefault());
		return String.format("%s/%04d", monthName, year);
	}
	
	public String nextRandomDomain(String domainTableName, String domainTableVersion) {
		
		String[] domainValues =
		domainTableValues.computeIfAbsent(Pair.of(domainTableName,domainTableVersion), 
			domainTableId->{
				
				if (domainTableRepository==null)
					return new String[0];

				try {
					Optional<DomainTable> ref_domain_table = domainTableRepository.findByNameAndVersion(domainTableName, domainTableVersion);
					if (!ref_domain_table.isPresent())
						return new String[0];
					
					return ref_domain_table.get()
					.getEntries().stream()
					.map(DomainEntry::getKey)
					.distinct()
					.toArray(String[]::new);
				}
				catch (Exception ex) {
					return new String[0];
				}
			});
		
		if (domainValues==null || domainValues.length==0)
			return null;
		
		return domainValues[random.nextInt(domainValues.length)];
		
	}
	
	/**
	 * Returns a random person name using some common first names and surnames.
	 */
	public String nextPersonName() {
		assertPersonNamesMaps();
        String first_name = pickName(random.nextBoolean() ? females : males);
        String surname = pickName(surnames);
        return String.join(" ", first_name, surname);
	}
	
	/**
	 * Load the names into memory as needed. Do it only once per JVM. Thread-safe.
	 */
	private static void assertPersonNamesMaps() {
		if (surnames!=null)
			return;
		synchronized (RandomDataGenerator.class) {
			if (surnames!=null)
				return;
			TreeMap<Float, String> surnames, females, males;
		    try {
		        surnames = loadNames(SURNAMES_FILE);
		    }
		    catch (IOException e) {
		    	surnames = new TreeMap<>();
		    }
		    try {
		        females = loadNames(FEMALE_NAMES_FILE);
		    }
		    catch (IOException e) {
		    	females = new TreeMap<>();
		    }
		    try {
		        males = loadNames(MALE_NAMES_FILE);
		    }
		    catch (IOException e) {
		    	males = new TreeMap<>();
		    }
		    RandomDataGenerator.males = males;
		    RandomDataGenerator.females = females;
		    RandomDataGenerator.surnames = surnames;
		}
	}

	/**
	 * Loads names from internal text file. The keys are provided by the name's cumulative frequency.
	 * This code was copied from org.ajbrown.namemachine.NameGenerator source code (Apache License 2.0) 
	 */
	private static TreeMap<Float, String> loadNames(final String file) throws IOException {
	    TreeMap<Float, String> names = new TreeMap<>();
	    InputStream is = RandomDataGenerator.class.getResourceAsStream(file);

	    try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
	    	String line = reader.readLine();
	    	while (line != null) {
	    		String[] fields = line.split("\\s+");
	    		names.put(Float.parseFloat(fields[2]), fields[0]);
	    		line = reader.readLine();
	    	}
	    }

	    return names;
	}

	/**
	 * Pick a name from the specified TreeMap based on a random number.
	 * This code was copied from org.ajbrown.namemachine.NameGenerator source code (Apache License 2.0) 
	 */
	private String pickName(final TreeMap<Float, String> map) {
		assert !map.isEmpty();

	    Float key = null;
	    while (key == null) {
	      try {
	        key = map.ceilingKey(random.nextFloat() * 100);
	      }
	      catch (NullPointerException ignored) {
	        //Do nothing.
	      }
	    }

	    return map.get(key);
	}
	
	/**
	 * Re-seed the Random data generator according to the current random state
	 */
	public void reseed() {
		random.setSeed(random.nextLong());
	}
	
	/**
	 * Re-seed the Random data generator according to the year chosen and the current random state. 
	 * Different years with the same initial seed provided at constructor will result in different data.
	 */
	public void reseedBasedOnYear(int year) {
		if (year!=BASE_YEAR_FOR_RESEED) {
			int number_of_reseed = Math.abs(year - BASE_YEAR_FOR_RESEED);
			long signal_of_reseed = (year<BASE_YEAR_FOR_RESEED) ? -1 : +1;
			for (int i=0; i<number_of_reseed; i++) {
				random.setSeed(signal_of_reseed*random.nextLong());
			}
		}
	}

	/**
	 * Random generator for the purpose of data generation
	 */
	public static Random newRandom() {
		SecureRandom rnd;
		try {
			rnd = SecureRandom.getInstance("SHA1PRNG", "SUN");
		} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
			rnd = new SecureRandom();
		}
		return rnd;
	}
	
	/**
	 * Random generator for the purpose of data generation
	 */
	public static Random newRandom(long seed) {
		Random rnd = newRandom(seed);
		rnd.setSeed(seed);
		return rnd;
	}

}
