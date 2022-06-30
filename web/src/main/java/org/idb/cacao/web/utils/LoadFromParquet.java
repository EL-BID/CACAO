/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.utils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.TreeMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.ParquetReadOptions;
import org.apache.parquet.Preconditions;
import org.apache.parquet.column.page.PageReadStore;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.hadoop.util.HadoopInputFile;
import org.apache.parquet.io.ColumnIOFactory;
import org.apache.parquet.io.InputFile;
import org.apache.parquet.io.MessageColumnIO;
import org.apache.parquet.io.RecordReader;
import org.apache.parquet.io.SeekableInputStream;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.io.api.Converter;
import org.apache.parquet.io.api.GroupConverter;
import org.apache.parquet.io.api.PrimitiveConverter;
import org.apache.parquet.io.api.RecordMaterializer;
import org.apache.parquet.schema.GroupType;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.Type;
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;

/**
 * Object used for reading a PARQUET file. In order to keep it simple, it does not support complex type.<BR>
 * <BR>
 * Simply create this object, set the input file with {@link #setInputFile(File) setInputFile}, 
 * initialize it with {@link #init() init}, and then call the method {@link #next() next} repeatedly, until it returns null. After all
 * reading, call {@link #close() close} to finish and close it.<BR>
 * <BR>
 * Optionally you may use {@link #setInputContents(byte[]) setInputContents} instead of {@link #setInputFile(File) setInputFile} if the file
 * contents is already in memory.<BR>
 * 
 * @author Gustavo Figueiredo
 *
 */
public class LoadFromParquet implements Closeable {

	/**
	 * Input file to read (parquet format). Use either this or 'inputContents', but not both.
	 */
	private Path inputFile;

	/**
	 * In-memory file contents. Use either this or 'inputFile', but not both.
	 */
	private byte[] inputContents;

	private ParquetFileReader reader;

	private MessageColumnIO columnIO;

	private PageReadStore pages;

	private boolean finished;

	private RecordReader<Map<String,Object>> recordReader;

	private MapRecordConverter recordConverter;

	private long rows;

	private long currentRow;

	/**
	 * Schema of Parquet columns
	 */
	private MessageType schema;

	/**
	 * Input file to read (parquet format). Use either this or 'inputContents', but not both.
	 */
	public Path getInputFile() {
		return inputFile;
	}

	/**
	 * Input file to read (parquet format). Use either this or 'inputContents', but not both.
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
	 * In-memory file contents. Use either this or 'inputFile', but not both.
	 */
	public byte[] getInputContents() {
		return inputContents;
	}

	/**
	 * In-memory file contents. Use either this or 'inputFile', but not both.
	 */
	public void setInputContents(byte[] inputContents) {
		this.inputContents = inputContents;
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

		if (inputFile==null && inputContents==null)
			throw new RuntimeException("Missing input file!");

		InputFile file;

		if (inputContents!=null) {
			file = new InMemoryFileContents(inputContents);
		}
		else {
			file = HadoopInputFile.fromPath(inputFile, new Configuration());
		}

		ParquetReadOptions.Builder builder = ParquetReadOptions.builder();
		reader = ParquetFileReader.open(file, builder.build());

		ParquetMetadata meta = reader.getFooter();
		schema = meta.getFileMetaData().getSchema();

		columnIO = new ColumnIOFactory().getColumnIO(schema);

		recordConverter = new MapRecordConverter(schema);

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
				Map<String,Object> record = recordReader.read();
				currentRow++;
				return record;
			}

			pages = reader.readNextRowGroup();
			if (pages==null)
				break;

			recordReader = columnIO.getRecordReader(pages, recordConverter);
			rows = pages.getRowCount();
			currentRow = 0;
		}

		finished = true;
		close();
		reader = null;
		return null;
	}

	@Override
	public void close() throws IOException {
		if (reader!=null) {
			reader.close();
		}
	}

	public static class MapRecordConverter extends RecordMaterializer<Map<String,Object>> {

		private MapEntriesConverter root;

		public MapRecordConverter(MessageType schema) {
			this.root = new MapEntriesConverter(schema);
		}

		/*
		 * (non-Javadoc)
		 * @see org.apache.parquet.io.api.RecordMaterializer#getCurrentRecord()
		 */
		@Override
		public Map<String,Object> getCurrentRecord() {
			return root.getCurrentRecord();
		}

		/*
		 * (non-Javadoc)
		 * @see org.apache.parquet.io.api.RecordMaterializer#getRootConverter()
		 */
		@Override
		public GroupConverter getRootConverter() {
			return root;
		}

		class MapEntriesConverter extends GroupConverter {
			private Map<String,Object> current;
			private Converter[] converters;

			MapEntriesConverter(GroupType schema) {
				converters = new Converter[schema.getFieldCount()];

				for (int i = 0; i < converters.length; i++) {
					final Type type = schema.getType(i);
					if (type.isPrimitive()) {
						if (PrimitiveTypeName.INT96.equals(type.asPrimitiveType().getPrimitiveTypeName())) {
							converters[i] = new TimestampPrimitiveConverter(type.getName());
						}
						else {
							converters[i] = new SimplePrimitiveConverter(type.getName());
						}
					} else {
						throw new UnsupportedOperationException("Complex type is not currently supported");
					}

				}
			}

			@Override
			public void start() {
				if (this.current==null)
					this.current = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
				else
					this.current.clear();
			}

			@Override
			public Converter getConverter(int fieldIndex) {
				return converters[fieldIndex];
			}

			@Override
			public void end() {
			}

			public Map<String,Object> getCurrentRecord() {
				return current;
			}

			class SimplePrimitiveConverter extends PrimitiveConverter {

				final String fieldName;

				SimplePrimitiveConverter(String fieldName) {
					this.fieldName = fieldName;
				}

				/**
				 * {@inheritDoc}
				 * @see org.apache.parquet.io.api.PrimitiveConverter#addBinary(Binary)
				 */
				@Override
				public void addBinary(Binary value) {
					if (value==null)
						current.remove(fieldName);
					else
						current.put(fieldName, value.toStringUsingUTF8());
				}

				/**
				 * {@inheritDoc}
				 * @see org.apache.parquet.io.api.PrimitiveConverter#addBoolean(boolean)
				 */
				@Override
				public void addBoolean(boolean value) {
					current.put(fieldName, value);
				}

				/**
				 * {@inheritDoc}
				 * @see org.apache.parquet.io.api.PrimitiveConverter#addDouble(double)
				 */
				@Override
				public void addDouble(double value) {
					current.put(fieldName, value);
				}

				/**
				 * {@inheritDoc}
				 * @see org.apache.parquet.io.api.PrimitiveConverter#addFloat(float)
				 */
				@Override
				public void addFloat(float value) {
					current.put(fieldName, value);
				}

				/**
				 * {@inheritDoc}
				 * @see org.apache.parquet.io.api.PrimitiveConverter#addInt(int)
				 */
				@Override
				public void addInt(int value) {
					current.put(fieldName, value);
				}

				/**
				 * {@inheritDoc}
				 * @see org.apache.parquet.io.api.PrimitiveConverter#addLong(long)
				 */
				@Override
				public void addLong(long value) {
					current.put(fieldName, value);
				}

			}

			class TimestampPrimitiveConverter extends SimplePrimitiveConverter {

				final ZoneId zoneDefault;

				long prev_time_in_nanos = -1;
				int prev_julian_day = -1;
				java.util.Date prev_date = null;

				TimestampPrimitiveConverter(String fieldName) {
					super(fieldName);
					this.zoneDefault = ZoneId.systemDefault();
				}

				/**
				 * {@inheritDoc}
				 * @see org.apache.parquet.io.api.PrimitiveConverter#addBinary(Binary)
				 */
				@Override
				public void addBinary(Binary bytes) {
					if (bytes==null)
						current.remove(fieldName);
					else {
						Preconditions.checkArgument(bytes.length() == 12, "Must be 12 bytes");
						ByteBuffer buf = bytes.toByteBuffer();
						buf.order(ByteOrder.LITTLE_ENDIAN);
						long time_in_nanos = buf.getLong();
						int julianDay = buf.getInt();

						if (prev_date!=null && prev_time_in_nanos==time_in_nanos && prev_julian_day==julianDay) {
							current.put(fieldName, prev_date);
							return;
						}

						LocalDate date = LocalDate.MIN.with(java.time.temporal.JulianFields.JULIAN_DAY, julianDay);
						int nanoOfSecond = (int)(time_in_nanos % 1_000_000_000L);
						long time_in_seconds = time_in_nanos / 1_000_000_000L;
						int seconds = (int)(time_in_seconds%60);
						time_in_seconds/=60;
						int minutes = (int)(time_in_seconds%60);
						int hours = (int)(time_in_seconds/60);
						LocalTime time = LocalTime.of(hours, minutes, seconds, nanoOfSecond);
						java.util.Date d = java.util.Date.from(LocalDateTime.of(date, time).atZone(zoneDefault).toInstant());
						current.put(fieldName, d);
						prev_date = d;
						prev_time_in_nanos = time_in_nanos;
						prev_julian_day = julianDay;
					}					  
				}

			}

		}

	}

	/**
	 * Implementation of parquet's InputFile for reading file contents in memory
	 */
	public static class InMemoryFileContents implements InputFile {

		private final byte[] contents;

		public InMemoryFileContents(byte[] contents) {
			this.contents = contents;
		}

		/*
		 * (non-Javadoc)
		 * @see org.apache.parquet.io.InputFile#getLength()
		 */
		@Override
		public long getLength() throws IOException {
			return contents.length;
		}

		/*
		 * (non-Javadoc)
		 * @see org.apache.parquet.io.InputFile#newStream()
		 */
		@Override
		public SeekableInputStream newStream() throws IOException {
			return new SeekableInputStream() {

				private int pos;

				@Override
				public int read() throws IOException {
					return (pos < contents.length) ? (contents[pos++] & 0xff) : -1;
				}

				@Override
				public void seek(long newPos) throws IOException {
					pos = (int)Math.min(Integer.MAX_VALUE, newPos);
				}

				@Override
				public void readFully(byte[] bytes, int start, int len) throws IOException {
					readReturningInt(bytes, start, len);
				}

				private int readReturningInt(byte[] bytes, int start, int len) throws IOException {
					if (contents == null) {
						throw new NullPointerException();
					} else if (start < 0 || len < 0 || len > contents.length - start) {
						throw new IndexOutOfBoundsException();
					}

					if (pos >= contents.length) {
						return -1;
					}

					int avail = contents.length - pos;
					if (len > avail) {
						len = avail;
					}
					if (len <= 0) {
						return 0;
					}
					System.arraycopy(contents, pos, bytes, start, len);
					pos += len;
					return len;
				}

				@Override
				public void readFully(ByteBuffer buf) throws IOException {
					read(buf);
				}

				@Override
				public void readFully(byte[] bytes) throws IOException {
					readFully(bytes, 0, bytes.length);
				}

				@Override
				public int read(ByteBuffer buf) throws IOException {
					if (buf.hasArray()) {
						int n = readReturningInt(buf.array(), buf.position(), buf.remaining());
						if (n > 0) {
							buf.position(buf.position() + n);
						}
						return n;
					} else {
						byte[] tmp = new byte[buf.remaining()];
						int n = readReturningInt(tmp, 0, tmp.length);
						if (n > 0) {
							buf.put(tmp, 0, n);
						}
						return n;
					}
				}

				@Override
				public long getPos() throws IOException {
					return pos;
				}
			};
		}

	}
}
