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

import static org.idb.cacao.api.utils.ParserUtils.formatDecimal;
import static org.idb.cacao.api.utils.ParserUtils.formatTimestamp;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.pdmodel.interactive.form.PDXFAResource;
import org.idb.cacao.api.templates.DocumentInput;
import org.idb.cacao.api.templates.DocumentInputFieldMapping;
import org.idb.cacao.api.utils.ParserUtils;
import org.idb.cacao.validator.utils.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Parser for a PDF Document. PDF must use PDF Forms.
 *
 * @author Rivelino Patrício
 * 
 * @since 26/11/2021
 *
 */
public class PDFParser implements FileParser {

	private static final Logger log = Logger.getLogger(PDFParser.class.getName());

	private Path path;

	private DocumentInput documentInputSpec;

	private Map<String, Object> templateFields;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.idb.cacao.validator.parsers.FileParser#getPath()
	 */
	@Override
	public Path getPath() {
		return path;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.idb.cacao.validator.parsers.FileParser#setPath(java.nio.file.Path)
	 */
	@Override
	public void setPath(Path path) {
		this.path = path;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.idb.cacao.validator.parsers.FileParser#getDocumentInputSpec()
	 */
	@Override
	public DocumentInput getDocumentInputSpec() {
		return documentInputSpec;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.idb.cacao.validator.parsers.FileParser#setDocumentInputSpec(org.idb.cacao
	 * .api.templates.DocumentInput)
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
		
		if ( templateFields != null ) {
			templateFields.clear();
			templateFields = null;
		}
		
		try {
		
			templateFields = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			
			try (FileInputStream stream = new FileInputStream(path.toFile())) {
				try (PDDocument document = PDDocument.load(stream);) {
					processForms(document, /*includeAllNamedObject*/true, templateFields::put);
				} catch (Exception e) {
					log.log(Level.SEVERE, "Error trying to read file " + path.getFileName(), e);
				}
			}
		}
		catch (IOException e) {
			log.log(Level.SEVERE, "Error trying to read file " + path.getFileName(), e);
		}	
	}

	@Override
	public DataIterator iterator() {
		if ( path == null || !path.toFile().exists() ) {		
			return null;			
		}			
		
		if ( templateFields == null ) {
			start();
		}
		
		if ( templateFields == null )
			return null;
		
		try {
			
			final Iterator<Map.Entry<String, Object>> fieldsIterator = templateFields.entrySet().iterator();
			
			return new DataIterator() {
				
				@Override
				public Map<String, Object> next() {
					Map.Entry<String, Object> object = fieldsIterator.next();
					return null;
				}
				
				@Override
				public boolean hasNext() {					
					return fieldsIterator.hasNext();
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
		if ( templateFields != null ) {
			templateFields.clear();
			templateFields = null;
		}
	}
	
	private static String formatValue(Object obj) {
		if (obj instanceof Date)
			return formatTimestamp((Date)obj);
		if (obj instanceof Double)
			return formatDecimal((Double)obj);
		if (obj instanceof Float)
			return formatDecimal((Float)obj);
		return obj.toString();
	}	

	/**
	 * Process internal form structures
	 * @param document
	 * @param includeAllNamedObject If set to TRUE, will include in 'storeFields' all objects with 'name' property, including those ones without values
	 * @param storeFields
	 */
	protected void processForms(PDDocument document, boolean includeAllNamedObject, BiConsumer<String, Object> storeFields) throws Exception {
		
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
				DocumentBuilder builder = factory.newDocumentBuilder();
				try (InputStream in = new BufferedInputStream(new ByteArrayInputStream(pdfxfa.getBytes()))) {
					Document root = builder.parse(in);
					processXFAForm(root.getDocumentElement(), includeAllNamedObject, storeFields, /*prefix_field_name*/null);
				}
			}
			else {
				List<PDField> fields = pdAcroForm.getFields();
				if (fields!=null && !fields.isEmpty()) { 
					for (PDField field:fields) {
						storeFields.accept(field.getMappingName(), field.getValueAsString());
					}
				}
			}
		}
		try {
			List<PDSignatureField> sfields = document.getSignatureFields();
			if (sfields!=null && !sfields.isEmpty()) {
				for (PDSignatureField sfield:sfields) {
					System.out.println(sfield);
					storeFields.accept(sfield.getMappingName(), sfield.getValueAsString());
				}
			}
		} catch (IOException e) {
			log.log(Level.WARNING, "Error while parsing document", e);
		}
	}
	
	/**
	 * Process internal XFA form structures using recursive calls
	 * @param includeAllNamedObject If set to TRUE, will include in 'storeFields' all objects with 'name' property, including those ones without values
	 */
	protected static void processXFAForm(Node node, boolean includeAllNamedObject, BiConsumer<String, Object> storeFields, String prefixFieldName) throws TransformerException {
		NodeList children = node.getChildNodes();
		if (children!=null && children.getLength()>0) {
			for (int i=0; i<children.getLength(); i++) {
				Node child = children.item(i);
				if ("field".equalsIgnoreCase(child.getNodeName())) {
					List<Node> button = XMLUtils.locateNodesRecursive(child, null, "button");
					if (button!=null && !button.isEmpty())
						continue; // ignora botões
					String name = prefixFieldName+XMLUtils.getAttribute(child, "name");
					Node node_value = XMLUtils.locateNode(child, null, "value");
					if (node_value!=null) {
						try {
							Node text_value = XMLUtils.locateNode(node_value, null, "text");
							if (text_value!=null) {
								String content = text_value.getTextContent();
								if (content!=null)
									content = content.trim();
								if (content!=null && content.length()>0) {
									storeFields.accept(name, content);
								}
								else if (includeAllNamedObject) {
									storeFields.accept(name, null);												
								}
								continue;
							}
							else {
								Node float_value = XMLUtils.locateNode(node_value, null, "float");
								if (float_value!=null) {
									String content = float_value.getTextContent();
									if (content!=null)
										content = content.trim();
									if (content.length()>0) {
										Number valor;		
										if (content.contains(",") && !content.contains("."))
											valor = ParserUtils.parseDecimalWithComma(content);
										else
											valor = Double.valueOf(content);
										storeFields.accept(name, valor);
									}
									else if (includeAllNamedObject) {
										storeFields.accept(name, null);												
									}
									continue;
								}
								else {
									Node date_value = XMLUtils.locateNode(node_value, null, "date");
									if (date_value!=null) {
										String content = date_value.getTextContent();
										if (content!=null)
											content = content.trim();
										if (content.length()>0) {
											Date d = ParserUtils.parseFlexibleDate(content);
											storeFields.accept(name, d);
										}
										else if (includeAllNamedObject) {
											storeFields.accept(name, null);												
										}
										continue;
									}
									else {
										Node integer_value = XMLUtils.locateNode(node_value, null, "integer");
										if (integer_value!=null) {
											String content = integer_value.getTextContent();
											if (content!=null)
												content = content.trim();
											if (content.length()>0) {
												long valor = Long.valueOf(content);		
												storeFields.accept(name, valor);
											}
											else if (includeAllNamedObject) {
												storeFields.accept(name, null);												
											}
											continue;		
										}
										
										// Ignore other field types
										
										else if (includeAllNamedObject) {
											storeFields.accept(name, null);
										}
									}
								}
							}
						}
						catch (Throwable ex) {
							// Algum erro de conversão...
							storeFields.accept(name, null);
						}
					}
					else if (includeAllNamedObject) {
						storeFields.accept(name, null);												
					}
				}
				else {
					String name = XMLUtils.getAttribute(child, "name");
					if (name!=null) {
						if (includeAllNamedObject) {
							storeFields.accept(name, null);												
						}
						String next_prefix = (prefixFieldName==null) ? (name+".") : (prefixFieldName+name+".");
						processXFAForm(child, includeAllNamedObject, storeFields, next_prefix);
					}
					else {
						processXFAForm(child, includeAllNamedObject, storeFields, prefixFieldName);
					}
				}				
			}
		}
	}	
}
