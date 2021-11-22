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

import java.util.Arrays;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Enumeration of different situations of documents received from tax payers
 * 
 * @author Rivelino Patrício
 * 
 * @since 03/11/2021
 *
 */
public enum DocumentSituation {

	RECEIVED("doc.situation.received"),
	ACCEPTED("doc.situation.accepted"),
	
	/**
	 * Document considered VALID according to validation phase, but did not
	 * complete the ETL yet
	 */
	VALID("doc.situation.valid"),
	
	/**
	 * Document considered INVALID according to validation phase. The document
	 * should be replaced by another one in order to proceed
	 */
	INVALID("doc.situation.invalid"),
	
	/**
	 * Document considered temporarily INCONSISTENT according to the ETL phase.
	 * It may be still VALID, but should be processed again by the ETL phase
	 * in order to be considered PROCESSED. It usually means there are at least
	 * two uploaded files that does not conform to each other, so at least one
	 * of them should be replaced.
	 */
	PENDING("doc.situation.pending"),
	
	/**
	 * Document considered 'replaced' by another one
	 */
	REPLACED("doc.situation.replaced"),
	
	/**
	 * Document considered PROCESSED, which means it was considered VALID by
	 * the validation phase and has completed the ETL phase.
	 */
	PROCESSED("doc.situation.processed");	

	private final String display;
	
	DocumentSituation(String display) {
		this.display = display;
	}

	@Override
	public String toString() {
		return display;
	}
	
	public static DocumentSituation parse(String s) {
		if (s==null || s.trim().length()==0)
			return null;
		return Arrays.stream(values()).filter(t->t.name().equalsIgnoreCase(s)).findAny().orElse(null);
	}

	public static DocumentSituation parse(String s, MessageSource messageSource) {
		if (s==null || s.trim().length()==0)
			return null;
		return Arrays.stream(values()).filter(t->t.name().equalsIgnoreCase(s)).findAny()
				.orElse(Arrays.stream(values()).filter(t->messageSource.getMessage(t.toString(),null,LocaleContextHolder.getLocale()).equalsIgnoreCase(s)).findAny()
						.orElse(null));
	}

}
