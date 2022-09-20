/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.validator.parsers;

import java.nio.file.Path;

import org.idb.cacao.api.templates.DocumentInput;

/**
 * An adapter for all classes that implements {@link FileParser} interface
 *
 * @author Rivelino Patrício
 * 
 * @since 05/03/2022
 *
 */
public abstract class FileParserAdapter implements FileParser {
	
	protected String CURRENT_LINE = "line";

	protected Path path;

	protected DocumentInput documentInputSpec;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.idb.cacao.validator.parsers.FileParser#getPath()
	 */
	@Override
	public Path getPath() {
		return path;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.idb.cacao.validator.parsers.FileParser#setPath(java.nio.file.Path)
	 */
	@Override
	public void setPath(Path path) {
		this.path = path;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.idb.cacao.validator.parsers.FileParser#getDocumentInputSpec()
	 */
	@Override
	public DocumentInput getDocumentInputSpec() {
		return documentInputSpec;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.idb.cacao.validator.parsers.FileParser#setDocumentInputSpec(org.idb.cacao
	 * .api.templates.DocumentInput)
	 */
	@Override
	public void setDocumentInputSpec(DocumentInput inputSpec) {
		this.documentInputSpec = inputSpec;
	}
}
