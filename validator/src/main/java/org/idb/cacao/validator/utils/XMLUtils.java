/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
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
