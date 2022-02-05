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
package org.idb.cacao.account.archetypes;

import static org.idb.cacao.account.archetypes.ShareholdersArchetype.FIELDS_NAMES.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.idb.cacao.account.elements.StatementComprehensiveIncome;
import org.idb.cacao.account.etl.ShareholdersLoader;
import org.idb.cacao.account.generator.ShareholdersGenerator;
import org.idb.cacao.account.validations.ShareholdersValidations;
import org.idb.cacao.api.ETLContext;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.templates.CustomDataGenerator;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.DomainTable;
import org.idb.cacao.api.templates.FieldMapping;
import org.idb.cacao.api.templates.FieldType;
import org.idb.cacao.api.templates.TemplateArchetype;

/**
 * This is the archetype for DocumentTemplate's related to SHAREHOLDERS STATEMENT
 * 
 * @author Gustavo Figueiredo
 *
 */
public class ShareholdersArchetype implements TemplateArchetype {

	public static final String NAME = "accounting.shareholders";

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.TemplateArchetype#getPluginName()
	 */
	@Override
	public String getPluginName() {
		return "CACAO Accounting Plug-in";
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.TemplateArchetype#getName()
	 */
	@Override
	public String getName() {
		return NAME;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.TemplateArchetype#getSuggestedGroup()
	 */
	@Override
	public String getSuggestedGroup() {
		return "Financial Report";
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.TemplateArchetype#getBuiltInDomainTables()
	 */
	@Override
	public List<DomainTable> getBuiltInDomainTables() {
		return Arrays.asList(AccountBuiltInDomainTables.SHARE_TYPE);
	}

	public static enum FIELDS_NAMES {
		
		TaxPayerId,
		
		TaxYear,
		
		ShareholderName,
		
		ShareholderId,
		
		ShareType,

		ShareClass,
		
		ShareAmount,
		
		ShareQuantity,
		
		SharePercentage;
		
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.TemplateArchetype#getRequiredFields()
	 */
	@Override
	public List<DocumentField> getRequiredFields() {
		
		List<DocumentField> fields = new ArrayList<>(StatementComprehensiveIncome.values().length+2);
		
		fields.add(new DocumentField()
				.withFieldName(TaxPayerId.name())
				.withFieldType(FieldType.CHARACTER)
				.withFieldMapping(FieldMapping.TAXPAYER_ID)
				.withDescription("Taxpayer Identification Number")
				.withMaxLength(128)
				.withRequired(true)
				.withFileUniqueness(true)
				.withPersonalData(true));
		
		fields.add(new DocumentField()
				.withFieldName(TaxYear.name())
				.withFieldType(FieldType.INTEGER)
				.withFieldMapping(FieldMapping.TAX_YEAR)
				.withDescription("Fiscal year of this financial reporting")
				.withRequired(true)
				.withFileUniqueness(true));

		fields.add(new DocumentField()
				.withFieldName(ShareholderName.name())
				.withFieldType(FieldType.CHARACTER)
				.withDescription("Shareholder's name")
				.withMaxLength(1024)
				.withPersonalData(true));

		fields.add(new DocumentField()
				.withFieldName(ShareholderId.name())
				.withFieldType(FieldType.CHARACTER)
				.withFieldMapping(FieldMapping.TAXPAYER_ID)
				.withDescription("Shareholder's Identification Number")
				.withMaxLength(128)
				.withPersonalData(true));

		fields.add(new DocumentField()
				.withFieldName(ShareType.name())
				.withFieldType(FieldType.DOMAIN)
				.withDomainTableName(AccountBuiltInDomainTables.SHARE_TYPE.getName())
				.withDomainTableVersion(AccountBuiltInDomainTables.SHARE_TYPE.getVersion())
				.withDescription("Share type")
				.withRequired(true));

		fields.add(new DocumentField()
				.withFieldName(ShareClass.name())
				.withFieldType(FieldType.CHARACTER)
				.withMaxLength(128)
				.withDescription("Class of share"));
		
		fields.add(new DocumentField()
				.withFieldName(ShareAmount.name())
				.withFieldType(FieldType.DECIMAL)
				.withDescription("Total amount of shares held"));

		fields.add(new DocumentField()
				.withFieldName(ShareQuantity.name())
				.withFieldType(FieldType.DECIMAL)
				.withDescription("Number of shares held")
				.withRequired(true));

		fields.add(new DocumentField()
				.withFieldName(SharePercentage.name())
				.withFieldType(FieldType.DECIMAL)
				.withDescription("Percentage of shares held (100 = 100%)")
				.withRequired(true));
		
		return fields;		
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.TemplateArchetype#validateDocumentUploaded(org.idb.cacao.api.ValidationContext)
	 */
	@Override
	public boolean validateDocumentUploaded(ValidationContext context) {
		return ShareholdersValidations.validateDocumentUploaded(context, context.getParsedContents());
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.TemplateArchetype#performETL(org.idb.cacao.api.ETLContext)
	 */
	@Override
	public boolean performETL(ETLContext context) {
		return ShareholdersLoader.performETL(context);
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.TemplateArchetype#getRelatedPublishedDataIndices()
	 */
	@Override
	public List<String> getRelatedPublishedDataIndices() {
		return Arrays.asList(ShareholdersLoader.INDEX_PUBLISHED_SHAREHOLDERS);
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.TemplateArchetype#hasCustomGenerator(org.idb.cacao.api.templates.DocumentTemplate, org.idb.cacao.api.templates.DocumentFormat)
	 */
	@Override
	public boolean hasCustomGenerator(DocumentTemplate template, DocumentFormat format) {
		return ShareholdersArchetype.NAME.equalsIgnoreCase(template.getArchetype());
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.api.templates.TemplateArchetype#getCustomGenerator(org.idb.cacao.api.templates.DocumentTemplate, org.idb.cacao.api.templates.DocumentFormat, long, long)
	 */
	@Override
	public CustomDataGenerator getCustomGenerator(DocumentTemplate template, DocumentFormat format, long seed,
			long records) throws Exception {
		if (hasCustomGenerator(template, format))
			return new ShareholdersGenerator(template, format, seed, records);
		else
			return null;
	}

}
