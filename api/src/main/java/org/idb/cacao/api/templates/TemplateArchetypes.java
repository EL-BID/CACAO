/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.api.templates;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
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
	
	/**
	 * Returns all the built-in domain tables referenced by all defined 'TemplateArchetypes'.
	 */
	public static List<DomainTable> getBuiltInDomainTables() {
		return 
		StreamSupport.stream(getTemplateArchetypes().spliterator(),false)
		.flatMap(arch->Optional.ofNullable(arch.getBuiltInDomainTables()).orElse(Collections.emptyList()).stream())
		.distinct()
		.sorted()
		.collect(Collectors.toList());
	}
	
	/**
	 * Returns the names of the installed packages containing CACAO TemplateArchetypes
	 */
	public static Set<String> getInstalledPackages() {
		return 
		StreamSupport.stream(getTemplateArchetypes().spliterator(),false)
		.map(TemplateArchetype::getPluginName)
		.filter(name->name!=null)
		.collect(Collectors.toCollection(()->new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
	}
	
	/**
	 * Returns the list of all indices names related to published (denormalized data) generated
	 * by the archetypes.
	 */
	public static Set<String> getRelatedPublishedDataIndices(TemplateArchetype... archetypes) {
		if (archetypes==null)
			return Collections.emptySet();
		return Arrays.stream(archetypes)
			.flatMap(t->t.getRelatedPublishedDataIndices().stream())
			.collect(Collectors.toCollection(()->new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
	}

	/**
	 * Returns the list of all indices names related to published (denormalized data) generated
	 * by the archetypes.
	 */
	public static Set<String> getRelatedPublishedDataIndices(Predicate<String> filter, TemplateArchetype... archetypes) {
		if (archetypes==null)
			return Collections.emptySet();
		return Arrays.stream(archetypes)
			.flatMap(t->t.getRelatedPublishedDataIndices().stream())
			.filter(filter)
			.collect(Collectors.toCollection(()->new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
	}
}
