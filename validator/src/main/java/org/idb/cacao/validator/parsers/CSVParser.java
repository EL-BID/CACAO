/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.validator.parsers;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.input.BOMInputStream;

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
public class CSVParser extends FileParserAdapter {
	
	private static final Logger log = Logger.getLogger(CSVParser.class.getName());
	
	private int currentLine = 1;
	
	/**
	 * Scanner to iterate over file lines
	 */
	private Scanner scanner;
	
	/**
	 * File input stream (must be close at the end)
	 */
	private FileInputStream fis;
		
	/**
	 * A parser to handle CSV lines
	 */
	private CsvParser baseCSVParser;
	
	private TabulatedData tab;

	@Override
	public void start() {
		if ( path == null || !path.toFile().exists() ) {		
			return;			
		}			
		
		if ( scanner != null ) {
			try {
				scanner.close();
			} catch (Exception e) {
				log.log(Level.INFO,"Scanner close error", e);
			}
			scanner = null;
		}
		
		if ( fis != null ) {
			try {
				fis.close();
			} catch (Exception e) {
				log.log(Level.INFO,"File input stream close error", e);
			}			
			fis = null;
		}
		
		try {
			fis = new FileInputStream(path.toFile());
		} catch (IOException e) {
			log.log(Level.SEVERE, String.format("Error trying to read file %s", path.getFileName()), e);
			close();
			return;
		}

		try {
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
			currentLine = 1;
			
		} catch (IOException e) {
			log.log(Level.SEVERE, String.format("Error trying to read file %s", path.getFileName()), e);
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
					
					if(!hasNext()){
						throw new NoSuchElementException();
					}
					
					String line = scanner.nextLine();
					
					if ( line != null ) {					
						
						String[] parts = readLine(line);
						
						Map<String,Object> toRet = tab.parseLine(parts);
						toRet.put(CURRENT_LINE, currentLine++);
						return toRet;
					}
					return Collections.emptyMap();
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
			log.log(Level.SEVERE, String.format("Error trying to iterate data from file %s", path.getFileName()), e);			
		}
		
		return null;
	}

	@Override
	public void close() {
		if ( scanner != null ) {
			try {
				scanner.close();
			} catch (Exception e) {
				log.log(Level.INFO,"Scanner close error", e);
			}
			scanner = null;
		}
		if ( fis != null ) {
			try {
				fis.close();
			} catch (Exception e) {
				log.log(Level.INFO,"File input stream close error", e);
			}			
			fis = null;
		}
	}
	
	/**
	 * Parse a CSV line and returns an array of String with line values
	 * @param line	Line to be parsed
	 * @return	An array of String with line values
	 */
	private String[] readLine(String line) {
		
		if ( baseCSVParser == null ) {
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
			baseCSVParser = new CsvParser(settings);
		}
		
		return baseCSVParser.parseLine(line);
	}

}
