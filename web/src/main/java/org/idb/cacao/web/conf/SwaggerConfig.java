/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.conf;

import java.util.Collections;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;

import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * Configuration of Swagger (for documenting all the REST API)
 * 
 * @author Gustavo Figueiredo
 *
 */
@Configuration
public class SwaggerConfig {
	
	public static final String DECLARANT_GROUP = "Declarant";
	public static final String TAX_ADMIN_GROUP = "Tax Administration";

    @Autowired
    private MessageSource messages;
    
    @Autowired
    private SwaggerRolesBindingConfig swaggerRolesBindingConfig;

    @Bean
    public Docket apiForDeclarants() {
        return new Docket(DocumentationType.SWAGGER_2)
          .groupName(DECLARANT_GROUP)
          .select()
          .apis(RequestHandlerSelectors.basePackage("org.idb.cacao.web.controllers.rest").and(swaggerRolesBindingConfig.withDeclarantPrivilege()))
          .paths(PathSelectors.ant("/api/**"))
          .build()
          .genericModelSubstitutes(Optional.class, ResponseEntity.class)
          .apiInfo(apiInfo());
    }

    @Bean
    public Docket apiForTaxAuthority() {
        return new Docket(DocumentationType.SWAGGER_2)
          .groupName(TAX_ADMIN_GROUP)
          .select()
          .apis(RequestHandlerSelectors.basePackage("org.idb.cacao.web.controllers.rest"))
          .paths(PathSelectors.ant("/api/**"))
          .build()
          .genericModelSubstitutes(Optional.class, ResponseEntity.class)
          .apiInfo(apiInfo());
    }

    private ApiInfo apiInfo() {
    	return new ApiInfo(
    		/*title*/messages.getMessage("api.app.name", null, LocaleContextHolder.getLocale()),
    		/*description*/messages.getMessage("app.full.name", null, LocaleContextHolder.getLocale()),
    		/*version*/messages.getMessage("api.app.version", null, LocaleContextHolder.getLocale()),
    		/*termsOfServiceUrl*/"/terms", 
    		new Contact("", "", ""),
    		/*license*/"Apache License 2.0",
    		/*licenseUrl*/"https://www.apache.org/licenses/LICENSE-2.0",
    		Collections.emptyList());
    }
}
