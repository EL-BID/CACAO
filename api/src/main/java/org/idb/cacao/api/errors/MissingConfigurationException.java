/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.api.errors;

/**
 * Exception related to some required configuration missing (i.e. something that was expected
 * by the System Administrator or Tax Authority).
 * 
 * @author Gustavo Figueiredo
 *
 */
public class MissingConfigurationException extends RuntimeException {

	private static final long serialVersionUID = 1890358289841844271L;

	public MissingConfigurationException() {
        super();
    }

    public MissingConfigurationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public MissingConfigurationException(final String message) {
        super(message);
    }

    public MissingConfigurationException(final Throwable cause) {
        super(cause);
    }

}
