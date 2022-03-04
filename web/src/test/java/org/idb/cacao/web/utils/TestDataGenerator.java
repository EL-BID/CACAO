package org.idb.cacao.web.utils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.idb.cacao.api.Taxpayer;
import org.idb.cacao.api.utils.RandomDataGenerator;
import org.idb.cacao.web.utils.generators.SampleCompanySizes;
import org.idb.cacao.web.utils.generators.SampleCounty;
import org.idb.cacao.web.utils.generators.SampleEconomicSectors;
import org.idb.cacao.web.utils.generators.SampleLegalEntities;
import org.idb.cacao.web.utils.generators.SampleTaxRegimes;

import com.google.common.hash.Hashing;

public class TestDataGenerator {
	public static Taxpayer generateTaxpayer(long seed, int numDigits) {
		RandomDataGenerator randomDataGenerator = new RandomDataGenerator(seed);
		String taxpayerId = randomDataGenerator.nextRandomNumberFixedLength(numDigits).toString();
		Taxpayer taxpayer = new Taxpayer();
		taxpayer.setTaxPayerId(taxpayerId);
		generateTaxpayer(taxpayer, randomDataGenerator);
		return taxpayer;
	}
		
	public static void generateTaxpayer(Taxpayer taxpayer, RandomDataGenerator randomDataGenerator) {
		String name = randomDataGenerator.nextPersonName();
		taxpayer.setName(name) ;
		
		// suppose Qualifier1 refers to Economic Sector
		taxpayer.setQualifier1(SampleEconomicSectors.sample(randomDataGenerator.getRandomGenerator()).getName());

		// suppose Qualifier2 refers to Legal Entity
		taxpayer.setQualifier2(SampleLegalEntities.sample(randomDataGenerator.getRandomGenerator()).getName());

		// suppose Qualifier3 refers to County
		taxpayer.setQualifier3(SampleCounty.sample(randomDataGenerator.getRandomGenerator()).getName());

		// suppose Qualifier4 refers to Size of Company
		taxpayer.setQualifier4(SampleCompanySizes.sample(randomDataGenerator.getRandomGenerator()).getName());

		// suppose Qualifier5 refers to Tax Regime
		taxpayer.setQualifier5(SampleTaxRegimes.sample(randomDataGenerator.getRandomGenerator()).getName());
	}
	
	public static long generateSeed(String seedWord) {
		return ByteBuffer.wrap(Hashing.sha256().hashString(seedWord, StandardCharsets.UTF_8).asBytes()).asLongBuffer().get();
	}
	
}
