/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.api.errors;

public class DocumentCancelled extends RuntimeException {
	
	private static final long serialVersionUID = 7187308693611119841L;

	public DocumentCancelled() {
        super();
    }

    public DocumentCancelled(final String message, final Throwable cause) {
        super(message, cause);
    }

    public DocumentCancelled(final String message) {
        super(message);
    }

    public DocumentCancelled(final Throwable cause) {
        super(cause);
    }

}
