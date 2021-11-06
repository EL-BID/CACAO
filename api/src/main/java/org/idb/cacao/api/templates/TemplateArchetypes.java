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
package org.idb.cacao.api.templates;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Provides methods for discovery of all implementations of TemplateArchetype's 
 * 
 * @author Gustavo Figueiredo
 *
 */
public class TemplateArchetypes {

	/**
	 * Returns all implementations of TemplateArchetype.
	 */
	public static Iterable<TemplateArchetype> getTemplateArchetypes() {
		return ServiceLoader.load(TemplateArchetype.class);
	}
	
	/**
	 * Returns the general-purpose archetype (not related to any specific context)
	 */
	public static TemplateArchetype getGenericArchetype() {
		return new GenericTemplateArchetype();
	}
	
	/**
	 * Returns the name of the general-purpose archetype (not related to any specific context)
	 */
	public static String getGenericArchetypeName() {
		return GenericTemplateArchetype.NAME;
	}

	/**
	 * Returns all the archetypes names (only their names) that are installed and
	 * available to be used with this application.<BR>
	 * The names returned are actually references that may be resolved with 'message.properties'
	 * files to match a particular language.
	 */
	public static Set<String> getNames() {
		return
		StreamSupport.stream(getTemplateArchetypes().spliterator(),false)
		.map(TemplateArchetype::getName)
		.collect(Collectors.toCollection(()->new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
	}
	
	/**
	 * Returns the 'TemplateArchetype' related to a given name.<BR>
	 * The name should be the 'internal name' as is defined in {@link TemplateArchetype#getName() getName} 
	 * (not the displayed name related to a specific language).
	 */
	public static Optional<TemplateArchetype> getArchetype(String name) {
		return
		StreamSupport.stream(getTemplateArchetypes().spliterator(),false)
		.filter(arch->String.CASE_INSENSITIVE_ORDER.compare(name,arch.getName())==0)
		.findAny();		
	}

	/**
	 * Indicates if exists any 'TemplateArchetype' related to a given name.<BR>
	 * The name should be the 'internal name' as is defined in {@link TemplateArchetype#getName() getName} 
	 * (not the displayed name related to a specific language).
	 */
	public static boolean hasArchetype(String name) {
		return
		StreamSupport.stream(getTemplateArchetypes().spliterator(),false)
		.anyMatch(arch->String.CASE_INSENSITIVE_ORDER.compare(name,arch.getName())==0);		
	}
}