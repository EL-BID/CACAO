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
import java.util.Comparator;
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
	 * @param date Date of this record
	 * @param accountCode Account code
	 * @param amount Amount
	 * @param isDebit Indication whether the account was debited (otherwise it was credited)
	 */
	public void computeEntry(OffsetDateTime date, String accountCode, Number amount, boolean isDebit) {
		
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

			// Try combinations if we have only one account being debited or only one account being credited
			boolean found_all_matches = tryCombinationsOneToMany();
			if (found_all_matches)
				return;
			
			// If we got here, we have multiple credited accounts and multiple debited accounts
			// Let's try to organize the set of entries in multiple sets of matching values			
			boolean found_any = tryCombinationsManyToMany();
			if (creditEntries.isEmpty() && debitEntries.isEmpty())
				return;
			
			if (found_any) {
				// If we found any match in 'tryCombinationsManyToMany' but there are still some entries, let's
				// try once more with 'tryCombinationsOneToMany' considering only the remaining entries
				found_all_matches = tryCombinationsOneToMany();
				if (found_all_matches)
					return;
			}
			
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
	 * Try combinations if we have only one account being debited or only one account being credited<BR>
	 * The matches found are NOT removed from 'debitEntries' and 'creditEntries' (they remain unchanged).
	 * @return Returns TRUE if finished (found all matches). Returns FALSE otherwise.
	 */
	private boolean tryCombinationsOneToMany() {
		if (creditEntries.size()==1 && debitEntries.size()==1) {
			// If we have exactly one credit and one debit, turn it into one AccountingFlow
			DailyAccountingFlow flow = new DailyAccountingFlow();
			flow.setDate(currentDate);
			flow.setDebitedAccountCode(debitEntries.get(0).getAccount());
			flow.setCreditedAccountCode(creditEntries.get(0).getAccount());
			flow.setAmount(Math.min(debitEntries.get(0).getAmount(),creditEntries.get(0).getAmount()));
			addFlow(flow);
			return true;
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
			return true;
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
			return true;
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
			return true;
		}
		
		return false;
	}
	
	/**
	 * Try combinations if we have many accounts being debited and many accounts being credited.<BR>
	 * The matches found are removed from 'debitEntries' and 'creditEntries'.
	 * @return Returns TRUE if found at least one match. Returns FALSE if found none.
	 */
	private boolean tryCombinationsManyToMany() {
		
		// Let's order each set according to their values descending
		debitEntries.sort(Comparator.comparing(PartialEntry::getAmount).reversed());
		creditEntries.sort(Comparator.comparing(PartialEntry::getAmount).reversed());
		
		// Move along trying to match values from one to another
		
		boolean foundAny = false;
		
		// Try one debit to 1 or more credits
		
		int startingCreditNotBiggerThanDebit = 0;
		lookingForDebits:
		for (int debit=0; debit<debitEntries.size(); debit++) {
			double debitAmount = debitEntries.get(debit).getAmount();
			String debitAccount = debitEntries.get(debit).getAccount();
			boolean updateStartingCreditNotBiggerThanDebit = true;
			for (int credit=startingCreditNotBiggerThanDebit; credit<creditEntries.size(); credit++) {
				double creditAmount = creditEntries.get(credit).getAmount();
				String creditAccount = creditEntries.get(credit).getAccount();
				if (creditAmount>debitAmount)
					continue;
				if (updateStartingCreditNotBiggerThanDebit) {
					updateStartingCreditNotBiggerThanDebit = false;
					startingCreditNotBiggerThanDebit = credit; // in the next 'debit loop' we know for sure that any credit before this position is bigger
				}
				if (debitAccount.equals(creditAccount)) 
					 continue; // we won't try to match a debit/credit into the same account
				// Check if we have a 1:1 match
				if (Math.abs(debitAmount - creditAmount) < EPSILON) {
					// We have a 1:1 match, form an accounting flow
					commitFlow1D1C(debit, credit);
					foundAny = true;
					// Fix the debit index because we've removed one debit part when we built the flow
					debit--;
					// Let's move to the next debit
					continue lookingForDebits;
				}
				// If we got here, we know for sure that the 'debitAmount' is bigger than the 'creditAmount',
				// Check if we have a 1:2 match, starting with the extremes (upper and lower) boundaries in credits values
				for (int credit2=creditEntries.size()-1;credit2>credit;credit2--) {
					double credit2Amount = creditEntries.get(credit2).getAmount();
					String credit2Account = creditEntries.get(credit2).getAccount();
					double credits = creditAmount + credit2Amount;
					if (debitAccount.equals(credit2Account)) 
						 continue; // we won't try to match a debit/credit into the same account
					if (Math.abs(debitAmount - credits) < EPSILON) {
						// We have a 1:2 match, form an accounting flow
						commitFlow1D2C(debit, credit, credit2);
						foundAny = true;
						// Fix the debit index because we've removed one debit part when we built the flow
						debit--;
						// Let's move to the next debit
						continue lookingForDebits;
					}
					if (credits > creditAmount)
						break; // there is no point continuing this LOOP as 'the sum of credits' gets bigger
				}
			}
		}
		
		// Try one credit to 2 or more debits (we've already tested all the 1:1 possibilities in the previous loop)
		
		int startingDebitNotBiggerThanCredit = 0;
		lookingForCredits:
		for (int credit=0; credit<creditEntries.size(); credit++) {
			double creditAmount = creditEntries.get(credit).getAmount();
			String creditAccount = creditEntries.get(credit).getAccount();
			boolean updateStartingDebitNotBiggerThanCredit  = true;
			for (int debit=startingDebitNotBiggerThanCredit; debit<debitEntries.size(); debit++) {
				double debitAmount = debitEntries.get(debit).getAmount();
				String debitAccount = debitEntries.get(debit).getAccount();
				if (debitAmount>creditAmount)
					continue;
				if (updateStartingDebitNotBiggerThanCredit) {
					updateStartingDebitNotBiggerThanCredit = false;
					startingDebitNotBiggerThanCredit = debit; // in the next 'credit loop' we know for sure that any debit before this position is bigger
				}
				if (debitAccount.equals(creditAccount)) 
					 continue; // we won't try to match a debit/credit into the same account
				// If we got here, we know for sure that the 'creditAmount' is bigger than the 'debitAmount',
				// Check if we have a 1:2 match, starting with the extremes (upper and lower) boundaries in debit values
				for (int debit2=debitEntries.size()-1;debit2>debit;debit2--) {
					double debit2Amount = debitEntries.get(debit2).getAmount();
					String debit2Account = debitEntries.get(debit2).getAccount();
					double debits = debitAmount + debit2Amount;
					if (debit2Account.equals(creditAccount)) 
						 continue; // we won't try to match a debit/credit into the same account
					if (Math.abs(creditAmount - debits) < EPSILON) {
						// We have a 1:2 match, form an accounting flow
						commitFlow2D1C(debit, debit2, credit);
						foundAny = true;
						// Fix the credit index because we've removed one credit part when we built the flow
						credit--;
						// Let's move to the next credit
						continue lookingForCredits;
					}
					if (debits > creditAmount)
						break; // there is no point continuing this LOOP as 'the sum of debits' gets bigger
				}
			}
		}
		
		// Don't try any other combinations because with multiple debits and multiple credits
		// we don't have a 'good' accounting flow (it would be written as 'many to many')
		
		return foundAny;
	}
	
	/**
	 * Create an accounting flow with one debit and one credit taken from the set of debits and credits
	 */
	private void commitFlow1D1C(int debitIndex, int creditIndex) {
		PartialEntry debitEntry = debitEntries.remove(debitIndex);
		PartialEntry creditEntry = creditEntries.remove(creditIndex);
		DailyAccountingFlow flow = new DailyAccountingFlow();
		flow.setDate(currentDate);
		flow.setDebitedAccountCode(debitEntry.getAccount());
		flow.setCreditedAccountCode(creditEntry.getAccount());
		flow.setAmount(debitEntry.getAmount());
		addFlow(flow);					
	}

	/**
	 * Create two accounting flows with one debit and two credits taken from the set of debits and credits
	 */
	private void commitFlow1D2C(int debitIndex, int creditIndex1, int creditIndex2) {
		PartialEntry debitEntry = debitEntries.remove(debitIndex);
		PartialEntry creditEntry1 = creditEntries.remove(creditIndex1);
		DailyAccountingFlow flow1 = new DailyAccountingFlow();
		flow1.setDate(currentDate);
		flow1.setDebitedAccountCode(debitEntry.getAccount());
		flow1.setCreditedAccountCode(creditEntry1.getAccount());
		flow1.setAmount(creditEntry1.getAmount());
		addFlow(flow1);					
		
		if (creditIndex1<creditIndex2)
			creditIndex2--;
		PartialEntry creditEntry2 = creditEntries.remove(creditIndex2);
		DailyAccountingFlow flow2 = new DailyAccountingFlow();
		flow2.setDate(currentDate);
		flow2.setDebitedAccountCode(debitEntry.getAccount());
		flow2.setCreditedAccountCode(creditEntry2.getAccount());
		flow2.setAmount(creditEntry2.getAmount());
		addFlow(flow2);					

	}

	/**
	 * Create two accounting flows with two debits and one credit taken from the set of debits and credits
	 */
	private void commitFlow2D1C(int debitIndex1, int debitIndex2, int creditIndex) {
		PartialEntry creditEntry = creditEntries.remove(creditIndex);
		PartialEntry debitEntry1 = debitEntries.remove(debitIndex1);
		DailyAccountingFlow flow1 = new DailyAccountingFlow();
		flow1.setDate(currentDate);
		flow1.setDebitedAccountCode(debitEntry1.getAccount());
		flow1.setCreditedAccountCode(creditEntry.getAccount());
		flow1.setAmount(debitEntry1.getAmount());
		addFlow(flow1);					
		
		if (debitIndex1<debitIndex2)
			debitIndex2--;
		PartialEntry debitEntry2 = debitEntries.remove(debitIndex2);
		DailyAccountingFlow flow2 = new DailyAccountingFlow();
		flow2.setDate(currentDate);
		flow2.setDebitedAccountCode(debitEntry2.getAccount());
		flow2.setCreditedAccountCode(creditEntry.getAccount());
		flow2.setAmount(debitEntry2.getAmount());
		addFlow(flow2);					

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
}
