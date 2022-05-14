/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

public class GenericResponse {

	   private String message;
	    private String error;

	    public GenericResponse(final String message) {
	        super();
	        this.message = message;
	    }

	    public GenericResponse(final String message, final String error) {
	        super();
	        this.message = message;
	        this.error = error;
	    }

	    public GenericResponse(List<ObjectError> allErrors, String error) {
	        this.error = error;
	        String temp = allErrors.stream().map(e -> {
	            if (e instanceof FieldError) {
	                return "{\"field\":\"" + ((FieldError) e).getField() + "\",\"defaultMessage\":\"" + e.getDefaultMessage() + "\"}";
	            } else {
	                return "{\"object\":\"" + e.getObjectName() + "\",\"defaultMessage\":\"" + e.getDefaultMessage() + "\"}";
	            }
	        }).collect(Collectors.joining(","));
	        this.message = "[" + temp + "]";
	    }

	    public String getMessage() {
	        return message;
	    }

	    public void setMessage(final String message) {
	        this.message = message;
	    }

	    public String getError() {
	        return error;
	    }

	    public void setError(final String error) {
	        this.error = error;
	    }
}
