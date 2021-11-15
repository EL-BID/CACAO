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

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

import org.idb.cacao.api.templates.DocumentInput;
import org.idb.cacao.api.templates.DocumentInputFieldMapping;

public class CSVParser implements FileParser {
	
	private Path path;
	
	private DocumentInput documentInputSpec;
	
	private Scanner scanner;
	
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
			scanner = new Scanner(path.toFile());
			//Skips first line
			scanner.nextLine();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
						String[] parts = line.split(";");
						
						Map<String,Object> toRet = new LinkedHashMap<>();
						
						for ( DocumentInputFieldMapping fieldMapping : documentInputSpec.getFields() ) {
							
							String value = parts.length > fieldMapping.getColumnIndex() ? parts[fieldMapping.getColumnIndex()] : null; 
							toRet.put(fieldMapping.getFieldName(), value);
							
						}
						
						return toRet;
						
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
			// TODO Auto-generated catch block
			e.printStackTrace();			
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

}
