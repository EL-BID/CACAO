/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.api;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

@Documented
@Retention(RUNTIME)
@Target(FIELD)
/**
 * Basic definitions for {@link Field} details when they are used in GUI presentations.
 * 
 * @author Rivelino Patrício
 *
 * @since 29/10/2021
 */
public @interface AFieldDescriptor {

	/**
	 * External name to be displayed when the {@link Field} is displayed in HTML form or table.
	 */
	String externalName();

	/**
	 * Width in pixels when the {@link Field} is displayed in forms or tables. <br>
	 * If it's equals to 0 (default), system will user a default width for the {@link Field}. <br>
	 */
	int width() default 0;

	/**
	 * Alignment used do display the {@link Field}. Available options:<BR>
	 * -1: default alignment (numbers aligned to the right and other fields to the left).<BR>
	 * JLabel.RIGHT: alignment to the right<BR>
	 * JLabel.LEFT: alignment to the left<BR>
	 * JLabel.CENTER: alignment to center<BR>
	 */
	int alignment() default -1;

	/**
	 * If true, the {@link Field} value can be editable. If false, the {@link Field} is not editable.
	 */
	boolean editable() default true;
	
	/**
	 * A help for the {@link Field} that can be displayed at GUI.>
	 */
	String tooltip() default "";
	
	/**
	 * If true, this field will be collected as additional 'parameter' at {@link AuditTrail} objects
	 */
	boolean audit() default false;
	
}

