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
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.input.BOMInputStream;
import org.idb.cacao.api.templates.DocumentInput;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

/**
 * Implements {@link FileParser} interface to parse CSV files. <br>
 * <br>
 * IMPORTANT: CSV file must use comma ",", semicolon ";", pipe "|" or TAB '\t' as delimiter. <br> 
 * 			  None of these characters can be used in field values, except if column value is delimited by quotes (""). <br>
 *			  Fields can be enclosed in double quotes: aa,bb,"cc,cd",dd. <br>
 *			  Lines must be separated br CR LF ("\r\n"). <br>
 *			  Empty lines are accepted. <br>
 *  
 * @author Rivelino Patrício
 * 
 * @since 15/11/2021
 *
 */
public class CSVParser implements FileParser {
	
	private static final Logger log = Logger.getLogger(CSVParser.class.getName());
	
	/**
	 * Path for file in system storage
	 */
	private Path path;
	
	/**
	 * Document with field specifications
	 */
	private DocumentInput documentInputSpec;
	
	/**
	 * Scanner to iterate over file lines
	 */
	private Scanner scanner;
		
	/**
	 * A parser to handle CSV lines
	 */
	private CsvParser csvParser;
	
	private TabulatedData tab;
	
	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.parsers.FileParser#getPath()
	 */
	@Override
	public Path getPath() {
		return path;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.parsers.FileParser#setPath(java.nio.file.Path)
	 */
	@Override
	public void setPath(Path path) {
		this.path = path;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.parsers.FileParser#getDocumentInputSpec()
	 */
	@Override
	public DocumentInput getDocumentInputSpec() {
		return documentInputSpec;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.parsers.FileParser#setDocumentInputSpec(org.idb.cacao.api.templates.DocumentInput)
	 */
	@Override
	public void setDocumentInputSpec(DocumentInput inputSpec) {
		this.documentInputSpec = inputSpec;
	}

	@Override
	public void start() {
		if ( path == null || !path.toFile().exists() ) {		
			return;			
		}			
		
		if ( scanner != null ) {
			try {
				scanner.close();
			} catch (Exception e) {
			}
		}
		
		try {
			FileInputStream fis = new FileInputStream(path.toFile());
			
			BOMInputStream bis = new BOMInputStream(fis);
			String charset = bis.getBOMCharsetName();

			//Skips BOM if it exists
			scanner = (charset==null) ? new Scanner(bis) : new Scanner(bis, charset);
			
			tab = new TabulatedData(documentInputSpec);
			
			//Read first line and set field positions according with field mapping atributtes
			String firstLine = scanner.nextLine();
			if ( firstLine != null && !firstLine.isEmpty() ) {
				
				String[] parts = readLine(firstLine);
			
				tab.parseColumnNames(parts);
			}
			
		} catch (IOException e) {
			log.log(Level.SEVERE, "Error trying to read file " + path.getFileName(), e);
		}	
		
	}	
	
	@Override
	public DataIterator iterator() {
		
		if ( path == null || !path.toFile().exists() ) {		
			return null;			
		}			
		
		if ( scanner == null ) {
			start();
		}
		
		if ( scanner == null )
			return null;
		
		try {			
			
			return new DataIterator() {
				
				@Override
				public Map<String, Object> next() {
					String line = scanner.nextLine();
					
					if ( line != null ) {					
						
						String[] parts = readLine(line);
						
						return tab.parseLine(parts);
					}
					return null;
				}
				
				@Override
				public boolean hasNext() {					
					return scanner.hasNextLine();
				}
				
				@Override
				public void close() {
					scanner.close();					
				}
			}; 
			
		} catch (Exception e) {
			log.log(Level.SEVERE, "Error trying to iterate data from file " + path.getFileName(), e);			
		}
		
		return null;
	}

	@Override
	public void close() {
		if ( scanner != null ) {
			try {
				scanner.close();
			} catch (Exception e) {
			}
		}
	}
	
	/**
	 * Parse a CSV line and returns an array of String with line values
	 * @param line	Line to be parsed
	 * @return	An array of String with line values
	 */
	private String[] readLine(String line) {
		
		if ( csvParser == null ) {
			// creates a CSV parser
			CsvParserSettings settings = new CsvParserSettings();
			settings.detectFormatAutomatically();
			settings.setDelimiterDetectionEnabled(true);
			settings.setSkipEmptyLines(true);
			settings.setIgnoreLeadingWhitespaces(true);
			settings.setIgnoreTrailingWhitespaces(true);
			settings.setLineSeparatorDetectionEnabled(true);
			settings.setNormalizeLineEndingsWithinQuotes(true);
			settings.setQuoteDetectionEnabled(true);
			settings.setHeaderExtractionEnabled(false);
			settings.trimValues(true);
			settings.getFormat().setNormalizedNewline('\n');
			settings.setMaxColumns(1000);
			csvParser = new CsvParser(settings);
		}
		
		return csvParser.parseLine(line);
	}

}
