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

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.idb.cacao.web.controllers.services.PrivilegeService;
import org.idb.cacao.web.entities.SystemPrivilege;
import org.idb.cacao.web.entities.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.swagger.annotations.ApiOperation;
import springfox.documentation.RequestHandler;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.OperationBuilderPlugin;
import springfox.documentation.spi.service.contexts.OperationContext;
import springfox.documentation.spring.web.DescriptionResolver;
import springfox.documentation.swagger.common.SwaggerPluginSupport;

/**
 * Component used to include the 'roles' informed in 'Secured' annotations in
 * the REST API documentation (with Swagger and SpringFox)
 * 
 * @author Gustavo Figueiredo
 *
 */
@Component
@Order(SwaggerPluginSupport.SWAGGER_PLUGIN_ORDER + 1)
public class SwaggerRolesBindingConfig implements OperationBuilderPlugin {

    private static final Logger LOG = Logger.getLogger(SwaggerRolesBindingConfig.class.getName());

    private final DescriptionResolver descriptions;
    
    @Autowired
    private PrivilegeService privilegeService;
    
    private Set<String> declarantPrivileges;

    @Autowired
    public SwaggerRolesBindingConfig(DescriptionResolver descriptions) {
        this.descriptions = descriptions;
    }

    @Override
    public void apply(OperationContext context) {
        try {
            StringBuilder sb = new StringBuilder();

            // Check authorization
            Optional<Secured> securedAnnotation = context.findAnnotation(Secured.class);
            sb.append("<b>Roles</b>: ");
            if (securedAnnotation.isPresent()) {
            	
            	if (SwaggerConfig.DECLARANT_GROUP.equalsIgnoreCase(context.getDocumentationContext().getGroupName())) {
            		if (!hasDeclarantPrivilege(securedAnnotation.get().value())) {
            			context.operationBuilder().hidden(true);
            			return;
            		}
            	}
            	
                sb.append("<em>" + String.join(", ",securedAnnotation.get().value()) + "</em>");
                
            } else {
                sb.append("<em>&nbsp;</em>");
            }

            // Check notes
            Optional<ApiOperation> annotation = context.findAnnotation(ApiOperation.class);
            if (annotation.isPresent() && StringUtils.hasText(annotation.get().notes())) {
                sb.append("<br /><br />");
                sb.append(annotation.get().notes());
            }

            // Add the note text to the Swagger UI
            context.operationBuilder().notes(descriptions.resolve(sb.toString()));
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error when creating swagger documentation for security roles: ", e);
        }
    }

    @Override
    public boolean supports(DocumentationType delimiter) {
        return SwaggerPluginSupport.pluginDoesApply(delimiter);
    }
    
    /**
     * Returns a RequestHandler predicate selector for filtering out all API methods not accessible
     * by Declarant profile
     */
    public Predicate<RequestHandler> withDeclarantPrivilege() {
    	return new Predicate<RequestHandler>() {

			@Override
			public boolean test(RequestHandler t) {
				Optional<Secured> securedAnnotation = t.findAnnotation(Secured.class);
				return !securedAnnotation.isPresent()
						|| hasDeclarantPrivilege(securedAnnotation.get().value());
			}
    		
    	};
    }
    
    private boolean hasDeclarantPrivilege(String[] roles) {
    	if (roles==null || roles.length==0)
    		return true;
    	if (declarantPrivileges==null) {
    		declarantPrivileges = privilegeService.getPrivileges(UserProfile.DECLARANT).stream().map(SystemPrivilege::getRole)
    				.collect(Collectors.toCollection(()->new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
    	}
    	return Arrays.stream(roles).anyMatch(declarantPrivileges::contains);
    }
}
