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
package org.idb.cacao.api.utils;

import java.lang.reflect.Array;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.idb.cacao.api.templates.DocumentTemplate;

/**
 * Utility methods for naming convetions regarding indices and fields to be stored
 * at Elastic Search
 * 
 * @author Gustavo Figueiredo
 *
 */
public class IndexNamesUtils {
	
	/**
	 * Prefix for all indices related to validated data (output from validation phase)
	 */
	public static final String VALIDATED_DATA_INDEX_PREFIX = "cacao_doc_";

	/**
	 * Prefix for all indices related to published (denormalized) data (output from ETL phase)
	 */
	public static final String PUBLISHED_DATA_INDEX_PREFIX = "cacao_pub_";

	/**
	 * Returns a proper index name for using in ElasticSearch for validated documents related to a DocumentTemplate (output from validation)
	 */
	public static String formatIndexNameForValidatedData(DocumentTemplate template) {
		String name = template.getName()==null || template.getName().trim().length()==0 ? "generic" : template.getName();
		String version = template.getVersion()==null || template.getVersion().trim().length()==0 ? "0" : template.getVersion();
		return IndexNamesUtils.formatIndexName("cacao_doc_"+name+"_v_"+version);		
	}

	/**
	 * Returns a proper index name for using in ElasticSearch for validated documents related to a DocumentTemplate (output from validation)
	 */
	public static String formatIndexNameForValidatedData(String templateName, String templateVersion) {
		String name = templateName==null || templateName.trim().length()==0 ? "generic" : templateName;
		String version = templateVersion==null || templateVersion.trim().length()==0 ? "0" : templateVersion;
		return IndexNamesUtils.formatIndexName("cacao_doc_"+name+"_v_"+version);		
	}
	
	/**
	 * Returns a proper index name for using in ElasticSearch for published (denormalized) data (output from ETL)
	 */
	public static String formatIndexNameForPublishedData(String name) {
		return IndexNamesUtils.formatIndexName("cacao_pub_"+name);		
	}


	/**
	 * Get a proper index name for using in ElasticSearch.
	 * This implementations removes diacritical marks, replace spaces with underlines, turn everything lower case.
	 */
	public static String formatIndexName(String indexName) {
		if (indexName==null || indexName.trim().length()==0)
			return "generic";
		indexName = getNormalizedNameForES(indexName);
		if (indexName.length()==0)
			return "generic";
		return indexName;
	}
	
	/**
	 * Returns a 'normalized' name to be used with ElasticSearch to be used by fields in general.
	 */
	public static String formatFieldName(String fieldName) {
		if (fieldName==null || fieldName.trim().length()==0)
			return "field";
		fieldName = getNormalizedNameForES(fieldName);
		if (fieldName.length()==0)
			return "field";
		return fieldName;		
	}

	/**
	 * Returns a 'normalized' name to be used for fields and indices.
	 */
	public static String getNormalizedNameForES(String name) {
		if (name==null || name.trim().length()==0)
			return "";
		name = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("[\\p{InCombiningDiacriticalMarks}]", ""); // removes all diacritics
		name = String.join("_", StringUtils.splitByCharacterTypeCamelCase(name)); // split camel case style and change to underline-separated style
		name = name.toLowerCase(); // make all lowercases
		name = name.replaceAll("[^A-Za-z\\d ]", " "); // removes non letters, non numeric and non spaces
		name = name.trim();	// removes heading and trailing spaces
		name = name.replaceAll("\\s+", "_");		  // replaces one or more successive spaces with underlines
		while (name.startsWith("_"))
			name = name.substring(1);
		while (name.endsWith("_"))
			name = name.substring(0,name.length()-1);
		if (name.length()>255) {
			name = name.substring(0, 255);
		}
		return name;
	}

	/**
	 * Normalizes all the keys inside the map of objects. Performs recursively in inner maps.
	 */
	public static Map<String,Object> normalizeAllKeysForES(Map<String,Object> parsed_contents) {
		if (parsed_contents==null || parsed_contents.isEmpty())
			return parsed_contents;
		
		Map<String,Object> normalized_contents = (parsed_contents instanceof TreeMap) ? new TreeMap<>() : new HashMap<>();
		for (Map.Entry<String, Object> entry: parsed_contents.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (value instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String,Object> nested_map = (Map<String,Object>)value;
				value = normalizeAllKeysForES(nested_map);
			}
			String normalized_key = formatFieldName(key);
			Object previous_value = normalized_contents.get(normalized_key);
			if (previous_value!=null) {
				value = mergeObjects(previous_value, value);
			}
			normalized_contents.put(normalized_key,value);
		}
		
		return normalized_contents;
	}

	/**
	 * Merge two Java objects into one.<BR>
	 * If both objects are Map's or List's or Set's or arrays, merge into one Map or List or Set or array.<BR>
	 * If one object is array and the other isn't, add the singular object into the array.<BR>
	 * Otherwise, returns an array of the two objects.
	 */
	public static Object mergeObjects(Object obj1, Object obj2) {
		if (obj1==obj2)
			return obj1;
		if (obj1==null)
			return obj2;
		if (obj2==null)
			return obj1;
		if ((obj1 instanceof Map) && (obj2 instanceof Map)) {
			Map<?,?> map1 = (Map<?,?>)obj1;
			Map<?,?> map2 = (Map<?,?>)obj2;
			if (map1.isEmpty())
				return map2;
			if (map2.isEmpty())
				return map1;
			Map<Object,Object> merged_map = (map1 instanceof TreeMap) ? new TreeMap<>() : new HashMap<>();
			merged_map.putAll(map1);
			for (Map.Entry<?, ?> entry_in_map2: map2.entrySet()) {
				Object key = entry_in_map2.getKey();
				Object value_in_map2 = entry_in_map2.getValue();
				Object value_in_map1 = merged_map.get(key);
				if (value_in_map1==null)
					merged_map.put(key, value_in_map2);
				else
					merged_map.put(key, mergeObjects(value_in_map1, value_in_map2));
			}
			return merged_map;
		}
		if ((obj1 instanceof List) && (obj2 instanceof List)) {
			List<?> list1 = (List<?>)obj1;
			List<?> list2 = (List<?>)obj2;
			if (list1.isEmpty())
				return list2;
			if (list2.isEmpty())
				return list1;
			List<Object> merged_list = new ArrayList<>(list1.size()+list2.size());
			merged_list.addAll(list1);
			merged_list.addAll(list2);
			return merged_list;			
		}
		if ((obj1 instanceof Set) && (obj2 instanceof Set)) {
			Set<?> set1 = (Set<?>)obj1;
			Set<?> set2 = (Set<?>)obj2;
			if (set1.isEmpty())
				return obj2;
			if (set2.isEmpty())
				return obj1;
			Set<Object> merged_set = (set1 instanceof TreeSet) ? new TreeSet<>() : new HashSet<>();
			merged_set.addAll(set1);
			merged_set.addAll(set2);
			return merged_set;						
		}
		if ((obj1.getClass().isArray()) && (obj2.getClass().isArray())) {
			int len1 = Array.getLength(obj1);
			int len2 = Array.getLength(obj2);
			if (len1==0)
				return obj2;
			if (len2==0)
				return obj1;
			int len_merged = len1+len2;
			Class<?> t1 = obj1.getClass().getComponentType();
			Class<?> t2 = obj2.getClass().getComponentType();
			Class<?> t_merged = (t1!=null && t1.equals(t2)) ? t1 : Object.class;
			Object merged_array = Array.newInstance(t_merged, len_merged);
			for (int i=0; i<len1; i++) {
				Array.set(merged_array, i, Array.get(obj1, i));
			}
			for (int i=0; i<len2; i++) {
				Array.set(merged_array, i+len1, Array.get(obj2, i));
			}
			return merged_array;
		}
		if ((obj1.getClass().isArray()) || (obj2.getClass().isArray())) {
			int len1 = (obj1.getClass().isArray()) ? Array.getLength(obj1) : 1;
			int len2 = (obj2.getClass().isArray()) ? Array.getLength(obj2) : 1;
			int len_merged = len1+len2;
			Class<?> t1 = (obj1.getClass().isArray()) ? obj1.getClass().getComponentType() : obj1.getClass();
			Class<?> t2 = (obj2.getClass().isArray()) ? obj2.getClass().getComponentType() : obj2.getClass();
			Class<?> t_merged = (t1!=null && t1.equals(t2)) ? t1 : Object.class;
			Object merged_array = Array.newInstance(t_merged, len_merged);
			if (obj1.getClass().isArray()) {
				for (int i=0; i<len1; i++) {
					Array.set(merged_array, i, Array.get(obj1, i));
				}
			}
			else {
				Array.set(merged_array, 0, obj1);
			}
			if (obj2.getClass().isArray()) {
				for (int i=0; i<len2; i++) {
					Array.set(merged_array, i+len1, Array.get(obj2, i));
				}
			}
			else {
				Array.set(merged_array, len1, obj2);
			}
			return merged_array;
		}
		Object[] merged_array = new Object[2];
		merged_array[0] = obj1;
		merged_array[1] = obj2;
		return merged_array;
	}
}
