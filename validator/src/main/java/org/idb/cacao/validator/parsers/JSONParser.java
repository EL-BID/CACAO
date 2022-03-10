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
package org.idb.cacao.validator.parsers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.idb.cacao.validator.utils.JSONUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Implements {@link HirarquicalDocumentParser} interface to parse JSON files. <br>
 * <br>
 * 
 *  
 * @author Leon Silva
 * 
 * @since 15/11/2021
 *
 */
public class JSONParser extends HirarquicalDocumentParser {
	@SuppressWarnings("unchecked")
	@Override
	protected Map<String, Object> contentToMap(String textContent) {
		Map<String, Object> result;
		try {
			result = new ObjectMapper().readValue(textContent, HashMap.class);

			return result;
		} catch (JsonProcessingException e) {
			//e.printStackTrace();
		}
		
		return Collections.emptyMap();
	}

	@Override
	protected Boolean validateFile(String textContent) {
		return JSONUtils.isJSONValid(textContent);
	}

}
