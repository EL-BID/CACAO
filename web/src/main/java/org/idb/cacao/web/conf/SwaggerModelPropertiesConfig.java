/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.conf;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.idb.cacao.api.AFieldDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.schema.ModelPropertyBuilderPlugin;
import springfox.documentation.spi.schema.contexts.ModelPropertyContext;
import springfox.documentation.swagger.common.SwaggerPluginSupport;

/**
 * Component used to include descriptions to the model properties definitions using
 * the application specific annotations ('AFieldDescriptor') in replacement to the
 * Springfox standard annotations ('ApiModelProperty')
 * 
 * @author Gustavo Figueiredo
 *
 */
@Component
@Order(SwaggerPluginSupport.SWAGGER_PLUGIN_ORDER)
public class SwaggerModelPropertiesConfig implements ModelPropertyBuilderPlugin {

    private static final Logger LOG = Logger.getLogger(SwaggerModelPropertiesConfig.class.getName());

	@Autowired
	private MessageSource messageSource;

	@Override
	public boolean supports(DocumentationType delimiter) {
		return SwaggerPluginSupport.pluginDoesApply(delimiter);
	}

	@Override
	public void apply(ModelPropertyContext context) {
		try {
			
			if (context.getBeanPropertyDefinition().isPresent()
				&& !context.getAnnotatedElement().isPresent()) {
				
				// If we don't have the @ApiModelProperty for this model property and
				// if we have a bean reference to this property, let's check for the
				// presence of the application specific @AFieldDescriptor
				Optional<BeanPropertyDefinition> beanPropertyDefinition = context.getBeanPropertyDefinition();
				if ( beanPropertyDefinition.isPresent() ) {
					AnnotatedField field = beanPropertyDefinition.get().getField();
					if (field!=null) {
						AFieldDescriptor fd = field.getAnnotation(AFieldDescriptor.class);
						if (fd!=null) {
							String externalName = fd.externalName();
							if (externalName!=null) {
								try {
									String description = messageSource.getMessage(externalName, null, LocaleContextHolder.getLocale());
									context.getSpecificationBuilder().description(description);
								}
								catch (Exception ex) {
									// Ignores errors if the decription could not be found at message properties
								}
							}
						}
					}
				}
			}
			
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error when creating swagger documentation for security roles: ", e);
        }
	}

}
