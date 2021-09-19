package org.idb.cacao.web.controllers.services;

import java.util.function.Supplier;

import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

/**
 * Spring boot stream service to send messages to ETL of 
 * files to be processed
 * 
 * @author leon
 *
 */
@Service
public class FileProducerService {
	
	@Bean
	public Supplier<Message<String>> sendBookkeepingFile() {
		return () -> {
			return MessageBuilder.withPayload("019203192.json received").build();
		};
	}

}
