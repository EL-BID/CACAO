/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.services;

import java.util.Optional;

import org.idb.cacao.web.entities.ConfigEMail;
import org.idb.cacao.web.entities.ConfigSync;
import org.idb.cacao.web.repositories.ConfigSyncRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for functionality related to 'ConfigSync' object
 * 
 * @author Gustavo Figueiredo
 *
 */
@Service
@Transactional
public class ConfigSyncService implements IConfigSyncService {

	@Autowired
	ConfigSyncRepository configSyncRepository;
	
	@Autowired
	Environment env;

	@Autowired
	KeyStoreService keystoreService;

	@Override
	public ConfigSync getActiveConfig() {
		Optional<ConfigSync> config = configSyncRepository.findById(ConfigSync.ID_ACTIVE_CONFIG);
		return config.orElse(null);
	}
	
	@Override
	public void setActiveConfig(ConfigSync config) {
		config.setId(ConfigEMail.ID_ACTIVE_CONFIG);
		configSyncRepository.save(config);
	}

	@Override
	public String decryptToken(String token) {
		if (token==null || token.length()==0)
			return token;
		return keystoreService.decrypt(KeyStoreService.PREFIX_MAIL, token);
	}
	
	@Override
	public String encryptToken(String token) {
		if (token==null || token.length()==0)
			return token;
		return keystoreService.encrypt(KeyStoreService.PREFIX_MAIL, token);
	}

}
