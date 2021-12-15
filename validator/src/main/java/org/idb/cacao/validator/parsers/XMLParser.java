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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.input.BOMInputStream;
import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.api.templates.DocumentInput;
import org.idb.cacao.validator.fileformats.FileFormat;
import org.idb.cacao.validator.fileformats.FileFormatFactory;

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
public class XMLParser implements FileParser {

	private static final Logger log = Logger.getLogger(CSVParser.class.getName());

	private Path path;

	private Scanner scanner;

	private DocumentInput documentInputSpec;

	private Iterator<Map<String, Object>> entries;

	private CSVParser csvParser;

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

	private String convertXmlToCsv(List<Map<String, Object>> records) {
		String csvStr = "";
		Iterator<Map<String, Object>> iterator = records.iterator();

		String columns = null;

		while(iterator.hasNext()) {
			Map<String, Object> entry = iterator.next();

			if (columns == null) {
				columns = entry.keySet().toString();

				if (columns != null) {
					columns = columns.substring(1, columns.length() - 1);
				}
		
				csvStr += columns + "\n";
			}

			String values = entry.values().toString();
			values = values.substring(1, values.length() - 1);
			csvStr += values + "\n";
		}

		System.out.print(csvStr);

		return csvStr;
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
			//Skips BOM if it exists
			BOMInputStream bis = new BOMInputStream(fis);
			String charset = bis.getBOMCharsetName();
			scanner = (charset==null) ? new Scanner(bis) : new Scanner(bis, charset);
			
			//Read first line and set field positions according with field mapping atributtes
			String xmlText = "";

			while (scanner.hasNextLine()) {
				xmlText += scanner.nextLine();
			}
			
			scanner.close();

			final Map<String,Object> result =
					new ObjectMapper().readValue(xmlText, HashMap.class);
		
			System.out.println(result);

			Object mainKey = result.keySet().toArray()[0];
			
			List<Map<String, Object>> records = (List<Map<String, Object>>) result.get(mainKey);
			entries = records.iterator();

			String convertedCsv = this.convertXmlToCsv(records);

			// [TODO] Adapt parsers to receive a string instead of file. For now, let's create a csv file and 
			// pass to the csv parser
			String jsonPath = this.getPath().toString();
			Path csvPath = Paths.get(jsonPath + ".csv");
			
			FileOutputStream outputStream = new FileOutputStream(csvPath.toFile());
			outputStream.write(convertedCsv.getBytes("UTF-8"));

			FileFormat fileFormat = FileFormatFactory.getFileFormat(DocumentFormat.CSV);
			csvParser = (CSVParser) fileFormat.createFileParser();

			// Initializes the CSVParser for processing
			csvParser.setPath(csvPath);
			csvParser.setDocumentInputSpec(this.getDocumentInputSpec());

			// Let's start parsing the file contents
			csvParser.start();

		} catch (IOException e) {
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
			return csvParser.iterator();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

}
