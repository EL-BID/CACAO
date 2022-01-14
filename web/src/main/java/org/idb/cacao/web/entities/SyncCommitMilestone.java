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
package org.idb.cacao.web.entities;

import static org.springframework.data.elasticsearch.annotations.FieldType.Date;
import static org.springframework.data.elasticsearch.annotations.FieldType.Keyword;

import java.io.Serializable;
import java.time.OffsetDateTime;

import javax.persistence.Id;
import javax.validation.constraints.NotNull;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

/**
 * Sync commit tracking information (for subscribers/slaves)
 */
@Document(indexName="cacao_sync_commit_milestone")
public class SyncCommitMilestone implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;

	/**
	 * ID's are being generated automatically.
	 * PS: Elasticsearch generates by default 20 character long ID's, that are both URL-safe, base 64 encoded GUID
	 */
	@Id   
	private String id;

	/**
	 * Committed endpoint
	 */
	@Field(type=Keyword)
	@NotNull
	private String endPoint;

	/**
	 * Date/time of last SYNC
	 */
	@Field(type=Date, store = true, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
    private OffsetDateTime lastTimeRun;
	
	/**
	 * Date/time of start of the period of last SYNC
	 */
	@Field(type=Date, store = true, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
    private OffsetDateTime lastTimeStart;

	/**
	 * Date/time of end of the period of last SYNC
	 */
	@Field(type=Date, store = true, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
    private OffsetDateTime lastTimeEnd;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getEndPoint() {
		return endPoint;
	}

	public void setEndPoint(String endPoint) {
		this.endPoint = endPoint;
	}

	public OffsetDateTime getLastTimeRun() {
		return lastTimeRun;
	}

	public void setLastTimeRun(OffsetDateTime lastTimeRun) {
		this.lastTimeRun = lastTimeRun;
	}

	public OffsetDateTime getLastTimeStart() {
		return lastTimeStart;
	}

	public void setLastTimeStart(OffsetDateTime lastTimeStart) {
		this.lastTimeStart = lastTimeStart;
	}

	public OffsetDateTime getLastTimeEnd() {
		return lastTimeEnd;
	}

	public void setLastTimeEnd(OffsetDateTime lastTimeEnd) {
		this.lastTimeEnd = lastTimeEnd;
	}

	public SyncCommitMilestone clone() {
		try {
			return (SyncCommitMilestone)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	public int hashCode() {
		return 17 + 37 * (endPoint==null?0:endPoint.hashCode());
	}
	
	public boolean equals(Object o) {
		if (this==o)
			return true;
		if (!(o instanceof SyncCommitMilestone))
			return false;
		SyncCommitMilestone ref = (SyncCommitMilestone)o;
		if (endPoint!=ref.endPoint) {
			if (endPoint==null || ref.endPoint==null)
				return false;
			if (!endPoint.equals(ref.endPoint))
				return false;
		}
		return true;
	}
	
    @Override
    public String toString() {
        return endPoint;
    }

}
