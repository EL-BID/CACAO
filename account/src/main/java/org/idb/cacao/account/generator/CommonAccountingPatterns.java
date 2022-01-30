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

import static org.idb.cacao.account.generator.SampleChartOfAccounts.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.collections4.SetUtils;
import org.idb.cacao.api.utils.RandomDataGenerator;

/**
 * Some 'common accounting patterns' to be used when randomly generating accounting data
 * 
 * @author Gustavo Figueiredo
 *
 */
public enum CommonAccountingPatterns {

	STOCK_ISSUANCE(debit(CASH,10_000), 
			credit(STOCK, 10_000)),
	
	INVENTORY_PURCHASE(debit(INVENTORY,10_000), 
			credit(PAYABLE, 10_000)),
	
	RENT_EXPENSE(debit(RENT,500), 
			credit(CASH, 500)),

	SERVICES_EXPENSE(debit(SERVICES,500), 
			credit(CASH, 500)),

	ADMINISTRATIVE_EXPENSE(debit(ADMINISTRATIVE_EXPENSES,500), 
			credit(CASH, 500)),

	ACCOUNTS_PAYABLE(debit(PAYABLE,500), 
			credit(CASH, 500)),

	SERVICES_REVENUE_CASH(debit(CASH,500), debit(SALES_EXPENSES, 100), 
			credit(REVENUE_SERVICES, 500), credit(INVENTORY, 100)),
	
	SERVICES_REVENUE_RECEIVABLE(debit(RECEIVABLE,500), debit(SALES_EXPENSES, 100), 
			credit(REVENUE_SERVICES, 500), credit(INVENTORY, 100));
	
	private final Entry[] entries;
	
	CommonAccountingPatterns(Entry... entries) {
		this.entries = entries;
	}
		
	public Entry[] getEntries() {
		return entries;
	}
	
	public int getNumDebits() {
		return (int)Arrays.stream(entries).filter(Entry::isDebit).count();
	}
	
	public int getNumCredits() {
		return (int)Arrays.stream(entries).filter(Entry::isCredit).count();		
	}
	
	/**
	 * Returns the configured entries with different values calculated as random variables
	 * with roughly a lognormal distribution in the same order of magnitude as the configured
	 * value, keeping the same proportion.
	 */
	public List<Entry> getEntriesWithRandomValues(RandomDataGenerator r) {
		int debits = getNumDebits();
		int credits = getNumCredits();
		if (debits==1 && credits==1) {
			double logNormalMed = Math.log(entries[0].getValue());
			double logNormalVar = Math.min(logNormalMed-1, 6.0);
			Number amount = r.nextRandomLogNormal(logNormalMed, logNormalVar);
			return Arrays.asList(
				entries[0].withValue(amount.doubleValue()),
				entries[1].withValue(amount.doubleValue())
			);
		}
		else if (debits==1) {
			Entry debit = Arrays.stream(entries).filter(Entry::isDebit).findAny().get();
			double logNormalMed = Math.log(debit.getValue());
			double logNormalVar = Math.min(logNormalMed-1, 6.0);
			double amount = r.nextRandomLogNormal(logNormalMed, logNormalVar).doubleValue();
			amount = Math.floor(amount * 100.0) / 100.0; // round to 2 decimals
			List<Entry> new_entries = new ArrayList<>(entries.length);
			new_entries.add(debit.withValue(amount));
			int credits_remaining = entries.length - 1;
			double credits_balance_remaining = amount;
			double credits_normalization_value = debit.getValue(); // equals the sum of all credits
			for (Entry entry: entries) {
				if (entry.isDebit())
					continue; // already included the debit in new_entries
				if (credits_remaining==1) {
					double entry_value = credits_balance_remaining;
					entry_value = Math.floor(entry_value * 100.0) / 100.0; // round to 2 decimals
					new_entries.add(entry.withValue(entry_value));
				}
				else {
					double entry_value = entry.getValue() * amount / credits_normalization_value;
					entry_value = Math.floor(entry_value * 100.0) / 100.0; // round to 2 decimals
					new_entries.add(entry.withValue(entry_value));
				}
				credits_remaining--;
			}
			return new_entries;
		}
		else if (credits==1) {
			Entry credit = Arrays.stream(entries).filter(Entry::isCredit).findAny().get();
			double logNormalMed = Math.log(credit.getValue());
			double logNormalVar = Math.min(logNormalMed-1, 6.0);
			double amount = r.nextRandomLogNormal(logNormalMed, logNormalVar).doubleValue();
			amount = Math.floor(amount * 100.0) / 100.0; // round to 2 decimals
			List<Entry> new_entries = new ArrayList<>(entries.length);
			int debits_remaining = entries.length - 1;
			double debits_balance_remaining = amount;
			double debits_normalization_value = credit.getValue(); // equals the sum of all debits
			for (Entry entry: entries) {
				if (entry.isCredit())
					continue; // will include the credit in new_entries at the end
				if (debits_remaining==1) {
					double entry_value = debits_balance_remaining;
					entry_value = Math.floor(entry_value * 100.0) / 100.0; // round to 2 decimals
					new_entries.add(entry.withValue(entry_value));
				}
				else {
					double entry_value = entry.getValue() * amount / debits_normalization_value;
					entry_value = Math.floor(entry_value * 100.0) / 100.0; // round to 2 decimals
					new_entries.add(entry.withValue(entry_value));
				}
				debits_remaining--;
			}
			new_entries.add(credit.withValue(amount));
			return new_entries;
		}
		else {
			TreeSet<Double> values_debit = Arrays.stream(entries).filter(Entry::isDebit).map(Entry::getValue).collect(Collectors.toCollection(TreeSet::new));
			TreeSet<Double> values_credit = Arrays.stream(entries).filter(Entry::isCredit).map(Entry::getValue).collect(Collectors.toCollection(TreeSet::new));
			// If the debits matches the credits in value per entry (one debit for one credit), than we will choose one random value for the largest configured value
			// and keep the same proportion for the remaining values.
			if (SetUtils.isEqualSet(values_debit, values_credit)) {
				// Let's calculate a random value based on the largest value. All the other values will be proportions of this.
				double largest_value = values_debit.last();
				double logNormalMed = Math.log(largest_value);
				double logNormalVar = Math.min(logNormalMed-1, 10.0);
				double amount = r.nextRandomLogNormal(logNormalMed, logNormalVar).doubleValue();
				double epsilon = 0.01;
				amount = Math.floor(amount * 100.0) / 100.0; // round to 2 decimals
				List<Entry> new_entries = new ArrayList<>(entries.length);
				for (Entry entry: entries) {
					boolean is_entry_with_the_largest_value = (Math.abs(entry.getValue()-largest_value)<epsilon);
					if (is_entry_with_the_largest_value) {
						double entry_value = amount;
						new_entries.add(entry.withValue(entry_value));
					}
					else {
						double entry_value = entry.getValue() * amount / largest_value;
						entry_value = Math.floor(entry_value * 100.0) / 100.0; // round to 2 decimals
						new_entries.add(entry.withValue(entry_value));
					}
				}
				return new_entries;
			}
			else {
				return Arrays.asList(getEntries());
			}
		}
	}
	
	private static Entry debit(SampleChartOfAccounts account, double value) {
		return new Entry(account, true, value);
	}

	private static Entry credit(SampleChartOfAccounts account, double value) {
		return new Entry(account, false, value);
	}

	/**
	 * Bookeeping entry
	 */
	public static class Entry {
		/**
		 * Some account represented at the sample chart of accounts
		 */
		private final SampleChartOfAccounts account;
		
		/**
		 * True for debit, False for credit
		 */
		private final boolean debit;
		
		/**
		 * Value of this entry. The value may be randomly generated based on the same 'order'
		 * of this value. All other entries of the same pattern with the same value should hold
		 * the same random value.
		 */
		private final double value;
		
		public Entry(SampleChartOfAccounts account, boolean debit, double value) {
			this.account = account;
			this.debit = debit;
			this.value = value;
		}
		
		/**
		 * Some account represented at the sample chart of accounts
		 */
		public SampleChartOfAccounts getAccount() {
			return account;
		}
		
		/**
		 * True for debit, False for credit
		 */
		public boolean isDebit() {
			return debit;
		}

		/**
		 * False for debit, True for credit
		 */
		public boolean isCredit() {
			return !debit;
		}

		/**
		 * Value of this entry. The value may be randomly generated based on the same 'order'
		 * of this value. All other entries of the same pattern with the same value should hold
		 * the same random value.
		 */
		public double getValue() {
			return value;
		}
		
		/**
		 * Returns the same entry with a different value
		 */
		public Entry withValue(double value) {
			return new Entry(account, debit, value);
		}
		
		public String toString() {
			return String.format("%s $%.2f %s", account, value, debit?"D":"C");
		}
	}
}
