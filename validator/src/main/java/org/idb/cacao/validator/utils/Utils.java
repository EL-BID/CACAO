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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.idb.cacao.api.templates.DocumentFormat;
import org.mapdb.IndexTreeList;
import org.springframework.core.env.Environment;

/**
 * Define generic utilities methods to be used in project
 * 
 * @author Rivelino Patrício
 * 
 * @since 15/03/2022
 *
 */
public class Utils {
	
	private static final Logger log = Logger.getLogger(Utils.class.getName());
	
	private Utils() {
	}

	/**
	 * Provide an implementation of {@link List} to be use to store parsed data according with file type and size
	 * @param format	Format of data (file type)
	 * @param size		Size of file in bytes
	 * @return			An implementation of {@link List}. If file size is lower than size informed in 
	 * 					application.properties, a {@link LinkedList} will be returned. If greater, an instance of
	 * 					{@link IndexTreeList}
	 */
	@SuppressWarnings("unchecked")
	public static Supplier<List<Map<String, Object>>> getList(DocumentFormat format, long size, Environment env) {

		boolean limitReached = false;

		try {
			if (format.equals(DocumentFormat.CSV)) {
				String sLimitValue = env.getProperty("file.upload.memory.size.csv");
				long limitValue = Long.parseLong(sLimitValue);
				limitReached = size > limitValue;
			} else if (format.equals(DocumentFormat.DOC)) {
				String sLimitValue = env.getProperty("file.upload.memory.size.doc");
				long limitValue = Long.parseLong(sLimitValue);
				limitReached = size > limitValue;
			} else if (format.equals(DocumentFormat.JSON)) {
				String sLimitValue = env.getProperty("file.upload.memory.size.json");
				long limitValue = Long.parseLong(sLimitValue);
				limitReached = size > limitValue;
			} else if (format.equals(DocumentFormat.PDF)) {
				String sLimitValue = env.getProperty("file.upload.memory.size.pdf");
				long limitValue = Long.parseLong(sLimitValue);
				limitReached = size > limitValue;
			} else if (format.equals(DocumentFormat.XLS)) {
				String sLimitValue = env.getProperty("file.upload.memory.size.xls");
				long limitValue = Long.parseLong(sLimitValue);
				limitReached = size > limitValue;
			} else if (format.equals(DocumentFormat.XML)) {
				String sLimitValue = env.getProperty("file.upload.memory.size.xml");
				long limitValue = Long.parseLong(sLimitValue);
				limitReached = size > limitValue;
			}
		} catch (Exception e) {
			log.log(Level.INFO, e.getMessage(), e);
		}

		if (!limitReached)
			return LinkedList::new;
		
		return () -> (List<Map<String, Object>>) MapDBFactory.newGenericList(format.name());

	}

}
