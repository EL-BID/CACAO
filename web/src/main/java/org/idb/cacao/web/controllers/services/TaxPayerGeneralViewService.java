package org.idb.cacao.web.controllers.services;

import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class TaxPayerGeneralViewService {
	
	private static final Logger log = Logger.getLogger(TaxPayerGeneralViewService.class.getName());

	/**
	 * Maximum number of records that we will return for a given parameter.
	 * Should not be too high because it would compromise queries whenever used as query criteria
	 */
	public static final int MAX_TAXPAYERS_PER_TAXMANAGER = 10_000;	
	
	@Autowired
	private Environment env;
	
	

}
