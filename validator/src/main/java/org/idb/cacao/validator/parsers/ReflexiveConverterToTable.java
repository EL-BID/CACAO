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

import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * Generic conversor of a hierarchy of objects into a 'flattened' view (rows/columns)
 * 
 * @author Gustavo Figueiredo
 *
 */
public class ReflexiveConverterToTable extends ReflexiveConverter {

	private final List<String> titles;
	private final Map<String,Integer> mapTitleToColumn;
	private final List<Object[]> tab;
	private final Stack<String> parenthood;
	private final Stack<BitSet> parentCols;
	private int row;
	private final Stack<AtomicBoolean> flagsFirstRowInLevel;
	private final Stack<AtomicInteger> countersInLevel;
	private int maxColumns;
	
	public ReflexiveConverterToTable() {
		titles = new LinkedList<>();
		mapTitleToColumn = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		tab = new LinkedList<>();
		parenthood = new Stack<>();
		parentCols = new Stack<>();
		parentCols.push(new BitSet());
		flagsFirstRowInLevel = new Stack<>();
		flagsFirstRowInLevel.push(new AtomicBoolean(true));
		countersInLevel = new Stack<>();
	}
	
	/**
	 * Returns the gathered titles after parsing all data. The nested fields keeps the hierarchy.
	 * The names from the different levels in hierarchy are concatenated and separated by dots.
	 */
	public List<String> getTitles() {
		return titles;
	}

	/**
	 * Returns the gathered data in flattened structure after parsing all data.
	 */
	public List<Object[]> getTable() {
		return tab;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.parsers.ReflexiveConverter#visitFieldsStart(java.lang.Object)
	 */
	@Override
	protected void visitFieldsStart(Object obj) {
		flagsFirstRowInLevel.push(new AtomicBoolean(true));
		parentCols.push((BitSet)parentCols.peek().clone());
		countersInLevel.push(new AtomicInteger());
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.parsers.ReflexiveConverter#visitFieldsStop(java.lang.Object)
	 */
	@Override
	protected void visitFieldsStop(Object obj) {
		flagsFirstRowInLevel.pop();
		parentCols.pop();
		countersInLevel.pop();
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.parsers.ReflexiveConverter#visitFieldStart(java.lang.Object, org.idb.cacao.validator.parsers.ReflexiveConverter.FieldDescriptor, java.lang.Object)
	 */
	@Override
	protected void visitFieldStart(Object obj, FieldDescriptor field, Object value) {
		if (value!=null) {
			String objName = field.getFieldName();
			parenthood.push(objName);
		}			
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.parsers.ReflexiveConverter#visitFieldStop(java.lang.Object, org.idb.cacao.validator.parsers.ReflexiveConverter.FieldDescriptor, java.lang.Object)
	 */
	@Override
	protected void visitFieldStop(Object obj, FieldDescriptor field, Object value) {
		if (value!=null) {
			parenthood.pop();
		}
	}
	
	/**
	 * Store a value at a given row/column position in the flattened view of data. The internal
	 * structure grows as needed.
	 */
	private void setCell(int row, int col, Object value) {
		while (tab.size()<=row) {
			tab.add(new Object[maxColumns]);
		}
		Object[] cols = tab.get(row);
		if (cols.length<=col) {
			cols = Arrays.copyOf(cols, Math.max(maxColumns, col+1));
			tab.set(row, cols);
		}
		cols[col] = value;
		if (col>maxColumns)
			maxColumns = col;
	}
	
	/**
	 * Returns the stored value at a given row/column position in the flattened view of data.
	 */
	private Object getCell(int row, int col) {
		if (tab.size()<=row)
			return null;
		Object[] cols = tab.get(row);
		if (cols.length<=col)
			return null;
		return cols[col];
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.parsers.ReflexiveConverter#visitValuePrimitive(java.lang.Object)
	 */
	@Override
	protected void visitValuePrimitive(Object obj) {
		// While filling primitive values (e.g. an array of strings), it may be filling values in either different rows or different columns.
		// The heuristic will considers the hierarchy.
		// If there is an odd number of one-to-many relationships in the hierarchy, suppose we are filling different columns of the same row
		// If there is an even number of one-to-many relationships in the hierarchy, suppose we are filling different rows of the same column
		boolean fillingColumns = !countersInLevel.isEmpty() && ((parentCols.size()%2)==1);
		if (fillingColumns) {
			// fills in different values in different columns
			int count = countersInLevel.peek().incrementAndGet();
			String title;
			if (!parenthood.isEmpty())
				title = String.join(".", parenthood)+".element_"+count;
			else
				title = "element_"+count;
			int col = getOrCreateColumn(title);
			setCell(row, col, obj);
		}
		else {
			// fills in different values in different rows
			String title;
			if (!parenthood.isEmpty())
				title = String.join(".", parenthood);
			else
				title = "element";
			int col = getOrCreateColumn(title);
			setCell(row, col, obj);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.parsers.ReflexiveConverter#visitFieldPrimitive(java.lang.Object, org.idb.cacao.validator.parsers.ReflexiveConverter.FieldDescriptor, java.lang.Object)
	 */
	@Override
	protected void visitFieldPrimitive(Object obj, FieldDescriptor field, Object value) {
		String title;
		if (!parenthood.isEmpty())
			title = String.join(".", parenthood) + "." + field.getFieldName();
		else
			title = field.getFieldName();
		int col = getOrCreateColumn(title);
		setCell(row, col, value);
		// Signals we have filled this column, so that the same values may be copied into new rows that are nested
		BitSet colsMarks = parentCols.peek();
		colsMarks.set(col);
	}
	
	private int getOrCreateColumn(String title) {
		Integer col = mapTitleToColumn.get(title);
		if (col!=null)
			return col.intValue();
		col = titles.size();
		titles.add(title);
		mapTitleToColumn.put(title, col);
		return col;
	}
	
	@Override
	protected void visitManyValuesStart(Object obj, int size) {
		parentCols.push((BitSet)parentCols.peek().clone());
		countersInLevel.push(new AtomicInteger());
	}
	
	@Override
	protected void visitManyValuesStop(Object obj) {
		parentCols.pop();
		countersInLevel.pop();
	}
	
	@Override
	protected void visitManyValuesElement(Object obj, int index, Object el) {
		// For newer lines after the first, adds new lines
		AtomicBoolean firstRowInLevel = flagsFirstRowInLevel.peek();
		if (firstRowInLevel.get()) {
			firstRowInLevel.set(false);
		}
		else if (!countersInLevel.isEmpty() && ((parentCols.size()%2)==1) && (el==null || isPrimitiveType(el.getClass())) ) {
			// Do not increment the row if it's filling primitive values (e.g. Strings) for some object that is part
			// of a set of objects
			if (el==null) {
				countersInLevel.peek().incrementAndGet();
			}
		}
		else {
			row++;
			// While creating new rows, repeats the values from the ancestors
			if (!parentCols.isEmpty()) {
				BitSet colsMarks = parentCols.peek();
				for (int col=colsMarks.nextSetBit(0);col>=0;col=colsMarks.nextSetBit(col+1)) {
					setCell(row, col, getCell(row-1, col));
				}
			}
		}
	}

}
