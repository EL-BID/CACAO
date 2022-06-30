/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.services;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.idb.cacao.web.conf.StreamConfiguration;
import org.idb.cacao.web.dto.FileUploadedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
/**
 * 
 * @author leon
 *
 */
@Component
@Service
public class FileUploadedProducer {
	
	private static final Logger log = Logger.getLogger(FileUploadedProducer.class.getName());
	
	@Autowired
	private final StreamBridge streamBridge;
	
	public FileUploadedProducer(StreamBridge streamBridge) {
		this.streamBridge = streamBridge;
	}

	public void fileUploaded(FileUploadedEvent fileEvent) {
		fileUploaded(fileEvent, null);
	}
	
	public void fileUploaded(FileUploadedEvent fileEvent, Integer partition) {
		log.log(Level.INFO, "Sending a message with documentId " + fileEvent.getFileId());
		
		if (partition==null) {
	        streamBridge.send("fileUploaded-out-0", fileEvent.getFileId());			
		}
		else {
			Message<?> msg = MessageBuilder.withPayload(fileEvent.getFileId()).setHeader(StreamConfiguration.HEADER_PARTITION, partition).build();
	        streamBridge.send("fileUploaded-out-0", msg);
		}
    }

	

}
