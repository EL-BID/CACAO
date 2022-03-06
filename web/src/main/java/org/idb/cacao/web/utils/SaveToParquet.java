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
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.JobID;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.TaskAttemptID;
import org.apache.hadoop.mapreduce.TaskID;
import org.apache.hadoop.mapreduce.TaskType;
import org.apache.hadoop.mapreduce.task.TaskAttemptContextImpl;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.NanoTime;
import org.apache.parquet.example.data.simple.SimpleGroupFactory;
import org.apache.parquet.hadoop.ParquetOutputFormat;
import org.apache.parquet.hadoop.ParquetOutputFormat.JobSummaryLevel;
import org.apache.parquet.hadoop.codec.CodecConfig;
import org.apache.parquet.hadoop.example.GroupWriteSupport;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;
import org.apache.parquet.schema.Type;
import org.apache.parquet.schema.Types;
import org.apache.parquet.schema.Types.GroupBuilder;
import org.idb.cacao.api.ValidationContext;

/**
 * Object used for creating a PARQUET file. In order to keep it simple, it does not support complex type.<BR>
 * <BR>
 * Simply create this object, set the target file with {@link #setOutputFile(File) setOutputFile}, set the
 * schema with {@link #setSchema(MessageType) setSchema} or with {@link #setSchemaFromProperties(Map) setSchemaFromProperties},
 * initialize it with {@link #init() init}, and then call the method {@link #write(Map) write} for each record. After all
 * writings, call {@link #close() close} to finish and close it.<BR>
 * 
 * @author Gustavo Figueiredo
 *
 */
public class SaveToParquet implements Closeable {

	private ParquetOutputFormat<Group> outputFormat;

	private MessageType schema;

	private TaskAttemptContext taskContext;
	
	private RecordWriter<Void, Group> writer;

	private SimpleGroupFactory fact;

	private Path outputFile;
	
	private List<BiConsumer<Object,Group>> fieldsProjections;

	private Map<String,Integer> mapFieldsPositions;

	/**
	 * Target file to create (parquet format)
	 */
	public Path getOutputFile() {
		return outputFile;
	}

	/**
	 * Target file to create (parquet format)
	 */
	public void setOutputFile(Path outputFile) {
		this.outputFile = outputFile;
	}
	
	/**
	 * Target file to create (parquet format)
	 */
	public void setOutputFile(File outputFile) {
		setOutputFile(new Path(outputFile.getAbsolutePath()));
	}

	/**
	 * Schema of Parquet columns
	 */
	public MessageType getSchema() {
		return schema;
	}

	/**
	 * Schema of Parquet columns
	 */
	public void setSchema(MessageType schema) {
		this.schema = schema;
	}
	
	/**
	 * Schema of Parquet columns derived from Elastic Search fields properties.
	 */
	public void setSchemaFromProperties(Map<?,?> properties) {
		setSchema(toMessageType(properties));
	}

	/**
	 * Initialize the internal objects for writing data to Parquet file
	 */
	public void init() throws IOException {
		
		// Initialize PARQUET WRITER stuff .......
		
		if (schema==null)
			throw new RuntimeException("Missing PARQUET Schema (MessageType)!");
		if (outputFile==null)
			throw new RuntimeException("Missing output file!");
		
		Configuration configuration = new Configuration();
		configuration.set(ParquetOutputFormat.JOB_SUMMARY_LEVEL, JobSummaryLevel.NONE.name());
		configuration.set(ParquetOutputFormat.COMPRESSION, "SNAPPY");

	    JobConf jc = new JobConf(configuration);
	    jc.set(ParquetOutputFormat.WRITE_SUPPORT_CLASS, GroupWriteSupport.class.getName());
	    GroupWriteSupport.setSchema(schema, jc);
	    
	    FileSystem fs = outputFile.getFileSystem(configuration);
	    if (fs.exists(outputFile)) {
	    	fs.delete(outputFile, /*recursive*/false);
	    }

	    outputFormat = new ParquetOutputFormat<Group>();
	    try {
			writer = outputFormat.getRecordWriter(jc, outputFile, CodecConfig.getParquetCompressionCodec(configuration));
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		TaskID id = new TaskID(new JobID(),TaskType.MAP, 0);
		TaskAttemptID taskId = new TaskAttemptID(id,1);
		taskContext = new TaskAttemptContextImpl(jc, taskId);

		fact = new SimpleGroupFactory(schema);
		
		mapFieldsPositions = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		
		List<Type> parquet_fields = schema.getFields();
		fieldsProjections = new ArrayList<>(parquet_fields.size());
		for (int i=0; i<parquet_fields.size(); i++) {
			final int fieldIndex = i;
			Type parquet_field = parquet_fields.get(i);
			PrimitiveTypeName type = parquet_field.asPrimitiveType().getPrimitiveTypeName();
			switch (type) {
			case BOOLEAN:
				fieldsProjections.add((value,parquet_record)->parquet_record.add(fieldIndex, "true".equalsIgnoreCase(ValidationContext.toString(value))));
				break;
			case DOUBLE:
				fieldsProjections.add((value,parquet_record)->parquet_record.add(fieldIndex, ValidationContext.toNumber(value).doubleValue()));
				break;
			case INT64:
				fieldsProjections.add((value,parquet_record)->parquet_record.add(fieldIndex, ValidationContext.toNumber(value).longValue()));
				break;
			case INT96:
				fieldsProjections.add((value,parquet_record)->parquet_record.add(fieldIndex, toNanoTime(ValidationContext.toDate(value))));
				break;
			default:
				fieldsProjections.add((value,parquet_record)->parquet_record.add(fieldIndex, ValidationContext.toString(value)));
			}
			mapFieldsPositions.put(parquet_field.getName(), i);
		}
	}
	
	/**
	 * Given a record of fields (with fields names as keys and fields values as values), save it as a Parquet record
	 */
	public void write(Map<?,?> record) throws IOException {
		Group parquet_record = fact.newGroup();
		for (Map.Entry<?,?> entry: record.entrySet()) {
			String fieldName = (String)entry.getKey();
			Integer fieldIndex = mapFieldsPositions.get(fieldName);
			if (fieldIndex==null)
				continue;
			Object value = entry.getValue();
			if (value==null)
				continue;
			fieldsProjections.get(fieldIndex).accept(value, parquet_record);
		}
		try {
			writer.write(null, parquet_record);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Flushes the write buffers and closes the Parquet file
	 */
	@Override
	public void close() throws IOException {
		if (writer!=null) {
			try {
				writer.close(taskContext);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	/**
	 * Given ElasticSearch properties containing fields names and corresponding field types, returns
	 * the Parquet 'schema'.
	 */
	public static MessageType toMessageType(Map<?,?> properties) {
		GroupBuilder<MessageType> builder = Types.buildMessage();
		for (Map.Entry<?,?> prop: properties.entrySet()) {
			String fieldName = (String)prop.getKey();
			String fieldType = (String)((Map<?,?>)prop.getValue()).get("type");
			switch (fieldType) {
			case "boolean":
				builder = builder.optional(PrimitiveTypeName.BOOLEAN).named(fieldName);
				break;
			case "float":
				builder = builder.optional(PrimitiveTypeName.DOUBLE).named(fieldName);
				break;
			case "long":
				builder = builder.optional(PrimitiveTypeName.INT64).named(fieldName);
				break;
			case "date":
				builder = builder.optional(PrimitiveTypeName.INT96).named(fieldName);
				break;
			default:
				builder = builder.optional(PrimitiveTypeName.BINARY).as(LogicalTypeAnnotation.StringLogicalTypeAnnotation.stringType()).named(fieldName);
				break;
			}
		}
		return builder.named("Data");
	}
	
	/**
	 * Convert java Date object into 'NanoTime' for storing in Parquet
	 */
	public static NanoTime toNanoTime(java.util.Date d) {
		Instant timestamp = (d instanceof java.sql.Date) ? Instant.ofEpochMilli(new java.util.Date(d.getTime()).toInstant().toEpochMilli()) 
				: Instant.ofEpochMilli(d.toInstant().toEpochMilli());
		ZonedDateTime dateTime = timestamp.atZone(ZoneId.systemDefault());
		long julianDay = dateTime.getLong(java.time.temporal.JulianFields.JULIAN_DAY);
		long timeInSeconds = (dateTime.getHour()*60L+dateTime.getMinute())*60L+dateTime.getSecond();
		long nanos = dateTime.getNano();
		return new NanoTime((int)julianDay, timeInSeconds * 1_000_000_000L + nanos);
	}

}
