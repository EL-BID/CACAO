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
package org.idb.cacao.web.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

import org.apache.commons.lang3.StringUtils;
import org.idb.cacao.mock_es.ElasticsearchMockClient;
import org.idb.cacao.web.dto.UserDto;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.entities.UserProfile;
import org.idb.cacao.web.repositories.UserRepository;
import org.idb.cacao.web.utils.TestDataGenerator;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.Optional;
import java.util.Random;

@AutoConfigureJsonTesters
@RunWith(JUnitPlatform.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@SpringBootTest( webEnvironment = WebEnvironment.RANDOM_PORT)
class UserAPIControllerTests {

	private static ElasticsearchMockClient mockElastic;

	@Autowired
	private MockMvc mvc;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private JacksonTester<UserDto> json;
	
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

	public void assertEqualsSaved(String id, UserDto user) {
        Optional<User> existing = userRepository.findById(id);
        assertTrue(existing.isPresent());
        
        User saved = existing.get();

		assertEquals(user.getLogin(), saved.getLogin());
		assertEquals(user.getName(), saved.getName());
		assertEquals(user.getProfile(), saved.getProfile());
		assertEquals(user.getTaxpayerId(), saved.getTaxpayerId());
	}
	
	@WithUserDetails(value="admin@admin",userDetailsServiceBeanName="CustomUserDetailsService")
	@Test
	void testCreateUser() throws Exception {
		Random random = new Random(TestDataGenerator.generateSeed("CREATE"));
		
		for(UserProfile profile: UserProfile.values()) {
			UserDto user = TestDataGenerator.generateUser(random.nextLong(), "mydomain.com", profile, null);
			
			MockHttpServletResponse response = mvc.perform(
	                post("/api/user")
	                	.with(csrf())
	                    .accept(MediaType.APPLICATION_JSON)
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content(
	                        json.write(user).getJson()
	                    )
	            )
	            .andReturn()
	            .getResponse();
			
			assertEquals(HttpStatus.OK.value(),response.getStatus());
			String id = JsonPath.read(response.getContentAsString(), "$.id");
			
	        assertNotNull(id);
	        
	        assertEqualsSaved(id, user);
		}
	}
	
	public void assertValidationError(UserDto user, String message) throws Exception {
		MockHttpServletResponse response = mvc.perform(
                post("/api/user")
                	.with(csrf())
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.write(user).getJson()
                    )
            )
            .andReturn()
            .getResponse();
		
		assertEquals(HttpStatus.BAD_REQUEST.value(),response.getStatus());
		checkErrorMessage(response.getContentAsString(), message);
	}
	
	@WithUserDetails(value="admin@admin",userDetailsServiceBeanName="CustomUserDetailsService")
	@Test
	void testCreateUserWithErrors() throws Exception {
		Random random = new Random(TestDataGenerator.generateSeed("CREATE"));
		
		UserDto user = TestDataGenerator.generateUser(random.nextLong(), "mydomain.com", UserProfile.DECLARANT, u -> u.setLogin(null));
		assertValidationError(user, "login");
		user = TestDataGenerator.generateUser(random.nextLong(), "mydomain.com", UserProfile.DECLARANT, u -> u.setLogin("this.is.a.real.large.email.and.should.not.be.accepted@mydomain.com"));
		assertValidationError(user, "login");
		user = TestDataGenerator.generateUser(random.nextLong(), "mydomain.com", UserProfile.DECLARANT, u -> u.setLogin("this is not an email"));
		assertValidationError(user, "login");
		user = TestDataGenerator.generateUser(random.nextLong(), "mydomain.com", UserProfile.DECLARANT, u -> u.setName(""));
		assertValidationError(user, "name");
		user = TestDataGenerator.generateUser(random.nextLong(), "mydomain.com", UserProfile.DECLARANT, u -> u.setName("Joe"));
		assertValidationError(user, "name");
		user = TestDataGenerator.generateUser(random.nextLong(), "mydomain.com", UserProfile.DECLARANT, u -> u.setName(StringUtils.repeat("this is too long ", 10)));
		assertValidationError(user, "name");
		// Wrong confirmation password
		user = TestDataGenerator.generateUser(random.nextLong(), "mydomain.com", UserProfile.DECLARANT, u -> { u.setPassword("123"); u.setConfirmPassword("111"); });
		assertValidationError(user, "password");
	}

	private void checkErrorMessage(String errorMessage, String message) {
		assertTrue(errorMessage.contains(message), errorMessage + "[" + message + "]");
	}
	
	private UserDto save(UserDto user) {
		User entity = new User();
		user.updateEntity(entity);
		return new UserDto(userRepository.saveWithTimestamp(entity));
	}
	
	@WithUserDetails(value="admin@admin",userDetailsServiceBeanName="CustomUserDetailsService")
	@Test
	void testCreateExistingUser() throws Exception {
		Random random = new Random(TestDataGenerator.generateSeed("CREATE"));
		
		UserDto user = save(TestDataGenerator.generateUser(random.nextLong(), "mydomain.com", UserProfile.DECLARANT, null));
		
		MockHttpServletResponse response = mvc.perform(
                post("/api/user")
                	.with(csrf())
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.write(user).getJson()
                    )
            )
            .andReturn()
            .getResponse();
		
		assertEquals(HttpStatus.BAD_REQUEST.value(),response.getStatus());
		checkErrorMessage(response.getContentAsString(), "exists");
	}

	@WithUserDetails(value="admin@admin",userDetailsServiceBeanName="CustomUserDetailsService")
	@Test
	void testEditUser() throws Exception {
		Random random = new Random(TestDataGenerator.generateSeed("EDIT"));
		
		UserDto user = save(TestDataGenerator.generateUser(random.nextLong(), "mydomain.com", UserProfile.DECLARANT, null));
		UserDto user2 = TestDataGenerator.generateUser(random.nextLong(), "mydomain.com", UserProfile.SYSADMIN, null);
		
		MockHttpServletResponse response = mvc.perform(
                put("/api/user/" + user.getId())
                	.with(csrf())
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.write(user2).getJson()
                    )
            )
            .andReturn()
            .getResponse();
		
		assertEquals(HttpStatus.OK.value(),response.getStatus());
		String id = JsonPath.read(response.getContentAsString(), "$.id");
		
        assertNotNull(id);
        
        assertEqualsSaved(id, user2);
	}
	
	@WithUserDetails(value="admin@admin",userDetailsServiceBeanName="CustomUserDetailsService")
	@Test
	void testActivateUser() throws Exception {
		Random random = new Random(TestDataGenerator.generateSeed("ACTIVATE"));
		
		UserDto user = save(TestDataGenerator.generateUser(random.nextLong(), "mydomain.com", UserProfile.DECLARANT, t -> t.setActive(false)));
		
		MockHttpServletResponse response = mvc.perform(
                get("/api/user/" + user.getId() + "/activate")
                	.with(csrf())
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andReturn()
            .getResponse();
		
		assertEquals(HttpStatus.OK.value(),response.getStatus());
		String id = JsonPath.read(response.getContentAsString(), "$.id");
        assertNotNull(id);
        user.setActive(true);
        assertEqualsSaved(id, user);
	}
	
	@WithUserDetails(value="admin@admin",userDetailsServiceBeanName="CustomUserDetailsService")
	@Test
	void testDeactivateUser() throws Exception {
		Random random = new Random(TestDataGenerator.generateSeed("DEACTIVATE"));
		
		UserDto user = save(TestDataGenerator.generateUser(random.nextLong(), "mydomain.com", UserProfile.DECLARANT, t -> t.setActive(true)));
		
		MockHttpServletResponse response = mvc.perform(
                get("/api/user/" + user.getId() + "/deactivate")
                	.with(csrf())
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andReturn()
            .getResponse();
		
		assertEquals(HttpStatus.OK.value(),response.getStatus());
		String id = JsonPath.read(response.getContentAsString(), "$.id");
        assertNotNull(id);
        user.setActive(false);
        assertEqualsSaved(id, user);
        
	}



}
