/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.ui;

import java.util.List;

import org.idb.cacao.web.entities.Item;
import org.idb.cacao.web.errors.ItemNotFoundException;
import org.idb.cacao.web.repositories.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ItemUIController {
	
    @Autowired
    ItemRepository itemRepo;

    @RequestMapping("/getAllItems")
    @ResponseBody
    public ResponseEntity<List<Item>> getAllItems(){
        List<Item> items = itemRepo.findAll();
        return new ResponseEntity<List<Item>>(items, HttpStatus.OK);
    }

    @GetMapping("/item/{itemId}")
    @ResponseBody
    public ResponseEntity<Item> getItem(@PathVariable String itemId){
        if(itemId == null || itemId.isEmpty()){
            throw new ItemNotFoundException("Invalid ItemId");
        }
        Item item = itemRepo.findById(itemId).orElse(null);
        if ( item == null ) //NOT FOUND
        	return new ResponseEntity<Item>(HttpStatus.NOT_FOUND);
        return new ResponseEntity<Item>(item, HttpStatus.OK);
    }

    @PostMapping(value = "/addItem",consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Item> addItem(@RequestBody Item item){
        try {
			itemRepo.save(item);
		} catch (Exception e) {
			e.printStackTrace();
		}
        return new ResponseEntity<Item>(item, HttpStatus.CREATED);
    }

    @PutMapping(value = "/updateItem",consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Item> updateItem(@RequestBody Item item){
        if(item != null){
            try {
				Item saved = itemRepo.save(item);
				saved.toString();
			} catch (Exception e) {
				e.printStackTrace();
			}
        }
        return new ResponseEntity<Item>(item, HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteItem(@PathVariable String id){
        itemRepo.deleteById(id);
        return new ResponseEntity<Void>(HttpStatus.ACCEPTED);
    }
}
