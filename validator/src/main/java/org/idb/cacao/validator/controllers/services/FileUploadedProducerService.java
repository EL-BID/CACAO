package org.idb.cacao.validator.controllers.services;

import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
public class FileUploadedProducerService {

	@Bean
	public Function<String, String> processorBean() {
		return String::toUpperCase;
	}
	
}
