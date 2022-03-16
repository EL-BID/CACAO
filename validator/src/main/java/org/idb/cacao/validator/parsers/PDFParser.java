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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.pdmodel.interactive.form.PDXFAResource;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.templates.DocumentInputFieldMapping;
import org.idb.cacao.api.utils.ParserUtils;
import org.idb.cacao.validator.utils.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Parser for a PDF Document. PDF must use PDF Forms.
 * <br>
 * IMPORTANT:	PDF files must implement PDF Forms standards.<br> 
 * 				PDF Form fields cannot use "data binding" or "default value" properties.<br>
 * 				PDF FORM fields cannot be "ListBox" or "ComboBox" types.<br>
 *
 * @author Rivelino Patrício
 * 
 * @since 26/11/2021
 *
 */
public class PDFParser extends FileParserAdapter {

	private static final Logger log = Logger.getLogger(PDFParser.class.getName());

	private Map<String, List<Object>> allFieldsValues;
	
	/**
	 * Field positions relative to column positions
	 */
	private Map<String,String> fieldComlunKeys;	

	@Override
	public void start() {
		
		if ( path == null || !path.toFile().exists() ) {		
			return;			
		}			
		
		if ( allFieldsValues != null ) {
			allFieldsValues.clear();
			allFieldsValues = null;
		}
		
		try {
		
			allFieldsValues = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			
			try (FileInputStream stream = new FileInputStream(path.toFile())) {
				try (PDDocument document = PDDocument.load(stream);) {
					processForms(document, /*includeAllNamedObject*/false);
				} catch (Exception e) {
					log.log(Level.SEVERE, String.format("Error trying to read file %s", path.getFileName()), e);
				}
			}
			
			fieldComlunKeys = new HashMap<>();
			
			//Check all field mappings and set it's corresponding column
			for ( DocumentInputFieldMapping fieldMapping : documentInputSpec.getFields() ) {
				
				String fieldName = fieldMapping.getFieldName();
				if ( fieldName != null && !fieldName.isEmpty() ) {							
					Object key = ValidationContext.matchExpression(allFieldsValues.entrySet(), Map.Entry::getKey, fieldName).map(Map.Entry::getKey).orElse(null);
					if ( key != null ) {
						fieldComlunKeys.put(fieldMapping.getFieldName(), key.toString());
					}
					else {
						String expression = fieldMapping.getColumnNameExpression();
						if ( expression != null && !expression.isEmpty() ) {							
							key = ValidationContext.matchExpression(allFieldsValues.entrySet(), Map.Entry::getKey, expression).map(Map.Entry::getKey).orElse(null);
							if ( key != null )
								fieldComlunKeys.put(fieldMapping.getFieldName(), key.toString());
						}		
					}
				} 
				
			}			
			
		}
		catch (IOException e) {
			log.log(Level.SEVERE, String.format("Error trying to read file %s", path.getFileName()), e);
		}	
	}

	@Override
	public DataIterator iterator() {
		if ( path == null || !path.toFile().exists() ) {		
			return null;			
		}			
		
		if ( allFieldsValues == null ) {
			start();
		}
		
		if ( allFieldsValues == null )
			return null;
		
		try {
			
			final PDFParser parser = this;
			final int size = allFieldsValues.isEmpty() ? 0 : 
				allFieldsValues.values().stream().map(List::size).sorted(Comparator.reverseOrder()).findFirst().orElse(0);							
			
			return new DataIterator() {
				
				int atual = 0;
				
				@Override
				public Map<String, Object> next() {
					if(!hasNext()){
						throw new NoSuchElementException();
					}					
					return getNext(atual++);
				}
				
				@Override
				public boolean hasNext() {					
					return atual < size;
				}
				
				@Override
				public void close() {
					parser.close();	
				}
			}; 
			
		} catch (Exception e) {
			log.log(Level.SEVERE, String.format("Error trying to iterate data from file %s", path.getFileName()), e);			
		}
		
		return null;
	}

	/**
	 * Get record identified by index param.
	 * 
	 * @param index	Index of record to be returned
	 * @return	A record with all fiedls at index position.
	 */
	protected Map<String, Object> getNext(int index) {
		
		Map<String, Object> dataItem = new HashMap<>();
		
		for ( DocumentInputFieldMapping fieldMapping : documentInputSpec.getFields() ) {
			
			String key =  fieldComlunKeys.getOrDefault(fieldMapping.getFieldName(), null);
			String fieldName = fieldMapping.getFieldName();
			
			if ( key == null ) {
				dataItem.put(fieldName, null);
			}
			else {
				List<Object> fieldValues = allFieldsValues.get(key); 
				
				if ( fieldValues == null || fieldValues.isEmpty() ) {
					dataItem.put(fieldName, null);	
				}
				else {
				
					if ( index <= (fieldValues.size() -1) ) {
						dataItem.put(fieldName, fieldValues.get(index));	
					}
					else if ( index > 0 && fieldValues.size() == 1 ) {
						dataItem.put(fieldName, fieldValues.get(0));
					}
					else {
						dataItem.put(fieldName, null);	
					}
					
				}
			}
			
		}
		
		return dataItem;
	}

	@Override
	public void close() {
		if ( allFieldsValues != null ) {
			allFieldsValues.clear();
			allFieldsValues = null;
		}
	}	

	/**
	 * Process internal form structures
	 * @param document
	 * @param includeAllNamedObject If set to TRUE, will include in 'fieldValues' all objects with 'name' property, including those ones without values
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws TransformerException 
	 */
	protected void processForms(PDDocument document, boolean includeAllNamedObject) throws ParserConfigurationException, SAXException, IOException, TransformerException {
		
		// O arquivo PDF pode trabalhar com diferentes estruturas de formulário.
		
		// Daremos preferência à estrutura 'XFA', pois ela possibilita mais recursos.
		// Caso não esteja disponível, procuramos por estruturas mais simples no tipo 'AcroForm'
		
		PDAcroForm pdAcroForm = document.getDocumentCatalog().getAcroForm();
		if (pdAcroForm!=null) {
			PDXFAResource pdfxfa = pdAcroForm.getXFA();
			if (pdfxfa!=null) {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setNamespaceAware(true);
				factory.setValidating(false);
				//completely disable external entities declarations
				factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
				factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
				DocumentBuilder builder = factory.newDocumentBuilder();
				try (InputStream in = new BufferedInputStream(new ByteArrayInputStream(pdfxfa.getBytes()))) {
					Document root = builder.parse(in);
					processXFAForm(root.getDocumentElement(), includeAllNamedObject, allFieldsValues, /*prefix_field_name*/null);
				}
			}
			else {
				List<PDField> fields = pdAcroForm.getFields();
				if (fields!=null && !fields.isEmpty()) { 
					for (PDField field:fields) {
						allFieldsValues.compute(field.getMappingName(), (k,v)-> v == null ? new LinkedList<>() : v).add(field.getValueAsString());
					}
				}
			}
		}
		try {
			List<PDSignatureField> sfields = document.getSignatureFields();
			if (sfields!=null && !sfields.isEmpty()) {
				for (PDSignatureField sfield:sfields) {					
					allFieldsValues.compute(sfield.getMappingName(), (k,v)-> v == null ? new LinkedList<>() : v).add(sfield.getValueAsString());
				}
			}
		} catch (IOException e) {
			log.log(Level.WARNING, "Error while parsing document", e);
		}
	}
	
	/**
	 * Process internal XFA form structures using recursive calls
	 * @param includeAllNamedObject If set to TRUE, will include in 'fieldValues' all objects with 'name' property, including those ones without values
	 */
	protected static void processXFAForm(Node node, boolean includeAllNamedObject, Map<String, List<Object>> fieldValues, String prefixFieldName) throws TransformerException {
		NodeList children = node.getChildNodes();
		if (children!=null && children.getLength()>0) {
			for (int i=0; i<children.getLength(); i++) {
				Node child = children.item(i);				
				if ("field".equalsIgnoreCase(child.getNodeName())) {
					List<Node> button = XMLUtils.locateNodesRecursive(child, null, "button");
					if (button!=null && !button.isEmpty())
						continue; // ignora botões
					String name = prefixFieldName+XMLUtils.getAttribute(child, "name");
					Node nodeValue = XMLUtils.locateNode(child, null, "value");
					if (nodeValue!=null) {
						try {
							Node textValue = XMLUtils.locateNode(nodeValue, null, "text");
							if (textValue!=null) {
								String content = textValue.getTextContent();
								if (content!=null)
									content = content.trim();
								if (content!=null && content.length()>0) {
									fieldValues.compute(name, (k,v)-> v == null ? new LinkedList<>() : v).add(content);
								}
								else if (includeAllNamedObject) {
									fieldValues.compute(name, (k,v)-> v == null ? new LinkedList<>() : v).add(null);												
								}
							}
							else {
								Node floatValue = XMLUtils.locateNode(nodeValue, null, "float");
								if (floatValue!=null) {
									String content = floatValue.getTextContent();
									if (content!=null && !content.trim().isEmpty() ) {
										content = content.trim();
										Number valor;		
										if (content.contains(",") && !content.contains("."))
											valor = ParserUtils.parseDecimalWithComma(content);
										else
											valor = Double.valueOf(content);
										fieldValues.compute(name, (k,v)-> v == null ? new LinkedList<>() : v).add(valor);
									}
									else if (includeAllNamedObject) {
										fieldValues.compute(name, (k,v)-> v == null ? new LinkedList<>() : v).add(null);											
									}
								}
								else {
									Node dateValue = XMLUtils.locateNode(nodeValue, null, "date");
									if (dateValue!=null) {
										String content = dateValue.getTextContent();
										if (content!=null && !content.trim().isEmpty() ) {
											Date d = ParserUtils.parseFlexibleDate(content.trim());
											fieldValues.compute(name, (k,v)-> v == null ? new LinkedList<>() : v).add(d);
										}
										else if (includeAllNamedObject) {
											fieldValues.compute(name, (k,v)-> v == null ? new LinkedList<>() : v).add(null);												
										}
									}
									else {
										Node integerValue = XMLUtils.locateNode(nodeValue, null, "integer");
										if (integerValue!=null) {
											String content = integerValue.getTextContent();
											if (content!=null && !content.trim().isEmpty() ) {
												long valor = Long.parseLong(content.trim());		
												fieldValues.compute(name, (k,v)-> v == null ? new LinkedList<>() : v).add(valor);
											}
											else if (includeAllNamedObject) {
												fieldValues.compute(name, (k,v)-> v == null ? new LinkedList<>() : v).add(null);											
											}		
										}
										else {											
											Node decimalValue = XMLUtils.locateNode(nodeValue, null, "decimal");
											if (decimalValue!=null) {
												String content = decimalValue.getTextContent();
												if (content!=null && !content.trim().isEmpty() ) {
													content = content.trim();
													Number valor;		
													if (content.contains(",") && !content.contains("."))
														valor = ParserUtils.parseDecimalWithComma(content);
													else
														valor = Double.valueOf(content);
													fieldValues.compute(name, (k,v)-> v == null ? new LinkedList<>() : v).add(valor);
												}
												else if (includeAllNamedObject) {
													fieldValues.compute(name, (k,v)-> v == null ? new LinkedList<>() : v).add(null);											
												}
											}
										
											// Ignore other field types
											else if (includeAllNamedObject) {
												fieldValues.compute(name, (k,v)-> v == null ? new LinkedList<>() : v).add(null);
											}											
										}
									}
								}
							}
						}
						catch (Exception ex) {
							// Algum erro de conversão...
							fieldValues.compute(name, (k,v)-> v == null ? new LinkedList<>() : v).add(null);
						}
					}
					else if (includeAllNamedObject) {
						fieldValues.compute(name, (k,v)-> v == null ? new LinkedList<>() : v).add(null);												
					}
				}
				else {
					String name = XMLUtils.getAttribute(child, "name");
					if (name!=null) {
						if (includeAllNamedObject) {
							fieldValues.compute(name, (k,v)-> v == null ? new LinkedList<>() : v).add(null);											
						}
						String nextPrefix = (prefixFieldName==null) ? (name+".") : (prefixFieldName+name+".");
						processXFAForm(child, includeAllNamedObject, fieldValues, nextPrefix);
					}
					else {
						processXFAForm(child, includeAllNamedObject, fieldValues, prefixFieldName);
					}
				}				
			}
		}
	}	
}
