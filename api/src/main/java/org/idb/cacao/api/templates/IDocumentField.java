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
package org.idb.cacao.api.templates;

/**
 * Configuration of a individual field defined for a particular template.
 * 
 * @author Gustavo Figueiredo
 *
 */
public interface IDocumentField {

	/**
	 * The unique identification of this field scoped for a particular template. Usually
	 * a sequential number of fields defined inside a DocumentTemplate.
	 */
	public int getId();
	
	/**
	 * The unique identification of this field scoped for a particular template. Usually
	 * a sequential number of fields defined inside a DocumentTemplate.
	 */
	public void setId(int id);
	
	/**
	 * The name of this field for displaying on DocumentTemplate structure. This is also
	 * the name to be used for indexing in ElasticSearch database.<BR>
	 * The name may include a hierarchy of path elements separated by pipe character ('|').
	 */
	public String getFieldName();
	
	/**
	 * The name of this field for displaying on DocumentTemplate structure. This is also
	 * the name to be used for indexing in ElasticSearch database.<BR>
	 * The name may include a hierarchy of path elements separated by pipe character ('|').
	 */
	public void setFieldName(String fieldName);
	
	default public String getGroup() {
		String fieldName = getFieldName();
		if (fieldName==null || fieldName.trim().length()==0)
			return null;
		int sep = fieldName.lastIndexOf('|');
		if (sep<0)
			return null;
		return fieldName.substring(0, sep);
	}
	
	default public String getSimpleFieldName() {
		String fieldName = getFieldName();
		if (fieldName==null || fieldName.trim().length()==0)
			return fieldName;
		int sep = fieldName.lastIndexOf('|');
		if (sep<0)
			return fieldName;
		return fieldName.substring(sep+1);		
	}

}
