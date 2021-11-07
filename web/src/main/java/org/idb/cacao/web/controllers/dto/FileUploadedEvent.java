package org.idb.cacao.web.controllers.dto;

import org.springframework.stereotype.Component;

@Component
public class FileUploadedEvent {
	private String fileId;

		
	
	public String getFileId() {
		return fileId;
	}

	public void setFileId(String fileId) {
		this.fileId = fileId;
	}
}
