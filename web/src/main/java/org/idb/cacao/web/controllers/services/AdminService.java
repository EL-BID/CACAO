/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.services;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.catalina.valves.rewrite.QuotedStringTokenizer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.DeleteRecordsOptions;
import org.apache.kafka.clients.admin.DeleteRecordsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.RecordsToDelete;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.client.indices.ResizeRequest;
import org.elasticsearch.client.indices.ResizeResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.idb.cacao.api.DocumentSituation;
import org.idb.cacao.api.DocumentSituationHistory;
import org.idb.cacao.api.DocumentUploaded;
import org.idb.cacao.api.Taxpayer;
import org.idb.cacao.api.errors.CommonErrors;
import org.idb.cacao.api.errors.GeneralException;
import org.idb.cacao.api.storage.FileSystemStorageService;
import org.idb.cacao.api.templates.CustomDataGenerator;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.api.templates.DocumentInput;
import org.idb.cacao.api.templates.DocumentTemplate;
import org.idb.cacao.api.templates.DomainTable;
import org.idb.cacao.api.templates.FieldMapping;
import org.idb.cacao.api.templates.TemplateArchetype;
import org.idb.cacao.api.templates.TemplateArchetypes;
import org.idb.cacao.api.utils.DateTimeUtils;
import org.idb.cacao.api.utils.IndexNamesUtils;
import org.idb.cacao.api.utils.MappingUtils;
import org.idb.cacao.api.utils.ParserUtils;
import org.idb.cacao.api.utils.RandomDataGenerator;
import org.idb.cacao.api.utils.ScrollUtils;
import org.idb.cacao.web.controllers.rest.AdminAPIController;
import org.idb.cacao.web.controllers.ui.AdminUIController;
import org.idb.cacao.web.dto.FileUploadedEvent;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.repositories.DocumentSituationHistoryRepository;
import org.idb.cacao.web.repositories.DocumentTemplateRepository;
import org.idb.cacao.web.repositories.DocumentUploadedRepository;
import org.idb.cacao.web.repositories.DocumentValidationErrorMessageRepository;
import org.idb.cacao.web.repositories.DomainTableRepository;
import org.idb.cacao.web.repositories.SyncCommitHistoryRepository;
import org.idb.cacao.web.repositories.SyncCommitMilestoneRepository;
import org.idb.cacao.web.repositories.TaxpayerRepository;
import org.idb.cacao.web.utils.CreateDocumentTemplatesSamples;
import org.idb.cacao.web.utils.ESUtils;
import org.idb.cacao.web.utils.ErrorUtils;
import org.idb.cacao.web.utils.generators.FileGenerator;
import org.idb.cacao.web.utils.generators.FileGenerators;
import org.idb.cacao.web.utils.generators.SampleCompanySizes;
import org.idb.cacao.web.utils.generators.SampleCounty;
import org.idb.cacao.web.utils.generators.SampleEconomicSectors;
import org.idb.cacao.web.utils.generators.SampleLegalEntities;
import org.idb.cacao.web.utils.generators.SampleTaxRegimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;

/**
 * Service methods for administrative operations
 * 
 * @author Gustavo Figueiredo
 *
 */
@Service("AdminService")
public class AdminService {

	private static final Logger log = Logger.getLogger(AdminService.class.getName());
	
	private static final String FAILURE = "FAILURE";
	private static final String SUCCESS = "SUCCESS";	
	
	/**
	 * Default number of parallel threads to be used for generating documents with random data
	 */
	private static final int DEFAULT_PARALLELISM_FOR_DATA_GENERATOR = 4;

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
	private DocumentValidationErrorMessageRepository documentValidationErrorMessageRepository;

	@Autowired
	private DomainTableService domainTableService;
	
	@Autowired
	private FileSystemStorageService fileSystemStorageService;
	
    @Autowired
    private ResourceMonitorService sysInfoService;
    
    @Autowired
    private ElasticSearchService elasticSearchService;
    
    @Autowired
    private KibanaSpacesService kibanaSpacesService;

	@Autowired
	private FileUploadedProducer fileUploadedProducer;
	
	@Autowired
	private TaxpayerRepository taxPayerRepository;

	@Autowired
	private SyncCommitMilestoneRepository syncCommitMilestoneRepository;
	
	@Autowired
	private SyncCommitHistoryRepository syncCommitHistoryRepository;
	
	@Autowired
	private UserService userService;

	@Autowired
	private MessageSource messages;

    private RestTemplate restTemplate;

	/**
	 * Enumerates all the implemented administrative operations
	 */
	public enum AdminOperations {
				
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
				new Option("t","template",true, "Deletes the template with the name provided as argument. If there is more than one version for the same template, you may inform a specific version separated from the template name by a colon. Otherwise, it will delete all versions of the same template. If provided the argument 'all', it will delete all templates."),
				new Option("dt","domain_table",true, "Deletes the domain table with the name provided as argument. If there is more than one version for the same domain table, you may inform a specific version separated from the table name by a colon. Otherwise, it will delete all versions of the same domain table.  If provided the argument 'all', it will delete all domain tables. Recreates built-in domain tables automatically."),
				new Option("u","uploads",false, "Deletes all upload records and uploaded files."),
				new Option("v","validated",false, "Deletes all validated records."),
				new Option("p","published",false, "Deletes all published (denormalized) views."),
				new Option("s","sync",false, "Deletes all history of SYNC operations."),
				new Option("txp","taxpayers",false, "Deletes all taxpayers (their names and other information in registry)."),
				new Option("kp","kibana_patterns",false, "Deletes all index patterns related to CACAO from all spaces of Kibana."),
				new Option("a","all",false, "Deletes all data from ElasticSearch (corresponds to all the other options, except Kibana)")),
		
		KAFKA(AdminService::kafka,
				"Returns information about KAFKA",
				new Option("m","metrics",false, "Collects metrics about KAFKA"),
				new Option("c","consumers",false, "Returns information about consumers of KAFKA (groups, topics, partitions and offsets)"),
				new Option("t","topics",false, "Returns information about topics of KAFKA (topics, partitions and offsets)"),
				new Option("d","delete",true, "Deletes messages from KAFKA topics. The option parameter must be one of the following: the text 'all' for deleting all topics and all partitions, "
						+ "or some expression of format 'topic-name:partition-number:max-offset' for informing a specific topic, specific partition number and a maximum offset in partition (will delete all offsets below this number). "
						+ "You may also inform the expression 'topic-name:partition-number' for deleting all messages from the given partition. "
						+ "You may also inform the expression 'topic-name' for deleting all messages for all partitions of the give topic.")),
		
		KIBANA(AdminService::kibana,
			"Performs some operations on KIBANA",
			new Option("g","get",true, "Returns list of objects. Depending on the object type, each object may inform an identifier and a title. The parameter informed with this operation must be one of these: space, user, role, dashboard, pattern, visualization, lens"),
			new Option("s","space",true, "Inform the identifier of the SPACE when returning information about saved dashboards"),
			new Option("cs","create_spaces",false, "Creates missing Kibana SPACE's related to CACAO standards"),
			new Option("cr","create_roles",false, "Creates missing Kibana roles related to CACAO standards"),
			new Option("cp","create_patterns",false, "Creates missing index-patterns in all Kibana SPACE's related to all of the CACAO standard indices at ElasticSearch")),
		
		LOG(AdminService::log,
			"Search LOG records",
			new Option("l","limit",true, "Limit the number of lines to return. If not informed, use default 100."),
			new Option("q","query",true, "Expression to search for. Either use option 'q' or option 'r', but not both."),
			new Option("r","regex",true, "Regular expression to search for. Either use option 'q' or option 'r', but not both."),
			new Option("fd","first_day",true, "Limit the search for LOG entries greater than or equal to the informed day. The date must be informed in DD-MM-YYYY format. If not informed, consider any past date."),
			new Option("ld","last_day",true, "Limit the search for LOG entries lesser than or equal to the informed day. The date must be informed in DD-MM-YYYY format. If not informed, consider present date."),
			new Option("v","vicinity",true, "Includes a number of rows in the vicinity of the rows that matches that query and pattern regex informed in other options."),
			new Option("raw","raw_line",false, "Indicate that should output the LOG lines 'as is' (without previous treatment or cap length). If not informed, apply some treatments and cap lengths.")),
		
		LOGDIR(AdminService::logdir,
				"Return information about the directory for LOG files"),
		
		REDO(AdminService::redo,
			"Redo some operation",
			new Option("val","validation",true, "Redo the validation phase for one or more documents. Inform 'all' as parameter for this option in order to redo the validation for all previously uploaded documents. "
					+ "Inform 'unprocessed' as parameter for this option in order to redo only those documents that are not in PROCESSED state."
					+ "Inform 'processed' or 'pending' or 'received' or 'invalid' or 'valid' as parameter for this option in order to redo only those documents that are in the corresponding state.")),

		SAMPLES(AdminService::samples,
			"Add to database sample data and other configurations",
			new Option("t","templates",false, "Adds to database sample templates according to built-in specifications. Ignores existent templates (i.e. with the same name and version)"),
			new Option("dt","domain_tables",false, "Adds to database domain tables according to built-in specifications. Ignores existent domain tables (i.e. with the same name and version)"),
			new Option("d","docs",true, "Adds to database sample documents with random data according to the provided template name. The template name must be informed with this option, following this parameter indication. The taxpayer ID is also created randomly and the corresponding taxpayer record is created accordingly if absent."),
			new Option("bg","background",false, "Run the command at background (i.e.: do not wait until all the documents are created). This parameter is only considered together with 'docs' parameter. If not informed, waits until all the documents are generated (regardless the 'validation' and 'ETL' phases)."),
			new Option("s","seed",true, "Informs a word or number to be used as 'SEED' for generating random numbers. Different seeds will result in different contents. This parameter is only considered together with 'docs' parameter. If not informed, use a randomly generated seed."),
			new Option("y","year",true, "Informs the year to be used for generating random data (i.e. for dates and other periods). This parameter is only considered together with 'docs' parameter. If not informed, use the year before the current year."),
			new Option("p","period",true, "Informs the years interval to be used for generating random data (i.e. for dates and other periods). This parameter is only considered together with 'docs' parameter. If not informed, use the year before the current year. If it's informed with year parameter, it prevails."),
			new Option("thr","threads",true, "Number of parallel threads for generating random data (does not apply to validator/ETL phases). This parameter is only considered together with 'docs' parameter. If not informed, use "+DEFAULT_PARALLELISM_FOR_DATA_GENERATOR+" parallel threads."),
			new Option("ldoc","limit_docs",true, "Limit the number of sample documents to create. This parameter is only considered together with 'docs' parameter. If not informed, use default 10."),
			new Option("lrec","limit_records",true, "Limit the number of records to create. This parameter is only considered together with 'docs' parameter. If not informed, use some built-in default (usually 10000, but may be different depending on the archetype).")),
		
		TABLES(AdminService::tables,
			"Lists all the domain tables registered in the system"),
		
		TEMPLATES(AdminService::templates,
				"Lists all the templates registered in the system"),
		
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
	public AdminService(RestTemplateBuilder builder, InternalHttpRequestsService requestFactory) {
		this.restTemplate = builder
				.setConnectTimeout(Duration.ofMinutes(5))
				.setReadTimeout(Duration.ofMinutes(5))
				.requestFactory(requestFactory)
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
			throw new GeneralException("No operation!");
		
		QuotedStringTokenizer tokenizer = new QuotedStringTokenizer(command);
		String[] commandParts = new String[tokenizer.countTokens()];
		for (int i=0; i<commandParts.length; i++) {
			commandParts[i] = tokenizer.nextToken();
		}
		
		String commandName = (commandParts.length>0) ? commandParts[0].toUpperCase() : null;
		
		AdminOperations op;
		try {
			op = AdminOperations.valueOf(commandName);
		}
		catch (Exception ex) {
			throw new UnsupportedOperationException("Unknown operation: "+commandName);
		}
		if (op==null) {
			throw new UnsupportedOperationException("Unknown operation: "+commandName);
		}
		
		
		CommandLineParser parser = new DefaultParser();
		CommandLine cmdLine = null;
		try
		{
			cmdLine = parser.parse(op.getCommandLineOptions(), commandParts);
		}
		catch (ParseException ex)
		{
			throw new GeneralException("Invalid options for command "+op.name(), ex);
		}
		if (cmdLine==null) {
			throw new IllegalStateException("Invalid command line options for command "+op.name());
		}
		
		return op.doAction(this, cmdLine);
	}
	
	/**
	 * Return information about the implemented administrative operations
	 */
	public static Object getHelp(AdminService service, CommandLine cmdLine) throws Exception {
		if (cmdLine.hasOption("c")) {
			String commandName = cmdLine.getOptionValue("c").toUpperCase();
			return getHelp(commandName);
		}
		else if (cmdLine.getArgs().length==2) {
			String commandName = cmdLine.getArgs()[1].toUpperCase();
			AdminOperations op;
			try {
				op = AdminOperations.valueOf(commandName);
			}
			catch (Exception ex) {
				throw new UnsupportedOperationException("Unknown operation: "+commandName);
			}
			if (op==null) {
				throw new UnsupportedOperationException("Unknown operation: "+commandName);
			}
			HelpFormatter formatter = new HelpFormatter();
			StringWriter buffer = new StringWriter();
			PrintWriter output = new PrintWriter(buffer);
			formatter.printHelp(output, /*width*/80, /*cmdLineSyntax*/commandName, 
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
	
	public static String getHelp(String commandName) {
		AdminOperations op;
		try {
			op = AdminOperations.valueOf(commandName);
		}
		catch (Exception ex) {
			throw new UnsupportedOperationException("Unknown operation: "+commandName);
		}
		if (op==null) {
			throw new UnsupportedOperationException("Unknown operation: "+commandName);
		}
		return getHelp(op);
	}
	
	public static String getHelp(AdminOperations op) {
		HelpFormatter formatter = new HelpFormatter();
		StringWriter buffer = new StringWriter();
		PrintWriter output = new PrintWriter(buffer);
		formatter.printHelp(output, /*width*/80, /*cmdLineSyntax*/op.name(), 
			/*header*/null, op.getCommandLineOptions(), 
			formatter.getLeftPadding(), 
			formatter.getDescPadding(), 
			/*footer*/null);
		return buffer.toString();
	}
	
	/**
	 * Make a copy of an ElasticSearch index
	 */
	public static Object copyIndex(AdminService service, CommandLine cmdLine) throws Exception {
		if (!cmdLine.hasOption("s"))
			throw new GeneralException("Missing required command line option 's'!");
		if (!cmdLine.hasOption("d"))
			throw new GeneralException("Missing required command line option 'd'!");
		
		String sourceIndexName = cmdLine.getOptionValue("s");
		String destinationIndexName = cmdLine.getOptionValue("d");
		if (sourceIndexName.equals(destinationIndexName)) 
			throw new GeneralException("Source index should not be the same as the destination index!");

		// Check if destination already exists
		boolean destinationExists;
    	try {
    		MappingUtils.hasMappings(service.elasticsearchClient, destinationIndexName);
    		
    		// the index may exist and have no mapping
    		// if there was now exception thrown, we know there was an index
    		destinationExists = true;
    	}
    	catch (Exception ex) {
    		if (!ErrorUtils.isErrorNoIndexFound(ex))
    			throw ex;
    		destinationExists = false;
    	}
    	
    	if (destinationExists)
    		throw new GeneralException("Destination already exists!");

		// Make the index read only (necessary for 'clone')
		ESUtils.changeBooleanIndexSetting(service.elasticsearchClient, sourceIndexName, ESUtils.SETTING_READ_ONLY, /*setting_value*/true, /*closeAndReopenIndex*/false);
		
		try {

			// Make a copy of the current stored index, as is
			RequestConfig requestConfig = RequestConfig.custom()
			.setConnectTimeout(60000)
			.setSocketTimeout(120000)
			.build();
			RequestOptions options = RequestOptions.DEFAULT.toBuilder()
			.setRequestConfig(requestConfig)
			.build();
			ResizeRequest cloneRequest = new ResizeRequest(/*target_index*/destinationIndexName, /*source_index*/sourceIndexName);
			cloneRequest.setTimeout(TimeValue.timeValueSeconds(60));
			cloneRequest.setMasterTimeout(TimeValue.timeValueSeconds(60));
			cloneRequest.setWaitForActiveShards(1);
			ResizeResponse resizeResponse = service.elasticsearchClient.indices().clone(cloneRequest, options);
			if (!resizeResponse.isAcknowledged()) {
				throw new RuntimeException("Could not copy index '"+sourceIndexName+"'. Failed to acknownledge CLONE to "+destinationIndexName);
			}
			if (!resizeResponse.isShardsAcknowledged()) {
				throw new RuntimeException("Could not copy index '"+sourceIndexName+"'. Failed to acknownledge CLONE to "+destinationIndexName);
			}


		}
		finally {
			ESUtils.changeBooleanIndexSetting(service.elasticsearchClient, sourceIndexName, ESUtils.SETTING_READ_ONLY, /*setting_value*/false, /*closeAndReopenIndex*/false);
			try {
				ESUtils.changeBooleanIndexSetting(service.elasticsearchClient, destinationIndexName, ESUtils.SETTING_READ_ONLY, /*setting_value*/false, /*closeAndReopenIndex*/false);
			} catch (Exception ex2) {
				log.log(Level.INFO, ex2.getMessage(), ex2);
			}
		}
		
		return SUCCESS;
	}
	
	/**
	 * Performs operations on LOG registry
	 */
	public static Object log(AdminService service, CommandLine cmdLine) throws Exception {
		
		File logDir = AdminUIController.getLogDir();
		if (logDir==null || !logDir.exists()) {
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
	 * Return information about the directory for LOG files
	 */
	public static Object logdir(AdminService service, CommandLine cmdLine) {
		
		File logDir = AdminUIController.getLogDir();
		
		StringBuilder report = new StringBuilder();
		if (logDir==null) {
			report.append("LOG directory was not defined!");
		}
		else {
			report.append("LOG directory: ").append(logDir.getAbsolutePath()).append("\n");
			if (!logDir.exists()) {
				report.append("Directory does not exists!\n");
			}
			else {
				long totalSpace = logDir.getTotalSpace();
				long freeSpace = logDir.getFreeSpace();
				report.append("Total space at LOG disk: ").append(totalSpace).append("\n");
				report.append("Free space at LOG disk: ").append(freeSpace).append("\n");
			}
		}
		
		return report.toString();
	}
	
	/**
	 * Performs some operation on KIBANA
	 */
	public static Object kibana(AdminService service, CommandLine cmdLine) throws Exception {

		Option[] options = cmdLine.getOptions();
		if (options==null || options.length==0) {		
			return getHelp(AdminOperations.KIBANA);
		}

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
				return users.stream().map(org.elasticsearch.client.security.user.User::getUsername).sorted().collect(Collectors.joining("\n"));
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
				return roles.stream().map(org.elasticsearch.client.security.user.privileges.Role::getName).sorted().collect(Collectors.joining("\n"));
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

		StringBuilder report = new StringBuilder();

		if (cmdLine.hasOption("cs")) {			
			service.elasticSearchService.assertStandardSpaces(/*collectCreationInfo*/report::append);
		}

		if (cmdLine.hasOption("cr")) {			
			service.elasticSearchService.assertStandardRoles(/*collectCreationInfo*/report::append);
		}

		if (cmdLine.hasOption("cp")) {
			service.kibanaSpacesService.clearChecked();
			service.kibanaSpacesService.syncKibanaIndexPatterns(/*collectCreationInfo*/report::append);
		}

		return report.toString();
	}
	
	/**
	 * Copy one Dashboard from one Space to another, including all its references (visualizations and index patterns)
	 */
	public static Object copyDashboard(AdminService service, CommandLine cmdLine) throws Exception {
		
		if (!cmdLine.hasOption("d"))
			throw new GeneralException("Missing dashboard ID in command line option 'd'!");
		if (!cmdLine.hasOption("s"))
			throw new GeneralException("Missing the ID of the SOURCE SPACE in command line option 's'!");
		if (!cmdLine.hasOption("t"))
			throw new GeneralException("Missing the ID of the TARGET SPACE in command line option 't'!");
		
		String dashboardId = cmdLine.getOptionValue("d");
		String sourceSpaceId = cmdLine.getOptionValue("s");
		String targetSpaceId = cmdLine.getOptionValue("t");
		
		boolean success = ESUtils.copyKibanaSavedObjects(service.env, service.restTemplate, sourceSpaceId, targetSpaceId, "dashboard", new String[] { dashboardId });
		
		if (success) {
			try {
				ESUtils.copyTransitiveDependencies(service.env, service.restTemplate, sourceSpaceId, targetSpaceId, "dashboard", dashboardId, /*max_iterations*/5);
			}
			catch (Exception ex) {
				log.log(Level.SEVERE, ex, () -> "Error copying transitive dependencies for dashboard "+dashboardId+" from "+sourceSpaceId+" to "+targetSpaceId);
			}
		}
		
		if (success)
			return SUCCESS;
		else
			return FAILURE;
	}
	
	/**
	 * Add sample data and sample configurations to the database
	 */
	public static Object samples(AdminService service, CommandLine cmdLine) throws Exception {

		Option[] options = cmdLine.getOptions();
		if (options==null || options.length==0) {		
			return getHelp(AdminOperations.SAMPLES);
		}

		StringBuilder report = new StringBuilder();
		
		if (cmdLine.hasOption("dt")) {
			// Creates built-in domain tables
			int countDomainTablesCreated = service.domainTableService.assertDomainTablesForAllArchetypes(/*overwrite*/false);
			report.append("Created ").append(countDomainTablesCreated).append(" built-in domain tables from template's archetypes.\n");
		}
		
		if (cmdLine.hasOption("t")) {
			// Add sample templates
			
			Locale defaultLocale = new Locale(service.env.getProperty("cacao.user.language"), service.env.getProperty("cacao.user.country"));
			
			List<DocumentTemplate> samples = CreateDocumentTemplatesSamples.getSampleTemplates(service.messages, defaultLocale);
			
			for (DocumentTemplate s: samples) {
				if (service.templateRepository.findByNameIgnoreCaseAndVersion(s.getName(), s.getVersion()).isPresent())
					continue;
				service.templateRepository.saveWithTimestamp(s);
				report.append("Created template '").append(s.getName()).append("' version ").append(s.getVersion()).append("\n");
			}

		}
		
		if (cmdLine.hasOption("d")) {
			// Add sample data (documents) according to provided template name
			String templateName = cmdLine.getOptionValue("d");
			if (templateName.trim().length()==0)
				report.append("Missing template name with 'd' option!");
			else {
				boolean background = cmdLine.hasOption("bg");
				int limitDocs = (cmdLine.hasOption("ldoc")) ? Integer.parseInt(cmdLine.getOptionValue("ldoc")) : 10;
				long fixedLimitRecords = (cmdLine.hasOption("lrec")) ? Long.parseLong(cmdLine.getOptionValue("lrec")) : -1;
				String seedWord = (cmdLine.hasOption("s")) ? cmdLine.getOptionValue("s") : null;
				List<Integer> years = new ArrayList<>(2);
				years.add((cmdLine.hasOption("y")) ? Integer.parseInt(cmdLine.getOptionValue("y")) : Year.now().getValue() - 1);
				String period = (cmdLine.hasOption("p")) ? cmdLine.getOptionValue("p") : null;
				int threads = (cmdLine.hasOption("thr")) ? Integer.parseInt(cmdLine.getOptionValue("thr")) : DEFAULT_PARALLELISM_FOR_DATA_GENERATOR;
				
				years.add(years.get(0));
				if ( period != null ) {				
					String[] yearsPeriod = period.split(":");
					if ( yearsPeriod.length > 0 )
						years.set(0, Integer.parseInt(yearsPeriod[0]));
					if ( yearsPeriod.length > 1 )
						years.set(1, Integer.parseInt(yearsPeriod[1]));
					else
						years.set(1, Integer.parseInt(yearsPeriod[0]));
				}					
				
				Authentication auth = SecurityContextHolder.getContext().getAuthentication();
				RequestAttributes reqAttr = RequestContextHolder.currentRequestAttributes();
				String remoteIpAddr = (reqAttr instanceof ServletRequestAttributes) ? ((ServletRequestAttributes)reqAttr).getRequest().getRemoteAddr() : null;

				if (background) {
					new Thread("SampleDocuments") {
						{
							setDaemon(true);
						}
						@Override
						public void run() {
							StringBuilder bgReport = new StringBuilder();
							bgReport.append("Report for background process of creation of ").append(limitDocs).append(" documents with random data for template ").append(templateName).append("\n");
							try {
								for ( int year = years.get(0); year <= years.get(1); year++ )
									service.createSampleDocuments(auth, remoteIpAddr, templateName.trim(), limitDocs, fixedLimitRecords, seedWord, year, bgReport, threads);
							}
							catch (Exception ex) {
								log.log(Level.SEVERE, ex, () -> "Error while generating documents with arguments: "+String.join(" ",cmdLine.getArgs()));
							}
							finally {
								log.log(Level.INFO, bgReport.toString());
							}
						}
					}.start();
					report.append("Creating ").append(limitDocs).append(" documents with random data for template ").append(templateName).append(" at background\n");
				}
				else {
					for ( int year = years.get(0); year <= years.get(1); year++ )
						service.createSampleDocuments(auth, remoteIpAddr, templateName.trim(), limitDocs, fixedLimitRecords, seedWord, year, report, threads);
				}
			}
		}
		
		return report.toString();
		
	}
	
	/**
	 * Lists the domain tables
	 */
	public static Object tables(AdminService service, CommandLine cmdLine) {
		StringBuilder report = new StringBuilder();
		
		report.append(String.format("%-40s\t%-10s\t%s%n", "name", "version", "group"));
		try (Stream<DomainTable> tables = ScrollUtils.findAll(service.domainTableRepository, service.elasticsearchClient, 10).sorted();) {
			tables.forEach(t->report.append(String.format("%-40s\t%-10s\t%s%n", t.getName(), t.getVersion(), t.getGroup()==null?"":t.getGroup())));
		}

		return report.toString();
	}
	
	/**
	 * Lists the templates
	 */
	public static Object templates(AdminService service, CommandLine cmdLine) {
		StringBuilder report = new StringBuilder();

		report.append(String.format("%-40s\t%-10s\t%s%n", "name", "version", "group"));
		try (Stream<DocumentTemplate> templates = ScrollUtils.findAll(service.templateRepository, service.elasticsearchClient, 10).sorted();) {
			templates.forEach(t->report.append(String.format("%-40s\t%-10s\t%s%n", t.getName(), t.getVersion(), t.getGroup()==null?"":t.getGroup())));
		}

		return report.toString();
	}

	/**
	 * Creates sample documents with random data according to the provided template
	 */
	private void createSampleDocuments(Authentication auth, String remoteIpAddr, String templateName, int limitDocs, 
			long fixedLimitRecords, String seedWord, int year, StringBuilder report, int threads) throws Exception {
		List<DocumentTemplate> templatesVersions = templateRepository.findByNameIgnoreCase(templateName);
		if (templatesVersions==null || templatesVersions.isEmpty()) {
			report.append("Could not find a template with name: ").append(templateName).append("\n");
			return;
		}
		
		DocumentTemplate template;
		
		if (templatesVersions.size()>1) {
			// In case of multiple versions, use the last one
			template = templatesVersions.stream()
					.filter(t->t.getInputs()!=null && !t.getInputs().isEmpty())
					.sorted(Comparator.comparing(DocumentTemplate::getTemplateCreateTime).reversed())
					.findFirst().orElse(templatesVersions.get(0));
		}
		else {
			template = templatesVersions.get(0);
		}
		
		List<DocumentInput> inputFormats = template.getInputs();
		if (inputFormats==null || inputFormats.isEmpty()) {
			report.append("Template ").append(template.getName()).append(" version ").append(template.getVersion()).append(" has no input fields definitions!\n");
			return;			
		}
		
		Optional<TemplateArchetype> archetype =
				(template.getArchetype() != null && template.getArchetype().trim().length() > 0)
			? TemplateArchetypes.getArchetype(template.getArchetype())
			: Optional.empty();

		long seed;
		if (seedWord==null || seedWord.trim().length()==0)
			seed = UUID.randomUUID().getLeastSignificantBits() ^ UUID.randomUUID().getMostSignificantBits();
		else {
			seed = ByteBuffer.wrap(Hashing.sha256().hashString(seedWord, StandardCharsets.UTF_8).asBytes()).asLongBuffer().get();
		}
		
        ExecutorService executor = (limitDocs>1) ? Executors.newFixedThreadPool(threads) : null;

		// Let's use this for generating SEED per document
		Random genSeed = RandomDataGenerator.newRandom(seed);
		
		User userLogged = (auth==null) ? null : userService.getUser(auth);
		
		DocumentInput inputFormat = inputFormats.stream().filter(i->DocumentFormat.XLS.equals(i.getFormat())).findFirst().orElse(null);
		if (inputFormat!=null) {
			DocumentFormat format = inputFormat.getFormat();
			
			final boolean has_custom_generator = (archetype.isPresent()) && archetype.get().hasCustomGenerator(template, format);

			for (int i=0; i<limitDocs; i++) {
				String subDir = fileSystemStorageService.getSubDir();
				final Path location = fileSystemStorageService.getLocation(subDir);
				
				final String fileId = UUID.randomUUID().toString();

				final OffsetDateTime timestamp = DateTimeUtils.now();

				final Path destinationFile = location.resolve(Paths.get(fileId)).normalize().toAbsolutePath();
				
				final long doc_seed = genSeed.nextLong();
				
				final List<DocumentField> taxPayerIdFields = template.getFieldsOfType(FieldMapping.TAXPAYER_ID);
				final int num_digits_for_taxpayer_id = (taxPayerIdFields.isEmpty()) ? 10 : Math.min(20, Math.max(1, Optional.ofNullable(taxPayerIdFields.iterator().next().getMaxLength()).orElse(10)));

				final Integer partition = (limitDocs>1) ? i : null;
				
				final int doc_index = i;

	        	Callable<Object> procedure = ()->{

					final CustomDataGenerator customGen = (has_custom_generator) ? archetype.get().getCustomGenerator(template, format, doc_seed, fixedLimitRecords) : null;

					final long limit_records = (fixedLimitRecords<0 && customGen!=null) ? Long.MAX_VALUE // the actual termination will be decided by the custom generator
							: (fixedLimitRecords<0 && customGen==null) ? 10_000 
							: fixedLimitRecords;

	    			final FileGenerator gen = FileGenerators.getFileGenerator(format);
	    			gen.setDocumentTemplate(template);
	    			gen.setDocumentInputSpec(inputFormat);
	    			gen.setDomainTableRepository(domainTableRepository::findByNameAndVersion);
					gen.setRandomSeed(doc_seed);

					final String originalFilename;
					
					String taxpayerId = null;
					
					try {
						if (customGen!=null) {
							customGen.setDomainTableRepository(domainTableRepository::findByNameAndVersion);
							if (year!=0)
								customGen.setTaxYear(year);
							customGen.setOverallSeed(seed, limitDocs, doc_index);
							customGen.start();
							taxpayerId = customGen.getTaxpayerId();
							gen.setFixedYear(customGen.getTaxYear());
						}
						else {
							if (year!=0)
								gen.setFixedYear(year);
						}
						if (taxpayerId==null || taxpayerId.trim().length()==0) {
							RandomDataGenerator randomDataGenerator = new RandomDataGenerator(doc_seed);
							taxpayerId = randomDataGenerator.nextRandomNumberFixedLength(num_digits_for_taxpayer_id).toString();
						}
						gen.setFixedTaxpayerId(taxpayerId);
						gen.setPath(destinationFile);
						gen.start();
						
						if (customGen!=null) {
							originalFilename = Optional.ofNullable(customGen.getFileName()).orElseGet(gen::getOriginalFileName);
						}
						else {
							originalFilename = gen.getOriginalFileName();
						}
						
						for (long j=0; j<limit_records; j++) {
							if (customGen!=null) {
								Map<String,Object> record = customGen.nextRecord();
								if (record==null)
									break;
								gen.addRecord(record);
							}
							else {
								gen.addRandomRecord();
							}
						}
					}
					finally {
						if (customGen!=null) {
							customGen.close();
						}
						gen.close();
					}
					
					if (taxpayerId!=null && taxpayerId.trim().length()>0) {
						Optional<Taxpayer> txp = taxPayerRepository.findByTaxPayerId(taxpayerId);
						if (!txp.isPresent()) {
							// Creates a new taxpayer with random name
							RandomDataGenerator randomDataGenerator = new RandomDataGenerator(doc_seed);
							String name = randomDataGenerator.nextPersonName();
							Taxpayer newTxp = new Taxpayer();
							newTxp.setName(name);
							newTxp.setTaxPayerId(taxpayerId);
							
							// suppose Qualifier1 refers to Economic Sector
							newTxp.setQualifier1(SampleEconomicSectors.sample(randomDataGenerator.getRandomGenerator()).getName());

							// suppose Qualifier2 refers to Legal Entity
							newTxp.setQualifier2(SampleLegalEntities.sample(randomDataGenerator.getRandomGenerator()).getName());

							// suppose Qualifier3 refers to County
							newTxp.setQualifier3(SampleCounty.sample(randomDataGenerator.getRandomGenerator()).getName());

							// suppose Qualifier4 refers to Size of Company
							newTxp.setQualifier4(SampleCompanySizes.sample(randomDataGenerator.getRandomGenerator()).getName());

							// suppose Qualifier5 refers to Tax Regime
							newTxp.setQualifier5(SampleTaxRegimes.sample(randomDataGenerator.getRandomGenerator()).getName());

							taxPayerRepository.saveWithTimestamp(newTxp);
						}
					}
				
					String fileHash = Files.asByteSource(destinationFile.toFile()).hash(Hashing.sha256()).toString();
					
					// Keep this information in history of all uploads
					DocumentUploaded regUpload = new DocumentUploaded();
					regUpload.setTemplateName(template.getName());
					regUpload.setTemplateVersion(template.getVersion());
					regUpload.setInputName(inputFormat.getInputName());
					regUpload.setFileId(fileId);
					regUpload.setFilename(originalFilename);
					regUpload.setSubDir(subDir);
					regUpload.setTimestamp(timestamp);
					regUpload.setIpAddress(remoteIpAddr);
					regUpload.setHash(fileHash);
					regUpload.setSituation(DocumentSituation.RECEIVED);
					if (auth != null) {
						regUpload.setUser(String.valueOf(auth.getName()));
					}
					if (userLogged != null) {
						regUpload.setUserLogin(userLogged.getLogin());
					}
					DocumentUploaded savedInfo = documentUploadedRepository.saveWithTimestamp(regUpload);
	
					DocumentSituationHistory situationHistory = new DocumentSituationHistory();
					situationHistory.setDocumentId(savedInfo.getId());
					situationHistory.setDocumentFilename(savedInfo.getFilename());
					situationHistory.setTemplateName(savedInfo.getTemplateName());
					situationHistory.setSituation(DocumentSituation.RECEIVED);
					situationHistory.setTimestamp(timestamp);
	
					documentSituationHistoryRepository.saveWithTimestamp(situationHistory);
					
					// Generates an event at KAFKA in order to start the validation phase over this file
	
					FileUploadedEvent event = new FileUploadedEvent();
					event.setFileId(savedInfo.getId());
					fileUploadedProducer.fileUploaded(event, partition);
					
					return null;
	        	};
	        	
	        	if (executor==null)
	        		procedure.call();
	        	else
	        		executor.submit(procedure);

			} // LOOP over number of documents
			
			if (executor!=null) {
		        executor.shutdown();
		        try {
					boolean ok = executor.awaitTermination(1, TimeUnit.HOURS);
					if (!ok)
						log.log(Level.WARNING, () -> "Too much time waiting for termination of generation of "+limitDocs+" documents of template "+template.getName());
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}

			report.append("Created ").append(limitDocs).append(" documents with random data of format ").append(format.name()).append(" for template ").append(template.getName()).append(" version ").append(template.getVersion()).append("\n");
			return;
		}
		
		report.append("No implementation for random data generator of format ").append(inputFormats.get(0).getFormat().name()).append("\n");
	}
	
	/**
	 * Updates index patterns at KIBANA
	 * @throws IOException 
	 */
	public static Object updateIndexPattern(AdminService service, CommandLine cmdLine) throws IOException {

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
				catch (Exception ex) {
					log.log(Level.SEVERE, ex, () -> "Error while updating index patterns at Kibana space "+space.getId());
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
	
	private static void updateIndexPattern(AdminService service, String spaceId, String indexIdOrName, LongAdder countUpdates, LongAdder countErrors) {
		if (indexIdOrName==null || indexIdOrName.trim().length()==0) {
			
			// Updates all index patterns
			List<ESUtils.KibanaSavedObject> indexPatterns = ESUtils.getKibanaIndexPatterns(service.env, service.restTemplate, spaceId);
			if (indexPatterns!=null && !indexPatterns.isEmpty()) {				
				final String[] indexPatternsIds = indexPatterns.stream().map(ESUtils.KibanaSavedObject::getId).toArray(String[]::new);
				
				for (String patternId: indexPatternsIds) {
					updateIndexPatternInternal(service, spaceId, patternId, countUpdates, countErrors);
				}
			}
			
		}
		else {
			
			// Updates one specific index pattern
			
			boolean isUuid;
			try{
			    UUID.fromString(indexIdOrName);
			    isUuid = true;
			} catch (IllegalArgumentException exception){
				isUuid = false;
			}
			
			if (isUuid) {
			
				updateIndexPatternInternal(service, spaceId, indexIdOrName, countUpdates, countErrors);
				
			}
			else {
				
				List<ESUtils.KibanaSavedObject> indexPatterns = ESUtils.getKibanaIndexPatterns(service.env, service.restTemplate, spaceId);
				if (indexPatterns!=null && !indexPatterns.isEmpty()) {
					
					ESUtils.KibanaSavedObject indexPattern =
					indexPatterns.stream().filter(i->String.CASE_INSENSITIVE_ORDER.compare(i.getTitle(), indexIdOrName)==0).findAny().orElse(null);
					
					if (indexPattern!=null) {
						
						updateIndexPatternInternal(service, spaceId, indexPattern.getId(), countUpdates, countErrors);

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
		catch (Exception ex) {
			countErrors.increment();
			if (countErrors.longValue()==1) {
				log.log(Level.SEVERE, ex, () -> "Error while updating index pattern "+patternId+" of space "+spaceId);
			}
		}
	}
	
	/**
	 * Operations on KAFKA
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	public static Object kafka(AdminService service, CommandLine cmdLine) throws InterruptedException, ExecutionException {
		
		Option[] options = cmdLine.getOptions();
		if (options==null || options.length==0) {		
			return getHelp(AdminOperations.KAFKA);
		}

		StringBuilder report = new StringBuilder();
		
		if (cmdLine.hasOption("m")) {
			// Show KAFKA metrics
			try (AdminClient kafkaAdminClient = service.sysInfoService.getKafkaAdminClient();) {
				
				Map<String,String> metrics =
				kafkaAdminClient.metrics().entrySet().stream().collect(
					Collectors.toMap(
						e->e.getKey().group()+"."+e.getKey().name(), 
						e->ResourceMonitorService.getKafkaMetricDesc(e.getValue()),
						(a,b)->a,
						()->new TreeMap<>(String.CASE_INSENSITIVE_ORDER)));
				for (Map.Entry<String, String> entry: metrics.entrySet()) {
					report.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
				}
				
			}
		}
		
		if (cmdLine.hasOption("c")) {
			// Show information about KAFKA consumers
			try (AdminClient kafkaAdminClient = service.sysInfoService.getKafkaAdminClient();) {
			
				report.append(String.format("%-10s\t%-20s\t%s\t%s\t%s%n","group","topic","part","offset","metadata"));
				for (ConsumerGroupListing l:kafkaAdminClient.listConsumerGroups().all().get()) {
					String groupId = l.groupId();
					for (Map.Entry<TopicPartition,OffsetAndMetadata> entry:kafkaAdminClient.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata().get().entrySet()) {
						String topic = entry.getKey().topic();
						int part = entry.getKey().partition();
						long offset = entry.getValue().offset();
						report.append(String.format("%-10s\t%-20s\t%d\t%d\t%s%n", groupId, topic, part, offset, entry.getValue().metadata()));
					}
				}

			}
		}

		if (cmdLine.hasOption("t")) {
			// Show information about KAFKA topics
			try (AdminClient kafkaAdminClient = service.sysInfoService.getKafkaAdminClient();) {
			
				report.append(String.format("%-20s\t%s\t%s\t%s%n","topic","part","offset","timestamp"));
				ListTopicsResult topicsInfo = kafkaAdminClient.listTopics();
				for (Map.Entry<String,TopicDescription> tp_entry:kafkaAdminClient.describeTopics(topicsInfo.names().get()).all().get().entrySet()) {
					String topic = tp_entry.getKey();
					Map<TopicPartition,OffsetSpec> requestInfo = 
						tp_entry.getValue().partitions().stream()
						.map(tpInfo->new TopicPartition(topic,tpInfo.partition()))
						.collect(Collectors.toMap(Function.identity(), 
								e->OffsetSpec.latest()));
					Map<TopicPartition,ListOffsetsResultInfo> offsets = kafkaAdminClient.listOffsets(requestInfo).all().get();
					long total = 0;
					for (TopicPartitionInfo tp_info: tp_entry.getValue().partitions()) {
						int part = tp_info.partition();
						ListOffsetsResultInfo offsetInfo = offsets.get(new TopicPartition(topic,tp_info.partition()));
						long offset = (offsetInfo==null) ? 0 : offsetInfo.offset();
						long timestamp = (offsetInfo==null) ? 0 : offsetInfo.timestamp();
						if (timestamp>0)
							report.append(String.format("%-20s\t%d\t%d\t%s%n",topic, part, offset, ParserUtils.formatTimestampWithMS(new Date(timestamp))));
						else
							report.append(String.format("%-20s\t%d\t%d%n",topic, part, offset));
						total += offset;
						
					} // LOOP over each partition of a topic
					report.append(String.format("%-20s\t%s\t%d%n",topic, "total", total));
					report.append("\n");
				} // LOOP over each topic

			}
		}

		if (cmdLine.hasOption("d")) {
			// Delete KAFKA messages
			try (AdminClient kafkaAdminClient = service.sysInfoService.getKafkaAdminClient();) {
				Map<TopicPartition, RecordsToDelete> recordsToDelete = new HashMap<>();
				String arg = cmdLine.getOptionValue("d").trim();
				boolean allTopics = "all".equalsIgnoreCase(arg);
				if (allTopics) {
					ListTopicsResult topicsInfo = kafkaAdminClient.listTopics();
					for (Map.Entry<String,TopicDescription> tp_entry:kafkaAdminClient.describeTopics(topicsInfo.names().get()).all().get().entrySet()) {
						String topic = tp_entry.getKey();
						Map<TopicPartition,OffsetSpec> requestInfo = 
							tp_entry.getValue().partitions().stream()
							.map(tpInfo->new TopicPartition(topic,tpInfo.partition()))
							.collect(Collectors.toMap(Function.identity(), 
									e->OffsetSpec.latest()));
						Map<TopicPartition,ListOffsetsResultInfo> offsets = kafkaAdminClient.listOffsets(requestInfo).all().get();
						for (TopicPartitionInfo tp_info: tp_entry.getValue().partitions()) {
							int part = tp_info.partition();
							ListOffsetsResultInfo offsetInfo = offsets.get(new TopicPartition(topic,tp_info.partition()));
							long offset = (offsetInfo==null) ? -1 : offsetInfo.offset();
							if (offset<=0)
								continue;
							report.append("Deleting ").append(offset).append(" records from topic ").append(topic).append(" partition ").append(part).append("\n");
							recordsToDelete.put(new TopicPartition(topic, part), RecordsToDelete.beforeOffset(offset));
						}
					}
				}
				else {
					String[] parts = arg.split(":");
					String topic = parts[0];
					for (Map.Entry<String,TopicDescription> tp_entry:kafkaAdminClient.describeTopics(Collections.singleton(topic)).all().get().entrySet()) {
						Map<TopicPartition,OffsetSpec> requestInfo;
						if (parts.length>1) {
							int part = Integer.parseInt(parts[1]);
							requestInfo = Collections.singletonMap(new TopicPartition(topic,part), OffsetSpec.latest());
						}
						else {
							requestInfo = 
								tp_entry.getValue().partitions().stream()
								.map(tpInfo->new TopicPartition(topic,tpInfo.partition()))
								.collect(Collectors.toMap(Function.identity(), 
										e->OffsetSpec.latest()));
						}
						if (parts.length>2) {
							long offset = Long.parseLong(parts[2]);
							for (Map.Entry<TopicPartition,OffsetSpec> tp_info: requestInfo.entrySet()) {
								int part = tp_info.getKey().partition();
								report.append("Deleting ").append(offset).append(" records from topic ").append(topic).append(" partition ").append(part).append("\n");
								recordsToDelete.put(new TopicPartition(topic, part), RecordsToDelete.beforeOffset(offset));
							}
						}
						else {
							Map<TopicPartition,ListOffsetsResultInfo> offsets = kafkaAdminClient.listOffsets(requestInfo).all().get();
							for (TopicPartitionInfo tp_info: tp_entry.getValue().partitions()) {
								int part = tp_info.partition();
								ListOffsetsResultInfo offsetInfo = offsets.get(new TopicPartition(topic,tp_info.partition()));
								long offset = (offsetInfo==null) ? -1 : offsetInfo.offset();
								if (offset<=0)
									continue;
								report.append("Deleting ").append(offset).append(" records from topic ").append(topic).append(" partition ").append(part).append("\n");
								recordsToDelete.put(new TopicPartition(topic, part), RecordsToDelete.beforeOffset(offset));
							}
						}
					}
				}
                DeleteRecordsResult result = kafkaAdminClient.deleteRecords(recordsToDelete, new DeleteRecordsOptions());
                result.all().get();
                report.append("Records deleted!");
			}
		}
		
		return report.toString();
	}
	
	/**
	 * Deletes information from the database
	 */
	public static Object delete(AdminService service, CommandLine cmdLine) throws Exception {
		
		Option[] options = cmdLine.getOptions();
		if (options==null || options.length==0) {		
			return getHelp(AdminOperations.DELETE);
		}

		StringBuilder report = new StringBuilder();

		if (cmdLine.hasOption("p") || cmdLine.hasOption("a")) {
			// Deletes all published (denormalized) views
			int deletedIndices = 0;
			long deletedDocuments = 0;
			try {
				GetIndexRequest request = new GetIndexRequest(IndexNamesUtils.PUBLISHED_DATA_INDEX_PREFIX+"*");
				GetIndexResponse response = service.elasticsearchClient.indices().get(request, RequestOptions.DEFAULT);
				String[] indices = response.getIndices();
				for (String indexName: indices) {
					try {
						deletedDocuments = ESUtils.countDocs(service.elasticsearchClient, indexName);
						ESUtils.deleteIndex(service.elasticsearchClient, indexName);
						deletedIndices++;
					}
					catch (Exception ex) {
						log.log(Level.INFO, ex.getMessage(), ex);
					}
				}
			}
			catch (Exception ex) {
				if (!CommonErrors.isErrorNoIndexFound(ex) && !CommonErrors.isErrorNoMappingFoundForColumn(ex)) {
					throw ex;
				}
			}
			report.append("Deleted ").append(deletedIndices).append(" indices containing ").append(deletedDocuments).append(" published (denormalized) data.\n");
		}

		if (cmdLine.hasOption("v") || cmdLine.hasOption("a")) {
			// Deletes all validated records for all document templates
			int deletedIndices = 0;
			long deletedDocuments = 0;
			for (DocumentTemplate template: service.templateRepository.findAll(PageRequest.of(0, 10_000))) {
				String indexName = IndexNamesUtils.formatIndexNameForValidatedData(template);
				try {
					deletedDocuments = ESUtils.countDocs(service.elasticsearchClient, indexName);
					ESUtils.deleteIndex(service.elasticsearchClient, indexName);
					deletedIndices++;
				}
				catch (Exception ex) {
					log.log(Level.INFO, ex.getMessage(), ex);
				}
			}
			report.append("Deleted ").append(deletedIndices).append(" indices containing ").append(deletedDocuments).append(" validated data of uploaded files.\n");
		}

		if (cmdLine.hasOption("t") || cmdLine.hasOption("a")) {
			
			String argument = (cmdLine.hasOption("t")) ? cmdLine.getOptionValue("t") : null;
			
			if (cmdLine.hasOption("a") || argument==null || "all".equalsIgnoreCase(argument)) {
				// Deletes all templates
				long countTemplates = service.templateRepository.count();
				service.templateRepository.deleteAll();
				report.append("Deleted ").append(countTemplates).append(" templates from database.\n");
			}
			else if (argument.contains(":")) {
				// The argument includes a version
				int sep = argument.indexOf(':');
				String templateName = argument.substring(0, sep).trim();
				String version = argument.substring(sep+1).trim();
				Optional<DocumentTemplate> template = service.templateRepository.findByNameIgnoreCaseAndVersion(templateName, version);
				if (template.isPresent()) {
					service.templateRepository.delete(template.get());
					report.append("Deleted template with name '").append(templateName).append("' and version '").append(version).append("'\n");
				}
				else {
					report.append("ERROR: There is no template with name '").append(templateName).append("' and version '").append(version).append("'\n");
				}
			}
			else {
				// The argument does not include a version, so it should delete all versions of the same template
				List<DocumentTemplate> templates = service.templateRepository.findByNameIgnoreCase(argument);
				if (templates.isEmpty()) {
					report.append("ERROR: There is no template with name '").append(argument).append("'\n");
				}
				else {
					service.templateRepository.deleteAll(templates);
					if (templates.size()>1)
						report.append("Deleted ").append(templates.size()).append(" versions of the template with name '").append(argument).append("'\n");
					else
						report.append("Deleted one version of the template with name '").append(argument).append("'\n");
				}
			}
		}

		if (cmdLine.hasOption("dt") || cmdLine.hasOption("a")) {
			
			String argument = (cmdLine.hasOption("dt")) ? cmdLine.getOptionValue("dt") : null;
			
			if (cmdLine.hasOption("a") || argument==null || "all".equalsIgnoreCase(argument)) {
				// Deletes all domain tables. Recreates domain tables automatically.
				long countDomainTables = service.domainTableRepository.count();
				service.domainTableRepository.deleteAll();
				report.append("Deleted ").append(countDomainTables).append(" domain tables from database.\n");
				
				int countDomainTablesCreated = service.domainTableService.assertDomainTablesForAllArchetypes(/*overwrite*/true);
				report.append("Created ").append(countDomainTablesCreated).append(" built-in domain tables from template's archetypes.\n");
			}
			else if (argument.contains(":")) {
				// The argument includes a version
				int sep = argument.indexOf(':');
				String tableName = argument.substring(0, sep).trim();
				String version = argument.substring(sep+1).trim();
				Optional<DomainTable> table = service.domainTableRepository.findByNameIgnoreCaseAndVersion(tableName, version);
				if (table.isPresent()) {
					service.domainTableRepository.delete(table.get());
					report.append("Deleted domain table with name '").append(tableName).append("' and version '").append(version).append("'\n");					
				}
				else {
					report.append("ERROR: There is no domain table with name '").append(tableName).append("' and version '").append(version).append("'\n");
				}
			}
			else {
				// The argument does not include a version, so it should delete all versions of the same domain table
				List<DomainTable> tables = service.domainTableRepository.findByNameIgnoreCase(argument);
				if (tables.isEmpty()) {
					report.append("ERROR: There is no domain table with name '").append(argument).append("'\n");
				}
				else {
					service.domainTableRepository.deleteAll(tables);
					if (tables.size()>1)
						report.append("Deleted ").append(tables.size()).append(" versions of the domain table with name '").append(argument).append("'\n");
					else
						report.append("Deleted one version of the domain table with name '").append(argument).append("'\n");
				}
			}
		}

		if (cmdLine.hasOption("txp") || cmdLine.hasOption("a")) {
			// Deletes all taxpayers. Recreates domain tables automatically.
			long countTaxpayers = service.taxPayerRepository.count();
			service.taxPayerRepository.deleteAll();
			report.append("Deleted ").append(countTaxpayers).append(" taxpayers from database.\n");			
		}

		if (cmdLine.hasOption("u") || cmdLine.hasOption("a")) {
			// Deletes all upload records
			long countUploads = service.documentUploadedRepository.count();
			service.documentUploadedRepository.deleteAll();
			service.documentSituationHistoryRepository.deleteAll();
			service.documentValidationErrorMessageRepository.deleteAll();
			report.append("Deleted ").append(countUploads).append(" upload records from database.\n");
			int deletedFiles = service.fileSystemStorageService.deleteAll();
			report.append("Deleted ").append(deletedFiles).append(" uploaded files from file storage.\n");
		}
		
		if (cmdLine.hasOption("s") || cmdLine.hasOption("a")) {
			// Deletes history of SYNC operations
			long countHistory = service.syncCommitHistoryRepository.count();
			service.syncCommitHistoryRepository.deleteAll();
			report.append("Deleted ").append(countHistory).append(" records of SYNC history.\n");
			
			long countMilestones = service.syncCommitMilestoneRepository.count();
			service.syncCommitMilestoneRepository.deleteAll();
			report.append("Deleted ").append(countMilestones).append(" records of SYNC last state (milestones).\n");			
		}

		if (cmdLine.hasOption("kp")) {
			// Deletes all Index Patterns from all Kibana spaces
			List<org.idb.cacao.web.utils.ESUtils.KibanaSpace> spaces = ESUtils.getKibanaSpaces(service.env, service.restTemplate);
			if (!spaces.isEmpty()) {
				boolean deletedAny = false;
				for (org.idb.cacao.web.utils.ESUtils.KibanaSpace space: spaces) {					
					List<org.idb.cacao.web.utils.ESUtils.KibanaSavedObject> indexPatterns = ESUtils.getKibanaIndexPatterns(service.env, service.restTemplate, space.getId());
					int countDeleted = 0;
					if (indexPatterns!=null && !indexPatterns.isEmpty()) {
						for (org.idb.cacao.web.utils.ESUtils.KibanaSavedObject indexPattern: indexPatterns) {
							if (indexPattern.getTitle().startsWith("cacao_")) {
								report.append("Deleting ").append(indexPattern.getTitle()).append(" from space ").append(space.getId()).append("\n");
								ESUtils.deleteKibanaSavedObject(service.env, service.restTemplate, space.getId(), "index-pattern", indexPattern.getId());
								countDeleted++;
								deletedAny = true;
							}
						}
					}
					if (countDeleted>0) {
						report.append("Deleted ").append(countDeleted).append(" index patterns from space ").append(space.getId()).append("\n");
					}
				}
				if (deletedAny) {
					service.kibanaSpacesService.clearChecked();
				}
			}
		}

		return report.toString();
	}
	
	/**
	 * Copy one or more index patterns from one Space to another
	 */
	public static Object copyIndexPattern(AdminService service, CommandLine cmdLine) throws Exception {
		
		if (!cmdLine.hasOption("s"))
			throw new GeneralException("Missing the ID of the SOURCE SPACE in command line option 's'!");
		if (!cmdLine.hasOption("t"))
			throw new GeneralException("Missing the ID of the TARGET SPACE in command line option 't'!");
		
		String indexIdOrName = cmdLine.getOptionValue("i", null);
		String sourceSpaceId = cmdLine.getOptionValue("s");
		String targetSpaceId = cmdLine.getOptionValue("t");
		
		if (indexIdOrName==null || indexIdOrName.trim().length()==0) {
			
			// Copy all index patterns
			List<ESUtils.KibanaSavedObject> indexPatterns = ESUtils.getKibanaIndexPatterns(service.env, service.restTemplate, sourceSpaceId);
			if (indexPatterns==null || indexPatterns.isEmpty()) {
				
				return "No index patterns was found at space \""+sourceSpaceId+"\"";
				
			}
			else {
				
				final String[] indexPatternsIds = indexPatterns.stream().map(ESUtils.KibanaSavedObject::getId).toArray(String[]::new);
				boolean success = ESUtils.copyKibanaSavedObjects(service.env, service.restTemplate, sourceSpaceId, targetSpaceId, "index-pattern", indexPatternsIds);
				if (success)
					return SUCCESS;
				else
					return FAILURE;
				
			}
			
		}
		else {
			
			// Copy one specific index pattern
			
			boolean isUuid;
			try{
			    UUID.fromString(indexIdOrName);
			    isUuid = true;
			} catch (IllegalArgumentException exception){
				isUuid = false;
			}
			
			if (isUuid) {
			
				boolean success = ESUtils.copyKibanaSavedObjects(service.env, service.restTemplate, sourceSpaceId, targetSpaceId, "index-pattern", new String[] { indexIdOrName });
				if (success)
					return SUCCESS;
				else
					return FAILURE;
				
			}
			else {
				
				List<ESUtils.KibanaSavedObject> indexPatterns = ESUtils.getKibanaIndexPatterns(service.env, service.restTemplate, sourceSpaceId);
				if (indexPatterns==null || indexPatterns.isEmpty()) {
					
					return "No index patterns was found at space \""+sourceSpaceId+"\"";
					
				}
				else {
					
					ESUtils.KibanaSavedObject indexPattern =
					indexPatterns.stream().filter(i->String.CASE_INSENSITIVE_ORDER.compare(i.getTitle(), indexIdOrName)==0).findAny().orElse(null);
					
					if (indexPattern==null) {
						
						return "No index pattern with name \""+indexIdOrName+"\" was found at space \""+sourceSpaceId+"\"";
						
					}
					else {
						
						boolean success = ESUtils.copyKibanaSavedObjects(service.env, service.restTemplate, sourceSpaceId, targetSpaceId, "index-pattern", new String[] { indexPattern.getId() });
						if (success)
							return SUCCESS;
						else
							return FAILURE;

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
			throw new GeneralException("Missing the ID of the SOURCE SPACE in command line option 's'!");
		if (!cmdLine.hasOption("t"))
			throw new GeneralException("Missing the ID of the TARGET SPACE in command line option 't'!");

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

	/**
	 * Redo some operation
	 */
	public static Object redo(AdminService service, CommandLine cmdLine) throws Exception {

		Option[] options = cmdLine.getOptions();
		if (options==null || options.length==0) {		
			return getHelp(AdminOperations.REDO);
		}

		StringBuilder report = new StringBuilder();
		
		if (cmdLine.hasOption("val")) {
			// Redo validation phase
			Stream<DocumentUploaded> stream;
			String valOpt = cmdLine.getOptionValue("val");
			if (valOpt.equalsIgnoreCase("all")) {
				stream = ScrollUtils.findAll(service.documentUploadedRepository, service.elasticsearchClient, /*durationInMinutes*/5);
			}
			else if (valOpt.equalsIgnoreCase("unprocessed")
					|| valOpt.equalsIgnoreCase("processed")
					|| valOpt.equalsIgnoreCase("pending")
					|| valOpt.equalsIgnoreCase("received")
					|| valOpt.equalsIgnoreCase("invalid")
					|| valOpt.equalsIgnoreCase("valid")) {
				stream = ScrollUtils.findWithScroll(
					/*entity*/DocumentUploaded.class, 
					/*indexName*/"cacao_docs_uploaded", 
					/*clientForScrollSearch*/service.elasticsearchClient, 
					/*customizeSearch*/e->{
						BoolQueryBuilder query = QueryBuilders.boolQuery();
						if (valOpt.equalsIgnoreCase("unprocessed"))
							query.mustNot(new TermQueryBuilder("situation.keyword", "PROCESSED"));
						else if (valOpt.equalsIgnoreCase("processed"))
							query.must(new TermQueryBuilder("situation.keyword", "PROCESSED"));
						else if (valOpt.equalsIgnoreCase("pending"))
							query.must(new TermQueryBuilder("situation.keyword", "PENDING"));
						else if (valOpt.equalsIgnoreCase("invalid"))
							query.must(new TermQueryBuilder("situation.keyword", "INVALID"));
						else if (valOpt.equalsIgnoreCase("valid"))
							query.must(new TermQueryBuilder("situation.keyword", "VALID"));
						e.query(query);
					},
					/*durationInMinutes*/5);
			}
			else {
				report.append("Invalid argument for '--validation' option: "+valOpt);
				stream = null;
			}
			if (stream!=null) {
				LongAdder countDocs = new LongAdder();
				try {
					AtomicInteger partition = new AtomicInteger(0);
					stream.forEach(doc->{
						// Generates an event at KAFKA in order to start the validation phase over this file
						
						FileUploadedEvent event = new FileUploadedEvent();
						event.setFileId(doc.getId());
						service.fileUploadedProducer.fileUploaded(event, partition.getAndIncrement());
						countDocs.increment();

					});
				}
				finally {
					stream.close();
				}
				
				report.append("Submitted ").append(countDocs.longValue()).append(" events for validation of documents according to the criteria '").append(valOpt).append("'\n");
			}
		}

		return report.toString();
	}
}
