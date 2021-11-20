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
package org.idb.cacao.api;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.idb.cacao.api.templates.DocumentTemplate;
import org.springframework.context.MessageSource;

/**
 * This objects wraps up information collected during the ETL phase. Includes
 * all objects needed for building one or more denormalized views of the data
 * collected in one or more correlated (and pre-validated) files.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class ETLContext {

	/**
	 * This generic interface provides means for fetching pre-validated data
	 * (the output of the 'validation' phase') in generic format (agnostic to
	 * specific file format).
	 * 
	 * @author Gustavo Figueiredo
	 */
	public static interface ValidatedDataRepository {
		
		/**
		 * Given the {@link DocumentTemplate#getArchetype() archetype}, returns the collection of {@link DocumentTemplate DocumentTemplate} that implements it.
		 * Should return empty collection if there is no templates related to the archetype. Should throw exception in
		 * case of error while searching database.
		 */
		public Collection<DocumentTemplate> getTemplates(String archetype) throws Exception;
		
		/**
		 * Given the {@link DocumentUploaded#getTemplateName() templateName}, the {@link DocumentUploaded#getTemplateVersion() templateVersion},
		 * the {@link DocumentUploaded#getTaxPayerId() taxPayerId} and the {@link DocumentUploaded#getTaxPeriodNumber() taxPeriodNumber}, returns
		 * the collection of {@link DocumentUploaded DocumentUploaded} records of pre validated files regarding these criteria.
		 */
		public Collection<DocumentUploaded> getUploads(String templateName, String templateVersion, String taxPayerId, Integer taxPeriodNumber) throws Exception;
		
		/**
		 * Given the {@link DocumentUploaded#getTemplateName() templateName}, the {@link DocumentUploaded#getTemplateVersion() templateVersion},
		 * and the {@link DocumentUploaded#getFileId() fileId}, returns indication that
		 * the import operation finished. Should throw exception in
		 * case of error while searching database.
		 */
		public boolean hasValidation(String templateName, String templateVersion, String fileId) throws Exception;
		 
		/**
		 * Given the {@link DocumentUploaded#getTemplateName() templateName}, the {@link DocumentUploaded#getTemplateVersion() templateVersion},
		 * and the {@link DocumentUploaded#getFileId() fileId}, returns the corresponding
		 * pre-validated data stored in database. Should return empty stream if there is no data. Should throw exception in
		 * case of error while searching database.
		 */
		public Stream<Map<String,Object>> getValidatedData(String templateName, String templateVersion, String fileId) throws Exception;
		
	}
	
	/**
	 * This generic interface provides means for fetching Taxpayers information in order
	 * to project additional data in the denormalized views through the ETL job
	 * 
	 * @author Gustavo Figueiredo
	 *
	 */
	public static interface TaxpayerRepository {
		
		/**
		 * Given the taxpayer Id, should return additional data to be included in denormalized views
		 */
		public Optional<Map<String,Object>> getTaxPayerData(String taxPayerId);
		
	}
	
	/**
	 * Reference to the incoming file that triggered this ETL operation
	 */
	private DocumentUploaded documentUploaded;
	
	/**
	 * The document template related to the incoming file that triggered this ETL operation
	 */
	private DocumentTemplate documentTemplate;

	/**
	 * The consumer of ETL service provides an implementation of this interface in order to allow
	 * domain specific ETL operations gathering all the information it needs.
	 */
	private ValidatedDataRepository validatedDataRepository;
	
	/**
	 * The consumer of ETL service provides an implementation of this interface in order to allow
	 * domain specific ETL operations gathering information about taxpayers.
	 */
	private TaxpayerRepository taxpayerRepository;

	/**
	 * Object used to resolve errors according to a specific language
	 */
	private MessageSource messageSource;

	/**
	 * Reference to the incoming file that triggered this ETL operation
	 */
	public DocumentUploaded getDocumentUploaded() {
		return documentUploaded;
	}

	/**
	 * Reference to the incoming file that triggered this ETL operation
	 */
	public void setDocumentUploaded(DocumentUploaded documentUploaded) {
		this.documentUploaded = documentUploaded;
	}

	/**
	 * The document template related to the incoming file that triggered this ETL operation
	 */
	public DocumentTemplate getDocumentTemplate() {
		return documentTemplate;
	}

	/**
	 * The document template related to the incoming file that triggered this ETL operation
	 */
	public void setDocumentTemplate(DocumentTemplate documentTemplate) {
		this.documentTemplate = documentTemplate;
	}

	/**
	 * The consumer of ETL service provides an implementation of this interface in order to allow
	 * domain specific ETL operations gathering all the information it needs.
	 */
	public ValidatedDataRepository getValidatedDataRepository() {
		return validatedDataRepository;
	}

	/**
	 * The consumer of ETL service provides an implementation of this interface in order to allow
	 * domain specific ETL operations gathering all the information it needs.
	 */
	public void setValidatedDataRepository(ValidatedDataRepository validatedDataRepository) {
		this.validatedDataRepository = validatedDataRepository;
	}

	/**
	 * The consumer of ETL service provides an implementation of this interface in order to allow
	 * domain specific ETL operations gathering information about taxpayers.
	 */
	public TaxpayerRepository getTaxpayerRepository() {
		return taxpayerRepository;
	}

	/**
	 * The consumer of ETL service provides an implementation of this interface in order to allow
	 * domain specific ETL operations gathering information about taxpayers.
	 */
	public void setTaxpayerRepository(TaxpayerRepository taxpayerRepository) {
		this.taxpayerRepository = taxpayerRepository;
	}

	/**
	 * Object used to resolve errors according to a specific language
	 */
	public MessageSource getMessageSource() {
		return messageSource;
	}

	/**
	 * Object used to resolve errors according to a specific language
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}
		
}
