/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.validator.parsers;

import java.util.Map;

import org.idb.cacao.api.ValidationContext;
import org.springframework.data.util.CloseableIterator;

/**
 * Object used for iterating over records associated to a particular DocumentTemplate.<BR>
 * Each record is represented as the generic form of a 'map of fields'.<BR>
 * For more information about this generic form, please see {@link ValidationContext#setParsedContents(java.util.List) setParsedContents}.
 * 
 * @author Gustavo Figueiredo
 *
 */
public interface DataIterator extends CloseableIterator<Map<String,Object>> {

}
