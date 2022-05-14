/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.account.archetypes;

import org.idb.cacao.account.elements.AccountCategory;
import org.idb.cacao.account.elements.AccountStandard;
import org.idb.cacao.account.elements.AccountSubcategory;
import org.idb.cacao.account.elements.DebitCredit;
import org.idb.cacao.account.elements.ShareType;
import org.idb.cacao.account.elements.StatementComprehensiveIncome;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.DomainTable;
import org.idb.cacao.api.templates.FieldType;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES;

/**
 * Built-in domain tables related to accounting. These are 'in-memory replicas'. The application
 * must store domain tables at Elastic Search in order to be used (e.g. to be referenced by DocumentField).
 * 
 * @author Gustavo Figueiredo
 *
 */
public class AccountBuiltInDomainTables {
	
	/**
	 * Domain table for nature of account (i.e. DEBIT or CREDIT)
	 */
	public static DomainTable DEBIT_CREDIT = DomainTable.fromEnum("Debit/Credit", /*version*/"1.0", 
			/*enumeration with values*/DebitCredit.class); 
	
	/**
	 * Domain table for types of shares (i.e. ORDINARY, PREFERENCE, FOUNDER)
	 */
	public static DomainTable SHARE_TYPE = DomainTable.fromEnum("Share Type", /*version*/"1.0", 
			/*enumeration with values*/ShareType.class,
			/*getKey*/ShareType::getKey); 

	/**
	 * Return the account standard related to the chosen domain table name
	 */
	public static AccountStandard getAccountStandardRelatedToDomainTable(String domainTableName) {
		if (isRelatedToGAAP(domainTableName))
			return AccountStandard.GAAP;
		if (isRelatedToIFRS(domainTableName))
			return AccountStandard.IFRS;
		return null;
	}
	
	/**
	 * Returns TRUE if the domain table is related to GAAP built-in domain tables
	 */
	public static boolean isRelatedToGAAP(String domainTableName) {
		if (domainTableName==null)
			return false;
		return ACCOUNT_CATEGORY_GAAP.getName().equalsIgnoreCase(domainTableName)
				|| ACCOUNT_SUBCATEGORY_GAAP.getName().equalsIgnoreCase(domainTableName);
	}

	/**
	 * Returns TRUE if the domain table is related to IFRS built-in domain tables
	 */
	public static boolean isRelatedToIFRS(String domainTableName) {
		if (domainTableName==null)
			return false;
		return ACCOUNT_CATEGORY_IFRS.getName().equalsIgnoreCase(domainTableName)
				|| ACCOUNT_SUBCATEGORY_IFRS.getName().equalsIgnoreCase(domainTableName);		
	}
	
	/**
	 * Given a category name (one of the constants defined in 'AccountCategory' enumeration) and given
	 * a DocumentTemplate (from where we get the field mappings to domain tables), returns the corresponding
	 * code. Returns NULL if it's not possible to get a code from these information.
	 */
	public static String getAccountCategoryKey(
			String categoryName,
			DocumentTemplate template) {
		DocumentField account_category_field_map = template.getField(FIELDS_NAMES.AccountCategory.name());
		if (account_category_field_map==null 
				|| !FieldType.DOMAIN.equals(account_category_field_map.getFieldType())
				|| account_category_field_map.getDomainTableName()==null)
			return null;
		if (isRelatedToGAAP(account_category_field_map.getDomainTableName())) {
			return AccountCategory.parse(categoryName).getGaapNumber();
		}
		if (isRelatedToIFRS(account_category_field_map.getDomainTableName())) {
			return AccountCategory.parse(categoryName).getIfrsNumber();
		}
		return null;
	}

	/**
	 * Given a category name (one of the constants defined in 'AccountSubcategory' enumeration) and given
	 * a DocumentTemplate (from where we get the field mappings to domain tables), returns the corresponding
	 * code. Returns NULL if it's not possible to get a code from these information.
	 */
	public static String getAccountSubcategoryKey(
			String subcategoryName,
			DocumentTemplate template) {
		DocumentField account_subcategory_field_map = template.getField(FIELDS_NAMES.AccountSubcategory.name());
		if (account_subcategory_field_map==null 
				|| !FieldType.DOMAIN.equals(account_subcategory_field_map.getFieldType())
				|| account_subcategory_field_map.getDomainTableName()==null)
			return null;
		if (isRelatedToGAAP(account_subcategory_field_map.getDomainTableName())) {
			return AccountSubcategory.parse(subcategoryName).getGaapNumber();
		}
		if (isRelatedToIFRS(account_subcategory_field_map.getDomainTableName())) {
			return AccountSubcategory.parse(subcategoryName).getIfrsNumber();
		}
		return null;
	}
	
	/**
	 * Given a DocumentTemplate (from where we get the field mappings to domain tables), returns the corresponding
	 * Map the correlates the numeric code according to a specific account system (e.g. GAAP) to the corresponding
	 * category definition.
	 */
	public static Map<String, AccountCategory> getMapOfAccountCategories(DocumentTemplate template) {
		DocumentField account_category_field_map = template.getField(FIELDS_NAMES.AccountCategory.name());
		if (account_category_field_map==null 
				|| !FieldType.DOMAIN.equals(account_category_field_map.getFieldType())
				|| account_category_field_map.getDomainTableName()==null)
			return Collections.emptyMap();
		if (isRelatedToGAAP(account_category_field_map.getDomainTableName())) {
			return Arrays.stream(AccountCategory.values()).collect(Collectors.toMap(
				AccountCategory::getGaapNumber, 
				Function.identity(), 
				(a,b)->a, 
				()->new TreeMap<>(String.CASE_INSENSITIVE_ORDER)));
		}
		if (isRelatedToIFRS(account_category_field_map.getDomainTableName())) {
			return Arrays.stream(AccountCategory.values()).collect(Collectors.toMap(
				AccountCategory::getIfrsNumber, 
				Function.identity(), 
				(a,b)->a, 
				()->new TreeMap<>(String.CASE_INSENSITIVE_ORDER)));
		}
		return Collections.emptyMap();
	}

	/**
	 * Given a DocumentTemplate (from where we get the field mappings to domain tables), returns the corresponding
	 * Map the correlates the numeric code according to a specific account system (e.g. GAAP) to the corresponding
	 * sub-category definition.
	 */
	public static Map<String, AccountSubcategory> getMapOfAccountSubcategories(DocumentTemplate template) {
		DocumentField account_subcategory_field_map = template.getField(FIELDS_NAMES.AccountSubcategory.name());
		if (account_subcategory_field_map==null 
				|| !FieldType.DOMAIN.equals(account_subcategory_field_map.getFieldType())
				|| account_subcategory_field_map.getDomainTableName()==null)
			return Collections.emptyMap();
		if (isRelatedToGAAP(account_subcategory_field_map.getDomainTableName())) {
			return Arrays.stream(AccountSubcategory.values()).collect(Collectors.toMap(
					AccountSubcategory::getGaapNumber, 
				Function.identity(), 
				(a,b)->a, 
				()->new TreeMap<>(String.CASE_INSENSITIVE_ORDER)));
		}
		if (isRelatedToIFRS(account_subcategory_field_map.getDomainTableName())) {
			return Arrays.stream(AccountSubcategory.values()).collect(Collectors.toMap(
					AccountSubcategory::getIfrsNumber, 
				Function.identity(), 
				(a,b)->a, 
				()->new TreeMap<>(String.CASE_INSENSITIVE_ORDER)));
		}
		return Collections.emptyMap();		
	}
	
	/**
	 * Domain table for the category of accounts according to a supported standard
	 */
	public static DomainTable getDomainTableAccountCategory(AccountStandard standard) {
		if (standard==null)
			return null;
		switch (standard) {
		case IFRS:
			return ACCOUNT_CATEGORY_IFRS;
		case GAAP:
			return ACCOUNT_CATEGORY_GAAP;
		default:
			return null;
		}
	}

	/**
	 * Domain table for the category of accounts according to GAAP
	 */
	public static DomainTable ACCOUNT_CATEGORY_GAAP = DomainTable.fromEnum("Account Category GAAP", /*version*/"1.0", 
			/*enumeration with values*/AccountCategory.class, 
			/*getKey*/AccountCategory::getGaapNumber);

	/**
	 * Domain table for the category of accounts according to IFRS
	 */
	public static DomainTable ACCOUNT_CATEGORY_IFRS = DomainTable.fromEnum("Account Category IFRS", /*version*/"1.0", 
			/*enumeration with values*/AccountCategory.class, 
			/*getKey*/AccountCategory::getIfrsNumber);

	/**
	 * Domain table for the sub-category of accounts according to a supported standard
	 */
	public static DomainTable getDomainTableAccountSubcategory(AccountStandard standard) {
		if (standard==null)
			return null;
		switch (standard) {
		case IFRS:
			return ACCOUNT_SUBCATEGORY_IFRS;
		case GAAP:
			return ACCOUNT_SUBCATEGORY_GAAP;
		default:
			return null;
		}
	}

	/**
	 * Domain table for the category of accounts according to GAAP
	 */
	public static DomainTable ACCOUNT_SUBCATEGORY_GAAP = DomainTable.fromEnum("Account Sub-Category GAAP", /*version*/"1.0", 
			/*enumeration with values*/AccountSubcategory.class, 
			/*getKey*/AccountSubcategory::getGaapNumber);

	/**
	 * Domain table for the category of accounts according to IFRS
	 */
	public static DomainTable ACCOUNT_SUBCATEGORY_IFRS = DomainTable.fromEnum("Account Sub-Category IFRS", /*version*/"1.0", 
			/*enumeration with values*/AccountSubcategory.class, 
			/*getKey*/AccountSubcategory::getIfrsNumber);
	
	/**
	 * Domain table for the Statement of Comprehensive Income
	 */
	public static DomainTable ACCOUNT_SCI = DomainTable.fromEnum("Statement of Comprehensive Income", /*version*/"1.0", 
			/*enumeration with values*/StatementComprehensiveIncome.class);
}
