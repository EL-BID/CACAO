/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.api;

/**
 * This class is intended to enumerate all possible 'views' for use in User Interfaces. Each
 * entity will refer to these 'views' by using some 'annotations' over its 'fields'.<BR>
 * The controllers at the back-end will choose the fields of interest given one of these
 * 'views'.<BR> 
 * Doing this way, the same entity object may project different 'views' depending on the
 * context.
 */
public class Views {
	
	/**
	 * View representing basic fields for selection purposes
	 */
	public class Selection {}
	
	/**
	 * View representing 'any context'
	 */
	public class Public {}
	
	/**
	 * View representing 'contexts of interest of declarants'
	 */
	public class Declarant extends Public {}
	
	/**
	 * View representing 'contexts of interest of tax authorities'
	 */
	public class Authority extends Declarant {} 
}
