/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
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
