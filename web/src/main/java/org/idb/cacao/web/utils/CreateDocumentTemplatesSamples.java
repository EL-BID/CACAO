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

import static org.idb.cacao.account.archetypes.GeneralLedgerArchetype.FIELDS_NAMES.Date;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.idb.cacao.account.archetypes.AccountBuiltInDomainTables;
import org.idb.cacao.account.archetypes.ChartOfAccountsArchetype;
import org.idb.cacao.account.archetypes.GeneralLedgerArchetype;
import org.idb.cacao.api.Periodicity;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.api.templates.DocumentInput;
import org.idb.cacao.api.templates.DocumentInputFieldMapping;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.FieldMapping;
import org.idb.cacao.api.templates.FieldType;

public class CreateDocumentTemplatesSamples {

    public static List<DocumentTemplate> getSampleTemplates() {

        List<DocumentTemplate> toRet = new ArrayList<>(3);

        //Livro diário
        DocumentTemplate docTemplate = new DocumentTemplate();
        docTemplate.setName("General Ledger");
        docTemplate.setGroup("Accounting");
        docTemplate.setPeriodicity(Periodicity.YEARLY);
        docTemplate.setRequired(true);
        docTemplate.setVersion("1.0");
        docTemplate.setArchetype("accounting.general.ledger");

        toRet.add(docTemplate);
        List<DocumentField> fields = Arrays.asList(
                new DocumentField()
                        .withFieldName(GeneralLedgerArchetype.FIELDS_NAMES.TaxPayerId.name())
                        .withFieldType(FieldType.CHARACTER)
                        .withFieldMapping(FieldMapping.TAXPAYER_ID)
                        .withDescription("Taxpayer Identification Number")
                        .withMaxLength(128)
                        .withRequired(true),
                new DocumentField()
                        .withFieldName(GeneralLedgerArchetype.FIELDS_NAMES.TaxYear.name())
                        .withFieldType(FieldType.INTEGER)
                        .withFieldMapping(FieldMapping.TAX_YEAR)
                        .withDescription("Fiscal year of this financial reporting")
                        .withRequired(true),
                new DocumentField()
                        .withFieldName(Date.name())
                        .withFieldType(FieldType.DATE)
                        .withDescription("Date of the bookentry")
                        .withRequired(true),
                new DocumentField()
                        .withFieldName(GeneralLedgerArchetype.FIELDS_NAMES.AccountCode.name())
                        .withFieldType(FieldType.CHARACTER)
                        .withDescription("Account code (reference to Chart of Account)")
                        .withMaxLength(256)
                        .withRequired(true),
                new DocumentField()
                        .withFieldName(GeneralLedgerArchetype.FIELDS_NAMES.EntryId.name())
                        .withFieldType(FieldType.CHARACTER)
                        .withDescription("Unique identification of bookeeping entry (shared among counterparts of the same double-entry bookeeping)")
                        .withMaxLength(256)
                        .withRequired(true),
                new DocumentField()
                        .withFieldName(GeneralLedgerArchetype.FIELDS_NAMES.Description.name())
                        .withFieldType(FieldType.CHARACTER)
                        .withDescription("Description of this bookeeping entry")
                        .withMaxLength(1024)
                        .withRequired(false),
                new DocumentField()
                        .withFieldName(GeneralLedgerArchetype.FIELDS_NAMES.Amount.name())
                        .withFieldType(FieldType.DECIMAL)
                        .withDescription("The monetary amount of this bookeeping entry")
                        .withRequired(true),
                new DocumentField()
                        .withFieldName(GeneralLedgerArchetype.FIELDS_NAMES.DebitCredit.name())
                        .withFieldType(FieldType.DOMAIN)
                        .withDomainTableName(AccountBuiltInDomainTables.DEBIT_CREDIT.getName())
                        .withDomainTableVersion(AccountBuiltInDomainTables.DEBIT_CREDIT.getVersion())
                        .withDescription("This is an indication of whether this entry is a debit or a credit to the account")
                        .withMaxLength(32)
                        .withRequired(true)
        );

        docTemplate.setFields(fields);
        addInputsGeneralLedger(docTemplate);

        //Plano de contas
        docTemplate = new DocumentTemplate();
        docTemplate.setName("Chart Of Accounts");
        docTemplate.setGroup("Accounting");
        docTemplate.setPeriodicity(Periodicity.YEARLY);
        docTemplate.setRequired(true);
        docTemplate.setVersion("1.0");
        docTemplate.setArchetype("accounting.chart.accounts");

        toRet.add(docTemplate);
        fields = Arrays.asList(
                new DocumentField()
                        .withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.TaxPayerId.name())
                        .withFieldType(FieldType.CHARACTER)
                        .withFieldMapping(FieldMapping.TAXPAYER_ID)
                        .withDescription("Taxpayer Identification Number")
                        .withMaxLength(128)
                        .withRequired(true),
                new DocumentField()
                        .withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.TaxYear.name())
                        .withFieldType(FieldType.INTEGER)
                        .withFieldMapping(FieldMapping.TAX_YEAR)
                        .withDescription("Fiscal year of this financial reporting")
                        .withRequired(true),
                new DocumentField()
                        .withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountCode.name())
                        .withFieldType(FieldType.CHARACTER)
                        .withDescription("Account code")
                        .withMaxLength(256)
                        .withRequired(true),
                new DocumentField()
                        .withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountCategory.name())
                        .withFieldType(FieldType.DOMAIN)
                        .withDomainTableName(AccountBuiltInDomainTables.ACCOUNT_CATEGORY_IFRS.getName())
                        .withDomainTableVersion(AccountBuiltInDomainTables.ACCOUNT_CATEGORY_IFRS.getVersion())
                        .withDescription("Category of this account")
                        .withMaxLength(256)
                        .withRequired(true),
                new DocumentField()
                        .withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountSubcategory.name())
                        .withFieldType(FieldType.DOMAIN)
                        .withDomainTableName(AccountBuiltInDomainTables.ACCOUNT_SUBCATEGORY_IFRS.getName())
                        .withDomainTableVersion(AccountBuiltInDomainTables.ACCOUNT_SUBCATEGORY_IFRS.getVersion())
                        .withDescription("Sub-category of this account")
                        .withMaxLength(256)
                        .withRequired(true),
                new DocumentField()
                        .withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountName.name())
                        .withFieldType(FieldType.CHARACTER)
                        .withDescription("Account name for displaying alongside the account code in different financial reports")
                        .withMaxLength(256)
                        .withRequired(true),
                new DocumentField()
                        .withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountDescription.name())
                        .withFieldType(FieldType.CHARACTER)
                        .withDescription("Account description")
                        .withMaxLength(1024)
                        .withRequired(false)
        );

        docTemplate.setFields(fields);
        addInputsChartOfAccounts(docTemplate);

        //Saldos Iniciais
        docTemplate = new DocumentTemplate();
        docTemplate.setName("Opening Balance");
        docTemplate.setGroup("Accounting");
        docTemplate.setPeriodicity(Periodicity.YEARLY);
        docTemplate.setRequired(true);
        docTemplate.setVersion("1.0");
        docTemplate.setArchetype("accounting.opening.balance");

        toRet.add(docTemplate);
        fields = Arrays.asList(
                new DocumentField()
                        .withFieldName("TaxPayerId")
                        .withFieldType(FieldType.CHARACTER)
                        .withFieldMapping(FieldMapping.TAXPAYER_ID)
                        .withDescription("Taxpayer Identification Number")
                        .withMaxLength(128)
                        .withRequired(true),
                new DocumentField()
                        .withFieldName("TaxYear")
                        .withFieldType(FieldType.INTEGER)
                        .withFieldMapping(FieldMapping.TAX_YEAR)
                        .withDescription("Fiscal year of this financial reporting")
                        .withRequired(true),
                new DocumentField()
                        .withFieldName("InitialDate")
                        .withFieldType(FieldType.DATE)
                        .withDescription("Date for this initial balance and this particular account")
                        .withRequired(true),
                new DocumentField()
                        .withFieldName("AccountCode")
                        .withFieldType(FieldType.CHARACTER)
                        .withDescription("Account code (reference to Chart of Account)")
                        .withMaxLength(256)
                        .withRequired(true),
                new DocumentField()
                        .withFieldName("InitialBalance")
                        .withFieldType(FieldType.DECIMAL)
                        .withDescription("The monetary amount of this initial balance")
                        .withRequired(true),
                new DocumentField()
                        .withFieldName("DebitCredit")
                        .withFieldType(FieldType.DOMAIN)
                        .withDomainTableName(AccountBuiltInDomainTables.DEBIT_CREDIT.getName())
                        .withDomainTableVersion(AccountBuiltInDomainTables.DEBIT_CREDIT.getVersion())
                        .withDescription("This is an indication of whether this balance is debit or credit")
                        .withMaxLength(32)
                        .withRequired(true)
        );

        docTemplate.setFields(fields);
        addInputsOpeningBalance(docTemplate);

        //Lalur
        docTemplate = new DocumentTemplate();
        docTemplate.setName("Lalur");
        docTemplate.setGroup("Accounting");
        docTemplate.setPeriodicity(Periodicity.YEARLY);
        docTemplate.setRequired(false);
        docTemplate.setVersion("1.0");

        toRet.add(docTemplate);
        fields = Arrays.asList(
                new DocumentField()
                        .withFieldName("TaxPayerId")
                        .withFieldType(FieldType.CHARACTER)
                        .withFieldMapping(FieldMapping.TAXPAYER_ID)
                        .withDescription("Taxpayer Identification Number")
                        .withMaxLength(128)
                        .withRequired(true),
                new DocumentField()
                        .withFieldName("TaxYear")
                        .withFieldType(FieldType.INTEGER)
                        .withFieldMapping(FieldMapping.TAX_YEAR)
                        .withDescription("Fiscal year of this financial reporting")
                        .withRequired(true),
                new DocumentField()
                        .withFieldName("AccountCode")
                        .withFieldType(FieldType.CHARACTER)
                        .withDescription("Account code (reference to Chart of Account)")
                        .withMaxLength(256)
                        .withRequired(true),
                new DocumentField()
                        .withFieldName("InitialDate")
                        .withFieldType(FieldType.DATE)
                        .withDescription("The final date of the period when this account was created")
                        .withRequired(true),
                new DocumentField()
                        .withFieldName("FinalDate")
                        .withFieldType(FieldType.DATE)
                        .withDescription("The date until this balance can be used")
                        .withRequired(true),
                new DocumentField()
                        .withFieldName("FinalBalance")
                        .withFieldType(FieldType.DECIMAL)
                        .withDescription("The monetary amount of final balance for this account")
                        .withRequired(true),
                new DocumentField()
                        .withFieldName("DebitCredit")
                        .withFieldType(FieldType.DOMAIN)
                        .withDomainTableName(AccountBuiltInDomainTables.DEBIT_CREDIT.getName())
                        .withDomainTableVersion(AccountBuiltInDomainTables.DEBIT_CREDIT.getVersion())
                        .withDescription("This is an indication of whether this balance is debit or credit")
                        .withMaxLength(32)
                        .withRequired(true)
        );

        docTemplate.setFields(fields);
        addInputsLalur(docTemplate);

        return toRet;

    }

    /**
     * Add sample {@link DocumentInput} for a given {@link DocumentTemplate}
     *
     * @param docTemplate
     */
    private static void addInputsLalur(DocumentTemplate docTemplate) {

        if (docTemplate == null)
            return;

        DocumentInput input = new DocumentInput("CSV Lalur");
        input.setFormat(DocumentFormat.CSV);
        docTemplate.addInput(input);

        List<DocumentInputFieldMapping> mappings = Arrays.asList(
                new DocumentInputFieldMapping()
                        .withFieldName("TaxPayerId")
                        .withColumnIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName("TaxYear")
                        .withColumnIndex(1),
                new DocumentInputFieldMapping()
                        .withFieldName("AccountCode")
                        .withColumnIndex(2),
                new DocumentInputFieldMapping()
                        .withFieldName("InitialDate")
                        .withColumnIndex(3),
                new DocumentInputFieldMapping()
                        .withFieldName("FinalDate")
                        .withColumnIndex(4),
                new DocumentInputFieldMapping()
                        .withFieldName("FinalBalance")
                        .withColumnIndex(5),
                new DocumentInputFieldMapping()
                        .withFieldName("DebitCredit")
                        .withColumnIndex(6)
        );

        input.setFields(mappings);

        input = new DocumentInput("XLS Lalur");
        input.setFormat(DocumentFormat.XLS);
        docTemplate.addInput(input);

        mappings = Arrays.asList(
                new DocumentInputFieldMapping()
                        .withFieldName("TaxPayerId")
                        .withColumnIndex(0)
                        .withSheetIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName("TaxYear")
                        .withColumnIndex(1)
                        .withSheetIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName("AccountCode")
                        .withColumnIndex(2)
                        .withSheetIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName("InitialDate")
                        .withColumnIndex(3)
                        .withSheetIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName("FinalDate")
                        .withColumnIndex(4)
                        .withSheetIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName("FinalBalance")
                        .withColumnIndex(5)
                        .withSheetIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName("DebitCredit")
                        .withColumnIndex(6)
                        .withSheetIndex(0)
        );

        input.setFields(mappings);

    }

    /**
     * Add sample {@link DocumentInput} for a given {@link DocumentTemplate}
     *
     * @param docTemplate
     */
    private static void addInputsOpeningBalance(DocumentTemplate docTemplate) {

        if (docTemplate == null)
            return;

        DocumentInput input = new DocumentInput("CSV Opening Balance");
        input.setFormat(DocumentFormat.CSV);
        docTemplate.addInput(input);

        List<DocumentInputFieldMapping> mappings = Arrays.asList(
                new DocumentInputFieldMapping()
                        .withFieldName("TaxPayerId")
                        .withColumnIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName("TaxYear")
                        .withColumnIndex(1),
                new DocumentInputFieldMapping()
                        .withFieldName("InitialDate")
                        .withColumnIndex(2),
                new DocumentInputFieldMapping()
                        .withFieldName("AccountCode")
                        .withColumnIndex(3),
                new DocumentInputFieldMapping()
                        .withFieldName("InitialBalance")
                        .withColumnIndex(4),
                new DocumentInputFieldMapping()
                        .withFieldName("DebitCredit")
                        .withColumnIndex(5)
        );

        input.setFields(mappings);

        input = new DocumentInput("XLS Opening Balance");
        input.setFormat(DocumentFormat.XLS);
        docTemplate.addInput(input);

        mappings = Arrays.asList(
                new DocumentInputFieldMapping()
                        .withFieldName("TaxPayerId")
                        .withColumnIndex(0)
                        .withSheetIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName("TaxYear")
                        .withColumnIndex(1)
                        .withSheetIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName("InitialDate")
                        .withColumnIndex(2)
                        .withSheetIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName("AccountCode")
                        .withColumnIndex(3)
                        .withSheetIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName("InitialBalance")
                        .withColumnIndex(4)
                        .withSheetIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName("DebitCredit")
                        .withColumnIndex(5)
                        .withSheetIndex(0)
        );

        input.setFields(mappings);

    }

    /**
     * Add sample {@link DocumentInput} for a given {@link DocumentTemplate}
     *
     * @param docTemplate
     */
    private static void addInputsChartOfAccounts(DocumentTemplate docTemplate) {

        if (docTemplate == null)
            return;

        DocumentInput input = new DocumentInput("CSV Chart Of Accounts");
        input.setFormat(DocumentFormat.CSV);
        docTemplate.addInput(input);

        List<DocumentInputFieldMapping> mappings = Arrays.asList(
                new DocumentInputFieldMapping()
                        .withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.TaxPayerId.name())
                        .withColumnIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.TaxYear.name())
                        .withColumnIndex(1),
                new DocumentInputFieldMapping()
                        .withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountCode.name())
                        .withColumnIndex(2),
                new DocumentInputFieldMapping()
                        .withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountCategory.name())
                        .withColumnIndex(3),
                new DocumentInputFieldMapping()
                        .withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountSubcategory.name())
                        .withColumnIndex(4),
                new DocumentInputFieldMapping()
                        .withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountName.name())
                        .withColumnIndex(5),
                new DocumentInputFieldMapping()
                        .withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountDescription.name())
                        .withColumnIndex(6)
        );

        input.setFields(mappings);

        input = new DocumentInput("XLS Chart Of Accounts");
        input.setFormat(DocumentFormat.XLS);
        docTemplate.addInput(input);

        mappings = Arrays.asList(
                new DocumentInputFieldMapping()
                        .withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.TaxPayerId.name())
                        .withColumnIndex(0)
                        .withSheetIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.TaxYear.name())
                        .withColumnIndex(1)
                        .withSheetIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountCode.name())
                        .withColumnIndex(2)
                        .withSheetIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountCategory.name())
                        .withColumnIndex(3)
                        .withSheetIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountSubcategory.name())
                        .withColumnIndex(4)
                        .withSheetIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountName.name())
                        .withColumnIndex(5)
                        .withSheetIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountDescription.name())
                        .withColumnIndex(6)
                        .withSheetIndex(0)
        );

        input.setFields(mappings);


        input = new DocumentInput("JSON Chart Of Accounts");
        input.setFormat(DocumentFormat.JSON);
        docTemplate.addInput(input);

        mappings = Arrays.asList(
                new DocumentInputFieldMapping()
                        .withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.TaxPayerId.name())
                        .withColumnIndex(0)
                        .withSheetIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.TaxYear.name())
                        .withColumnIndex(1)
                        .withSheetIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountCode.name())
                        .withColumnIndex(2)
                        .withSheetIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountCategory.name())
                        .withColumnIndex(3)
                        .withSheetIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountSubcategory.name())
                        .withColumnIndex(4)
                        .withSheetIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountName.name())
                        .withColumnIndex(5)
                        .withSheetIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName(ChartOfAccountsArchetype.FIELDS_NAMES.AccountDescription.name())
                        .withColumnIndex(6)
                        .withSheetIndex(0)
        );

        input.setFields(mappings);


    }

    /**
     * Add sample {@link DocumentInput} for a given {@link DocumentTemplate}
     *
     * @param docTemplate
     */
    private static void addInputsGeneralLedger(DocumentTemplate docTemplate) {

        if (docTemplate == null)
            return;

        DocumentInput input = new DocumentInput("CSV General Ledger");
        input.setFormat(DocumentFormat.CSV);
        docTemplate.addInput(input);

        List<DocumentInputFieldMapping> mappings = Arrays.asList(
                new DocumentInputFieldMapping()
                        .withFieldName(GeneralLedgerArchetype.FIELDS_NAMES.TaxPayerId.name())
                        .withColumnIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName(GeneralLedgerArchetype.FIELDS_NAMES.TaxYear.name())
                        .withColumnIndex(1),
                new DocumentInputFieldMapping()
                        .withFieldName(GeneralLedgerArchetype.FIELDS_NAMES.Date.name())
                        .withColumnIndex(2),
                new DocumentInputFieldMapping()
                        .withFieldName(GeneralLedgerArchetype.FIELDS_NAMES.AccountCode.name())
                        .withColumnIndex(3),
                new DocumentInputFieldMapping()
                        .withFieldName(GeneralLedgerArchetype.FIELDS_NAMES.EntryId.name())
                        .withColumnIndex(4),
                new DocumentInputFieldMapping()
                        .withFieldName(GeneralLedgerArchetype.FIELDS_NAMES.Description.name())
                        .withColumnIndex(5),
                new DocumentInputFieldMapping()
                        .withFieldName(GeneralLedgerArchetype.FIELDS_NAMES.Amount.name())
                        .withColumnIndex(6),
                new DocumentInputFieldMapping()
                        .withFieldName(GeneralLedgerArchetype.FIELDS_NAMES.DebitCredit.name())
                        .withColumnIndex(7)
        );

        input.setFields(mappings);

        input = new DocumentInput("XLS General Ledger");
        input.setFormat(DocumentFormat.XLS);
        docTemplate.addInput(input);

        mappings = Arrays.asList(
                new DocumentInputFieldMapping()
                        .withFieldName(GeneralLedgerArchetype.FIELDS_NAMES.TaxPayerId.name())
                        .withColumnIndex(0)
                        .withSheetIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName(GeneralLedgerArchetype.FIELDS_NAMES.TaxYear.name())
                        .withColumnIndex(1)
                        .withSheetIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName(GeneralLedgerArchetype.FIELDS_NAMES.Date.name())
                        .withColumnIndex(2)
                        .withSheetIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName(GeneralLedgerArchetype.FIELDS_NAMES.AccountCode.name())
                        .withColumnIndex(3)
                        .withSheetIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName(GeneralLedgerArchetype.FIELDS_NAMES.EntryId.name())
                        .withColumnIndex(4)
                        .withSheetIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName(GeneralLedgerArchetype.FIELDS_NAMES.Description.name())
                        .withColumnIndex(5)
                        .withSheetIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName(GeneralLedgerArchetype.FIELDS_NAMES.Amount.name())
                        .withColumnIndex(6)
                        .withSheetIndex(0),
                new DocumentInputFieldMapping()
                        .withFieldName(GeneralLedgerArchetype.FIELDS_NAMES.DebitCredit.name())
                        .withColumnIndex(7)
                        .withSheetIndex(0)
        );

        input.setFields(mappings);
    }

}
