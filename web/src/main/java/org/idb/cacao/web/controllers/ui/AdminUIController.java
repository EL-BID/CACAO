/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.ui;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.text.StringEscapeUtils;
import org.idb.cacao.web.utils.ControllerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller class for endpoints related to 'System Administration' information returned by a user interface and other administrative operations.
 * 
 * @author Gustavo Figueiredo
 *
 */
@Controller
public class AdminUIController {
	
	private static final Pattern IGNORE_STACKTRACE_LOG_LINE = Pattern.compile("^\\s*at (?>org\\.apache\\.|org\\.springframework\\.|org\\.thymeleaf\\.|javax\\.servlet|sun\\.reflect)");
	
	private static final String INVALID_CHARACTERS_PRE = "Invalid character found in the request target [";
	private static final String INVALID_CHARACTERS_POS = "]. The valid characters";
	
	private static final int MAX_LINE_LENGTH = 512;
	
	public static final int LOG_TAIL_MAX_LINES = 100;

	@Value("${presentation.mode}")
	private Boolean presentationMode;

	@Autowired
	private MessageSource messageSource;

	/**
	 * Returns user interface for administrative operations
	 */
	@Secured({"ROLE_ADMIN_OPS"})
	@GetMapping("/admin-shell")
	public String getAdminShell(Model model) {
		
		if (Boolean.TRUE.equals(presentationMode))
			return ControllerUtils.redirectToPresentationWarning(model, messageSource);

		return "admin/admin-shell";
		
	}
	
	public static String formatMemory(long amount, NumberFormat format) {
		if (amount<1024)
			return format.format(amount)+" bytes";
		double kbytes = ((double)amount)/1024.0;
		if (kbytes<1024)
			return format.format(kbytes)+" Kilobytes";
		double mbytes = ((double)kbytes)/1024.0;
		if (mbytes<1024)
			return format.format(mbytes)+" Megabytes";
		double gbytes = ((double)mbytes)/1024.0;
		return format.format(gbytes)+" Gigabytes";
	}
	
	/**
	 * Returns the current LOG directory configured for use with LOG4J (@see 'log4j2.xml')
	 */
	public static File getLogDir() {
		String logDir = System.getProperty("LOG_DIR");
		if (logDir!=null && logDir.trim().length()>0)
			return new File(logDir);
		File userHome = new File(System.getProperty("user.home"));
		return new File(userHome, "cacao_log");
	}

	/**
	 * Returns the last lines of LOG file. Removes some contents according to hardcoded filters.
	 */
	public static String getLogTail(final int max_lines,
			final boolean treatLine,
			final Pattern search,
			final Pattern avoid,
			final Date minimumTimestamp,
			final Date maximumTimestamp, 
			final Integer vicinity) {
		// Get the LOG directory (according to system properties)
		File logDir = getLogDir();
		if (logDir==null)
			return null;

		// If we have a 'minimumTimestamp' constraint, we should not care about files
		// created or changed in the 'previous day' before the provided timestamp, because
		// the LOG files are daily.
		final Date minDayBefore;
		if (minimumTimestamp==null) {
			minDayBefore = null;
		}
		else {
			Calendar c = Calendar.getInstance(Locale.getDefault());
			c.setTime(minimumTimestamp);
			c.set(Calendar.MILLISECOND, 0);
			c.set(Calendar.SECOND, 0);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.HOUR_OF_DAY, 0);
			c.add(Calendar.DAY_OF_YEAR, -1);
			minDayBefore = c.getTime();
		}

		// If we have a 'maximumTimestamp' constraint, we should not care about files
		// created or changed in the 'next day' after the provided timestamp, because
		// the LOG files are daily.
		final Date maxDayAfter;
		if (maximumTimestamp==null) {
			maxDayAfter = null;
		}
		else {
			Calendar c = Calendar.getInstance(Locale.getDefault());
			c.setTime(maximumTimestamp);
			c.set(Calendar.MILLISECOND, 0);
			c.set(Calendar.SECOND, 0);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.HOUR_OF_DAY, 0);
			c.add(Calendar.DAY_OF_YEAR, 1);
			maxDayAfter = c.getTime();
		}
		
		// Get the last modified file in LOG directory
		final File[] logFiles = logDir.listFiles(new FileFilter() {
			
			@Override
			public boolean accept(File f) {
				long t = f.lastModified();
				if (minimumTimestamp!=null) {
					if (t<minDayBefore.getTime())
						return false;					
				}
				if (maximumTimestamp!=null) {
					if (t>maxDayAfter.getTime())
						return false;
				}
				return f.isFile();
			}
		});
		if (logFiles==null || logFiles.length==0)
			return null;
		
		// Put files in reverse chronological order
		Arrays.sort(logFiles, new Comparator<File>() {
			public int compare(File f1, File f2) {
				long t1 = f1.lastModified();
				long t2 = f2.lastModified();
				if (t1<t2) return 1;
				if (t1>t2) return -1;
				return f2.getName().compareTo(f1.getName());
			}
		});
		
		final SimpleDateFormat logSdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.US);
		final Pattern pSdf = Pattern.compile("^\\d{2}-[^-]{3}-\\d{4} \\d{2}:\\d{2}:\\d{2}");
		
		// The following structure is only used during 'search' with a 'Pattern' and when it's desired to include additional rows in the 'vicinity' of each match
		final class CircularBuffer {
			final String[] lines; // lines to keep at this circular buffer
			int index; // present cursor into this circular buffer
			boolean full; // indication that lines is full filled
			CircularBuffer(int capacity) {
				this.lines = new String[capacity];
			}
			/**
			 * Stores temporarily a line in this circular buffer. This may be included in the response
			 * if there is a match in the vicinity of this line.
			 */
			void add(String line) {
				lines[index] = line;
				index++;
				if (index==lines.length) {
					index = 0; 
					full = true;
				}
			}
			/**
			 * Writes into 'sink' all the lines stored in this circular buffer and clear this buffer afterwards.
			 */
			void flush(List<String> sink) {
				int firstPos = (full) ? index : 0;
				int num = (full) ? lines.length : index;
				for (int i=0; i<num; i++) {
					int pos = (firstPos+i)%lines.length;
					String line = lines[pos];
					sink.add(line);
				}
				clear();
			}
			/**
			 * Indication that we have stored nothing in this circular buffer
			 */
			boolean isEmpty() {
				return !full && index==0;
			}
			/**
			 * Clears this circular buffer
			 */
			void clear() {
				index = 0;
				full = false;
			}
			/**
			 * The capacity of this circular buffer
			 */
			int capacity() {
				return lines.length;
			}
		}
		final CircularBuffer circularBuffer = (search!=null && vicinity!=null && vicinity.intValue()>1) ? new CircularBuffer(vicinity.intValue()/2) : null;
		int countLinesAfterMatch = 0;
		final int include_lines_after_match = (search!=null && vicinity!=null) ? vicinity.intValue() - (circularBuffer==null?0:circularBuffer.capacity()) : 0;
		
		// Read the last file contents
		List<String> lines = new LinkedList<>();
		try {
			for (File file: logFiles) {
				boolean ignoredPrevStackTraceLine = false;
				boolean seekingTimestamp = false;
				boolean printBoundary = false;
				try (ReversedLinesFileReader reader = new ReversedLinesFileReader(file, StandardCharsets.UTF_8);) {
					String line;
					while ((line = reader.readLine())!=null) {
						if (IGNORE_STACKTRACE_LOG_LINE.matcher(line).find()) {
							ignoredPrevStackTraceLine = true;
							continue;
						}
						final boolean looking_for_timestamps = (minimumTimestamp!=null) || (maximumTimestamp!=null && seekingTimestamp);
						if (looking_for_timestamps) {
							Date timestampFromLogLine;
							Matcher m_sdf = pSdf.matcher(line);
							if (!m_sdf.find()) {
								timestampFromLogLine = null;
							}
							else {
								try {
									timestampFromLogLine = logSdf.parse(m_sdf.group());
								} catch (ParseException e) {
									timestampFromLogLine = null;
								}
							}
							if (minimumTimestamp!=null) {
								if (timestampFromLogLine!=null && timestampFromLogLine.before(minimumTimestamp))
									break;
							}
							if (maximumTimestamp!=null && seekingTimestamp) {
								if (timestampFromLogLine==null)
									continue;
								if (timestampFromLogLine.after(maximumTimestamp))
									continue;
								seekingTimestamp = true;
							}
						}
						String treatedLine;
						if (treatLine) {
							treatedLine = treatLine(line);
						}
						else {
							treatedLine = StringEscapeUtils.escapeHtml4(line);		// escapes all HTML characters (including '<', '>') to avoid HTML injection when printing
						}
						// If we are avoiding some lines according to a pattern...
						if (avoid!=null && avoid.matcher(line).find()) {
							continue;
						}
						// If we are looking for a match (or for the vicinity of a match)...
						if (search!=null) {
							// Check if we got a match
							if (!search.matcher(line).find()) {
								// If we didn't get a match, let's check if we are in the desired vicinity
								// before the match
								// Note that we are reading lines backwards, so the rows 'before the match'
								// are actually the lines we are reading after the match.
								if (countLinesAfterMatch<include_lines_after_match) {
									countLinesAfterMatch++;
									if (countLinesAfterMatch==include_lines_after_match)
										printBoundary = true;
								}
								else {
									if (printBoundary) {
										printBoundary = false;
										lines.add("----------------------"); // print a 'boundary'
									}
									if (circularBuffer!=null)
										circularBuffer.add(treatedLine);
									continue;
								}
							}
							else {
								// If we got a match, let's flush whatever we got in the vicinity after the match (circular buffer)								
								// Note that we are reading lines backwards, so the 'circular buffer' corresponds to the rows that comes 'after'
								// the match
								if (circularBuffer!=null && !circularBuffer.isEmpty()) {
									circularBuffer.flush(lines);
								}
								// Reset the counter for considering the other rows before the match
								countLinesAfterMatch = 0;
							}
						}
						if (ignoredPrevStackTraceLine) {
							ignoredPrevStackTraceLine = false;
							lines.add("\t...");
						}
						lines.add(treatedLine);
						if (lines.size()>=max_lines)
							break;
					} // LOOP for each line
				}
				if (lines.size()>=max_lines)
					break;
			} // LOOP for each file
		} catch (IOException e) {
			return null;
		}
		
		Collections.reverse(lines);
		return String.join("\n", lines);
	}
	
	/**
	 * Removes invalid contents from logged lines before presenting it to screen (i.e. avoid something that could
	 * be used as 'script injection' or could somehow overflow the screen with too much information).
	 */
	public static String treatLine(String line) {
		int t1 = line.indexOf(INVALID_CHARACTERS_PRE);		// replaces offending URL by dots
		if (t1>=0) {
			t1 += INVALID_CHARACTERS_PRE.length();
			int t2 = line.indexOf(INVALID_CHARACTERS_POS, t1);
			if (t2<0)
				line = line.substring(0, t1)+"...";
			else
				line = line.substring(0, t1)+"..."+line.substring(t2);
		}
		if (line.length()>MAX_LINE_LENGTH) {
			line = line.substring(0, MAX_LINE_LENGTH)+"...";	// truncates long lines
		}
		line = StringEscapeUtils.escapeHtml4(line);		// escapes all HTML characters (including '<', '>') to avoid HTML injection when printing
		line = line.replaceAll("[^\\x20-\\x7e]", ".");	// replaces all non-UTF8-printable characters by dots
		return line;
	}
}
