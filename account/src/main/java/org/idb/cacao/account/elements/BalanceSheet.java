package org.idb.cacao.account.elements;

/**
 * Wraps computed information about balance sheet while iterating over General Ledger entries
 * @author Gustavo Figueiredo
 */
public class BalanceSheet {
	
	/**
	 * Positive = debit, Negative = credit
	 */
	private double initialValue;
	
	/**
	 * Sum of debits
	 */
	private double debits;
	
	/**
	 * Sum of credits
	 */
	private double credits;
	
	/**
	 * Count of book entries
	 */
	private int countEntries;

	/**
	 * Positive = debit, Negative = credit
	 */
	public double getInitialValue() {
		return initialValue;
	}

	/**
	 * Positive = debit, Negative = credit
	 */
	public void setInitialValue(double initialValue) {
		this.initialValue = initialValue;
	}
	
	public boolean isInitialValueDebit() {
		return initialValue >= 0;
	}

	/**
	 * Sum of debits
	 */
	public double getDebits() {
		return debits;
	}

	/**
	 * Sum of debits
	 */
	public void setDebits(double debits) {
		this.debits = debits;
	}

	/**
	 * Sum of credits
	 */
	public double getCredits() {
		return credits;
	}

	/**
	 * Sum of credits
	 */
	public void setCredits(double credits) {
		this.credits = credits;
	}
	
	/**
	 * Compute a book entry from journal into this object
	 */
	public void computeEntry(Number amount, boolean isDebit) {
		if (amount==null)
			return;
		if (isDebit)
			debits += Math.abs(amount.doubleValue());
		else
			credits += Math.abs(amount.doubleValue());
		countEntries++;
	}
	
	/**
	 * Positive = debit, Negative = credit
	 */
	public double getFinalValue() {
		return initialValue + debits - credits;
	}
	
	public boolean isFinalValueDebit() {
		return  ( initialValue + debits - credits ) >= 0;
	}

	/**
	 * Count of book entries
	 */
	public int getCountEntries() {
		return countEntries;
	}

	/**
	 * Count of book entries
	 */
	public void setCountEntries(int countEntries) {
		this.countEntries = countEntries;
	}
			
	/**
	 * Overwrite the initial balance amount with the final balance amount and reset the total debits and credits.
	 * Useful for making use of the same object for the 'next period' for monthly balance sheets.
	 */
	public void flipBalance() {
		initialValue = getFinalValue();
		debits = 0;
		credits = 0;
		countEntries = 0;
	}
}