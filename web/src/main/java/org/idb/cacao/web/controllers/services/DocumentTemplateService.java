/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.services;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.idb.cacao.api.Periodicity;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DocumentInput;
import org.idb.cacao.api.templates.DocumentInputFieldMapping;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.TemplateArchetype;
import org.idb.cacao.api.templates.TemplateArchetypes;
import org.idb.cacao.web.dto.DocumentTemplateDto;
import org.idb.cacao.web.dto.TemplateAndInput;
import org.idb.cacao.web.repositories.DocumentTemplateRepository;
import org.idb.cacao.web.utils.ErrorUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

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

	@Autowired
	private MessageSource messageSource;

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
		catch (Exception ex) {
			pTemplate = Pattern.compile("^"+Pattern.quote(template)+"$",Pattern.CASE_INSENSITIVE);
		}
		
		List<DocumentTemplate> matchingTemplates = new LinkedList<>();
		
		for (DocumentTemplate t: templateRepository.findAll()) {
			if (t.getId()!=null && t.getId().equals(template)) {
				matchingTemplates.add(t);
				continue;
			}
			if (t.getName()!=null && pTemplate.matcher(t.getName()).find()) {
				matchingTemplates.add(t);
				continue;				
			}
		}
		
		return matchingTemplates;
		
	}
	

	/**
	 * Returns all templates names (only their names, not their versions)
	 */
	public Set<String> getAllTemplates() {

		Set<String> templatesNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		Iterable<DocumentTemplate> allTemplates = templateRepository.findAll();
		for (DocumentTemplate template:allTemplates) {
			if (null==template.getName() || (template.getName().trim().length()==0))
				continue;
			templatesNames.add(template.getName());
		}

		return templatesNames;
	}
	
	/**
	 * Returns the templates names that requires declaration and their corresponding periodicities
	 */
	public Map<String, Periodicity> getTemplatesWithFilesPeriodicityMap() {
		
		Map<String, Periodicity> templatesPeriodicity = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		Map<String, OffsetDateTime> templatesTimestamps = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		List<DocumentTemplate> templates = getTemplatesWithFiles();
		for (DocumentTemplate template:templates) {
			if (null==template.getName() || (template.getName().trim().length()==0))
				continue;
			String name = template.getName();
			OffsetDateTime timestamp = template.getTemplateCreateTime();
			Periodicity periodicity = template.getPeriodicity();
			Periodicity lastKnownPeriodicity = templatesPeriodicity.get(name);
			if (lastKnownPeriodicity==null || Periodicity.UNKNOWN.equals(lastKnownPeriodicity)) {
				templatesPeriodicity.put(name, periodicity);
				templatesTimestamps.put(name, timestamp);
			}
			else if (!Periodicity.UNKNOWN.equals(periodicity)
					&& !lastKnownPeriodicity.equals(periodicity)) {
				// If we got different periodicities for the same template name, keeps the last one
				OffsetDateTime lastKnownTimestamp = templatesTimestamps.get(name);
				if (lastKnownTimestamp!=null && timestamp!=null && lastKnownTimestamp.isBefore(timestamp)) {
					templatesPeriodicity.put(name, periodicity);
					templatesTimestamps.put(name, timestamp);					
				}
			}
		}

		return templatesPeriodicity;		
	}

	/**
	 * Returns all templates defined in repository. Returns the objects themselves.
	 */
	public List<DocumentTemplate> getTemplatesWithFiles() {
		try {
			Page<DocumentTemplate> allTemplates = templateRepository.findByActive(true, PageRequest.of(0, 10_000, Sort.by("name.keyword","version.keyword").ascending()));
			return allTemplates.getContent();
		}
		catch (Exception ex) {
			if (!ErrorUtils.isErrorNoIndexFound(ex))
				throw ex;
			return Collections.emptyList();
		}
	}
	
	/**
	 * Returns the names of all templates that have some 'file' configured (i.e. they are 'downloadable' and 'uploadable'). Returns only their names.
	 */
	public Set<String> getNamesTemplatesWithFiles() {
		
		Set<String> templatesNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		Iterable<DocumentTemplate> allTemplates = getTemplatesWithFiles();
		for (DocumentTemplate template:allTemplates) {
			if (null==template.getName() || (template.getName().trim().length()==0))
				continue;
			templatesNames.add(template.getName());
		}

		return templatesNames;
	}
	
	/**
	 * Returns the names of all templates that have some 'file' configured (i.e. they are 'downloadable' and 'uploadable'). 
	 * Returns only their names and version.
	 */
	public List<TemplateAndInput> getNamesTemplatesWithVersions() {
		
		List<TemplateAndInput> templatesAndInputs= new LinkedList<>();
		Iterable<DocumentTemplate> allTemplates = getTemplatesWithFiles();
		for (DocumentTemplate template:allTemplates) {
			if (null==template.getName() || (template.getName().trim().length()==0))
				continue;
			
			List<DocumentInput> inputs = template.getInputs();
			
			if (inputs != null && !inputs.isEmpty()) {
				
				inputs.forEach( input-> {
					
					String templateName = template.getName();
					String inputName = input.getInputName();
					String inputNameDisplay = inputName;
				
					
					if ( inputName.contains(templateName) )
						inputNameDisplay = inputName.replace(templateName, "").trim();
					
					TemplateAndInput item = new TemplateAndInput();
					item.setTemplateName(templateName);
					item.setVersion(template.getVersion());
					item.setInputName(inputName);
					item.setInputNameDisplay(inputNameDisplay);
					templatesAndInputs.add(item);
					
				});
				
			}
			

		}
		
		return templatesAndInputs;
	}

	/**
	 * Returns the names of all templates that have some 'file' configured (i.e. they are 'downloadable' and 'uploadable') and also have some periodicity defined (excludes 'unknown'). Returns only their names.
	 */
	public Set<String> getNamesTemplatesWithPeriodicity() {
		
		Set<String> templatesNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		Iterable<DocumentTemplate> allTemplates = getTemplatesWithFiles();
		for (DocumentTemplate template:allTemplates) {
			if (null==template.getName() || (template.getName().trim().length()==0) || (Periodicity.UNKNOWN.equals(template.getPeriodicity())))
				continue;
			templatesNames.add(template.getName());
		}

		return templatesNames;
	}

	/**
	 * Returns the names of all templates that have some 'file' configured (i.e. they are 'downloadable' and 'uploadable') and also have the given periodicity defined. Returns only their names.
	 */
	public Set<String> getNamesTemplatesWithPeriodicity(Periodicity periodicity) {
		
		Set<String> templatesNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		Iterable<DocumentTemplate> allTemplates = getTemplatesWithFiles();
		for (DocumentTemplate template:allTemplates) {
			if (null==template.getName() || (template.getName().trim().length()==0) || (!periodicity.equals(template.getPeriodicity())))
				templatesNames.add(template.getName());
		}

		return templatesNames;
	}

	/**
	 * Given a template name, returns its periodicity. If we have different versions of the same template name, sorts in inverse chronological order (according to their creation)
	 * and returns the periodicity of the most recent one.
	 */
	public Periodicity getPeriodicity(String templateName) {
		if (templateName==null || templateName.trim().length()==0)
			return null;
		List<DocumentTemplate> matchingTemplates = templateRepository.findByName(templateName);
		if (matchingTemplates==null || matchingTemplates.isEmpty())
			return Periodicity.UNKNOWN;
		if (matchingTemplates.size()==1)
			return matchingTemplates.get(0).getPeriodicity();
		// If we got more than one template (different versions of the same template), returns the periodicity of the last
		// created template, unless it's null.
		DocumentTemplate t = matchingTemplates.stream().sorted(DocumentTemplate.TIMESTAMP_COMPARATOR).filter(d->d.getPeriodicity()!=null).findFirst().orElse(null);
		if (t==null)
			return Periodicity.UNKNOWN;
		else
			return t.getPeriodicity();
	}
	
	/**
	 * The document template fields should match each mapping in each document input format
	 */
	public void compatibilizeTemplateFieldsMappings(DocumentTemplate template) {
		if (template==null)
			return;
		if (template.getFields()==null 
				|| template.getFields().isEmpty()
				|| template.getInputs()==null
				|| template.getInputs().isEmpty()) {
			return;
		}
		
		// Let's get all the fields information from the DocumentTemplate

		Map<String,Integer> allMapFieldNames = template.getFields().stream()
				.filter(f->f!=null && f.getFieldName()!=null && f.getFieldName().trim().length()>0)
				.collect(Collectors.toMap(
					/*keyMapper*/DocumentField::getFieldName, 
					/*valueMapper*/DocumentField::getId, 
					/*mergeFunction*/(a,b)->a, 
					/*mapSupplier*/()->new TreeMap<>(String.CASE_INSENSITIVE_ORDER)));
		if (allMapFieldNames.isEmpty())
			return;

		// First of all, let's make sure every field in DocumentTemplate has an unique ID
		Map<Integer,String> mapFieldIds = new HashMap<>();
		for (Map.Entry<String,Integer> entry: new ArrayList<>(allMapFieldNames.entrySet())) {
			String fieldName = entry.getKey();
			Integer fieldId = entry.getValue();
			if (mapFieldIds.containsKey(fieldId)) {
				fieldId = template.getNextUnassignedFieldId();
				allMapFieldNames.put(fieldName, fieldId);
				mapFieldIds.put(fieldId, fieldName);
				template.getField(fieldName).setId(fieldId);
				template.evictNextUnassignedFieldId();
			}
			else {
				mapFieldIds.put(fieldId, fieldName);
			}
		}

		for (DocumentInput input: template.getInputs()) {
			if (input==null)
				continue;
			
			Map<String,Integer> mapFieldNames = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			mapFieldNames.putAll(allMapFieldNames);
			
			if (input.getFields()==null || input.getFields().isEmpty()) {
				// If the DocumentInput has no mappings, let's include one for each DocumentField
				for (Map.Entry<String,Integer> entry: mapFieldNames.entrySet()) {
					DocumentInputFieldMapping map = new DocumentInputFieldMapping();
					map.setFieldName(entry.getKey());
					map.setFieldId(entry.getValue());
					input.addField(map);
				}
			}
			else {
				// If the DocumentInput has some mappings, let's check what is absent and what is exceeding
				List<DocumentInputFieldMapping> exceedingMaps = new LinkedList<>();
				for (DocumentInputFieldMapping map: input.getFields()) {
					if (map.getFieldName()==null || map.getFieldName().trim().length()==0)
						continue;
					Integer fieldId = mapFieldNames.remove(map.getFieldName());
					if (fieldId==null) {
						// This DocumentInputFieldMapping refers to a name that does not exist in DocumentTemplate
						// Let's see check later if they have been renamed
						exceedingMaps.add(map);
					}
					else {
						// This DocumentInputFieldMapping matches a DocumentField in DocumentTemplate
					}
				} // LOOP over DocumentInputFieldMapping's
				if (!mapFieldNames.isEmpty()) {
					// Some fields are missing in DocumentInput ...
					for (Map.Entry<String,Integer> entry: mapFieldNames.entrySet()) {
						String fieldName = entry.getKey();
						Integer fieldId = entry.getValue();
						// Check for possible renamed field in FieldMappings (something with different name but the same id)
						DocumentInputFieldMapping possibleRenamedField = exceedingMaps.stream().filter(m->fieldId.equals(m.getFieldId())).findAny().orElse(null);
						if (possibleRenamedField!=null) {
							// Change the name of the existent field mapping
							possibleRenamedField.setFieldName(fieldName);
							exceedingMaps.remove(possibleRenamedField); // it's not to be considered 'exceeding' anymore
						}
						else {
							// Add a new field mapping
							DocumentInputFieldMapping map = new DocumentInputFieldMapping();
							map.setFieldName(fieldName);
							map.setFieldId(fieldId);
							input.addField(map);							
						}
					} // LOOP over missing DocumentInputFieldMapping's in DocumentInput
				}
				if (!exceedingMaps.isEmpty()) {
					// Some fields are exceeding in DocumentInput ...
					exceedingMaps.stream().forEach(input::removeField);
				}
			}
		}
	}

	/**
	 * Performs additional validation over templates
	 */
	public void validateTemplate(DocumentTemplateDto template, BindingResult result) {
		if (template==null || result==null)
			return;
		if (template.getFields()!=null && !template.getFields().isEmpty()) {
			// Check for duplicate fields
			Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
			for (DocumentField field: template.getFields()) {
				if (field.getFieldName()==null || field.getFieldName().trim().length()==0) {
					String fieldName = messageSource.getMessage("field.name", null, LocaleContextHolder.getLocale());
					String errorMessage = messageSource.getMessage("error.field.empty", new Object[] {fieldName}, LocaleContextHolder.getLocale());
					result.addError(new ObjectError(
						fieldName,
						errorMessage));
				}
				else if (names.contains(field.getFieldName())) {
					String fieldName = messageSource.getMessage("field.name", null, LocaleContextHolder.getLocale());
					String errorMessage = messageSource.getMessage("error.unique.name", new Object[] {field.getFieldName()}, LocaleContextHolder.getLocale());
					result.addError(new ObjectError(
						fieldName,
						errorMessage));					
				}
				else {
					names.add(field.getFieldName());
				}
			}
		}
		if (template.getArchetype()!=null && template.getArchetype().trim().length()>0) {
			Optional<TemplateArchetype> arch = TemplateArchetypes.getArchetype(template.getArchetype());
			if (arch.isPresent()) {
				// If the document has declared an archetype, check if the required fields defined in the built-in archetype are present
				List<DocumentField> requiredFieldsInArchetype = arch.get().getRequiredFields();
				if (requiredFieldsInArchetype!=null && !requiredFieldsInArchetype.isEmpty()) {
					Map<String,DocumentField> fieldsInTemplate = (template.getFields()==null) ? new TreeMap<>(String.CASE_INSENSITIVE_ORDER)
							: template.getFields().stream().collect(Collectors.toMap(
									/*keyMapper*/DocumentField::getFieldName, 
									/*valueMapper*/Function.identity(), 
									/*mergeFunction*/(a,b)->a, 
									/*mapSupplier*/()->new TreeMap<>(String.CASE_INSENSITIVE_ORDER)));
					for (DocumentField fieldInArchetype: requiredFieldsInArchetype) {
						if (!Boolean.TRUE.equals(fieldInArchetype.getRequired()))
							continue;
						DocumentField fieldInTemplate = fieldsInTemplate.get(fieldInArchetype.getFieldName());
						if (fieldInTemplate==null) {
							String fieldName = messageSource.getMessage("field.name", null, LocaleContextHolder.getLocale());
							String archName = messageSource.getMessage(arch.get().getName(), null, LocaleContextHolder.getLocale());
							String errorMessage = messageSource.getMessage("error.missing.archetype.field", 
									new Object[] {fieldInArchetype.getFieldName(),archName,template.getName()}, 
									LocaleContextHolder.getLocale());
							result.addError(new ObjectError(
								fieldName,
								errorMessage));												
						}
					}
				}
			}
		}
	}

	/**
	 * Check if there are two or more fields with the same id
	 */
	public static boolean hasConflictingFieldsId(DocumentTemplate template) {
		if (template==null || template.getFields()==null || template.getFields().isEmpty())
			return false;
		Set<Integer> ids = new HashSet<>();
		for (DocumentField field: template.getFields()) {
			int id = field.getId();
			if (ids.contains(id))
				return true;
			ids.add(id);
		}
		return false;
	}
	
	/**
	 * Reassigns new ids to every field in the DocumentTemplate
	 */
	public static void reassignFieldId(DocumentTemplate template) {
		if (template==null || template.getFields()==null || template.getFields().isEmpty())
			return;
		List<DocumentField> templateFields = new ArrayList<>(template.getFields());
		templateFields.stream().forEach(field->field.setId(0)); // will assign a new id
		template.clearFields();	// clears out all the fields
		template.setFields(templateFields);	// reinserts all the fields again, this time reassigning new ids
	}
}
