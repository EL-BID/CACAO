/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.services;

import java.util.Optional;

import org.idb.cacao.web.entities.ConfigEMail;
import org.idb.cacao.web.repositories.ConfigEMailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for functionality related to 'ConfigEMail' object
 * 
 * @author Gustavo Figueiredo
 *
 */
@Service
@Transactional
public class ConfigEMailService implements IConfigEMailService {
	
	@Autowired
	ConfigEMailRepository configEmailRepository;
	
	@Autowired
	Environment env;

	@Autowired
	KeyStoreService keystoreService;

	@Override
	public ConfigEMail getActiveConfig() {
		Optional<ConfigEMail> config = configEmailRepository.findById(ConfigEMail.ID_ACTIVE_CONFIG);
		return config.orElse(null);
	}
	
	@Override
	public void setActiveConfig(ConfigEMail config) {
		config.setId(ConfigEMail.ID_ACTIVE_CONFIG);
		configEmailRepository.save(config);
	}

	@Override
	public String decryptPassword(String password) {
		if (password==null || password.length()==0)
			return password;
		return keystoreService.decrypt(KeyStoreService.PREFIX_MAIL, password);
	}
	
	@Override
	public String encryptPassword(String password) {
		if (password==null || password.length()==0)
			return password;
		return keystoreService.encrypt(KeyStoreService.PREFIX_MAIL, password);
	}
	
}
