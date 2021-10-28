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
package org.idb.cacao.web;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.OffsetDateTime;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.idb.cacao.web.entities.DocumentTemplate;
import org.idb.cacao.web.utils.ParserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.StandardServletEnvironment;

@Component
public class IncomingFileStorage {

	private static final Logger log = Logger.getLogger(IncomingFileStorage.class.getName());

	@Autowired
	Environment env;
	
	private static Boolean is_windows;
	
	private static volatile long prev_timestamp;
	
	public static boolean isWindows() {
		if (is_windows!=null)
			return is_windows.booleanValue();
		String operSys = System.getProperty("os.name").toLowerCase();
		is_windows = (operSys.contains("win"));
		return is_windows;
	}
	
	public static synchronized long getIncreasingLongNumber() {
		long curr_timestamp = System.currentTimeMillis();
		if (curr_timestamp<=prev_timestamp) {
			return ++prev_timestamp;
		}
		else {
			prev_timestamp = curr_timestamp;
			return curr_timestamp;
		}
	}
	
	public File getIncomingDirOriginalFiles() throws IOException {
		String prop_name = "storage.incoming.files.original."+(isWindows()?"win":"nix");
		String prop = env.getProperty(prop_name);
		if (prop==null || prop.trim().length()==0)
			throw new IOException("Missing property: "+prop_name);
		return new File(prop.trim());
	}

	public File getTemplateDir() throws IOException {
		String prop_name = "storage.template.files."+(isWindows()?"win":"nix");
		String prop = env.getProperty(prop_name);
		if (prop==null || prop.trim().length()==0)
			throw new IOException("Missing property: "+prop_name);
		return new File(prop.trim());
	}

	public File getSampleDir() throws IOException {
		String prop_name = "storage.sample.files."+(isWindows()?"win":"nix");
		String prop = env.getProperty(prop_name);
		if (prop==null || prop.trim().length()==0)
			throw new IOException("Missing property: "+prop_name);
		return new File(prop.trim());
	}

	public File getGenericFileDir() throws IOException {
		String prop_name = "storage.generic.files."+(isWindows()?"win":"nix");
		String prop = env.getProperty(prop_name);
		if (prop==null || prop.trim().length()==0)
			throw new IOException("Missing property: "+prop_name);
		return new File(prop.trim());
	}

	public File getIncomingDirXHTMLFiles() throws IOException {
		String prop_name = "storage.incoming.files.xhtml."+(isWindows()?"win":"nix");
		String prop = env.getProperty(prop_name);
		if (prop==null || prop.trim().length()==0)
			throw new IOException("Missing property: "+prop_name);
		return new File(prop.trim());
	}

	public File saveOriginalFile(String filename, InputStream inputStream) throws IOException {
		
		return saveOriginalFile(filename, inputStream, /*closeInputStream*/true);
		
	}
	
	public File saveOriginalFile(String filename, InputStream inputStream, boolean closeInputStream) throws IOException {
		
		// Target directory for storing original files, partitioned by year/month/day
		File output_dir = getIncomingDirOriginalFiles();
		Calendar cal = Calendar.getInstance();
		File year_dir = new File(output_dir, String.format("%04d",cal.get(Calendar.YEAR)));
		File month_dir = new File(year_dir, String.format("%02d",cal.get(Calendar.MONTH)+1));
		File day_dir = new File(month_dir, String.format("%02d",cal.get(Calendar.DAY_OF_MONTH)));
		if (!day_dir.exists())
			day_dir.mkdirs();
		String number = String.format("%014d",getIncreasingLongNumber());
		
		// Choose a proper filename (keep only the sufix for original file)
		File incoming_file = new File(filename);
		String incoming_filename = incoming_file.getName();
		int suffix_sep = incoming_filename.lastIndexOf('.');
		String suffix = (suffix_sep>0) ? Pattern.compile("[^\\.A-Za-z\\d_]").matcher(incoming_filename.substring(suffix_sep)).replaceAll("") : null;
		if (suffix!=null && suffix.length()>6)
			suffix = suffix.substring(0, 6);
		String output_filename = (suffix!=null) ? number+suffix : number;
		File output_file =   new File(day_dir, output_filename);
		
		log.log(Level.FINE, "Creating local file "+output_file.getAbsolutePath()+" for incoming file (upload): "+filename);

		try {
			Files.copy(
					inputStream, 
					output_file.toPath(), 
		      StandardCopyOption.REPLACE_EXISTING);			 
		}
		finally {
			if (closeInputStream) {
				IOUtils.closeQuietly(inputStream);
			}
		}

		return output_file;
	}

	public File getOriginalFile(String filename, OffsetDateTime timestamp) throws IOException {
		
		// Target directory for storing original files, partitioned by year/month/day
		File output_dir = getIncomingDirOriginalFiles();
		File year_dir = new File(output_dir, String.format("%04d",timestamp.getYear()));
		File month_dir = new File(year_dir, String.format("%02d",timestamp.getMonthValue()));
		File day_dir = new File(month_dir, String.format("%02d",timestamp.getDayOfMonth()));
		if (!day_dir.exists())
			return null;
		
		if (filename!=null && filename.trim().length()>0) {
			File file = new File(day_dir, filename);
			if (file.exists())
				return file;
		}

		return null;
	}
	
	/**
	 * Lists all original files stored in a time range
	 * @param startingTimestamp Starting date/time (in unix epoch)
	 * @param endingTimestamp Ending date/time (in unix epoch)
	 * @param interrupt If different than NULL, the provided function may return TRUE if it should interrupt
	 * @param consumer Callback for each file
	 */
	public void listOriginalFiles(
			final long startingTimestamp, 
			final long endingTimestamp, 
			final BooleanSupplier interrupt,
			final Consumer<File> consumer) throws IOException {
		File output_dir = getIncomingDirOriginalFiles();
		if (!output_dir.exists())
			return;
		
		final Calendar cal = Calendar.getInstance();
		
		cal.setTimeInMillis(startingTimestamp);
		final int start_year = cal.get(Calendar.YEAR);
		final int start_month = cal.get(Calendar.MONTH)+1;
		final int start_day = cal.get(Calendar.DAY_OF_MONTH);
		
		cal.setTimeInMillis(endingTimestamp);
		final int end_year = cal.get(Calendar.YEAR);
		final int end_month = cal.get(Calendar.MONTH)+1;
		final int end_day = cal.get(Calendar.DAY_OF_MONTH);
		
		final LongAdder inc_hierarchy = new LongAdder();
		final AtomicInteger searching_year = new AtomicInteger();
		final AtomicInteger searching_month = new AtomicInteger();
		final AtomicInteger searching_day = new AtomicInteger();
		Files.walkFileTree(output_dir.toPath(), new FileVisitor<Path>() {
			// Called after a directory visit is complete.
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
            	inc_hierarchy.decrement();
            	return FileVisitResult.CONTINUE;
            }
            // called before a directory visit.
            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                    BasicFileAttributes attrs) throws IOException {
            	if (interrupt!=null && interrupt.getAsBoolean())
            		return FileVisitResult.TERMINATE;
            	File subdir = dir.toFile();
            	String subdir_name = subdir.getName();
            	final int hierarchy_level = inc_hierarchy.intValue();
            	switch (hierarchy_level) {
        			case 0: // first level: the root dir
        				inc_hierarchy.increment();
        				return FileVisitResult.CONTINUE;        				
            		case 1: // second level: year of date/time of upload
            			if (!ParserUtils.isOnlyNumbers(subdir_name))
            				return FileVisitResult.SKIP_SUBTREE;
            			else {
            				int year = Integer.parseInt(subdir_name);
            				if (year<start_year || year>end_year)
            					return FileVisitResult.SKIP_SUBTREE;
            				searching_year.set(year);
            				inc_hierarchy.increment();
            				return FileVisitResult.CONTINUE;
            			}
            		case 2: // third level: month of date/time of upload
            			if (!ParserUtils.isOnlyNumbers(subdir_name))
            				return FileVisitResult.SKIP_SUBTREE;
            			else {
            				int month = Integer.parseInt(subdir_name);
            				if (month<1 || month>12)
            					return FileVisitResult.SKIP_SUBTREE;
            				if (searching_year.get()==start_year && month<start_month)
            					return FileVisitResult.SKIP_SUBTREE;
            				if (searching_year.get()==end_year && month>end_month)
            					return FileVisitResult.SKIP_SUBTREE;
            				searching_month.set(month);
            				inc_hierarchy.increment();
            				return FileVisitResult.CONTINUE;
            			}
            		case 3: // forth level: day of date/time of upload
            			if (!ParserUtils.isOnlyNumbers(subdir_name))
            				return FileVisitResult.SKIP_SUBTREE;
            			else {
            				int day = Integer.parseInt(subdir_name);
            				if (day<1 || day>31)
            					return FileVisitResult.SKIP_SUBTREE;
            				if (searching_year.get()==start_year && searching_month.get()==start_month 
            						&& day<start_day)
            					return FileVisitResult.SKIP_SUBTREE;
            				if (searching_year.get()==end_year && searching_month.get()==end_month
            						&& day>end_day)
            					return FileVisitResult.SKIP_SUBTREE;
            				searching_day.set(day);
            				inc_hierarchy.increment();
            				return FileVisitResult.CONTINUE;
            			}
            		default: // fifth level and beyond: don't care about subdirs
            			return FileVisitResult.SKIP_SUBTREE;
            	}
            }
            // This method is called for each file visited. The basic attributes of the files are also available.
            @Override
            public FileVisitResult visitFile(Path file,
                    BasicFileAttributes attrs) throws IOException {
            	if (interrupt!=null && interrupt.getAsBoolean())
            		return FileVisitResult.TERMINATE;
            	File f = file.toFile();
            	long file_timestamp = f.lastModified();
            	if (file_timestamp>=startingTimestamp && file_timestamp<endingTimestamp) {
            		consumer.accept(f);
            	}
            	return FileVisitResult.CONTINUE;
            }
            // if the file visit fails for any reason, the visitFileFailed method is called.
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc)
                    throws IOException {
            	return FileVisitResult.CONTINUE;
            }
		});
	}

	public File saveTemplateFile(DocumentTemplate template, String filename, InputStream inputStream) throws IOException {
		
		// Target directory for storing template files
		File output_dir = getTemplateDir();
		if (!output_dir.exists())
			output_dir.mkdirs();
		String template_id = template.getId();
		
		// Choose a proper filename (keep only the sufix for original file)
		File incoming_file = new File(filename);
		String incoming_filename = incoming_file.getName();
		int suffix_sep = incoming_filename.lastIndexOf('.');
		String suffix = (suffix_sep>0) ? Pattern.compile("[^\\.A-Za-z\\d_]").matcher(incoming_filename.substring(suffix_sep)).replaceAll("") : null;
		if (suffix!=null && suffix.length()>6)
			suffix = suffix.substring(0, 6);
		String output_filename = (suffix!=null) ? template_id+suffix : template_id;
		File output_file =   new File(output_dir, output_filename);
		
		log.log(Level.FINE, "Creating local file "+output_file.getAbsolutePath()+" for template file (upload): "+filename);

		try {
			Files.copy(
					inputStream, 
					output_file.toPath(), 
		      StandardCopyOption.REPLACE_EXISTING);			 
		}
		finally {
			IOUtils.closeQuietly(inputStream);
		}

		return output_file;
	}
	
	public File getTemplateFile(DocumentTemplate template) throws IOException {
		
		// Target directory for storing template files
		File output_dir = getTemplateDir();
		if (!output_dir.exists())
			return null;
		
		if (template.getFilename()!=null && template.getFilename().trim().length()>0) {
			File file = new File(output_dir, template.getFilename());
			if (file.exists())
				return file;
		}

		return null;
	}

	/**
	 * Lists all template files created or updated in a time range
	 * @param startingTimestamp Starting date/time (in unix epoch)
	 * @param endingTimestamp Ending date/time (in unix epoch)
	 * @param interrupt If different than NULL, the provided function may return TRUE if it should interrupt
	 * @param consumer Callback for each file
	 */
	public void listTemplateFiles(
			final long startingTimestamp, 
			final long endingTimestamp, 
			final BooleanSupplier interrupt,
			final Consumer<File> consumer) throws IOException {
		File output_dir = getTemplateDir();
		if (!output_dir.exists())
			return;
		
		final AtomicBoolean inside_dir = new AtomicBoolean(false);
		Files.walkFileTree(output_dir.toPath(), new FileVisitor<Path>() {
			// Called after a directory visit is complete.
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
            	inside_dir.set(false);
            	return FileVisitResult.CONTINUE;
            }
            // called before a directory visit.
            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                    BasicFileAttributes attrs) throws IOException {
            	if (interrupt!=null && interrupt.getAsBoolean())
            		return FileVisitResult.TERMINATE;
            	if (inside_dir.get())
            		return FileVisitResult.SKIP_SUBTREE; // ignore all subdirs in templates dir
            	// enter in template dir
            	inside_dir.set(true);
            	return FileVisitResult.CONTINUE;
            }
            // This method is called for each file visited. The basic attributes of the files are also available.
            @Override
            public FileVisitResult visitFile(Path file,
                    BasicFileAttributes attrs) throws IOException {
            	if (interrupt!=null && interrupt.getAsBoolean())
            		return FileVisitResult.TERMINATE;
            	File f = file.toFile();
            	long file_timestamp = f.lastModified();
            	if (file_timestamp>=startingTimestamp && file_timestamp<endingTimestamp) {
            		consumer.accept(f);
            	}
            	return FileVisitResult.CONTINUE;
            }
            // if the file visit fails for any reason, the visitFileFailed method is called.
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc)
                    throws IOException {
            	return FileVisitResult.CONTINUE;
            }
		});
	}

	public File saveSampleFile(DocumentTemplate template, String filename, InputStream inputStream) throws IOException {
		
		// Target directory for storing sample files
		File output_dir = getSampleDir();
		if (!output_dir.exists())
			output_dir.mkdirs();
		String template_id = template.getId();
		
		// Choose a proper filename (keep only the sufix for original file)
		File incoming_file = new File(filename);
		String incoming_filename = incoming_file.getName();
		int suffix_sep = incoming_filename.lastIndexOf('.');
		String suffix = (suffix_sep>0) ? Pattern.compile("[^\\.A-Za-z\\d_]").matcher(incoming_filename.substring(suffix_sep)).replaceAll("") : null;
		if (suffix!=null && suffix.length()>6)
			suffix = suffix.substring(0, 6);
		String output_filename = (suffix!=null) ? template_id+suffix : template_id;
		File output_file =   new File(output_dir, output_filename);
		
		log.log(Level.FINE, "Creating local file "+output_file.getAbsolutePath()+" for sample file (upload): "+filename);

		try {
			Files.copy(
					inputStream, 
					output_file.toPath(), 
		      StandardCopyOption.REPLACE_EXISTING);			 
		}
		finally {
			IOUtils.closeQuietly(inputStream);
		}

		return output_file;
	}

	public File getSampleFile(DocumentTemplate template) throws IOException {
		
		// Target directory for storing sample files
		File output_dir = getSampleDir();
		if (!output_dir.exists())
			return null;
		
		if (template.getSample()!=null && template.getSample().trim().length()>0) {
			File file = new File(output_dir, template.getSample());
			if (file.exists())
				return file;
		}

		return null;
	}

	/**
	 * Lists all sample files created or updated in a time range
	 * @param startingTimestamp Starting date/time (in unix epoch)
	 * @param endingTimestamp Ending date/time (in unix epoch)
	 * @param interrupt If different than NULL, the provided function may return TRUE if it should interrupt
	 * @param consumer Callback for each file
	 */
	public void listSampleFiles(
			final long startingTimestamp, 
			final long endingTimestamp, 
			final BooleanSupplier interrupt,
			final Consumer<File> consumer) throws IOException {
		File output_dir = getSampleDir();
		if (!output_dir.exists())
			return;
		
		final AtomicBoolean inside_dir = new AtomicBoolean(false);
		Files.walkFileTree(output_dir.toPath(), new FileVisitor<Path>() {
			// Called after a directory visit is complete.
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
            	inside_dir.set(false);
            	return FileVisitResult.CONTINUE;
            }
            // called before a directory visit.
            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                    BasicFileAttributes attrs) throws IOException {
            	if (interrupt!=null && interrupt.getAsBoolean())
            		return FileVisitResult.TERMINATE;
            	if (inside_dir.get())
            		return FileVisitResult.SKIP_SUBTREE; // ignore all subdirs in samples dir
            	// enter in sample dir
            	inside_dir.set(true);
            	return FileVisitResult.CONTINUE;
            }
            // This method is called for each file visited. The basic attributes of the files are also available.
            @Override
            public FileVisitResult visitFile(Path file,
                    BasicFileAttributes attrs) throws IOException {
            	if (interrupt!=null && interrupt.getAsBoolean())
            		return FileVisitResult.TERMINATE;
            	File f = file.toFile();
            	long file_timestamp = f.lastModified();
            	if (file_timestamp>=startingTimestamp && file_timestamp<endingTimestamp) {
            		consumer.accept(f);
            	}
            	return FileVisitResult.CONTINUE;
            }
            // if the file visit fails for any reason, the visitFileFailed method is called.
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc)
                    throws IOException {
            	return FileVisitResult.CONTINUE;
            }
		});
	}
	
	public File saveXHTMLFiles(String prefixFileName, String... contents) throws IOException {
		
		if (contents==null || contents.length==0)
			return null;
		
		// Target directory for storing XHTML contents, partitioned by year/month/day
		File output_dir = getIncomingDirXHTMLFiles();
		Calendar cal = Calendar.getInstance();
		File year_dir = new File(output_dir, String.format("%04d",cal.get(Calendar.YEAR)));
		File month_dir = new File(year_dir, String.format("%02d",cal.get(Calendar.MONTH)+1));
		File day_dir = new File(month_dir, String.format("%02d",cal.get(Calendar.DAY_OF_MONTH)));
		if (!day_dir.exists())
			day_dir.mkdirs();

		int suffix_sep = prefixFileName.lastIndexOf('.');
		if (suffix_sep>0)
			prefixFileName = prefixFileName.substring(0, suffix_sep);
		
		byte[] transfer_buffer = new byte[10000];
		String zip_filename = prefixFileName+".ZIP";
		File zip_file = new File(day_dir, zip_filename);
		
		log.log(Level.FINE, "Creating local file "+zip_file.getAbsolutePath()+" for processed file");

		try (FileOutputStream dest = new FileOutputStream(zip_file);
			CheckedOutputStream checksum = new CheckedOutputStream(dest, new Adler32());
			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(checksum));) {

			for (int seq=0; seq<contents.length; seq++) {
				String c = contents[seq];
				if (c==null || c.length()==0)
					continue;			
				
				// Choose a proper filename (keeps prefix, may include sequential sufix)
				String name = (seq==0) ? prefixFileName+".XHTML" : prefixFileName+"."+(1+seq)+".XHTML";
				
				ZipEntry entry = new ZipEntry(name);
				out.putNextEntry(entry);

				try (InputStream inputStream=new ByteArrayInputStream(c.getBytes(StandardCharsets.UTF_8))) {
					
					int length;
					while ((length=inputStream.read(transfer_buffer))!=-1) {
						out.write(transfer_buffer, 0, length);
					}
					
				}
				
				out.closeEntry();
				out.flush();

			}
			
			out.finish();
		}
		
		return zip_file;
	}
	
	/**
	 * Lists all Generic files created or updated in a time range
	 * @param startingTimestamp Starting date/time (in unix epoch)
	 * @param endingTimestamp Ending date/time (in unix epoch)
	 * @param interrupt If different than NULL, the provided function may return TRUE if it should interrupt
	 * @param consumer Callback for each file
	 */
	public void listGenericFiles(
			final long startingTimestamp, 
			final long endingTimestamp, 
			final BooleanSupplier interrupt,
			final Consumer<File> consumer) throws IOException {
		File output_dir = getGenericFileDir();
		if (!output_dir.exists())
			return;
		
		final AtomicBoolean inside_dir = new AtomicBoolean(false);
		Files.walkFileTree(output_dir.toPath(), new FileVisitor<Path>() {
			// Called after a directory visit is complete.
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
            	inside_dir.set(false);
            	return FileVisitResult.CONTINUE;
            }
            // called before a directory visit.
            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                    BasicFileAttributes attrs) throws IOException {
            	if (interrupt!=null && interrupt.getAsBoolean())
            		return FileVisitResult.TERMINATE;
            	if (inside_dir.get())
            		return FileVisitResult.SKIP_SUBTREE; // ignore all subdirs in Generics dir
            	// enter in Generic dir
            	inside_dir.set(true);
            	return FileVisitResult.CONTINUE;
            }
            // This method is called for each file visited. The basic attributes of the files are also available.
            @Override
            public FileVisitResult visitFile(Path file,
                    BasicFileAttributes attrs) throws IOException {
            	if (interrupt!=null && interrupt.getAsBoolean())
            		return FileVisitResult.TERMINATE;
            	File f = file.toFile();
            	long file_timestamp = f.lastModified();
            	if (file_timestamp>=startingTimestamp && file_timestamp<endingTimestamp) {
            		consumer.accept(f);
            	}
            	return FileVisitResult.CONTINUE;
            }
            // if the file visit fails for any reason, the visitFileFailed method is called.
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc)
                    throws IOException {
            	return FileVisitResult.CONTINUE;
            }
		});
	}	
	
	public IncomingFileStorage forTesting() {
		
		env = new StandardServletEnvironment();
		
		return this;
	}
}
