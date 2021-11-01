package org.idb.cacao.web.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.idb.cacao.web.controllers.ui.ItemUIController;
import org.idb.cacao.web.entities.Item;
import org.idb.cacao.web.repositories.ItemRepository;
import org.idb.cacao.web.utils.ElasticsearchMockClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@RunWith(JUnitPlatform.class)
@AutoConfigureJsonTesters
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = {
		"storage.incoming.files.original.dir=${java.io.tmpdir}/cacao/storage" })
/**
 * A group of tests over a test entity using API controller 
 * 
 * @author Rivelino Patrício
 * 
 * @since 31/10/2021
 *
 */
public class ItemRESTControllerTests {

	private static ElasticsearchMockClient mockElastic;

	@InjectMocks
	ItemUIController itemController;

	@Autowired
	ItemRepository itemRepository;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JacksonTester<Item> json;
	
	@BeforeAll
	public static void beforeClass() throws Exception {

		int port = ElasticsearchMockClient.findRandomPort();
		mockElastic = new ElasticsearchMockClient(port);
		System.setProperty("es.port", String.valueOf(port));
	}

	@AfterAll
	public static void afterClass() {
		if (mockElastic != null)
			mockElastic.stop();
	}

	@BeforeEach
	void setUp() throws Exception {
	}


	@WithUserDetails(value = "admin@admin", userDetailsServiceBeanName = "CustomUserDetailsService")
	@Test
	public void addItem() throws Exception {
		Item item = new Item("iPhoneX", "Mobiles");
		MockHttpServletResponse response = mockMvc.perform(MockMvcRequestBuilders.post("/api/addItem").accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.content(json.write(item).getJson())).andReturn().getResponse();
		
		assertEquals(HttpStatus.OK.value(),response.getStatus());
		
		List<Item> itens = itemRepository.findAll();
		
		assertNotNull(itens);
		assertEquals(1,itens.size());
		
		item = itens.get(0);

        assertNotNull(item.getId());
        assertEquals(item.getName(),"iPhoneX");
        assertEquals(item.getCategory(),"Mobiles");

	}
	
	@WithUserDetails(value = "admin@admin", userDetailsServiceBeanName = "CustomUserDetailsService")
	@Test
	public void updateItem() throws Exception {
		Item item = new Item("iPhoneX", "Mobiles");
		Item saved = itemRepository.save(item);
		
		saved.setName("Mi Band 3");
		saved.setCategory("Watch");
		
		MockHttpServletResponse response = mockMvc.perform(MockMvcRequestBuilders.put("/api/updateItem")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.write(saved).getJson()))
				.andReturn().getResponse();
		
		assertEquals(HttpStatus.OK.value(),response.getStatus());
		
		Item recovered = itemRepository.findById(saved.getId()).orElse(null);
		
		assertNotNull(recovered);
		
		assertEquals(saved.getId(),recovered.getId());
        assertEquals("Mi Band 3",recovered.getName());
        assertEquals("Watch",recovered.getCategory());
		
	}
	
	@WithUserDetails(value = "admin@admin", userDetailsServiceBeanName = "CustomUserDetailsService")
    @Test
    public void deleteItem() throws Exception{
		Item item = new Item("iPhoneX", "Mobiles");
		Item saved = itemRepository.save(item);
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/delete/" + saved.getId())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isAccepted());
    }	

	@WithUserDetails(value = "admin@admin", userDetailsServiceBeanName = "CustomUserDetailsService")
	@Test
	public void getAllItems() throws Exception {
		List<Item> itens = Arrays.asList(new Item("iPhoneX", "Mobiles"));
		itemRepository.save(itens.get(0));	
		mockMvc.perform(MockMvcRequestBuilders.get("/api/getAllItems")).andExpect(MockMvcResultMatchers.status().isOk());
	}
}
