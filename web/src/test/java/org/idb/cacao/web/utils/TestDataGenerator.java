/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.utils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import org.idb.cacao.api.Taxpayer;
import org.idb.cacao.api.utils.RandomDataGenerator;
import org.idb.cacao.web.dto.UserDto;
import org.idb.cacao.web.entities.UserProfile;
import org.idb.cacao.web.utils.generators.SampleCompanySizes;
import org.idb.cacao.web.utils.generators.SampleCounty;
import org.idb.cacao.web.utils.generators.SampleEconomicSectors;
import org.idb.cacao.web.utils.generators.SampleLegalEntities;
import org.idb.cacao.web.utils.generators.SampleTaxRegimes;

import com.google.common.hash.Hashing;

public class TestDataGenerator {
	public static Taxpayer generateTaxpayer(long seed, int numDigits) {
		RandomDataGenerator randomDataGenerator = new RandomDataGenerator(seed);
		String taxpayerId = generateTaxpayerId(randomDataGenerator, numDigits);
		Taxpayer taxpayer = new Taxpayer();
		taxpayer.setTaxPayerId(taxpayerId);
		generateTaxpayer(taxpayer, randomDataGenerator);
		return taxpayer;
	}
	
	public static String generateTaxpayerId(RandomDataGenerator generator, int numDigits) {
		return generator.nextRandomNumberFixedLength(numDigits).toString();
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
	
	public static UserDto generateUser(long seed, String domain, UserProfile profile, Consumer<UserDto> function) {
		RandomDataGenerator randomDataGenerator = new RandomDataGenerator(seed);
		String taxpayerId = generateTaxpayerId(randomDataGenerator, 11);
		String name = randomDataGenerator.nextPersonName();
		UserDto user = new UserDto();
		user.setName(name);
		user.setLogin(name.toLowerCase().replaceAll("\\s", ".") + "@" + domain);
		user.setTaxpayerId(taxpayerId);
		user.setProfile(profile);
		if (function!=null) {
			function.accept(user);
		}
		return user;
	}
	
	public static long generateSeed(String seedWord) {
		return ByteBuffer.wrap(Hashing.sha256().hashString(seedWord, StandardCharsets.UTF_8).asBytes()).asLongBuffer().get();
	}
	
}
