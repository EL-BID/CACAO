/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import org.idb.cacao.web.entities.Item;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

@RunWith(JUnitPlatform.class)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
/**
 * A group of tests over a test entity using UI controller 
 * 
 * @author Rivelino Patrício
 * 
 * @since 31/10/2021
 *
 */
class FieldPropertiesTests {


	@Test
	void allProperties() throws Exception {
		
		Map<String,FieldProperties> allProperties = FieldProperties.toFieldProperties(Item.class);
		
		assertNotNull(allProperties, "Expected to have an object");
		
		//assertEquals(4, allProperties.size(), "Expected to have 4 itens");
		
		FieldProperties p = allProperties.get("name");
		
		assertNotNull(p, "Expected to have an object");
		
		assertEquals("item.name", p.getExternalName(), "Expected to have this external name");
		
		assertEquals(Boolean.TRUE, p.isEditable());
		
		assertEquals(Boolean.TRUE, p.isNotNull());
		
		assertEquals(Boolean.TRUE, p.isNotEmpty());
		
		assertEquals(Boolean.FALSE, p.isEmail());
		
		assertEquals(Boolean.FALSE, p.isId());
		
	}

}