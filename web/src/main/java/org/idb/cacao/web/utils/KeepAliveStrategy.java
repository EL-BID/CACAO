/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.utils;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

/**
 * This object may be used as a Keep-Alive strategy for configuring a HttpClient. There is a default
 * timeout in milliseconds configured at construction, that may be overridden by a header called 'timeout' with the timeout
 * in seconds.
 * 
 */
public class KeepAliveStrategy implements ConnectionKeepAliveStrategy {

	private static final String KEEP_ALIVE_TIMEOUT_PARAM_NAME = "timeout";
	
	private final long defaultTimeout;
	
	public KeepAliveStrategy(long defaultTimeout) {
		this.defaultTimeout = defaultTimeout;
	}
	
	@Override
	public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
		HeaderElementIterator it = new BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE));
		while (it.hasNext()) {
			HeaderElement he = it.nextElement();
			String param = he.getName();
			String value = he.getValue();
			if (value != null && param.equalsIgnoreCase(KEEP_ALIVE_TIMEOUT_PARAM_NAME)) {
				return Long.parseLong(value) * 1000;
			}
		}
		return defaultTimeout;
	}

}
