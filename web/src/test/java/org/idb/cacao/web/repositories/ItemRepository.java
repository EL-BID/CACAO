package org.idb.cacao.web.repositories;

import java.util.List;
import java.util.Optional;

import org.idb.cacao.web.entities.Item;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends ElasticsearchRepository<Item, String>{

	/**
	 * Find a user given the user id. There should be only one.
	 */
	public Optional<Item> findById(String id);
	
	public List<Item> findAll();

}