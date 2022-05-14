/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.validator.parsers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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
	
	private static final Logger log = Logger.getLogger(JSONParser.class.getName());
	
	@SuppressWarnings("unchecked")
	@Override
	protected Map<String, Object> contentToMap(String textContent) {
		Map<String, Object> result;
		try {
			result = new ObjectMapper().readValue(textContent, HashMap.class);

			return result;
		} catch (JsonProcessingException e) {
			log.log(Level.FINEST, e.getMessage(), e);
		}
		
		return Collections.emptyMap();
	}

	@Override
	protected Boolean validateFile(String textContent) {
		return JSONUtils.isJSONValid(textContent);
	}

}
