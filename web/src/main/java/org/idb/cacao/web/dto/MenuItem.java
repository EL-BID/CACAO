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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.idb.cacao.web.utils.HTMLUtils;

/**
 * This is a generic structure for presenting menu itens, with or without submenus. It's
 * also used to represent generic structures (e.g. for viewing in 'accordion' style) 
 *  
 * @author Gustavo Figueiredo
 */
public class MenuItem implements Serializable, Comparable<MenuItem> {

	private static final long serialVersionUID = 1L;

	/**
	 * Text to be displayed about this menu item.
	 */
	private String name;

	/**
	 * Flag indicating if this menu item should be expanded by default
	 */
	private boolean active;
	
	private boolean html;
	
	private boolean markdown;
	
	private String link;
	
    private MenuItem parent;

    private List<MenuItem> children;

	public MenuItem() { 
		this.active = true;
	}
	
	public MenuItem(String name) {
		this.name = name;
		this.active = true;
	}

	public MenuItem(String name, String link) {
		this.name = name;
		this.link = link;
		this.active = true;
	}

    public MenuItem(String name,List<MenuItem> children) {
        this.name = name;
		this.active = true;
        this.children = children;
        if (children!=null) {
	        for (MenuItem child : children) {
	            child.parent = this;
	        }
        }
    }

	/**
	 * Text to be displayed about this menu item.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Text to be displayed about this menu item.
	 */
	public void setName(String name) {
		this.name = name;
	}

	public boolean isHtml() {
		return html;
	}

	public void setHtml(boolean html) {
		this.html = html;
	}

	public boolean isMarkdown() {
		return markdown;
	}

	public void setMarkdown(boolean markdown) {
		this.markdown = markdown;
	}

	public String getLink() {
		return link;
	}

	/**
	 * Flag indicating if this menu item should be expanded by default
	 */
	public void setLink(String link) {
		this.link = link;
	}
	
	/**
	 * Flag indicating if this menu item should be expanded by default
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * Flag indicating if this menu item should be expanded by default
	 */
	public void setActive(boolean active) {
		this.active = active;
	}
	
	public MenuItem withActive(boolean active) {
		setActive(active);
		return this;
	}

	public MenuItem getParent() {
        return parent;
    }

    public void setParent(MenuItem parent) {
		this.parent = parent;
	}

	public List<MenuItem> getChildren() {
		if (children==null)
			return Collections.emptyList();
        return children;
    }
	
	public void setChildren(List<MenuItem> children) {
		this.children = children;
		if (children!=null) {
	        for (MenuItem child : children) {
	            child.parent = this;
	        }			
		}
	}

	public void addChild(MenuItem item) {
		if (children==null)
			children = new ArrayList<>();
		children.add(item);
		item.setParent(this);
	}
	
	public MenuItem withChild(MenuItem item) {
		addChild(item);
		return this;
	}

	public MenuItem withChild(String itemName) {
		if (itemName==null)
			return this;
		return withChild(new MenuItem(itemName));
	}
	
	public MenuItem withMarkdownChild(String markdown) {
		if (markdown==null)
			return this;
		MenuItem child = new MenuItem(markdown);
		child.setMarkdown(true);
		return withChild(child);
	}

	public MenuItem withChild(String itemName, String link) {
		return withChild(new MenuItem(itemName, link));
	}
	
	public MenuItem treatNewLinesAndSpaces(boolean recursive) {
		if (name!=null) {
			name = HTMLUtils.replaceNewLinesForTags(name);
			name = HTMLUtils.replaceSpacesForHTMLEntities(name);
		}
		if (recursive) {
			if (children!=null)
				children.forEach(child->child.treatNewLinesAndSpaces(true));
		}
		return this;
	}
	
	public MenuItem chkForHTMLContents(boolean recursive) {
		if (name!=null) {
			html = HTMLUtils.hasTag(name) || HTMLUtils.hasHTMLEntities(name); 
		}
		if (recursive) {
			if (children!=null)
				children.forEach(child->child.chkForHTMLContents(true));
		}
		return this;
	}

	@Override
	public int compareTo(MenuItem o) {
		return name.compareToIgnoreCase(o.name);
	}

	public String toString() {
		return name;
	}
}
