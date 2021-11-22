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
package org.idb.cacao.account.etl;

import static org.idb.cacao.api.ValidationContext.ISO_8601_DATE;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.idb.cacao.account.elements.DailyAccountingFlow;

/**
 * Computes the accounting flows while iterating over the bookeeping entries from a
 * General Ledger.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class AccountingFlowProcessor {
		
	/**
	 * Keep track of the current date being processed
	 */
	private LocalDate currentDate;
	
	/**
	 * Stores temporarily the partial entries in order to generate 'DailyAccountingFlow's
	 */
	private final List<PartialEntry> debitEntries; 

	/**
	 * Stores temporarily the partial entries in order to generate 'DailyAccountingFlow's
	 */
	private final List<PartialEntry> creditEntries; 
	
	/**
	 * For each credit account and debited account, acumulates the daily sum
	 */
	private final Map<DailyAccountingFlow, DailyAccountingFlow> mapAccountingFlows;

	/**
	 * Current balance of debits and credits. Positive values means 'debits' and
	 * negative values means 'credits'.<BR> 
	 * Once it reaches zero, it will 'commit' the possible 'accounting flows' up to this point.
	 */
	private double balance;
	
	/**
	 * Sum of debits in one day. Useful for presenting warning messages if unbalanced.
	 */
	private double debits;
	
	/**
	 * Sum of credits in one day. Useful for presenting warning messages if unbalanced.
	 */
	private double credits;
	
	/**
	 * Collects warnings generated during this process
	 */
	private Consumer<String> collectWarnings;
	
	/**
	 * Collects the daily accounting flows during this process
	 */
	private Consumer<DailyAccountingFlow> collectDailyAccountingFlows;
	
	/**
	 * Ignores differences lesser than half of a cent	
	 */
	private static final double EPSILON = 0.005;		
	
	public AccountingFlowProcessor() {
		debitEntries = new LinkedList<>();
		creditEntries = new LinkedList<>();
		mapAccountingFlows = new HashMap<>();
	}

	/**
	 * Collects warnings generated during this process
	 */
	public Consumer<String> getCollectWarnings() {
		return collectWarnings;
	}

	/**
	 * Collects warnings generated during this process
	 */
	public void setCollectWarnings(Consumer<String> collectWarnings) {
		this.collectWarnings = collectWarnings;
	}

	/**
	 * Collects the daily accounting flows during this process
	 */
	public Consumer<DailyAccountingFlow> getCollectDailyAccountingFlows() {
		return collectDailyAccountingFlows;
	}

	/**
	 * Collects the daily accounting flows during this process
	 */
	public void setCollectDailyAccountingFlows(Consumer<DailyAccountingFlow> collectDailyAccountingFlows) {
		this.collectDailyAccountingFlows = collectDailyAccountingFlows;
	}

	/**
	 * Computes a bookeeping entry. It's expected to receive all the entries in the following order: the Date (1st criteria) and the Entry Id (2nd criteria)
	 * @param entryId The entry ID. 
	 * @param date Date of this record
	 * @param accountCode Account code
	 * @param amount Amount
	 * @param isDebit Indication whether the account was debited (otherwise it was credited)
	 */
	public void computeEntry(String entryId, OffsetDateTime date, String accountCode, Number amount, boolean isDebit) {
		
		if (accountCode==null || accountCode.trim().length()==0 || amount==null || date==null || Math.abs(amount.doubleValue())<EPSILON)
			return;
		
		LocalDate d = date.toLocalDate();
		
		final boolean changedDate = (currentDate!=null && !currentDate.equals(d));
		if (changedDate) {
			if (Math.abs(balance) > EPSILON && collectWarnings!=null) {
				collectWarnings.accept("{account.error.debits.credits.unbalanced("+debits+","+credits+","+ISO_8601_DATE.get().format(new Date(currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()))+")}");
			}
			commitPartialEntries();
			commitFlows();
			balance = debits = credits = 0;
		}
		
		if (currentDate==null || changedDate) {
			currentDate = d;
		}
		
		
		if (isDebit) {
			debitEntries.add(new PartialEntry(accountCode, amount));
			balance += Math.abs(amount.doubleValue());
			debits += Math.abs(amount.doubleValue());
		}
		else {
			creditEntries.add(new PartialEntry(accountCode, amount));
			balance -= Math.abs(amount.doubleValue());
			credits += Math.abs(amount.doubleValue());
		}
		
		final boolean shouldCommit = Math.abs(balance) < EPSILON;
		
		if (shouldCommit) {
			commitPartialEntries();
			balance = 0;
		}
	}
	
	/**
	 * Should be called after iterating all of the book entries
	 */
	public void finish() {
		commitFlows();
		balance = debits = credits = 0;
		currentDate = null;
	}
	
	/**
	 * Turns the collection of 'PartialEntry' objects into one or more 'DailyAccountingFlow' objects
	 */
	private void commitPartialEntries() {
		
		try {
		
			if (creditEntries.isEmpty() || debitEntries.isEmpty())
				return; // can't create DailyAccountingFlow without debited accounts or without credited accounts
			
			if (creditEntries.size()==1 && debitEntries.size()==1) {
				// If we have exactly one credit and one debit, turn it into one AccountingFlow
				DailyAccountingFlow flow = new DailyAccountingFlow();
				flow.setDate(currentDate);
				flow.setDebitedAccountCode(debitEntries.get(0).getAccount());
				flow.setCreditedAccountCode(creditEntries.get(0).getAccount());
				flow.setAmount(Math.min(debitEntries.get(0).getAmount(),creditEntries.get(0).getAmount()));
				addFlow(flow);
				return;
			}
			
			Set<String> accounts_credited = creditEntries.stream().map(PartialEntry::getAccount).collect(Collectors.toSet());
			Set<String> accounts_debited = debitEntries.stream().map(PartialEntry::getAccount).collect(Collectors.toSet());

			if (accounts_credited.size()==1 && accounts_debited.size()==1) {
				// If we have multiple entries related to the same account being credited and account being debited, turn into one AccountingFlow
				DailyAccountingFlow flow = new DailyAccountingFlow();
				flow.setDate(currentDate);
				flow.setDebitedAccountCode(accounts_debited.iterator().next());
				flow.setCreditedAccountCode(accounts_credited.iterator().next());
				double sum_amounts_credited = creditEntries.stream().mapToDouble(PartialEntry::getAmount).sum();
				double sum_amounts_debited = debitEntries.stream().mapToDouble(PartialEntry::getAmount).sum();
				flow.setAmount(Math.min(sum_amounts_debited,sum_amounts_credited));
				addFlow(flow);
				return;
			}

			if (accounts_credited.size()==1) {
				// If we have multiple entries of multiple debited account crediting the same account, turn into multiple AccountingFlows (one for each debited account)
				final String creditAccount = accounts_credited.iterator().next();
				for (PartialEntry debit: debitEntries) {
					DailyAccountingFlow flow = new DailyAccountingFlow();
					flow.setDate(currentDate);
					flow.setDebitedAccountCode(debit.getAccount());
					flow.setCreditedAccountCode(creditAccount);
					flow.setAmount(debit.getAmount());
					addFlow(flow);					
				}
				return;
			}
			
			if (accounts_debited.size()==1) {
				// If we have multiple entries of multiple credited account debiting the same account, turn into multiple AccountingFlows (one for each credited account)
				final String debitAccount = accounts_debited.iterator().next();
				for (PartialEntry credit: creditEntries) {
					DailyAccountingFlow flow = new DailyAccountingFlow();
					flow.setDate(currentDate);
					flow.setDebitedAccountCode(debitAccount);
					flow.setCreditedAccountCode(credit.getAccount());
					flow.setAmount(credit.getAmount());
					addFlow(flow);
				}
				return;
			}
			
			// If we got here, we have multiple credited accounts and multiple debited accounts
			
			// TODO: ....
			
			// Let's report the values that could not be represented by AccountingFlows due to lack of specific accounts
			// We will represent them as generic '*' accounts
			DailyAccountingFlow flow = new DailyAccountingFlow();
			flow.setDate(currentDate);
			flow.setDebitedAccountCode(DailyAccountingFlow.MANY_ACCOUNTS);
			flow.setCreditedAccountCode(DailyAccountingFlow.MANY_ACCOUNTS);
			double sum_amounts_credited = creditEntries.stream().mapToDouble(PartialEntry::getAmount).sum();
			double sum_amounts_debited = debitEntries.stream().mapToDouble(PartialEntry::getAmount).sum();
			flow.setAmount(Math.min(sum_amounts_debited,sum_amounts_credited));
			addFlow(flow);					
			
		}
		finally {
			
			creditEntries.clear();
			debitEntries.clear();
			
		}
	}
	
	/**
	 * Writes the committed partial entries after a day have been completed.
	 */
	private void commitFlows() {
		if (collectDailyAccountingFlows!=null) {
			mapAccountingFlows.values().forEach(collectDailyAccountingFlows);
		}
		mapAccountingFlows.clear();
	}
	
	/**
	 * Aggregates daily sum of accounting flows (aggregated by pair of accounts)
	 */
	private void addFlow(DailyAccountingFlow flow) {
		DailyAccountingFlow acumulated = mapAccountingFlows.get(flow);
		if (acumulated==null) {
			mapAccountingFlows.put(flow, flow);
		}		
		else {
			acumulated.addAmount(flow.getAmount());
		}
	}
	
	/**
	 * Auxiliary class for storing temporarily information regarding book entries. Each counterpart will
	 * have one 'PartialEntry' object.
	 * 
	 * @author Gustavo Figueiredo
	 */
	private static class PartialEntry {
		
		/**
		 * The account that was credited or debited
		 */
		private final String account;
		
		/**
		 * The amount credited or debited
		 */
		private final double amount;
		
		PartialEntry(String account, Number amount) {
			this.account = account;
			this.amount = Math.abs(amount.doubleValue());
		}

		public String getAccount() {
			return account;
		}

		public double getAmount() {
			return amount;
		}
		
		public String toString () {
			return String.format("%s: %d", account, amount);
		}
	}
}
