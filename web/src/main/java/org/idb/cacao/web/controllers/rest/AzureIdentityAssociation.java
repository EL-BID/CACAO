/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.tags.Tag;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Controller class for publishing endpoint related to Publisher Domain confirmation required by Azure Identity service
 * 
 * @see https://docs.microsoft.com/pt-br/azure/active-directory/develop/howto-configure-publisher-domain
 * 
 * @author Gustavo Figueiredo
 *
 */
@RestController
@RequestMapping("/.well-known")
@Tag(name="azure-identity-api-controller", description="Controller class for publishing endpoint related to Publisher Domain confirmation required by Azure Identity service")
@ApiIgnore
public class AzureIdentityAssociation {

	@Autowired
	private Environment env;

	/**
	 * Return information about client ID. Microsoft Azure check this endpoint in our domain. This information should be publicly exposed.
	 */
	@GetMapping(value = "/microsoft-identity-association.json", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value="Return information about client ID. Microsoft Azure check this endpoint in our domain. This information should be publicly exposed.",
			response=IdentityAssociations.class)
	@ResponseBody
	public IdentityAssociations getPaymentSlipDetails() {
		
		IdentityAssociations id = new IdentityAssociations();
		
		// Configured App Registration's Application ID
		String clientId = env.getProperty("spring.security.oauth2.client.registration.azure.client-id");
		if (clientId!=null && clientId.trim().length()>0) {
			AssociatedApplication aa = new AssociatedApplication();
			aa.setApplicationId(clientId);
			id.setAssociatedApplications(new AssociatedApplication[] { aa });
		}
		
		return id;
	}
	
	public static class IdentityAssociations {
		
		private AssociatedApplication[] associatedApplications;

		public AssociatedApplication[] getAssociatedApplications() {
			return associatedApplications;
		}

		public void setAssociatedApplications(AssociatedApplication[] associatedApplications) {
			this.associatedApplications = associatedApplications;
		}
		
	}
	
	public static class AssociatedApplication {
		
		private String applicationId;

		public String getApplicationId() {
			return applicationId;
		}

		public void setApplicationId(String applicationId) {
			this.applicationId = applicationId;
		}
		
	}
}
