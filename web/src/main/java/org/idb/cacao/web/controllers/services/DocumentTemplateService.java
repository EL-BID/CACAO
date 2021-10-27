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
package org.idb.cacao.web.controllers.services;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.idb.cacao.web.entities.DocumentTemplate;
import org.idb.cacao.web.entities.Periodicity;
import org.idb.cacao.web.repositories.DocumentTemplateRepository;
import org.idb.cacao.web.utils.ErrorUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service methods for template operations and queries
 * 
 * @author Gustavo Figueiredo
 *
 */
@Service
public class DocumentTemplateService {

	@Autowired
	private DocumentTemplateRepository templateRepository;

    /**
     * Search document templates matching their names or ID's or tax names (may be any of these).
     * It's case insensitive and does not considers differences in any part that does not correspond
     * to a letter or a number (so, for example, 'DIGV_TEST' will match with 'DIGV TEST').
     */
	@Transactional(readOnly=true)
	public List<DocumentTemplate> findTemplateWithNameOrId(
			String template) {
		if (template==null || template.trim().length()==0)
			return Collections.emptyList();
		
		Pattern pTemplate;
		try {
			pTemplate = Pattern.compile("^"+(template.replaceAll("[^A-Za-z\\d]", "\\."))+"$",Pattern.CASE_INSENSITIVE);
		}
		catch (Throwable ex) {
			pTemplate = Pattern.compile("^"+Pattern.quote(template)+"$",Pattern.CASE_INSENSITIVE);
		}
		
		List<DocumentTemplate> matching_templates = new LinkedList<>();
		
		for (DocumentTemplate t: templateRepository.findAll()) {
			if (t.getId()!=null && t.getId().equals(template)) {
				matching_templates.add(t);
				continue;
			}
			if (t.getName()!=null && pTemplate.matcher(t.getName()).find()) {
				matching_templates.add(t);
				continue;				
			}
			if (t.getTax()!=null && pTemplate.matcher(t.getTax()).find()) {
				matching_templates.add(t);
				continue;								
			}
		}
		
		return matching_templates;
		
	}
	

	/**
	 * Returns all templates names (only their names, not their versions)
	 */
	public Set<String> getAllTemplates() {

		Set<String> templates_names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		Iterable<DocumentTemplate> all_templates = templateRepository.findAll();
		for (DocumentTemplate template:all_templates) {
			if (null==template.getName())
				continue;
			if (template.getName().trim().length()==0)
				continue;
			templates_names.add(template.getName());
		}

		return templates_names;
	}
	
	/**
	 * Returns a map where the key is the template name and the value is the corresponding tax name
	 * associated to the template (ignores templates with no tax name informed)
	 */
	public Map<String, String> getMapTemplatesNamesToTaxes() {

		Map<String, String> templates_names_and_taxes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		Iterable<DocumentTemplate> all_templates = templateRepository.findAll();
		for (DocumentTemplate template:all_templates) {
			if (null==template.getName())
				continue;
			if (template.getName().trim().length()==0)
				continue;
			if (null==template.getTax())
				continue;
			if (template.getTax().trim().length()==0)
				continue;
			templates_names_and_taxes.put(template.getName(), template.getTax());
		}

		return templates_names_and_taxes;
	}

	/**
	 * Returns a map where the key is the tax name associated to a template and the value is the corresponding template name
	 */
	public Map<String, String> getMapTaxesToTemplatesNames() {

		Map<String, String> templates_taxes_and_names = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		Iterable<DocumentTemplate> all_templates = templateRepository.findAll();
		for (DocumentTemplate template:all_templates) {
			if (null==template.getName())
				continue;
			if (template.getName().trim().length()==0)
				continue;
			if (null==template.getTax())
				continue;
			if (template.getTax().trim().length()==0)
				continue;
			templates_taxes_and_names.put(template.getTax(), template.getName());
		}

		return templates_taxes_and_names;
	}

	/**
	 * Returns the templates names that allows simple pay (returns only their names, not their versions)
	 */
	public Set<String> getSimplePayTemplates() {
		
		Set<String> templates_names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		Iterable<DocumentTemplate> templates = templateRepository.findByAllowSimplePay(Boolean.TRUE);
		for (DocumentTemplate template:templates) {
			if (null==template.getName())
				continue;
			if (template.getName().trim().length()==0)
				continue;
			templates_names.add(template.getName());
		}

		return templates_names;		
	}
	
	/**
	 * Returns the templates names that allows simple pay and their corresponding periodicities
	 */
	public Map<String, Periodicity> getSimplePayTemplatesPeriodicityMap() {
		
		Map<String, Periodicity> templates_periodicity = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		Map<String, Date> templates_timestamps = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		Iterable<DocumentTemplate> templates = templateRepository.findByAllowSimplePay(Boolean.TRUE);
		for (DocumentTemplate template:templates) {
			if (null==template.getName())
				continue;
			if (template.getName().trim().length()==0)
				continue;
			String name = template.getName();
			Date timestamp = template.getTemplateCreateTime();
			Periodicity periodicity = template.getPeriodicity();
			Periodicity last_known_periodicity = templates_periodicity.get(name);
			if (last_known_periodicity==null || Periodicity.UNKNOWN.equals(last_known_periodicity)) {
				templates_periodicity.put(name, periodicity);
				templates_timestamps.put(name, timestamp);
			}
			else if (!Periodicity.UNKNOWN.equals(periodicity)
					&& !last_known_periodicity.equals(periodicity)) {
				// If we got different periodicities for the same template name, keeps the last one
				Date last_known_timestamp = templates_timestamps.get(name);
				if (last_known_timestamp!=null && timestamp!=null && last_known_timestamp.before(timestamp)) {
					templates_periodicity.put(name, periodicity);
					templates_timestamps.put(name, timestamp);					
				}
			}
		}

		return templates_periodicity;		
	}
	
	/**
	 * Returns the templates names that requires declaration and their corresponding periodicities
	 */
	public Map<String, Periodicity> getTemplatesWithFilesPeriodicityMap() {
		
		Map<String, Periodicity> templates_periodicity = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		Map<String, Date> templates_timestamps = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		List<DocumentTemplate> templates = getTemplatesWithFiles();
		for (DocumentTemplate template:templates) {
			if (null==template.getName())
				continue;
			if (template.getName().trim().length()==0)
				continue;
			String name = template.getName();
			Date timestamp = template.getTemplateCreateTime();
			Periodicity periodicity = template.getPeriodicity();
			Periodicity last_known_periodicity = templates_periodicity.get(name);
			if (last_known_periodicity==null || Periodicity.UNKNOWN.equals(last_known_periodicity)) {
				templates_periodicity.put(name, periodicity);
				templates_timestamps.put(name, timestamp);
			}
			else if (!Periodicity.UNKNOWN.equals(periodicity)
					&& !last_known_periodicity.equals(periodicity)) {
				// If we got different periodicities for the same template name, keeps the last one
				Date last_known_timestamp = templates_timestamps.get(name);
				if (last_known_timestamp!=null && timestamp!=null && last_known_timestamp.before(timestamp)) {
					templates_periodicity.put(name, periodicity);
					templates_timestamps.put(name, timestamp);					
				}
			}
		}

		return templates_periodicity;		
	}

	/**
	 * Returns all taxes names according to the templates configurations
	 */
	public Set<String> getAllTaxesTypes() {
		
		Set<String> taxes_names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		Iterable<DocumentTemplate> all_templates = templateRepository.findAll();
		for (DocumentTemplate template:all_templates) {
			if (null==template.getTax())
				continue;
			if (template.getTax().trim().length()==0)
				continue;
			taxes_names.add(template.getTax());
		}

		return taxes_names;
	}
	
	/**
	 * Returns all templates that have some 'file' configured (i.e. they are 'downloadable' and 'uploadable'). Returns the objects themselves.
	 */
	public List<DocumentTemplate> getTemplatesWithFiles() {
		try {
			Page<DocumentTemplate> all_templates = templateRepository.findAll(PageRequest.of(0, 10_000, Sort.by("name.keyword","version.keyword").ascending()));
			// Filter out those templates that have no file to download
			List<DocumentTemplate> downloadable_templates = all_templates.stream().filter(t->t.getFilename()!=null && t.getFilename().trim().length()>0)
					.collect(Collectors.toList());
			return downloadable_templates;
		}
		catch (Throwable ex) {
			if (!ErrorUtils.isErrorNoIndexFound(ex))
				throw ex;
			return Collections.emptyList();
		}
	}
	
	/**
	 * Returns the names of all templates that have some 'file' configured (i.e. they are 'downloadable' and 'uploadable'). Returns only their names.
	 */
	public Set<String> getNamesTemplatesWithFiles() {
		
		Set<String> templates_names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		Iterable<DocumentTemplate> all_templates = getTemplatesWithFiles();
		for (DocumentTemplate template:all_templates) {
			if (null==template.getName())
				continue;
			if (template.getName().trim().length()==0)
				continue;
			templates_names.add(template.getName());
		}

		return templates_names;
	}

	/**
	 * Returns the names of all templates that have some 'file' configured (i.e. they are 'downloadable' and 'uploadable') and also have some periodicity defined (excludes 'unknown'). Returns only their names.
	 */
	public Set<String> getNamesTemplatesWithPeriodicity() {
		
		Set<String> templates_names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		Iterable<DocumentTemplate> all_templates = getTemplatesWithFiles();
		for (DocumentTemplate template:all_templates) {
			if (null==template.getName())
				continue;
			if (template.getName().trim().length()==0)
				continue;
			if (Periodicity.UNKNOWN.equals(template.getPeriodicity()))
				continue;
			templates_names.add(template.getName());
		}

		return templates_names;
	}

	/**
	 * Returns the names of all templates that have some 'file' configured (i.e. they are 'downloadable' and 'uploadable') and also have the given periodicity defined. Returns only their names.
	 */
	public Set<String> getNamesTemplatesWithPeriodicity(Periodicity periodicity) {
		
		Set<String> templates_names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		Iterable<DocumentTemplate> all_templates = getTemplatesWithFiles();
		for (DocumentTemplate template:all_templates) {
			if (null==template.getName())
				continue;
			if (template.getName().trim().length()==0)
				continue;
			if (!periodicity.equals(template.getPeriodicity()))
				continue;
			templates_names.add(template.getName());
		}

		return templates_names;
	}

	/**
	 * Given a template name, returns its periodicity. If we have different versions of the same template name, sorts in inverse chronological order (according to their creation)
	 * and returns the periodicity of the most recent one.
	 */
	public Periodicity getPeriodicity(String templateName) {
		if (templateName==null || templateName.trim().length()==0)
			return null;
		List<DocumentTemplate> matching_templates = templateRepository.findByName(templateName);
		if (matching_templates==null || matching_templates.isEmpty())
			return Periodicity.UNKNOWN;
		if (matching_templates.size()==1)
			return matching_templates.get(0).getPeriodicity();
		// If we got more than one template (different versions of the same template), returns the periodicity of the last
		// created template, unless it's null.
		DocumentTemplate t = matching_templates.stream().sorted(DocumentTemplate.TIMESTAMP_COMPARATOR).filter(d->d.getPeriodicity()!=null).findFirst().orElse(null);
		if (t==null)
			return Periodicity.UNKNOWN;
		else
			return t.getPeriodicity();
	}
}
