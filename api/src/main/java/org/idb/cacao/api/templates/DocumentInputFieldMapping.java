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

import static org.springframework.data.elasticsearch.annotations.FieldType.Integer;
import static org.springframework.data.elasticsearch.annotations.FieldType.Keyword;

import java.io.Serializable;

import org.springframework.data.elasticsearch.annotations.Field;

/**
 * For each DocumentInput (which in turn is related to a file format, such as XLS, XML, etc.) there
 * will be a collection of DocumentInputFieldMapping.<BR>
 * <BR>
 * Each DocumentInputFieldMapping will refer to one particular DocumentField related to a DocumentTemplate.<BR>
 * <BR>
 * EXAMPLE:<BR>
 * ================================<BR>
 * Suppose we have a DocumentTemplate named 'LEDGER' with the following DocumentField's:<BR>
 * DocumentField[1] "Date" of type DATE<BR>
 * DocumentField[2] "Account" of type CHARACTER<BR>
 * DocumentField[3] "Value" of type DECIMAL<BR>
 * DocumentField[4] "D/C" of type CHARACTER<BR>
 * DocumentField[5] "History" of type CHARACTER<BR>
 * <BR>
 * Now suppose we have a DocumentInput related to this DocumentTemplate that should be used for importing
 * files in XLSX format (Excel files).<BR>
 * In such a DocumentInput structure we should find a corresponding DocumentInputFieldMapping for
 * each DocumentField, so that the system knows how to fetch each information from XLSX file and put
 * it into the 'LEDGER' structure.<BR>
 * So we could have something like this:<BR>
 * DocumentInputFieldMapping[1] take "Date" from worksheet 'Ledger', column 'A'<BR>
 * DocumentInputFieldMapping[2] take "Account" from worksheet 'Ledger', column 'B'<BR>
 * DocumentInputFieldMapping[3] take "Value" from worksheet 'Ledger', column 'C'<BR>
 * DocumentInputFieldMapping[4] take "D/C" from worksheet 'Ledger', column 'D'<BR>
 * DocumentInputFieldMapping[5] take "History" from worksheet 'Ledger', column 'E'<BR>
 * <BR>
 * Not all of the field members of this object are mappable to every file format. See the javadocs for
 * more information about each one.
 *  
 * 
 * @author Gustavo Figueiredo
 *
 */
public class DocumentInputFieldMapping implements Serializable, Cloneable, Comparable<DocumentInputFieldMapping> {

	private static final long serialVersionUID = 1L;

	/**
	 * The ID of the DocumentField this mapping refers to.
	 */
	@Field(type=Integer)
	private int fieldId;

	/**
	 * The column (0-based) position of this information in the input file.<BR>
	 * May be NULL if not specific to a particular column in a specific order or if a columnNameExpression has been provided<BR>
	 * Applies to these file formats:<BR>
	 * XLS/XLSX<BR>
	 * TXT/CSV<BR>
	 * DOC/DOCX (for tables inside)<BR>
	 */
	@Field(type=Integer)
	private Integer columnIndex;

	/**
	 * The column name expression of this information in the input file.<BR>
	 * May be NULL if not specific to a particular column or if a columnIndex has been provided.<BR>
	 * Applies to these file formats:<BR>
	 * XLS/XLSX<BR>
	 * TXT/CSV<BR>
	 * DOC/DOCX (for tables inside)<BR>
	 */
	@Field(type=Keyword)
	private String columnNameExpression;

	/**
	 * The row (0-based) position of this information in the input file.<BR>
	 * May be NULL if not specific to a particular row in a specific order<BR>
	 * Applies to these file formats:<BR>
	 * XLS/XLSX<BR>
	 * TXT/CSV<BR>
	 * DOC/DOCX (for tables inside)<BR>
	 */
	@Field(type=Integer)
	private Integer rowIndex;

	/**
	 * The sheet (0-based) position of this information in the input file.<BR>
	 * May be NULL if not specific to a particular sheet or if a sheetNameExpression has been provided.<BR>
	 * Applies to these file formats:<BR>
	 * XLS/XLSX<BR>
	 */
	@Field(type=Keyword)
	private Integer sheetIndex;

	/**
	 * The sheet name expression of this information in the input file.<BR>
	 * May be NULL if not specific to a particular sheet or if a sheetIndex has been provided.<BR>
	 * Applies to these file formats:<BR>
	 * XLS/XLSX<BR>
	 */
	@Field(type=Keyword)
	private String sheetNameExpression;

	/**
	 * The XPATH expression of this information in the input file.<BR>
	 * Applies to these file formats:<BR>
	 * JSON<BR>
	 * XML<BR>
	 */
	@Field(type=Keyword)
	private String pathExpression;

	/**
	 * The form field name of this information in the input file.<BR>
	 * Applies to these file formats:<BR>
	 * XLS/XLSX (named cells)<BR>
	 * PDF (FORMS)<BR>
	 */
	@Field(type=Keyword)
	private String fieldName;

	/**
	 * The ID of the DocumentField this mapping refers to.
	 */
	public int getFieldId() {
		return fieldId;
	}

	/**
	 * The ID of the DocumentField this mapping refers to.
	 */
	public void setFieldId(int fieldId) {
		this.fieldId = fieldId;
	}

	/**
	 * The column (0-based) position of this information in the input file.<BR>
	 * May be NULL if not specific to a particular column in a specific order or if a columnNameExpression has been provided<BR>
	 * Applies to these file formats:<BR>
	 * XLS/XLSX<BR>
	 * TXT/CSV<BR>
	 * DOC/DOCX (for tables inside)<BR>
	 */
	public Integer getColumnIndex() {
		return columnIndex;
	}

	/**
	 * The column (0-based) position of this information in the input file.<BR>
	 * May be NULL if not specific to a particular column in a specific order or if a columnNameExpression has been provided<BR>
	 * Applies to these file formats:<BR>
	 * XLS/XLSX<BR>
	 * TXT/CSV<BR>
	 * DOC/DOCX (for tables inside)<BR>
	 */
	public void setColumnIndex(Integer columnIndex) {
		this.columnIndex = columnIndex;
	}

	/**
	 * The column name expression of this information in the input file.<BR>
	 * May be NULL if not specific to a particular column or if a columnIndex has been provided.<BR>
	 * Applies to these file formats:<BR>
	 * XLS/XLSX<BR>
	 * TXT/CSV<BR>
	 * DOC/DOCX (for tables inside)<BR>
	 */
	public String getColumnNameExpression() {
		return columnNameExpression;
	}

	/**
	 * The column name expression of this information in the input file.<BR>
	 * May be NULL if not specific to a particular column or if a columnIndex has been provided.<BR>
	 * Applies to these file formats:<BR>
	 * XLS/XLSX<BR>
	 * TXT/CSV<BR>
	 * DOC/DOCX (for tables inside)<BR>
	 */
	public void setColumnNameExpression(String columnNameExpression) {
		this.columnNameExpression = columnNameExpression;
	}

	/**
	 * The row (0-based) position of this information in the input file.<BR>
	 * May be NULL if not specific to a particular row in a specific order<BR>
	 * Applies to these file formats:<BR>
	 * XLS/XLSX<BR>
	 * TXT/CSV<BR>
	 * DOC/DOCX (for tables inside)<BR>
	 */
	public Integer getRowIndex() {
		return rowIndex;
	}

	/**
	 * The row (0-based) position of this information in the input file.<BR>
	 * May be NULL if not specific to a particular row in a specific order<BR>
	 * Applies to these file formats:<BR>
	 * XLS/XLSX<BR>
	 * TXT/CSV<BR>
	 * DOC/DOCX (for tables inside)<BR>
	 */
	public void setRowIndex(Integer rowIndex) {
		this.rowIndex = rowIndex;
	}

	/**
	 * The sheet (0-based) position of this information in the input file.<BR>
	 * May be NULL if not specific to a particular sheet or if a sheetNameExpression has been provided.<BR>
	 * Applies to these file formats:<BR>
	 * XLS/XLSX<BR>
	 */
	public Integer getSheetIndex() {
		return sheetIndex;
	}

	/**
	 * The sheet (0-based) position of this information in the input file.<BR>
	 * May be NULL if not specific to a particular sheet or if a sheetNameExpression has been provided.<BR>
	 * Applies to these file formats:<BR>
	 * XLS/XLSX<BR>
	 */
	public void setSheetIndex(Integer sheetIndex) {
		this.sheetIndex = sheetIndex;
	}

	/**
	 * The sheet name expression of this information in the input file.<BR>
	 * May be NULL if not specific to a particular sheet or if a sheetIndex has been provided.<BR>
	 * Applies to these file formats:<BR>
	 * XLS/XLSX<BR>
	 */
	public String getSheetNameExpression() {
		return sheetNameExpression;
	}

	/**
	 * The sheet name expression of this information in the input file.<BR>
	 * May be NULL if not specific to a particular sheet or if a sheetIndex has been provided.<BR>
	 * Applies to these file formats:<BR>
	 * XLS/XLSX<BR>
	 */
	public void setSheetNameExpression(String sheetNameExpression) {
		this.sheetNameExpression = sheetNameExpression;
	}

	/**
	 * The XPATH expression of this information in the input file.<BR>
	 * Applies to these file formats:<BR>
	 * JSON<BR>
	 * XML<BR>
	 */
	public String getPathExpression() {
		return pathExpression;
	}

	/**
	 * The XPATH expression of this information in the input file.<BR>
	 * Applies to these file formats:<BR>
	 * JSON<BR>
	 * XML<BR>
	 */
	public void setPathExpression(String pathExpression) {
		this.pathExpression = pathExpression;
	}

	/**
	 * The form field name of this information in the input file.<BR>
	 * Applies to these file formats:<BR>
	 * XLS/XLSX (named cells)<BR>
	 * PDF (FORMS)<BR>
	 */
	public String getFieldName() {
		return fieldName;
	}

	/**
	 * The form field name of this information in the input file.<BR>
	 * Applies to these file formats:<BR>
	 * XLS/XLSX (named cells)<BR>
	 * PDF (FORMS)<BR>
	 */
	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof DocumentInputFieldMapping))
			return false;
		return fieldId==((DocumentInputFieldMapping)o).fieldId;
	}
	
	@Override
	public int hashCode() {
		return 17 + (int) ( 37 * fieldId );
	}

	public DocumentInputFieldMapping clone() {
		try {
			return (DocumentInputFieldMapping)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

    @Override
    public String toString() {
        return "DocumentInputFieldMapping{fieldId=" + fieldId + '}';
    }

	@Override
	public int compareTo(DocumentInputFieldMapping o) {
		if (fieldId<o.fieldId)
			return -1;
		if (fieldId>o.fieldId)
			return 1;
		return 0;
	}
	
}
