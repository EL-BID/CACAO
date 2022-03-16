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
package org.idb.cacao.validator.utils;

import java.util.List;

import org.mapdb.DB;
import org.mapdb.DB.IndexTreeListMaker;
import org.mapdb.DBMaker;
import org.mapdb.IndexTreeList;
import org.mapdb.Serializer;

/**
 * A factory for {@link DB} objects
 * 
 * @author Rivelino Patrício
 * 
 * @since 16/03/2022
 *
 */
public class MapDBFactory {
	
	/**
	 * Singleton {@link DB} instance
	 */
	private static DB db;
	
	/**
	 * Hide public constructor
	 */
	private MapDBFactory() {		
	}
	
	/**
	 * 
	 * @return Singleton {@link DB} instance
	 */
	private static synchronized DB getDBInstance() {		
		if ( db == null )
			db = DBMaker.tempFileDB().make();
		
		return db;
	}
	
	/**
	 * Make a new {@link List}
	 * 
	 * @param prefix	List prefix name
	 * 
	 * @return	A new implementation {@link List} backed on disk storage
	 */
	@SuppressWarnings("unchecked")
	public static IndexTreeList<?> newGenericList(String prefix) {		
		IndexTreeListMaker<?> listMaker = getDBInstance().indexTreeList(prefix + "_" + 
				System.currentTimeMillis(),Serializer.JAVA);
		return listMaker.create();			
	}
	
	/**
	 * Make a new {@link List}
	 * 
	 * @param prefix	List prefix name
	 * 
	 * @return	A new implementation {@link List} backed on disk storage
	 */
	@SuppressWarnings("unchecked")
	public static IndexTreeList<?> newGenericList() {		
		IndexTreeListMaker<?> listMaker = getDBInstance().indexTreeList(String.valueOf(System.currentTimeMillis()),Serializer.JAVA);
		return listMaker.create();			
	}	
	
	/**
	 * Make a new {@link List}
	 * 
	 * @param prefix	List prefix name
	 * 
	 * @return	A new implementation {@link List} backed on disk storage
	 */	
	public static IndexTreeList<String> newStringList() {		
		IndexTreeListMaker<String> listMaker = getDBInstance().indexTreeList(String.valueOf(System.currentTimeMillis()),Serializer.STRING);
		return listMaker.create();			
	}		

}
