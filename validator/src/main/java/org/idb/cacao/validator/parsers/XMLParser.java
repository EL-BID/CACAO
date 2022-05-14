/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.validator.parsers;

import java.util.Map;
import com.github.underscore.U;

/**
 * Implements {@link HirarquicalDocumentParser} interface to parse XML files. <br>
 * <br>
 * 
 *  
 * @author Leon Silva
 * 
 * @since 15/11/2021
 *
 */
public class XMLParser extends HirarquicalDocumentParser {

	@Override
	protected Map<String, Object> contentToMap(String textContent) {
		return U.fromXmlMap(textContent);
	}

	@Override
	protected Boolean validateFile(String textContent) {
		return true;
	}

}