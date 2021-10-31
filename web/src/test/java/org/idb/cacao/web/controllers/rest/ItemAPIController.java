package org.idb.cacao.web.controllers.rest;

import java.util.List;

import javax.validation.Valid;

import org.idb.cacao.web.entities.Item;
import org.idb.cacao.web.repositories.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
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
@RequestMapping("/api")
public class ItemAPIController {

	@Autowired
	ItemRepository itemRepository;

	@Autowired
	private MessageSource messageSource;

	@PostMapping(value = "/addItem", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> addItem(@Valid @RequestBody Item item, BindingResult result) {

		try {
			itemRepository.saveWithTimestamp(item);
		} catch (Exception ex) {
			return ResponseEntity.badRequest()
					.body(messageSource.getMessage("op.failed", null, LocaleContextHolder.getLocale()));
		}
		return ResponseEntity.ok().body(item);

	}

	@PutMapping(value = "/updateItem", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> updateItem(@Valid @RequestBody Item item, BindingResult result) {

		System.out.println(item.toString());

		try {
			itemRepository.saveWithTimestamp(item);
		} catch (Exception ex) {
			return ResponseEntity.badRequest()
					.body(messageSource.getMessage("op.failed", null, LocaleContextHolder.getLocale()));
		}
		return ResponseEntity.ok().body(item);

	}

	@DeleteMapping("/delete/{id}")
	@ResponseBody
	public ResponseEntity<Void> deleteItem(@PathVariable String id) {
		itemRepository.deleteById(id);
		return new ResponseEntity<Void>(HttpStatus.ACCEPTED);
	}

	@GetMapping(value = "/getAllItems", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<List<Item>> getAllItems() {
		List<Item> items = itemRepository.findAll();
		return new ResponseEntity<List<Item>>(items, HttpStatus.OK);
	}
	
}