package org.idb.cacao.web.controllers.services;

import java.util.function.Supplier;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
/**
 * 
 * @author leon
 *
 */
@Service
public class FileUploadedProducer {
	@Bean
	public Supplier<String> supplierBean() {
		return () -> "File uploaded";
	}
}
