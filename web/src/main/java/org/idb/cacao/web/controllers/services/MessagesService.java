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
package org.idb.cacao.web.controllers.services;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.idb.cacao.api.DocumentValidationErrorMessage;
import org.idb.cacao.web.repositories.DocumentValidationErrorMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

/**
 * Utilities methods to 
 * 
 * @author Rivelino Patrício
 * 
 * @since 20/11/2021
 *
 */
@Service
public class MessagesService {
	
	private static final Logger log = Logger.getLogger(MessagesService.class.getName());
	
	//Find message and parameters
	private Pattern pMessage = Pattern.compile("\\{([\\w\\.]+)(\\(.*?\\))?\\}",Pattern.CASE_INSENSITIVE);
	
	//Find a message inside a parameter
	private Pattern pMessageParam = Pattern.compile("\\{([\\w\\.]*)\\}", Pattern.CASE_INSENSITIVE);

	@Autowired
	private MessageSource messages;
	
	@Autowired
	private DocumentValidationErrorMessageRepository documentValidationErrorMessageRepository;
	
	/**
	 * Convert a parameterized message to a language specific message
	 *  
	 * @param errorMessage	Message to be converted
	 * @return	Language specific message
	 */
	public String getMessage(String errorMessage) {
		
		if ( errorMessage == null )
			return "";
		
		if ( !errorMessage.contains("{") )
			return errorMessage;
		
		Matcher m = pMessage.matcher(errorMessage);
		
		if( !m.find() )
			return errorMessage;
		
		String message = m.group(1);
		
		List<String> params = new LinkedList<>();
		
		if ( m.groupCount() > 1 ) {
			
			String paramValues = m.group(2);
			if ( paramValues != null ) {
				paramValues = paramValues.replace(")", "").substring(1);
				params.addAll(Arrays.asList(paramValues.split("\\(")));
			}

			//Replace any reference to messages inside a parameter for a language specific message
			for ( int i = 0; i < params.size(); i++  ) {
				
				String param = params.get(i);
				Matcher m2 = pMessageParam.matcher(param);
				
				if ( m2.find() ) {
					params.set(i, messages.getMessage(param.replace("{", "").replace("}", ""), null, LocaleContextHolder.getLocale()));
				}						
				
			}
			
		}
		
		String messageToReturn = errorMessage;
		
		try {
			messageToReturn = messages.getMessage(message, params.isEmpty() ? null : params.toArray(new Object[0]), LocaleContextHolder.getLocale());	
		} catch (Exception e) {
			log.log(Level.WARNING, "Error creating langage specific message for " + errorMessage, e);			
		}
		
		return messageToReturn;
	}
	
	/**
	 * Find and return all document error messages, update the message and return it all.
	 * @param documentId
	 * @return
	 */
	public List<DocumentValidationErrorMessage> findByDocumentId(String documentId) {
		
		List<DocumentValidationErrorMessage> messages = documentValidationErrorMessageRepository.findByDocumentId(documentId);
		
		if ( messages == null )
			return Collections.emptyList();
		
		if ( messages.isEmpty() )
			return messages;
		
		for ( DocumentValidationErrorMessage message : messages ) {
			message.setErrorMessage(getMessage(message.getErrorMessage()));
		}
		
		return messages;
		
	}
	
}
