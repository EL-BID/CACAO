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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.io.input.BOMInputStream;
import org.idb.cacao.api.errors.InvalidFileException;
import org.idb.cacao.api.templates.DocumentInput;
import org.idb.cacao.api.templates.DocumentInputFieldMapping;
import org.idb.cacao.validator.utils.JSONUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JSONParser implements FileParser {

	private Path path;

	private DocumentInput documentInputSpec;

	private Scanner scanner;
	
	private Iterator<Map<String, Object>> entries;

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
			//Skips BOM if it exists
			BOMInputStream bis = new BOMInputStream(fis);
			String charset = bis.getBOMCharsetName();
			scanner = (charset==null) ? new Scanner(bis) : new Scanner(bis, charset);
			
			String jsonText = "";

			while (scanner.hasNextLine()) {
				jsonText += scanner.nextLine();
			}
			
			scanner.close();

			if (!JSONUtils.isJSONValid(jsonText)) {
				throw new InvalidFileException("Invalid JSON file");
			}

			final Map<String,Object> result =
					new ObjectMapper().readValue(jsonText, HashMap.class);
		
			System.out.println(result);

			Object mainKey = result.keySet().toArray()[0];
			
			List<Map<String, Object>> records = (List<Map<String, Object>>) result.get(mainKey);
			entries = records.iterator();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
					
					return entries.next();
				}

				@Override
				public boolean hasNext() {
					return entries.hasNext();
				}

				@Override
				public void close() {
					scanner.close();
				}
			};

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
