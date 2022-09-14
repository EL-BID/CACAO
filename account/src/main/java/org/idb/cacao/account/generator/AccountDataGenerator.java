/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.account.generator;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.collections4.ListUtils;
import org.idb.cacao.account.archetypes.AccountBuiltInDomainTables;
import org.idb.cacao.account.archetypes.ChartOfAccountsArchetype;
import org.idb.cacao.account.archetypes.GeneralLedgerArchetype;
import org.idb.cacao.account.archetypes.OpeningBalanceArchetype;
import org.idb.cacao.account.elements.AccountCategory;
import org.idb.cacao.account.elements.AccountStandard;
import org.idb.cacao.account.elements.AccountSubcategory;
import org.idb.cacao.account.etl.PartialEntry;
import org.idb.cacao.api.errors.GeneralException;
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
	
	private static final Logger log = Logger.getLogger(AccountDataGenerator.class.getName());

	public static final int FIXED_NUMBER_OF_ACCOUNTS = 100;
	
	public static final int DEFAULT_NUMBER_OF_LEDGER_ENTRIES = 10_000;
	
	/**
	 * Tells with it should generate additional warnings at LOG whenever getting absurd account balances
	 * whenever generating journal entries.
	 */
	public static boolean LOG_WARN_ABSURD_BALANCES = false;
	
	/**
	 * Some minimum value of CASH to start with. 
	 */
	public static final double MIN_CASH_VALUE = 10_000;

	/**
	 * Some minimum value of INVENTORY to start with. 
	 */
	public static final double MIN_INVENTORY_VALUE = 10_000;

	/**
	 * Some minimum value of EQUITY to start with. 
	 */
	public static final double MIN_EQUITY_VALUE = 10_000;
	
	/**
	 * Some maximum value of 'accounts payable' at LIABILITY
	 */
	public static final double MAX_LIABILITY_PAYABLE = 10_000;
	
	/**
	 * Some arbitrary 'small value' (e.g. little expense)
	 */
	public static final double SMALL_VALUE = 100;

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

	private final Set<String> assetAccounts;

	private final Set<String> liabilityAccounts;

	private final Set<String> equityAccounts;

	private final Set<String> revenueAccounts;
	
	private final Set<String> expenseAccounts;
	
	private final Set<String> customerRelatedAccounts;
	
	private final Set<String> supplierRelatedAccounts;
	
	private Set<String> cashAccounts;
	
	private Set<String> inventoryAccounts;
	
	private Set<String> payableAccounts;

	private AccountStandard standard;

	private int recordsCreated;
	
	private DocumentField taxPayerIdField;
	
	private int numDigitsForTaxpayerId;
	
	private Number taxpayerId;
	
	private int year;
	
	private int providedYear;
	
	private int[] transactionsPerDay;
	
	private int currentDay;
	
	private LocalDate[] days;
	
	private List<PartialEntry> nextCredits, nextDebits;
	
	private long transactionId;
	
	private String currTransactionTrackingInfoForWarnings;
	
	private Map<String, Double> previousBalancesForWarnings;
		
	/**
	 * This is the random generator for other 'documents' related to the same template. Useful for generating
	 * other 'taxpayers id' that might actually be generated in other instances.
	 */
	private Random genSeed;
	
	/**
	 * Some customers ID's to be used in some of the entries
	 */
	private Number[] customers;
	
	/**
	 * Customer ID to use in the current transaction of General Ledger
	 */
	private Number customerId;
	
	/**
	 * Some suppliers ID's to be used in some of the entries
	 */
	private Number[] suppliers;

	/**
	 * Supplier ID to use in the current transaction of General Ledger
	 */
	private Number supplierId;

	public AccountDataGenerator(DocumentTemplate template, DocumentFormat format, long seed, long records) 
			throws GeneralException {
		
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

		this.assetAccounts = Arrays.stream(SampleChartOfAccounts.values())
				.filter(a->AccountCategory.ASSET.equals(a.getCategory()))
				.map(SampleChartOfAccounts::getAccountCode)
				.collect(Collectors.toCollection(()->new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));

		this.liabilityAccounts = Arrays.stream(SampleChartOfAccounts.values())
				.filter(a->AccountCategory.LIABILITY.equals(a.getCategory()))
				.map(SampleChartOfAccounts::getAccountCode)
				.collect(Collectors.toCollection(()->new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));

		this.equityAccounts = Arrays.stream(SampleChartOfAccounts.values())
				.filter(a->AccountCategory.EQUITY.equals(a.getCategory()))
				.map(SampleChartOfAccounts::getAccountCode)
				.collect(Collectors.toCollection(()->new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));

		this.revenueAccounts = Arrays.stream(SampleChartOfAccounts.values())
				.filter(a->AccountCategory.REVENUE.equals(a.getCategory()))
				.map(SampleChartOfAccounts::getAccountCode)
				.collect(Collectors.toCollection(()->new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));

		this.expenseAccounts = Arrays.stream(SampleChartOfAccounts.values())
				.filter(a->AccountCategory.EXPENSE.equals(a.getCategory()))
				.map(SampleChartOfAccounts::getAccountCode)
				.collect(Collectors.toCollection(()->new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
		
		this.cashAccounts = Arrays.stream(SampleChartOfAccounts.values())
				.filter(a->AccountSubcategory.ASSET_CASH.equals(a.getSubcategory()))
				.map(SampleChartOfAccounts::getAccountCode)
				.collect(Collectors.toCollection(()->new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
		
		this.inventoryAccounts = Arrays.stream(SampleChartOfAccounts.values())
				.filter(a->AccountSubcategory.ASSET_INVENTORY.equals(a.getSubcategory()))
				.map(SampleChartOfAccounts::getAccountCode)
				.collect(Collectors.toCollection(()->new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
		
		this.payableAccounts = Arrays.stream(SampleChartOfAccounts.values())
				.filter(a->AccountSubcategory.LIABILITY_PAYABLE.equals(a.getSubcategory()))
				.map(SampleChartOfAccounts::getAccountCode)
				.collect(Collectors.toCollection(()->new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));

		this.customerRelatedAccounts = Arrays.stream(SampleChartOfAccounts.values())
				.filter(a->AccountSubcategory.ASSET_RECEIVABLE.equals(a.getSubcategory()) 
						|| AccountSubcategory.REVENUE_NET.equals(a.getSubcategory()))
				.map(SampleChartOfAccounts::getAccountCode)
				.collect(Collectors.toCollection(()->new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));

		this.supplierRelatedAccounts = Arrays.stream(SampleChartOfAccounts.values())
				.filter(a->AccountSubcategory.ASSET_INVENTORY.equals(a.getSubcategory()))
				.map(SampleChartOfAccounts::getAccountCode)
				.collect(Collectors.toCollection(()->new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
		
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
				throw new GeneralException("General ledger must have at least 2 records!");
			}
			taxPayerIdField = template.getField(GeneralLedgerArchetype.FIELDS_NAMES.TaxPayerId.name());
		}

		numDigitsForTaxpayerId = (taxPayerIdField==null) ? 10 : Math.min(20, Math.max(1, Optional.ofNullable(taxPayerIdField.getMaxLength()).orElse(10)));

		accountBalance = new HashMap<>();
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
				initialEquityBalance = roundDecimals(equityProportion * initialAssetBalance);
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
						value = roundDecimals(initialAssetBalance * proportion);
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
						value = roundDecimals(initialLiabilityBalance * proportion);
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
			
			// Defines some 'customers' and some 'suppliers' to be used in some of the generated entries
			int number_of_customers = randomDataGenerator.getRandomGenerator().nextInt(5)+5;
			int number_of_suppliers = randomDataGenerator.getRandomGenerator().nextInt(4)+2;
			customers = generateOthersTaxpayersIds(number_of_customers).toArray(new Number[0]);
			suppliers = generateOthersTaxpayersIds(number_of_suppliers).toArray(new Number[0]);
		}
	}
	
	/**
	 * Generates a number of 'id's for others taxpayers different from the declaring one
	 */
	private Set<Number> generateOthersTaxpayersIds(int number) {
		if (genSeed==null)
			genSeed = newRandom(randomDataGenerator.getRandomGenerator().nextLong());
		Random rSkipIds = newRandom(randomDataGenerator.getRandomGenerator().nextLong());
		Set<Number> generated = new TreeSet<>();
		while (generated.size()<number) {
			long doc_seed = genSeed.nextLong();
			if (rSkipIds.nextInt(3)!=0)
				continue; // skip some (2 out of 3) of the id's
			RandomDataGenerator doc_random = new RandomDataGenerator(doc_seed);
			Number id = doc_random.nextRandomNumberFixedLength(numDigitsForTaxpayerId);
			if (taxpayerId!=null && taxpayerId.equals(id))
				continue;
			generated.add(id);
		}
		return generated;
	}
	
	public double getBalance(Collection<String> accounts) {
		return accounts.stream().mapToDouble(acc->accountBalance.getOrDefault(acc, 0.0)).sum();
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
			int recordsRemainingAtDay = Math.min(transactionsPerDay[currentDay], 
					(records-recordsCreated)>Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)(records-recordsCreated));
			
			final double cash_balance = getBalance(cashAccounts);
			final double inventory_balance = getBalance(inventoryAccounts);
			final double expenses_balance = getBalance(expenseAccounts);

			// The minus sign is due to the convention of representing 'credit' as 'negative value'
			final double equity_balance = -getBalance(equityAccounts);
			final double revenue_balance = -getBalance(revenueAccounts);
			
			// If the cash is low, try to increase using some standard patterns
			if ((recordsRemainingAtDay==4 || recordsRemainingAtDay>=6) 
					&& cash_balance<MIN_CASH_VALUE) {
				
				// If the equity is also low, use the standard pattern of stock issuance
				if (equity_balance<MIN_EQUITY_VALUE) {
					chooseStandardTransaction(CommonAccountingPatterns.STOCK_ISSUANCE);
				}
				// If the inventory is also low, use the standard pattern of inventory purchase
				else if (inventory_balance<MIN_INVENTORY_VALUE) {
					chooseStandardTransaction(CommonAccountingPatterns.INVENTORY_PURCHASE);
				}
				// For other cases, use the standard pattern of revenues + costs for increasing CASH
				else {
					chooseStandardTransaction(CommonAccountingPatterns.SERVICES_REVENUE_CASH);
				}
			}
			
			// If the inventory is low, use the standard pattern of inventory purchase
			else if ((recordsRemainingAtDay==2 || recordsRemainingAtDay>=4) 
					&& inventory_balance<MIN_INVENTORY_VALUE) {
				chooseStandardTransaction(CommonAccountingPatterns.INVENTORY_PURCHASE);
			}
			
			// Three out of 5 will be a standard pattern of revenue + cost
			// One out of 5 will be a standard pattern of some expense (unless the total expenses approaches total revenues)
			else if (recordsRemainingAtDay>=6) {
						
				final int odds = randomDataGenerator.getRandomGenerator().nextInt(5);
				
				if ((revenue_balance-expenses_balance)<SMALL_VALUE) {
					if (odds<2)
						chooseStandardTransaction(CommonAccountingPatterns.SERVICES_REVENUE_RECEIVABLE);
					else
						chooseStandardTransaction(CommonAccountingPatterns.SERVICES_REVENUE_CASH);					
				}
				else {
					if (odds<3) {				
						if (odds==0)
							chooseStandardTransaction(CommonAccountingPatterns.SERVICES_REVENUE_RECEIVABLE);
						else
							chooseStandardTransaction(CommonAccountingPatterns.SERVICES_REVENUE_CASH);
					}
					else if (odds==3) {
						double liability_payable_balance = -getBalance(payableAccounts);
						if (liability_payable_balance>MAX_LIABILITY_PAYABLE) {
							chooseStandardTransaction(CommonAccountingPatterns.ACCOUNTS_PAYABLE);						
						}
						else {
							int expense_type = randomDataGenerator.getRandomGenerator().nextInt(3);
							switch (expense_type) {
							case 0:
								chooseStandardTransaction(CommonAccountingPatterns.SERVICES_EXPENSE);
								break;
							case 1:
								chooseStandardTransaction(CommonAccountingPatterns.ADMINISTRATIVE_EXPENSE);
								break;
							default:
								chooseStandardTransaction(CommonAccountingPatterns.RENT_EXPENSE);
							}
						}
					}
					else {					
						// For all other cases, generates a random transaction
						chooseRandomTransaction();
					}
				}
				
			}
			
			// For all other cases, generates a random transaction
			else {
				chooseRandomTransaction();
			}
			
			customerId = supplierId = null;
			
			boolean has_customer_in_tx = nextDebits.stream().anyMatch(e->customerRelatedAccounts.contains(e.getAccount()))
					|| nextCredits.stream().anyMatch(e->customerRelatedAccounts.contains(e.getAccount()));
			boolean has_supplier_in_tx = nextDebits.stream().anyMatch(e->supplierRelatedAccounts.contains(e.getAccount()))
					|| nextCredits.stream().anyMatch(e->supplierRelatedAccounts.contains(e.getAccount()));
			if (has_customer_in_tx) {
				customerId = customers[randomDataGenerator.getRandomGenerator().nextInt(customers.length)];
			}
			if (has_supplier_in_tx) {
				supplierId = suppliers[randomDataGenerator.getRandomGenerator().nextInt(suppliers.length)];
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
			
			if (customerId!=null && customerRelatedAccounts.contains(entry.getAccount())) {
				record.put(GeneralLedgerArchetype.FIELDS_NAMES.CustomerSupplierId.name(), customerId.toString());			
			}
			else if (supplierId!=null && supplierRelatedAccounts.contains(entry.getAccount())) {
				record.put(GeneralLedgerArchetype.FIELDS_NAMES.CustomerSupplierId.name(), supplierId.toString());							
			}
			
			// Updates balance for this account (avoid picking the same account with insufficient balance for the next transaction)
			Double balance = accountBalance.getOrDefault(entry.getAccount(), 0.0);
			accountBalance.put(entry.getAccount(), balance + entry.getAmount());
		}
		else {
			// Consuming credits of the same transaction
			PartialEntry entry = nextCredits.remove(0);
			record.put(GeneralLedgerArchetype.FIELDS_NAMES.AccountCode.name(), entry.getAccount());						
			record.put(GeneralLedgerArchetype.FIELDS_NAMES.Description.name(), "Credit - "+accountDescriptions.get(entry.getAccount()));			
			record.put(GeneralLedgerArchetype.FIELDS_NAMES.Amount.name(), entry.getAmount());	
			record.put(GeneralLedgerArchetype.FIELDS_NAMES.AmountDebitOnly.name(), null);
			record.put(GeneralLedgerArchetype.FIELDS_NAMES.AmountCreditOnly.name(), null);
			record.put(GeneralLedgerArchetype.FIELDS_NAMES.DebitCredit.name(), "C");			

			if (customerId!=null && customerRelatedAccounts.contains(entry.getAccount())) {
				record.put(GeneralLedgerArchetype.FIELDS_NAMES.CustomerSupplierId.name(), customerId.toString());							
			}
			else if (supplierId!=null && supplierRelatedAccounts.contains(entry.getAccount())) {
				record.put(GeneralLedgerArchetype.FIELDS_NAMES.CustomerSupplierId.name(), supplierId.toString());											
			}

			// Updates balance for this account (avoid picking the same account with insufficient balance for the next transaction)
			Double balance = accountBalance.getOrDefault(entry.getAccount(), 0.0);
			accountBalance.put(entry.getAccount(), balance - entry.getAmount());
		}

		if (LOG_WARN_ABSURD_BALANCES) {
			checkAbsurdBalances();
		}
	}
	
	/**
	 * Generates warnings at LOG for some absurd situations
	 */
	private void checkAbsurdBalances() {
		double prev_total_asset_balance = 0;
		double curr_total_asset_balance = 0;
		
		if (previousBalancesForWarnings==null) {
			previousBalancesForWarnings = new HashMap<>(accountBalance);
		}
		
		for (String acc: assetAccounts) {
			double curr_balance = roundDecimals(accountBalance.getOrDefault(acc, 0.0));
			double prev_balance = roundDecimals(previousBalancesForWarnings.getOrDefault(acc, 0.0));
			if (curr_balance<0 && prev_balance>=0) {
				log.log(Level.WARNING, "Absurd account balance! Account: "+acc+" "+accountDescriptions.get(acc)
						+", category: ASSET, balance: "+curr_balance+", prev: "+prev_balance
						+", taxpayer: "+getTaxpayerId()+", year: "+getTaxYear()+", journal entryId: "+String.format("#%06d", transactionId)
						+", trackInfo: "+currTransactionTrackingInfoForWarnings);
			}
			curr_total_asset_balance += curr_balance;
			prev_total_asset_balance += prev_balance;
		}
		
		for (String acc: equityAccounts) {
			double curr_balance = roundDecimals(-accountBalance.getOrDefault(acc, 0.0)); // invert the sign because by convention we represent credit as negative values
			double prev_balance = roundDecimals(-previousBalancesForWarnings.getOrDefault(acc, 0.0)); // invert the sign because by convention we represent credit as negative values
			if (curr_balance<0 && prev_balance>=0) {
				log.log(Level.WARNING, "Absurd account balance! Account: "+acc+" "+accountDescriptions.get(acc)
						+", category: EQUITY, balance: "+curr_balance+", prev: "+prev_balance
						+", taxpayer: "+getTaxpayerId()+", year: "+getTaxYear()+", journal entryId: "+String.format("#%06d", transactionId)
						+", trackInfo: "+currTransactionTrackingInfoForWarnings);
			}
		}
		
		// If all the entries of the last transaction have already been accounted, let's check the total balance so far ...
		if (nextDebits.isEmpty() && nextCredits.isEmpty()) {
			double prev_total_liability_balance = 0;
			double curr_total_liability_balance = 0;
			for (String acc: liabilityAccounts) {
				double curr_balance = roundDecimals(-accountBalance.getOrDefault(acc, 0.0)); // invert the sign because by convention we represent credit as negative values
				double prev_balance = roundDecimals(-previousBalancesForWarnings.getOrDefault(acc, 0.0)); // invert the sign because by convention we represent credit as negative values
				if (curr_balance<0 && prev_balance>=0) {
					log.log(Level.WARNING, "Absurd account balance! Account: "+acc+" "+accountDescriptions.get(acc)
							+", category: LIABILITY, balance: "+curr_balance+", prev: "+prev_balance
							+", taxpayer: "+getTaxpayerId()+", year: "+getTaxYear()+", journal entryId: "+String.format("#%06d", transactionId)
							+", trackInfo: "+currTransactionTrackingInfoForWarnings);
				}
				curr_total_liability_balance += curr_balance;
				prev_total_liability_balance += prev_balance;
			}
			if ((roundDecimals(curr_total_liability_balance)>roundDecimals(curr_total_asset_balance)) 
					&& (roundDecimals(prev_total_liability_balance)<=roundDecimals(prev_total_asset_balance))) {
				// Not exactly an 'absurd hypothesis', but we shall warn nonetheless
				log.log(Level.WARNING, "Absurd account balance! The total assets amounts to "+curr_total_asset_balance
					+", but the total liabilities amounts to the greater value: "+curr_total_liability_balance
					+", taxpayer: "+getTaxpayerId()+", year: "+getTaxYear()+", journal entryId: "+String.format("#%06d", transactionId)
					+", trackInfo: "+currTransactionTrackingInfoForWarnings);
			}
			double prev_total_revenues = 0;
			double curr_total_revenues = 0;
			for (String acc: revenueAccounts) {
				double curr_balance = -accountBalance.getOrDefault(acc, 0.0); // invert the sign because by convention we represent credit as negative values
				double prev_balance = -previousBalancesForWarnings.getOrDefault(acc, 0.0); // invert the sign because by convention we represent credit as negative values
				prev_total_revenues += prev_balance;
				curr_total_revenues += curr_balance;
			}
			double prev_total_expenses = 0;
			double curr_total_expenses = 0;
			for (String acc: expenseAccounts) {
				double curr_balance = accountBalance.getOrDefault(acc, 0.0); 
				double prev_balance = previousBalancesForWarnings.getOrDefault(acc, 0.0);
				prev_total_expenses += prev_balance;
				curr_total_expenses += curr_balance;
			}
			if ((roundDecimals(curr_total_expenses)>roundDecimals(curr_total_revenues)) 
					&& (roundDecimals(prev_total_expenses)<=roundDecimals(prev_total_revenues))) {
				// Not exactly an 'absurd hypothesis', but we shall warn nonetheless
				log.log(Level.WARNING, "Absurd account balance! The total revenues amounts to "+curr_total_revenues
					+", but the total expenses amounts to the greater value: "+curr_total_expenses
					+", taxpayer: "+getTaxpayerId()+", year: "+getTaxYear()+", journal entryId: "+String.format("#%06d", transactionId)
					+", trackInfo: "+currTransactionTrackingInfoForWarnings);			
			}
			
			previousBalancesForWarnings = new HashMap<>(accountBalance);
		}
		
	}
	
	/**
	 * Use a standard accounting pattern in order to generate a transaction with random numbers
	 */
	private void chooseStandardTransaction(CommonAccountingPatterns pattern) {
		int recordsRemainingAtDay = transactionsPerDay[currentDay];
		List<CommonAccountingPatterns.Entry> entries = pattern.getEntriesWithRandomValues(randomDataGenerator);
		recordsRemainingAtDay -= entries.size();
		transactionsPerDay[currentDay] = recordsRemainingAtDay;
		for (CommonAccountingPatterns.Entry p_entry: entries) {
			PartialEntry entry = new PartialEntry(p_entry.getAccount().getAccountCode(), p_entry.getValue());
			if (p_entry.isDebit())
				nextDebits.add(entry);
			else
				nextCredits.add(entry);
		}
		
		if (LOG_WARN_ABSURD_BALANCES) {
			// Keep track of additional information about the creation of this transaction in case we need to report
			// warnings about it
			currTransactionTrackingInfoForWarnings = pattern.name();
		}
	}
	
	/**
	 * Choose an entirely random transaction
	 */
	private void chooseRandomTransaction() {
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
		List<String> revenueAccounts = new ArrayList<>(this.revenueAccounts);
		List<String> expenseAccounts = new ArrayList<>(this.expenseAccounts);
		List<String> assetAccountsWithNoBalance = assetAccounts.stream()
				.filter(acc->accountBalance.getOrDefault(acc, 0.0)<=0.0) // negative means credit balance, but ASSET has a debit nature
				.collect(Collectors.toList()); 
		List<String> liabilityAccountsWithNoBalance = liabilityAccounts.stream()
				.filter(acc->accountBalance.getOrDefault(acc, 0.0)>=0.0)	// positive means debit balance, but LIABILITY has a credit nature
				.collect(Collectors.toList()); 
		List<String> equityAccountsWithNoBalance = equityAccounts.stream()
				.filter(acc->accountBalance.getOrDefault(acc, 0.0)>=0.0)	// positive means debit balance, but EQUITY has a credit nature
				.collect(Collectors.toList()); 
		List<String> choosableAccountsForDebit = // Accounts that may use for DEBIT (any account except REVENUE, and also except LIABILITY or EQUITY with not enough balance)
				ListUtils.subtract(
				ListUtils.subtract(
				ListUtils.subtract(
				choosableAccounts,
					revenueAccounts), 
					liabilityAccountsWithNoBalance),
					equityAccountsWithNoBalance);
		List<String> choosableAccountsForCredit = // Accounts that may use for CREDIT (any account except EXPENSE)
				ListUtils.subtract(
				ListUtils.subtract(
				choosableAccounts,
					expenseAccounts),
					assetAccountsWithNoBalance);
		
		// Computes the total balance for ASSETS and for LIABILITIES in order to prevent unusual situation of having total LIABILITIES greater than total ASSETS
		double assets_total_balance = getBalance(assetAccounts);
		double liabilities_total_balance = -getBalance(liabilityAccounts); // negative means 'credit', so we invert the sign for comparison with assets
		double revenues_total_balance = -getBalance(revenueAccounts); // negative means 'credit', so we invert the sign for comparison with assets
		double expenses_total_balance = getBalance(expenseAccounts); 
		
		// Choose DEBIT entries
		for (int i=0; i<transactionDebits; i++) {
			double amount = roundDecimals(randomDataGenerator.nextRandomDecimal().doubleValue());
			String account = chooseDebitedAccount(amount, choosableAccountsForDebit);
			if (account!=null) {
				// Avoid leaving LIABILITY or EQUITY accounts with negative (debit) balance
				if (liabilityAccounts.contains(account)
						|| equityAccounts.contains(account)) {
					Double balance =  - accountBalance.getOrDefault(account, 0.0);	// by convention in 'accountBalance' we keep credit balance as negative value
					if (balance > 0 && balance<amount) {
						amount = roundDecimals(balance);	// if the randomly chosen amount surpasses the account balance, use the remaining balance
					}
				}
				// Avoid leaving EXPENSES accounts higher than overall REVENUES
				else if (expenseAccounts.contains(account)) {
					if (revenues_total_balance<=(expenses_total_balance+amount)) {
						double limited_amount = revenues_total_balance - expenses_total_balance;
						if (limited_amount>SMALL_VALUE) {
							amount = limited_amount;
							expenses_total_balance += amount;
						}
						else {
							choosableAccountsForDebit.removeAll(expenseAccounts);
							account = chooseDebitedAccount(amount, choosableAccountsForDebit);
						}
					}
					else {
						expenses_total_balance += amount;
					}
				}
			}
			if (account==null)
				account = expenseAccounts.get(randomDataGenerator.getRandomGenerator().nextInt(expenseAccounts.size()));
			PartialEntry entry = new PartialEntry(account, amount);
			nextDebits.add(entry);
			choosableAccounts.remove(account);
			choosableAccountsForDebit.remove(account);
			choosableAccountsForCredit.remove(account);
			totalDebitsAmount += amount;
			if (assetAccounts.contains(account))
				assets_total_balance += amount;
			else if (liabilityAccounts.contains(account))
				liabilities_total_balance -= amount; // negative means 'credit', so we invert the sign for comparison with assets
		}
		// Choose CREDIT entries
		if (transactionCredits==1) {
			double amount = totalDebitsAmount;
			List<String> assetAccountsWithNotEnoughBalance = assetAccounts.stream()
					.filter(acc->accountBalance.getOrDefault(acc, 0.0)<=amount) 
					.collect(Collectors.toList());
			choosableAccountsForCredit.removeAll(assetAccountsWithNotEnoughBalance);
			if (assets_total_balance<=liabilities_total_balance+amount) {
				choosableAccountsForCredit.removeAll(liabilityAccounts);
			}
			String account = chooseCreditedAccount(amount, choosableAccountsForCredit);
			if (account==null)
				account = revenueAccounts.get(randomDataGenerator.getRandomGenerator().nextInt(revenueAccounts.size()));
			PartialEntry entry = new PartialEntry(account, amount);
			nextCredits.add(entry);
		}
		else {
			int[] grade = IntStream.range(0, transactionCredits).map(i->1+randomDataGenerator.getRandomGenerator().nextInt(10)).toArray();
			int grade_sum = Arrays.stream(grade).sum();
			double accumulated = 0;
			for (int i=0; i<transactionCredits; i++) {
				double proportion = (double)grade[i] / (double)grade_sum;
				double value;
				boolean last_entry = (i==transactionCredits-1);
				if (last_entry) {
					// The last one must complete the whole value (avoid problem with rounding precision or with the surplus discounted in the next condition block)
					value = roundDecimals(totalDebitsAmount - accumulated);
				}
				else {
					value = roundDecimals(totalDebitsAmount * proportion);
				}
				
				List<String> choosableAccountsForCreditThisEntry = choosableAccountsForCredit;
				
				// Avoid choosing for this CREDIT some ASSET account with not enough balance
				final double amount = value;
				List<String> assetAccountsWithNotEnoughBalance = assetAccounts.stream()
						.filter(acc->accountBalance.getOrDefault(acc, 0.0)<=amount) 
						.collect(Collectors.toList()); 
				if (!assetAccountsWithNotEnoughBalance.isEmpty()) {
					assetAccountsWithNotEnoughBalance = new ArrayList<>(choosableAccountsForCredit);
					choosableAccountsForCreditThisEntry.removeAll(assetAccountsWithNotEnoughBalance);
				}

				if (assets_total_balance<=liabilities_total_balance+amount) {
					choosableAccountsForCreditThisEntry.removeAll(liabilityAccounts);
				}

				String account = chooseCreditedAccount(value, choosableAccountsForCreditThisEntry);
				// Avoid leaving ASSET accounts with negative (credit) balance (unless it's the last entry for this transaction)
				if (account!=null && !last_entry 
						&& assetAccounts.contains(account)) {
					Double balance =  accountBalance.getOrDefault(account, 0.0);	// by convention in 'accountBalance' we keep debit balance as positive value
					if (balance > 0 && balance<value) {
						value = roundDecimals(balance);	// if the randomly chosen amount surpasses the account balance, use the remaining balance
					}						
				}
				if (account==null)
					account = revenueAccounts.get(randomDataGenerator.getRandomGenerator().nextInt(revenueAccounts.size()));
				PartialEntry entry = new PartialEntry(account, value);
				nextCredits.add(entry);
				choosableAccounts.remove(account);
				choosableAccountsForCredit.remove(account);
				accumulated += value;
				
				if (assetAccounts.contains(account))
					assets_total_balance -= amount;
				else if (liabilityAccounts.contains(account))
					liabilities_total_balance += amount; // negative means 'credit', so we invert the sign for comparison with assets
			}
		}
		
		if (LOG_WARN_ABSURD_BALANCES) {
			// Keep track of additional information about the creation of this transaction in case we need to report
			// warnings about it
			currTransactionTrackingInfoForWarnings = new StringBuilder()
					.append("random of ")
					.append(transactionDebits)
					.append(" debits and ")
					.append(transactionCredits)
					.append(" credits")
					.toString();
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

	/**
	 * Round to two decimals
	 */
	private static double roundDecimals(double amount) {
		return Math.round(amount * 100.0) / 100.0; // round to 2 decimals
	}

}
