/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.errors;

/**
 * Exception generated for warning about some feature that has been disabled because the current
 * application is running in PRESENTATION MODE
 *
 */
public class PresentationDisabledFeature extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PresentationDisabledFeature() {
        super();
    }

    public PresentationDisabledFeature(final String message, final Throwable cause) {
        super(message, cause);
    }

    public PresentationDisabledFeature(final String message) {
        super(message);
    }

    public PresentationDisabledFeature(final Throwable cause) {
        super(cause);
    }

}
