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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.idb.cacao.account.archetypes.AccountBuiltInDomainTables;
import org.idb.cacao.account.archetypes.ChartOfAccountsArchetype;
import org.idb.cacao.account.archetypes.GeneralLedgerArchetype;
import org.idb.cacao.account.archetypes.OpeningBalanceArchetype;
import org.idb.cacao.account.elements.AccountStandard;
import org.idb.cacao.account.etl.PartialEntry;
import org.idb.cacao.api.templates.CustomDataGenerator;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.utils.RandomDataGenerator;

/**
 * Custom implementation of a 'random data generator' for data related to the accounting archetypes.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class AccountDataGenerator implements CustomDataGenerator {
	
	public static final int FIXED_NUMBER_OF_ACCOUNTS = 100;
	
	public static final int DEFAULT_NUMBER_OF_LEDGER_ENTRIES = 10_000;
	
	/**
	 * Odds to consider 'start of business'. Bigger value means smaller odds.<BR>
	 * '10' means '1 out of 10' will be considered 'start of business'
	 */
	public static final int ODDS_START_OF_BUSINESS = 10;
	
	private final long records;
	
	private final boolean chartOfAccounts;
	
	private final boolean openingBalance;
	
	private final boolean generalLedger;
	
	private final RandomDataGenerator randomDataGenerator;
	
	/**
	 * Initial/current balance for each account code. Positive value means 'debit' and negative value means 'credit'
	 */
	private final Map<String, Double> accountBalance;
	
	private final Map<String, String> accountDescriptions;

	private AccountStandard standard;

	private int recordsCreated;
	
	private DocumentField taxPayerIdField;
	
	private Number taxpayerId;
	
	private int year;
	
	private int[] transactionsPerDay;
	
	private int currentDay;
	
	private LocalDate[] days;
	
	private List<PartialEntry> nextCredits, nextDebits;
	
	private long transactionId;
		
	public AccountDataGenerator(DocumentTemplate template, DocumentFormat format, long seed, long records) 
			throws Exception {
		
		this.chartOfAccounts = ChartOfAccountsArchetype.NAME.equalsIgnoreCase(template.getArchetype());
		this.openingBalance = OpeningBalanceArchetype.NAME.equalsIgnoreCase(template.getArchetype());
		this.generalLedger = GeneralLedgerArchetype.NAME.equalsIgnoreCase(template.getArchetype());

		this.records = (generalLedger && records<0) ? DEFAULT_NUMBER_OF_LEDGER_ENTRIES : records;
		
		this.randomDataGenerator = new RandomDataGenerator(seed);
		
		this.accountDescriptions = Arrays.stream(SampleChartOfAccounts.values()).collect(Collectors.toMap(
				/*keyMapper*/SampleChartOfAccounts::getAccountCode, 
				/*valueMapper*/SampleChartOfAccounts::getAccountDescription, 
				/*mergeFunction*/(a,b)->a, 
				/*mapSupplier*/()->new TreeMap<>(String.CASE_INSENSITIVE_ORDER)));
		
		if (chartOfAccounts
			|| openingBalance) {
			
			if (records>=0 && records!=SampleChartOfAccounts.values().length)
				throw new UnsupportedOperationException("When generating data for template '"+template.getName()
					+"' it's not possible to define a total number of records different than "
					+SampleChartOfAccounts.values().length);
		}
		
		if (chartOfAccounts) {
			DocumentField account_category_field_map = template.getField(ChartOfAccountsArchetype.FIELDS_NAMES.AccountCategory.name());
			String account_category_domain_table_name = (account_category_field_map==null) ? null : account_category_field_map.getDomainTableName();
			standard = AccountBuiltInDomainTables.getAccountStandardRelatedToDomainTable(account_category_domain_table_name);
			taxPayerIdField = template.getField(ChartOfAccountsArchetype.FIELDS_NAMES.TaxPayerId.name());
		}
		
		if (openingBalance) {
			taxPayerIdField = template.getField(OpeningBalanceArchetype.FIELDS_NAMES.TaxPayerId.name());
		}

		if (generalLedger) {
			if (this.records<2) {
				throw new Exception("General ledger must have at least 2 records!");
			}
			taxPayerIdField = template.getField(GeneralLedgerArchetype.FIELDS_NAMES.TaxPayerId.name());
		}

		accountBalance = new HashMap<>();
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.CustomDataGenerator#start()
	 */
	@Override
	public void start() {
		recordsCreated = 0;
		
		year = randomDataGenerator.nextRandomYear();
		
		int num_digits_for_taxpayer_id = (taxPayerIdField==null) ? 10 : Math.min(20, Math.max(1, Optional.ofNullable(taxPayerIdField.getMaxLength()).orElse(10)));
		taxpayerId = randomDataGenerator.nextRandomNumberFixedLength(num_digits_for_taxpayer_id);
		
		if (openingBalance || generalLedger) {
			
			// Define initial balance for accounts
			
			double initialAssetBalance, initialLiabilityBalance, initialEquityBalance;

			boolean startOfBusiness = randomDataGenerator.getRandomGenerator().nextInt(ODDS_START_OF_BUSINESS) == 0;
			if (startOfBusiness) {
				initialAssetBalance = initialLiabilityBalance = initialEquityBalance = 0.0;
			}
			else {
				initialAssetBalance = Math.max(1.0, randomDataGenerator.nextRandomDecimal().doubleValue()) * 100.0;
				double equityProportion = (0.1+randomDataGenerator.getRandomGenerator().nextDouble()*0.5);
				initialEquityBalance = equityProportion * initialAssetBalance;
				initialEquityBalance = Math.floor(initialEquityBalance * 100.0) / 100.0; // round to 2 decimals
				initialLiabilityBalance = initialAssetBalance - initialEquityBalance;
			}
			
			if (initialAssetBalance!=0) {
				SampleChartOfAccounts assets[] = SampleChartOfAccounts.assets();
				// Random grades for each asset (bigger numbers means larger proportion of total value)
				int[] grade = IntStream.range(0, assets.length).map(i->randomDataGenerator.getRandomGenerator().nextInt(10)).toArray();
				int grade_sum = Arrays.stream(grade).sum();
				if (grade_sum==0) {
					// If all grades are zero, let's put all the value in the first account
					grade_sum = grade[0] = 1;
				}
				double accumulated = 0;
				for (int i=0; i<assets.length; i++) {
					double proportion = (double)grade[i] / (double)grade_sum;
					double value;
					if (i==assets.length-1) {
						// The last one must complete the whole value (avoid problem with rounding precision)
						value = initialAssetBalance - accumulated;
					}
					else {
						value = initialAssetBalance * proportion;
						value = Math.floor(value * 100.0) / 100.0; // round to 2 decimals
					}
					accumulated += value;
					accountBalance.put(assets[i].getAccountCode(), value); // positive means 'debit nature'
				}
			}
			
			if (initialLiabilityBalance!=0) {
				SampleChartOfAccounts liabilities[] = SampleChartOfAccounts.liabilities();				
				// Random grades for each liability (bigger numbers means larger proportion of total value)
				int[] grade = IntStream.range(0, liabilities.length).map(i->randomDataGenerator.getRandomGenerator().nextInt(10)).toArray();
				int grade_sum = Arrays.stream(grade).sum();
				if (grade_sum==0) {
					// If all grades are zero, let's put all the value in the first account
					grade_sum = grade[0] = 1;
				}
				double accumulated = 0;
				for (int i=0; i<liabilities.length; i++) {
					double proportion = (double)grade[i] / (double)grade_sum;
					double value;
					if (i==liabilities.length-1) {
						// The last one must complete the whole value (avoid problem with rounding precision)
						value = initialLiabilityBalance - accumulated;
					}
					else {
						value = initialLiabilityBalance * proportion;
						value = Math.floor(value * 100.0) / 100.0; // round to 2 decimals
					}
					accumulated += value;
					accountBalance.put(liabilities[i].getAccountCode(), -value); // negative means 'credit nature'
				}
			}
			
			if (initialEquityBalance!=0) {
				accountBalance.put(SampleChartOfAccounts.STOCK.getAccountCode(), -initialEquityBalance); // negative means 'credit nature'
			}
		}
		
		if (generalLedger) {
			
			// Define number of transactions per day
			// Each transaction corresponds to 2 or more records
			
			int number_of_days = new GregorianCalendar(year, Calendar.JANUARY, 1).getActualMaximum(Calendar.DAY_OF_YEAR);
			transactionsPerDay = new int[number_of_days];
			days = new LocalDate[number_of_days];
			currentDay = 0;
			int filledDays = 0;
			
			if (records/2<number_of_days) {
				long recordsAccumulated = 0;
				LocalDate currentDay = LocalDate.of(year, /*month*/1, /*dayOfMonth*/1);
				while (recordsAccumulated<records) {
					long recordsRemaining = records - recordsAccumulated;
					int recordsAtDay = (recordsRemaining<=4) ? (int)recordsRemaining 
							: ( 2 + randomDataGenerator.getRandomGenerator().nextInt(2) );  // 2 or 3
					transactionsPerDay[filledDays] = recordsAtDay;
					days[filledDays] = currentDay;
					recordsAccumulated += recordsAtDay;
					currentDay = currentDay.plusDays(1);
					filledDays++;
				}		
			}
			else {
				long recordsPerDayAverage = records / number_of_days;
				long recordsAccumulated = 0;
				LocalDate currentDay = LocalDate.of(year, /*month*/1, /*dayOfMonth*/1);
				for (int i=0; i<number_of_days && recordsAccumulated<records; i++) {
					long recordsRemaining = records - recordsAccumulated;
					int recordsAtDay = (recordsRemaining<=2) ? (int)recordsRemaining 
						: (int)Math.max(2, (long)Math.ceil(randomDataGenerator.nextRandomGauss() * recordsPerDayAverage + recordsPerDayAverage));
					if (recordsAtDay>recordsRemaining)
						recordsAtDay = (int)recordsRemaining;
					if (recordsRemaining-recordsAtDay==1) {
						recordsAtDay++;
					}
					transactionsPerDay[filledDays] = recordsAtDay;
					days[filledDays] = currentDay;
					recordsAccumulated += recordsAtDay;
					currentDay = currentDay.plusDays(1);
					filledDays++;
				}
				long recordsRemaining = records - recordsAccumulated;
				if (recordsRemaining>0)
					transactionsPerDay[filledDays-1] += recordsRemaining;
			}
			if (filledDays<number_of_days) {
				transactionsPerDay = Arrays.copyOf(transactionsPerDay, filledDays);
				days = Arrays.copyOf(days, filledDays);
			}
			
			transactionId = 0;
			nextDebits = new LinkedList<>();
			nextCredits = new LinkedList<>();
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.CustomDataGenerator#getTaxpayerId()
	 */
	@Override
	public String getTaxpayerId() {
		return (taxpayerId==null) ? null : taxpayerId.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.CustomDataGenerator#getTaxYear()
	 */
	@Override
	public Number getTaxYear() {
		return (year==0) ? null : year;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.CustomDataGenerator#nextRecord()
	 */
	@Override
	public Map<String, Object> nextRecord() {
		if (records>=0 && recordsCreated>=records) {
			return null;
		}
		if (chartOfAccounts || openingBalance) {
			if (recordsCreated>=SampleChartOfAccounts.values().length)
				return null;
		}
		if (generalLedger) {
			if (nextDebits.isEmpty() && nextCredits.isEmpty()) {
				while (currentDay<transactionsPerDay.length 
						&& transactionsPerDay[currentDay]==0)
					currentDay++;
				if (currentDay>=transactionsPerDay.length)
					return null;
			}
		}
		
		Map<String, Object> record = new HashMap<>();
		
		if (chartOfAccounts) {
			nextRecordForChartOfAccounts(record);
		}
		
		else if (openingBalance) {
			nextRecordForOpeningBalance(record);			
		}
		
		else if (generalLedger) {
			nextRecordForGeneralLedger(record);
		}
		
		recordsCreated++;
		return record;
	}
	
	private void nextRecordForChartOfAccounts(Map<String, Object> record) {
		
		SampleChartOfAccounts account = SampleChartOfAccounts.values()[recordsCreated];
		
		record.put(ChartOfAccountsArchetype.FIELDS_NAMES.TaxPayerId.name(), taxpayerId.toString());
		record.put(ChartOfAccountsArchetype.FIELDS_NAMES.TaxYear.name(), year);
		record.put(ChartOfAccountsArchetype.FIELDS_NAMES.AccountCode.name(), account.getAccountCode());
		record.put(ChartOfAccountsArchetype.FIELDS_NAMES.AccountCategory.name(), account.getCategory().getNumber(standard));
		record.put(ChartOfAccountsArchetype.FIELDS_NAMES.AccountSubcategory.name(), account.getSubcategory().getNumber(standard));
		record.put(ChartOfAccountsArchetype.FIELDS_NAMES.AccountName.name(), account.getAccountName());
		record.put(ChartOfAccountsArchetype.FIELDS_NAMES.AccountDescription.name(), account.getAccountDescription());
		
	}

	private void nextRecordForOpeningBalance(Map<String, Object> record) {
		
		SampleChartOfAccounts account = SampleChartOfAccounts.values()[recordsCreated];

		record.put(OpeningBalanceArchetype.FIELDS_NAMES.TaxPayerId.name(), taxpayerId.toString());
		record.put(OpeningBalanceArchetype.FIELDS_NAMES.TaxYear.name(), year);
		record.put(OpeningBalanceArchetype.FIELDS_NAMES.InitialDate.name(), LocalDate.of(year, /*month*/1, /*dayOfMonth*/1));
		record.put(OpeningBalanceArchetype.FIELDS_NAMES.AccountCode.name(), account.getAccountCode());
		double balance = accountBalance.getOrDefault(account.getAccountCode(), 0.0);
		record.put(OpeningBalanceArchetype.FIELDS_NAMES.InitialBalance.name(), Math.abs(balance));
		record.put(OpeningBalanceArchetype.FIELDS_NAMES.DebitCredit.name(), (balance>0.0) ? "D" : "C");
		
	}

	private void nextRecordForGeneralLedger(Map<String, Object> record) {
		
		if (nextDebits.isEmpty() && nextCredits.isEmpty()) {
			// We need another transaction
			transactionId++;
			int recordsRemainingAtDay = transactionsPerDay[currentDay];
			int transactionDebits, transactionCredits;
			if (recordsRemainingAtDay<=3) {
				transactionDebits = (recordsRemainingAtDay==3) ? 2 : 1; 
				transactionCredits = 1;
			}
			else {
				transactionDebits = 1 + randomDataGenerator.getRandomGenerator().nextInt(Math.min(3, recordsRemainingAtDay-2));
				transactionCredits = 1 + randomDataGenerator.getRandomGenerator().nextInt(Math.min(3, recordsRemainingAtDay-transactionDebits));
			}
			int transactionEntries = transactionDebits + transactionCredits;
			recordsRemainingAtDay -= transactionEntries;
			if (recordsRemainingAtDay==1) {
				transactionDebits++;
				transactionEntries++;
				recordsRemainingAtDay--;
			}
			transactionsPerDay[currentDay] = recordsRemainingAtDay;
			// Choose random debits and random credits in such a way the total debited amount equals the total credited amount
			double totalDebitsAmount = 0;
			List<String> choosableAccounts = new ArrayList<>(accountDescriptions.keySet());
			for (int i=0; i<transactionDebits; i++) {
				double amount = randomDataGenerator.nextRandomDecimal().doubleValue();
				amount = Math.floor(amount * 100.0) / 100.0; // round to 2 decimals
				String account = chooseDebitedAccount(amount, choosableAccounts);
				PartialEntry entry = new PartialEntry(account, amount);
				nextDebits.add(entry);
				choosableAccounts.remove(account);
				totalDebitsAmount += amount;
			}
			if (transactionCredits==1) {
				double amount = totalDebitsAmount;
				String account = chooseCreditedAccount(amount, choosableAccounts);
				PartialEntry entry = new PartialEntry(account, amount);
				nextCredits.add(entry);
				choosableAccounts.remove(account);
			}
			else {
				int[] grade = IntStream.range(0, transactionCredits).map(i->1+randomDataGenerator.getRandomGenerator().nextInt(10)).toArray();
				int grade_sum = Arrays.stream(grade).sum();
				double accumulated = 0;
				for (int i=0; i<transactionCredits; i++) {
					double proportion = (double)grade[i] / (double)grade_sum;
					double value;
					if (i==transactionCredits-1) {
						// The last one must complete the whole value (avoid problem with rounding precision)
						value = totalDebitsAmount - accumulated;
						value = Math.round(value * 100.0) / 100.0; // round to 2 decimals
					}
					else {
						value = totalDebitsAmount * proportion;
						value = Math.floor(value * 100.0) / 100.0; // round to 2 decimals
					}
					String account = chooseCreditedAccount(value, choosableAccounts);
					PartialEntry entry = new PartialEntry(account, value);
					nextCredits.add(entry);
					choosableAccounts.remove(account);
					accumulated += value;
				}
			}
		}
		
		LocalDate currentDate = days[currentDay];

		record.put(GeneralLedgerArchetype.FIELDS_NAMES.TaxPayerId.name(), taxpayerId.toString());
		record.put(GeneralLedgerArchetype.FIELDS_NAMES.TaxYear.name(), year);
		record.put(GeneralLedgerArchetype.FIELDS_NAMES.EntryId.name(), String.format("#%06d", transactionId));			
		record.put(GeneralLedgerArchetype.FIELDS_NAMES.Date.name(), currentDate);			
		
		if (!nextDebits.isEmpty()) {
			// Consuming debits of the same transaction
			PartialEntry entry = nextDebits.remove(0);
			record.put(GeneralLedgerArchetype.FIELDS_NAMES.AccountCode.name(), entry.getAccount());			
			record.put(GeneralLedgerArchetype.FIELDS_NAMES.Description.name(), "Debit - "+accountDescriptions.get(entry.getAccount()));			
			record.put(GeneralLedgerArchetype.FIELDS_NAMES.Amount.name(), entry.getAmount());			
			record.put(GeneralLedgerArchetype.FIELDS_NAMES.DebitCredit.name(), "D");			
		}
		else {
			// Consuming credits of the same transaction
			PartialEntry entry = nextCredits.remove(0);
			record.put(GeneralLedgerArchetype.FIELDS_NAMES.AccountCode.name(), entry.getAccount());						
			record.put(GeneralLedgerArchetype.FIELDS_NAMES.Description.name(), "Credit - "+accountDescriptions.get(entry.getAccount()));			
			record.put(GeneralLedgerArchetype.FIELDS_NAMES.Amount.name(), entry.getAmount());			
			record.put(GeneralLedgerArchetype.FIELDS_NAMES.DebitCredit.name(), "C");			
		}

	}
	
	/**
	 * Choose some account for debit
	 * @param amount Amount to be debited
	 * @param choosableAccounts Accounts that may be chosen
	 */
	private String chooseDebitedAccount(double amount, List<String> choosableAccounts) {
		if (choosableAccounts.isEmpty())
			return null;
		if (choosableAccounts.size()==1)
			return choosableAccounts.get(0);
		return choosableAccounts.get(randomDataGenerator.getRandomGenerator().nextInt(choosableAccounts.size()));
	}

	/**
	 * Choose some account for credit
	 * @param amount Amount to be credited
	 * @param choosableAccounts Accounts that may be chosen
	 */
	private String chooseCreditedAccount(double amount, List<String> choosableAccounts) {
		if (choosableAccounts.isEmpty())
			return null;
		if (choosableAccounts.size()==1)
			return choosableAccounts.get(0);
		return choosableAccounts.get(randomDataGenerator.getRandomGenerator().nextInt(choosableAccounts.size()));
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		
	}

}
