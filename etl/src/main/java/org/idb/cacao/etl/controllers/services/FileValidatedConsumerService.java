package org.idb.cacao.etl.controllers.services;

import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
public class FileValidatedConsumerService {

	@Bean
	public Consumer<String> consumerBean() {
		// Get the file, ETL and send message to ready topic
		return System.out::println;
	}
}
