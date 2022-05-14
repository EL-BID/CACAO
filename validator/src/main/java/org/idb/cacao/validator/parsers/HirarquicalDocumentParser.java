/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.validator.parsers;

import java.io.FileInputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.input.BOMInputStream;
import org.idb.cacao.api.errors.InvalidFileException;


/**
 * Absctract class for hierarquical document parsers.
 * 
 * Examples of such documents are XML and JSON.
 * 
 * Implements {@link FileParserAdapter} interface to parse hierarquical files. <br>
 * <br>
 * 
 * This parser take advantage of {@link CSVParser} by converting the hierarquical documents using the csv parser.
 *  
 * @author Leon Silva
 * 
 * @since 05/03/2022
 *
 */
public abstract class HirarquicalDocumentParser extends FileParserAdapter {
	private static final Logger log = Logger.getLogger(HirarquicalDocumentParser.class.getName());

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

			StringBuilder contentText = new StringBuilder();

			try (Scanner scanner = (charset==null) ? new Scanner(bis) : new Scanner(bis, charset);) {

				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					if (line.trim().length()==0)
						continue;
					contentText.append(line);
				}
			
			}

			String text = contentText.toString();

            if (Boolean.FALSE.equals(validateFile(text))) {
				throw new InvalidFileException("Invalid file");
			}

			Map<String, Object> result = this.contentToMap(text);
		
			ReflexiveConverterToTable flattener = new ReflexiveConverterToTable();
			flattener.parse(result);
			List<Object[]> flattenedTable = flattener.getTable();
			List<String> titles = flattener.getTitles();
		
			tab = new TabulatedData(documentInputSpec);

			entries = flattenedTable.iterator();
			tab.parseColumnNames(titles.toArray());

		} catch (Exception e) {
			log.log(Level.SEVERE, String.format("Error trying to read file %s", path.getFileName()), e);
		}
	}

    protected abstract Map<String, Object> contentToMap(String textContent);

    protected abstract Boolean validateFile(String textContent);

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
					return Collections.emptyMap();
				}
				
				@Override
				public boolean hasNext() {					
					return entries.hasNext();
				}
				
				@Override
				public void close() {
					entries = null;
				}
			}; 
			
		} catch (Exception e) {
			log.log(Level.SEVERE, String.format("Error trying to iterate data from file %s", path.getFileName()), e);			
		}
		
		return null;
	}

	@Override
	public void close() {		
		entries = null;		
	}

}