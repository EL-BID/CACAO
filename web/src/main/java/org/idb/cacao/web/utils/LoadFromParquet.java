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
package org.idb.cacao.web.utils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.ParquetReadOptions;
import org.apache.parquet.column.page.PageReadStore;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.NanoTime;
import org.apache.parquet.example.data.simple.convert.GroupRecordConverter;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.hadoop.util.HadoopInputFile;
import org.apache.parquet.io.ColumnIOFactory;
import org.apache.parquet.io.InputFile;
import org.apache.parquet.io.MessageColumnIO;
import org.apache.parquet.io.RecordReader;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.Type;
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;

/**
 * Object used for reading a PARQUET file. In order to keep it simple, it does not support complex type.<BR>
 * <BR>
 * Simply create this object, set the input file with {@link #setInputFile(File) setInputFile}, 
 * initialize it with {@link #init() init}, and then call the method {@link #next() next} repeatedly, until it returns null. After all
 * reading, call {@link #close() close} to finish and close it.<BR>
 * 
 * @author Gustavo Figueiredo
 *
 */
public class LoadFromParquet implements Closeable {

	/**
	 * Input file to read (parquet format)
	 */
	private Path inputFile;
	
	private ParquetFileReader reader;
	
	private MessageColumnIO columnIO;
	
	private PageReadStore pages;
	
	private boolean finished;
	
	private RecordReader<Group> recordReader;
	
	private long rows;
	
	private long currentRow;

	/**
	 * Schema of Parquet columns
	 */
	private MessageType schema;
	
	private List<Function<Group,Object>> fieldsProjections;
	
	/**
	 * Input file to read (parquet format)
	 */
	public Path getInputFile() {
		return inputFile;
	}

	/**
	 * Input file to read (parquet format)
	 */
	public void setInputFile(Path inputFile) {
		this.inputFile = inputFile;
	}

	/**
	 * Input file to read (parquet format)
	 */
	public void setInputFile(File inputFile) {
		setInputFile(new Path(inputFile.getAbsolutePath()));
	}

	/**
	 * Schema of Parquet columns
	 */
	public MessageType getSchema() {
		return schema;
	}

	/**
	 * Initialize the internal objects for reading data from Parquet file
	 */
	public void init() throws IOException {
		
		// Initialize PARQUET READER stuff .......
		
		if (inputFile==null)
			throw new RuntimeException("Missing input file!");

		Configuration configuration = new Configuration();

		InputFile file = HadoopInputFile.fromPath(inputFile, configuration);

		ParquetReadOptions.Builder builder = ParquetReadOptions.builder();
		reader = ParquetFileReader.open(file, builder.build());
		
		ParquetMetadata meta = reader.getFooter();
		schema = meta.getFileMetaData().getSchema();

		columnIO = new ColumnIOFactory().getColumnIO(schema);
		
		List<Type> parquet_fields = schema.getFields();
		fieldsProjections = new ArrayList<>(parquet_fields.size());
		for (int i=0; i<parquet_fields.size(); i++) {
			final int fieldIndex = i;
			Type parquet_field = parquet_fields.get(i);
			PrimitiveTypeName type = parquet_field.asPrimitiveType().getPrimitiveTypeName();
			switch (type) {
			case BOOLEAN:
				fieldsProjections.add((parquet_record)->parquet_record.getBoolean(fieldIndex, 0));
				break;
			case DOUBLE:
				fieldsProjections.add((parquet_record)->parquet_record.getDouble(fieldIndex, 0));
				break;
			case INT64:
				fieldsProjections.add((parquet_record)->parquet_record.getLong(fieldIndex, 0));
				break;
			case INT96:
				fieldsProjections.add((parquet_record)->fromNanoTime(NanoTime.fromBinary((org.apache.parquet.io.api.Binary)parquet_record.getInt96(fieldIndex, 0))));
				break;
			default:
				fieldsProjections.add((parquet_record)->parquet_record.getString(fieldIndex, 0));
			}
		}

		finished = false;
	}
	
	/**
	 * Read next Parquet record. Returns NULL if there is no more records to read.
	 */
	public Map<String, Object> next() throws IOException {
		
		if (finished) {
			return null;
		}
		
		while (true) {			
		
			if (currentRow<rows) {
				Group g = recordReader.read();
				currentRow++;
				return convertParquetRecord(g);
			}
			
			pages = reader.readNextRowGroup();
			if (pages==null)
				break;
			
			recordReader = columnIO.getRecordReader(pages, new GroupRecordConverter(schema));
			rows = pages.getRowCount();
			currentRow = 0;
			System.out.println("===================> MORE "+rows+" ROWS");
		}

		finished = true;
		close();
		reader = null;
		return null;
	}
	
	private Map<String, Object> convertParquetRecord(Group parquetRecord) {
		Map<String, Object> record = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		
		List<Type> parquet_fields = schema.getFields();
		for (int i=0; i<parquet_fields.size(); i++) {
			Type field = parquet_fields.get(i);
			int rep = parquetRecord.getFieldRepetitionCount(i);
			if (rep==0)
				continue; // no value for this field in this record
			Object value = fieldsProjections.get(i).apply(parquetRecord);
			record.put(field.getName(), value);
		}
		
		return record;
	}
	
	@Override
	public void close() throws IOException {
		if (reader!=null) {
			reader.close();
		}
	}
	/**
	 * Faz o oposto ao método {@link SaveToParquet#toNanoTime(java.util.Date) toNanoTime}
	 */
	public static java.util.Date fromNanoTime(NanoTime n) {
		LocalDate date = LocalDate.MIN.with(java.time.temporal.JulianFields.JULIAN_DAY, n.getJulianDay());
		long time_in_nanos = n.getTimeOfDayNanos();
		int nanoOfSecond = (int)(time_in_nanos % 1_000_000_000L);
		long time_in_seconds = time_in_nanos / 1_000_000_000L;
		int seconds = (int)(time_in_seconds%60);
		time_in_seconds/=60;
		int minutes = (int)(time_in_seconds%60);
		int hours = (int)(time_in_seconds/60);
		LocalTime time = LocalTime.of(hours, minutes, seconds, nanoOfSecond);
		return java.util.Date.from(LocalDateTime.of(date, time).atZone(ZoneId.systemDefault()).toInstant());
	}

}
