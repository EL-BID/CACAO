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
