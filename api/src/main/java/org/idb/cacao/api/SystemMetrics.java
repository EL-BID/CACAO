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
package org.idb.cacao.api;

import static org.springframework.data.elasticsearch.annotations.FieldType.*;

import java.io.Serializable;
import java.time.OffsetDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Metrics collected from the underlying running system. Each service should subclass this entity
 * and provide a specific index name.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class SystemMetrics implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * ID's are being generated automatically.
	 * PS: Elasticsearch generates by default 20 character long ID's, that are both URL-safe, base 64 encoded GUID
	 */
	@Id
	@AFieldDescriptor(externalName = "doc.id")
	private String id;
	
	@Field(type=Date, store = true, format = DateFormat.date_time)
	@AFieldDescriptor(externalName = "saved.time")
    private OffsetDateTime timestamp;
	
	@Field(type=Boolean)
	@AFieldDescriptor(externalName = "system.metrics.restarted")
	private Boolean restarted;

	@Field(type=Keyword)
	@AFieldDescriptor(externalName = "system.metrics.host")
	private String host;

	@Field(type=Long)
	@AFieldDescriptor(externalName = "system.metrics.heap.used.bytes")
	private Long heapUsedBytes;
	
	@Field(type=Long)
	@AFieldDescriptor(externalName = "system.metrics.heap.free.bytes")
	private Long heapFreeBytes;
	
	@Field(type=Long)
	@AFieldDescriptor(externalName = "system.metrics.memory.used.bytes")
	private Long memoryUsedBytes;
	
	@Field(type=Long)
	@AFieldDescriptor(externalName = "system.metrics.memory.free.bytes")
	private Long memoryFreeBytes;

	@Field(type=Long)
	@AFieldDescriptor(externalName = "system.metrics.disk.temporary.files.used.bytes")
	private Long diskTemporaryFilesUsedBytes;
	
	@Field(type=Long)
	@AFieldDescriptor(externalName = "system.metrics.disk.temporary.files.free.bytes")
	private Long diskTemporaryFilesFreeBytes;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Boolean getRestarted() {
		return restarted;
	}

	public void setRestarted(Boolean restarted) {
		this.restarted = restarted;
	}

	public String getHost() {
		return host;
	}

	public OffsetDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(OffsetDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public Long getHeapUsedBytes() {
		return heapUsedBytes;
	}

	public void setHeapUsedBytes(Long heapUsedBytes) {
		this.heapUsedBytes = heapUsedBytes;
	}

	public Long getHeapFreeBytes() {
		return heapFreeBytes;
	}

	public void setHeapFreeBytes(Long heapFreeBytes) {
		this.heapFreeBytes = heapFreeBytes;
	}

	public Long getMemoryUsedBytes() {
		return memoryUsedBytes;
	}

	public void setMemoryUsedBytes(Long memoryUsedBytes) {
		this.memoryUsedBytes = memoryUsedBytes;
	}

	public Long getMemoryFreeBytes() {
		return memoryFreeBytes;
	}

	public void setMemoryFreeBytes(Long memoryFreeBytes) {
		this.memoryFreeBytes = memoryFreeBytes;
	}
	
	public Long getDiskTemporaryFilesUsedBytes() {
		return diskTemporaryFilesUsedBytes;
	}

	public void setDiskTemporaryFilesUsedBytes(Long diskTemporaryFilesUsedBytes) {
		this.diskTemporaryFilesUsedBytes = diskTemporaryFilesUsedBytes;
	}

	public Long getDiskTemporaryFilesFreeBytes() {
		return diskTemporaryFilesFreeBytes;
	}

	public void setDiskTemporaryFilesFreeBytes(Long diskTemporaryFilesFreeBytes) {
		this.diskTemporaryFilesFreeBytes = diskTemporaryFilesFreeBytes;
	}

	public String toString() {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			return objectMapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return super.toString();
		}
	}

}
