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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DB.IndexTreeListMaker;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.IndexTreeList;
import org.mapdb.Serializer;
import org.mapdb.SortedTableMap;

/**
 * A factory for {@link DB} objects
 * 
 * @author Rivelino Patrício
 * 
 * @since 16/03/2022
 *
 */
public class MapDBFactory {
	
	private static final Logger log = Logger.getLogger(MapDBFactory.class.getName());
	
	/**
	 * Hide public constructor
	 */
	private MapDBFactory() {		
	}

	
	/**
	 * 
	 * @return Singleton {@link DB} instance
	 */
	private static synchronized DB newInstance() {		
		return DBMaker.tempFileDB()
				.closeOnJvmShutdown()
				.fileLockDisable()
				.fileMmapEnableIfSupported()
				.make();
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
		IndexTreeListMaker<?> listMaker = newInstance().indexTreeList(prefix + "_" + 
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
		IndexTreeListMaker<?> listMaker = newInstance().indexTreeList(String.valueOf(System.currentTimeMillis()),Serializer.JAVA);
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
		IndexTreeListMaker<String> listMaker = newInstance().indexTreeList(String.valueOf(System.currentTimeMillis()),Serializer.STRING);
		return listMaker.create();			
	}
	
	/**
	 * Close a database referenced by a list
	 * 
	 * @param list	List to close database
	 */
	public static void close(List<?> list) {
		if ( !(list instanceof IndexTreeList) )
			return;		
		try {
			((IndexTreeList<?>)list).getStore().close();
		} catch (Exception e) {
			log.log(Level.INFO, e.getMessage(), e);
		}
	}
	
	/**
	 * Close a database referenced by a list
	 * 
	 * @param list	List to close database
	 */
	public static void close(Map<?,?> map) {
		try {
			if ( (map instanceof BTreeMap) )					
				((BTreeMap<?,?>)map).close();
			else if ( (map instanceof HTreeMap) )					
				((HTreeMap<?,?>)map).close();
			else if ( (map instanceof SortedTableMap) )					
				((SortedTableMap<?,?>)map).close();
		} catch (Exception e) {
			log.log(Level.INFO, e.getMessage(), e);
		}
	}	

	/**
	 * Clear unused temporary files from previous executions
	 */
	public static void clearTemporaryFiles() {
		
		File tempDir = new File(System.getProperty("java.io.tmpdir"));
		
		if ( tempDir.exists() && tempDir.isDirectory() ) {
			
			Pattern pFilename = Pattern.compile("^mapdb(\\d{1,50})temp", Pattern.CASE_INSENSITIVE);
			
			String[] filenames = tempDir.list((dir,name)->pFilename.matcher(name).matches());
			
			if ( filenames.length > 0 ) {				
				for ( String filename : filenames ) {					
					try {
						Files.delete(Paths.get(new File(tempDir, filename).toURI()));						
					} catch (Exception e) {
						log.log(Level.INFO, String.format("Couldn't delete temporary file %s", filename), e);
					}					
				}				 
			}			
		}
				
		
	}		

}
