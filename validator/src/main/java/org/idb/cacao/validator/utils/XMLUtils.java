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
package org.idb.cacao.validator.utils;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Utility methods for XML files manipulations
 * 
 * @author Gustavo Figueiredo
 *
 */
public class XMLUtils {

	public static String getAttribute(Node node, String name) {
		if (!node.hasAttributes())
			return null;
		NamedNodeMap attributes = node.getAttributes();
		if (attributes == null || attributes.getLength() == 0)
			return null;
		Node attr = attributes.getNamedItem(name);
		if (attr == null)
			return null;
		return attr.getTextContent();
	}

	public static Node locateNode(Node parent, String prefix, String name) {
		List<Node> nodes = locateNodes(parent,prefix,name);
		if ( nodes == null || nodes.isEmpty() )
			return null;
		return nodes.get(0);
//		NodeList subNodes = parent.getChildNodes();
//		if (subNodes == null)
//			return null;
//		int num = subNodes.getLength();
//		if (num == 0)
//			return null;
//		for (int i = 0; i < num; i++) {
//			Node node = subNodes.item(i);
//			if (prefix != null) {
//				if (node.getPrefix() == null)
//					continue;
//				if (!prefix.equalsIgnoreCase(node.getPrefix()))
//					continue;
//			}
//			String this_name = node.getLocalName();
//			if (this_name == null)
//				this_name = node.getNodeName();
//			if (this_name != null) {
//				if (name.equalsIgnoreCase(this_name)) {
//					return node;
//				}
//			}
//		}
//		return null;
	}

	public static List<Node> locateNodesRecursive(Node parent, String prefix, String name) {
		if (parent==null)
			return null;
		// Primeiro busca os filhos diretos
		List<Node> nodes = locateNodes(parent, prefix, name);
		// Depois busca os filhos dos filhos, e assim recursivamente
		NodeList subNodes = parent.getChildNodes();
		if (subNodes == null)
			return nodes;
		int num = subNodes.getLength();
		if (num == 0)
			return nodes;
		for (int i = 0; i < num; i++) {
			Node node = subNodes.item(i);
			List<Node> nodes_indirect = locateNodesRecursive(node, prefix, name);
			if (nodes_indirect!=null && !nodes_indirect.isEmpty()) {
				if (nodes==null)
					nodes = nodes_indirect;
				else
					nodes.addAll(nodes_indirect);
			}
		}
		return nodes;
	}

	public static List<Node> locateNodes(Node parent, String prefix, String name) {
		List<Node> nodes = null;
		NodeList subNodes = parent.getChildNodes();
		if (subNodes == null)
			return null;
		int num = subNodes.getLength();
		if (num == 0)
			return null;
		for (int i = 0; i < num; i++) {
			Node node = subNodes.item(i);
			if (prefix != null) {
				if (node.getPrefix() == null)
					continue;
				if (!prefix.equalsIgnoreCase(node.getPrefix()))
					continue;
			}
			String this_name = node.getLocalName();
			if (this_name == null)
				this_name = node.getNodeName();
			if (this_name != null) {
				if (name.equalsIgnoreCase(this_name)) {
					if (nodes == null)
						nodes = new ArrayList<Node>();
					nodes.add(node);
				}
			}
		}
		return nodes;
	}

}
