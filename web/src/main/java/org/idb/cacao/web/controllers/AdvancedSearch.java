/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Class used between view and controller for representing advanced search with several filters
 * that can be used for arbitrary objects.
 * 
 * @author Gustavo Figueiredo
 *
 */
@JsonInclude(Include.NON_NULL)
public class AdvancedSearch implements Cloneable {
	
	@JsonProperty("f")
	private List<QueryFilter> filters;
	
	public AdvancedSearch() { }
	
	public AdvancedSearch(List<QueryFilter> filters) {
		if (filters!=null)
			this.filters = new ArrayList<>(filters);
	}
	
	public AdvancedSearch(AdvancedSearch other) {
		this(other.filters);
	}
	
	public List<QueryFilter> getFilters() {
		return filters;
	}

	public void setFilters(List<QueryFilter> filters) {
		this.filters = filters;
	}
	
	public void addFilter(QueryFilter filter) {
		if (filters==null)
			filters = new LinkedList<>();
		filters.add(filter);
	}
	
	public Optional<QueryFilter> getFilter(String name) {
		if (filters==null || name==null)
			return Optional.empty();
		return filters.stream().filter(p->name.equalsIgnoreCase(p.getName())).findAny();
	}
	
	public AdvancedSearch withFilter(QueryFilter filter) {
		addFilter(filter);
		return this;
	}
	
	public void addFilter(String name, String argument) {
		addFilter(new QueryFilterTerm(name, argument));
	}
	
	public AdvancedSearch withFilter(String name, String argument) {
		addFilter(name, argument);
		return this;
	}

	public AdvancedSearch withAlternativeFilters(String name1, String argument1, String name2, String argument2) {
		QueryFilter filter1 = new QueryFilterTerm(name1, argument1);
		QueryFilter filter2 = new QueryFilterTerm(name2, argument2);
		return withFilter(new QueryFilterOr(null, filter1, filter2));
	}

	/**
	 * Return this object after removing all filters whose 'name' is equals to argument
	 */
	public AdvancedSearch withoutFilter(String name) {
		if (hasFilters()) {
			for (Iterator<QueryFilter> it=filters.iterator(); it.hasNext(); ) {
				QueryFilter filter = it.next();
				if (filter==null || filter.getName()==null || filter.getName().equals(name))
					it.remove(); // removes filters without a name or with the 'name' equals to argument
			}
		}
		return this;
	}

	/**
	 * Return this object after making a copy of all arguments provided in argument
	 */
	public AdvancedSearch withArguments(Optional<AdvancedSearch> filters) {
		if (filters.isPresent() && !filters.get().isEmpty()) {
			for (QueryFilter filter:filters.get().getFilters()) {
				getFilter(filter.getName()).ifPresent(f->f.copyArguments(filter));
			}
		}
		return this;
	}

	/**
	 * Return this object after making a copy of 'display names' provided in argument'
	 */
	public AdvancedSearch withDisplayNames(AdvancedSearch objWithNames) {
		if (!objWithNames.hasFilters() || !this.hasFilters())
			return this;
		Map<String, String> mapNamesToDisplayNames = objWithNames.getFilters().stream()
				.collect(Collectors.toMap(QueryFilter::getName, QueryFilter::getDisplayName, (a,b)->a));
		for (QueryFilter filter: getFilters()) {
			if (filter==null || filter.getName()==null)
				continue;
			String displayName = mapNamesToDisplayNames.get(filter.getName());
			if (displayName!=null && displayName.length()>0)
				filter.setDisplayName(displayName);
		}
		return this;
	}
	
	/**
	 * Return this object after wiring all internal filters to provided 'MessageSource' object (useful for display localized contexts). It's
	 * not persistent.
	 */
	public AdvancedSearch wiredTo(MessageSource messageSource) {
		if (hasFilters()) {
			for (QueryFilter filter:filters) {
				filter.messageSource = messageSource;
			}
		}
		return this;
	}
	
	/**
	 * Returns TRUE if there are no filters with arguments. Returns FALSE otherwise.
	 */
	@JsonIgnore
	public boolean isEmpty() {
		return filters==null || filters.isEmpty() || filters.stream().allMatch(QueryFilter::isEmpty);
	}

	/**
	 * Returns TRUE if there are any filters. It does not check for the presence of arguments in those filters. 
	 * Therefore it's NOT the opposite of {@link #isEmpty() isEmpty}
	 * 
	 */
	@JsonIgnore
	public boolean hasFilters() {
		return filters!=null && !filters.isEmpty();
	}
	
	/**
	 * Given some filter names, remove them from this filters set and returns a new filter set
	 * containing only those filters. This filters set may be modified by this method as well.
	 */
	@JsonIgnore
	public Optional<AdvancedSearch> splitFilters(String... filterNames) {
		if (filterNames==null || filterNames.length==0)
			return Optional.empty();
		
		if (!hasFilters())
			return Optional.empty();
		
		Set<String> filterNamesSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		Arrays.stream(filterNames).forEach(filterNamesSet::add);
		List<QueryFilter> filteredFilters = filters.stream().filter(f->filterNamesSet.contains(f.getName())).collect(Collectors.toList());
		if (filteredFilters.isEmpty())
			return Optional.empty();
		
		filteredFilters.forEach(this.filters::remove);
		return Optional.of(new AdvancedSearch(filteredFilters));
	}

	@JsonTypeInfo(
			  use = JsonTypeInfo.Id.NAME, 
			  include = JsonTypeInfo.As.PROPERTY, 
			  property = "t")
			@JsonSubTypes({ 
			  @Type(value = QueryFilterTerm.class, name = "term"), 
			  @Type(value = QueryFilterRange.class, name = "range"),
			  @Type(value = QueryFilterBoolean.class, name = "bool"),
			  @Type(value = QueryFilterDate.class, name = "date"),
			  @Type(value = QueryFilterValue.class, name = "value"),
			  @Type(value = QueryFilterList.class, name = "list"),
			  @Type(value = QueryFilterEnum.class, name = "enum"),
			  @Type(value = QueryFilterOr.class, name = "or"),
			  @Type(value = QueryFilterExist.class, name = "exist"),
			  @Type(value = QueryFilterDoesNotExist.class, name = "dont_exist")
			})
	@JsonInclude(Include.NON_NULL)
	public abstract static class QueryFilter implements Cloneable {
		
		@JsonProperty("n")
		private String name;
		
		@JsonIgnore
		private String displayName;
		
		@JsonIgnore
		protected transient MessageSource messageSource;

		protected QueryFilter() { }
		
		protected QueryFilter(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
		
		public String getDisplayName() {
			return displayName;
		}

		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}
		
		public QueryFilter withDisplayName(String displayName) {
			setDisplayName(displayName);
			return this;
		}

		public abstract void copyArguments(QueryFilter other);
		
		@JsonIgnore
		public abstract boolean isEmpty();
		
		@JsonIgnore
		public abstract String getPredicate();
		
		@JsonIgnore
		public MessageSource getMessageSource() {
			return messageSource;
		}
		
		public QueryFilter wiredTo(MessageSource messageSource) {
			this.messageSource = messageSource;
			return this;
		}

	}
	
	@JsonInclude(Include.NON_NULL)
	public static class QueryFilterTerm extends QueryFilter {
		@JsonProperty("a")
		private String argument;
		
		public QueryFilterTerm() { }
		
		public QueryFilterTerm(String name) { 
			super(name);
		}

		public QueryFilterTerm(String name, String argument) {
			super(name);
			this.argument = argument;
		}

		public String getArgument() {
			return argument;
		}

		public void setArgument(String argument) {
			this.argument = argument;
		}
		
		@Override
		public void copyArguments(QueryFilter other) {
			if (!(other instanceof QueryFilterTerm))
				return;
			this.argument = ((QueryFilterTerm)other).argument;
		}
		
		@Override
		public boolean isEmpty() {
			return argument==null || argument.trim().length()==0;
		}
		
		@Override
		public String getPredicate() {
			return "= "+argument;
		}
	}
	
	@JsonInclude(Include.NON_NULL)
	public static class QueryFilterList extends QueryFilter {
		@JsonProperty("a")
		private List<String> argument;
		
		public QueryFilterList() { }
		
		public QueryFilterList(String name) { 
			super(name);
			this.argument = new ArrayList<>();
		}

		public QueryFilterList(String name, String... arguments) {
			super(name);
			this.argument = new ArrayList<>();
			if (arguments!=null) {
				for (String a:arguments) {
					this.argument.add(a);
				}
			}
		}

		public QueryFilterList(String name, Collection<String> arguments) {
			super(name);
			this.argument = new ArrayList<>();
			if (arguments!=null) {
				for (String a:arguments) {
					this.argument.add(a);
				}
			}
		}
		
		public List<String> getArgument() {
			return argument;
		}

		public void setArgument(List<String> argument) {
			this.argument = argument;
		}
		
		public void addArgument(String argument) {
			if (this.argument==null)
				this.argument = new ArrayList<>();
			this.argument.add(argument);
		}
		
		public QueryFilterList withArgument(String argument) {
			addArgument(argument);
			return this;
		}

		@Override
		public void copyArguments(QueryFilter other) {
			if (!(other instanceof QueryFilterList))
				return;
			this.argument = new ArrayList<>(((QueryFilterList)other).argument);
		}
		
		@Override
		public boolean isEmpty() {
			return argument==null || argument.isEmpty();
		}
		
		@Override
		public String getPredicate() {
			String word;
			if (messageSource!=null)
				word = messageSource.getMessage("in", null, LocaleContextHolder.getLocale())+" ";
			else
				word = "in ";

			return word+String.join(",", argument);
		}
	}

	@JsonInclude(Include.NON_NULL)
	public static class QueryFilterBoolean extends QueryFilter {
		@JsonProperty("a")
		private String argument;
		
		public QueryFilterBoolean() { }
		
		public QueryFilterBoolean(String name) { 
			super(name);
		}

		public QueryFilterBoolean(String name, String argument) {
			super(name);
			this.argument = argument;
		}

		public String getArgument() {
			return argument;
		}

		public void setArgument(String argument) {
			this.argument = argument;
		}
		
		@Override
		public void copyArguments(QueryFilter other) {
			if (!(other instanceof QueryFilterBoolean))
				return;
			this.argument = ((QueryFilterBoolean)other).argument;
		}
		
		@Override
		public boolean isEmpty() {
			return argument==null || argument.trim().length()==0;
		}
		
		@JsonIgnore
		public boolean isArgumentTrue() {
			return "true".equalsIgnoreCase(argument);
		}

		@JsonIgnore
		public boolean isArgumentFalse() {
			return "false".equalsIgnoreCase(argument);
		}

		@Override
		public String getPredicate() {
			if (isArgumentTrue()) {
				if (messageSource!=null)
					return "= "+messageSource.getMessage("yes", null, LocaleContextHolder.getLocale());
				else
					return "= "+argument;				
			}
			else if (isArgumentFalse()) {
				if (messageSource!=null)
					return "= "+messageSource.getMessage("no", null, LocaleContextHolder.getLocale());
				else
					return "= "+argument;								
			}
			else {
				return null;
			}
		}
	}

	@JsonInclude(Include.NON_NULL)
	public static class QueryFilterRange extends QueryFilter {
		@JsonProperty("s")
		private String start;
		@JsonProperty("e")
		private String end;
		
		public QueryFilterRange() { }

		public QueryFilterRange(String name) { 
			super(name);
		}

		public QueryFilterRange(String name, String start, String end) {
			super(name);
			this.start = start;
			this.end = end;
		}
		
		public String getStart() {
			return start;
		}
		
		public void setStart(String start) {
			this.start = start;
		}
		
		public boolean hasStart() {
			return start!=null && start.trim().length()>0;
		}
		
		public String getEnd() {
			return end;
		}
		
		public void setEnd(String end) {
			this.end = end;
		}
		
		public boolean hasEnd() {
			return end!=null && end.trim().length()>0;
		}
		
		@Override
		public void copyArguments(QueryFilter other) {
			if (!(other instanceof QueryFilterRange))
				return;
			this.start = ((QueryFilterRange)other).start;
			this.end = ((QueryFilterRange)other).end;
		}

		@Override
		public boolean isEmpty() {
			return (start==null || start.trim().length()==0)
				&& (end==null || end.trim().length()==0);
		}

		@Override
		public String getPredicate() {
			List<String> terms = new ArrayList<>(2);
			if (hasStart() && hasEnd() && start.equals(end))
				terms.add("= "+start);
			else {
				if (hasStart())
					terms.add(">= "+start);
				if (hasEnd())
					terms.add("<= "+end);
			}
			if (terms.size()<2)
				return String.join("", terms);
			String separator;
			if (messageSource!=null)
				separator = " "+messageSource.getMessage("and", null, LocaleContextHolder.getLocale())+" ";
			else
				separator = ", ";
			return String.join(separator, terms);
		}
	}
	
	@JsonInclude(Include.NON_NULL)
	public static class QueryFilterValue extends QueryFilter {
		@JsonProperty("s")
		private Double start;
		@JsonProperty("e")
		private Double end;
		
		public QueryFilterValue() { }

		public QueryFilterValue(String name) { 
			super(name);
		}

		public QueryFilterValue(String name, Double start, Double end) {
			super(name);
			this.start = start;
			this.end = end;
		}
		
		public Double getStart() {
			return start;
		}
		
		public void setStart(Double start) {
			this.start = start;
		}
		
		public boolean hasStart() {
			return start!=null && !Double.isNaN(start);
		}
		
		public Double getEnd() {
			return end;
		}
		
		public void setEnd(Double end) {
			this.end = end;
		}
		
		public boolean hasEnd() {
			return end!=null && !Double.isNaN(end);
		}
		
		@Override
		public void copyArguments(QueryFilter other) {
			if (!(other instanceof QueryFilterValue))
				return;
			this.start = ((QueryFilterValue)other).start;
			this.end = ((QueryFilterValue)other).end;
		}

		@Override
		public boolean isEmpty() {
			return (start==null || Double.isNaN(start))
				&& (end==null || Double.isNaN(end));
		}

		@Override
		public String getPredicate() {
			List<String> terms = new ArrayList<>(2);
			if (hasStart())
				terms.add(">= "+start.longValue());
			if (hasEnd())
				terms.add("<= "+end.longValue());
			if (terms.size()<2)
				return String.join("", terms);
			String separator;
			if (messageSource!=null)
				separator = " "+messageSource.getMessage("and", null, LocaleContextHolder.getLocale())+" ";
			else
				separator = ", ";
			return String.join(separator, terms);
		}
	}

	@JsonInclude(Include.NON_NULL)
	public static class QueryFilterDate extends QueryFilter {
		@JsonProperty("s")
		private String start;
		@JsonProperty("e")
		private String end;
		
		public QueryFilterDate() { }

		public QueryFilterDate(String name) { 
			super(name);
		}

		public QueryFilterDate(String name, String start, String end) {
			super(name);
			this.start = start;
			this.end = end;
		}
		
		public String getStart() {
			return start;
		}
		
		public void setStart(String start) {
			this.start = start;
		}
		
		public boolean hasStart() {
			return start!=null && start.trim().length()>0;
		}
		
		public String getEnd() {
			return end;
		}
		
		public void setEnd(String end) {
			this.end = end;
		}
		
		public boolean hasEnd() {
			return end!=null && end.trim().length()>0;
		}
		
		@Override
		public void copyArguments(QueryFilter other) {
			if (!(other instanceof QueryFilterDate))
				return;
			this.start = ((QueryFilterDate)other).start;
			this.end = ((QueryFilterDate)other).end;
		}

		@Override
		public boolean isEmpty() {
			return (start==null || start.trim().length()==0)
				&& (end==null || end.trim().length()==0);
		}

		@Override
		public String getPredicate() {
			List<String> terms = new ArrayList<>(2);
			if (hasStart())
				terms.add(">= "+start);
			if (hasEnd())
				terms.add("<= "+end);
			if (terms.size()<2)
				return String.join("", terms);
			String separator;
			if (messageSource!=null)
				separator = " "+messageSource.getMessage("and", null, LocaleContextHolder.getLocale())+" ";
			else
				separator = ", ";
			return String.join(separator, terms);
		}
	}

	@JsonInclude(Include.NON_NULL)
	public static class QueryFilterEnum extends QueryFilter {
		@JsonProperty("a")
		private String argument;
		
		@JsonIgnore
		private Class<? extends Enum<?>> enumeration;
		
		public QueryFilterEnum() { }

		public QueryFilterEnum(String name, Class<? extends Enum<?>> enumeration, MessageSource messageSource) {
			super(name);
			this.enumeration = enumeration;
			super.wiredTo(messageSource);
		}
		
		public String getArgument() {
			return argument;
		}

		public void setArgument(String argument) {
			this.argument = argument;
		}

		@JsonIgnore
		public Class<? extends Enum<?>> getEnumeration() {
			return enumeration;
		}

		@JsonIgnore
		public void setEnumeration(Class<? extends Enum<?>> enumeration) {
			this.enumeration = enumeration;
		}
		
		@JsonProperty(value="options", access = JsonProperty.Access.READ_ONLY)
		public List<FilterOption> getFilterOptions() {
			if (enumeration==null)
				return Collections.emptyList();
			List<FilterOption> options = new LinkedList<>();
			for (Enum<?> option: enumeration.getEnumConstants()) {
				options.add(new FilterOption(option));
			}
			return options;
		}

		@Override
		public void copyArguments(QueryFilter other) {
			if (!(other instanceof QueryFilterEnum))
				return;
			this.argument = ((QueryFilterEnum)other).argument;
			if (((QueryFilterEnum)other).enumeration!=null)
				this.enumeration = ((QueryFilterEnum)other).enumeration;
		}

		@Override
		public boolean isEmpty() {
			return argument==null || argument.trim().length()==0;
		}

		@Override
		public String getPredicate() {
			if (messageSource!=null && argument!=null)
				return "= "+messageSource.getMessage(argument, null, LocaleContextHolder.getLocale());
			else
				return "= "+argument;
		}
		
		public String getSelectedConstant() {
			return getSelectedConstant(enumeration);
		}

		public String getSelectedConstant(Class<?> enumeration) {
			if (argument==null || argument.trim().length()==0)
				return null;
			if (enumeration==null)
				return null;
			for (Object option: enumeration.getEnumConstants()) {
				if (((Enum<?>)option).name().equalsIgnoreCase(argument) || option.toString().equalsIgnoreCase(argument))
					return ((Enum<?>)option).name();
			}
			return null;
		}

		public class FilterOption {
			private final Enum<?> option;
			FilterOption(Enum<?> option) {
				this.option = option;
			}
			@JsonProperty(value="text", access = JsonProperty.Access.READ_ONLY)
			public String getText() {
				if (messageSource!=null)
					return messageSource.getMessage(toString(), null, LocaleContextHolder.getLocale());
				return toString();
			}
			@JsonProperty(value="value", access = JsonProperty.Access.READ_ONLY)
			public String getValue() {
				return toString();
			}
			public String toString() {
				return option.toString();
			}
		}
	}
	
	@JsonInclude(Include.NON_NULL)
	public static class QueryFilterOr extends QueryFilter {
		@JsonProperty("alt")
		private List<QueryFilter> alternatives;
		
		public QueryFilterOr() { }

		public QueryFilterOr(List<QueryFilter> alternatives, MessageSource messageSource) {
			this.alternatives = alternatives;
			wiredTo(messageSource);
		}

		public QueryFilterOr(MessageSource messageSource, QueryFilter... alternatives) {
			this.alternatives = Arrays.asList(alternatives);
			wiredTo(messageSource);
		}

		@Override
		public QueryFilter wiredTo(MessageSource messageSource) {
			if (alternatives!=null) {
				for (QueryFilter nested: alternatives) {
					nested.wiredTo(messageSource);
				}
			}
			return super.wiredTo(messageSource);
		}

		public List<QueryFilter> getAlternatives() {
			return alternatives;
		}

		public void setAlternatives(List<QueryFilter> alternatives) {
			this.alternatives = alternatives;
		}

		@Override
		public void copyArguments(QueryFilter other) {
			if (!(other instanceof QueryFilterOr))
				return;
			this.alternatives = ((QueryFilterOr)other).alternatives;
		}

		@Override
		public boolean isEmpty() {
			return alternatives==null || alternatives.isEmpty();
		}

		@Override
		public String getPredicate() {
			if (alternatives==null || alternatives.isEmpty())
				return "";
			String delimiter = (messageSource!=null) ? messageSource.getMessage("or", null, LocaleContextHolder.getLocale()) : "or";
			return alternatives.stream().map(QueryFilter::getPredicate).collect(Collectors.joining(" "+delimiter+" "));
		}
		
	}

	@JsonInclude(Include.NON_NULL)
	public static class QueryFilterExist extends QueryFilter {
		
		public QueryFilterExist() { }
		
		public QueryFilterExist(String name) { 
			super(name);
		}

		@Override
		public void copyArguments(QueryFilter other) {
			// This class does not use arguments
		}
		
		@Override
		public boolean isEmpty() {
			return false;
		}
		
		@Override
		public String getPredicate() {
			return "exists";
		}
	}

	@JsonInclude(Include.NON_NULL)
	public static class QueryFilterDoesNotExist extends QueryFilter {
		
		public QueryFilterDoesNotExist() { }
		
		public QueryFilterDoesNotExist(String name) { 
			super(name);
		}

		@Override
		public void copyArguments(QueryFilter other) {
			// This class does not use arguments
		}
		
		@Override
		public boolean isEmpty() {
			return false;
		}
		
		@Override
		public String getPredicate() {
			return "does not exist";
		}
	}

	public AdvancedSearch clone() {
		return new AdvancedSearch(this);
	}
}
