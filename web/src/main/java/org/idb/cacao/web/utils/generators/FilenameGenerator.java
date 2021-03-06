/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.utils.generators;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import org.idb.cacao.api.templates.FieldMapping;
import org.idb.cacao.api.utils.RandomDataGenerator;

import com.mifmif.common.regex.Generex;

public class FilenameGenerator {

	private final RandomDataGenerator randomGenerator;

	private final Map<String, FieldMapping> regexForFilename;
	
	private long seed;
	
	private String fixedTaxpayerId;
	
	private Number fixedYear;

	public FilenameGenerator(RandomDataGenerator randomGenerator) {
		this.randomGenerator = randomGenerator;
		this.seed = randomGenerator.getRandomGenerator().nextLong();
		this.regexForFilename = new HashMap<>();
	}
	
	public void addFilenameExpression(String filenameExpression, FieldMapping fieldMapping) {
		FieldMapping fm = Optional.ofNullable(fieldMapping).orElse(FieldMapping.ANY);
		regexForFilename.put(filenameExpression, fm);
	}
	
	public String getFixedTaxpayerId() {
		return fixedTaxpayerId;
	}

	public void setFixedTaxpayerId(String fixedTaxpayerId) {
		this.fixedTaxpayerId = fixedTaxpayerId;
	}

	public Number getFixedYear() {
		return fixedYear;
	}

	public void setFixedYear(Number fixedYear) {
		this.fixedYear = fixedYear;
	}

	public boolean isEmpty() {
		return regexForFilename.isEmpty();
	}
	
	public String generateFileName() {
		String generatedOriginalFileName;
		if (regexForFilename.size()==1) {
			String regex = regexForFilename.keySet().iterator().next();
			FieldMapping fm = regexForFilename.values().iterator().next();
			RegexGen g = new RegexGen(regex, fm);
			generatedOriginalFileName = g.nextRandom();
		}
		else {
			// If we have to generate a filename that resolves to multiple regex'es, we need to try to match
			// all of them.
			List<RegexGen> regexAnchoredAtStart = new LinkedList<>();
			List<RegexGen> regexAnchoredAtEnd = new LinkedList<>();
			List<RegexGen> regexMiddle = new LinkedList<>();
			for (Map.Entry<String,FieldMapping> entry: regexForFilename.entrySet()) {
				String regex = entry.getKey();
				FieldMapping fm = entry.getValue();
				if (regex.startsWith("^") || regex.startsWith("(^")) {
					regexAnchoredAtStart.add(new RegexGen(regex.replace("^", ""),fm));
				}
				else if (regex.endsWith("$") || regex.endsWith("$)")) {
					regexAnchoredAtEnd.add(new RegexGen(regex.replace("$", ""),fm));
				}
				else {
					regexMiddle.add(new RegexGen(regex,fm));
				}
			}
			Collections.sort(regexAnchoredAtStart);
			Collections.sort(regexAnchoredAtEnd);
			Collections.sort(regexMiddle);
			if (!regexAnchoredAtStart.isEmpty()) {
				// if we have multiple choices of pattern anchored at start of string, let's use the one with the largest regex expression
				generatedOriginalFileName = regexAnchoredAtStart.get(0).nextRandom();
			}
			else {
				generatedOriginalFileName = "";
			}
			for (RegexGen g: regexMiddle) {
				if (g.matches(generatedOriginalFileName))
					continue; // if this pattern is already satisfied by the provided string, keep as is
				generatedOriginalFileName += g.nextRandom();
			}
			if (!regexAnchoredAtEnd.isEmpty()) {
				// if we have multiple choices of pattern anchored at end of string, let's use the one with the largest regex expression
				RegexGen g = regexAnchoredAtEnd.get(0);
				if (!g.matches(generatedOriginalFileName)) {
					generatedOriginalFileName += g.nextRandom();
				}
			}
		}
		return generatedOriginalFileName;
	}
	
	private class RegexGen implements Comparable<RegexGen> {
		private final String regex;
		private Pattern pattern;
		private Generex generex;
		private FieldMapping fm;
		RegexGen(String r, FieldMapping fm) {
			this.regex = r;
			this.fm = fm;
			try {
				this.pattern = Pattern.compile(r);
			}
			catch (Exception ex) {
				this.pattern = Pattern.compile(Pattern.quote(r));
			}
		}
		public String toString() {
			return regex;
		}
		public int compareTo(RegexGen o) {
			// The largest regex comes first
			if (regex.length()>o.regex.length())
				return -1;
			if (regex.length()<o.regex.length())
				return 1;
			return 0;
		}
		public String nextRandom() {
			if (generex==null) {
				generex = new Generex(pattern.pattern());
				generex.setSeed(seed);
			}
			String r = generex.random();
			if (FieldMapping.TAX_YEAR.equals(fm)) {
				int year = (fixedYear!=null) ? fixedYear.intValue() : randomGenerator.nextRandomYear();
				r = r.replaceFirst("\\d{4}", String.valueOf(year));
			}
			if (FieldMapping.TAXPAYER_ID.equals(fm) && fixedTaxpayerId!=null && fixedTaxpayerId.trim().length()>0) {
				r = r.replaceFirst("\\d+", fixedTaxpayerId);
			}
			return r;
		}
		public boolean matches(String txt) {
			return pattern.matcher(txt).find();
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Objects.hash(regex);
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			RegexGen other = (RegexGen) obj;
			if (!getEnclosingInstance().equals(other.getEnclosingInstance()))
				return false;
			return Objects.equals(regex, other.regex);
		}
		
		private FilenameGenerator getEnclosingInstance() {
			return FilenameGenerator.this;
		}
		
	}

}
