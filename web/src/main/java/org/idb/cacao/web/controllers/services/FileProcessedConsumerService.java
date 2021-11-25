package org.idb.cacao.web.controllers.services;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

/**
 * 
 * @author leon
 *
 */
@Service
public class FileProcessedConsumerService {

	private static final Logger log = Logger.getLogger(FileProcessedConsumerService.class.getName());

	@Bean
	public Consumer<String> receiveProcessedFile() {
		return documentId -> {
			log.log(Level.INFO, "Received a message with documentId " + documentId);			
		};
	}

	
	/**
	 * Try to rollback any transactions that wasn't finished correctly 
	 * 
	 * @param rollbackProcedures	A list ou {@link Runnable} with data to be rolled back.
	 */
	public static void callRollbackProcedures(Collection<Runnable> rollbackProcedures) {
		if (rollbackProcedures==null || rollbackProcedures.isEmpty())
			return;
		for (Runnable proc: rollbackProcedures) {
			try {
				proc.run();
			}
			catch (Throwable ex) {
				//TODO Add logging
				log.log(Level.SEVERE, "Could not rollback", ex);
			}
		}
	}
	
	
}
