/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for HTML contents
 * 
 * @author Gustavo Figueiredo
 *
 */
public class HTMLUtils {

	private static final Pattern pTag = Pattern.compile("<[^><\n]{1,50}>");
	private static final Pattern pHTMLEntities = Pattern.compile("&[a-z]{2,5};");
	private static final Pattern pSpaces = Pattern.compile(" +");
	private static final String SPACE_ENTITY = "&nbsp;";

	/**
	 * Check for the presence of any HTML tag-like contents
	 */
	public static boolean hasTag(String text) {
		return text!=null && pTag.matcher(text).find();
	}

	/**
	 * Check for the presence of any HTML entity-like contents
	 */
	public static boolean hasHTMLEntities(String text) {
		return text!=null && pHTMLEntities.matcher(text).find();
	}

	/**
	 * Replace all new lines characters by the equivalent tag in HTML syntax
	 */
	public static String replaceNewLinesForTags(String text) {
		if (text==null)
			return null;
		return text.replaceAll("\r?\n", "<BR>");
	}
	
	/**
	 * Replace all white spaces by the equivalent entity in HTML syntax
	 */
	public static String replaceSpacesForHTMLEntities(String text) {
		if (text==null)
			return null;
		
		final StringBuffer sb = new StringBuffer();
		Matcher mSpaces = pSpaces.matcher(text);
		while (mSpaces.find()) {
			String spaces = mSpaces.group();
			int num_spaces = spaces.length();
			if (num_spaces==1)
				mSpaces.appendReplacement(sb, " ");
			else {
				char[] replacement = new char[num_spaces*SPACE_ENTITY.length()];
				for (int i=0; i<num_spaces; i++)
					System.arraycopy(SPACE_ENTITY.toCharArray(), 0, replacement, i*SPACE_ENTITY.length(), SPACE_ENTITY.length());
				mSpaces.appendReplacement(sb, new String(replacement));
			}
		}
		mSpaces.appendTail(sb);
		text = sb.toString();
		return text.replaceAll("\t", "&nbsp;&nbsp;&nbsp;");
	}

}
