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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.idb.cacao.api.AFieldDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.introspect.AnnotatedField;

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
				AnnotatedField field = context.getBeanPropertyDefinition().get().getField();
				if (field!=null) {
					AFieldDescriptor fd = field.getAnnotation(AFieldDescriptor.class);
					if (fd!=null) {
						String externalName = fd.externalName();
						if (externalName!=null) {
							try {
								String description = messageSource.getMessage(externalName, null, LocaleContextHolder.getLocale());
								context.getSpecificationBuilder().description(description);
							}
							catch (Throwable ex) {
								// Ignores errors if the decription could not be found at message properties
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
