/*******************************************************************************
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in the Software without 
 * restriction, including without limitation the rights to use, copy, modify, merge, publish, 
 * distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or 
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS 
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR 
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN 
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
