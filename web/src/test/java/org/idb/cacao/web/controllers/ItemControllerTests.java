package org.idb.cacao.web.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.hamcrest.Matchers;
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
import org.mockito.Mockito;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@RunWith(JUnitPlatform.class)
@AutoConfigureJsonTesters
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = {
		"storage.incoming.files.original.dir=${java.io.tmpdir}/cacao/storage" })
class ItemControllerTests {

	private static ElasticsearchMockClient mockElastic;

	@InjectMocks
	ItemController itemController;

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

//	@WithUserDetails(value = "admin@admin", userDetailsServiceBeanName = "CustomUserDetailsService")
//	@Test
//	public void getAllItems() throws Exception {
//		List<Item> itens = Arrays.asList(new Item("iPhoneX", "Mobiles"));
//		itemRepository.save(itens.get(0));
//        //Mockito.when(itemRepository.findAll()).thenReturn(itens);	
//		mockMvc.perform(MockMvcRequestBuilders.get("/getAllItems")).andExpect(MockMvcResultMatchers.status().isOk());
//	}

	@WithUserDetails(value = "admin@admin", userDetailsServiceBeanName = "CustomUserDetailsService")
	@Test
	public void getItem() throws Exception {
		Item item = new Item("iPhoneX", "Mobiles");
		Item saved = itemRepository.save(item);

		mockMvc.perform(MockMvcRequestBuilders.get("/item/" + saved.getId()).accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.name", Matchers.is("iPhoneX")))
				.andExpect(MockMvcResultMatchers.jsonPath("$.category", Matchers.is("Mobiles")));
		Mockito.verify(itemRepository).findById(saved.getId()).get();
	}

//	@WithUserDetails(value = "admin@admin", userDetailsServiceBeanName = "CustomUserDetailsService")
//	@Test
//	public void addItem() throws Exception {
//		Item item = new Item("iPhoneX", "Mobiles");
//		MockHttpServletResponse response = mockMvc.perform(MockMvcRequestBuilders.post("/addItem").accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
//				.content(json.write(item).getJson())).andReturn().getResponse();
//		
//		assertEquals(HttpStatus.CREATED.value(),response.getStatus());
//		
//		String responseType = response.getContentType();
//		
//		List<Item> itens = itemRepository.findAll();
//		
//		assertNotNull(itens);
//		assertEquals(1,itens.size());
//		
//		item = itens.get(0);
//
//        assertNotNull(item.getId());
//        assertEquals(item.getName(),"iPhoneX");
//        assertEquals(item.getCategory(),"Mobiles");		
//
//	}

//    @Test
//    @Ignore
//    public void updateItem() throws Exception {
//        String jsonString = "{\n" +
//                "\"id\":1,\n" +
//                "\"name\":\"iPhoneX\",\n" +
//                "\"category\":\"Mobiles\"\n" +
//                "}";
//        //Item item = new Item("1","iPhoneX","Mobiles");
//        mockMvc.perform(MockMvcRequestBuilders.put("/updateItem")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(jsonString))
//                .andExpect(MockMvcResultMatchers.status().isOk())
//                .andExpect(MockMvcResultMatchers.jsonPath("$.id", Matchers.is(1)))
//                .andExpect(MockMvcResultMatchers.jsonPath("$.name",Matchers.is("iPhoneX")))
//                .andExpect(MockMvcResultMatchers.jsonPath("$.category",Matchers.is("Mobiles")));
//    }
//
//    @Test
//    @Ignore
//    public void deleteItem() throws Exception{
//        mockMvc.perform(MockMvcRequestBuilders.delete("/delete/1")
//                .accept(MediaType.APPLICATION_JSON))
//                .andExpect(MockMvcResultMatchers.status().isAccepted());
//    }
	
}
