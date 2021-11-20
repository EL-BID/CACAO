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

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.idb.cacao.account.archetypes.ChartOfAccountsArchetype;
import org.idb.cacao.account.archetypes.GeneralLedgerArchetype;
import org.idb.cacao.account.archetypes.OpeningBalanceArchetype;
import org.idb.cacao.api.DocumentSituation;
import org.idb.cacao.api.DocumentUploaded;
import org.idb.cacao.api.ETLContext;
import org.idb.cacao.api.templates.DocumentTemplate;

/**
 * Projects accounting data into Database after validation phases. Performs denormalization
 * of data.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class AccountingLoader {
	
	private static final Logger log = Logger.getLogger(AccountingLoader.class.getName());

	public static final String ACCOUNT_GROUP = "Accounting";
	
	/**
	 * Comparator of Document Templates that gives precedence to the most recent (according to date of creation)
	 */
	public static final Comparator<DocumentTemplate> DOCUMENT_TEMPLATE_COMPARATOR = new Comparator<DocumentTemplate>() {

		@Override
		public int compare(DocumentTemplate t1, DocumentTemplate t2) {
			OffsetDateTime t1_timestamp = t1.getTemplateCreateTime();
			OffsetDateTime t2_timestamp = t2.getTemplateCreateTime();
			return t2_timestamp.compareTo(t1_timestamp);
		}
		
	};
	
	/**
	 * Returns validated document regarding a specific taxpayerId and tax Period according to a collection of document templates
	 */
	public static DocumentUploaded getValidatedDocument(String taxPayerId, Integer taxPeriodNumber, Collection<DocumentTemplate> templates, ETLContext context) throws Exception {
		DocumentUploaded foundUploadWithEmptyData = null;
		for (DocumentTemplate template: templates.stream().sorted(DOCUMENT_TEMPLATE_COMPARATOR).collect(Collectors.toList())) {
			Collection<DocumentUploaded> uploads = context.getValidatedDataRepository().getUploads(template.getName(), template.getVersion(), taxPayerId, taxPeriodNumber);
			if (uploads.isEmpty()) {
				continue; // no uploads found with this particular template, try another template 
			}
			for (DocumentUploaded upload: uploads) {
				if (!DocumentSituation.VALID.equals(upload.getSituation())
					&& !DocumentSituation.PROCESSED.equals(upload.getSituation()))
					continue; // ignores invalid or not validated files
				boolean has_data = context.getValidatedDataRepository().hasValidation(template.getName(), template.getVersion(), upload.getFileId());
				if (!has_data) {
					if (foundUploadWithEmptyData==null)
						foundUploadWithEmptyData = upload; // keep this information in case we don't find any other valid data
					continue;
				}
				// Found data for this template
				return upload;
			}
		}
		if (foundUploadWithEmptyData!=null)
			return foundUploadWithEmptyData;
		// TODO: should report missing upload (not an error, but a warning for the taxpayer)
		return null;
	}

	/**
	 * Performs the Extract/Transform/Load operations with available data
	 */
	public static boolean performETL(ETLContext context) {
		
		try {
				
			// Check for the presence of all required data
			
			Collection<DocumentTemplate> templatesForCoA = context.getValidatedDataRepository().getTemplates(ChartOfAccountsArchetype.NAME);
			if (templatesForCoA.isEmpty())
				return false;
			
			Collection<DocumentTemplate> templatesForLedger = context.getValidatedDataRepository().getTemplates(GeneralLedgerArchetype.NAME);
			if (templatesForLedger.isEmpty())
				return false;
			
			Collection<DocumentTemplate> templatesForOpening = context.getValidatedDataRepository().getTemplates(OpeningBalanceArchetype.NAME);
			if (templatesForOpening.isEmpty())
				return false;
			
			String taxPayerId = context.getDocumentUploaded().getTaxPayerId();
			Integer taxPeriodNumber = context.getDocumentUploaded().getTaxPeriodNumber();
			
			DocumentUploaded coa = getValidatedDocument(taxPayerId, taxPeriodNumber, templatesForCoA, context);
			if (coa==null)
				return false;
			DocumentUploaded gl = getValidatedDocument(taxPayerId, taxPeriodNumber, templatesForLedger, context);
			if (gl==null)
				return false;
			DocumentUploaded ob = getValidatedDocument(taxPayerId, taxPeriodNumber, templatesForOpening, context);
			if (ob==null)
				return false;
			
			// If we got here, we have enough information for generating denormalized data
			
			Stream<Map<String, Object>> gl_data = context.getValidatedDataRepository().getValidatedData(gl.getTemplateName(), gl.getTemplateVersion(), gl.getFileId());
			try {
				
				gl_data.forEach(record->{
					
					// TODO:
					
					// TODO: should write denormalized data (BULK LOAD) to other indices
					// that are used with DASHBOARDS and REPORTS

				});
				
			}
			finally {
				gl_data.close();
			}
			
			// TODO:
			

			return false;
			
		}
		catch (Throwable ex) {
			String fileId = (context==null || context.getDocumentUploaded()==null) ? null : context.getDocumentUploaded().getFileId();
			log.log(Level.SEVERE, "Error while performing ETL regarding file "+fileId, ex);
			return false;
		}
	}
	
}
