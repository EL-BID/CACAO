package org.idb.cacao.web.controllers;

import static org.junit.jupiter.api.Assertions.*;

import org.idb.cacao.mock_es.ElasticsearchMockClient;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.entities.UserProfile;
import org.idb.cacao.web.repositories.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.Optional;

@AutoConfigureJsonTesters
@RunWith(JUnitPlatform.class)
@AutoConfigureMockMvc
@SpringBootTest( webEnvironment = WebEnvironment.RANDOM_PORT)
class UserAPIControllerTests {

	private static ElasticsearchMockClient mockElastic;

	@Autowired
	private MockMvc mvc;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private JacksonTester<User> json;
	
	@BeforeAll
	public static void beforeClass() throws Exception {

		int port = ElasticsearchMockClient.findRandomPort();
		mockElastic = new ElasticsearchMockClient(port);
		System.setProperty("es.port", String.valueOf(port));
	}
	
	@AfterAll
	public static void afterClass() {
		if (mockElastic!=null)
			mockElastic.stop();
	}
	
	@BeforeEach
	void setUp() throws Exception {
	}

	@WithUserDetails(value="admin@admin",userDetailsServiceBeanName="CustomUserDetailsService")
	@Test
	void testHandleFileUpload() throws Exception {
		User user = new User();
		user.setLogin("k@g.com");
		user.setName("Joao da Silva");
		user.setProfile(UserProfile.SYSADMIN);
		MockHttpServletResponse response = mvc.perform(
                post("/api/user")
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.write(user).getJson()
                    )
            )
            .andReturn()
            .getResponse();
		
		assertEquals(response.getStatus(), HttpStatus.OK.value());
		String id = JsonPath.read(response.getContentAsString(), "$.id");
		
        assertNotNull(id);
        
        Optional<User> savedUser = userRepository.findById(id);
        assertNotNull(savedUser);
        
        user = savedUser.get();
        
        assertEquals("k@g.com", user.getLogin());
        assertEquals("Joao da Silva", user.getName());
	}
}
