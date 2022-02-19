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

    @Bean
    public Docket apiForDeclarants() {
        return new Docket(DocumentationType.SWAGGER_2)
          .groupName(DECLARANT_GROUP)
          .select()
          .apis(RequestHandlerSelectors.basePackage("org.idb.cacao.web.controllers.rest"))
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
