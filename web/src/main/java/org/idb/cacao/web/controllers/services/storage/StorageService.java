package org.idb.cacao.web.controllers.services.storage;

import java.io.InputStream;
import java.nio.file.Path;

import org.springframework.core.io.Resource;

public interface StorageService {

	Path store(String originalFilename, InputStream inputStream, boolean closeInputStream);

	Path find(String filename);
	
	Resource load(String filename);

}
