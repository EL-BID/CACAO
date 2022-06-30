/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.validator.parsers;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

/**
 * Implements {@link FileParser} interface to parse WORD (DOC/DOCX) files. <br>
 * <br>
 * IMPORTANT: 	WORD file must contains at least one table. If there is more than one table, parser will<br>
 * 				try to aggregate all data in one table, and all tables must have same number of columns and<br>
 * 				columns descriptions must be the same in all tables.<br>
 * 				If a second table has different number of columsn of different columns descriptions, it will<br>
 * 				be discarded.<br>
 * 
 * @author Rivelino Patrício
 * 
 * @since 15/12/2021
 *
 */
public class WordParser extends FileParserAdapter {

	private static final Logger log = Logger.getLogger(WordParser.class.getName());

	// Table with data
	private XWPFTable dataTable;

	private TabulatedData tab;
	
	@Override
	public void start() {

		dataTable = null;

		// If there is no information about WORD file, return
		if (path == null)
			return;

		try (FileInputStream fis = new FileInputStream(path.toFile())) {

			try (XWPFDocument docu = new XWPFDocument(fis)) {

				List<XWPFTable> tables = docu.getTables();
				if (tables != null) {

					for (XWPFTable table : tables) {

						if (dataTable == null) {
							dataTable = table;
							continue;
						}

						if (table.getRow(0).getTableCells().size() == dataTable.getRow(0).getTableCells().size() 
								&& isFirstRowEquals(table)) {

							// Remove first row, supposed to be column descriptions
							table.removeRow(0);
							// Add new table data inside actual table
							table.getRows().forEach(row -> dataTable.addRow(row));

						}

					}

				}

			}

		} catch (IOException e) {
			log.log(Level.SEVERE, String.format("Error trying to read file %s", path.getFileName()), e);
		}

		if (dataTable != null) {
			tab = new TabulatedData(documentInputSpec);
			String[] parts = readLine(dataTable.getRow(0));
			tab.parseColumnNames(parts);
		}

	}

	/**
	 * Read first row in a table to get columns descriptions
	 * 
	 * @param row First row of a table
	 * @return An array of column description
	 */
	private String[] readLine(XWPFTableRow row) {
		if (row == null)
			return new String[0];
		List<XWPFTableCell> columns = row.getTableCells();
		List<String> columnsDescriptions = new ArrayList<>(columns.size());
		columns.forEach(col -> columnsDescriptions.add(col.getText()));
		return columnsDescriptions.toArray(new String[0]);
	}

	/**
	 * Check if the first row a given table contais same text as first row of
	 * {@link #dataTable}
	 * 
	 * @param table A table to be checked
	 * @return True if first row is equals. False if not.
	 */
	private boolean isFirstRowEquals(XWPFTable table) {
		if (table == null || dataTable == null)
			return false;

		List<XWPFTableCell> columns = table.getRow(0).getTableCells();
		List<XWPFTableCell> columns2 = dataTable.getRow(0).getTableCells();

		for (int i = 0; i < columns.size(); i++) {
			XWPFTableCell cell = columns.get(i);
			XWPFTableCell cell2 = columns2.get(i);
			String value = cell.getText();
			String value2 = cell2.getText();
			if (value == null && value2 != null)
				return false;
			if (value != null && !value.equalsIgnoreCase(value2))
				return false;
		}
		return true;
	}

	@Override
	public DataIterator iterator() {
		if (path == null || !path.toFile().exists()) {
			return null;
		}

		if (dataTable == null) {
			start();
		}

		if (dataTable == null)
			return null;

		try {

			return new DataIterator() {

				int numberOfRows = dataTable.getNumberOfRows();
				int nextRow = 1;

				@Override
				public Map<String, Object> next() {
					
					if(!hasNext()){
						throw new NoSuchElementException();
					}

					XWPFTableRow row = dataTable.getRow(nextRow++);

					if (row != null) {
						String[] parts = readLine(row);
						return tab.parseLine(parts);
					}
					return Collections.emptyMap();
				}

				@Override
				public boolean hasNext() {
					return nextRow < numberOfRows;
				}

				@Override
				public void close() {
					dataTable = null;
				}
			};

		} catch (Exception e) {
			log.log(Level.SEVERE, String.format("Error trying to iterate data from file %s", path.getFileName()), e);
		}

		return null;
	}

	@Override
	public void close() {
		dataTable = null;
	}

}
