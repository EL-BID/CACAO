/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for any 'synchronizable' repository
 * 
 * @author Gustavo Figueiredo
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Synchronizable {

	/**
	 * Must inform the name of the entity field where we store the 'timestamp' of the last modification ou creation
	 * of objects. The field itself should be of type 'java.util.Date' or type 'java.time.OffsetDateTime'
	 * 
	 * IMPORTANT: you must make sure that all operations in the application updates this field during 'save's
	 */
	String timestamp();
	
	/**
	 * Must inform the name of the entity field where we store the unique 'id' of each instance. By default consider 
	 * fields with name 'id'.
	 */
	String id() default "id";
	
	/**
	 * May inform name of fields that should not be included in the sync copy
	 */
	String[] dontSync() default {};
	
	/**
	 * May inform name of fields that should be considered as 'unique constraints' in the sync copy. Avoid to recreate ambiguous
	 * records with different ID's but the same values for these fields. Do not prevent updating these records.
	 */
	String[] uniqueConstraint() default {};
}
