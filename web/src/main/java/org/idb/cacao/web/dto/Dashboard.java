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
package org.idb.cacao.web.dto;

/**
 * Data Transfer Object for dashboard management<BR>
 * View: dashboards_list.html<BR>
 * Controller: DashboardsUIController<BR>
 * 
 * @author Gustavo Figueiredo
 *
 */
public class Dashboard implements Comparable<Dashboard> {

	private String id;
	
	private String title;
	
	private String spaceId;
	
	private String spaceName;
	
	private String url;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSpaceId() {
		return spaceId;
	}

	public void setSpaceId(String spaceId) {
		this.spaceId = spaceId;
	}

	public String getSpaceName() {
		return spaceName;
	}

	public void setSpaceName(String spaceName) {
		this.spaceName = spaceName;
	}
		
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String toString() {
		return getTitle()+":"+getSpaceName();
	}

	@Override
	public int compareTo(Dashboard o) {
		if (title!=o.title) {
			if (title==null)
				return -1;
			if (o.title==null)
				return 1;
			int c = String.CASE_INSENSITIVE_ORDER.compare(title, o.title);
			if (c!=0)
				return c;
		}
		if (spaceName!=o.spaceName) {
			if (spaceName==null)
				return -1;
			if (o.spaceName==null)
				return 1;
			int c = String.CASE_INSENSITIVE_ORDER.compare(spaceName, o.spaceName);
			if (c!=0)
				return c;
		}
		return 0;
	}

}
