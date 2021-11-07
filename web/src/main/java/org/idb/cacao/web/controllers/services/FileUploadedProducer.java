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
package org.idb.cacao.web.controllers.services;

import org.idb.cacao.web.controllers.dto.FileUploadedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
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
	
	@Autowired
	private final StreamBridge streamBridge;
	

/*
	public Supplier<String> supplierBean() {
		return () -> "File uploaded";
	}
*/
	
	public FileUploadedProducer(StreamBridge streamBridge) {
		this.streamBridge = streamBridge;
	}
	/*	
	@Bean
	public Supplier<String> fileUploaded(FileUploadedEvent fileEvent) {
		return () -> fileEvent.getFileId();
	}
	*/

	public void fileUploaded(FileUploadedEvent fileEvent) {
		System.out.println("MANDANDO MENSAGEM");
        streamBridge.send("fileUploaded-out-0", fileEvent.getFileId());
    }

}
