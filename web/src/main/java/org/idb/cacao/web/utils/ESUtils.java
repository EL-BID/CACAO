/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.utils;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.client.indices.CloseIndexRequest;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.client.security.DisableUserRequest;
import org.elasticsearch.client.security.EnableUserRequest;
import org.elasticsearch.client.security.GetRolesRequest;
import org.elasticsearch.client.security.GetRolesResponse;
import org.elasticsearch.client.security.GetUsersRequest;
import org.elasticsearch.client.security.GetUsersResponse;
import org.elasticsearch.client.security.PutRoleRequest;
import org.elasticsearch.client.security.PutRoleResponse;
import org.elasticsearch.client.security.PutUserRequest;
import org.elasticsearch.client.security.PutUserResponse;
import org.elasticsearch.client.security.RefreshPolicy;
import org.elasticsearch.client.security.user.User;
import org.elasticsearch.client.security.user.privileges.ApplicationResourcePrivileges;
import org.elasticsearch.client.security.user.privileges.IndicesPrivileges;
import org.elasticsearch.client.security.user.privileges.Role;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.IndexSettings;
import org.idb.cacao.api.utils.ElasticClientFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility methods for Elastic Search
 * 
 * @author Gustavo Figueiredo
 *
 */
public class ESUtils {
	
	/**
	 * The ignore_malformed parameter, if set to true, allows the exception to be ignored. The malformed field is not indexed, but other fields in the document are processed normally.
	 * @see https://www.elastic.co/guide/en/elasticsearch/reference/current/ignore-malformed.html
	 */
	public static final String SETTING_IGNORE_MALFORMED = "index.mapping.ignore_malformed";
	
	/**
	 * Property for making the index 'read only' (required for 'clone' operations).
	 * @see https://www.elastic.co/guide/en/elasticsearch/reference/master/index-modules-blocks.html#index-block-settings
	 */
	public static final String SETTING_READ_ONLY = "index.blocks.read_only";

	/**
	 * Whether or not to fsync and commit the translog after every index, delete, update, or bulk request.
	 * @see https://www.elastic.co/guide/en/elasticsearch/reference/current/index-modules-translog.html 
	 */
	public static final String SETTING_INDEX_TRANSLOG_DURABILITY = "index.translog.durability";
	
	/**
	 * The translog stores all operations that are not yet safely persisted in Lucene (i.e., are not part of 
	 * a Lucene commit point). Although these operations are available for reads, they will need to be reindexed 
	 * if the shard was to shutdown and has to be recovered. This settings controls the maximum total size of 
	 * these operations, to prevent recoveries from taking too long. Once the maximum size has been reached a 
	 * flush will happen, generating a new Lucene commit point.
	 * @see https://www.elastic.co/guide/en/elasticsearch/reference/current/index-modules-translog.html
	 */
	public static final String SETTING_INDEX_TRANSLOG_FLUSH_THRESHOLD_SIZE = "index.translog.flush_threshold_size";

	/**
	 * The number of replicas each primary shard has.
	 */
	public static final String SETTING_INDEX_NUMBER_OF_REPLICAS = "index.number_of_replicas";
	
	/**
	 * How often to perform a refresh operation, which makes recent changes to the index visible to search. 
	 * Defaults to 1s. Can be set to -1 to disable refresh. If this setting is not explicitly set, shards that 
	 * haven’t seen search traffic for at least index.search.idle.after seconds will not receive background refreshes 
	 * until they receive a search request. 
	 * Searches that hit an idle shard where a refresh is pending will wait for the next background refresh (within 1s). 
	 * This behavior aims to automatically optimize bulk indexing in the default case when no searches are performed. 
	 * In order to opt out of this behavior an explicit value of 1s should set as the refresh interval.
	 */
	public static final String SETTING_INDEX_REFRESH_INTERVAL = "index.refresh_interval";

	/**
	 * Change index-scoped boolean setting
	 * @param elasticsearchClient Object for RESTful communication with ElasticSearch
	 * @param index_name Index name
	 * @param setting_name Name of the setting to change
	 * @param setting_value Boolean value of the setting
	 * @param closeAndReopenIndex Tells if the index should be closed before changing settings and reopened afterwards
	 */
	public static void changeBooleanIndexSetting(RestHighLevelClient elasticsearchClient, String index_name, String setting_name, boolean setting_value, boolean closeAndReopenIndex) throws IOException {
		changeIndexSettings(elasticsearchClient,
				index_name,
				Settings.builder()
					.put(setting_name, setting_value)
					.build(),
				closeAndReopenIndex);
	}
	
	/**
	 * Change index settings for faster Bulk Loads
	 */
	public static void changeIndexSettingsForFasterBulkLoad(RestHighLevelClient elasticsearchClient, String index_name) throws IOException {
		// fsync and commit in the background every sync_interval. In the event of a failure, all acknowledged writes since the last automatic commit will be discarded.
		changeIndexSettings(elasticsearchClient,
				index_name,
				Settings.builder()
					.put(SETTING_INDEX_TRANSLOG_DURABILITY, "async")
					.put(SETTING_INDEX_TRANSLOG_FLUSH_THRESHOLD_SIZE, "2gb")
					.put(SETTING_INDEX_REFRESH_INTERVAL,"-1")
					.put(SETTING_INDEX_NUMBER_OF_REPLICAS, "0")
					.build(),
				/*closeAndReopenIndex*/false);		
	}
	
	/**
	 * Change index settings to default values (undo 'changeIndexSettingsForFasterBulkLoad')
	 */
	public static void changeIndexSettingsForDefaultBulkLoad(RestHighLevelClient elasticsearchClient, String index_name) throws IOException {
		// fsync and commit after every request. In the event of hardware failure, all acknowledged writes will already have been committed to disk.
		changeIndexSettings(elasticsearchClient,
				index_name,
				Settings.builder()
					.put(SETTING_INDEX_TRANSLOG_DURABILITY, "request")
					.put(SETTING_INDEX_TRANSLOG_FLUSH_THRESHOLD_SIZE, "512mb")
					.put(SETTING_INDEX_REFRESH_INTERVAL,(String)null)
					.put(SETTING_INDEX_NUMBER_OF_REPLICAS, "1")
					.build(),
				/*closeAndReopenIndex*/false);				
	}
	
	/**
	 * Change index-scoped settings
	 * @param elasticsearchClient Object for RESTful communication with ElasticSearch
	 * @param index_name Index name
	 * @param settings Settings to provide
	 * @param closeAndReopenIndex Tells if the index should be closed before changing settings and reopened afterwards
	 */
	public static void changeIndexSettings(RestHighLevelClient elasticsearchClient, String index_name, Settings settings, boolean closeAndReopenIndex) throws IOException {
		UpdateSettingsRequest request = new UpdateSettingsRequest(index_name);
		request.settings(settings);
		
		if (closeAndReopenIndex)
			elasticsearchClient.indices().close(new CloseIndexRequest(index_name), RequestOptions.DEFAULT);
		
		try {
			
			elasticsearchClient.indices().putSettings(request, RequestOptions.DEFAULT);
			
		}
		finally {
			
			if (closeAndReopenIndex)
				elasticsearchClient.indices().open(new OpenIndexRequest(index_name), RequestOptions.DEFAULT);
			
		}

	}	

	/**
	 * Change property-scoped settings
	 * @param elasticsearchClient Object for RESTful communication with ElasticSearch
	 * @param index_name Index name
	 * @param property_name Property name
	 * @param settings Settings to provide
	 */
	public static void changeMappingSettings(RestHighLevelClient elasticsearchClient, String index_name, String property_name, Map<String, Object> settings) throws IOException {
		PutMappingRequest request = new PutMappingRequest(index_name);
		Map<String, Object> jsonMap = new HashMap<>();
		Map<String, Object> properties = new HashMap<>();
		jsonMap.put("properties", properties);
		properties.put(property_name, settings);
		request.source(jsonMap);
		
		elasticsearchClient.indices().putMapping(request, RequestOptions.DEFAULT);
	}

	/**
	 * Deletes an existent index
	 * @param elasticsearchClient Object for RESTful communication with ElasticSearch
	 * @param index_name Index name to delete
	 */
	public static void deleteIndex(RestHighLevelClient elasticsearchClient, String index_name) throws IOException {
		DeleteIndexRequest request = new DeleteIndexRequest(index_name);
		elasticsearchClient.indices().delete(request, RequestOptions.DEFAULT);
	}

	/**
	 * Get ElasticSearch cluster health information
	 */
	public static ClusterHealthResponse getClusterStatus(RestHighLevelClient elasticsearchClient) throws IOException {
		ClusterHealthRequest request = new ClusterHealthRequest();
		request.timeout(TimeValue.timeValueSeconds(30));
		return elasticsearchClient.cluster().health(request, RequestOptions.DEFAULT);
	}

	/**
	 * Returns count of documents for a given index at ElasticSearch
	 */
	public static long countDocs(RestHighLevelClient elasticsearchClient, String index) throws IOException {
		CountRequest countRequest = new CountRequest(index);
		CountResponse countResponse = elasticsearchClient.count(countRequest, RequestOptions.DEFAULT);
		return countResponse.getCount();
	}

	/**
	 * Returns summaries about all the indices in ElasticSearch
	 */
	public static List<IndexSummary> catIndices(RestHighLevelClient elasticsearchClient) throws IOException {
		Request request = new Request("GET","/_cat/indices?v&s=index&h=health,status,index,pri,rep,docs.count,docs.deleted,store.size,pri.store.size");
		Response response = elasticsearchClient.getLowLevelClient().performRequest(request);
		List<IndexSummary> summary = new LinkedList<>();
		try (InputStream is = response.getEntity().getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(is,StandardCharsets.UTF_8))) {
			while (reader.ready()) {
				String line = reader.readLine();
				if (line==null || line.length()==0)
					continue;
				if (line.startsWith("health"))
					continue;
				String[] parts = line.split("\\s+");
				if (parts.length<9)
					continue;
				IndexSummary s = new IndexSummary();
				s.setHealth(parts[0]);
				s.setStatus(parts[1]);
				s.setIndex(parts[2]);
				s.setPri(Integer.parseInt(parts[3]));
				s.setRep(Integer.parseInt(parts[4]));
				s.setDocsCount(Long.parseLong(parts[5]));
				s.setDocsDeleted(Long.parseLong(parts[6]));
				s.setStoreSize(parts[7]);
				s.setPriStoreSize(parts[8]);
				summary.add(s);
			}
		}
		return summary;
	}

	/**
	 * Invoke a index request. In case of an error relative to type mismatch, try to fix it by changing an index parameter (index.mapping.ignore_malformed).<BR>
	 * Warning: this configuration needs to be done with the index closed. So it's possible that this operation disables the index temporarily. 
	 */
	public static void indexWithRetry(RestHighLevelClient client, IndexRequest indexRequest) throws IOException {
		try {
			client.index(indexRequest, RequestOptions.DEFAULT);
		}
		catch (Exception ex) {
			if (null!=ErrorUtils.getIllegalArgumentTypeMismatch(ex)
					|| null!=ErrorUtils.getIllegalArgumentInputString(ex)) {
				// In case of an error relative to type mismatch, lets try again after changing some of the index parameters
				changeBooleanIndexSetting(client, indexRequest.index(), SETTING_IGNORE_MALFORMED, true, /*closeAndReopenIndex*/true);
				client.index(indexRequest, RequestOptions.DEFAULT);
			}
			else {
				throw ex;
			}
		}
	}	
	
	/**
	 * Creates a new index
	 * @param elasticsearchClient Object for RESTful communication with ElasticSearch
	 * @param index_name Index name
	 * @param settings Settings for the new index
	 */
	public static void createIndex(RestHighLevelClient elasticsearchClient, String index_name, Settings settings) throws IOException {
		CreateIndexRequest request = new CreateIndexRequest(index_name);
		request.settings(settings);
		elasticsearchClient.indices().create(request, RequestOptions.DEFAULT);
	}

	/**
	 * Creates a new index initialized with some settings
	 * @param elasticsearchClient Object for RESTful communication with ElasticSearch
	 * @param index_name Index name
	 * @param ignore_malformed If set to true, allows the 'malformed' exception to be ignored.
	 */
	public static void createIndex(RestHighLevelClient elasticsearchClient, String index_name, boolean ignore_malformed) throws IOException {
		Settings settings = Settings.builder()
			.put(SETTING_IGNORE_MALFORMED, ignore_malformed)
			.build();
		createIndex(elasticsearchClient, index_name, settings);
	}
	
	/**
	 * Creates a new index initialized with some settings
	 * @param elasticsearchClient Object for RESTful communication with ElasticSearch
	 * @param index_name Index name
	 * @param ignore_malformed If set to true, allows the 'malformed' exception to be ignored.
	 * @param max_result_window Maximum value of from + size on a query. The Default maximum value of from + size on a query is 10,000.
	 */
	public static void createIndex(RestHighLevelClient elasticsearchClient, String index_name, boolean ignore_malformed, int max_result_window) throws IOException {
		Settings settings = Settings.builder()
			.put(IndexSettings.MAX_RESULT_WINDOW_SETTING.getKey(), max_result_window)
			.put(SETTING_IGNORE_MALFORMED, ignore_malformed)
			.build();
		createIndex(elasticsearchClient, index_name, settings);
	}

	/**
	 * Return the list of roles registered in ElasticSearch if security module is enabled.
	 */
	public static List<Role> getRoles(RestHighLevelClient client) throws IOException {
		GetRolesRequest request = new GetRolesRequest();
		GetRolesResponse response = client.security().getRoles(request, RequestOptions.DEFAULT);
		return response.getRoles();
	}
	
	/**
	 * Return information about the provided role
	 */
	public static Role getRole(RestHighLevelClient client, String roleName) throws IOException {
		GetRolesRequest request = new GetRolesRequest(roleName);
		try {
			GetRolesResponse response = client.security().getRoles(request, RequestOptions.DEFAULT);
			List<Role> found = response.getRoles();
			if (found!=null && !found.isEmpty())
				return found.get(0);
			else
				return null;
		} catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
			return null;
		}
	}

	/**
	 * Create a new role for a specific application in ElasticSearch if security module is enabled.
	 * @param client Object for communication with ElasticSearch
	 * @param name Name of the new role
	 * @param application Name of the application associated with this role
	 * @param privileges Privileges of this role over the application
	 * @param resources Resources to access with this role and this application
	 */
	public static Role createRoleForSingleApplication(RestHighLevelClient client, 
			String name, 
			String application, 
			Collection<String> privileges, 
			Collection<String> resources,
			Collection<String> allIndicesPrivilege) throws IOException {
		Role role = Role.builder()
				.name(name)
				.applicationResourcePrivileges(new ApplicationResourcePrivileges(application, privileges, resources))
				.indicesPrivileges(IndicesPrivileges.builder().indices("*").privileges(allIndicesPrivilege).allowRestrictedIndices(false).build())
				.build();
		PutRoleRequest request = new PutRoleRequest(role, RefreshPolicy.IMMEDIATE);
		PutRoleResponse response = client.security().putRole(request, RequestOptions.DEFAULT);
		return (response.isCreated()) ? role : null;
	}
	
	/**
	 * Returns information about the user in ElasticSearch if security module is enabled.
	 */
	public static User getUser(RestHighLevelClient client, String username) {
		Set<User> users_at_es;
		try {
			users_at_es = getUsers(client, username);
		} catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
			return null;
		} catch (IOException e) {
			return null;
		}
		if (users_at_es!=null && !users_at_es.isEmpty()) 
			return users_at_es.iterator().next();
		else
			return null;
	}
	
	/**
	 * Returns the users registered in ElasticSearch if security module is enabled.
	 * @param client Object for communication with ElasticSearch
	 * @param usernames Names of the users to search for.
	 */
	public static Set<User> getUsers(RestHighLevelClient client, String... usernames) throws IOException {
		try {
			GetUsersRequest request = new GetUsersRequest(usernames);
			GetUsersResponse response = client.security().getUsers(request, RequestOptions.DEFAULT);
			return response.getUsers();
		}
		catch (Exception ex) {
			if (ErrorUtils.isErrorNotFound(ex))
				return Collections.emptySet();
			else
				throw ex;
		}
	}
	
	/**
	 * Creates a new user at ElasticSearch if security module is enabled.
	 * @param client Object for communication with ElasticSearch
	 * @param username Username
	 * @param roles Roles to assign to the user
	 * @param password Password
	 */
	public static User createUser(RestHighLevelClient client, String username, List<String> roles, char[] password) throws IOException {
		User user = new User(username, roles);
		PutUserRequest request = PutUserRequest.withPassword(user, password, /*enabled*/true, RefreshPolicy.IMMEDIATE);
		PutUserResponse response = client.security().putUser(request, RequestOptions.DEFAULT);
		return response.isCreated() ? user : null;
	}

	/**
	 * Updates an existing user at ElasticSearch if security module is enabled.
	 * @param client Object for communication with ElasticSearch
	 * @param username Username
	 * @param roles Roles to assign to the user
	 */
	public static User updateUser(RestHighLevelClient client, String username, List<String> roles) throws IOException {
		User user = new User(username, roles);
		PutUserRequest request = PutUserRequest.updateUser(user, /*enabled*/true, RefreshPolicy.IMMEDIATE);
		PutUserResponse response = client.security().putUser(request, RequestOptions.DEFAULT);
		return response.isCreated() ? user : null;
	}

	/**
	 * Disable user at ElasticSearch if security module is enabled
	 * @param client Object for communication with ElasticSearch
	 * @param username Username
	 */
	public static boolean disableUser(RestHighLevelClient client, String username) throws IOException {
		DisableUserRequest request = new DisableUserRequest(username, RefreshPolicy.IMMEDIATE);
		return client.security().disableUser(request, RequestOptions.DEFAULT);
	}

	/**
	 * Enable user at ElasticSearch if security module is enabled
	 * @param client Object for communication with ElasticSearch
	 * @param username Username
	 */
	public static boolean enableUser(RestHighLevelClient client, String username) throws IOException {
		EnableUserRequest request = new EnableUserRequest(username, RefreshPolicy.IMMEDIATE);
		return client.security().enableUser(request, RequestOptions.DEFAULT);
	}
	
	/**
	 * Create a Kibana SPACE
	 */
	public static void createKibanaSpace(Environment env, RestTemplate restTemplate, KibanaSpace space) {
		String url = getKibanaURL(env, "/api/spaces/space");
		
		ResponseExtractor<Boolean> responseExtractor = clientHttpResponse->{
			return true;
		}; 
		try {
			restTemplate.execute(new URI(url), HttpMethod.POST, getKibanaRequestCallback(env,/*requestBody*/space), responseExtractor);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}		
	}

	/**
	 * Returns information about all Kibana SPACE's created
	 */
	public static List<KibanaSpace> getKibanaSpaces(Environment env, RestTemplate restTemplate) {
		String url = getKibanaURL(env, "/api/spaces/space");
		
		ResponseExtractor<List<KibanaSpace>> responseExtractor = clientHttpResponse->{
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			try (InputStream is = clientHttpResponse.getBody();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is,StandardCharsets.UTF_8))) {
				KibanaSpace[] result = mapper.readValue(reader, KibanaSpace[].class);
				return Arrays.asList(result);
			}
		}; 
		try {
			return restTemplate.execute(new URI(url), HttpMethod.GET, getKibanaRequestCallback(env,/*requestBody*/null), responseExtractor);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Returns indication that a Kibana SPACE exists
	 */
	public static boolean hasKibanaSpace(Environment env, RestTemplate restTemplate, String spaceId) {
		String url = getKibanaURL(env, "/api/spaces/space/"+spaceId);
		
		ResponseExtractor<Boolean> responseExtractor = clientHttpResponse->{
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			try (InputStream is = clientHttpResponse.getBody();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is,StandardCharsets.UTF_8))) {
				Map<?,?> result = mapper.readValue(reader, Map.class);
				return spaceId.equalsIgnoreCase((String)result.get("id"));
			}
		}; 
		try {
			return restTemplate.execute(new URI(url), HttpMethod.GET, getKibanaRequestCallback(env,/*requestBody*/null), responseExtractor);
		} catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
			return false;
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns information about all Kibana saved objects of some type created
	 */
	public static List<KibanaSavedObject> getKibanaSavedObjects(Environment env, RestTemplate restTemplate, String spaceId, String type) {
		String url = getKibanaURL(env, 
				(spaceId!=null && spaceId.trim().length()>0 && !"Default".equalsIgnoreCase(spaceId)) ? "/s/"+spaceId+"/api/saved_objects/_find?type="+type+"&per_page=10000" 
						: "/api/saved_objects/_find?type="+type+"&per_page=10000");
		
		ResponseExtractor<List<KibanaSavedObject>> responseExtractor = clientHttpResponse->{
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			try (InputStream is = clientHttpResponse.getBody();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is,StandardCharsets.UTF_8))) {
				Map<?,?> result = mapper.readValue(reader, Map.class);
				Number total = (Number)result.get("total");
				if (total==null || total.longValue()==0)
					return Collections.emptyList();
				List<?> saved_objects = (List<?>)result.get("saved_objects");
				if (saved_objects==null || saved_objects.isEmpty())
					return Collections.emptyList();
				return saved_objects.stream().map(obj->new KibanaSavedObject((Map<?,?>)obj)).collect(Collectors.toList());
			}
		}; 
		try {
			return restTemplate.execute(new URI(url), HttpMethod.GET, getKibanaRequestCallback(env,/*requestBody*/null), responseExtractor);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns information about all Kibana Dashboards created
	 */
	public static List<KibanaSavedObject> getKibanaDashboards(Environment env, RestTemplate restTemplate, String spaceId) {
		return getKibanaSavedObjects(env, restTemplate, spaceId, "dashboard");
	}
	
	/**
	 * Copy one or more SavedObjects (e.g.: Dashboards, IndexPatterns, Lens, etc.) from one SPACE to another
	 */
	public static boolean copyKibanaSavedObjects(Environment env, RestTemplate restTemplate, String spaceIdSource, String spaceIdTarget, String type, String[] ids) {
		String url = getKibanaURL(env, 
				(spaceIdSource!=null && spaceIdSource.trim().length()>0 && !"Default".equalsIgnoreCase(spaceIdSource)) ? "/s/"+spaceIdSource+"/api/spaces/_copy_saved_objects" 
						: "/api/spaces/_copy_saved_objects");
		
		@SuppressWarnings("unused")
		final class SavedObject {
			public final String type;
			public final String id;
			SavedObject(String type, String id) {
				this.type = type;
				this.id = id;
			}
		}
		@SuppressWarnings("unused")
		final class RequestBody {
			public final SavedObject[] objects;
			public final String[] spaces;
			public final boolean includeReferences;
			public final boolean overwrite;
			public final boolean createNewCopies;
			RequestBody(String type, String... ids) {
				this.objects = Arrays.stream(ids).map(id->new SavedObject(type, id)).toArray(SavedObject[]::new);
				this.spaces = new String[] { spaceIdTarget };
				this.includeReferences = true;
				this.overwrite = true;
				this.createNewCopies = false;
			}
		}
		
		RequestBody body = new RequestBody(type, ids);
		
		ResponseExtractor<Boolean> responseExtractor = clientHttpResponse->{
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			try (InputStream is = clientHttpResponse.getBody();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is,StandardCharsets.UTF_8))) {
				Map<?,?> result = mapper.readValue(reader, Map.class);
				Map<?,?> resultForTarget = (Map<?,?>)result.get(spaceIdTarget);
				if (resultForTarget==null)
					return null;
				Boolean success = (Boolean)resultForTarget.get("success");
				return Boolean.TRUE.equals(success);
			}
		}; 
		try {
			return restTemplate.execute(new URI(url), HttpMethod.POST, getKibanaRequestCallback(env, body), responseExtractor);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Returns references for a given Kibana Saved Object (e.g. dashboard, index pattern, etc.)
	 * @return Returns NULL if the object was not found. Returns EMPTY if the object was found but there were no references.
	 */
	public static List<KibanaSavedObject> getKibanaSavedObjectReferences(Environment env, RestTemplate restTemplate, String spaceId, String type, String id) {
		String url = getKibanaURL(env, 
				(spaceId!=null && spaceId.trim().length()>0 && !"Default".equalsIgnoreCase(spaceId)) ? "/s/"+spaceId+"/api/saved_objects/"+type+"/"+id 
						: "/api/saved_objects/"+type+"/"+id);
		
		ResponseExtractor<List<KibanaSavedObject>> responseExtractor = clientHttpResponse->{
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			try (InputStream is = clientHttpResponse.getBody();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is,StandardCharsets.UTF_8))) {
				Map<?,?> result = mapper.readValue(reader, Map.class);
				// Look for a property called 'references'
				List<?> references = (List<?>)result.get("references");
				if (references==null || references.isEmpty()) {
					// If there were no property called 'references' or if it was empty, check for an additional property called 'attributes'
					// The 'LENS' object may have internal references to 'index-pattern' object by means of 'attributes'
					Map<?,?> attributes = (Map<?,?>)result.get("attributes");
					if (attributes!=null && !attributes.isEmpty()) {
						Map<?,?> state = (Map<?,?>)attributes.get("state");
						if (state!=null && !state.isEmpty()) {
							Set<String> indexPatternIds = new HashSet<>();
							Map<?,?> datasourceStates = (Map<?,?>)state.get("datasourceStates");
							if (datasourceStates!=null && !datasourceStates.isEmpty()) {
								Map<?,?> indexpattern = (Map<?,?>)datasourceStates.get("indexpattern");
								if (indexpattern!=null && !indexpattern.isEmpty()) {
									String currentIndexPatternId = (String)indexpattern.get("currentIndexPatternId");
									if (currentIndexPatternId!=null)
										indexPatternIds.add(currentIndexPatternId);
									Map<?,?> layers = (Map<?,?>)indexpattern.get("layers");
									if (layers!=null && !layers.isEmpty()) {
										for (Object layer: layers.values()) {
											String indexPatternId = (String)((Map<?,?>)layer).get("indexPatternId");
											if (indexPatternId!=null)
												indexPatternIds.add(indexPatternId);
										}
									}
								}
							}
							if (!indexPatternIds.isEmpty())
								return indexPatternIds.stream().map(ip_id->new KibanaSavedObject(ip_id,"index-pattern")).collect(Collectors.toList());
						}
					}
					return Collections.emptyList();
				}
				return references.stream().map(obj->new KibanaSavedObject((Map<?,?>)obj)).collect(Collectors.toList());
			}
		}; 
		try {
			return restTemplate.execute(new URI(url), HttpMethod.GET, getKibanaRequestCallback(env,/*requestBody*/null), responseExtractor);
		}
		catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
			return null;
		}
		catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Returns information about all Kibana IndexPattern created
	 */
	public static List<KibanaSavedObject> getKibanaIndexPatterns(Environment env, RestTemplate restTemplate, String spaceId) {
		return getKibanaSavedObjects(env, restTemplate, spaceId, "index-pattern");
	}

	/**
	 * Returns information about all Kibana Visualization created
	 */
	public static List<KibanaSavedObject> getKibanaVisualizations(Environment env, RestTemplate restTemplate, String spaceId) {
		return getKibanaSavedObjects(env, restTemplate, spaceId, "visualization");
	}

	/**
	 * Returns information about all Kibana Lens created
	 */
	public static List<KibanaSavedObject> getKibanaLens(Environment env, RestTemplate restTemplate, String spaceId) {
		return getKibanaSavedObjects(env, restTemplate, spaceId, "lens");
	}

	/**
	 * Create a Kibana INDEX-PATTERN. Returns the ID of the new INDEX-PATTERN.
	 */
	public static String createKibanaIndexPattern(Environment env, 
			RestTemplate restTemplate,
			String spaceId,
			KibanaIndexPattern indexPattern) {
		
		String url = getKibanaURL(env, 
			(spaceId!=null && spaceId.trim().length()>0 && !"Default".equalsIgnoreCase(spaceId)) ? "/s/"+spaceId+"/api/index_patterns/index_pattern" 
					: "/api/index_patterns/index_pattern");

		ResponseExtractor<String> responseExtractor = clientHttpResponse->{
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			try (InputStream is = clientHttpResponse.getBody();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is,StandardCharsets.UTF_8))) {
				Map<?,?> result = mapper.readValue(reader, Map.class);
				Map<?,?> info = (Map<?,?>)result.get("index_pattern");
				return (String)info.get("id");
			}
		}; 
		try {
			Map<Object,Object> requestBody = new HashMap<>();
			requestBody.put("index_pattern", indexPattern);
			return restTemplate.execute(new URI(url), HttpMethod.POST, getKibanaRequestCallback(env,requestBody), responseExtractor);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}		
	}

	/**
	 * Updates an existent Kibana INDEX-PATTERN. Only the provided (not-null) fields are updated. All others are preserved.
	 * @param patternId The index patterns fields you want to update.
	 * @param refreshFields Reloads the index pattern fields after the index pattern is updated. The default is false.
	 */
	public static void updateKibanaIndexPattern(Environment env, 
			RestTemplate restTemplate,
			String spaceId,
			String patternId,
			UpdateIndexPatternRequest request) throws IOException {
		
		String url = getKibanaURL(env, 
			(spaceId!=null && spaceId.trim().length()>0 && !"Default".equalsIgnoreCase(spaceId)) ? "/s/"+spaceId+"/api/index_patterns/index_pattern/"+patternId 
					: "/api/index_patterns/index_pattern/"+patternId);
		
		ResponseExtractor<Boolean> responseExtractor = clientHttpResponse->{
			return true;
		}; 
		try {
			restTemplate.execute(new URI(url), HttpMethod.POST, getKibanaRequestCallback(env,/*requestBody*/request), responseExtractor);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}		
	}

	/**
	 * Returns information about all Kibana configurations
	 */
	public static List<KibanaSavedObject> getKibanaConfig(Environment env, RestTemplate restTemplate, String spaceId) throws IOException {
		return getKibanaSavedObjects(env, restTemplate, spaceId, "config");
	}

	/**
	 * Remove Kibana saved object
	 */
	public static void deleteKibanaSavedObject(Environment env, RestTemplate restTemplate, String spaceId, String type, String id) {
		String url = getKibanaURL(env, 
				(spaceId!=null && spaceId.trim().length()>0 && !"Default".equalsIgnoreCase(spaceId)) ? "/s/"+spaceId+"/api/saved_objects/"+type+"/"+id+"?force=true" 
						: "/api/saved_objects/"+type+"/"+id+"?force=true");
		
		try {
			restTemplate.execute(new URI(url), HttpMethod.DELETE, getKibanaRequestCallback(env,/*requestBody*/null), null);
		}
		catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
			return;
		}
		catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Returns URL for Kibana API requests
	 */
	public static String getKibanaURL(Environment env, String endpoint) {
		String kibanaHost = env.getProperty("kibana.host", "127.0.0.1");
		int kibanaPort = Integer.parseInt(env.getProperty("kibana.port", String.valueOf("5601")));	
		String protocol = (kibanaPort==443 || "true".equalsIgnoreCase(env.getProperty("es.ssl"))) ? "https" : "http";
		String context = env.getProperty("kibana.endpoint", "/kibana");
		return String.format("%s://%s:%d/%s/%s", protocol, kibanaHost, kibanaPort, context, endpoint);
	}
	
	/**
	 * Object used with Kibana API requests for passing along authorization token given the application properties
	 */
	public static RequestCallback getKibanaRequestCallback(Environment env, Object requestBody) {
		String kibanaUser = env.getProperty("es.user", "elastic");
		String kibanaPassword = ElasticClientFactory.readESPassword(env);
		if (kibanaUser!=null && kibanaUser.length()>0 && kibanaPassword!=null && kibanaPassword.length()>0) {
			String encoding = Base64.getEncoder().encodeToString((String.join(":", kibanaUser, kibanaPassword)).getBytes());
			return req->{
				req.getHeaders().set(HttpHeaders.AUTHORIZATION, "Basic "+encoding);
				if (requestBody!=null) {
					req.getHeaders().set("kbn-xsrf", "reporting");
					ObjectMapper mapper = new ObjectMapper();
					mapper.setSerializationInclusion(Include.NON_NULL);
					req.getBody().write(mapper.writeValueAsBytes(requestBody));
				}
			};
		}
		else if (requestBody!=null) {
			return req->{
				req.getHeaders().set("kbn-xsrf", "reporting");
				ObjectMapper mapper = new ObjectMapper();
				mapper.setSerializationInclusion(Include.NON_NULL);
				req.getBody().write(mapper.writeValueAsBytes(requestBody));
			};			
		}
		else {
			return req->{
				req.getHeaders().set("kbn-xsrf", "reporting");
			};			
		}
	}
	
	/**
	 * Copy transitive dependencies from one SPACE to another at KIBANA
	 */
	public static void copyTransitiveDependencies(Environment env, RestTemplate restTemplate, String sourceSpaceId, String targetSpaceId, String type, String id, final int max_iterations) throws IOException {
		Set<String> checked_id = new HashSet<>();
		checked_id.add(id);
		List<org.idb.cacao.web.utils.ESUtils.KibanaSavedObject> references = ESUtils.getKibanaSavedObjectReferences(env, restTemplate, targetSpaceId, type, id);
		int count_iterations = 0;
		while (references!=null && !references.isEmpty()) {
			count_iterations++;
			if (count_iterations>max_iterations)
				break;
			List<org.idb.cacao.web.utils.ESUtils.KibanaSavedObject> ref_to_check = references;
			references = null;
			for (org.idb.cacao.web.utils.ESUtils.KibanaSavedObject ref: ref_to_check) {
				if (checked_id.contains(ref.getId()))
					continue;
				checked_id.add(ref.getId());
				List<org.idb.cacao.web.utils.ESUtils.KibanaSavedObject> transitive_references = ESUtils.getKibanaSavedObjectReferences(env, restTemplate, targetSpaceId, ref.getType(), ref.getId());
				if (transitive_references==null) {
					// At this point we know that 'ref' does not exist, so we need to copy
					boolean success_copy_ref = ESUtils.copyKibanaSavedObjects(env, restTemplate, sourceSpaceId, targetSpaceId, ref.getType(), new String[] { ref.getId() });
					if (success_copy_ref) {
						transitive_references = ESUtils.getKibanaSavedObjectReferences(env, restTemplate, targetSpaceId, ref.getType(), ref.getId());
					}						
				}
				if (transitive_references!=null) {
					if (transitive_references.isEmpty())
						continue; // 'ref' exists and does not have additional references
					// At this point we know that 'ref' exists, but there are additional references to be checked at next iteration
					if (references==null)
						references = new LinkedList<>();
					references.addAll(transitive_references);
				}
			}
		}		
	}

	/**
	 * Simplified representation of an index summary from ElasticSearch
	 * 
	 * @author Gustavo Figueiredo
	 *
	 */
	public static class IndexSummary {
		private String health;
		private String status;
		private String index;
		private int pri;
		private int rep;
		private long docsCount;
		private long docsDeleted;
		private String storeSize;
		private String priStoreSize;
		public String getHealth() {
			return health;
		}
		public void setHealth(String health) {
			this.health = health;
		}
		public String getStatus() {
			return status;
		}
		public void setStatus(String status) {
			this.status = status;
		}
		public String getIndex() {
			return index;
		}
		public void setIndex(String index) {
			this.index = index;
		}
		public int getPri() {
			return pri;
		}
		public void setPri(int pri) {
			this.pri = pri;
		}
		public int getRep() {
			return rep;
		}
		public void setRep(int rep) {
			this.rep = rep;
		}
		public long getDocsCount() {
			return docsCount;
		}
		public void setDocsCount(long docsCount) {
			this.docsCount = docsCount;
		}
		public long getDocsDeleted() {
			return docsDeleted;
		}
		public void setDocsDeleted(long docsDeleted) {
			this.docsDeleted = docsDeleted;
		}
		public String getStoreSize() {
			return storeSize;
		}
		public void setStoreSize(String storeSize) {
			this.storeSize = storeSize;
		}
		public String getPriStoreSize() {
			return priStoreSize;
		}
		public void setPriStoreSize(String priStoreSize) {
			this.priStoreSize = priStoreSize;
		}		
	}
	
	@JsonInclude(NON_NULL)
	public static class UpdateIndexPatternRequest {
		
		/**
		 * Reloads the index pattern fields after the index pattern is updated. The default is false.
		 */
		private Boolean refresh_fields;
		
		/**
		 * The index patterns fields you want to update.
		 */
		private Map<String,Object> index_pattern;

		/**
		 * Reloads the index pattern fields after the index pattern is updated. The default is false.
		 */
		public Boolean getRefresh_fields() {
			return refresh_fields;
		}

		/**
		 * Reloads the index pattern fields after the index pattern is updated. The default is false.
		 */
		public void setRefresh_fields(Boolean refresh_fields) {
			this.refresh_fields = refresh_fields;
		}

		/**
		 * The index patterns fields you want to update.
		 */
		public Map<String, Object> getIndex_pattern() {
			return index_pattern;
		}

		/**
		 * The index patterns fields you want to update.
		 */
		public void setIndex_pattern(Map<String, Object> index_pattern) {
			this.index_pattern = index_pattern;
		}
		
	}

	/**
	 * This object represents a SPACE in KIBANA for usage in API
	 * 
	 * @author Gustavo Figueiredo
	 *
	 */
	@JsonInclude(NON_NULL)
	public static class KibanaSpace implements Comparable<KibanaSpace> {

		private String id;
		
		private String name;
		
		private String description;
		
		private String imageUrl;
		
		private String color;
		
		private String initials;
		
		private String[] disabledFeatures;
		
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getImageUrl() {
			return imageUrl;
		}

		public void setImageUrl(String imageUrl) {
			this.imageUrl = imageUrl;
		}

		public String getColor() {
			return color;
		}

		public void setColor(String color) {
			this.color = color;
		}

		public String getInitials() {
			return initials;
		}

		public void setInitials(String initials) {
			this.initials = initials;
		}

		public String[] getDisabledFeatures() {
			return disabledFeatures;
		}

		public void setDisabledFeatures(String[] disabledFeatures) {
			this.disabledFeatures = disabledFeatures;
		}
		
		public String toString() {
			ObjectMapper objectMapper = new ObjectMapper();
			try {
				return objectMapper.writeValueAsString(this);
			} catch (JsonProcessingException e) {
				return super.toString();
			}
		}

		@Override
		public int compareTo(KibanaSpace o) {
			return String.CASE_INSENSITIVE_ORDER.compare(name, o.name);
		}
	}
	
	/**
	 * This object represents a SAVED OBJECT in KIBANA  (e.g.: Dashboard, IndexPattern, Lens, Visualization, etc.) for usage in API
	 * 
	 * @author Gustavo Figueiredo
	 *
	 */
	@JsonInclude(NON_NULL)
	public static class KibanaSavedObject implements Comparable<KibanaSavedObject> {

		private String id;
		
		private String type;
		
		private String title;
		
		private String timeFieldName;
		
		private String namespace;

		public KibanaSavedObject() { }
		
		public KibanaSavedObject(String id, String type) {
			this.id = id;
			this.type = type;
		}
		
		public KibanaSavedObject(Map<?,?> properties) {
			this.id = (String)properties.get("id");
			this.type = (String)properties.get("type");
			Map<?,?> attributes = (Map<?,?>)properties.get("attributes");
			if (attributes!=null) {
				this.title = ((String)attributes.get("title"));
				this.timeFieldName = ((String)attributes.get("timeFieldName"));
			}
			else {
				this.title = (String)properties.get("name");
			}
			List<?> namespaces = (List<?>)properties.get("namespaces");
			if (namespaces!=null && !namespaces.isEmpty()) {
				this.namespace = (String)namespaces.get(0);
			}
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getTimeFieldName() {
			return timeFieldName;
		}

		public void setTimeFieldName(String timeFieldName) {
			this.timeFieldName = timeFieldName;
		}

		public String getNamespace() {
			return namespace;
		}

		public void setNamespace(String namespace) {
			this.namespace = namespace;
		}
		
		public String toString() {
			ObjectMapper objectMapper = new ObjectMapper();
			try {
				return objectMapper.writeValueAsString(this);
			} catch (JsonProcessingException e) {
				return super.toString();
			}
		}

		@Override
		public int compareTo(KibanaSavedObject o) {
			return String.CASE_INSENSITIVE_ORDER.compare(nvl(title,id), nvl(o.title,o.id));
		}
		
		private static String nvl(String s1, String s2) {
			if (s1==null)
				return s2;
			else
				return s1;
		}
	}
	
	/**
	 * This object represents a INDEX-PATTERN in KIBANA for usage in API
	 * 
	 * @author Gustavo Figueiredo
	 *
	 */
	@JsonInclude(NON_NULL)
	public static class KibanaIndexPattern implements Comparable<KibanaIndexPattern> {

		private String id;
		
		private String version;
		
		private String title;
		
		private String timeFieldName;
		
		private Map<?,?> fields;
		
		private Boolean allowNoIndex;
		
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getTimeFieldName() {
			return timeFieldName;
		}

		public void setTimeFieldName(String timeFieldName) {
			this.timeFieldName = timeFieldName;
		}

		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}

		public Map<?, ?> getFields() {
			return fields;
		}

		public void setFields(Map<?, ?> fields) {
			this.fields = fields;
		}

		public Boolean getAllowNoIndex() {
			return allowNoIndex;
		}

		public void setAllowNoIndex(Boolean allowNoIndex) {
			this.allowNoIndex = allowNoIndex;
		}

		public String toString() {
			ObjectMapper objectMapper = new ObjectMapper();
			try {
				return objectMapper.writeValueAsString(this);
			} catch (JsonProcessingException e) {
				return super.toString();
			}
		}

		@Override
		public int compareTo(KibanaIndexPattern o) {
			return String.CASE_INSENSITIVE_ORDER.compare(title, o.title);
		}
	}
}
