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
