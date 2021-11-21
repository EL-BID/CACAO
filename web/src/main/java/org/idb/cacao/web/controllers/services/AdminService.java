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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.catalina.valves.rewrite.QuotedStringTokenizer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.http.client.config.RequestConfig;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.ResizeRequest;
import org.elasticsearch.client.indices.ResizeResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.idb.cacao.api.storage.FileSystemStorageService;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.utils.IndexNamesUtils;
import org.idb.cacao.web.controllers.rest.AdminAPIController;
import org.idb.cacao.web.controllers.ui.AdminUIController;
import org.idb.cacao.web.repositories.DocumentSituationHistoryRepository;
import org.idb.cacao.web.repositories.DocumentTemplateRepository;
import org.idb.cacao.web.repositories.DocumentUploadedRepository;
import org.idb.cacao.web.repositories.DomainTableRepository;
import org.idb.cacao.web.utils.CreateDocumentTemplatesSamples;
import org.idb.cacao.web.utils.ESUtils;
import org.idb.cacao.web.utils.ErrorUtils;
import org.idb.cacao.web.utils.HttpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service methods for administrative operations
 * 
 * @author Gustavo Figueiredo
 *
 */
@Service("AdminService")
public class AdminService {

	private static final Logger log = Logger.getLogger(AdminService.class.getName());

	@Autowired
	private RestHighLevelClient elasticsearchClient;

	@Autowired
	private Environment env;
	
	@Autowired
	private DocumentTemplateRepository templateRepository;
	
	@Autowired
	private DomainTableRepository domainTableRepository;
	
	@Autowired
	private DocumentUploadedRepository documentUploadedRepository;
	
	@Autowired
	private DocumentSituationHistoryRepository documentSituationHistoryRepository;

	@Autowired
	private DomainTableService domainTableService;
	
	@Autowired
	private FileSystemStorageService fileSystemStorageService;

    private RestTemplate restTemplate;

	/**
	 * Enumerates all the implemented administrative operations
	 */
	public static enum AdminOperations {
				
		HELP(AdminService::getHelp,
			"Returns information about all commands that can be issued through this terminal",
			new Option("c", "cmd", true, "Returns information about the provided command option")
			),

		COPY_CONFIG(AdminService::copyConfig,
			"Copy Kibana configurations from one SPACE to another, including index pattern definitions",
			new Option("s","source",true, "The identifier of the SPACE to copy from (source)"),
			new Option("t","target",true, "The identifier of the SPACE to copy to (target)")),
		
		COPY_DASHBOARD(AdminService::copyDashboard,
			"Copy dashboard from one SPACE to another, including all its references (visualizations and index patterns)",
			new Option("d","dashboard",true, "Identifier of the dashboard to copy"),
			new Option("s","source",true, "The identifier of the SPACE to copy from (source)"),
			new Option("t","target",true, "The identifier of the SPACE to copy to (target)")),
					
		COPY_INDEX(AdminService::copyIndex,
			"Make a copy of an ElasticSearch index",
			new Option("s", "source", true, "Name of the index to copy (source)."),
			new Option("d", "destination", true, "Name of the copy (destination). Should not exist yet.")
			),

		COPY_INDEX_PATTERN(AdminService::copyIndexPattern,
			"Copy index pattern from one SPACE to another",
			new Option("i","index",true, "Identifier or name of the index to copy. If absent will copy all the index patterns from one space to another."),
			new Option("s","source",true, "The identifier of the SPACE to copy from (source)"),
			new Option("t","target",true, "The identifier of the SPACE to copy to (target)")),

		DELETE(AdminService::delete,
				"Delete information from the application database",
				new Option("t","templates",false, "Deletes all templates and domain tables. Recreates domain tables automatically."),
				new Option("u","uploads",false, "Deletes all upload records and uploaded files."),
				new Option("v","validated",false, "Deletes all validated records."),
				new Option("a","all",false, "Deletes all the above options.")),
		
		KIBANA(AdminService::kibana,
			"Performs some operations on KIBANA",
			new Option("g","get",true, "Returns list of objects. Depending on the object type, each object may inform an identifier and a title. The parameter informed with this operation must be one of these: space, user, role, dashboard, pattern, visualization, lens"),
			new Option("s","space",true, "Inform the identifier of the SPACE when returning information about saved dashboards")),
		
		LOG(AdminService::log,
			"Search LOG records",
			new Option("l","limit",true, "Limit the number of lines to return. If not informed, use default 100."),
			new Option("q","query",true, "Expression to search for. Either use option 'q' or option 'r', but not both."),
			new Option("r","regex",true, "Regular expression to search for. Either use option 'q' or option 'r', but not both."),
			new Option("fd","first_day",true, "Limit the search for LOG entries greater than or equal to the informed day. The date must be informed in DD-MM-YYYY format. If not informed, consider any past date."),
			new Option("ld","last_day",true, "Limit the search for LOG entries lesser than or equal to the informed day. The date must be informed in DD-MM-YYYY format. If not informed, consider present date."),
			new Option("v","vicinity",true, "Includes a number of rows in the vicinity of the rows that matches that query and pattern regex informed in other options."),
			new Option("raw","raw_line",false, "Indicate that should output the LOG lines 'as is' (without previous treatment or cap length). If not informed, apply some treatments and cap lengths.")),
		
		SAMPLES(AdminService::samples,
			"Add to database sample data and other configurations",
			new Option("t","templates",false, "Adds to database sample templates according to built-in specifications.")),
		
		UPDATE_INDEX_PATTERN(AdminService::updateIndexPattern,
				"Updates the index pattern mapping in Kibana according to the index fields in ElasticSearch",
				new Option("i","index",true, "Identifier of the index pattern at Kibana to update. If absent, will update all existent index patterns."),
				new Option("s","space",true, "Identifier of the space containing index patterns to update. If ansent, will update all existent spaces."))
		
		;
		
		private final String description;
		private final Options commandLineOptions;
		private final AdminOperationImplementation implementation;
		
		AdminOperations(AdminOperationImplementation implementation, String description, Option... options) {
			this.implementation = implementation;
			this.description = description;
			commandLineOptions = new Options();
			for (Option option: options) {
				commandLineOptions.addOption(option);
			}
		}

		public Options getCommandLineOptions() {
			return commandLineOptions;
		}
		
		public String getDescription() {
			return description;
		}

		public Object doAction(AdminService context, CommandLine cmdLine) throws Exception {
			return implementation.doAction(context, cmdLine);
		}
	}
	
	@Autowired
	public AdminService(RestTemplateBuilder builder) {
		this.restTemplate = builder
				.setConnectTimeout(Duration.ofMinutes(5))
				.setReadTimeout(Duration.ofMinutes(5))
				.requestFactory(HttpUtils::getTrustAllHttpRequestFactory)
				.build();
	}

	/**
	 * Functional interface to be implemented by each of the AdminOperations
	 */
	@FunctionalInterface
	public static interface AdminOperationImplementation {
		
		public Object doAction(AdminService service, CommandLine cmdLine) throws Exception;
		
	}

	/**
	 * Performs an administrative operation according to one of the AdminOperations 
	 */
	public Object performOperation(String command) throws Exception {
		
		if (command==null || command.trim().length()==0)
			throw new Exception("No operation!");
		
		QuotedStringTokenizer tokenizer = new QuotedStringTokenizer(command);
		String[] command_parts = new String[tokenizer.countTokens()];
		for (int i=0; i<command_parts.length; i++) {
			command_parts[i] = tokenizer.nextToken();
		}
		
		String command_name = (command_parts.length>0) ? command_parts[0].toUpperCase() : null;
		
		AdminOperations op;
		try {
			op = AdminOperations.valueOf(command_name);
		}
		catch (Throwable ex) {
			throw new UnsupportedOperationException("Unknown operation: "+command_name);
		}
		if (op==null) {
			throw new UnsupportedOperationException("Unknown operation: "+command_name);
		}
		
		
		CommandLineParser parser = new DefaultParser();
		CommandLine cmdLine = null;
		try
		{
			cmdLine = parser.parse(op.getCommandLineOptions(), command_parts);
		}
		catch (ParseException ex)
		{
			throw new Exception("Invalid options for command "+op.name(), ex);
		}
		if (cmdLine==null) {
			throw new IllegalStateException("Invalid command line options for command "+op.name());
		}
		
		Object response = op.doAction(this, cmdLine);

		return response;
	}
	
	/**
	 * Return information about the implemented administrative operations
	 */
	public static Object getHelp(AdminService service, CommandLine cmdLine) throws Exception {
		if (cmdLine.hasOption("c")) {
			String command_name = cmdLine.getOptionValue("c").toUpperCase();
			AdminOperations op;
			try {
				op = AdminOperations.valueOf(command_name);
			}
			catch (Throwable ex) {
				throw new UnsupportedOperationException("Unknown operation: "+command_name);
			}
			if (op==null) {
				throw new UnsupportedOperationException("Unknown operation: "+command_name);
			}
			HelpFormatter formatter = new HelpFormatter();
			StringWriter buffer = new StringWriter();
			PrintWriter output = new PrintWriter(buffer);
			formatter.printHelp(output, /*width*/80, /*cmdLineSyntax*/command_name, 
				/*header*/null, op.getCommandLineOptions(), 
				formatter.getLeftPadding(), 
				formatter.getDescPadding(), 
				/*footer*/null);
			return buffer.toString();
		}
		else if (cmdLine.getArgs().length==2) {
			String command_name = cmdLine.getArgs()[1].toUpperCase();
			AdminOperations op;
			try {
				op = AdminOperations.valueOf(command_name);
			}
			catch (Throwable ex) {
				throw new UnsupportedOperationException("Unknown operation: "+command_name);
			}
			if (op==null) {
				throw new UnsupportedOperationException("Unknown operation: "+command_name);
			}
			HelpFormatter formatter = new HelpFormatter();
			StringWriter buffer = new StringWriter();
			PrintWriter output = new PrintWriter(buffer);
			formatter.printHelp(output, /*width*/80, /*cmdLineSyntax*/command_name, 
				/*header*/null, op.getCommandLineOptions(), 
				formatter.getLeftPadding(), 
				formatter.getDescPadding(), 
				/*footer*/null);
			return buffer.toString();			
		}
		else {
			StringBuilder header = new StringBuilder();
			header.append("Each command requires the command name (the first word in the command line) followed by zero, one or more command line options.\n");
			header.append("The command name is case insensitive. The command line options are not (they are case sensitive).\n");
			header.append("Each command line option has two forms: the short form is the option name preceded by one dash and the longer form is the option name preceded by two dashes. Both forms of any command line option works exactly the same way.\n");
			header.append("Some command line options may or may not require an additional parameter.\n");
			header.append("For example, the command 'help' was invoked with no command line options.\n");
			header.append("If you needed help about a particular command, you could execute the command 'help' with the command line option '-c' (or '--cmd') followed by a parameter indicating a particular command name.\n");
			header.append("For example, to get help about the command 'LOG', you could execute this:\n");
			header.append(" help -c LOG\n");
			header.append("Likewise, you could execute the long form of the '-c' command line options (i.e.: '--cmd'), like this:\n");
			header.append(" help --cmd LOG\n");
			header.append("Each command defines a different set of command line options. Please refer to the documentation for each one of them.\n");
			header.append("If the parameter for some command line option contains spaces, you need to wrap the parameter in double quotes.\n");
			header.append("\n");
			header.append("These are the command line options you may use with the 'HELP' command:\n");
			StringBuilder footer = new StringBuilder();
			footer.append("You may get HELP about any of these commands:\n");
			for (AdminOperations op: AdminOperations.values()) {
				footer.append(op.name());
				footer.append(" - ");
				footer.append(op.getDescription());
				footer.append("\n");
			}
			HelpFormatter formatter = new HelpFormatter();
			StringWriter buffer = new StringWriter();
			PrintWriter output = new PrintWriter(buffer);
			formatter.printHelp(output, /*width*/130, /*cmdLineSyntax*/"help", 
				/*header*/header.toString(), AdminOperations.HELP.getCommandLineOptions(), 
				formatter.getLeftPadding(), 
				formatter.getDescPadding(), 
				/*footer*/footer.toString());
			//response.append(buffer.toString());
			return buffer.toString();
		}
	}
	
	/**
	 * Make a copy of an ElasticSearch index
	 */
	public static Object copyIndex(AdminService service, CommandLine cmdLine) throws Exception {
		if (!cmdLine.hasOption("s"))
			throw new Exception("Missing required command line option 's'!");
		if (!cmdLine.hasOption("d"))
			throw new Exception("Missing required command line option 'd'!");
		
		String source_index_name = cmdLine.getOptionValue("s");
		String destination_index_name = cmdLine.getOptionValue("d");
		if (source_index_name.equals(destination_index_name)) 
			throw new Exception("Source index should not be the same as the destination index!");

		// Check if destination already exists
		boolean destination_exists;
    	try {
    		ESUtils.hasMappings(service.elasticsearchClient, destination_index_name);
    		
    		// the index may exist and have no mapping
    		// if there was now exception thrown, we know there was an index
    		destination_exists = true;
    	}
    	catch (Throwable ex) {
    		if (!ErrorUtils.isErrorNoIndexFound(ex))
    			throw ex;
    		destination_exists = false;
    	}
    	
    	if (destination_exists)
    		throw new Exception("Destination already exists!");

		// Make the index read only (necessary for 'clone')
		ESUtils.changeBooleanIndexSetting(service.elasticsearchClient, source_index_name, ESUtils.SETTING_READ_ONLY, /*setting_value*/true, /*closeAndReopenIndex*/false);
		
		try {

			// Make a copy of the current stored index, as is
			RequestConfig requestConfig = RequestConfig.custom()
			.setConnectTimeout(60000)
			.setSocketTimeout(120000)
			.build();
			RequestOptions options = RequestOptions.DEFAULT.toBuilder()
			.setRequestConfig(requestConfig)
			.build();
			ResizeRequest clone_request = new ResizeRequest(/*target_index*/destination_index_name, /*source_index*/source_index_name);
			clone_request.setTimeout(TimeValue.timeValueSeconds(60));
			clone_request.setMasterTimeout(TimeValue.timeValueSeconds(60));
			clone_request.setWaitForActiveShards(1);
			ResizeResponse resizeResponse = service.elasticsearchClient.indices().clone(clone_request, options);
			if (!resizeResponse.isAcknowledged()) {
				throw new RuntimeException("Could not copy index '"+source_index_name+"'. Failed to acknownledge CLONE to "+destination_index_name);
			}
			if (!resizeResponse.isShardsAcknowledged()) {
				throw new RuntimeException("Could not copy index '"+source_index_name+"'. Failed to acknownledge CLONE to "+destination_index_name);
			}


		}
		finally {
			ESUtils.changeBooleanIndexSetting(service.elasticsearchClient, source_index_name, ESUtils.SETTING_READ_ONLY, /*setting_value*/false, /*closeAndReopenIndex*/false);
			try {
				ESUtils.changeBooleanIndexSetting(service.elasticsearchClient, destination_index_name, ESUtils.SETTING_READ_ONLY, /*setting_value*/false, /*closeAndReopenIndex*/false);
			} catch (Throwable ex2) { }
		}
		
		return "SUCCESS";
	}
	
	/**
	 * Performs operations on LOG registry
	 */
	public static Object log(AdminService service, CommandLine cmdLine) throws Exception {
		
		File log_dir = AdminUIController.getLogDir();
		if (log_dir==null || !log_dir.exists()) {
			return "LOG dir is not configured property!";
		}
		
		int limit = AdminUIController.LOG_TAIL_MAX_LINES;
		if (cmdLine.hasOption("l")) {
			limit = Integer.parseInt(cmdLine.getOptionValue("l"));
		}
		
		Pattern search = null;
		if (cmdLine.hasOption("q")) {
			search = Pattern.compile(Pattern.quote(cmdLine.getOptionValue("q")),Pattern.CASE_INSENSITIVE);
		}
		else if (cmdLine.hasOption("r")) {
			search = Pattern.compile(cmdLine.getOptionValue("r"),Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);
		}

		Date minimumTimestamp = null;
		if (cmdLine.hasOption("fd")) {
			minimumTimestamp = new SimpleDateFormat("dd-MM-yyyy").parse(cmdLine.getOptionValue("fd"));
		}

		Date maximumTimestamp = null;
		if (cmdLine.hasOption("ld")) {
			maximumTimestamp = new SimpleDateFormat("dd-MM-yyyy").parse(cmdLine.getOptionValue("ld"));
		}
		
		Integer vicinity = null;
		if (cmdLine.hasOption("v")) {
			vicinity = Integer.parseInt(cmdLine.getOptionValue("v"));
		}
		
		boolean raw = cmdLine.hasOption("raw");
		
		// Let's avoid returning occurrences of ADMIN shell commands, unless the search pattern explicitly looks for them
		Pattern avoid = (search==null || search.matcher(AdminAPIController.LOG_PREFIX_FOR_SHELL_COMMANDS).find()) ? null 
				: Pattern.compile(AdminAPIController.LOG_PREFIX_FOR_SHELL_COMMANDS,Pattern.CASE_INSENSITIVE);
		
		String log = AdminUIController.getLogTail(limit, !raw, search, avoid, minimumTimestamp, maximumTimestamp, vicinity);
		if (log==null)
			return "";
		return log;
	}
		
	/**
	 * Performs some operation on KIBANA
	 */
	public static Object kibana(AdminService service, CommandLine cmdLine) throws Exception {

		if (cmdLine.hasOption("g")) {
			
			String objType = cmdLine.getOptionValue("g").trim().toLowerCase();
			switch (objType) {
			case "user":
			case "users": {
				Set<org.elasticsearch.client.security.user.User> users;
				try {
					users = ESUtils.getUsers(service.elasticsearchClient);
				}
				catch (ElasticsearchException ex) {
					users = null;
				}
				if (users==null || users.isEmpty())
					return Collections.emptyList();
				return users.stream().map(u->u.getUsername()).sorted().collect(Collectors.joining("\n"));
			}
			case "role":
			case "roles": {
				List<org.elasticsearch.client.security.user.privileges.Role> roles;
				try {
					roles = ESUtils.getRoles(service.elasticsearchClient);
				}
				catch (ElasticsearchException ex) {
					roles = null;
				}
				if (roles==null || roles.isEmpty())
					return Collections.emptyList();
				return roles.stream().map(r->r.getName()).sorted().collect(Collectors.joining("\n"));
			}
			case "space":
			case "spaces": {
				List<org.idb.cacao.web.utils.ESUtils.KibanaSpace> spaces = ESUtils.getKibanaSpaces(service.env, service.restTemplate);
				if (spaces==null || spaces.isEmpty())
					return Collections.emptyList();
				return spaces.stream().sorted().map(s->String.join("\t|\t", s.getId(), s.getName())).collect(Collectors.joining("\n"));
			}
			case "dashboard":
			case "dashboards": {
				List<org.idb.cacao.web.utils.ESUtils.KibanaSavedObject> dashboards = ESUtils.getKibanaDashboards(service.env, service.restTemplate , /*spaceId*/cmdLine.getOptionValue("s", null));
				if (dashboards==null || dashboards.isEmpty())
					return Collections.emptyList();
				return dashboards.stream().sorted().map(d->String.join("\t|\t", d.getId(), d.getTitle())).collect(Collectors.joining("\n"));
			}
			case "pattern":
			case "patterns": {
				List<org.idb.cacao.web.utils.ESUtils.KibanaSavedObject> patterns = ESUtils.getKibanaIndexPatterns(service.env, service.restTemplate , /*spaceId*/cmdLine.getOptionValue("s", null));
				if (patterns==null || patterns.isEmpty())
					return Collections.emptyList();
				return patterns.stream().sorted().map(p->String.join("\t|\t", p.getId(), p.getTitle())).collect(Collectors.joining("\n"));
			}
			case "vis":
			case "visualization":
			case "visualizations": {
				List<org.idb.cacao.web.utils.ESUtils.KibanaSavedObject> patterns = ESUtils.getKibanaVisualizations(service.env, service.restTemplate , /*spaceId*/cmdLine.getOptionValue("s", null));
				if (patterns==null || patterns.isEmpty())
					return Collections.emptyList();
				return patterns.stream().sorted().map(v->String.join("\t|\t", v.getId(), v.getTitle())).collect(Collectors.joining("\n"));
			}
			case "lens":
			case "lenses": {
				List<org.idb.cacao.web.utils.ESUtils.KibanaSavedObject> lens = ESUtils.getKibanaVisualizations(service.env, service.restTemplate , /*spaceId*/cmdLine.getOptionValue("s", null));
				if (lens==null || lens.isEmpty())
					return Collections.emptyList();
				return lens.stream().sorted().map(v->String.join("\t|\t", v.getId(), v.getTitle())).collect(Collectors.joining("\n"));
			}
			default:
				throw new UnsupportedOperationException("Unknown object type informed with 'get' operation! "+objType);
			}
			
		}
		else {
			throw new Exception("Missing operation for use with this command!");
		}
	}
	
	/**
	 * Copy one Dashboard from one Space to another, including all its references (visualizations and index patterns)
	 */
	public static Object copyDashboard(AdminService service, CommandLine cmdLine) throws Exception {
		
		if (!cmdLine.hasOption("d"))
			throw new Exception("Missing dashboard ID in command line option 'd'!");
		if (!cmdLine.hasOption("s"))
			throw new Exception("Missing the ID of the SOURCE SPACE in command line option 's'!");
		if (!cmdLine.hasOption("t"))
			throw new Exception("Missing the ID of the TARGET SPACE in command line option 't'!");
		
		String dashboardId = cmdLine.getOptionValue("d");
		String sourceSpaceId = cmdLine.getOptionValue("s");
		String targetSpaceId = cmdLine.getOptionValue("t");
		
		boolean success = ESUtils.copyKibanaSavedObjects(service.env, service.restTemplate, sourceSpaceId, targetSpaceId, "dashboard", new String[] { dashboardId });
		
		if (success) {
			try {
				ESUtils.copyTransitiveDependencies(service.env, service.restTemplate, sourceSpaceId, targetSpaceId, "dashboard", dashboardId, /*max_iterations*/5);
			}
			catch (Throwable ex) {
				log.log(Level.SEVERE, "Error copying transitive dependencies for dashboard "+dashboardId+" from "+sourceSpaceId+" to "+targetSpaceId, ex);
			}
		}
		
		if (success)
			return "SUCCESS";
		else
			return "FAILURE";
	}
	
	/**
	 * Add sample data and sample configurations to the database
	 */
	public static Object samples(AdminService service, CommandLine cmdLine) throws Exception {

		StringBuilder report = new StringBuilder();
		
		if (cmdLine.hasOption("t")) {
			// Add sample templates
			List<DocumentTemplate> samples = CreateDocumentTemplatesSamples.getSampleTemplates();
			
			for (DocumentTemplate s: samples) {
				service.templateRepository.saveWithTimestamp(s);
				report.append("Created template '").append(s.getName()).append("' version ").append(s.getVersion()).append("\n");
			}

		}
		
		return report.toString();
		
	}
	
	/**
	 * Updates index patterns at KIBANA
	 */
	public static Object updateIndexPattern(AdminService service, CommandLine cmdLine) throws Exception {

		String indexIdOrName = cmdLine.getOptionValue("i", null);
		String spaceId = cmdLine.getOptionValue("s");
		
		LongAdder countUpdates = new LongAdder();
		LongAdder countErrors = new LongAdder();

		if (spaceId==null || spaceId.trim().length()==0) {
			
			// Updates all spaces at Kibana
			
			List<org.idb.cacao.web.utils.ESUtils.KibanaSpace> spaces = ESUtils.getKibanaSpaces(service.env, service.restTemplate);
			for (org.idb.cacao.web.utils.ESUtils.KibanaSpace space: spaces) {
				try {
					
					updateIndexPattern(service, space.getId(), indexIdOrName, countUpdates, countErrors);
					
				}
				catch (Throwable ex) {
					log.log(Level.SEVERE, "Error while updating index patterns at Kibana space "+space.getId(), ex);
					countErrors.increment();
				}
			}
		}
		else {
			
			// Updates one particular Kibana space
			
			updateIndexPattern(service, spaceId, indexIdOrName, countUpdates, countErrors);
			
		}
		
		return new StringBuilder()
			.append("Number of index-patterns updated: ").append(countUpdates.longValue()).append("\n")
			.append("Number of errors while updating index-patterns: ").append(countErrors.longValue()).append("\n")
			.toString();
	}
	
	private static void updateIndexPattern(AdminService service, String spaceId, String indexIdOrName, LongAdder countUpdates, LongAdder countErrors) throws IOException {
		if (indexIdOrName==null || indexIdOrName.trim().length()==0) {
			
			// Updates all index patterns
			List<ESUtils.KibanaSavedObject> index_patterns = ESUtils.getKibanaIndexPatterns(service.env, service.restTemplate, spaceId);
			if (index_patterns==null || index_patterns.isEmpty()) {
				
				return;
				
			}
			else {
				
				final String[] indexPatternsIds = index_patterns.stream().map(ESUtils.KibanaSavedObject::getId).toArray(String[]::new);
				
				for (String patternId: indexPatternsIds) {
					updateIndexPatternInternal(service, spaceId, patternId, countUpdates, countErrors);
				}
			}
			
		}
		else {
			
			// Updates one specific index pattern
			
			boolean is_uuid;
			try{
			    UUID.fromString(indexIdOrName);
			    is_uuid = true;
			} catch (IllegalArgumentException exception){
				is_uuid = false;
			}
			
			if (is_uuid) {
			
				updateIndexPatternInternal(service, spaceId, indexIdOrName, countUpdates, countErrors);
				
			}
			else {
				
				List<ESUtils.KibanaSavedObject> index_patterns = ESUtils.getKibanaIndexPatterns(service.env, service.restTemplate, spaceId);
				if (index_patterns==null || index_patterns.isEmpty()) {
					
					return;
					
				}
				else {
					
					ESUtils.KibanaSavedObject index_pattern =
					index_patterns.stream().filter(i->String.CASE_INSENSITIVE_ORDER.compare(i.getTitle(), indexIdOrName)==0).findAny().orElse(null);
					
					if (index_pattern==null) {
						
						return;
						
					}
					else {
						
						updateIndexPatternInternal(service, spaceId, index_pattern.getId(), countUpdates, countErrors);

					}
					
				}

			}
			
		}
	}
	
	private static void updateIndexPatternInternal(AdminService service, String spaceId, String patternId, LongAdder countUpdates, LongAdder countErrors) {
		ESUtils.UpdateIndexPatternRequest indexPattern = new ESUtils.UpdateIndexPatternRequest();
		indexPattern.setRefresh_fields(true);
		indexPattern.setIndex_pattern(Collections.singletonMap("fields", Collections.emptyMap()));
		try {
			ESUtils.updateKibanaIndexPattern(service.env, service.restTemplate, spaceId, patternId, indexPattern);
			countUpdates.increment();
		}
		catch (Throwable ex) {
			countErrors.increment();
			if (countErrors.longValue()==1) {
				log.log(Level.SEVERE, "Error while updating index pattern "+patternId+" of space "+spaceId, ex);
			}
		}
	}
	
	/**
	 * Deletes information from the database
	 */
	public static Object delete(AdminService service, CommandLine cmdLine) throws Exception {
		
		StringBuilder report = new StringBuilder();
		
		if (cmdLine.hasOption("v") || cmdLine.hasOption("a")) {
			// Deletes all validated records for all document templates
			int deleted_indices = 0;
			long deleted_documents = 0;
			for (DocumentTemplate template: service.templateRepository.findAll(PageRequest.of(0, 10_000))) {
				String indexName = IndexNamesUtils.formatIndexNameForValidatedData(template);
				try {
					deleted_documents = ESUtils.countDocs(service.elasticsearchClient, indexName);
					ESUtils.deleteIndex(service.elasticsearchClient, indexName);
					deleted_indices++;
				}
				catch (Throwable ex) { }
			}
			report.append("Deleted ").append(deleted_indices).append(" indices containing ").append(deleted_documents).append(" validated data of uploaded files).\n");
		}

		if (cmdLine.hasOption("t") || cmdLine.hasOption("a")) {
			// Deletes all templates and domain tables. Recreates domain tables automatically.
			long count_templates = service.templateRepository.count();
			service.templateRepository.deleteAll();
			report.append("Deleted ").append(count_templates).append(" templates from database.\n");
			
			long count_domain_tables = service.domainTableRepository.count();
			service.domainTableRepository.deleteAll();
			report.append("Deleted ").append(count_domain_tables).append(" domain tables from database.\n");
			
			int count_domain_tables_created = service.domainTableService.assertDomainTablesForAllArchetypes(/*overwrite*/true);
			report.append("Created ").append(count_domain_tables_created).append(" built-in domain tables from template's archetypes.\n");
		}

		if (cmdLine.hasOption("u") || cmdLine.hasOption("a")) {
			// Deletes all upload records
			long count_uploads = service.documentUploadedRepository.count();
			service.documentUploadedRepository.deleteAll();
			service.documentSituationHistoryRepository.deleteAll();
			report.append("Deleted ").append(count_uploads).append(" upload records from database.\n");
			int deleted_files = service.fileSystemStorageService.deleteAll();
			report.append("Deleted ").append(deleted_files).append(" uploaded files from file storage.\n");
		}

		return report.toString();
	}
	
	/**
	 * Copy one or more index patterns from one Space to another
	 */
	public static Object copyIndexPattern(AdminService service, CommandLine cmdLine) throws Exception {
		
		if (!cmdLine.hasOption("s"))
			throw new Exception("Missing the ID of the SOURCE SPACE in command line option 's'!");
		if (!cmdLine.hasOption("t"))
			throw new Exception("Missing the ID of the TARGET SPACE in command line option 't'!");
		
		String indexIdOrName = cmdLine.getOptionValue("i", null);
		String sourceSpaceId = cmdLine.getOptionValue("s");
		String targetSpaceId = cmdLine.getOptionValue("t");
		
		if (indexIdOrName==null || indexIdOrName.trim().length()==0) {
			
			// Copy all index patterns
			List<ESUtils.KibanaSavedObject> index_patterns = ESUtils.getKibanaIndexPatterns(service.env, service.restTemplate, sourceSpaceId);
			if (index_patterns==null || index_patterns.isEmpty()) {
				
				return "No index patterns was found at space \""+sourceSpaceId+"\"";
				
			}
			else {
				
				final String[] indexPatternsIds = index_patterns.stream().map(ESUtils.KibanaSavedObject::getId).toArray(String[]::new);
				boolean success = ESUtils.copyKibanaSavedObjects(service.env, service.restTemplate, sourceSpaceId, targetSpaceId, "index-pattern", indexPatternsIds);
				if (success)
					return "SUCCESS";
				else
					return "FAILURE";
				
			}
			
		}
		else {
			
			// Copy one specific index pattern
			
			boolean is_uuid;
			try{
			    UUID.fromString(indexIdOrName);
			    is_uuid = true;
			} catch (IllegalArgumentException exception){
				is_uuid = false;
			}
			
			if (is_uuid) {
			
				boolean success = ESUtils.copyKibanaSavedObjects(service.env, service.restTemplate, sourceSpaceId, targetSpaceId, "index-pattern", new String[] { indexIdOrName });
				if (success)
					return "SUCCESS";
				else
					return "FAILURE";
				
			}
			else {
				
				List<ESUtils.KibanaSavedObject> index_patterns = ESUtils.getKibanaIndexPatterns(service.env, service.restTemplate, sourceSpaceId);
				if (index_patterns==null || index_patterns.isEmpty()) {
					
					return "No index patterns was found at space \""+sourceSpaceId+"\"";
					
				}
				else {
					
					ESUtils.KibanaSavedObject index_pattern =
					index_patterns.stream().filter(i->String.CASE_INSENSITIVE_ORDER.compare(i.getTitle(), indexIdOrName)==0).findAny().orElse(null);
					
					if (index_pattern==null) {
						
						return "No index pattern with name \""+indexIdOrName+"\" was found at space \""+sourceSpaceId+"\"";
						
					}
					else {
						
						boolean success = ESUtils.copyKibanaSavedObjects(service.env, service.restTemplate, sourceSpaceId, targetSpaceId, "index-pattern", new String[] { index_pattern.getId() });
						if (success)
							return "SUCCESS";
						else
							return "FAILURE";

					}
					
				}

			}
			
		}
				
	}
	
	/**
	 * Copy configurations from one Space to another, including index patterns.
	 */
	public static Object copyConfig(AdminService service, CommandLine cmdLine) throws Exception {

		if (!cmdLine.hasOption("s"))
			throw new Exception("Missing the ID of the SOURCE SPACE in command line option 's'!");
		if (!cmdLine.hasOption("t"))
			throw new Exception("Missing the ID of the TARGET SPACE in command line option 't'!");

		String sourceSpaceId = cmdLine.getOptionValue("s");
		String targetSpaceId = cmdLine.getOptionValue("t");
		
		StringBuilder report = new StringBuilder();
		
		List<ESUtils.KibanaSavedObject> configs = ESUtils.getKibanaConfig(service.env, service.restTemplate, sourceSpaceId);
		report.append("Number of 'config' saved objects at '"+sourceSpaceId+"': "+configs.size()+"\n");
		if (!configs.isEmpty()) {
			String[] ids = configs.stream().map(ESUtils.KibanaSavedObject::getId).sorted().toArray(String[]::new);
			boolean success = ESUtils.copyKibanaSavedObjects(service.env, service.restTemplate, sourceSpaceId, targetSpaceId, "config", ids);
			if (success)
				report.append("Config copied to '"+targetSpaceId+"' successfully!\n");
			else
				report.append("Failed to copy config to '"+targetSpaceId+"'!\n");
		}
		
		List<ESUtils.KibanaSavedObject> patterns = ESUtils.getKibanaIndexPatterns(service.env, service.restTemplate, sourceSpaceId);
		report.append("Number of 'index-pattern' saved objects at '"+sourceSpaceId+"': "+patterns.size()+"\n");
		if (!patterns.isEmpty()) {
			String[] ids = patterns.stream().map(ESUtils.KibanaSavedObject::getId).sorted().toArray(String[]::new);
			boolean success = ESUtils.copyKibanaSavedObjects(service.env, service.restTemplate, sourceSpaceId, targetSpaceId, "index-pattern", ids);
			if (success)
				report.append("Index-patterns copied to '"+targetSpaceId+"' successfully!\n");
			else
				report.append("Failed to copy index-patterns to '"+targetSpaceId+"'!\n");			
		}
		
		return report.toString();
	}

}