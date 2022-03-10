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
