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
package org.idb.cacao.web.utils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.LogManager;

/**
 * This is a 'mocked' ElasticSearch client. It may be used for unit testing simulating 
 * a ElasticSearch server without actually using one.<BR>
 * <BR>
 * All the responses will be mocked according to the expectations programmed.
 *
 */
public class ElasticsearchMockClient {

	private final ClientAndServer mockServer;

	public String version = "7.14.1";
	public String clusterName = "mock";
	private static final String MOCK_SERVER_LOG_LEVEL = "WARNING";
	//private static final String MOCK_SERVER_LOG_LEVEL = "INFO";
	//private static final String MOCK_SERVER_LOG_LEVEL = "DEBUG";

	// Setup LOG level for MockServer
	static {
		ConfigurationProperties.logLevel(MOCK_SERVER_LOG_LEVEL);
		String loggingConfiguration = "" +
		    "handlers=org.mockserver.logging.StandardOutConsoleHandler\n" +
		    "org.mockserver.logging.StandardOutConsoleHandler.level="+MOCK_SERVER_LOG_LEVEL+"\n" +
		    "org.mockserver.logging.StandardOutConsoleHandler.formatter=java.util.logging.SimpleFormatter\n" +
		    "java.util.logging.SimpleFormatter.format=%1$tF %1$tT  %3$s  %4$s  %5$s %6$s%n\n" +
		    ".level="+MOCK_SERVER_LOG_LEVEL+"\n" +
		    "io.netty.handler.ssl.SslHandler.level="+MOCK_SERVER_LOG_LEVEL;
		try {
			LogManager.getLogManager().readConfiguration(new ByteArrayInputStream(loggingConfiguration.getBytes(StandardCharsets.UTF_8)));
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	private final Map<String, MockedIndex> map_indices;

	public ElasticsearchMockClient(final int port) {
		this.mockServer = startClientAndServer(port);
		this.map_indices = new HashMap<>();
		init();
	}

	public static Integer findRandomPort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}

	public void stop() {
		mockServer.stop();
	}
	
	protected void init() {
		
		// Program all the expectations for each request pattern
		
        this.mockServer.when(
                HttpRequest.request()
                        .withPath(".*/_count")
        ).respond(toHttpResponse(new JSONObject(map("count", 0))));

        this.mockServer.when(
                HttpRequest.request()
                        .withPath(".*/_search")
        ).respond(toHttpResponse(new JSONObject(map(
        		"took", 10,
        		"timed_out", false,
        		"_shards", map("total", 1, 
        				"successful", 1, 
        				"skipped", 0, 
        				"failed", 0),
        		"hits" , map(
        				"total", map("value", 0, "relation", "eq"),
        				"hits", new JSONArray()
        				)
        		))));

        this.mockServer.when(
                HttpRequest.request()
                        .withPath(".*/_refresh")
        ).respond(toHttpResponse(new JSONObject(map("_shards", map("total", 100, "successful", 100, "failed", 0)))));

        this.mockServer.when(
                HttpRequest.request()
                        .withPath(".*/_bulk")
        ).respond(toHttpResponse(new JSONObject(map("took", 10, "errors", false, "items", new JSONArray()))));
		
        // Create index
        this.mockServer.when(
                HttpRequest.request().withMethod("PUT")
        )
        .respond(createIndex());
		
        // Add document to index
        this.mockServer.when(
                HttpRequest.request()
                	.withPath(".*/_doc")
                	.withMethod("POST")
        )
        .respond(postDocument());

        // Check if index exists
        this.mockServer.when(
                HttpRequest.request().withMethod("HEAD")
        )
        .respond(checkIndexExists());
		
		// Default: return cluster version information
		this.mockServer.when(HttpRequest.request()).respond(toHttpResponse(
				new JSONObject(map("name", "mock", 
						"cluster_name", clusterName, 
						"version", map("number", version)))));
		
	}
	
    private HttpResponse toHttpResponse(final JSONObject data) {
        return HttpResponse.response(data.toString()).withHeader("Content-Type", "application/json");
    }
    
    private ExpectationResponseCallback createIndex() {
    	return new ExpectationResponseCallback() {
			@Override
			public HttpResponse handle(HttpRequest request) throws Exception {
				String index_name = request.getPath().toString().replace("/", "");
				MockedIndex mocked_index = new MockedIndex();
				mocked_index.setName(index_name);
				map_indices.put(index_name, mocked_index);
				return toHttpResponse(new JSONObject(map("acknowledged", true, "shards_acknowledged", true, "index", index_name)));
			}    		
    	};
    }
    
    private ExpectationResponseCallback postDocument() {
       	return new ExpectationResponseCallback() {
    			@Override
    			public HttpResponse handle(HttpRequest request) throws Exception {
    				String index_name = request.getPath().toString().split("/")[1];
    				//MockedIndex mocked_index = map_indices.get(index_name);
    				JSONObject response = new JSONObject(
    				map("_shards", map("total", 1, 
            				"successful", 1, 
            				"skipped", 0, 
            				"failed", 0),
    					"_index", index_name,
    					"_type", "_doc",
    					"_id", UUID.randomUUID().toString(),
    					"_version", 1,
    					"_seq_no", 0,
    					"_primary_term", 1,
    					"result", "created")
    				);
    				return toHttpResponse(response);
    			}    		
        	};    	
    }
	
    private ExpectationResponseCallback checkIndexExists() {
    	return new ExpectationResponseCallback() {
			@Override
			public HttpResponse handle(HttpRequest request) throws Exception {
				String index_name = request.getPath().toString().replace("/", "");
				if (map_indices.containsKey(index_name))
					return HttpResponse.response();
				else
					return HttpResponse.notFoundResponse();
			}    		
    	};
    }
    
    @SuppressWarnings("unchecked")
	private <K, V> Map<K, V> map(Object... args) {
        Map<K, V> res = new HashMap<>();
        K key = null;
        for (Object arg : args) {
            if (key == null) {
                key = (K) arg;
            } else {
                res.put(key, (V) arg);
                key = null;
            }
        }
        return res;
    }

    public static class MockedIndex {
    	private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}    	
		
		public String toString() {
			return name;
		}
    }
}
