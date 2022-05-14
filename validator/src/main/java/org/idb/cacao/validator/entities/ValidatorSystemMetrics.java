/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.validator.entities;

import org.idb.cacao.api.SystemMetrics;
import org.springframework.data.elasticsearch.annotations.Document;

/**
 * Metrics collected from the running system and provided by ResourceMonitorService
 * @author Gustavo Figueiredo
 *
 */
@Document(indexName="validatormetrics")
public class ValidatorSystemMetrics extends SystemMetrics {

	private static final long serialVersionUID = 1L;

}
