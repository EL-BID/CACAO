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

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;

import org.idb.cacao.api.errors.GeneralException;

/**
 * Utility class designated to consume contents of a Zip File with some more control (e.g. avoid expanding
 * too much data).
 * 
 * @author Gustavo Figueiredo
 *
 */
public class ZipConsumer {
	
	private final ZipEntrySupplier zipEntrySupplier;
	
	private final ConsumeContents consumeContents;
	
	private final InputStream contentsInputStream;
	
	private int threadholdEntries;
	
	private int totalEntryArchive;
	
	public ZipConsumer(ZipEntrySupplier zipEntrySupplier, InputStream contentsInputStream, ConsumeContents consumeContents) {
		this.zipEntrySupplier = zipEntrySupplier;
		this.contentsInputStream = contentsInputStream;
		this.consumeContents = consumeContents;
		this.totalEntryArchive = 0;
	}
	
	public int getThreadholdEntries() {
		return threadholdEntries;
	}

	public void setThreadholdEntries(int threadholdEntries) {
		this.threadholdEntries = threadholdEntries;
	}
	
	public ZipConsumer threadholdEntries(int threadholdEntries) {
		setThreadholdEntries(threadholdEntries);
		return this;
	}

	public void run() throws IOException, GeneralException {
		ZipEntry ze;

		while ((ze = zipEntrySupplier.getEntry()) != null) {
			
			totalEntryArchive++;
			// Guard against too many zip entries
			if (threadholdEntries>0 && totalEntryArchive>threadholdEntries) {
				throw new GeneralException(String.format("Too much entries in ZIP file (limit: %d). Ignoring the exceeding entries.",threadholdEntries));
			}

			consumeContents.consume(ze, contentsInputStream);
		}
		
	}

	public int getTotalEntryArchive() {
		return totalEntryArchive;
	}

	@FunctionalInterface
	public static interface ZipEntrySupplier {
		
		public ZipEntry getEntry() throws IOException;
		
	}
	
	@FunctionalInterface
	public static interface ConsumeContents {
		
		public void consume(ZipEntry entry, InputStream contentsInputStream) throws IOException;
		
	}
}
