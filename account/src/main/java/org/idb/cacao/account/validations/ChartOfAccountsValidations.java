/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.account.validations;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.idb.cacao.account.archetypes.AccountBuiltInDomainTables;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DocumentTemplate;

import static org.idb.cacao.api.ValidationContext.*;
import static org.idb.cacao.account.archetypes.ChartOfAccountsArchetype.FIELDS_NAMES.*;
import static org.idb.cacao.account.elements.AccountCategory.*;

/**
 * Performs some validation tests over incoming file related to Chart of Accounts
 * 
 * @author Gustavo Figueiredo
 *
 */
public class ChartOfAccountsValidations {

	/**
	 * Performs domain-specific validations over the uploaded file contents.
	 * The method should return TRUE if the document is OK to proceed in the workflow and should return FALSE if the document
	 * should be rejected.<BR>
	 * This method may insert into {@link org.idb.cacao.api.ValidationContext#addAlert(String) addAlert} any alerts regarding this validation.
	 * It doesn't mean that the file should be rejected. If this method returns TRUE and also outputs some alerts, those alerts
	 * are considered simple warnings (i.e. the file may be accepted despite of these warnings).
	 * @param context Object created by the validator with information regarding the incoming document
	 * @param records Records to validate
	 * @return Returns TRUE if the document is OK and may be considered. Returns FALSE if the document should be rejected
	 */
	public static boolean validateDocumentUploaded(ValidationContext context, List<Map<String,Object>> records) {
		if (context==null 
				|| records==null
				|| records.isEmpty())
			return false;
		
		int count_asset_accounts = 0;
		int count_liabilities_accounts = 0;
		int count_equity_accounts = 0;
		
		DocumentTemplate template = context.getDocumentTemplate();
		DocumentField account_category_field_map = template.getField(AccountCategory.name());
		String account_category_domain_table_name = account_category_field_map.getDomainTableName();
		boolean account_category_gaap = AccountBuiltInDomainTables.isRelatedToGAAP(account_category_domain_table_name);
		boolean account_category_ifrs = AccountBuiltInDomainTables.isRelatedToIFRS(account_category_domain_table_name);
		if (!account_category_gaap && !account_category_ifrs) {
			context.addAlert("The document template '"+template.getName()+"' version '"+template.getVersion()+"' refers to an unknown accounting system! Expected either GAAP or IFRS.");
			return false;
		}
		
		DocumentField account_subcategory_field_map = template.getField(AccountSubcategory.name());		
		String account_subcategory_domain_table_name = account_subcategory_field_map.getDomainTableName();
		boolean account_subcategory_gaap = AccountBuiltInDomainTables.isRelatedToGAAP(account_subcategory_domain_table_name);
		boolean account_subcategory_ifrs = AccountBuiltInDomainTables.isRelatedToIFRS(account_subcategory_domain_table_name);
		if (account_category_gaap && !account_subcategory_gaap) {
			context.addAlert("The document template '"+template.getName()+"' version '"+template.getVersion()
					+"' has inconsistences! The field '"+AccountCategory.name()+"' refers to GAAP, but the field '"+AccountSubcategory.name()+"' refers to something else!");
			return false;
		}
		if (account_category_ifrs && !account_subcategory_ifrs) {
			context.addAlert("The document template '"+template.getName()+"' version '"+template.getVersion()
					+"' has inconsistences! The field '"+AccountCategory.name()+"' refers to IFRS, but the field '"+AccountSubcategory.name()+"' refers to something else!");
			return false;
		}
		
		String key_ASSET = AccountBuiltInDomainTables.getAccountCategoryKey(ASSET.name(), template);
		if (key_ASSET==null) {
			context.addAlert("The document template '"+template.getName()+"' version '"+template.getVersion()
				+"' has inconsistences! Could not find the ASSET in the domain table "+account_category_domain_table_name+"!");
			return false;			
		}
		String key_LIABILITY = AccountBuiltInDomainTables.getAccountCategoryKey(LIABILITY.name(), template);
		if (key_LIABILITY==null) {
			context.addAlert("The document template '"+template.getName()+"' version '"+template.getVersion()
				+"' has inconsistences! Could not find the LIABILITY in the domain table "+account_category_domain_table_name+"!");
			return false;						
		}
		String key_EQUITY = AccountBuiltInDomainTables.getAccountCategoryKey(EQUITY.name(), template);
		if (key_EQUITY==null) {
			context.addAlert("The document template '"+template.getName()+"' version '"+template.getVersion()
				+"' has inconsistences! Could not find the EQUITY in the domain table "+account_category_domain_table_name+"!");
			return false;						
		}
		
		Set<String> accountCodes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		Set<String> accountCodesDuplicates = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		
		boolean success = true;
		
		for (Map<String, Object> record: records) {

			String accountCode = getParsedRequiredContent(context, String.class, record, AccountCode.name());
			if (accountCode==null)
				return false;

			String accountCategory = getParsedRequiredContent(context, String.class, record, AccountCategory.name());
			if (accountCategory==null)
				return false;

			String accountSubcategory = getParsedRequiredContent(context, String.class, record, AccountSubcategory.name());
			if (accountSubcategory==null)
				return false;
			
			if (!accountSubcategory.startsWith(accountCategory)) {
				context.addAlert("{account.error.account.subcategory.misplaced("
					+accountCode.replaceAll("[^\\d\\.\\-]","")
					+","
					+accountCategory.replaceAll("[^\\d\\.\\-]","")
					+","
					+accountSubcategory.replaceAll("[^\\d\\.\\-]","")
					+")}");
				return false;									
			}
			
			if (accountCodes.contains(accountCode)) {
				if (!accountCodesDuplicates.contains(accountCode)) {
					context.addAlert("{account.error.account.duplicate("
						+accountCode.replaceAll("[^\\d\\.\\-]","")
						+")}");
					accountCodesDuplicates.add(accountCode);
				}
				success = false;
				continue;
			}
			accountCodes.add(accountCode);
			
			if (key_ASSET.equals(accountCategory))
				count_asset_accounts++;
			else if (key_LIABILITY.equals(accountCategory))
				count_liabilities_accounts++;
			else if (key_EQUITY.equals(accountCategory))
				count_equity_accounts++;
			
		}
		
		if (count_asset_accounts==0) {
			context.addAlert("{account.error.missing.assets}");
			return false;						
		}

		if (count_liabilities_accounts==0) {
			context.addAlert("{account.error.missing.liabilities}");
			return false;									
		}

		if (count_equity_accounts==0) {
			context.addAlert("{account.error.missing.equity}");
			return false;												
		}

		return success;
	}
	
}
