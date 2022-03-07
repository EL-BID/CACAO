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
package org.idb.cacao.validator.parsers;

import java.io.FileInputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.input.BOMInputStream;

import com.github.underscore.U;


/**
 * Implements {@link FileParser} interface to parse XML files. <br>
 * <br>
 * 
 * This parser take advantage of {@link CSVParser} by converting the xml file into a csv file and using the csv parser.
 *  
 * @author Leon Silva
 * 
 * @since 15/11/2021
 *
 */
public class XMLParser extends FileParserAdapter {

	private static final Logger log = Logger.getLogger(JSONParser.class.getName());

	private Iterator<Object[]> entries;

	private TabulatedData tab;

	@Override
	public void start() {
		if ( path == null || !path.toFile().exists() ) {
			return;
		}

		try (FileInputStream fis = new FileInputStream(path.toFile());
				//Skips BOM if it exists
				BOMInputStream bis = new BOMInputStream(fis);) {

			String charset = bis.getBOMCharsetName();

			StringBuilder xmlText = new StringBuilder();

			try (Scanner scanner = (charset==null) ? new Scanner(bis) : new Scanner(bis, charset);) {

				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					if (line.trim().length()==0)
						continue;
						xmlText.append(line);
				}
			
			}

			String xml = xmlText.toString();

			Map<String, Object> result = U.fromXmlMap(xml);
		
			ReflexiveConverterToTable flattener = new ReflexiveConverterToTable();
			flattener.parse(result);
			List<Object[]> flattenedTable = flattener.getTable();
			List<String> titles = flattener.getTitles();
		
			tab = new TabulatedData(documentInputSpec);

			entries = flattenedTable.iterator();
			tab.parseColumnNames(titles.toArray());

		} catch (Exception e) {
			log.log(Level.SEVERE, "Error trying to read file " + path.getFileName(), e);
		}
	}

	@Override
	public DataIterator iterator() {
		if ( path == null || !path.toFile().exists() ) {
			return null;
		}

		if ( entries == null ) {
			start();
		}

		if ( entries == null )
			return null;

		try {			
			
			return new DataIterator() {
				
				@Override
				public Map<String, Object> next() {
					Object[] parts = entries.next();
					
					if ( parts != null ) {					
						
						return tab.parseLine(parts);
					}
					return null;
				}
				
				@Override
				public boolean hasNext() {					
					return entries.hasNext();
				}
				
				@Override
				public void close() {
									
				}
			}; 
			
		} catch (Exception e) {
			log.log(Level.SEVERE, "Error trying to iterate data from file " + path.getFileName(), e);			
		}
		
		return null;
	}

	@Override
	public void close() {
		
		entries = null;
		
	}

}
