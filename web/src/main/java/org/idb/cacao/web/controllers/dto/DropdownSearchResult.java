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
package org.idb.cacao.web.controllers.dto;

import java.util.List;

/**
 * Data Transfer Object for the result of a search on for use with 
 * Fomantic UI dropdown
 * @author Luis Kauer
 *
 */
public class DropdownSearchResult {
	
	/**
	 * The search was successfull
	 */
	private boolean success=true;
	
	private List<ResultItem> results;
	
	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public List<ResultItem> getResults() {
		return results;
	}

	public void setResults(List<ResultItem> results) {
		this.results = results;
	}

	/**
	 * Holds the each result item
	 */
	public static class ResultItem {
		
		/**
		 * Html or text displayed in dropdown
		 */
		private String name;
		
		/**
		 * Selected value
		 */
		private String value;
		
		/**
		 * Text displayed after selection
		 */
		private String text;
		
		/**
		 * Details displayed to the right of the text
		 */
		private String description;
		
		public ResultItem(String name, String value, String description) {
			this.name = name;
			this.value = value;
			this.description = description;
		}
		
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}
}
